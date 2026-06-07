package de.gupta.xl.adapter.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("xl CLI end-to-end")
final class CliITCase
{
	private Path file;
	private CommandLine commandLine;

	@BeforeEach
	void setUp(@TempDir final Path directory)
	{
		file = directory.resolve("workbook.xlsx");
		commandLine = XlCommand.buildCommandLine();
	}

	private int execute(final String... arguments)
	{
		return commandLine.execute(arguments);
	}

	private String captureOutput(final String... arguments)
	{
		var buffer = new ByteArrayOutputStream();
		var originalOut = System.out;
		System.setOut(new PrintStream(buffer));
		try
		{
			commandLine.execute(arguments);
		}
		finally
		{
			System.setOut(originalOut);
		}
		return buffer.toString().strip();
	}

	@Nested
	@DisplayName("create")
	final class Create
	{
		@Test
		@DisplayName("exits 0 and creates the file")
		void exits0AndCreatesTheFile()
		{
			var exitCode = execute("create", file.toString());

			assertThat(exitCode).as("exit code").isZero();
			assertThat(file.toFile().exists()).as("file created").isTrue();
		}

		@Test
		@DisplayName("exits 1 when file already exists and --overwrite is not given")
		void exits1WhenFileAlreadyExistsWithoutOverwrite()
		{
			execute("create", file.toString());
			var exitCode = execute("create", file.toString());

			assertThat(exitCode).as("exit code without overwrite").isEqualTo(1);
		}

		@Test
		@DisplayName("exits 0 when file already exists and --overwrite is given")
		void exits0WhenFileExistsWithOverwrite()
		{
			execute("create", file.toString());
			var exitCode = execute("create", "--overwrite", file.toString());

			assertThat(exitCode).as("exit code with overwrite").isZero();
		}
	}

	@Nested
	@DisplayName("sheets")
	final class Sheets
	{
		@Test
		@DisplayName("lists Sheet1 after create")
		void listsSheet1AfterCreate()
		{
			execute("create", file.toString());

			var output = captureOutput("sheets", file.toString());

			assertThat(output).as("sheets output").contains("Sheet1");
		}
	}

	@Nested
	@DisplayName("write and read")
	final class WriteAndRead
	{
		@BeforeEach
		void createWorkbook()
		{
			execute("create", file.toString());
		}

		@Test
		@DisplayName("reads back a string value written to a cell")
		void readsBackStringValueWrittenToCell()
		{
			execute("write", file.toString(), "Sheet1", "A1", "hello");

			var output = captureOutput("read", file.toString(), "Sheet1", "A1");

			assertThat(output).as("read output").isEqualTo("STR:hello");
		}

		@Test
		@DisplayName("reads back a numeric value written to a cell")
		void readsBackNumericValueWrittenToCell()
		{
			execute("write", file.toString(), "Sheet1", "B2", "42.5");

			var output = captureOutput("read", file.toString(), "Sheet1", "B2");

			assertThat(output).as("read output").isEqualTo("NUM:42.5");
		}

		@Test
		@DisplayName("reads back a boolean value written with explicit type")
		void readsBackBooleanValueWrittenWithExplicitType()
		{
			execute("write", file.toString(), "Sheet1", "C3", "true", "--type", "BOOL");

			var output = captureOutput("read", file.toString(), "Sheet1", "C3");

			assertThat(output).as("read output").isEqualTo("BOOL:true");
		}

		@Test
		@DisplayName("reads back a date value written with explicit type")
		void readsBackDateValueWrittenWithExplicitType()
		{
			execute("write", file.toString(), "Sheet1", "D4", "2026-06-07", "--type", "DATE");

			var output = captureOutput("read", file.toString(), "Sheet1", "D4");

			assertThat(output).as("read output").isEqualTo("DATE:2026-06-07");
		}

		@Test
		@DisplayName("exits 0 on successful write")
		void exits0OnSuccessfulWrite()
		{
			var exitCode = execute("write", file.toString(), "Sheet1", "A1", "data");

			assertThat(exitCode).as("exit code").isZero();
		}

		@Test
		@DisplayName("exits 1 when cell reference is invalid")
		void exits1WhenCellReferenceIsInvalid()
		{
			var exitCode = execute("read", file.toString(), "Sheet1", "INVALID");

			assertThat(exitCode).as("exit code for invalid ref").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("add-sheet")
	final class AddSheet
	{
		@Test
		@DisplayName("adds a sheet and it appears in sheets listing")
		void addsSheetAndItAppearsInSheetsListing()
		{
			execute("create", file.toString());
			execute("add-sheet", file.toString(), "Q2");

			var output = captureOutput("sheets", file.toString());

			assertThat(output).as("sheets output").contains("Q2");
		}

		@Test
		@DisplayName("exits 1 when sheet already exists")
		void exits1WhenSheetAlreadyExists()
		{
			execute("create", file.toString());
			var exitCode = execute("add-sheet", file.toString(), "Sheet1");

			assertThat(exitCode).as("duplicate sheet exit code").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("delete-sheet")
	final class DeleteSheet
	{
		@Test
		@DisplayName("removes the sheet from the workbook")
		void removesTheSheetFromTheWorkbook()
		{
			execute("create", file.toString());
			execute("add-sheet", file.toString(), "ToDelete");
			execute("delete-sheet", file.toString(), "ToDelete");

			var output = captureOutput("sheets", file.toString());

			assertThat(output).as("sheets output after delete").doesNotContain("ToDelete");
		}

		@Test
		@DisplayName("exits 1 when trying to delete the last sheet")
		void exits1WhenDeletingLastSheet()
		{
			execute("create", file.toString());
			var exitCode = execute("delete-sheet", file.toString(), "Sheet1");

			assertThat(exitCode).as("last sheet exit code").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("copy-sheet")
	final class CopySheet
	{
		@Test
		@DisplayName("copy appears in sheets listing")
		void copyAppearsInSheetsListing()
		{
			execute("create", file.toString());
			execute("copy-sheet", file.toString(), "Sheet1", "Sheet1Copy");

			var output = captureOutput("sheets", file.toString());

			assertThat(output).as("sheets output after copy").contains("Sheet1Copy");
		}
	}

	@Nested
	@DisplayName("read-row")
	final class ReadRow
	{
		@Test
		@DisplayName("outputs a TSV line of the row values")
		void outputsATsvLineOfTheRowValues()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "alpha");
			execute("write", file.toString(), "Sheet1", "B1", "42");

			var output = captureOutput("read-row", file.toString(), "Sheet1", "1");

			assertThat(output).as("row 1 as TSV").isEqualTo("alpha\t42");
		}

		@Test
		@DisplayName("outputs typed tokens with --typed flag")
		void outputsTypedTokensWithTypedFlag()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "hello");

			var output = captureOutput("read-row", file.toString(), "Sheet1", "1", "--typed");

			assertThat(output).as("typed row 1").isEqualTo("STR:hello");
		}
	}

	@Nested
	@DisplayName("read-col")
	final class ReadColumn
	{
		@Test
		@DisplayName("outputs one value per line for the column")
		void outputsOneValuePerLineForTheColumn()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "first");
			execute("write", file.toString(), "Sheet1", "A2", "second");

			var lines = captureOutput("read-col", file.toString(), "Sheet1", "A").lines().toList();

			assertThat(lines).as("column values").containsExactly("first", "second");
		}

		@Test
		@DisplayName("accepts 1-based integer column notation")
		void accepts1BasedIntegerColumnNotation()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "value");

			var output = captureOutput("read-col", file.toString(), "Sheet1", "1");

			assertThat(output).as("column 1 value").isEqualTo("value");
		}
	}

	@Nested
	@DisplayName("evaluate")
	final class Evaluate
	{
		@Test
		@DisplayName("returns computed formula result")
		void returnsComputedFormulaResult()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "10");
			execute("write", file.toString(), "Sheet1", "A2", "20");
			execute("write", file.toString(), "Sheet1", "A3", "=SUM(A1:A2)", "--type", "FORMULA");

			var output = captureOutput("evaluate", file.toString(), "Sheet1", "A3");

			assertThat(output).as("evaluated formula").isEqualTo("NUM:30.0");
		}

		@Test
		@DisplayName("returns plain value for non-formula cell")
		void returnsPlainValueForNonFormulaCell()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "B1", "hello");

			assertThat(captureOutput("evaluate", file.toString(), "Sheet1", "B1"))
					.as("plain value").isEqualTo("STR:hello");
		}
	}

	@Nested
	@DisplayName("insert-row and delete-row")
	final class RowOperations
	{
		@Test
		@DisplayName("insert-row shifts existing data down")
		void insertRowShiftsExistingDataDown()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "was-row-1");
			execute("insert-row", file.toString(), "Sheet1", "1");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "A2"))
					.as("original data shifted to row 2").isEqualTo("STR:was-row-1");
		}

		@Test
		@DisplayName("delete-row shifts data below it up")
		void deleteRowShiftsDataBelowItUp()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "delete-me");
			execute("write", file.toString(), "Sheet1", "A2", "shift-up");
			execute("delete-row", file.toString(), "Sheet1", "1");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1"))
					.as("row 2 shifted to row 1").isEqualTo("STR:shift-up");
		}
	}

	@Nested
	@DisplayName("set-col-width and auto-fit")
	final class ColumnWidthCommands
	{
		@Test
		@DisplayName("set-col-width exits 0 and preserves cell data")
		void setColWidthExits0AndPreservesCellData()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "data");

			assertThat(execute("set-col-width", file.toString(), "Sheet1", "A", "25"))
					.as("exit code").isZero();
			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1"))
					.as("data preserved").isEqualTo("STR:data");
		}

		@Test
		@DisplayName("auto-fit exits 0 and preserves cell data")
		void autoFitExits0AndPreservesCellData()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "a long value to auto-fit");

			assertThat(execute("auto-fit", file.toString(), "Sheet1"))
					.as("exit code for all-columns fit").isZero();
			assertThat(execute("auto-fit", file.toString(), "Sheet1", "A"))
					.as("exit code for single column fit").isZero();
			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1"))
					.as("data preserved after auto-fit").isEqualTo("STR:a long value to auto-fit");
		}

		@Test
		@DisplayName("set-col-width exits 1 for width zero")
		void setColWidthExits1ForWidthZero()
		{
			execute("create", file.toString());

			assertThat(execute("set-col-width", file.toString(), "Sheet1", "A", "0"))
					.as("exit code for zero width").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("import-csv")
	final class ImportCsv
	{
		@Test
		@DisplayName("writes all rows including header to the sheet starting at A1")
		void writesAllRowsIncludingHeaderToSheetStartingAtA1(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			var csvFile = directory.resolve("data.csv");
			java.nio.file.Files.writeString(csvFile, "Name,Score\nAlice,95\nBob,87");

			execute("import-csv", file.toString(), "Sheet1", csvFile.toString());

			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1")).isEqualTo("STR:Name");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "B1")).isEqualTo("STR:Score");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "A2")).isEqualTo("STR:Alice");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "B2")).isEqualTo("NUM:95.0");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "A3")).isEqualTo("STR:Bob");
		}

		@Test
		@DisplayName("writes to a non-default start cell with --start-cell")
		void writesToNonDefaultStartCellWithStartCellOption(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			var csvFile = directory.resolve("data.csv");
			java.nio.file.Files.writeString(csvFile, "X,Y\n1,2");

			execute("import-csv", file.toString(), "Sheet1", csvFile.toString(), "--start-cell", "C3");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "C3")).isEqualTo("STR:X");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "D3")).isEqualTo("STR:Y");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "C4")).isEqualTo("NUM:1.0");
		}

		@Test
		@DisplayName("preserves existing cells without --overwrite")
		void preservesExistingCellsWithoutOverwrite(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "protected");
			var csvFile = directory.resolve("data.csv");
			java.nio.file.Files.writeString(csvFile, "replacement");

			execute("import-csv", file.toString(), "Sheet1", csvFile.toString());

			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1")).isEqualTo("STR:protected");
		}

		@Test
		@DisplayName("overwrites existing cells with --overwrite")
		void overwritesExistingCellsWithOverwrite(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "old");
			var csvFile = directory.resolve("data.csv");
			java.nio.file.Files.writeString(csvFile, "new");

			execute("import-csv", file.toString(), "Sheet1", csvFile.toString(), "--overwrite");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1")).isEqualTo("STR:new");
		}

		@Test
		@DisplayName("handles semicolon-delimited files with --delimiter option")
		void handlesSemicolonDelimitedFilesWithDelimiterOption(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			var csvFile = directory.resolve("data.csv");
			java.nio.file.Files.writeString(csvFile, "Name;Score\nAlice;95");

			execute("import-csv", file.toString(), "Sheet1", csvFile.toString(), "--delimiter", ";");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1")).isEqualTo("STR:Name");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "B2")).isEqualTo("NUM:95.0");
		}

		@Test
		@DisplayName("exits 0 on success")
		void exits0OnSuccess(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			var csvFile = directory.resolve("data.csv");
			java.nio.file.Files.writeString(csvFile, "a,b\n1,2");

			assertThat(execute("import-csv", file.toString(), "Sheet1", csvFile.toString()))
					.as("exit code").isZero();
		}

		@Test
		@DisplayName("exits 1 when csv file does not exist")
		void exits1WhenCsvFileDoesNotExist(@TempDir final Path directory)
		{
			execute("create", file.toString());
			var missing = directory.resolve("missing.csv");

			assertThat(execute("import-csv", file.toString(), "Sheet1", missing.toString()))
					.as("exit code for missing csv").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("find-col")
	final class FindCol
	{
		@Test
		@DisplayName("outputs the column letter of the matching header")
		void outputsTheColumnLetterOfTheMatchingHeader()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "Name");
			execute("write", file.toString(), "Sheet1", "B1", "Score");
			execute("write", file.toString(), "Sheet1", "C1", "Grade");

			var output = captureOutput("find-col", file.toString(), "Sheet1", "Score");

			assertThat(output).as("column letter for Score").isEqualTo("B");
		}

		@Test
		@DisplayName("exits 1 when header is not found")
		void exits1WhenHeaderIsNotFound()
		{
			execute("create", file.toString());

			assertThat(execute("find-col", file.toString(), "Sheet1", "Missing"))
					.as("exit code").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("insert-col and append-col")
	final class ColumnOperations
	{
		@Test
		@DisplayName("insert-col shifts existing data right")
		void insertColShiftsExistingDataRight()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "Name");
			execute("write", file.toString(), "Sheet1", "B1", "Score");
			executeWithStdin("Rank\n1\n2", "insert-col", file.toString(), "Sheet1", "B");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "B1"))
					.as("B1 has new header").isEqualTo("STR:Rank");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "C1"))
					.as("Score shifted to C").isEqualTo("STR:Score");
		}

		@Test
		@DisplayName("append-col writes after the last column")
		void appendColWritesAfterTheLastColumn()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "X");
			execute("write", file.toString(), "Sheet1", "B1", "Y");
			executeWithStdin("Z", "append-col", file.toString(), "Sheet1");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "C1"))
					.as("C1 has appended value").isEqualTo("STR:Z");
		}

		@Test
		@DisplayName("find-col then insert-col pipe pattern works")
		void findColThenInsertColPipePatternWorks()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "Name");
			execute("write", file.toString(), "Sheet1", "B1", "Score");

			var column = captureOutput("find-col", file.toString(), "Sheet1", "Score");
			executeWithStdin("Rank", "insert-col", file.toString(), "Sheet1", column);

			assertThat(captureOutput("read", file.toString(), "Sheet1", "B1"))
					.as("Rank inserted at B").isEqualTo("STR:Rank");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "C1"))
					.as("Score shifted to C").isEqualTo("STR:Score");
		}
	}

	@Nested
	@DisplayName("stats")
	final class Stats
	{
		@Test
		@DisplayName("outputs correct statistics for a numeric range")
		void outputsCorrectStatisticsForANumericRange()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "10");
			execute("write", file.toString(), "Sheet1", "A2", "20");
			execute("write", file.toString(), "Sheet1", "A3", "30");

			var output = captureOutput("stats", file.toString(), "Sheet1", "A1", "A3");
			var lines = output.lines().toList();

			assertThat(lines).as("output lines").anySatisfy(l -> assertThat(l).startsWith("count:3"));
			assertThat(lines).as("numeric count").anySatisfy(l -> assertThat(l).startsWith("numeric:3"));
			assertThat(lines).as("non-numeric").anySatisfy(l -> assertThat(l).startsWith("non-numeric:0"));
			assertThat(lines).as("mean").anySatisfy(l -> assertThat(l).startsWith("mean:20"));
		}

		@Test
		@DisplayName("omits min/max/mean/stdev when range has no numeric cells")
		void omitsNumericFieldsWhenRangeHasNoNumericCells()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "hello");

			var output = captureOutput("stats", file.toString(), "Sheet1", "A1", "A1");

			assertThat(output).as("no min field").doesNotContain("min:");
			assertThat(output).as("non-numeric count").contains("non-numeric:1");
		}

		@Test
		@DisplayName("exits 0 on success")
		void exits0OnSuccess()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "42");

			assertThat(execute("stats", file.toString(), "Sheet1", "A1", "A1"))
					.as("exit code").isZero();
		}
	}

	@Nested
	@DisplayName("find-row")
	final class FindRow
	{
		@Test
		@DisplayName("outputs 1-based row number of matching value")
		void outputs1BasedRowNumberOfMatchingValue()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "100");
			execute("write", file.toString(), "Sheet1", "A2", "200");
			execute("write", file.toString(), "Sheet1", "A3", "900");

			var output = captureOutput("find-row", file.toString(), "Sheet1", "A", "900");

			assertThat(output).as("row number").isEqualTo("3");
		}

		@Test
		@DisplayName("matches numbers by plain decimal representation")
		void matchesNumbersByPlainDecimalRepresentation()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "42.5");

			var output = captureOutput("find-row", file.toString(), "Sheet1", "A", "42.5");

			assertThat(output).as("fractional number row").isEqualTo("1");
		}

		@Test
		@DisplayName("exits 1 when value is not found")
		void exits1WhenValueIsNotFound()
		{
			execute("create", file.toString());

			assertThat(execute("find-row", file.toString(), "Sheet1", "A", "missing"))
					.as("exit code").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("dims")
	final class DimsCommand
	{
		@Test
		@DisplayName("outputs A1:CN notation for populated range")
		void outputsA1ColonNotationForPopulatedRange()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "B2", "tl");
			execute("write", file.toString(), "Sheet1", "D4", "br");

			var output = captureOutput("dims", file.toString(), "Sheet1");

			assertThat(output).as("dims output").isEqualTo("B2:D4");
		}

		@Test
		@DisplayName("exits 0 for a single-cell populated sheet")
		void exits0ForSingleCellPopulatedSheet()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "C3", "data");

			assertThat(execute("dims", file.toString(), "Sheet1")).as("exit code").isZero();
			assertThat(captureOutput("dims", file.toString(), "Sheet1")).isEqualTo("C3:C3");
		}

		@Test
		@DisplayName("exits 1 for an empty sheet")
		void exits1ForAnEmptySheet()
		{
			execute("create", file.toString());

			assertThat(execute("dims", file.toString(), "Sheet1")).as("exit code").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("export-csv")
	final class ExportCsv
	{
		@Test
		@DisplayName("exports sheet data to a CSV file")
		void exportsSheetDataToACsvFile(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "Name");
			execute("write", file.toString(), "Sheet1", "B1", "Score");
			execute("write", file.toString(), "Sheet1", "A2", "Alice");
			execute("write", file.toString(), "Sheet1", "B2", "95");
			var csvFile = directory.resolve("out.csv");

			execute("export-csv", file.toString(), "Sheet1", csvFile.toString());

			var lines = java.nio.file.Files.readAllLines(csvFile);
			assertThat(lines.get(0)).as("header row").startsWith("Name,Score");
			assertThat(lines.get(1)).as("data row").startsWith("Alice,95");
		}

		@Test
		@DisplayName("round-trips with import-csv")
		void roundTripsWithImportCsv(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "Name");
			execute("write", file.toString(), "Sheet1", "B1", "Score");
			execute("write", file.toString(), "Sheet1", "A2", "Alice");
			execute("write", file.toString(), "Sheet1", "B2", "95");
			var csvFile = directory.resolve("out.csv");
			var destination = directory.resolve("dest.xlsx");
			execute("create", destination.toString());

			execute("export-csv", file.toString(), "Sheet1", csvFile.toString());
			execute("import-csv", destination.toString(), "Sheet1", csvFile.toString(), "--overwrite");

			assertThat(captureOutput("read", destination.toString(), "Sheet1", "A1")).isEqualTo("STR:Name");
			assertThat(captureOutput("read", destination.toString(), "Sheet1", "B2")).isEqualTo("NUM:95.0");
		}

		@Test
		@DisplayName("exits 1 for empty sheet")
		void exits1ForEmptySheet(@TempDir final Path directory)
		{
			execute("create", file.toString());
			var csvFile = directory.resolve("out.csv");

			assertThat(execute("export-csv", file.toString(), "Sheet1", csvFile.toString()))
					.as("exit code for empty sheet").isEqualTo(1);
		}

		@Test
		@DisplayName("handles quoted fields with commas correctly")
		void handlesQuotedFieldsWithCommasCorrectly(@TempDir final Path directory) throws Exception
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "Smith, John");
			var csvFile = directory.resolve("out.csv");

			execute("export-csv", file.toString(), "Sheet1", csvFile.toString());

			var content = java.nio.file.Files.readString(csvFile);
			assertThat(content).as("quoted field").contains("\"Smith, John\"");
		}
	}

	private int executeWithStdin(final String stdinContent, final String... arguments)
	{
		var originalIn = System.in;
		System.setIn(new java.io.ByteArrayInputStream(stdinContent.getBytes(StandardCharsets.UTF_8)));
		try
		{
			return commandLine.execute(arguments);
		}
		finally
		{
			System.setIn(originalIn);
		}
	}

	@Nested
	@DisplayName("rename-sheet")
	final class RenameSheet
	{
		@Test
		@DisplayName("renamed sheet appears in sheets listing")
		void renamedSheetAppearsInSheetsListing()
		{
			execute("create", file.toString());
			execute("rename-sheet", file.toString(), "Sheet1", "Renamed");

			var output = captureOutput("sheets", file.toString());

			assertThat(output).as("new name present").contains("Renamed");
			assertThat(output).as("old name absent").doesNotContain("Sheet1");
		}

		@Test
		@DisplayName("exits 1 when sheet does not exist")
		void exits1WhenSheetDoesNotExist()
		{
			execute("create", file.toString());

			assertThat(execute("rename-sheet", file.toString(), "Missing", "NewName"))
					.as("exit code").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("move-sheet")
	final class MoveSheet
	{
		@Test
		@DisplayName("moved sheet appears at the requested position")
		void movedSheetAppearsAtRequestedPosition()
		{
			execute("create", file.toString());
			execute("add-sheet", file.toString(), "Second");
			execute("move-sheet", file.toString(), "Second", "0");

			var lines = captureOutput("sheets", file.toString()).lines().toList();

			assertThat(lines.getFirst()).as("first tab").startsWith("Second");
		}

		@Test
		@DisplayName("exits 1 when position is out of range")
		void exits1WhenPositionIsOutOfRange()
		{
			execute("create", file.toString());

			assertThat(execute("move-sheet", file.toString(), "Sheet1", "99"))
					.as("exit code").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("delete-column")
	final class DeleteColumn
	{
		@Test
		@DisplayName("column after the deleted one shifts left")
		void columnAfterDeletedOneShiftsLeft()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "keep");
			execute("write", file.toString(), "Sheet1", "B1", "delete-me");
			execute("write", file.toString(), "Sheet1", "C1", "shift-left");
			execute("delete-column", file.toString(), "Sheet1", "B");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "B1"))
					.as("B1 after delete").isEqualTo("STR:shift-left");
		}

		@Test
		@DisplayName("accepts 1-based integer column notation")
		void accepts1BasedIntegerColumnNotation()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "A");
			execute("write", file.toString(), "Sheet1", "B1", "gone");
			execute("write", file.toString(), "Sheet1", "C1", "C");
			execute("delete-column", file.toString(), "Sheet1", "2");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "B1"))
					.as("B1 after deleting column 2").isEqualTo("STR:C");
		}
	}

	@Nested
	@DisplayName("tab-color")
	final class TabColorTest
	{
		@Test
		@DisplayName("exits 0 on success and workbook remains readable")
		void exits0OnSuccessAndWorkbookRemainsReadable()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "data");

			var exitCode = execute("tab-color", file.toString(), "Sheet1", "70AD47");

			assertThat(exitCode).as("exit code").isZero();
			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1"))
					.as("data after color set").isEqualTo("STR:data");
		}

		@Test
		@DisplayName("exits 1 for invalid hex color")
		void exits1ForInvalidHexColor()
		{
			execute("create", file.toString());

			assertThat(execute("tab-color", file.toString(), "Sheet1", "ZZZZZZ"))
					.as("exit code for invalid color").isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("read-range --typed")
	final class ReadRangeTyped
	{
		@Test
		@DisplayName("prefixes values with type tokens when --typed is given")
		void prefixesValuesWithTypeTokensWhenTypedIsGiven()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "hello");
			execute("write", file.toString(), "Sheet1", "B1", "42");

			var output = captureOutput("read-range", file.toString(), "Sheet1", "A1", "B1", "--typed");

			assertThat(output).as("typed output").isEqualTo("STR:hello\tNUM:42.0");
		}
	}

	@Nested
	@DisplayName("read-range")
	final class ReadRange
	{
		@BeforeEach
		void createAndPopulateWorkbook()
		{
			execute("create", file.toString());
			execute("write", file.toString(), "Sheet1", "A1", "Name");
			execute("write", file.toString(), "Sheet1", "B1", "Score");
			execute("write", file.toString(), "Sheet1", "A2", "Alice");
			execute("write", file.toString(), "Sheet1", "B2", "95");
			execute("write", file.toString(), "Sheet1", "A3", "Bob");
			execute("write", file.toString(), "Sheet1", "B3", "87");
		}

		@Test
		@DisplayName("outputs TSV with correct values for the requested range")
		void outputsTsvWithCorrectValuesForRequestedRange()
		{
			var output = captureOutput("read-range", file.toString(), "Sheet1", "A1", "B3");
			var lines = output.lines().toList();

			assertThat(lines).as("line count").hasSize(3);
			assertThat(lines.get(0)).as("header row").isEqualTo("Name\tScore");
			assertThat(lines.get(1)).as("data row 1").isEqualTo("Alice\t95");
			assertThat(lines.get(2)).as("data row 2").isEqualTo("Bob\t87");
		}

		@Test
		@DisplayName("outputs empty field for a mid-row empty cell")
		void outputsEmptyFieldForMidRowEmptyCell()
		{
			execute("write", file.toString(), "Sheet1", "D1", "Extra");

			var output = captureOutput("read-range", file.toString(), "Sheet1", "A1", "D1");

			assertThat(output).as("empty C1 produces empty field").isEqualTo("Name\tScore\t\tExtra");
		}

		@Test
		@DisplayName("exits 0 on successful range read")
		void exits0OnSuccessfulRangeRead()
		{
			var exitCode = execute("read-range", file.toString(), "Sheet1", "A1", "B2");

			assertThat(exitCode).as("exit code").isZero();
		}

		@Test
		@DisplayName("exits 1 when file does not exist")
		void exits1WhenFileDoesNotExist(@TempDir final Path directory)
		{
			var missing = directory.resolve("missing.xlsx");
			var exitCode = execute("read-range", missing.toString(), "Sheet1", "A1", "B2");

			assertThat(exitCode).as("exit code for missing file").isEqualTo(1);
		}

		@Test
		@DisplayName("round-trips with write-range")
		void roundTripsWithWriteRange(@TempDir final Path directory)
		{
			var source = file;
			var destination = directory.resolve("dest.xlsx");
			execute("create", destination.toString());

			var tsv = captureOutput("read-range", source.toString(), "Sheet1", "A1", "B3");
			executeWithStdin(tsv, "write-range", destination.toString(), "Sheet1", "A1", "--overwrite");

			assertThat(captureOutput("read", destination.toString(), "Sheet1", "A1"))
					.as("A1 round-tripped").isEqualTo("STR:Name");
			assertThat(captureOutput("read", destination.toString(), "Sheet1", "B2"))
					.as("B2 round-tripped").isEqualTo("NUM:95.0");
		}
	}

	@Nested
	@DisplayName("write-range")
	final class WriteRange
	{
		@BeforeEach
		void createWorkbook()
		{
			execute("create", file.toString());
		}

		@Test
		@DisplayName("writes a TSV table and cells are readable at correct positions")
		void writesATsvTableAndCellsAreReadableAtCorrectPositions()
		{
			var tsv = "Name\tScore\nAlice\t95\nBob\t87";
			executeWithStdin(tsv, "write-range", file.toString(), "Sheet1", "B2");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "B2"))
					.as("header col 1").isEqualTo("STR:Name");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "C2"))
					.as("header col 2").isEqualTo("STR:Score");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "B3"))
					.as("row 1 col 1").isEqualTo("STR:Alice");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "C3"))
					.as("row 1 col 2").isEqualTo("NUM:95.0");
			assertThat(captureOutput("read", file.toString(), "Sheet1", "B4"))
					.as("row 2 col 1").isEqualTo("STR:Bob");
		}

		@Test
		@DisplayName("preserves existing non-empty cell when --overwrite is not given")
		void preservesExistingNonEmptyCellWhenOverwriteNotGiven()
		{
			execute("write", file.toString(), "Sheet1", "A1", "protected");
			executeWithStdin("replacement", "write-range", file.toString(), "Sheet1", "A1");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1"))
					.as("protected cell").isEqualTo("STR:protected");
		}

		@Test
		@DisplayName("overwrites existing cell when --overwrite is given")
		void overwritesExistingCellWhenOverwriteIsGiven()
		{
			execute("write", file.toString(), "Sheet1", "A1", "old");
			executeWithStdin("new value", "write-range", file.toString(), "Sheet1", "A1", "--overwrite");

			assertThat(captureOutput("read", file.toString(), "Sheet1", "A1"))
					.as("overwritten cell").isEqualTo("STR:new value");
		}

		@Test
		@DisplayName("exits 0 on successful range write")
		void exits0OnSuccessfulRangeWrite()
		{
			var exitCode = executeWithStdin("hello\tworld", "write-range", file.toString(), "Sheet1", "A1");

			assertThat(exitCode).as("exit code").isZero();
		}
	}
}