package de.gupta.xl.application.facade;

import de.gupta.aletheia.trials.Fallible;
import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.service.WorkbookService;
import de.gupta.xl.application.transfer.SheetSummary;
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.application.transfer.WriteRangeRequest;
import de.gupta.xl.domain.CellGrid;
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
    public Fallible<CellGrid> readRange(final Path file, final String sheet,
                                        final String fromCell, final String toCell)
    {
        return Fallible.<Void>beckon(null).map(_ -> workbookService.readRange(file, sheet, fromCell, toCell));
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
    public Fallible<Void> writeRange(final WriteRangeRequest request)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.writeRange(request);
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
    public Fallible<Void> renameSheet(final Path file, final String sheetName, final String newName)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.renameSheet(file, sheetName, newName);
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

    @Override
    public Fallible<Void> moveSheet(final Path file, final String sheetName, final int position)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.moveSheet(file, sheetName, position);
            return null;
        });
    }

    @Override
    public Fallible<Void> deleteColumn(final Path file, final String sheet, final String columnNotation)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.deleteColumn(file, sheet, columnNotation);
            return null;
        });
    }

    @Override
    public Fallible<Void> setTabColor(final Path file, final String sheet, final String hexRgb)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.setTabColor(file, sheet, hexRgb);
            return null;
        });
    }

    @Override
    public Fallible<List<CellValue>> readRow(final Path file, final String sheet, final String rowRef)
    {
        return Fallible.<Void>beckon(null).map(_ -> workbookService.readRow(file, sheet, rowRef));
    }

    @Override
    public Fallible<List<CellValue>> readColumn(final Path file, final String sheet, final String columnRef)
    {
        return Fallible.<Void>beckon(null).map(_ -> workbookService.readColumn(file, sheet, columnRef));
    }

    @Override
    public Fallible<CellValue> evaluateCell(final Path file, final String sheet, final String cellReference)
    {
        return Fallible.<Void>beckon(null).map(_ -> workbookService.evaluateCell(file, sheet, cellReference));
    }

    @Override
    public Fallible<Void> insertRow(final Path file, final String sheet, final String rowRef)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.insertRow(file, sheet, rowRef);
            return null;
        });
    }

    @Override
    public Fallible<Void> deleteRow(final Path file, final String sheet, final String rowRef)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.deleteRow(file, sheet, rowRef);
            return null;
        });
    }

    @Override
    public Fallible<Void> setColumnWidth(final Path file, final String sheet,
                                         final String columnRef, final int characterWidth)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.setColumnWidth(file, sheet, columnRef, characterWidth);
            return null;
        });
    }

    @Override
    public Fallible<Void> autoFitColumn(final Path file, final String sheet, final String columnRef)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.autoFitColumn(file, sheet, columnRef);
            return null;
        });
    }

    @Override
    public Fallible<Void> autoFitAllColumns(final Path file, final String sheet)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            workbookService.autoFitAllColumns(file, sheet);
            return null;
        });
    }

    @Override
    public Fallible<Void> importCsv(final Path xlFile, final String sheet, final Path csvFile,
                                    final String startCell, final boolean overwrite, final char delimiter)
    {
        return Fallible.<Void>beckon(null).map(_ ->
        {
            var grid = CsvImporter.toCellGrid(csvFile, delimiter);
            workbookService.writeRange(new WriteRangeRequest(xlFile, sheet, startCell, grid, overwrite));
            return null;
        });
    }
}