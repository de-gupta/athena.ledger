package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.transfer.RangeStats;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "stats", mixinStandardHelpOptions = true,
		description = {"Compute statistics over the numeric cells in a rectangular range.",
				"Non-numeric cells are counted but excluded from aggregation.",
				"Output: key:value pairs, one per line."})
public final class RangeStatsCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Top-left cell (e.g. A1)")
	private String fromCell;
	@Parameters(index = "3", description = "Bottom-right cell (e.g. E7)")
	private String toCell;

	public RangeStatsCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.rangeStats(file, sheet, fromCell, toCell).fold(
				stats ->
				{
					print(stats);
					return 0;
				},
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}

	private static void print(final RangeStats stats)
	{
		System.out.println("count:" + stats.count());
		System.out.println("numeric:" + stats.numericCount());
		System.out.println("non-numeric:" + stats.nonNumericCount());
		if (stats.hasNumericData())
		{
			System.out.printf("min:%.6g%n", stats.minimum());
			System.out.printf("max:%.6g%n", stats.maximum());
			System.out.printf("mean:%.6g%n", stats.mean());
			System.out.printf("stdev:%.6g%n", stats.standardDeviation());
		}
	}
}