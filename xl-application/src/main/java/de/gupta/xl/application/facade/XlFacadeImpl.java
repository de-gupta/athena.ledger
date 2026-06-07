package de.gupta.xl.application.facade;

import de.gupta.aletheia.trials.Fallible;
import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.service.WorkbookService;
import de.gupta.xl.application.transfer.RangeStats;
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

	@Override
	public Fallible<String> findColumn(final Path file, final String sheet, final String header)
	{
		return Fallible.<Void>beckon(null).map(_ -> workbookService.findColumn(file, sheet, header));
	}

	@Override
	public Fallible<Void> insertColumn(final Path file, final String sheet,
	                                   final String columnRef, final List<CellValue> values)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			workbookService.insertColumn(file, sheet, columnRef, values);
			return null;
		});
	}

	@Override
	public Fallible<Void> appendColumn(final Path file, final String sheet, final List<CellValue> values)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			workbookService.appendColumn(file, sheet, values);
			return null;
		});
	}

	@Override
	public Fallible<RangeStats> rangeStats(final Path file, final String sheet,
	                                       final String fromCell, final String toCell)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			var grid = workbookService.readRange(file, sheet, fromCell, toCell);
			var allValues = grid.rows().stream().flatMap(List::stream).toList();
			var numericValues = allValues.stream()
			                             .filter(CellValue.Num.class::isInstance)
			                             .mapToDouble(v -> ((CellValue.Num) v).value())
			                             .toArray();
			var total = (long) allValues.size();
			var numericCount = (long) numericValues.length;
			if (numericCount == 0)
			{
				return new RangeStats(total, 0L, total, null, null, null, null);
			}
			var min = java.util.Arrays.stream(numericValues).min().orElseThrow();
			var max = java.util.Arrays.stream(numericValues).max().orElseThrow();
			var mean = java.util.Arrays.stream(numericValues).average().orElseThrow();
			var variance = java.util.Arrays.stream(numericValues)
			                               .map(v -> (v - mean) * (v - mean))
			                               .average().orElseThrow();
			return new RangeStats(total, numericCount, total - numericCount,
					min, max, mean, Math.sqrt(variance));
		});
	}


	@Override
	public Fallible<Integer> findRow(final Path file, final String sheet,
	                                 final String columnRef, final String value)
	{
		return Fallible.<Void>beckon(null).map(_ -> workbookService.findRow(file, sheet, columnRef, value));
	}

	@Override
	public Fallible<String> dims(final Path file, final String sheet)
	{
		return Fallible.<Void>beckon(null).map(_ -> workbookService.dims(file, sheet));
	}

	@Override
	public Fallible<Void> exportCsv(final Path xlFile, final String sheet,
	                                final Path csvFile, final char delimiter)
	{
		return Fallible.<Void>beckon(null).map(_ ->
		{
			var dimsString = workbookService.dims(xlFile, sheet);
			var parts = dimsString.split(":");
			var grid = workbookService.readRange(xlFile, sheet, parts[0], parts[1]);
			CsvExporter.write(grid, csvFile, delimiter);
			return null;
		});
	}

}