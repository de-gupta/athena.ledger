package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "find-row", mixinStandardHelpOptions = true,
		description = {"Find the 1-based row number of the first cell in a column that matches a value.",
				"Output is a bare integer — clean for use in $(...). Complement to find-col."})
public final class FindRowCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Column letter or 1-based integer")
	private String column;
	@Parameters(index = "3", description = "Value to search for (plain display string, no TYPE: prefix)")
	private String value;

	public FindRowCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.findRow(file, sheet, column, value).fold(
				row ->
				{
					System.out.print(row);
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