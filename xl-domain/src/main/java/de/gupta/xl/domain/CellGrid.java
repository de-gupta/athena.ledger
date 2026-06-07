package de.gupta.xl.domain;

import java.util.List;

public record CellGrid(List<List<CellValue>> rows)
{
	public static CellGrid of(final List<List<CellValue>> rows)
	{
		return new CellGrid(rows);
	}

	public static CellGrid empty()
	{
		return new CellGrid(List.of());
	}

	public CellGrid
	{
		rows = rows.stream().map(List::copyOf).toList();
	}

	public boolean isEmpty()
	{
		return rows.isEmpty();
	}

	public int rowCount()
	{
		return rows.size();
	}

	public int columnCount()
	{
		return rows.isEmpty() ? 0 : rows.getFirst().size();
	}
}