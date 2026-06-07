package de.gupta.xl.application.port.in;

import de.gupta.aletheia.trials.Fallible;
import de.gupta.xl.application.transfer.SheetSummary;
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.domain.CellValue;

import java.nio.file.Path;
import java.util.List;

public interface XlFacade
{
	Fallible<List<SheetSummary>> listSheets(Path file);

	Fallible<CellValue> readCell(Path file, String sheet, String cellReference);

	Fallible<Void> writeCell(WriteCellRequest request);

	Fallible<Void> createWorkbook(Path file, boolean overwrite);

	Fallible<Void> addSheet(Path file, String sheetName);

	Fallible<Void> deleteSheet(Path file, String sheetName);

	Fallible<Void> copySheet(Path file, String sourceSheet, String targetName);
}