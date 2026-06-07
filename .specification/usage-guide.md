# xl — Usage Guide

## Overview

`xl` exposes a single application core through multiple entry points. All operations flow through `XlFacade`, which
returns `Fallible<T>` (from aletheia) so callers handle success and failure uniformly without try/catch.

This guide covers:

1. [CLI](#cli) — shell commands for agent and human use
2. [Java API](#java-api) — direct programmatic use via `XlFacade`
3. [REST](#rest) — not yet implemented (planned)

---

## CLI

Installed at `~/.local/bin/xl` via `install.sh`. All commands follow the pattern:

```
xl <command> <file> [args...] [options]
```

Exit codes: `0` = success, `1` = error (message on stderr).

---

### `xl sheets <file>`

List all sheets with their row counts.

```
xl sheets report.xlsx
```

```
Sheet1      (142 rows)
Summary     (12 rows)
```

**Errors:** file not found.

---

### `xl read <file> <sheet> <cell>`

Read a single cell. Output format: `TYPE:value`.

```
xl read report.xlsx Sheet1 A1
# → STR:Order ID

xl read report.xlsx Sheet1 B3
# → NUM:42.5

xl read report.xlsx Sheet1 C4
# → DATE:2026-06-01

xl read report.xlsx Sheet1 D5
# → BOOL:true

xl read report.xlsx Sheet1 E6
# → FORMULA:SUM(B2:B10)

xl read report.xlsx Sheet1 F7
# → EMPTY:
```

**Cell reference format:** standard A1 notation, case-insensitive (`A1`, `bc42`, `ZZ99`).

**Type tokens:**

| Token     | Meaning                                  |
|-----------|------------------------------------------|
| `STR`     | String                                   |
| `NUM`     | Double-precision number                  |
| `BOOL`    | Boolean                                  |
| `DATE`    | ISO local date (`YYYY-MM-DD`)            |
| `FORMULA` | Formula expression (without leading `=`) |
| `EMPTY`   | Blank or missing cell                    |

**Errors:** file not found, invalid cell reference.

---

### `xl write <file> <sheet> <cell> <value> [--type TYPE]`

Write a value to a cell. Creates the sheet if it does not exist.

```
xl write report.xlsx Sheet1 A1 "Hello World"
xl write report.xlsx Sheet1 B2 42.5
xl write report.xlsx Sheet1 C3 2026-06-01 --type DATE
xl write report.xlsx Sheet1 D4 true --type BOOL
xl write report.xlsx Sheet1 E5 "=SUM(B2:B10)" --type FORMULA
```

**Type inference (when `--type` is omitted):**

| Input matches                        | Inferred type |
|--------------------------------------|---------------|
| `true` or `false` (case-insensitive) | `BOOL`        |
| ISO date `YYYY-MM-DD`                | `DATE`        |
| Parseable as a number                | `NUM`         |
| Anything else                        | `STR`         |

**`--type` values:** `STR`, `NUM`, `BOOL`, `DATE`, `FORMULA`.

For `FORMULA`, the leading `=` is optional — `"=SUM(A1:A5)"` and `"SUM(A1:A5)"` are equivalent.

**Errors:** file not found, invalid cell reference, unparseable value for the given type.

---

### `xl create <file> [--overwrite]`

Create a new workbook with a single blank sheet named `Sheet1`.

```
xl create report.xlsx
xl create report.xlsx --overwrite
```

Without `--overwrite`, fails if the file already exists.

**Errors:** file already exists (without `--overwrite`).

---

### `xl add-sheet <file> <sheet-name>`

Add a new blank sheet to an existing workbook.

```
xl add-sheet report.xlsx "Q2 Summary"
```

**Errors:** file not found, sheet name already exists.

---

### `xl delete-sheet <file> <sheet-name>`

Delete a sheet by name.

```
xl delete-sheet report.xlsx "Draft"
```

**Errors:** file not found, sheet not found, sheet is the last remaining sheet.

---

### `xl copy-sheet <file> <source-sheet> <new-name>`

Copy an existing sheet to a new sheet in the same workbook.

```
xl copy-sheet report.xlsx "Template" "Q3"
```

**Errors:** file not found, source sheet not found, target name already exists.

---

## Java API

### Dependency

Add `xl-application` to your Maven dependencies:

```xml

<dependency>
    <groupId>io.github.de-gupta</groupId>
    <artifactId>xl-application</artifactId>
    <version>0.0.3-SNAPSHOT</version>
</dependency>
```

Add an adapter for I/O. The only current implementation is the POI adapter:

```xml

<dependency>
    <groupId>io.github.de-gupta</groupId>
    <artifactId>xl-adapter-poi</artifactId>
    <version>0.0.3-SNAPSHOT</version>
</dependency>
```

---

### Wiring

Construct the object graph manually (no DI framework required):

```java
import de.gupta.xl.adapter.poi.PoiWorkbookRepository;
import de.gupta.xl.application.facade.XlFacadeImpl;
import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.service.WorkbookService;

var repository = new PoiWorkbookRepository();
var service = new WorkbookService(repository);
XlFacade xl = new XlFacadeImpl(service);
```

With a DI framework, register `PoiWorkbookRepository` as `WorkbookRepository`, `WorkbookService` as itself, and
`XlFacadeImpl` as `XlFacade`.

---

### `XlFacade` interface

All methods return `Fallible<T>` from the aletheia library. Use `.fold(onSuccess, onFailure)` to extract the result:

```java
var result = xl.someMethod(...);
var value = result.fold(
		success -> /* handle success */,
		failure -> /* handle failure */
);
```

Or use `.map()` to chain transformations and let failures propagate automatically.

---

#### `listSheets`

```java
Fallible<List<SheetSummary>> listSheets(Path file)
```

Returns all sheets with their row counts.

```java
xl.listSheets(Path.of("report.xlsx"))
		.

fold(
		sheets ->sheets.

forEach(s ->System.out.

println(s.name() +": "+s.

rowCount())),
ex    ->System.err.

println(ex.getMessage())
		);
```

`SheetSummary` fields: `String name()`, `int rowCount()`.

**Failures:** `WorkbookNotFoundException` — file does not exist.

---

#### `readCell`

```java
Fallible<CellValue> readCell(Path file, String sheet, String cellReference)
```

Reads a single cell. Returns `CellValue.Empty` for blank or missing cells (never fails due to emptiness alone).

```java
xl.readCell(Path.of("report.xlsx"), "Sheet1","B3")
		.

fold(
		value ->switch(value){
		case CellValue.

Str(var s)     ->"string: "+s;
          case CellValue.

Num(var n)     ->"number: "+n;
          case CellValue.

Bool(var b)    ->"bool: "+b;
          case CellValue.

Date(var d)    ->"date: "+d;
          case CellValue.

Formula(var f) ->"formula: "+f;
          case CellValue.

Empty()        ->"empty";
		},
ex ->{System.err.

println(ex.getMessage());return null;}
		);
```

**Failures:** `WorkbookNotFoundException`, `IllegalArgumentException` (invalid cell reference).

---

#### `writeCell`

```java
Fallible<Void> writeCell(WriteCellRequest request)
```

Writes a value to a cell. Creates the row/cell if absent; preserves all other content.

```java
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.domain.CellValue;

var request = new WriteCellRequest(
		Path.of("report.xlsx"),
		"Sheet1",
		"A1",
		new CellValue.Str("Hello")
);

xl.

writeCell(request).

fold(
		_  ->null,                          // success — no return value
ex ->{System.err.

println(ex.getMessage());return null;}
		);
```

**`CellValue` constructors:**

| Type    | Constructor                                    |
|---------|------------------------------------------------|
| String  | `new CellValue.Str("text")`                    |
| Number  | `new CellValue.Num(42.5)`                      |
| Boolean | `new CellValue.Bool(true)`                     |
| Date    | `new CellValue.Date(LocalDate.of(2026, 6, 1))` |
| Formula | `new CellValue.Formula("SUM(A1:A5)")`          |
| Blank   | `new CellValue.Empty()`                        |

**Type inference from a raw string:**

```java
CellValue value = CellValue.infer("2026-06-01");  // → CellValue.Date
CellValue value = CellValue.infer("42.5");         // → CellValue.Num
CellValue value = CellValue.infer("true");         // → CellValue.Bool
CellValue value = CellValue.infer("hello");        // → CellValue.Str
CellValue value = CellValue.infer(null);           // → CellValue.Empty
```

**Failures:** `WorkbookNotFoundException`, `IllegalArgumentException` (invalid cell reference).

---

#### `createWorkbook`

```java
Fallible<Void> createWorkbook(Path file, boolean overwrite)
```

Creates a new workbook with one blank sheet named `Sheet1`. Pass `overwrite = true` to replace an existing file.

```java
xl.createWorkbook(Path.of("new.xlsx"), false).

fold(
		_  ->null,
ex ->{System.err.

println(ex.getMessage());return null;}
		);
```

**Failures:** `WorkbookAlreadyExistsException` (file exists and `overwrite` is false).

---

#### `addSheet`

```java
Fallible<Void> addSheet(Path file, String sheetName)
```

```java
xl.addSheet(Path.of("report.xlsx"), "Q3 Summary");
```

**Failures:** `WorkbookNotFoundException`, `SheetAlreadyExistsException`.

---

#### `deleteSheet`

```java
Fallible<Void> deleteSheet(Path file, String sheetName)
```

```java
xl.deleteSheet(Path.of("report.xlsx"), "Draft");
```

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`, `LastSheetException`.

---

#### `copySheet`

```java
Fallible<Void> copySheet(Path file, String sourceSheet, String targetName)
```

```java
xl.copySheet(Path.of("report.xlsx"), "Template","Q4");
```

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException` (source), `SheetAlreadyExistsException` (target).

---

### Domain types (package `de.gupta.xl.domain`)

| Type              | Description                                                                     |
|-------------------|---------------------------------------------------------------------------------|
| `CellValue`       | Sealed interface with variants `Str`, `Num`, `Bool`, `Date`, `Formula`, `Empty` |
| `CellReference`   | Parsed A1 notation; `CellReference.of("B3")` → `columnIndex()=1, rowIndex()=2`  |
| `Sheet`           | Record: `name()`, `rowCount()`                                                  |
| `WorkbookContent` | Record: `path()`, `sheets()`                                                    |

### Exception types (package `de.gupta.xl.domain.exception`)

| Exception                        | When thrown                                |
|----------------------------------|--------------------------------------------|
| `WorkbookNotFoundException`      | File path does not exist                   |
| `WorkbookAlreadyExistsException` | File exists and overwrite not requested    |
| `SheetNotFoundException`         | Sheet name not found in workbook           |
| `SheetAlreadyExistsException`    | Sheet name already present                 |
| `LastSheetException`             | Attempt to delete the only remaining sheet |

All are unchecked (`RuntimeException`). They carry a descriptive message including the offending path or sheet name.

---

## REST

Not yet implemented. The architecture supports adding a REST adapter without any changes to the application core:

```java
// Future: a REST controller would simply inject XlFacade and delegate
@PostMapping("/sheets/{file}")
ResponseEntity<?> listSheets(@PathVariable String file)
{
	return xl.listSheets(Path.of(file))
	         .fold(ResponseEntity::ok, ex -> ResponseEntity.badRequest().body(ex.getMessage()));
}
```

The `xl-application` module exports `XlFacade` as the only interface a new adapter needs to depend on.