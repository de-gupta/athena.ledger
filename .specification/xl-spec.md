# xl — Agent-First Excel Tool: Implementation Spec

## Overview

`xl` is a library and tool for reading and writing Excel (`.xlsx`) files. The **CLI** is the first consumer, designed to
be called by agents as naturally as `cp` or `grep`, but the architecture is intentionally layered so that any caller — a
CLI command, a REST controller, or a direct Java call — goes through the same application core.

The architecture follows **Clean Architecture / Hexagonal (Ports & Adapters)** principles with an explicit **API →
Facade → Service → Domain** layering. No layer reaches past its immediate neighbour.

---

## Technology Stack

- **Java 25**
- **picocli** — CLI framework; provides structured `--help`, subcommands, type conversion, and shell completion
- **Apache POI** (`poi-ooxml`) — Excel read/write (infrastructure concern, isolated in the `adapter.out.poi` package)
- **JUnit 5 + AssertJ** — tests (follow `~/.claude/TestStyle.md`)
- **Maven** — single-module project, fat JAR via `maven-shade-plugin`
- **Coding style** — follow `~/.claude/CODING_GUIDELINES.md` throughout

---

## Layered Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  API Layer  (adapter.in.*)                                   │
│  CLI (picocli), REST controller, or plain Java call          │
│  Translates user input → facade DTOs; formats results        │
└────────────────────────┬─────────────────────────────────────┘
                         │ calls
┌────────────────────────▼─────────────────────────────────────┐
│  Facade Layer  (application.facade.*)                        │
│  XlFacade interface + XlFacadeImpl                           │
│  Orchestrates use cases; speaks in plain value objects       │
│  (no POI types, no picocli types visible here)               │
└────────────────────────┬─────────────────────────────────────┘
                         │ calls
┌────────────────────────▼─────────────────────────────────────┐
│  Service Layer  (application.service.*)                      │
│  WorkbookService — pure business logic                       │
│  Enforces invariants; delegates I/O to output port           │
└──────────┬─────────────────────────────┬─────────────────────┘
           │ uses                        │ calls output port
┌──────────▼──────────┐      ┌───────────▼────────────────────┐
│  Domain             │      │  Output Port                   │
│  (domain.*)         │      │  WorkbookRepository (interface)│
│  CellReference      │      └───────────┬────────────────────┘
│  CellValue          │                  │ implemented by
│  WorkbookContent    │      ┌───────────▼────────────────────┐
│  Sheet              │      │  POI Adapter  (adapter.out.poi)│
└─────────────────────┘      │  PoiWorkbookRepository         │
                             └────────────────────────────────┘
```

### Layer contract rules

| From        | May depend on                          | Must not depend on                 |
|-------------|----------------------------------------|------------------------------------|
| CLI adapter | Facade interface, domain value objects | Service, POI adapter               |
| Facade impl | Service, domain                        | CLI types, POI types               |
| Service     | Domain, output port interface          | Adapter implementations, CLI types |
| Domain      | Nothing else in this project           | Any framework or adapter           |
| POI adapter | POI library, domain                    | Facade, service, CLI               |

---

## Project Structure

```
xl/
  src/
    main/java/de/gupta/xl/
      domain/
        CellReference.java         # parses A1 notation → row/col indices
        CellValue.java             # sealed type: Str | Num | Bool | Date | Formula | Empty
        Sheet.java                 # value object: name + rows
        WorkbookContent.java       # aggregate root: file path + list of Sheet

      application/
        port/
          in/
            XlFacade.java          # primary input port (interface) — the public API contract
          out/
            WorkbookRepository.java # secondary output port (interface) — load/save workbooks
        facade/
          XlFacadeImpl.java        # implements XlFacade; translates between API DTOs and service calls
        service/
          WorkbookService.java     # business logic; calls WorkbookRepository for I/O
        dto/
          SheetSummary.java        # {name, rowCount}
          CellAddress.java         # {sheet, cellRef} — used in read/write requests
          WriteRequest.java        # {file, sheet, cell, value, type?}

      adapter/
        in/
          cli/
            XlCommand.java         # @Command entry point, registers subcommands
            command/
              SheetsCommand.java
              ReadCommand.java
              WriteCommand.java
              CreateCommand.java
              AddSheetCommand.java
              DeleteSheetCommand.java
              CopySheetCommand.java
        out/
          poi/
            PoiWorkbookRepository.java  # implements WorkbookRepository using Apache POI

  src/
    test/java/de/gupta/xl/
      domain/
        CellReferenceTest.java
        CellValueTest.java
      application/
        service/
          WorkbookServiceTest.java
        facade/
          XlFacadeImplTest.java
      adapter/
        in/
          cli/
            CliIntegrationTest.java    # end-to-end: invoke commands, assert stdout/exit code
        out/
          poi/
            PoiWorkbookRepositoryTest.java

  pom.xml
  install.sh
```

---

## Domain Model

### `CellReference`

- Parses `A1`, `b3`, `AA10`, etc. (case-insensitive)
- Column letters → 0-based column index (A=0, B=1, Z=25, AA=26, …)
- Row number → 0-based row index (row 1 = index 0)
- Invalid reference → throw `IllegalArgumentException` with a clear message

### `CellValue` — sealed type

```java
public sealed interface CellValue
		permits CellValue.Str, CellValue.Num, CellValue.Bool,
		CellValue.Date, CellValue.Formula, CellValue.Empty
{
	record Str(String value) implements CellValue
	{
	}

	record Num(double value) implements CellValue
	{
	}

	record Bool(boolean value) implements CellValue
	{
	}

	record Date(LocalDate value) implements CellValue
	{
	}

	record Formula(String expr) implements CellValue
	{
	}

	record Empty() implements CellValue
	{
	}
}
```

`CellValue` carries no POI or CLI concerns. Type inference lives here as a static factory:

```java
static CellValue infer(String raw)
{ ...}
```

**Inference rules:**

- Matches `true`/`false` (case-insensitive) → `Bool`
- Matches ISO date `YYYY-MM-DD` → `Date`
- Parseable as a number → `Num`
- Otherwise → `Str`

### `Sheet` and `WorkbookContent`

```java
public record Sheet(String name, int rowCount)
{
}

public record WorkbookContent(Path path, List<Sheet> sheets)
{
}
```

These are immutable value objects used throughout the application without leaking POI types past the repository
boundary.

---

## Application Layer

### `XlFacade` — primary input port

```java
public interface XlFacade
{
	List<SheetSummary> listSheets(Path file);

	CellValue readCell(Path file, String sheet, String cellRef);

	void writeCell(WriteRequest request);

	void createWorkbook(Path file, boolean overwrite);

	void addSheet(Path file, String sheetName);

	void deleteSheet(Path file, String sheetName);

	void copySheet(Path file, String sourceSheet, String newName);
}
```

All parameters and return types are domain value objects or standard Java types — no POI, no picocli.

### `WorkbookRepository` — output port

```java
public interface WorkbookRepository
{
	WorkbookContent load(Path file);

	void save(WorkbookContent content);

	CellValue readCell(Path file, String sheet, CellReference ref);

	void writeCell(Path file, String sheet, CellReference ref, CellValue value);

	void createWorkbook(Path file);

	void addSheet(Path file, String sheetName);

	void deleteSheet(Path file, String sheetName);

	void copySheet(Path file, String sourceSheet, String newName);
}
```

### `WorkbookService`

Enforces invariants that belong to the application (not the domain and not the adapter):

- File-must-exist checks before read operations
- Sheet-must-not-exist check before `addSheet`
- Last-sheet guard on `deleteSheet`
- Overwrite guard on `createWorkbook`

Delegates actual I/O to `WorkbookRepository`.

### `XlFacadeImpl`

Thin orchestrator between the CLI/API and the service. Translates `WriteRequest` DTOs into typed service calls, converts
exceptions into domain-friendly error types if needed.

---

## Infrastructure Layer — POI Adapter

`PoiWorkbookRepository` implements `WorkbookRepository` using Apache POI. It:

- Opens existing files without corrupting them
- Preserves all sheets, styles, and content not explicitly modified
- Writes atomically: write to a temp file then rename, so a crash never corrupts the original

All POI types (`XSSFWorkbook`, `XSSFSheet`, `XSSFCell`, etc.) are confined to this class. Nothing outside
`adapter.out.poi` imports from `org.apache.poi`.

---

## CLI Layer — Adapter

`XlCommand` is a picocli `@Command` class that wires together the dependency graph manually (no DI framework in v1) and
registers subcommands. Each subcommand:

1. Validates and converts raw CLI strings (picocli handles most of this via type converters)
2. Calls one method on `XlFacade`
3. Formats the result to stdout following the output conventions below

The CLI adapter does not contain any business logic. If you find yourself enforcing an invariant inside a `*Command`
class, move it to `WorkbookService`.

### Commands

#### `xl sheets <file>`

List all sheet names with row counts.

```
$ xl sheets data.xlsx
Sheet1    (142 rows)
Summary   (12 rows)
```

#### `xl read <file> <sheet> <cell>`

Read a single cell. Prints `TYPE:value` on stdout.

```
$ xl read data.xlsx Sheet1 B3
NUM:42.5

$ xl read data.xlsx Sheet1 A1
STR:Order ID
```

#### `xl write <file> <sheet> <cell> <value> [--type TYPE]`

Write a value to a cell. Infers type from value if `--type` is omitted.

```
$ xl write data.xlsx Sheet1 A1 "Hello"
$ xl write data.xlsx Sheet1 B2 42.5
$ xl write data.xlsx Sheet1 C3 2026-06-01 --type DATE
$ xl write data.xlsx Sheet1 D4 true --type BOOL
$ xl write data.xlsx Sheet1 E5 "=SUM(B2:B10)" --type FORMULA
```

**Types:**
| Type | POI cell type |
|---|---|
| `STR` | `STRING` |
| `NUM` | `NUMERIC` |
| `BOOL` | `BOOLEAN` |
| `DATE` | `NUMERIC` with date format |
| `FORMULA` | `FORMULA` (value is the formula string without leading `=`) |

#### `xl create <file> [--overwrite]`

Create a new empty workbook with one blank sheet named `Sheet1`. Fails if file already exists unless `--overwrite` is
passed.

#### `xl add-sheet <file> <sheet-name>`

Add a new blank sheet. Fails if a sheet with that name already exists.

#### `xl delete-sheet <file> <sheet-name>`

Delete a sheet by name. Fails if the sheet does not exist or is the last sheet in the workbook.

#### `xl copy-sheet <file> <source-sheet> <new-name>`

Copy an existing sheet to a new sheet within the same workbook.

---

## Output Conventions (CLI)

- **Success**: zero or one line of output (the result), exit code 0
- **Failure**: one-line error message to stderr, exit code 1
- **No silent failures**: if the file does not exist, the sheet is not found, or the cell reference is invalid, print a
  clear message and exit 1
- Never print stack traces to stdout

---

## Error Handling Strategy

Domain and service exceptions propagate as unchecked exceptions:

| Exception                        | Thrown by         | Meaning                               |
|----------------------------------|-------------------|---------------------------------------|
| `WorkbookNotFoundException`      | `WorkbookService` | File does not exist                   |
| `SheetNotFoundException`         | `WorkbookService` | Sheet name not found                  |
| `SheetAlreadyExistsException`    | `WorkbookService` | Duplicate sheet name                  |
| `LastSheetException`             | `WorkbookService` | Cannot delete the only sheet          |
| `WorkbookAlreadyExistsException` | `WorkbookService` | File exists and `--overwrite` not set |
| `IllegalArgumentException`       | `CellReference`   | Invalid A1 notation                   |

The CLI adapter catches these at the top level and maps them to a one-line stderr message + exit code 1. Other callers (
REST, direct Java) catch them at their own boundary.

---

## Testing Requirements

Follow `~/.claude/TestStyle.md` strictly.

| Test class                  | Scope                      | Coverage targets                                              |
|-----------------------------|----------------------------|---------------------------------------------------------------|
| `CellReferenceTest`         | unit                       | valid, invalid, edge cases — parameterised                    |
| `CellValueTest`             | unit                       | type inference rules — parameterised                          |
| `WorkbookServiceTest`       | unit (mock repository)     | all guard conditions, happy paths                             |
| `XlFacadeImplTest`          | unit (mock service)        | DTO mapping, delegation                                       |
| `PoiWorkbookRepositoryTest` | integration (real `.xlsx`) | read/write round-trip, atomic write, sheet preservation       |
| `CliIntegrationTest`        | end-to-end                 | each subcommand against real files; stdout, stderr, exit code |

---

## Installation

Build produces a fat JAR. A thin wrapper script makes it a system command:

```bash
#!/usr/bin/env bash
exec java -jar ~/.local/lib/xl/xl.jar "$@"
```

### `install.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail
mvn -q package -DskipTests
mkdir -p ~/.local/lib/xl
cp target/xl-*-jar-with-dependencies.jar ~/.local/lib/xl/xl.jar
cat > ~/.local/bin/xl <<'EOF'
#!/usr/bin/env bash
exec java -jar ~/.local/lib/xl/xl.jar "$@"
EOF
chmod +x ~/.local/bin/xl
echo "Installed: $(which xl)"
```

---

## Future Work (not in scope for v1, but architecture must not prevent them)

- `xl range <file> <sheet> <from> <to>` — read a rectangular range as TSV
- `xl import <file> <sheet> <csv>` — bulk-write from CSV
- `xl format <file> <sheet> <cell> --bold --color RED` — cell formatting
- REST adapter (`XlController`) calling `XlFacade` — zero changes to application core
- Library JAR distribution: consumers depend on `application.port.in.XlFacade` and inject `XlFacadeImpl` with their own
  `WorkbookRepository` if needed
- DI framework (Quarkus / Spring) wiring — all classes are POJO; adding `@ApplicationScoped` or `@Service` is the only
  change needed