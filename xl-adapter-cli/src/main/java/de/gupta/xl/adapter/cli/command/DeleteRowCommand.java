package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "delete-row", mixinStandardHelpOptions = true,
		description = "Delete a row and shift all rows below it up.")
public final class DeleteRowCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "1-based row number to delete")
	private String row;

	public DeleteRowCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.deleteRow(file, sheet, row).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}