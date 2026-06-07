package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
		name = "move-sheet",
		mixinStandardHelpOptions = true,
		description = "Move a sheet to the given 0-based position in the tab order."
)
public final class MoveSheetCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "Sheet name")
	private String sheetName;
	@Parameters(index = "2", description = "Target 0-based position (0 = first tab)")
	private int position;

	public MoveSheetCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.moveSheet(file, sheetName, position).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}