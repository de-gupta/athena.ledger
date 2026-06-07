package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "create", mixinStandardHelpOptions = true, description = "Create a new empty workbook.")
public final class CreateCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Option(names = "--overwrite", description = "Overwrite if file already exists")
	private boolean overwrite;

	public CreateCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.createWorkbook(file, overwrite).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}