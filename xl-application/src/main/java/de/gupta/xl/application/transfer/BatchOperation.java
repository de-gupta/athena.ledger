package de.gupta.xl.application.transfer;

import de.gupta.xl.domain.CellValue;

public sealed interface BatchOperation
		permits BatchOperation.WriteCell,
		BatchOperation.FormatCell,
		BatchOperation.FormatRange,
		BatchOperation.FreezePanes,
		BatchOperation.SetTabColor,
		BatchOperation.AddSheet
{
	record WriteCell(String sheet, String cellRef, CellValue value) implements BatchOperation
	{
	}

	record FormatCell(String sheet, String cellRef, CellFormat format) implements BatchOperation
	{
	}

	record FormatRange(String sheet, String fromCell, String toCell, CellFormat format) implements BatchOperation
	{
	}

	record FreezePanes(String sheet, int frozenRows, int frozenColumns) implements BatchOperation
	{
	}

	record SetTabColor(String sheet, String hexRgb) implements BatchOperation
	{
	}

	record AddSheet(String sheetName) implements BatchOperation
	{
	}
}