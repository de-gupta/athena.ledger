package de.gupta.xl.application.port.in;

import de.gupta.aletheia.trials.Fallible;
import de.gupta.xl.application.transfer.RangeStats;
import de.gupta.xl.application.transfer.SheetSummary;
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.application.transfer.WriteRangeRequest;
import de.gupta.xl.domain.CellGrid;
import de.gupta.xl.domain.CellValue;

import java.nio.file.Path;
import java.util.List;

public interface XlFacade
{
	Fallible<List<SheetSummary>> listSheets(Path file);

	Fallible<CellValue> readCell(Path file, String sheet, String cellReference);

	Fallible<CellGrid> readRange(Path file, String sheet, String fromCell, String toCell);

	Fallible<Void> writeCell(WriteCellRequest request);

	Fallible<Void> writeRange(WriteRangeRequest request);

	Fallible<Void> createWorkbook(Path file, boolean overwrite);

	Fallible<Void> addSheet(Path file, String sheetName);

	Fallible<Void> renameSheet(Path file, String sheetName, String newName);

	Fallible<Void> deleteSheet(Path file, String sheetName);

	Fallible<Void> copySheet(Path file, String sourceSheet, String targetName);

	Fallible<Void> moveSheet(Path file, String sheetName, int position);

	Fallible<Void> deleteColumn(Path file, String sheet, String columnNotation);

	Fallible<Void> setTabColor(Path file, String sheet, String hexRgb);

	Fallible<List<CellValue>> readRow(Path file, String sheet, String rowRef);

	Fallible<List<CellValue>> readColumn(Path file, String sheet, String columnRef);

	Fallible<CellValue> evaluateCell(Path file, String sheet, String cellReference);

	Fallible<Void> insertRow(Path file, String sheet, String rowRef);

	Fallible<Void> deleteRow(Path file, String sheet, String rowRef);

	Fallible<Void> setColumnWidth(Path file, String sheet, String columnRef, int characterWidth);

	Fallible<Void> autoFitColumn(Path file, String sheet, String columnRef);

	Fallible<Void> autoFitAllColumns(Path file, String sheet);

	Fallible<Void> importCsv(Path xlFile, String sheet, Path csvFile,
	                         String startCell, boolean overwrite, char delimiter);

	Fallible<String> findColumn(Path file, String sheet, String header);

	Fallible<Void> insertColumn(Path file, String sheet, String columnRef, List<CellValue> values);

	Fallible<Void> appendColumn(Path file, String sheet, List<CellValue> values);

	Fallible<RangeStats> rangeStats(Path file, String sheet, String fromCell, String toCell);

	Fallible<Integer> findRow(Path file, String sheet, String columnRef, String value);

	Fallible<String> dims(Path file, String sheet);

	Fallible<Void> exportCsv(Path xlFile, String sheet, Path csvFile, char delimiter);
}