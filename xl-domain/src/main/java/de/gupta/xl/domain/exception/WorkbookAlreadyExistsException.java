package de.gupta.xl.domain.exception;

import java.nio.file.Path;

public final class WorkbookAlreadyExistsException extends RuntimeException
{
	private static final String MESSAGE = "Workbook already exists: ";

	public static WorkbookAlreadyExistsException forFile(final Path path)
	{
		return new WorkbookAlreadyExistsException(MESSAGE + path);
	}

	private WorkbookAlreadyExistsException(final String message)
	{
		super(message);
	}
}