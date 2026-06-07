package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.adapter.cli.command.support.CellValueFormatter;
import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "read-row", mixinStandardHelpOptions = true,
		description = "Read an entire row and print as a single TSV line.")
public final class ReadRowCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "1-based row number")
	private String row;
	@Option(names = "--typed", description = "Prefix each value with its type token")
	private boolean typed;

	public ReadRowCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.readRow(file, sheet, row).fold(
				values ->
				{
					System.out.println(values.stream()
					                         .map(typed ? CellValueFormatter::typed : CellValueFormatter::raw)
					                         .collect(Collectors.joining("\t")));
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