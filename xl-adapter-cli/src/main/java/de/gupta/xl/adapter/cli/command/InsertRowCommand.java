package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "insert-row", mixinStandardHelpOptions = true,
		description = "Insert a blank row at the given position, shifting existing rows down.")
public final class InsertRowCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "1-based row number to insert at")
	private String row;

	public InsertRowCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.insertRow(file, sheet, row).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}