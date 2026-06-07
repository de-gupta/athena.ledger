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

**Cell references** — A1 notation, case-insensitive (`A1`, `bc42`, `ZZ99`).  
**Column references** — letter (`A`, `AA`) or 1-based integer (`1`, `27`).  
**Row references** — 1-based integer only (`1`, `42`).  
**Type tokens** — `STR` `NUM` `BOOL` `DATE` `FORMULA` `EMPTY`.  
**Type inference** (used by `write`, `write-range`, `import-csv`) — `true`/`false` → `BOOL`; `YYYY-MM-DD` → `DATE`;
numeric → `NUM`; anything else → `STR`. For `FORMULA`, leading `=` is optional.

### Command summary

| Command                                                                                   | Purpose                                                     |
|-------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| `xl sheets <file>`                                                                        | list sheets with row counts                                 |
| `xl read <file> <sheet> <cell>`                                                           | read one cell → `TYPE:value`                                |
| `xl write <file> <sheet> <cell> <value> [--type TYPE]`                                    | write one cell                                              |
| `xl read-range <file> <sheet> <from> <to> [--typed]`                                      | read rectangular range → TSV                                |
| `xl write-range <file> <sheet> <start-cell> [--overwrite]`                                | write TSV from stdin                                        |
| `xl read-row <file> <sheet> <row> [--typed]`                                              | read entire row → single TSV line                           |
| `xl read-col <file> <sheet> <col> [--typed]`                                              | read entire column → one value per line                     |
| `xl evaluate <file> <sheet> <cell>`                                                       | read computed value; evaluates formula cells → `TYPE:value` |
| `xl import-csv <file> <sheet> <csv-file> [--start-cell A1] [--overwrite] [--delimiter ,]` | import CSV file into sheet                                  |
| `xl create <file> [--overwrite]`                                                          | create new workbook                                         |
| `xl add-sheet <file> <name>`                                                              | add blank sheet                                             |
| `xl rename-sheet <file> <name> <new-name>`                                                | rename sheet in place                                       |
| `xl delete-sheet <file> <name>`                                                           | delete sheet (fails if last)                                |
| `xl copy-sheet <file> <source> <new-name>`                                                | copy sheet within workbook                                  |
| `xl move-sheet <file> <name> <position>`                                                  | reorder sheet (0-based position)                            |
| `xl insert-row <file> <sheet> <row>`                                                      | insert blank row, shift down                                |
| `xl delete-row <file> <sheet> <row>`                                                      | delete row, shift up                                        |
| `xl delete-column <file> <sheet> <col>`                                                   | delete column, shift left                                   |
| `xl set-col-width <file> <sheet> <col> <width>`                                           | set column width in character units                         |
| `xl auto-fit <file> <sheet> [<col>]`                                                      | auto-fit one or all columns                                 |
| `xl tab-color <file> <sheet> <hex-rgb>`                                                   | set sheet tab color                                         |
| `xl find-col <file> <sheet> <header>`                                                     | find column letter of named header in first row             |
| `xl insert-col <file> <sheet> <col>`                                                      | insert column at position, shift right, fill from stdin     |
| `xl append-col <file> <sheet>`                                                            | append column after last occupied, fill from stdin          |
| `xl stats <file> <sheet> <from> <to>`                                                     | compute count/min/max/mean/stdev over numeric range         |

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

### `xl read-range <file> <sheet> <from> <to> [--typed]`

Read a rectangular range of cells and print as TSV (tab-separated values). Each row is a line; columns are separated by
tabs. Empty cells produce empty fields.

```
$ xl read-range report.xlsx Sheet1 A1 C3
Name    Score   Grade
Alice   95      A
Bob     87      B
```

Output is compatible with `xl write-range` stdin — you can pipe the output of `read-range` directly into `write-range`
to copy a range.

```
$ xl read-range src.xlsx Sheet1 A1 C10 | xl write-range dst.xlsx Sheet1 A1 --overwrite
```

Add `--typed` to prefix each cell value with its type token — identical to the format used by `xl read`:

```
$ xl read-range report.xlsx Sheet1 A1 B2 --typed
STR:Name    STR:Score
STR:Alice   NUM:95.0
```

**Number formatting (plain mode):** whole numbers are printed without a decimal (e.g. `95`, not `95.0`). Fractional
numbers print as-is (`42.5`). Dates use ISO-8601 (`2026-06-07`). Formulas print their expression without a leading `=`.

**Trailing empty columns:** trailing empty cells at the end of a row are stripped from the output line. Mid-row empty
cells produce empty fields (e.g. `Name\t\tGrade` for a missing middle cell).

**Errors:** file not found, invalid cell reference, inverted range (from is below or to the right of to).

---

### `xl write-range <file> <sheet> <start-cell> [--overwrite]`

Write a table of values from stdin, starting at the given cell. Input is read as TSV (tab-separated values): each line
is a row, tabs separate columns. Type inference is applied to each cell value using the same rules as `xl write`.

```
$ printf "Name\tScore\nAlice\t95\nBob\t87" | xl write-range report.xlsx Sheet1 B2
```

This writes a 3-row × 2-column table starting at B2:

```
B2=STR:Name   C2=STR:Score
B3=STR:Alice  C3=NUM:95.0
B4=STR:Bob    C4=NUM:87.0
```

Without `--overwrite`, any target cell that already contains a value is skipped; only empty cells are written. Pass
`--overwrite` to replace all cells in the range regardless of existing content.

```
$ printf "header\t42" | xl write-range report.xlsx Sheet1 A1 --overwrite
```

**Errors:** file not found, invalid start cell reference.

---

### `xl copy-sheet <file> <source-sheet> <new-name>`

Copy an existing sheet to a new sheet in the same workbook.

```
xl copy-sheet report.xlsx "Template" "Q3"
```

**Errors:** file not found, source sheet not found, target name already exists.

---

### `xl rename-sheet <file> <sheet-name> <new-name>`

Rename a sheet in place. Safer than `copy-sheet` + `delete-sheet` because it never creates an intermediate name.

```
xl rename-sheet vol-surface.xlsx VolSurface AAPL
```

**Errors:** file not found, sheet not found, new name already exists.

---

### `xl move-sheet <file> <sheet-name> <position>`

Move a sheet to the given 0-based tab position. Position 0 = first tab.

```
xl move-sheet position.xlsx positions 0
```

**Errors:** file not found, sheet not found, position out of range.

---

### `xl delete-column <file> <sheet> <column>`

Delete a column and shift all columns to its right one position left. Column can be a letter (`A`, `B`, `AA`) or a
1-based integer (`1`, `2`).

```
xl delete-column vol-surface.xlsx MU I
xl delete-column vol-surface.xlsx MU 9
```

**Errors:** file not found, sheet not found, invalid column notation.

---

### `xl tab-color <file> <sheet> <hex-rgb>`

Set the tab color of a sheet. Color is a 6-digit hex RGB string without a leading `#`.

```
xl tab-color position-valuation.xlsx positions   4472C4   # blue
xl tab-color position-valuation.xlsx trajectories FFFF00   # yellow
xl tab-color position-valuation.xlsx results      70AD47   # green
xl tab-color position-valuation.xlsx totals       FFC000   # orange
```

**Errors:** file not found, sheet not found, invalid hex string.

---

### `xl read-row <file> <sheet> <row> [--typed]`

Read an entire row as a single TSV line. `row` is a 1-based row number.

```
xl read-row vol-surface.xlsx AAPL 1
# → strike	2025-07-18	2025-09-19	2026-01-16

xl read-row report.xlsx Sheet1 1 --typed
# → STR:Name	STR:Score
```

**Errors:** file not found, invalid row number.

---

### `xl read-col <file> <sheet> <col> [--typed]`

Read an entire column, one value per line. `col` accepts a letter (`A`) or 1-based integer (`1`).

```
xl read-col positions.xlsx positions A
# → AAPL
# → TSLA
# → NVDA
```

**Errors:** file not found, invalid column notation.

---

### `xl evaluate <file> <sheet> <cell>`

Read a cell's computed value. For formula cells, evaluates the formula and returns the result as a typed value. For
non-formula cells, behaves identically to `xl read`.

```
xl evaluate report.xlsx Sheet1 A3
# → NUM:30.0   (where A3 contains =SUM(A1:A2))
```

Output format is identical to `xl read`: `TYPE:value`.

**Errors:** file not found, invalid cell reference.

---

### `xl insert-row <file> <sheet> <row>`

Insert a blank row at the given 1-based position, shifting all existing rows at or below it down by one.

```
xl insert-row report.xlsx Sheet1 3
```

**Errors:** file not found, sheet not found, invalid row number.

---

### `xl delete-row <file> <sheet> <row>`

Delete a row and shift all rows below it up by one.

```
xl delete-row report.xlsx Sheet1 3
```

**Errors:** file not found, sheet not found, invalid row number.

---

### `xl set-col-width <file> <sheet> <col> <width>`

Set a column width in Excel character units (the same units shown in Excel's column width dialog).

```
xl set-col-width report.xlsx Sheet1 A 20
xl set-col-width report.xlsx Sheet1 3 15
```

**Errors:** file not found, sheet not found, invalid column notation, width less than 1.

---

### `xl auto-fit <file> <sheet> [<col>]`

Auto-fit column widths to their content. If `col` is omitted, fits all columns in the sheet.

```
xl auto-fit report.xlsx Sheet1          # fit all columns
xl auto-fit report.xlsx Sheet1 B        # fit column B only
xl auto-fit report.xlsx Sheet1 2        # fit column 2 (B) only
```

**Errors:** file not found, sheet not found.

---

### `xl import-csv <file> <sheet> <csv-file> [options]`

Import a CSV file into a sheet. All rows — including the header row — are written to Excel. Type inference is applied to
each cell using the same rules as `xl write`.

```
xl import-csv report.xlsx Sheet1 data.csv
xl import-csv report.xlsx Sheet1 data.csv --start-cell B2
xl import-csv report.xlsx Sheet1 data.csv --overwrite
xl import-csv report.xlsx Sheet1 european.csv --delimiter ";"
xl import-csv report.xlsx Sheet1 data.tsv --delimiter "\t"
```

**Options:**

| Option         | Default | Description                                                                  |
|----------------|---------|------------------------------------------------------------------------------|
| `--start-cell` | `A1`    | Top-left cell to begin writing                                               |
| `--overwrite`  | false   | Replace existing non-empty cells; without this, only empty cells are written |
| `--delimiter`  | `,`     | Field separator character; use `;` for European CSVs, `\t` for TSV           |

**Input format:** standard RFC 4180 CSV — quoted fields (with `""` escaping for embedded quotes), `\r\n` or `\n` line
endings. Blank lines are skipped.

**Errors:** CSV file not found, invalid start cell, invalid Excel file path.

---

### `xl find-col <file> <sheet> <header>`

Find the column letter of a named header in the first row of a sheet. Outputs only the letter with no trailing newline —
designed for shell variable assignment.

```bash
xl find-col vol-surface.xlsx AAPL strike
# → A

col=$(xl find-col position.xlsx positions spotPrice)
printf "950\n950\n950\n" | xl write-range vol-surface.xlsx AAPL "${col}2"
```

Search is case-sensitive. Returns the leftmost match if duplicates exist.

**Errors:** file not found, sheet not found, header not found in first row.

---

### `xl insert-col <file> <sheet> <col>`

Insert a new blank column at the given position, shifting all existing columns right by one. Reads one cell value per
line from stdin and writes into the new column starting at row 1. Type inference is applied.

```bash
printf "strike\n700\n800\n900\n" | xl insert-col vol-surface.xlsx MU A
printf "spotPrice\n950\n950\n950\n" | xl insert-col position.xlsx positions N
```

`col` accepts a letter (`A`, `N`) or 1-based integer (`1`, `14`).

**Errors:** file not found, sheet not found, invalid column reference.

---

### `xl append-col <file> <sheet>`

Append a new column immediately after the last occupied column. Reads one cell value per line from stdin. Type inference
is applied.

```bash
printf "spotPrice\n950\n950\n" | xl append-col position.xlsx positions
```

**Errors:** file not found, sheet not found.

---

### `xl stats <file> <sheet> <from> <to>`

Compute statistics over the numeric cells in a rectangular range. Non-numeric cells are excluded from aggregation but
counted. Output is `key:value` pairs, one per line.

```
$ xl stats vol-surface.xlsx AAPL B2 E7
count:24
numeric:24
non-numeric:0
min:0.198000
max:0.550000
mean:0.305000
stdev:0.074000
```

If `numeric` is 0, the `min`, `max`, `mean`, and `stdev` lines are omitted.

**Scripting pattern** — fail if vols are out of range:

```bash
result=$(xl stats vol-surface.xlsx MU B2 E7)
min=$(echo "$result" | grep '^min:' | cut -d: -f2)
max=$(echo "$result" | grep '^max:' | cut -d: -f2)
awk "BEGIN { exit !($min >= 0.05 && $max <= 2.0) }" && echo "OK" || echo "Out of range" >&2
```

**Errors:** file not found, sheet not found, invalid cell reference, inverted range.

---

### `xl find-row <file> <sheet> <col> <value>`

Find the 1-based row number of the first cell in a column that matches a value. Output is a bare integer with no
trailing newline — clean for use in `$(...)`. Complement to `find-col`.

```bash
xl find-row vol-surface.xlsx MU A 900
# → 4

row=$(xl find-row vol-surface.xlsx MU A 900)
xl read-row vol-surface.xlsx MU $row          # read that strike's full vol curve
xl write vol-surface.xlsx MU B$row 0.4050     # update a single vol
```

**Matching:** uses the plain display value (same as `read-range` plain mode, no `TYPE:` prefix). Numbers match without
trailing `.0` where possible (`900` matches a cell containing `NUM:900`).

**Errors:** file not found, sheet not found, invalid column reference, value not found.

---

### `xl dims <file> <sheet>`

Print the populated range of a sheet as `FROM:TO` (e.g. `A1:E7`). No trailing newline — clean for shell variable
assignment. Useful for avoiding hardcoded range bounds in scripts.

```bash
xl dims vol-surface.xlsx AAPL
# → A1:E7

range=$(xl dims vol-surface.xlsx MU)
xl read-range vol-surface.xlsx MU $range               # full surface as TSV
xl read-range vol-surface.xlsx MU $range | xl write-range dst.xlsx MU A1 --overwrite
```

Empty sheets produce an error, not a degenerate range.

**Errors:** file not found, sheet not found, sheet is empty.

---

### `xl export-csv <file> <sheet> <csv-file> [--delimiter ,]`

Export a full sheet to a CSV file. Inverse of `import-csv`. Sheet bounds are discovered automatically via `dims`. RFC
4180 quoting is applied to fields containing the delimiter, double-quotes, or newlines.

```bash
xl export-csv vol-surface.xlsx MU MU-vol-surface.csv
xl export-csv vol-surface.xlsx MU MU-vol-surface.csv --delimiter ;
```

Cell values are written as their plain display strings (no `TYPE:` prefix). The `--delimiter` option matches
`import-csv`.

**Round-trip:** `export-csv` followed by `import-csv` is lossless for strings, numbers, and booleans. Date cells export
as `YYYY-MM-DD` and re-infer as `DATE` on import. Formula cells export their last computed string value — the formula
expression is lost.

**Errors:** file not found, sheet not found, sheet is empty, output file not writable.

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

#### `readRange`

```java
Fallible<CellGrid> readRange(Path file, String sheet, String fromCell, String toCell)
```

Read a rectangular range. Returns a `CellGrid` whose dimensions match `rowCount = toRow - fromRow + 1` and
`columnCount = toCol - fromCol + 1`. Cells not present in the sheet are returned as `CellValue.Empty`.

```java
xl.readRange(Path.of("report.xlsx"), "Sheet1","A1","C3")
		.

fold(
		grid ->
		{
		grid.

rows().

forEach(row ->
		System.out.

println(row.stream()
                  .

map(Object::toString)
                  .

collect(Collectors.joining("\t"))));
		return null;
		},
ex ->{System.err.

println(ex.getMessage());return null;}
		);
```

`CellGrid` accessors:

```java
int rows = grid.rowCount();        // number of rows
int cols = grid.columnCount();     // columns in the first row
boolean none = grid.isEmpty();         // true when rowCount() == 0
List<List<CellValue>> raw = grid.rows(); // immutable row-major data
CellValue cell = grid.rows().get(r).get(c); // cell at row r, column c (0-based)
```

The range corners are validated: `fromCell` must be at or above and to the left of `toCell`. Passing an inverted range (
e.g. `"C5"` → `"A1"`) throws `IllegalArgumentException`.

**Failures:** `WorkbookNotFoundException`, `IllegalArgumentException` (invalid cell reference or inverted range).

---

#### `writeRange`

```java
Fallible<Void> writeRange(WriteRangeRequest request)
```

Write a 2D table starting at a given cell. The target range is defined by the start cell plus the dimensions of the
data — no end-cell argument needed.

```java
import de.gupta.xl.application.transfer.WriteRangeRequest;
import de.gupta.xl.domain.CellGrid;
import de.gupta.xl.domain.CellValue;

var grid = CellGrid.of(List.of(
		List.of(new CellValue.Str("Name"), new CellValue.Str("Score")),
		List.of(new CellValue.Str("Alice"), new CellValue.Num(95.0)),
		List.of(new CellValue.Str("Bob"), new CellValue.Num(87.0))
));

var request = new WriteRangeRequest(
		Path.of("report.xlsx"),
		"Sheet1",
		"B2",
		grid,
		true        // overwrite=true → replace existing content
);

xl.

writeRange(request).

fold(
		_  ->null,
ex ->{System.err.

println(ex.getMessage());return null;}
		);
```

`WriteRangeRequest` fields:

| Field       | Type       | Description                                                |
|-------------|------------|------------------------------------------------------------|
| `file`      | `Path`     | Workbook file                                              |
| `sheet`     | `String`   | Target sheet name (created if absent)                      |
| `startCell` | `String`   | Top-left corner in A1 notation                             |
| `grid`      | `CellGrid` | Table data (see below)                                     |
| `overwrite` | `boolean`  | `true` = replace all cells; `false` = skip non-empty cells |

`CellGrid` is the domain type for 2D tabular cell data:

```java
CellGrid grid = CellGrid.of(rows);   // from List<List<CellValue>>
CellGrid empty = CellGrid.empty();   // convenience factory
int count = grid.rowCount();         // number of rows
boolean nothing = grid.isEmpty();    // true when rowCount() == 0
List<List<CellValue>> raw = grid.rows(); // underlying data (immutable)
```

An empty `CellGrid` is a no-op — the repository is not called.

**Failures:** `WorkbookNotFoundException`, `IllegalArgumentException` (invalid start cell).

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

#### `renameSheet`

```java
Fallible<Void> renameSheet(Path file, String sheetName, String newName)
```

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`, `SheetAlreadyExistsException` (new name taken).

---

#### `moveSheet`

```java
Fallible<Void> moveSheet(Path file, String sheetName, int position)
```

`position` is 0-based. Throws `IllegalArgumentException` if out of range.

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`, `IllegalArgumentException`.

---

#### `deleteColumn`

```java
Fallible<Void> deleteColumn(Path file, String sheet, String columnNotation)
```

`columnNotation` accepts letter notation (`"A"`, `"AA"`) or 1-based integer (`"1"`, `"26"`). Columns after the deleted
one shift left.

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`, `IllegalArgumentException` (invalid notation).

---

#### `setTabColor`

```java
Fallible<Void> setTabColor(Path file, String sheet, String hexRgb)
```

`hexRgb` is a 6-digit hex RGB string, with or without a leading `#` (e.g. `"70AD47"` or `"#70AD47"`).

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`, `IllegalArgumentException` (invalid hex).

---

#### `readRow`

```java
Fallible<List<CellValue>> readRow(Path file, String sheet, String rowRef)
```

`rowRef` is a 1-based row number string. Returns values for every used cell in the row; missing cells return
`CellValue.Empty`.

---

#### `readColumn`

```java
Fallible<List<CellValue>> readColumn(Path file, String sheet, String columnRef)
```

`columnRef` accepts letter (`"A"`) or 1-based integer (`"1"`). Returns one `CellValue` per row from row 0 to the sheet's
last used row.

---

#### `evaluateCell`

```java
Fallible<CellValue> evaluateCell(Path file, String sheet, String cellReference)
```

For formula cells, evaluates the formula and returns the computed typed value (`Num`, `Str`, `Bool`). For non-formula
cells, behaves identically to `readCell`.

---

#### `insertRow`

```java
Fallible<Void> insertRow(Path file, String sheet, String rowRef)
```

Inserts a blank row at the 1-based `rowRef`, shifting existing rows at or below it down by one.

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`, `IllegalArgumentException` (invalid row).

---

#### `deleteRow`

```java
Fallible<Void> deleteRow(Path file, String sheet, String rowRef)
```

Deletes the row and shifts all rows below it up by one. Deleting a non-existent row is a no-op.

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`, `IllegalArgumentException` (invalid row).

---

#### `setColumnWidth`

```java
Fallible<Void> setColumnWidth(Path file, String sheet, String columnRef, int characterWidth)
```

`characterWidth` uses the same units Excel shows in the column width dialog.

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`, `IllegalArgumentException`.

---

#### `autoFitColumn` / `autoFitAllColumns`

```java
Fallible<Void> autoFitColumn(Path file, String sheet, String columnRef)
Fallible<Void> autoFitAllColumns(Path file, String sheet)
```

Auto-size one or all columns to fit their content.

**Failures:** `WorkbookNotFoundException`, `SheetNotFoundException`.

---

#### `importCsv`

```java
Fallible<Void> importCsv(Path xlFile, String sheet, Path csvFile,
                         String startCell, boolean overwrite, char delimiter)
```

Parse a CSV file and write it to a sheet as a `CellGrid`, starting at `startCell`. Internally converts the CSV rows to a
`CellGrid` with type inference, then delegates to `writeRange`. No new repository method is involved.

```java
xl.importCsv(
		Path.of("report.xlsx"),
    "Sheet1",
			Path.

of("data.csv"),
    "A1",
			true,   // overwrite existing content
			','     // comma delimiter
			);
```

Common delimiter values: `','` (standard CSV), `';'` (European CSV), `'\t'` (TSV).

**Failures:** `UncheckedIOException` (CSV file unreadable), `WorkbookNotFoundException`, `IllegalArgumentException` (
invalid start cell).

---

### Domain types (package `de.gupta.xl.domain`)

| Type                 | Description                                                                                             |
|----------------------|---------------------------------------------------------------------------------------------------------|
| `CellValue`          | Sealed interface with variants `Str`, `Num`, `Bool`, `Date`, `Formula`, `Empty`                         |
| `CellReference`      | Parsed A1 notation; `CellReference.of("B3")` → `columnIndex()=1, rowIndex()=2`                          |
| `CellGrid`           | Immutable 2D table; `CellGrid.of(rows)`, `CellGrid.empty()`, `rowCount()`, `columnCount()`, `isEmpty()` |
| `CellRangeReference` | Rectangular range address; `CellRangeReference.of("A1","C5")`, `rowCount()`, `columnCount()`            |
| `ColumnReference`    | Column address; `ColumnReference.of("C")` or `ColumnReference.of("3")` → `index()` (0-based)            |
| `RowReference`       | Row address; `RowReference.of("1")` → `index()` (0-based); only 1-based integers accepted               |
| `TabColor`           | RGB tab color; `TabColor.of("70AD47")` → `red()`, `green()`, `blue()`                                   |
| `Sheet`              | Record: `name()`, `rowCount()`                                                                          |
| `WorkbookContent`    | Record: `path()`, `sheets()`                                                                            |

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