package de.gupta.xl.domain;

public final class CellRangeReference
{
	private static final String INVERTED_RANGE_MESSAGE =
			"Top-left corner must not be below or to the right of the bottom-right corner";

	private final CellReference topLeft;
	private final CellReference bottomRight;

	public static CellRangeReference of(final String from, final String to)
	{
		return of(CellReference.of(from), CellReference.of(to));
	}

	public static CellRangeReference of(final CellReference topLeft, final CellReference bottomRight)
	{
		if (topLeft.rowIndex() > bottomRight.rowIndex()
				|| topLeft.columnIndex() > bottomRight.columnIndex())
		{
			throw new IllegalArgumentException(INVERTED_RANGE_MESSAGE);
		}
		return new CellRangeReference(topLeft, bottomRight);
	}

	public CellReference topLeft()
	{
		return topLeft;
	}

	public CellReference bottomRight()
	{
		return bottomRight;
	}

	public int rowCount()
	{
		return bottomRight.rowIndex() - topLeft.rowIndex() + 1;
	}

	public int columnCount()
	{
		return bottomRight.columnIndex() - topLeft.columnIndex() + 1;
	}

	@Override
	public int hashCode()
	{
		return 31 * topLeft.hashCode() + bottomRight.hashCode();
	}

	@Override
	public boolean equals(final Object object)
	{
		return this == object || (object instanceof CellRangeReference other
				&& topLeft.equals(other.topLeft)
				&& bottomRight.equals(other.bottomRight));
	}

	@Override
	public String toString()
	{
		return "CellRangeReference[" + topLeft + ":" + bottomRight + "]";
	}

	private CellRangeReference(final CellReference topLeft, final CellReference bottomRight)
	{
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}
}