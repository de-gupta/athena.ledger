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

@Command(name = "append-col", mixinStandardHelpOptions = true,
		description = {"Append a new column after the last occupied column in the sheet.",
				"Reads one cell value per line from stdin. Type inference is applied to each value."})
public final class AppendColumnCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;

	public AppendColumnCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var values = readValuesFromStdin();
		return facade.appendColumn(file, sheet, values).fold(
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