package de.gupta.xl.domain.exception;

public final class RowNotFoundException extends RuntimeException
{
	private static final String MESSAGE = "Value not found in column: ";

	public static RowNotFoundException forValue(final String value)
	{
		return new RowNotFoundException(MESSAGE + value);
	}

	private RowNotFoundException(final String message)
	{
		super(message);
	}
}