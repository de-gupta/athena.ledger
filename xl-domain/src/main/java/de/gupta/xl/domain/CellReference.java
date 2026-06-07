package de.gupta.xl.domain;

import java.util.regex.Pattern;

public final class CellReference
{
	private static final Pattern NOTATION_PATTERN = Pattern.compile("([A-Za-z]+)([0-9]+)");
	private static final String INVALID_NOTATION_MESSAGE = "Invalid A1 cell reference: ";

	private final int columnIndex;
	private final int rowIndex;

	public static CellReference of(final int columnIndex, final int rowIndex)
	{
		return new CellReference(columnIndex, rowIndex);
	}

	public static CellReference of(final String notation)
	{
		if (notation == null || notation.isBlank())
		{
			throw new IllegalArgumentException(INVALID_NOTATION_MESSAGE + notation);
		}
		var matcher = NOTATION_PATTERN.matcher(notation.trim());
		if (!matcher.matches() || matcher.group(1).length() + matcher.group(2).length() != notation.trim().length())
		{
			throw new IllegalArgumentException(INVALID_NOTATION_MESSAGE + notation);
		}
		var columnIndex = parseColumnIndex(matcher.group(1).toUpperCase());
		var rowIndex = parseRowIndex(matcher.group(2));
		return new CellReference(columnIndex, rowIndex);
	}

	public int columnIndex()
	{
		return columnIndex;
	}

	public int rowIndex()
	{
		return rowIndex;
	}

	@Override
	public int hashCode()
	{
		return 31 * columnIndex + rowIndex;
	}

	@Override
	public boolean equals(final Object object)
	{
		return this == object || (object instanceof CellReference other
				&& columnIndex == other.columnIndex
				&& rowIndex == other.rowIndex);
	}

	@Override
	public String toString()
	{
		return "CellReference[column=" + columnIndex + ", row=" + rowIndex + "]";
	}

	private static int parseColumnIndex(final String letters)
	{
		return ColumnReference.parseLetters(letters);
	}

	private static int parseRowIndex(final String digits)
	{
		var row = Integer.parseInt(digits);
		if (row < 1)
		{
			throw new IllegalArgumentException(INVALID_NOTATION_MESSAGE + digits);
		}
		return row - 1;
	}

	private CellReference(final int columnIndex, final int rowIndex)
	{
		this.columnIndex = columnIndex;
		this.rowIndex = rowIndex;
	}
}