package de.gupta.xl.domain.exception;

import java.nio.file.Path;

public final class WorkbookNotFoundException extends RuntimeException
{
	private static final String MESSAGE = "Workbook not found: ";

	public static WorkbookNotFoundException forFile(final Path path)
	{
		return new WorkbookNotFoundException(MESSAGE + path);
	}

	private WorkbookNotFoundException(final String message)
	{
		super(message);
	}
}