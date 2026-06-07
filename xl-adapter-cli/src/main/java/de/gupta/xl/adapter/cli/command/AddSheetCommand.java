package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "add-sheet", mixinStandardHelpOptions = true, description = "Add a new blank sheet.")
public final class AddSheetCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "New sheet name")
	private String sheetName;

	public AddSheetCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.addSheet(file, sheetName).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}