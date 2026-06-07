package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
		name = "tab-color",
		mixinStandardHelpOptions = true,
		description = {
				"Set the tab color of a sheet.",
				"Color is a 6-digit hex RGB value without leading # (e.g. 70AD47 for green)."
		}
)
public final class TabColorCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "Sheet name")
	private String sheet;
	@Parameters(index = "2", description = "Hex RGB color (e.g. 70AD47)")
	private String hexRgb;

	public TabColorCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.setTabColor(file, sheet, hexRgb).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}