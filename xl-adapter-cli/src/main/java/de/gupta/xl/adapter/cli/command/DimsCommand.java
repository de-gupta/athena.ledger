package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "dims", mixinStandardHelpOptions = true,
		description = {"Print the populated range of a sheet as FROM:TO (e.g. A1:E7).",
				"No trailing newline — clean for use in $(...). Empty sheet is an error."})
public final class DimsCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;

	public DimsCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.dims(file, sheet).fold(
				range ->
				{
					System.out.print(range);
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