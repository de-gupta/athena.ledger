package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.adapter.cli.command.support.CellValueFormatter;
import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
		name = "read-range",
		mixinStandardHelpOptions = true,
		description = {
				"Read a rectangular range of cells and print as TSV (tab-separated values).",
				"Each row is a line; columns are separated by tabs.",
				"Empty cells produce empty fields. Output is compatible with write-range stdin."
		}
)
public final class ReadRangeCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "Sheet name")
	private String sheet;
	@Parameters(index = "2", description = "Top-left cell (e.g. A1)")
	private String fromCell;
	@Parameters(index = "3", description = "Bottom-right cell (e.g. C5)")
	private String toCell;
	@Option(names = "--typed", description = "Prefix each value with its type token (e.g. STR:, NUM:)")
	private boolean typed;

	public ReadRangeCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.readRange(file, sheet, fromCell, toCell).fold(
				grid ->
				{
					grid.rows().forEach(row ->
							System.out.println(row.stream()
							                      .map(typed ? CellValueFormatter::typed :
														  CellValueFormatter::raw)
							                      .collect(Collectors.joining("\t"))));
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