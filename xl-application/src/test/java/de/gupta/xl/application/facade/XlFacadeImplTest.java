package de.gupta.xl.application.facade;

import de.gupta.xl.application.service.WorkbookService;
import de.gupta.xl.application.transfer.SheetSummary;
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.domain.CellValue;
import de.gupta.xl.domain.exception.SheetNotFoundException;
import de.gupta.xl.domain.exception.WorkbookAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("XlFacadeImpl")
final class XlFacadeImplTest
{
	private final WorkbookService workbookService = mock(WorkbookService.class);
	private final XlFacadeImpl facade = new XlFacadeImpl(workbookService);

	@Nested
	@DisplayName("listSheets")
	final class ListSheets
	{
		@Test
		@DisplayName("returns Triumph wrapping the sheet summaries on success")
		void returnsTriumphWrappingSheetSummariesOnSuccess(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			var summaries = List.of(new SheetSummary("Sheet1", 5));
			when(workbookService.listSheets(file)).thenReturn(summaries);

			var result = facade.listSheets(file);

			List<SheetSummary> extracted = result.fold(value -> value, _ -> null);
			assertThat(extracted)
					.as("sheet summaries")
					.isEqualTo(summaries);
		}

		@Test
		@DisplayName("returns Fury wrapping the exception on failure")
		void returnsFuryWrappingExceptionOnFailure(@TempDir final Path directory)
		{
			var file = directory.resolve("missing.xlsx");
			var exception = SheetNotFoundException.forSheet("Sheet1");
			when(workbookService.listSheets(file)).thenThrow(exception);

			var result = facade.listSheets(file);

			Exception captured = result.fold(_ -> null, caught -> caught);
			assertThat(captured)
					.as("captured exception")
					.isSameAs(exception);
		}
	}

	@Nested
	@DisplayName("readCell")
	final class ReadCell
	{
		@Test
		@DisplayName("returns Triumph wrapping the cell value on success")
		void returnsTriumphWrappingCellValueOnSuccess(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			when(workbookService.readCell(file, "Sheet1", "A1"))
					.thenReturn(new CellValue.Str("hello"));

			var result = facade.readCell(file, "Sheet1", "A1");

			CellValue extracted = result.fold(value -> value, _ -> null);
			assertThat(extracted)
					.as("cell value")
					.isEqualTo(new CellValue.Str("hello"));
		}
	}

	@Nested
	@DisplayName("writeCell")
	final class WriteCell
	{
		@Test
		@DisplayName("returns success and delegates to service")
		void returnsSuccessAndDelegatesToService(@TempDir final Path directory)
		{
			var file = directory.resolve("workbook.xlsx");
			var request = new WriteCellRequest(file, "Sheet1", "B2", new CellValue.Num(42.0));

			var result = facade.writeCell(request);

			Boolean succeeded = result.fold(_ -> true, _ -> false);
			assertThat(succeeded).as("operation succeeded").isTrue();
			verify(workbookService).writeCell(request);
		}
	}

	@Nested
	@DisplayName("createWorkbook")
	final class CreateWorkbook
	{
		@Test
		@DisplayName("returns Fury when service throws")
		void returnsFuryWhenServiceThrows(@TempDir final Path directory)
		{
			var file = directory.resolve("existing.xlsx");
			var exception = WorkbookAlreadyExistsException.forFile(file);
			doThrow(exception).when(workbookService).createWorkbook(file, false);

			var result = facade.createWorkbook(file, false);

			Exception captured = result.fold(_ -> null, caught -> caught);
			assertThat(captured)
					.as("captured exception")
					.isSameAs(exception);
		}
	}
}