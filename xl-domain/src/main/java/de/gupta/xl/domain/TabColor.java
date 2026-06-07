package de.gupta.xl.domain;

public record TabColor(int red, int green, int blue)
{
	private static final String INVALID_MESSAGE = "Invalid hex RGB color (expected 6 hex digits, e.g. 70AD47): ";

	public static TabColor of(final String hexRgb)
	{
		if (hexRgb == null)
		{
			throw new IllegalArgumentException(INVALID_MESSAGE + hexRgb);
		}
		var normalized = hexRgb.startsWith("#") ? hexRgb.substring(1) : hexRgb;
		if (!normalized.matches("[0-9A-Fa-f]{6}"))
		{
			throw new IllegalArgumentException(INVALID_MESSAGE + hexRgb);
		}
		return new TabColor(
				Integer.parseInt(normalized.substring(0, 2), 16),
				Integer.parseInt(normalized.substring(2, 4), 16),
				Integer.parseInt(normalized.substring(4, 6), 16)
		);
	}
}