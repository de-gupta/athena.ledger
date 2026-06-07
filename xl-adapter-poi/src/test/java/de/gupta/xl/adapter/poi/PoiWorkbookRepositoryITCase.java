package de.gupta.xl.adapter.poi;

import de.gupta.xl.domain.CellGrid;
import de.gupta.xl.domain.CellRangeReference;
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
import java.util.List;
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

	@Nested
	@DisplayName("renameSheet")
	final class RenameSheet
	{
		@Test
		@DisplayName("sheet appears under its new name after rename")
		void sheetAppearsUnderNewNameAfterRename(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			repository.renameSheet(file, "Sheet1", "RenamedSheet");

			var content = repository.load(file);
			assertThat(content.sheets()).extracting("name")
			                            .as("sheet names after rename")
			                            .containsExactly("RenamedSheet");
		}
	}

	@Nested
	@DisplayName("moveSheet")
	final class MoveSheet
	{
		@Test
		@DisplayName("sheet appears at the requested position after move")
		void sheetAppearsAtRequestedPositionAfterMove(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.addSheet(file, "Second");
			repository.addSheet(file, "Third");

			repository.moveSheet(file, "Third", 0);

			var content = repository.load(file);
			assertThat(content.sheets().getFirst().name()).as("first sheet after move").isEqualTo("Third");
		}
	}

	@Nested
	@DisplayName("deleteColumn")
	final class DeleteColumn
	{
		@Test
		@DisplayName("column data shifts left after deletion")
		void columnDataShiftsLeftAfterDeletion(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("keep-A"));
			repository.writeCell(file, "Sheet1", CellReference.of("B1"), new CellValue.Str("delete-B"));
			repository.writeCell(file, "Sheet1", CellReference.of("C1"), new CellValue.Str("keep-C"));

			repository.deleteColumn(file, "Sheet1", 1);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("A unchanged").isEqualTo(new CellValue.Str("keep-A"));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("B1")))
					.as("B now has former C").isEqualTo(new CellValue.Str("keep-C"));
		}
	}

	@Nested
	@DisplayName("setTabColor")
	final class SetTabColor
	{
		@Test
		@DisplayName("tab color can be set without corrupting the workbook")
		void tabColorCanBeSetWithoutCorruptingWorkbook(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("data"));

			repository.setTabColor(file, "Sheet1", de.gupta.xl.domain.TabColor.of("70AD47"));

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("cell preserved after tab color change")
					.isEqualTo(new CellValue.Str("data"));
		}
	}

	@Nested
	@DisplayName("readRange")
	final class ReadRange
	{
		@Test
		@DisplayName("reads all written cell values in their correct positions")
		void readsAllWrittenCellValuesInCorrectPositions(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("B2"), new CellValue.Str("hello"));
			repository.writeCell(file, "Sheet1", CellReference.of("C2"), new CellValue.Num(42.0));
			repository.writeCell(file, "Sheet1", CellReference.of("B3"), new CellValue.Bool(true));

			var grid = repository.readRange(file, "Sheet1", CellRangeReference.of("B2", "C3"));

			assertThat(grid.rowCount()).as("row count").isEqualTo(2);
			assertThat(grid.columnCount()).as("column count").isEqualTo(2);
			assertThat(grid.rows().get(0).get(0)).as("B2").isEqualTo(new CellValue.Str("hello"));
			assertThat(grid.rows().get(0).get(1)).as("C2").isEqualTo(new CellValue.Num(42.0));
			assertThat(grid.rows().get(1).get(0)).as("B3").isEqualTo(new CellValue.Bool(true));
			assertThat(grid.rows().get(1).get(1)).as("C3 empty").isInstanceOf(CellValue.Empty.class);
		}

		@Test
		@DisplayName("returns Empty for every cell when sheet does not exist")
		void returnsEmptyForEveryCellWhenSheetDoesNotExist(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			var grid = repository.readRange(file, "NoSuchSheet", CellRangeReference.of("A1", "B2"));

			assertThat(grid.rows().stream().flatMap(List::stream))
					.as("all cells empty")
					.allMatch(CellValue.Empty.class::isInstance);
		}

		@Test
		@DisplayName("single-cell range returns one row with one value")
		void singleCellRangeReturnsOneRowWithOneValue(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("single"));

			var grid = repository.readRange(file, "Sheet1", CellRangeReference.of("A1", "A1"));

			assertThat(grid.rowCount()).as("row count").isEqualTo(1);
			assertThat(grid.rows().getFirst().getFirst()).as("A1").isEqualTo(new CellValue.Str("single"));
		}
	}

	@Nested
	@DisplayName("writeRange")
	final class WriteRange
	{
		@Test
		@DisplayName("writes all cells in the correct positions")
		void writesAllCellsInCorrectPositions(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			List<List<CellValue>> rows = List.of(
					List.of(new CellValue.Str("Name"), new CellValue.Num(1.0)),
					List.of(new CellValue.Str("Age"), new CellValue.Num(2.0))
			);

			repository.writeRange(file, "Sheet1", CellReference.of("B2"), CellGrid.of(rows), true);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("B2")))
					.as("B2").isEqualTo(new CellValue.Str("Name"));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("C2")))
					.as("C2").isEqualTo(new CellValue.Num(1.0));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("B3")))
					.as("B3").isEqualTo(new CellValue.Str("Age"));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("C3")))
					.as("C3").isEqualTo(new CellValue.Num(2.0));
		}

		@Test
		@DisplayName("preserves existing non-empty cells when overwrite is false")
		void preservesExistingNonEmptyCellsWhenOverwriteIsFalse(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("protected"));
			var grid = CellGrid.of(List.of(List.of(new CellValue.Str("replacement"))));

			repository.writeRange(file, "Sheet1", CellReference.of("A1"), grid, false);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("protected cell unchanged")
					.isEqualTo(new CellValue.Str("protected"));
		}

		@Test
		@DisplayName("overwrites existing content when overwrite is true")
		void overwritesExistingContentWhenOverwriteIsTrue(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("old"));
			var grid = CellGrid.of(List.of(List.of(new CellValue.Str("new"))));

			repository.writeRange(file, "Sheet1", CellReference.of("A1"), grid, true);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("cell overwritten")
					.isEqualTo(new CellValue.Str("new"));
		}

		@Test
		@DisplayName("creates the sheet when it does not exist")
		void createsSheetWhenItDoesNotExist(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			var grid = CellGrid.of(List.of(List.of(new CellValue.Num(42.0))));

			repository.writeRange(file, "NewSheet", CellReference.of("A1"), grid, true);

			assertThat(repository.readCell(file, "NewSheet", CellReference.of("A1")))
					.as("cell in new sheet")
					.isEqualTo(new CellValue.Num(42.0));
		}
	}
}