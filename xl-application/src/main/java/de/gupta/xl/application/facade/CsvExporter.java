package de.gupta.xl.application.facade;

import de.gupta.xl.domain.CellGrid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class CsvExporter
{
	static void write(final CellGrid grid, final Path destination, final char delimiter)
	{
		try
		{
			Files.writeString(destination, render(grid, delimiter));
		}
		catch (IOException caught)
		{
			throw new UncheckedIOException("Cannot write CSV file: " + destination, caught);
		}
	}

	static String render(final CellGrid grid, final char delimiter)
	{
		var output = new StringBuilder();
		for (var row : grid.rows())
		{
			for (var cellIndex = 0; cellIndex < row.size(); cellIndex++)
			{
				if (cellIndex > 0)
				{
					output.append(delimiter);
				}
				output.append(quote(row.get(cellIndex).displayValue(), delimiter));
			}
			output.append("\r\n");
		}
		return output.toString();
	}

	private static String quote(final String field, final char delimiter)
	{
		if (field.indexOf(delimiter) >= 0
				|| field.contains("\"")
				|| field.contains("\r")
				|| field.contains("\n"))
		{
			return '"' + field.replace("\"", "\"\"") + '"';
		}
		return field;
	}

	private CsvExporter()
	{
	}
}