package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.domain.CellValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "insert-col", mixinStandardHelpOptions = true,
		description = {"Insert a new blank column at the given position, shifting existing columns right.",
				"Reads one cell value per line from stdin and writes into the new column starting at row 1.",
				"Type inference is applied to each value."})
public final class InsertColumnCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Column letter (A) or 1-based integer (1)")
	private String column;

	public InsertColumnCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var values = readValuesFromStdin();
		return facade.insertColumn(file, sheet, column, values).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}

	private static List<CellValue> readValuesFromStdin()
	{
		return new BufferedReader(new InputStreamReader(System.in)).lines()
		                                                           .map(CellValue::infer)
		                                                           .toList();
	}
}