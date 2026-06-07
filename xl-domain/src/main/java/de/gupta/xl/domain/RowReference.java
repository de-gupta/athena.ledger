package de.gupta.xl.domain;

public final class RowReference
{
	private static final String INVALID_MESSAGE = "Invalid row reference (expected a positive integer): ";

	private final int index;

	public static RowReference of(final String notation)
	{
		if (notation == null || notation.isBlank())
		{
			throw new IllegalArgumentException(INVALID_MESSAGE + notation);
		}
		try
		{
			var oneBased = Integer.parseInt(notation.trim());
			if (oneBased < 1)
			{
				throw new IllegalArgumentException(INVALID_MESSAGE + notation);
			}
			return new RowReference(oneBased - 1);
		}
		catch (NumberFormatException ignored)
		{
			throw new IllegalArgumentException(INVALID_MESSAGE + notation);
		}
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
		return this == object || (object instanceof RowReference other && index == other.index);
	}

	@Override
	public String toString()
	{
		return "RowReference[index=" + index + "]";
	}

	private RowReference(final int index)
	{
		this.index = index;
	}
}