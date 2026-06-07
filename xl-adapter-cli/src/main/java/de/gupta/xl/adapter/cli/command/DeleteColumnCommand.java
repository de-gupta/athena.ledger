package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
		name = "delete-column",
		mixinStandardHelpOptions = true,
		description = {
				"Delete a column and shift remaining columns left.",
				"Column may be specified as a letter (A, B, AA) or a 1-based integer (1, 2)."
		}
)
public final class DeleteColumnCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "Sheet name")
	private String sheet;
	@Parameters(index = "2", description = "Column to delete (e.g. I or 9)")
	private String column;

	public DeleteColumnCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.deleteColumn(file, sheet, column).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}