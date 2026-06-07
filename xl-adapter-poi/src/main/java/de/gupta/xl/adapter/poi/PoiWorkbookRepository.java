package de.gupta.xl.adapter.poi;

import de.gupta.xl.application.port.out.WorkbookRepository;
import de.gupta.xl.domain.*;
import de.gupta.xl.domain.exception.WorkbookNotFoundException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class PoiWorkbookRepository implements WorkbookRepository
{
	private static final Logger log = LoggerFactory.getLogger(PoiWorkbookRepository.class);

	@Override
	public WorkbookContent load(final Path file)
	{
		requireExists(file);
		try (var inputStream = Files.newInputStream(file);
		     var workbook = new XSSFWorkbook(inputStream))
		{
			var sheets = new ArrayList<Sheet>();
			for (var index = 0; index < workbook.getNumberOfSheets(); index++)
			{
				var poiSheet = workbook.getSheetAt(index);
				sheets.add(new Sheet(poiSheet.getSheetName(), poiSheet.getPhysicalNumberOfRows()));
			}
			return new WorkbookContent(file, List.copyOf(sheets));
		}
		catch (IOException caught)
		{
			log.error("Failed to load workbook: {}", file, caught);
			throw new IllegalStateException("Failed to load workbook: " + file, caught);
		}
	}

	@Override
	public void createWorkbook(final Path file)
	{
		try (var workbook = new XSSFWorkbook())
		{
			workbook.createSheet("Sheet1");
			writeAtomically(file, workbook);
		}
		catch (IOException caught)
		{
			log.error("Failed to create workbook: {}", file, caught);
			throw new IllegalStateException("Failed to create workbook: " + file, caught);
		}
	}

	@Override
	public void addSheet(final Path file, final String sheetName)
	{
		modifyWorkbook(file, workbook -> workbook.createSheet(sheetName));
	}

	@Override
	public void renameSheet(final Path file, final String sheetName, final String newName)
	{
		modifyWorkbook(file, workbook ->
				workbook.setSheetName(workbook.getSheetIndex(sheetName), newName));
	}

	@Override
	public void moveSheet(final Path file, final String sheetName, final int position)
	{
		modifyWorkbook(file, workbook -> workbook.setSheetOrder(sheetName, position));
	}

	@Override
	public void deleteColumn(final Path file, final String sheet, final int columnIndex)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			poiSheet.shiftColumns(columnIndex + 1, org.apache.poi.ss.SpreadsheetVersion.EXCEL2007.getLastColumnIndex(),
					-1);
		});
	}

	@Override
	public void setTabColor(final Path file, final String sheet, final TabColor color)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			poiSheet.setTabColor(new org.apache.poi.xssf.usermodel.XSSFColor(
					new byte[]{(byte) color.red(), (byte) color.green(), (byte) color.blue()}, null));
		});
	}

	@Override
	public void deleteSheet(final Path file, final String sheetName)
	{
		modifyWorkbook(file, workbook ->
		{
			var sheetIndex = workbook.getSheetIndex(sheetName);
			workbook.removeSheetAt(sheetIndex);
		});
	}

	@Override
	public void copySheet(final Path file, final String sourceSheet, final String targetName)
	{
		modifyWorkbook(file, workbook ->
		{
			var sourceIndex = workbook.getSheetIndex(sourceSheet);
			var cloned = workbook.cloneSheet(sourceIndex);
			workbook.setSheetName(workbook.getSheetIndex(cloned), targetName);
		});
	}

	@Override
	public CellValue readCell(final Path file, final String sheet, final CellReference reference)
	{
		requireExists(file);
		try (var inputStream = Files.newInputStream(file);
		     var workbook = new XSSFWorkbook(inputStream))
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return new CellValue.Empty();
			}
			var row = poiSheet.getRow(reference.rowIndex());
			if (row == null)
			{
				return new CellValue.Empty();
			}
			var cell = row.getCell(reference.columnIndex());
			if (cell == null)
			{
				return new CellValue.Empty();
			}
			return toCellValue(cell);
		}
		catch (IOException caught)
		{
			log.error("Failed to read cell from workbook: {}", file, caught);
			throw new IllegalStateException("Failed to read cell from workbook: " + file, caught);
		}
	}

	@Override
	public CellGrid readRange(final Path file, final String sheet, final CellRangeReference range)
	{
		requireExists(file);
		try (var inputStream = Files.newInputStream(file);
		     var workbook = new XSSFWorkbook(inputStream))
		{
			var poiSheet = workbook.getSheet(sheet);
			var rows = new ArrayList<List<CellValue>>();
			for (var rowOffset = 0; rowOffset < range.rowCount(); rowOffset++)
			{
				var absoluteRowIndex = range.topLeft().rowIndex() + rowOffset;
				var poiRow = poiSheet != null ? poiSheet.getRow(absoluteRowIndex) : null;
				var cells = new ArrayList<CellValue>();
				for (var columnOffset = 0; columnOffset < range.columnCount(); columnOffset++)
				{
					var absoluteColumnIndex = range.topLeft().columnIndex() + columnOffset;
					var cell = poiRow != null ? poiRow.getCell(absoluteColumnIndex) : null;
					cells.add(cell != null ? toCellValue(cell) : new CellValue.Empty());
				}
				rows.add(List.copyOf(cells));
			}
			return CellGrid.of(rows);
		}
		catch (IOException caught)
		{
			log.error("Failed to read range from workbook: {}", file, caught);
			throw new IllegalStateException("Failed to read range from workbook: " + file, caught);
		}
	}

	@Override
	public void writeCell(final Path file, final String sheet, final CellReference reference, final CellValue value)
	{
		requireExists(file);
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				poiSheet = workbook.createSheet(sheet);
			}
			var row = poiSheet.getRow(reference.rowIndex());
			if (row == null)
			{
				row = poiSheet.createRow(reference.rowIndex());
			}
			var cell = row.getCell(reference.columnIndex());
			if (cell == null)
			{
				cell = row.createCell(reference.columnIndex());
			}
			applyCellValue(workbook, cell, value);
		});
	}

	@Override
	public void writeRange(final Path file, final String sheet, final CellReference startReference,
	                       final CellGrid grid, final boolean overwrite)
	{
		requireExists(file);
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = Objects.requireNonNullElseGet(
					workbook.getSheet(sheet), () -> workbook.createSheet(sheet));
			var rows = grid.rows();
			for (var rowOffset = 0; rowOffset < rows.size(); rowOffset++)
			{
				var rowData = rows.get(rowOffset);
				var absoluteRowIndex = startReference.rowIndex() + rowOffset;
				var poiRow = Objects.requireNonNullElseGet(
						poiSheet.getRow(absoluteRowIndex), () -> poiSheet.createRow(absoluteRowIndex));
				for (var columnOffset = 0; columnOffset < rowData.size(); columnOffset++)
				{
					var absoluteColumnIndex = startReference.columnIndex() + columnOffset;
					if (!overwrite)
					{
						var existingCell = poiRow.getCell(absoluteColumnIndex);
						if (existingCell != null && existingCell.getCellType() != CellType.BLANK)
						{
							continue;
						}
					}
					var cell = Objects.requireNonNullElseGet(
							poiRow.getCell(absoluteColumnIndex),
							() -> poiRow.createCell(absoluteColumnIndex));
					applyCellValue(workbook, cell, rowData.get(columnOffset));
				}
			}
		});
	}

	private static CellValue toCellValue(final Cell cell)
	{
		return switch (cell.getCellType())
		{
			case STRING -> new CellValue.Str(cell.getStringCellValue());
			case BOOLEAN -> new CellValue.Bool(cell.getBooleanCellValue());
			case NUMERIC ->
			{
				if (DateUtil.isCellDateFormatted(cell))
				{
					var localDate = cell.getDateCellValue().toInstant()
					                    .atZone(ZoneId.systemDefault()).toLocalDate();
					yield new CellValue.Date(localDate);
				}
				yield new CellValue.Num(cell.getNumericCellValue());
			}
			case FORMULA -> new CellValue.Formula(cell.getCellFormula());
			case BLANK, ERROR, _NONE -> new CellValue.Empty();
		};
	}

	private static void applyCellValue(final XSSFWorkbook workbook, final Cell cell, final CellValue value)
	{
		switch (value)
		{
			case CellValue.Str(var string) -> cell.setCellValue(string);
			case CellValue.Num(var number) -> cell.setCellValue(number);
			case CellValue.Bool(var bool) -> cell.setCellValue(bool);
			case CellValue.Date(LocalDate date) ->
			{
				cell.setCellValue(date);
				var style = workbook.createCellStyle();
				var format = workbook.getCreationHelper().createDataFormat();
				style.setDataFormat(format.getFormat("yyyy-mm-dd"));
				cell.setCellStyle(style);
			}
			case CellValue.Formula(var expression) -> cell.setCellFormula(expression);
			case CellValue.Empty() -> cell.setBlank();
		}
	}

	private static void requireExists(final Path file)
	{
		if (!Files.exists(file))
		{
			throw WorkbookNotFoundException.forFile(file);
		}
	}

	private void modifyWorkbook(final Path file, final WorkbookModification modification)
	{
		requireExists(file);
		try (var inputStream = Files.newInputStream(file);
		     var workbook = new XSSFWorkbook(inputStream))
		{
			modification.apply(workbook);
			writeAtomically(file, workbook);
		}
		catch (IOException caught)
		{
			log.error("Failed to modify workbook: {}", file, caught);
			throw new IllegalStateException("Failed to modify workbook: " + file, caught);
		}
	}

	private void writeAtomically(final Path file, final XSSFWorkbook workbook) throws IOException
	{
		var temporaryFile = file.resolveSibling(file.getFileName() + ".temporary");
		try (OutputStream outputStream = Files.newOutputStream(temporaryFile))
		{
			workbook.write(outputStream);
		}
		catch (IOException caught)
		{
			try
			{
				Files.deleteIfExists(temporaryFile);
			}
			catch (IOException deleteException)
			{
				log.error("Failed to delete temporary file: {}", temporaryFile, deleteException);
			}
			throw caught;
		}
		Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	@Override
	public List<CellValue> readRow(final Path file, final String sheet, final RowReference reference)
	{
		requireExists(file);
		try (var inputStream = Files.newInputStream(file);
		     var workbook = new XSSFWorkbook(inputStream))
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return List.of();
			}
			var row = poiSheet.getRow(reference.index());
			if (row == null)
			{
				return List.of();
			}
			var cells = new ArrayList<CellValue>();
			for (var columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++)
			{
				var cell = row.getCell(columnIndex);
				cells.add(cell != null ? toCellValue(cell) : new CellValue.Empty());
			}
			return List.copyOf(cells);
		}
		catch (IOException caught)
		{
			log.error("Failed to read row from workbook: {}", file, caught);
			throw new IllegalStateException("Failed to read row from workbook: " + file, caught);
		}
	}

	@Override
	public List<CellValue> readColumn(final Path file, final String sheet, final ColumnReference reference)
	{
		requireExists(file);
		try (var inputStream = Files.newInputStream(file);
		     var workbook = new XSSFWorkbook(inputStream))
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return List.of();
			}
			var cells = new ArrayList<CellValue>();
			for (var rowIndex = 0; rowIndex <= poiSheet.getLastRowNum(); rowIndex++)
			{
				var row = poiSheet.getRow(rowIndex);
				if (row == null)
				{
					cells.add(new CellValue.Empty());
					continue;
				}
				var cell = row.getCell(reference.index());
				cells.add(cell != null ? toCellValue(cell) : new CellValue.Empty());
			}
			return List.copyOf(cells);
		}
		catch (IOException caught)
		{
			log.error("Failed to read column from workbook: {}", file, caught);
			throw new IllegalStateException("Failed to read column from workbook: " + file, caught);
		}
	}

	@Override
	public CellValue evaluateCell(final Path file, final String sheet, final CellReference reference)
	{
		requireExists(file);
		try (var inputStream = Files.newInputStream(file);
		     var workbook = new XSSFWorkbook(inputStream))
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return new CellValue.Empty();
			}
			var row = poiSheet.getRow(reference.rowIndex());
			if (row == null)
			{
				return new CellValue.Empty();
			}
			var cell = row.getCell(reference.columnIndex());
			if (cell == null)
			{
				return new CellValue.Empty();
			}
			if (cell.getCellType() != CellType.FORMULA)
			{
				return toCellValue(cell);
			}
			var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			var evaluated = evaluator.evaluate(cell);
			return switch (evaluated.getCellType())
			{
				case NUMERIC -> DateUtil.isCellDateFormatted(cell)
						? new CellValue.Date(cell.getDateCellValue().toInstant()
						                         .atZone(ZoneId.systemDefault()).toLocalDate())
						: new CellValue.Num(evaluated.getNumberValue());
				case STRING -> new CellValue.Str(evaluated.getStringValue());
				case BOOLEAN -> new CellValue.Bool(evaluated.getBooleanValue());
				default -> new CellValue.Empty();
			};
		}
		catch (IOException caught)
		{
			log.error("Failed to evaluate cell in workbook: {}", file, caught);
			throw new IllegalStateException("Failed to evaluate cell in workbook: " + file, caught);
		}
	}

	@Override
	public void insertRow(final Path file, final String sheet, final RowReference reference)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			var lastRow = poiSheet.getLastRowNum();
			if (reference.index() <= lastRow)
			{
				poiSheet.shiftRows(reference.index(), lastRow, 1);
			}
			poiSheet.createRow(reference.index());
		});
	}

	@Override
	public void deleteRow(final Path file, final String sheet, final RowReference reference)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			var row = poiSheet.getRow(reference.index());
			if (row != null)
			{
				poiSheet.removeRow(row);
			}
			var lastRow = poiSheet.getLastRowNum();
			if (reference.index() < lastRow)
			{
				poiSheet.shiftRows(reference.index() + 1, lastRow, -1);
			}
		});
	}

	@Override
	public void setColumnWidth(final Path file, final String sheet,
	                           final ColumnReference reference, final int characterWidth)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			poiSheet.setColumnWidth(reference.index(), characterWidth * 256);
		});
	}

	@Override
	public void autoFitColumn(final Path file, final String sheet, final ColumnReference reference)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			poiSheet.autoSizeColumn(reference.index());
		});
	}

	@Override
	public void autoFitAllColumns(final Path file, final String sheet)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			var lastColumn = 0;
			for (var rowIndex = 0; rowIndex <= poiSheet.getLastRowNum(); rowIndex++)
			{
				var row = poiSheet.getRow(rowIndex);
				if (row != null && row.getLastCellNum() > lastColumn)
				{
					lastColumn = row.getLastCellNum();
				}
			}
			for (var columnIndex = 0; columnIndex < lastColumn; columnIndex++)
			{
				poiSheet.autoSizeColumn(columnIndex);
			}
		});
	}

	@Override
	public int findColumn(final Path file, final String sheet, final String header)
	{
		requireExists(file);
		try (var inputStream = Files.newInputStream(file);
		     var workbook = new XSSFWorkbook(inputStream))
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return -1;
			}
			var firstRow = poiSheet.getRow(0);
			if (firstRow == null)
			{
				return -1;
			}
			for (var columnIndex = 0; columnIndex < firstRow.getLastCellNum(); columnIndex++)
			{
				var cell = firstRow.getCell(columnIndex);
				if (cell != null
						&& cell.getCellType() == CellType.STRING
						&& header.equals(cell.getStringCellValue()))
				{
					return columnIndex;
				}
			}
			return -1;
		}
		catch (IOException caught)
		{
			log.error("Failed to search header in workbook: {}", file, caught);
			throw new IllegalStateException("Failed to search header in workbook: " + file, caught);
		}
	}

	@Override
	public void insertColumn(final Path file, final String sheet,
	                         final ColumnReference reference, final List<CellValue> values)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			poiSheet.shiftColumns(reference.index(),
					org.apache.poi.ss.SpreadsheetVersion.EXCEL2007.getLastColumnIndex(), 1);
			writeColumnValues(workbook, poiSheet, reference.index(), values);
		});
	}

	@Override
	public void appendColumn(final Path file, final String sheet, final List<CellValue> values)
	{
		modifyWorkbook(file, workbook ->
		{
			var poiSheet = workbook.getSheet(sheet);
			if (poiSheet == null)
			{
				return;
			}
			var lastColumn = -1;
			for (var rowIndex = 0; rowIndex <= poiSheet.getLastRowNum(); rowIndex++)
			{
				var row = poiSheet.getRow(rowIndex);
				if (row != null && row.getLastCellNum() - 1 > lastColumn)
				{
					lastColumn = row.getLastCellNum() - 1;
				}
			}
			writeColumnValues(workbook, poiSheet, lastColumn + 1, values);
		});
	}

	private void writeColumnValues(final XSSFWorkbook workbook,
	                               final org.apache.poi.xssf.usermodel.XSSFSheet poiSheet,
	                               final int columnIndex, final List<CellValue> values)
	{
		for (var rowOffset = 0; rowOffset < values.size(); rowOffset++)
		{
			final var rowIndex = rowOffset;
			var row = Objects.requireNonNullElseGet(
					poiSheet.getRow(rowIndex), () -> poiSheet.createRow(rowIndex));
			var cell = Objects.requireNonNullElseGet(
					row.getCell(columnIndex), () -> row.createCell(columnIndex));
			applyCellValue(workbook, cell, values.get(rowOffset));
		}
	}

	@FunctionalInterface
	private interface WorkbookModification
	{
		void apply(XSSFWorkbook workbook) throws IOException;
	}
}