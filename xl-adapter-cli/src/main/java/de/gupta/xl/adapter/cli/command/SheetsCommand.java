package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "sheets", mixinStandardHelpOptions = true, description = "List all sheets with row counts.")
public final class SheetsCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;

	public SheetsCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.listSheets(file).fold(
				summaries ->
				{
					summaries.forEach(summary ->
							System.out.printf("%-10s  (%d rows)%n", summary.name(), summary.rowCount()));
					return 0;
				},
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}