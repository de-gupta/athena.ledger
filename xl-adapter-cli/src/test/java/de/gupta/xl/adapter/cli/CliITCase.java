package de.gupta.xl.adapter.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
}