package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "find-col", mixinStandardHelpOptions = true,
		description = {"Find the column letter of a named header in the first row of a sheet.",
				"Outputs the column letter (e.g. N) to stdout. Useful for header-addressed scripting."})
public final class FindColumnCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Header value to search for (case-sensitive)")
	private String header;

	public FindColumnCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.findColumn(file, sheet, header).fold(
				column ->
				{
					System.out.print(column);
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