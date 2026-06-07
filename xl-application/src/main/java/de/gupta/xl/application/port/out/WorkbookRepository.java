package de.gupta.xl.application.port.out;

import de.gupta.xl.domain.CellReference;
import de.gupta.xl.domain.CellValue;
import de.gupta.xl.domain.WorkbookContent;

import java.nio.file.Path;

public interface WorkbookRepository
{
	WorkbookContent load(Path file);

	void createWorkbook(Path file);

	void addSheet(Path file, String sheetName);

	void deleteSheet(Path file, String sheetName);

	void copySheet(Path file, String sourceSheet, String targetName);

	CellValue readCell(Path file, String sheet, CellReference reference);

	void writeCell(Path file, String sheet, CellReference reference, CellValue value);
}