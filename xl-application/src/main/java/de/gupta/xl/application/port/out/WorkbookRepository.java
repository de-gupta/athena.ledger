package de.gupta.xl.application.port.out;

import de.gupta.xl.domain.*;

import java.nio.file.Path;

public interface WorkbookRepository
{
	WorkbookContent load(Path file);

	void createWorkbook(Path file);

	void addSheet(Path file, String sheetName);

	void renameSheet(Path file, String sheetName, String newName);

	void deleteSheet(Path file, String sheetName);

	void copySheet(Path file, String sourceSheet, String targetName);

	void moveSheet(Path file, String sheetName, int position);

	void deleteColumn(Path file, String sheet, int columnIndex);

	void setTabColor(Path file, String sheet, TabColor color);

	CellValue readCell(Path file, String sheet, CellReference reference);

	CellGrid readRange(Path file, String sheet, CellRangeReference range);

	void writeCell(Path file, String sheet, CellReference reference, CellValue value);

	void writeRange(Path file, String sheet, CellReference startReference,
	                CellGrid grid, boolean overwrite);
}