package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "copy-sheet", mixinStandardHelpOptions = true, description = "Copy a sheet to a new name.")
public final class CopySheetCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "Source sheet name")
	private String sourceSheet;
	@Parameters(index = "2", description = "New sheet name")
	private String targetName;

	public CopySheetCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.copySheet(file, sourceSheet, targetName).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}