package de.gupta.xl.domain.exception;

public final class SheetAlreadyExistsException extends RuntimeException
{
	private static final String MESSAGE = "Sheet already exists: ";

	public static SheetAlreadyExistsException forSheet(final String name)
	{
		return new SheetAlreadyExistsException(MESSAGE + name);
	}

	private SheetAlreadyExistsException(final String message)
	{
		super(message);
	}
}