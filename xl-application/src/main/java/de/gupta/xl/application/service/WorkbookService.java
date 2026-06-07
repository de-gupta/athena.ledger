package de.gupta.xl.application.service;

import de.gupta.commons.utility.string.StringSanitizationUtility;
import de.gupta.xl.application.port.out.WorkbookRepository;
import de.gupta.xl.application.transfer.SheetSummary;
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.application.transfer.WriteRangeRequest;
import de.gupta.xl.domain.*;
import de.gupta.xl.domain.exception.LastSheetException;
import de.gupta.xl.domain.exception.SheetAlreadyExistsException;
import de.gupta.xl.domain.exception.SheetNotFoundException;
import de.gupta.xl.domain.exception.WorkbookAlreadyExistsException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class WorkbookService
{
	private final WorkbookRepository repository;

	public WorkbookService(final WorkbookRepository repository)
	{
		this.repository = repository;
	}

	public List<SheetSummary> listSheets(final Path file)
	{
		return repository.load(file).sheets().stream()
		                 .map(sheet -> new SheetSummary(sheet.name(), sheet.rowCount()))
		                 .toList();
	}

	public CellValue readCell(final Path file, final String sheet, final String cellReference)
	{
		StringSanitizationUtility.requireNotBlank(sheet, "Sheet name must not be blank");
		StringSanitizationUtility.requireNotBlank(cellReference, "Cell reference must not be blank");
		return repository.readCell(file, sheet, CellReference.of(cellReference));
	}

    public CellGrid readRange(final Path file, final String sheet, final String fromCell, final String toCell)
    {
        StringSanitizationUtility.requireNotBlank(sheet, "Sheet name must not be blank");
        StringSanitizationUtility.requireNotBlank(fromCell, "From cell must not be blank");
        StringSanitizationUtility.requireNotBlank(toCell, "To cell must not be blank");
        return repository.readRange(file, sheet, CellRangeReference.of(fromCell, toCell));
    }

	public void writeCell(final WriteCellRequest request)
	{
		StringSanitizationUtility.requireNotBlank(request.sheet(), "Sheet name must not be blank");
		StringSanitizationUtility.requireNotBlank(request.cellReference(), "Cell reference must not be blank");
		repository.writeCell(request.file(), request.sheet(),
				CellReference.of(request.cellReference()), request.value());
	}

    public void writeRange(final WriteRangeRequest request)
    {
        StringSanitizationUtility.requireNotBlank(request.sheet(), "Sheet name must not be blank");
        StringSanitizationUtility.requireNotBlank(request.startCell(), "Start cell must not be blank");
        if (request.grid().isEmpty())
        {
            return;
        }
        var startReference = CellReference.of(request.startCell());
        repository.writeRange(request.file(), request.sheet(), startReference,
                request.grid(), request.overwrite());
    }

	public void createWorkbook(final Path file, final boolean overwrite)
	{
		if (!overwrite && Files.exists(file))
		{
			throw WorkbookAlreadyExistsException.forFile(file);
		}
		repository.createWorkbook(file);
	}

	public void addSheet(final Path file, final String sheetName)
	{
		StringSanitizationUtility.requireNotBlank(sheetName, "Sheet name must not be blank");
		var content = repository.load(file);
		var nameAlreadyExists = content.sheets().stream()
		                               .anyMatch(sheet -> sheet.name().equals(sheetName));
		if (nameAlreadyExists)
		{
			throw SheetAlreadyExistsException.forSheet(sheetName);
		}
		repository.addSheet(file, sheetName);
	}

	public void deleteSheet(final Path file, final String sheetName)
	{
		StringSanitizationUtility.requireNotBlank(sheetName, "Sheet name must not be blank");
		var content = repository.load(file);
		var sheetExists = content.sheets().stream()
		                         .anyMatch(sheet -> sheet.name().equals(sheetName));
		if (!sheetExists)
		{
			throw SheetNotFoundException.forSheet(sheetName);
		}
		if (content.sheets().size() == 1)
		{
			throw LastSheetException.instance();
		}
		repository.deleteSheet(file, sheetName);
	}

    public void renameSheet(final Path file, final String sheetName, final String newName)
    {
        StringSanitizationUtility.requireNotBlank(sheetName, "Sheet name must not be blank");
        StringSanitizationUtility.requireNotBlank(newName, "New name must not be blank");
        var content = repository.load(file);
        var sheetExists = content.sheets().stream().anyMatch(s -> s.name().equals(sheetName));
        if (!sheetExists)
        {
            throw SheetNotFoundException.forSheet(sheetName);
        }
        var targetAlreadyExists = content.sheets().stream().anyMatch(s -> s.name().equals(newName));
        if (targetAlreadyExists)
        {
            throw SheetAlreadyExistsException.forSheet(newName);
        }
        repository.renameSheet(file, sheetName, newName);
    }

    public void moveSheet(final Path file, final String sheetName, final int position)
    {
        StringSanitizationUtility.requireNotBlank(sheetName, "Sheet name must not be blank");
        var content = repository.load(file);
        var sheetExists = content.sheets().stream().anyMatch(s -> s.name().equals(sheetName));
        if (!sheetExists)
        {
            throw SheetNotFoundException.forSheet(sheetName);
        }
        var sheetCount = content.sheets().size();
        if (position < 0 || position >= sheetCount)
        {
            throw new IllegalArgumentException(
                    "Position " + position + " out of range for workbook with " + sheetCount + " sheets");
        }
        repository.moveSheet(file, sheetName, position);
    }

    public void deleteColumn(final Path file, final String sheet, final String columnNotation)
    {
        StringSanitizationUtility.requireNotBlank(sheet, "Sheet name must not be blank");
        StringSanitizationUtility.requireNotBlank(columnNotation, "Column reference must not be blank");
        var content = repository.load(file);
        var sheetExists = content.sheets().stream().anyMatch(s -> s.name().equals(sheet));
        if (!sheetExists)
        {
            throw SheetNotFoundException.forSheet(sheet);
        }
        repository.deleteColumn(file, sheet, ColumnReference.of(columnNotation).index());
    }

    public void setTabColor(final Path file, final String sheet, final String hexRgb)
    {
        StringSanitizationUtility.requireNotBlank(sheet, "Sheet name must not be blank");
        StringSanitizationUtility.requireNotBlank(hexRgb, "Color must not be blank");
        var content = repository.load(file);
        var sheetExists = content.sheets().stream().anyMatch(s -> s.name().equals(sheet));
        if (!sheetExists)
        {
            throw SheetNotFoundException.forSheet(sheet);
        }
        repository.setTabColor(file, sheet, TabColor.of(hexRgb));
    }

	public void copySheet(final Path file, final String sourceSheet, final String targetName)
	{
		StringSanitizationUtility.requireNotBlank(sourceSheet, "Source sheet name must not be blank");
		StringSanitizationUtility.requireNotBlank(targetName, "Target sheet name must not be blank");
		var content = repository.load(file);
		var sourceExists = content.sheets().stream()
		                          .anyMatch(sheet -> sheet.name().equals(sourceSheet));
		if (!sourceExists)
		{
			throw SheetNotFoundException.forSheet(sourceSheet);
		}
		var targetAlreadyExists = content.sheets().stream()
		                                 .anyMatch(sheet -> sheet.name().equals(targetName));
		if (targetAlreadyExists)
		{
			throw SheetAlreadyExistsException.forSheet(targetName);
		}
		repository.copySheet(file, sourceSheet, targetName);
	}
}