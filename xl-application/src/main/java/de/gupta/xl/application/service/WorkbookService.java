package de.gupta.xl.application.service;

import de.gupta.commons.utility.string.StringSanitizationUtility;
import de.gupta.xl.application.port.out.WorkbookRepository;
import de.gupta.xl.application.transfer.SheetSummary;
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.domain.CellReference;
import de.gupta.xl.domain.CellValue;
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

	public void writeCell(final WriteCellRequest request)
	{
		StringSanitizationUtility.requireNotBlank(request.sheet(), "Sheet name must not be blank");
		StringSanitizationUtility.requireNotBlank(request.cellReference(), "Cell reference must not be blank");
		repository.writeCell(request.file(), request.sheet(),
				CellReference.of(request.cellReference()), request.value());
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