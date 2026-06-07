package de.gupta.xl.application.facade;

import de.gupta.aletheia.trials.Fallible;
import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.service.WorkbookService;
import de.gupta.xl.application.transfer.SheetSummary;
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.domain.CellValue;

import java.nio.file.Path;
import java.util.List;

public final class XlFacadeImpl implements XlFacade
{
	private final WorkbookService workbookService;

	public XlFacadeImpl(final WorkbookService workbookService)
	{
		this.workbookService = workbookService;
	}

	@Override
	public Fallible<List<SheetSummary>> listSheets(final Path file)
	{
		return Fallible.<Void>beckon(null).map(_ -> workbookService.listSheets(file));
	}

	@Override
	public Fallible<CellValue> readCell(final Path file, final String sheet, final String cellReference)
	{
		return Fallible.<Void>beckon(null).map(_ -> workbookService.readCell(file, sheet, cellReference));
	}

	@Override
	public Fallible<Void> writeCell(final WriteCellRequest request)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			workbookService.writeCell(request);
			return null;
		});
	}

	@Override
	public Fallible<Void> createWorkbook(final Path file, final boolean overwrite)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			workbookService.createWorkbook(file, overwrite);
			return null;
		});
	}

	@Override
	public Fallible<Void> addSheet(final Path file, final String sheetName)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			workbookService.addSheet(file, sheetName);
			return null;
		});
	}

	@Override
	public Fallible<Void> deleteSheet(final Path file, final String sheetName)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			workbookService.deleteSheet(file, sheetName);
			return null;
		});
	}

	@Override
	public Fallible<Void> copySheet(final Path file, final String sourceSheet, final String targetName)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			workbookService.copySheet(file, sourceSheet, targetName);
			return null;
		});
	}
}