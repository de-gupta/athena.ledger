package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.transfer.BatchOperation;
import de.gupta.xl.application.transfer.CellFormat;
import de.gupta.xl.domain.CellValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "batch", mixinStandardHelpOptions = true,
		description = {
				"Apply multiple operations to a workbook in a single file open/save cycle.",
				"Reads one operation per line from stdin. Lines starting with # are comments.",
				"",
				"Supported operations:",
				"  write <sheet> <cell> <value> [--type TYPE]",
				"  format-cell <sheet> <cell> [--bold] [--italic] [--number-format FMT] [--font-color HEX] [--bg-color HEX]",
				"  format-range <sheet> <from> <to> [same options as format-cell]",
				"  freeze-panes <sheet> <frozen-rows> <frozen-cols>",
				"  tab-color <sheet> <hex>",
				"  add-sheet <name>"
		})
public final class BatchCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Target Excel file")
	private Path file;

	static Optional<BatchOperation> parseLine(final String line)
	{
		var trimmed = line.strip();
		if (trimmed.isEmpty() || trimmed.startsWith("#"))
		{
			return Optional.empty();
		}
		var tokens = tokenize(trimmed);
		if (tokens.isEmpty())
		{
			return Optional.empty();
		}
		return Optional.ofNullable(switch (tokens.getFirst())
		{
			case "write" -> parseWrite(tokens);
			case "format-cell" -> parseFormatCell(tokens);
			case "format-range" -> parseFormatRange(tokens);
			case "freeze-panes" -> parseFreezePanes(tokens);
			case "tab-color" -> parseTabColor(tokens);
			case "add-sheet" -> parseAddSheet(tokens);
			default -> null;
		});
	}

	public BatchCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var operations = new ArrayList<BatchOperation>();
		new BufferedReader(new InputStreamReader(System.in)).lines()
		                                                    .map(BatchCommand::parseLine)
		                                                    .filter(Optional::isPresent)
		                                                    .map(Optional::get)
		                                                    .forEach(operations::add);
		if (operations.isEmpty())
		{
			return 0;
		}
		return facade.batch(file, operations).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}

	private static BatchOperation parseWrite(final List<String> tokens)
	{
		if (tokens.size() < 4)
		{
			return null;
		}
		var sheet = tokens.get(1);
		var cell = tokens.get(2);
		var raw = tokens.get(3);
		var type = optionValue(tokens, "--type");
		var value = type != null ? parseTyped(raw, type) : CellValue.infer(raw);
		return new BatchOperation.WriteCell(sheet, cell, value);
	}

	private static CellValue parseTyped(final String raw, final String type)
	{
		return switch (type.toUpperCase())
		{
			case "STR" -> new CellValue.Str(raw);
			case "NUM" -> new CellValue.Num(Double.parseDouble(raw));
			case "BOOL" -> new CellValue.Bool(Boolean.parseBoolean(raw));
			case "DATE" -> new CellValue.Date(java.time.LocalDate.parse(raw));
			case "FORMULA" -> new CellValue.Formula(raw.startsWith("=") ? raw.substring(1) : raw);
			default -> CellValue.infer(raw);
		};
	}

	private static BatchOperation parseFormatCell(final List<String> tokens)
	{
		if (tokens.size() < 3)
		{
			return null;
		}
		return new BatchOperation.FormatCell(tokens.get(1), tokens.get(2), buildFormat(tokens));
	}

	private static BatchOperation parseFormatRange(final List<String> tokens)
	{
		if (tokens.size() < 4)
		{
			return null;
		}
		return new BatchOperation.FormatRange(tokens.get(1), tokens.get(2), tokens.get(3), buildFormat(tokens));
	}

	private static BatchOperation parseFreezePanes(final List<String> tokens)
	{
		if (tokens.size() < 4)
		{
			return null;
		}
		try
		{
			return new BatchOperation.FreezePanes(
					tokens.get(1),
					Integer.parseInt(tokens.get(2)),
					Integer.parseInt(tokens.get(3)));
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
	}

	private static BatchOperation parseTabColor(final List<String> tokens)
	{
		if (tokens.size() < 3)
		{
			return null;
		}
		return new BatchOperation.SetTabColor(tokens.get(1), tokens.get(2));
	}

	private static BatchOperation parseAddSheet(final List<String> tokens)
	{
		if (tokens.size() < 2)
		{
			return null;
		}
		return new BatchOperation.AddSheet(tokens.get(1));
	}

	private static CellFormat buildFormat(final List<String> tokens)
	{
		return CellFormat.builder()
		                 .numberFormat(optionValue(tokens, "--number-format"))
		                 .bold(hasFlag(tokens, "--bold") ? true : null)
		                 .italic(hasFlag(tokens, "--italic") ? true : null)
		                 .fontColor(optionValue(tokens, "--font-color"))
		                 .backgroundColor(optionValue(tokens, "--bg-color"))
		                 .build();
	}

	private static boolean hasFlag(final List<String> tokens, final String flag)
	{
		return tokens.contains(flag);
	}

	private static String optionValue(final List<String> tokens, final String option)
	{
		for (var index = 0; index < tokens.size() - 1; index++)
		{
			if (tokens.get(index).equals(option))
			{
				return tokens.get(index + 1);
			}
		}
		return null;
	}

	private static List<String> tokenize(final String line)
	{
		var tokens = new ArrayList<String>();
		var current = new StringBuilder();
		var inQuotes = false;
		for (var index = 0; index < line.length(); index++)
		{
			var character = line.charAt(index);
			if (character == '"')
			{
				inQuotes = !inQuotes;
			}
			else if (character == ' ' && !inQuotes)
			{
				if (!current.isEmpty())
				{
					tokens.add(current.toString());
					current.setLength(0);
				}
			}
			else
			{
				current.append(character);
			}
		}
		if (!current.isEmpty())
		{
			tokens.add(current.toString());
		}
		return tokens;
	}
}