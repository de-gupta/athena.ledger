package de.gupta.xl.domain.exception;

public final class SheetNotFoundException extends RuntimeException
{
	private static final String MESSAGE = "Sheet not found: ";

	public static SheetNotFoundException forSheet(final String name)
	{
		return new SheetNotFoundException(MESSAGE + name);
	}

	private SheetNotFoundException(final String message)
	{
		super(message);
	}
}