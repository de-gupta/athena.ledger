package de.gupta.xl.domain;

public final class ColumnReference
{
	private static final String INVALID_MESSAGE = "Invalid column reference: ";

	private final int index;

	public static ColumnReference of(final String notation)
	{
		if (notation == null || notation.isBlank())
		{
			throw new IllegalArgumentException(INVALID_MESSAGE + notation);
		}
		var trimmed = notation.trim();
		if (trimmed.matches("[0-9]+"))
		{
			var oneBased = Integer.parseInt(trimmed);
			if (oneBased < 1)
			{
				throw new IllegalArgumentException(INVALID_MESSAGE + notation);
			}
			return new ColumnReference(oneBased - 1);
		}
		if (!trimmed.matches("[A-Za-z]+"))
		{
			throw new IllegalArgumentException(INVALID_MESSAGE + notation);
		}
		return new ColumnReference(parseLetters(trimmed.toUpperCase()));
	}

	public static String toLetters(final int index)
	{
		var result = new StringBuilder();
		var remaining = index + 1;
		while (remaining > 0)
		{
			remaining--;
			result.insert(0, (char) ('A' + remaining % 26));
			remaining /= 26;
		}
		return result.toString();
	}

	static int parseLetters(final String upperCaseLetters)
	{
		var columnIndex = 0;
		for (final var character : upperCaseLetters.toCharArray())
		{
			columnIndex = columnIndex * 26 + (character - 'A' + 1);
		}
		return columnIndex - 1;
	}

	public int index()
	{
		return index;
	}

	@Override
	public int hashCode()
	{
		return index;
	}

	@Override
	public boolean equals(final Object object)
	{
		return this == object || (object instanceof ColumnReference other && index == other.index);
	}

	@Override
	public String toString()
	{
		return "ColumnReference[index=" + index + "]";
	}

	private ColumnReference(final int index)
	{
		this.index = index;
	}
}