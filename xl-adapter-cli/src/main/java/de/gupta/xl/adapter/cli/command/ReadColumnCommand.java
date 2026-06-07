package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.adapter.cli.command.support.CellValueFormatter;
import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "read-col", mixinStandardHelpOptions = true,
		description = "Read an entire column and print one value per line.")
public final class ReadColumnCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Column letter or 1-based integer (e.g. A or 1)")
	private String column;
	@Option(names = "--typed", description = "Prefix each value with its type token")
	private boolean typed;

	public ReadColumnCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.readColumn(file, sheet, column).fold(
				values ->
				{
					values.stream()
					      .map(typed ? CellValueFormatter::typed : CellValueFormatter::raw)
					      .forEach(System.out::println);
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