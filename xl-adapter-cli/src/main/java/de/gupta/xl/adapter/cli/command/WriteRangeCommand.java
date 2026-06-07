package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.transfer.WriteRangeRequest;
import de.gupta.xl.domain.CellGrid;
import de.gupta.xl.domain.CellValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
		name = "write-range",
		mixinStandardHelpOptions = true,
		description = {
				"Write a table of values starting at a cell. Reads TSV (tab-separated) rows from stdin.",
				"Each line is a row; tabs separate columns. Type inference is applied per cell.",
				"Without --overwrite, existing non-empty cells are preserved."
		}
)
public final class WriteRangeCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "Sheet name")
	private String sheet;
	@Parameters(index = "2", description = "Top-left cell of the target range (e.g. A1)")
	private String startCell;
	@Option(names = "--overwrite", description = "Overwrite existing non-empty cells (default: preserve)")
	private boolean overwrite;

	public WriteRangeCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var grid = CellGrid.of(readRowsFromStdin());
		var request = new WriteRangeRequest(file, sheet, startCell, grid, overwrite);
		return facade.writeRange(request).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}

	private static List<List<CellValue>> readRowsFromStdin()
	{
		return new BufferedReader(new InputStreamReader(System.in)).lines()
		                                                           .map(line -> Arrays.stream(line.split("\t", -1))
		                                                                              .map(CellValue::infer)
		                                                                              .toList())
		                                                           .toList();
	}
}