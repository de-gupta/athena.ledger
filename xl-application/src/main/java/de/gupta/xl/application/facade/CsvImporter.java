package de.gupta.xl.application.facade;

import de.gupta.xl.domain.CellGrid;
import de.gupta.xl.domain.CellValue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CsvImporter
{
	static CellGrid toCellGrid(final Path csvFile, final char delimiter)
	{
		try
		{
			return toCellGrid(Files.readString(csvFile), delimiter);
		}
		catch (IOException caught)
		{
			throw new UncheckedIOException("Cannot read CSV file: " + csvFile, caught);
		}
	}

	static CellGrid toCellGrid(final String content, final char delimiter)
	{
		var rows = parseRawRows(content, delimiter).stream()
		                                           .map(rawRow -> rawRow.stream()
		                                                                .map(CellValue::infer)
		                                                                .toList())
		                                           .toList();
		return CellGrid.of(rows);
	}

	private static List<List<String>> parseRawRows(final String content, final char delimiter)
	{
		var rows = new ArrayList<List<String>>();
		var currentRow = new ArrayList<String>();
		var field = new StringBuilder();
		var inQuotedField = false;
		var afterClosingQuote = false;
		final var length = content.length();

		for (var index = 0; index < length; index++)
		{
			final var character = content.charAt(index);

			if (afterClosingQuote)
			{
				afterClosingQuote = false;
				if (character == '"')
				{
					field.append('"');
					inQuotedField = true;
				}
				else if (character == delimiter)
				{
					currentRow.add(field.toString());
					field.setLength(0);
				}
				else if (character == '\r' || character == '\n')
				{
					if (character == '\r' && index + 1 < length && content.charAt(index + 1) == '\n')
					{
						index++;
					}
					currentRow.add(field.toString());
					field.setLength(0);
					rows.add(currentRow);
					currentRow = new ArrayList<>();
				}
			}
			else if (inQuotedField)
			{
				if (character == '"')
				{
					afterClosingQuote = true;
					inQuotedField = false;
				}
				else
				{
					field.append(character);
				}
			}
			else
			{
				if (character == '"' && field.isEmpty())
				{
					inQuotedField = true;
				}
				else if (character == delimiter)
				{
					currentRow.add(field.toString());
					field.setLength(0);
				}
				else if (character == '\r' || character == '\n')
				{
					if (character == '\r' && index + 1 < length && content.charAt(index + 1) == '\n')
					{
						index++;
					}
					currentRow.add(field.toString());
					field.setLength(0);
					rows.add(currentRow);
					currentRow = new ArrayList<>();
				}
				else
				{
					field.append(character);
				}
			}
		}

		if (!field.isEmpty() || !currentRow.isEmpty())
		{
			currentRow.add(field.toString());
			rows.add(currentRow);
		}

		return rows.stream()
		           .filter(row -> row.stream().anyMatch(cell -> !cell.isBlank()))
		           .toList();
	}

	private CsvImporter()
	{
	}
}