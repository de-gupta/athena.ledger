package de.gupta.xl.domain.exception;

public final class LastSheetException extends RuntimeException
{
	private static final String MESSAGE = "Cannot delete the last sheet in a workbook";

	public static LastSheetException instance()
	{
		return new LastSheetException(MESSAGE);
	}

	private LastSheetException(final String message)
	{
		super(message);
	}
}