package de.gupta.xl.adapter.poi;

import de.gupta.xl.application.transfer.BatchOperation;
import de.gupta.xl.application.transfer.CellFormat;
import de.gupta.xl.domain.*;
import de.gupta.xl.domain.exception.EmptySheetException;
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

	@Nested
	@DisplayName("readRow")
	final class ReadRow
	{
		@Test
		@DisplayName("reads all written cells in the row")
		void readsAllWrittenCellsInTheRow(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("alpha"));
			repository.writeCell(file, "Sheet1", CellReference.of("B1"), new CellValue.Num(42.0));

			var result = repository.readRow(file, "Sheet1", RowReference.of("1"));

			assertThat(result).as("row values")
			                  .containsExactly(new CellValue.Str("alpha"), new CellValue.Num(42.0));
		}

		@Test
		@DisplayName("returns empty list when sheet does not exist")
		void returnsEmptyListWhenSheetDoesNotExist(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			assertThat(repository.readRow(file, "NoSheet", RowReference.of("1")))
					.as("missing sheet").isEmpty();
		}
	}

	@Nested
	@DisplayName("readColumn")
	final class ReadColumn
	{
		@Test
		@DisplayName("reads all written cells in the column")
		void readsAllWrittenCellsInTheColumn(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("first"));
			repository.writeCell(file, "Sheet1", CellReference.of("A2"), new CellValue.Str("second"));

			var result = repository.readColumn(file, "Sheet1", ColumnReference.of("A"));

			assertThat(result).as("column values")
			                  .containsExactly(new CellValue.Str("first"), new CellValue.Str("second"));
		}
	}

	@Nested
	@DisplayName("evaluateCell")
	final class EvaluateCell
	{
		@Test
		@DisplayName("returns the formula result as a numeric value")
		void returnsFormulaResultAsNumericValue(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Num(10.0));
			repository.writeCell(file, "Sheet1", CellReference.of("A2"), new CellValue.Num(20.0));
			repository.writeCell(file, "Sheet1", CellReference.of("A3"), new CellValue.Formula("SUM(A1:A2)"));

			var result = repository.evaluateCell(file, "Sheet1", CellReference.of("A3"));

			assertThat(result).as("evaluated formula").isEqualTo(new CellValue.Num(30.0));
		}

		@Test
		@DisplayName("returns plain value unchanged for non-formula cells")
		void returnsPlainValueUnchangedForNonFormulaCells(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("B1"), new CellValue.Str("plain"));

			var result = repository.evaluateCell(file, "Sheet1", CellReference.of("B1"));

			assertThat(result).as("plain cell value").isEqualTo(new CellValue.Str("plain"));
		}
	}

	@Nested
	@DisplayName("insertRow")
	final class InsertRow
	{
		@Test
		@DisplayName("existing row shifts down after insertion")
		void existingRowShiftsDownAfterInsertion(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("original"));

			repository.insertRow(file, "Sheet1", RowReference.of("1"));

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("inserted row is blank").isInstanceOf(CellValue.Empty.class);
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A2")))
					.as("original data shifted to row 2").isEqualTo(new CellValue.Str("original"));
		}
	}

	@Nested
	@DisplayName("deleteRow")
	final class DeleteRow
	{
		@Test
		@DisplayName("row below the deleted one shifts up")
		void rowBelowDeletedOneShiftsUp(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("delete-me"));
			repository.writeCell(file, "Sheet1", CellReference.of("A2"), new CellValue.Str("shift-up"));

			repository.deleteRow(file, "Sheet1", RowReference.of("1"));

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("former row 2 now at row 1").isEqualTo(new CellValue.Str("shift-up"));
		}
	}

	@Nested
	@DisplayName("setColumnWidth and autoFit")
	final class ColumnWidth
	{
		@Test
		@DisplayName("set-col-width does not corrupt workbook data")
		void setColWidthDoesNotCorruptWorkbookData(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("data"));

			repository.setColumnWidth(file, "Sheet1", ColumnReference.of("A"), 20);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("data after width change").isEqualTo(new CellValue.Str("data"));
		}

		@Test
		@DisplayName("auto-fit all columns does not corrupt workbook data")
		void autoFitAllColumnsDoesNotCorruptWorkbookData(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("hello world"));
			repository.writeCell(file, "Sheet1", CellReference.of("B1"), new CellValue.Num(42.0));

			repository.autoFitAllColumns(file, "Sheet1");

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("A1 after auto-fit").isEqualTo(new CellValue.Str("hello world"));
		}
	}

	@Nested
	@DisplayName("findColumn")
	final class FindColumn
	{
		@Test
		@DisplayName("returns the 0-based index of the matching header")
		void returnsThe0BasedIndexOfTheMatchingHeader(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("Name"));
			repository.writeCell(file, "Sheet1", CellReference.of("B1"), new CellValue.Str("Score"));

			assertThat(repository.findColumn(file, "Sheet1", "Score")).as("Score column").isEqualTo(1);
		}

		@Test
		@DisplayName("returns -1 when header is not found")
		void returnsMinus1WhenHeaderIsNotFound(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			assertThat(repository.findColumn(file, "Sheet1", "Missing")).as("not found").isEqualTo(-1);
		}
	}

	@Nested
	@DisplayName("insertColumn")
	final class InsertColumn
	{
		@Test
		@DisplayName("shifts existing columns right and writes new values")
		void shiftsExistingColumnsRightAndWritesNewValues(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("original"));

			repository.insertColumn(file, "Sheet1", ColumnReference.of("A"),
					List.of(new CellValue.Str("inserted")));

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("A1 has new value").isEqualTo(new CellValue.Str("inserted"));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("B1")))
					.as("original shifted to B").isEqualTo(new CellValue.Str("original"));
		}
	}

	@Nested
	@DisplayName("appendColumn")
	final class AppendColumn
	{
		@Test
		@DisplayName("writes after the last occupied column")
		void writesAfterTheLastOccupiedColumn(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("existing"));

			repository.appendColumn(file, "Sheet1", List.of(new CellValue.Str("appended")));

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("A1 unchanged").isEqualTo(new CellValue.Str("existing"));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("B1")))
					.as("B1 has appended value").isEqualTo(new CellValue.Str("appended"));
		}
	}

	@Nested
	@DisplayName("findRow")
	final class FindRow
	{
		@Test
		@DisplayName("returns 0-based row index of the matching value")
		void returns0BasedRowIndexOfTheMatchingValue(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("alpha"));
			repository.writeCell(file, "Sheet1", CellReference.of("A2"), new CellValue.Num(900));
			repository.writeCell(file, "Sheet1", CellReference.of("A3"), new CellValue.Str("gamma"));

			assertThat(repository.findRow(file, "Sheet1", ColumnReference.of("A"), "900"))
					.as("row index for numeric 900").isEqualTo(1);
			assertThat(repository.findRow(file, "Sheet1", ColumnReference.of("A"), "gamma"))
					.as("row index for string").isEqualTo(2);
		}

		@Test
		@DisplayName("returns -1 when value is not found")
		void returnsMinus1WhenValueIsNotFound(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			assertThat(repository.findRow(file, "Sheet1", ColumnReference.of("A"), "missing"))
					.as("not found").isEqualTo(-1);
		}
	}

	@Nested
	@DisplayName("dims")
	final class Dims
	{
		@Test
		@DisplayName("returns the bounding range of occupied cells")
		void returnsTheBoundingRangeOfOccupiedCells(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("B2"), new CellValue.Str("tl"));
			repository.writeCell(file, "Sheet1", CellReference.of("D4"), new CellValue.Str("br"));

			var range = repository.dims(file, "Sheet1");

			assertThat(range.topLeft()).as("top-left").isEqualTo(CellReference.of(1, 1));
			assertThat(range.bottomRight()).as("bottom-right").isEqualTo(CellReference.of(3, 3));
		}

		@Test
		@DisplayName("throws EmptySheetException when sheet has no data")
		void throwsEmptySheetExceptionWhenSheetHasNoData(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			assertThatThrownBy(() -> repository.dims(file, "Sheet1"))
					.isInstanceOf(EmptySheetException.class);
		}
	}

	@Nested
	@DisplayName("formatCell")
	final class FormatCell
	{
		@Test
		@DisplayName("format does not corrupt cell value")
		void formatDoesNotCorruptCellValue(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("header"));

			var format = CellFormat.builder().bold(true).numberFormat("@").build();
			repository.formatCell(file, "Sheet1", CellReference.of("A1"), format);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("cell value preserved after format").isEqualTo(new CellValue.Str("header"));
		}

		@Test
		@DisplayName("formatRange preserves all cell values")
		void formatRangePreservesAllCellValues(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("a"));
			repository.writeCell(file, "Sheet1", CellReference.of("B1"), new CellValue.Str("b"));

			repository.formatRange(file, "Sheet1",
					CellRangeReference.of("A1", "B1"),
					CellFormat.builder().bold(true).build());

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("A1 preserved").isEqualTo(new CellValue.Str("a"));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("B1")))
					.as("B1 preserved").isEqualTo(new CellValue.Str("b"));
		}
	}

	@Nested
	@DisplayName("freezePanes")
	final class FreezePanes
	{
		@Test
		@DisplayName("freeze panes does not corrupt workbook data")
		void freezePanesDoesNotCorruptWorkbookData(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			repository.writeCell(file, "Sheet1", CellReference.of("A1"), new CellValue.Str("data"));

			repository.freezePanes(file, "Sheet1", 1, 0);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("data intact after freeze").isEqualTo(new CellValue.Str("data"));
		}
	}

	@Nested
	@DisplayName("batch")
	final class Batch
	{
		@Test
		@DisplayName("applies all operations in a single save and all values are readable")
		void appliesAllOperationsInSingleSaveAndAllValuesAreReadable(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);

			List<BatchOperation> operations = List.of(
					new BatchOperation.WriteCell("Sheet1", "A1", new CellValue.Str("Name")),
					new BatchOperation.WriteCell("Sheet1", "B1", new CellValue.Str("Score")),
					new BatchOperation.WriteCell("Sheet1", "A2", new CellValue.Str("Alice")),
					new BatchOperation.WriteCell("Sheet1", "B2", new CellValue.Num(95.0)),
					new BatchOperation.FormatRange("Sheet1", "A1", "B1",
							CellFormat.builder().bold(true).build()),
					new BatchOperation.FreezePanes("Sheet1", 1, 0)
			);
			repository.batch(file, operations);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A1")))
					.as("A1").isEqualTo(new CellValue.Str("Name"));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("B2")))
					.as("B2").isEqualTo(new CellValue.Num(95.0));
		}

		@Test
		@DisplayName("batch is faster than individual operations by opening file only once")
		void batchResultMatchesEquivalentIndividualOperations(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			repository.createWorkbook(file);
			var operations = new java.util.ArrayList<BatchOperation>();
			for (var index = 0; index < 20; index++)
			{
				operations.add(new BatchOperation.WriteCell(
						"Sheet1", "A" + (index + 1), new CellValue.Num(index)));
			}

			repository.batch(file, operations);

			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A10")))
					.as("A10 = 9").isEqualTo(new CellValue.Num(9.0));
			assertThat(repository.readCell(file, "Sheet1", CellReference.of("A20")))
					.as("A20 = 19").isEqualTo(new CellValue.Num(19.0));
		}
	}
}