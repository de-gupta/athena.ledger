package de.gupta.xl.adapter.poi;

import de.gupta.xl.domain.CellReference;
import de.gupta.xl.domain.CellValue;
import de.gupta.xl.domain.exception.WorkbookNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PoiWorkbookRepository integration")
final class PoiWorkbookRepositoryITCase
{
	private final PoiWorkbookRepository repository = new PoiWorkbookRepository();

	@Nested
	@DisplayName("createWorkbook")
	final class CreateWorkbook
	{
		@Test
		@DisplayName("creates a workbook with a single Sheet1")
		void createsWorkbookWithSingleSheet1(@TempDir final Path directory)
		{
			var file = directory.resolve("new.xlsx");

			repository.createWorkbook(file);

			assertThat(Files.exists(file)).as("file created").isTrue();
			var content = repository.load(file);
			assertThat(content.sheets()).as("sheets")
			                            .extracting("name")
			                            .containsExactly("Sheet1");
		}
	}

	@Nested
	@DisplayName("readCell and writeCell round-trip")
	final class ReadWriteRoundTrip
	{
		private Path file;

		@BeforeEach
		void createWorkbook(@TempDir final Path directory)
		{
			file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("cellValueCases")
		@DisplayName("reads back the value written for each cell type")
		void readsBackValueWrittenForEachCellType(final String as, final CellValue written, final CellValue expected)
		{
			var reference = CellReference.of("A1");

			repository.writeCell(file, "Sheet1", reference, written);
			var result = repository.readCell(file, "Sheet1", reference);

			assertThat(result).as("round-trip for %s", as).isEqualTo(expected);
		}

		private static Stream<Arguments> cellValueCases()
		{
			return Stream.of(
					Arguments.of("string value", new CellValue.Str("hello"), new CellValue.Str("hello")),
					Arguments.of("numeric value", new CellValue.Num(42.5), new CellValue.Num(42.5)),
					Arguments.of("boolean true", new CellValue.Bool(true), new CellValue.Bool(true)),
					Arguments.of("boolean false", new CellValue.Bool(false), new CellValue.Bool(false)),
					Arguments.of("date value", new CellValue.Date(LocalDate.of(2026, 6, 7)),
							new CellValue.Date(LocalDate.of(2026, 6, 7))),
					Arguments.of("empty value", new CellValue.Empty(), new CellValue.Empty())
			);
		}
	}

	@Nested
	@DisplayName("sheet preservation")
	final class SheetPreservation
	{
		@Test
		@DisplayName("preserves existing sheet content after a write to a different cell")
		void preservesExistingSheetContentAfterWriteToADifferentCell(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("original"));

			repository.writeCell(file, "Sheet1", CellReference.of("B2"), new CellValue.Num(99.0));

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("original cell preserved")
					.isEqualTo(new CellValue.Str("original"));
		}

		@Test
		@DisplayName("preserves other sheets when adding a new sheet")
		void preservesOtherSheetsWhenAddingNewSheet(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("keep me"));

			repository.addSheet(file, "NewSheet");

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("original data preserved")
					.isEqualTo(new CellValue.Str("keep me"));
		}
	}

	@Nested
	@DisplayName("addSheet")
	final class AddSheet
	{
		@Test
		@DisplayName("adds a new sheet to an existing workbook")
		void addsNewSheetToExistingWorkbook(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			repository.addSheet(file, "Q2");

			var content = repository.load(file);
			assertThat(content.sheets()).as("sheets after add")
			                            .extracting("name")
			                            .containsExactly("Sheet1", "Q2");
		}
	}

	@Nested
	@DisplayName("deleteSheet")
	final class DeleteSheet
	{
		@Test
		@DisplayName("removes the named sheet from the workbook")
		void removesNamedSheetFromWorkbook(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.addSheet(file, "ToDelete");

			repository.deleteSheet(file, "ToDelete");

			var content = repository.load(file);
			assertThat(content.sheets()).as("sheets after delete")
			                            .extracting("name")
			                            .containsExactly("Sheet1");
		}
	}

	@Nested
	@DisplayName("copySheet")
	final class CopySheet
	{
		@Test
		@DisplayName("creates a copy of the sheet under the new name")
		void createsACopyOfTheSheetUnderTheNewName(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("template data"));

			repository.copySheet(file, "Sheet1", "Sheet1Copy");

			var content = repository.load(file);
			assertThat(content.sheets()).as("sheets after copy")
			                            .extracting("name")
			                            .contains("Sheet1", "Sheet1Copy");
		}
	}

	@Nested
	@DisplayName("atomic write")
	final class AtomicWrite
	{
		@Test
		@DisplayName("no temporary file remains after a successful write")
		void noTemporaryFileRemainsAfterSuccessfulWrite(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("data"));

			var temporaryFile = file.resolveSibling(file.getFileName() + ".temporary");
			assertThat(Files.exists(temporaryFile)).as("temporary file cleaned up").isFalse();
		}

		@Test
		@DisplayName("original file is intact when workbook does not exist")
		void originalFileIsIntactWhenWorkbookDoesNotExist(@TempDir final Path directory)
		{
			var file = directory.resolve("missing.xlsx");

			assertThatThrownBy(() -> repository.writeCell(
					file, "Sheet1", CellReference.of("A1"), new CellValue.Str("data")))
					.as("missing file error")
					.isInstanceOf(WorkbookNotFoundException.class);
			assertThat(Files.exists(file)).as("file not created on error").isFalse();
		}
	}
}