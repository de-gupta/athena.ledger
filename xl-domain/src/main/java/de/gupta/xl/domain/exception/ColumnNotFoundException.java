package de.gupta.xl.domain.exception;

public final class ColumnNotFoundException extends RuntimeException
{
	private static final String MESSAGE = "Column header not found in first row: ";

	public static ColumnNotFoundException forHeader(final String header)
	{
		return new ColumnNotFoundException(MESSAGE + header);
	}

	private ColumnNotFoundException(final String message)
	{
		super(message);
	}
}