package de.gupta.xl.domain.exception;

public final class EmptySheetException extends RuntimeException
{
	private static final String MESSAGE = "Sheet contains no data: ";

	public static EmptySheetException forSheet(final String name)
	{
		return new EmptySheetException(MESSAGE + name);
	}

	private EmptySheetException(final String message)
	{
		super(message);
	}
}