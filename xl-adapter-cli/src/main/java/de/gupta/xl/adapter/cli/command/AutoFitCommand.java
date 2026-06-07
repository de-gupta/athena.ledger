package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "auto-fit", mixinStandardHelpOptions = true,
		description = {"Auto-fit column widths to their content.",
				"If a column is given, fits only that column. Otherwise, fits all columns."})
public final class AutoFitCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", arity = "0..1",
			description = "Column letter or 1-based integer (omit to fit all columns)")
	private String column;

	public AutoFitCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var result = column == null
				? facade.autoFitAllColumns(file, sheet)
				: facade.autoFitColumn(file, sheet, column);
		return result.fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}