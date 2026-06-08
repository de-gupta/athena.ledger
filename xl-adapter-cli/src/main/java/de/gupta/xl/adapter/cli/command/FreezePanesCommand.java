package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "freeze-panes", mixinStandardHelpOptions = true,
		description = {"Freeze rows and/or columns in a sheet.",
				"frozen-rows=1 freezes the header row. frozen-cols=0 leaves columns unfrozen."})
public final class FreezePanesCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Number of rows to freeze from the top")
	private int frozenRows;
	@Parameters(index = "3", description = "Number of columns to freeze from the left")
	private int frozenColumns;

	public FreezePanesCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.freezePanes(file, sheet, frozenRows, frozenColumns).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}