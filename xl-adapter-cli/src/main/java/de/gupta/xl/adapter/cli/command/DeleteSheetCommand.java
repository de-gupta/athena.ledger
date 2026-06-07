package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "delete-sheet", mixinStandardHelpOptions = true, description = "Delete a sheet by name.")
public final class DeleteSheetCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "Sheet name to delete")
	private String sheetName;

	public DeleteSheetCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.deleteSheet(file, sheetName).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}