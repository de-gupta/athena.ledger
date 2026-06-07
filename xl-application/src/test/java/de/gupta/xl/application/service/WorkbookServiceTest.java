package de.gupta.xl.application.service;

import de.gupta.xl.application.port.out.WorkbookRepository;
import de.gupta.xl.domain.CellValue;
import de.gupta.xl.domain.Sheet;
import de.gupta.xl.domain.WorkbookContent;
import de.gupta.xl.domain.exception.LastSheetException;
import de.gupta.xl.domain.exception.SheetAlreadyExistsException;
import de.gupta.xl.domain.exception.SheetNotFoundException;
import de.gupta.xl.domain.exception.WorkbookAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("WorkbookService")
final class WorkbookServiceTest
{
	private final WorkbookRepository repository = mock(WorkbookRepository.class);
	private final WorkbookService service = new WorkbookService(repository);

	private static WorkbookContent workbookWith(final Path file, final String... sheetNames)
	{
		var sheets = java.util.Arrays.stream(sheetNames)
		                             .map(name -> new Sheet(name, 0))
		                             .toList();
		return new WorkbookContent(file, sheets);
	}

	@Nested
	@DisplayName("listSheets")
	final class ListSheets
	{
		@Test
		@DisplayName("maps loaded sheets to summaries")
		void mapsLoadedSheetsToSummaries(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			var sheets = List.of(new Sheet("Alpha", 10), new Sheet("Beta", 5));
			when(repository.load(file)).thenReturn(new WorkbookContent(file, sheets));

			var result = service.listSheets(file);

			assertThat(result).as("sheet summaries")
			                  .extracting("name")
			                  .containsExactly("Alpha", "Beta");
			assertThat(result).as("sheet row counts")
			                  .extracting("rowCount")
			                  .containsExactly(10, 5);
		}
	}

	@Nested
	@DisplayName("createWorkbook")
	final class CreateWorkbook
	{
		@Test
		@DisplayName("delegates to repository when overwrite is true and file exists")
		void delegatesToRepositoryWhenOverwriteIsTrueAndFileExists(@TempDir final Path directory) throws Exception
		{
			var file = directory.resolve("existing.xlsx");
			file.toFile().createNewFile();

			service.createWorkbook(file, true);

			verify(repository).createWorkbook(file);
		}

		@Test
		@DisplayName("throws WorkbookAlreadyExistsException when file exists and overwrite is false")
		void throwsWhenFileExistsAndOverwriteIsFalse(@TempDir final Path directory) throws Exception
		{
			var file = directory.resolve("existing.xlsx");
			file.toFile().createNewFile();

			assertThatThrownBy(() -> service.createWorkbook(file, false))
					.as("overwrite guard")
					.isInstanceOf(WorkbookAlreadyExistsException.class)
					.hasMessageContaining("existing.xlsx");
		}

		@Test
		@DisplayName("delegates to repository when file does not exist")
		void delegatesToRepositoryWhenFileDoesNotExist(@TempDir final Path directory)
		{
			var file = directory.resolve("new.xlsx");

			service.createWorkbook(file, false);

			verify(repository).createWorkbook(file);
		}
	}

	@Nested
	@DisplayName("addSheet")
	final class AddSheet
	{
		@Test
		@DisplayName("throws SheetAlreadyExistsException when sheet name already exists")
		void throwsWhenSheetNameAlreadyExists(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.load(file)).thenReturn(workbookWith(file, "Existing"));

			assertThatThrownBy(() -> service.addSheet(file, "Existing"))
					.as("duplicate sheet guard")
					.isInstanceOf(SheetAlreadyExistsException.class)
					.hasMessageContaining("Existing");
		}

		@Test
		@DisplayName("delegates to repository when sheet name is new")
		void delegatesToRepositoryWhenSheetNameIsNew(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

			service.addSheet(file, "NewSheet");

			verify(repository).addSheet(file, "NewSheet");
		}
	}

	@Nested
	@DisplayName("deleteSheet")
	final class DeleteSheet
	{
		@Test
		@DisplayName("throws SheetNotFoundException when sheet does not exist")
		void throwsWhenSheetDoesNotExist(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

			assertThatThrownBy(() -> service.deleteSheet(file, "Missing"))
					.as("sheet not found guard")
					.isInstanceOf(SheetNotFoundException.class)
					.hasMessageContaining("Missing");
		}

		@Test
		@DisplayName("throws LastSheetException when workbook has only one sheet")
		void throwsWhenWorkbookHasOnlyOneSheet(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.load(file)).thenReturn(workbookWith(file, "OnlySheet"));

			assertThatThrownBy(() -> service.deleteSheet(file, "OnlySheet"))
					.as("last sheet guard")
					.isInstanceOf(LastSheetException.class);
		}

		@Test
		@DisplayName("delegates to repository when sheet exists and is not the last")
		void delegatesToRepositoryWhenSheetExistsAndIsNotLast(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1", "Sheet2"));

			service.deleteSheet(file, "Sheet2");

			verify(repository).deleteSheet(file, "Sheet2");
		}
	}

	@Nested
	@DisplayName("copySheet")
	final class CopySheet
	{
		@Test
		@DisplayName("throws SheetNotFoundException when source sheet does not exist")
		void throwsWhenSourceSheetDoesNotExist(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

			assertThatThrownBy(() -> service.copySheet(file, "Missing", "NewSheet"))
					.as("source not found guard")
					.isInstanceOf(SheetNotFoundException.class)
					.hasMessageContaining("Missing");
		}

		@Test
		@DisplayName("throws SheetAlreadyExistsException when target name is already taken")
		void throwsWhenTargetNameIsAlreadyTaken(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1", "Target"));

			assertThatThrownBy(() -> service.copySheet(file, "Sheet1", "Target"))
					.as("target exists guard")
					.isInstanceOf(SheetAlreadyExistsException.class)
					.hasMessageContaining("Target");
		}

		@Test
		@DisplayName("delegates to repository when source exists and target name is free")
		void delegatesToRepositoryWhenSourceExistsAndTargetIsFree(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

			service.copySheet(file, "Sheet1", "Sheet1Copy");

			verify(repository).copySheet(file, "Sheet1", "Sheet1Copy");
		}
	}

	@Nested
	@DisplayName("readCell")
	final class ReadCell
	{
		@Test
		@DisplayName("delegates to repository with parsed cell reference")
		void delegatesToRepositoryWithParsedCellReference(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(repository.readCell(eq(file), eq("Sheet1"), any()))
					.thenReturn(new CellValue.Str("hello"));

			var result = service.readCell(file, "Sheet1", "A1");

			assertThat(result).as("cell value").isEqualTo(new CellValue.Str("hello"));
		}
	}

    @Nested
    @DisplayName("readRange")
    final class ReadRange
    {
        @Test
        @DisplayName("delegates to repository with parsed range reference")
        void delegatesToRepositoryWithParsedRangeReference(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            var grid = de.gupta.xl.domain.CellGrid.of(List.of(
                    List.of(new CellValue.Str("A"), new CellValue.Str("B"))
            ));
            when(repository.readRange(eq(file), eq("Sheet1"), any())).thenReturn(grid);

            var result = service.readRange(file, "Sheet1", "A1", "B1");

            assertThat(result).as("returned grid").isEqualTo(grid);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when from cell notation is invalid")
        void throwsWhenFromCellNotationIsInvalid(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");

            assertThatThrownBy(() -> service.readRange(file, "Sheet1", "INVALID", "B2"))
                    .as("invalid from cell")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when range is inverted")
        void throwsWhenRangeIsInverted(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");

            assertThatThrownBy(() -> service.readRange(file, "Sheet1", "B2", "A1"))
                    .as("inverted range")
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("writeRange")
    final class WriteRange
    {
        @Test
        @DisplayName("delegates to repository with parsed start reference and grid")
        void delegatesToRepositoryWithParsedStartReferenceAndGrid(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            List<List<CellValue>> rows = List.of(
                    List.of(new CellValue.Str("A"), new CellValue.Num(1.0)),
                    List.of(new CellValue.Str("B"), new CellValue.Num(2.0))
            );
            var grid = de.gupta.xl.domain.CellGrid.of(rows);
            var request = new de.gupta.xl.application.transfer.WriteRangeRequest(
                    file, "Sheet1", "B2", grid, true);

            service.writeRange(request);

            verify(repository).writeRange(eq(file), eq("Sheet1"), any(), eq(grid), eq(true));
        }

        @Test
        @DisplayName("does nothing when grid is empty")
        void doesNothingWhenGridIsEmpty(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            var request = new de.gupta.xl.application.transfer.WriteRangeRequest(
                    file, "Sheet1", "A1", de.gupta.xl.domain.CellGrid.empty(), false);

            service.writeRange(request);

            verify(repository, never()).writeRange(any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when start cell is invalid")
        void throwsWhenStartCellIsInvalid(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            var grid = de.gupta.xl.domain.CellGrid.of(
                    List.of(List.of(new CellValue.Str("x"))));
            var request = new de.gupta.xl.application.transfer.WriteRangeRequest(
                    file, "Sheet1", "INVALID", grid, false);

            assertThatThrownBy(() -> service.writeRange(request))
                    .as("invalid start cell")
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("renameSheet")
    final class RenameSheet
    {
        @Test
        @DisplayName("throws SheetNotFoundException when sheet does not exist")
        void throwsWhenSheetDoesNotExist(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

            assertThatThrownBy(() -> service.renameSheet(file, "Missing", "NewName"))
                    .as("not found guard")
                    .isInstanceOf(de.gupta.xl.domain.exception.SheetNotFoundException.class);
        }

        @Test
        @DisplayName("throws SheetAlreadyExistsException when new name is already taken")
        void throwsWhenNewNameIsAlreadyTaken(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1", "Sheet2"));

            assertThatThrownBy(() -> service.renameSheet(file, "Sheet1", "Sheet2"))
                    .as("duplicate name guard")
                    .isInstanceOf(de.gupta.xl.domain.exception.SheetAlreadyExistsException.class);
        }

        @Test
        @DisplayName("delegates to repository when name is valid")
        void delegatesToRepositoryWhenNameIsValid(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

            service.renameSheet(file, "Sheet1", "Renamed");

            verify(repository).renameSheet(file, "Sheet1", "Renamed");
        }
    }

    @Nested
    @DisplayName("moveSheet")
    final class MoveSheet
    {
        @Test
        @DisplayName("throws SheetNotFoundException when sheet does not exist")
        void throwsWhenSheetDoesNotExist(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

            assertThatThrownBy(() -> service.moveSheet(file, "Missing", 0))
                    .as("not found guard")
                    .isInstanceOf(de.gupta.xl.domain.exception.SheetNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when position is out of range")
        void throwsWhenPositionIsOutOfRange(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1", "Sheet2"));

            assertThatThrownBy(() -> service.moveSheet(file, "Sheet1", 5))
                    .as("out of range guard")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("delegates to repository when position is valid")
        void delegatesToRepositoryWhenPositionIsValid(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1", "Sheet2"));

            service.moveSheet(file, "Sheet2", 0);

            verify(repository).moveSheet(file, "Sheet2", 0);
        }
    }

    @Nested
    @DisplayName("deleteColumn")
    final class DeleteColumn
    {
        @Test
        @DisplayName("throws SheetNotFoundException when sheet does not exist")
        void throwsWhenSheetDoesNotExist(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

            assertThatThrownBy(() -> service.deleteColumn(file, "Missing", "A"))
                    .as("not found guard")
                    .isInstanceOf(de.gupta.xl.domain.exception.SheetNotFoundException.class);
        }

        @Test
        @DisplayName("delegates with 0-based column index for letter notation")
        void delegatesWithCorrectColumnIndexForLetterNotation(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

            service.deleteColumn(file, "Sheet1", "C");

            verify(repository).deleteColumn(file, "Sheet1", 2);
        }

        @Test
        @DisplayName("delegates with 0-based column index for integer notation")
        void delegatesWithCorrectColumnIndexForIntegerNotation(@TempDir final Path directory)
        {
            var file = directory.resolve("workbook.xlsx");
            when(repository.load(file)).thenReturn(workbookWith(file, "Sheet1"));

            service.deleteColumn(file, "Sheet1", "3");

            verify(repository).deleteColumn(file, "Sheet1", 2);
        }
    }
}