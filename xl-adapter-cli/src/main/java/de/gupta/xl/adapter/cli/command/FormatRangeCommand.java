package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.transfer.CellFormat;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "format-range", mixinStandardHelpOptions = true,
		description = "Apply uniform formatting to a rectangular range of cells.")
public final class FormatRangeCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Top-left cell (e.g. A1)")
	private String fromCell;
	@Parameters(index = "3", description = "Bottom-right cell (e.g. E1)")
	private String toCell;
	@Option(names = "--number-format", description = "Excel number format code (e.g. '0.0000')")
	private String numberFormat;
	@Option(names = "--bold")
	private boolean bold;
	@Option(names = "--italic")
	private boolean italic;
	@Option(names = "--font-color", description = "6-digit hex RGB")
	private String fontColor;
	@Option(names = "--bg-color", description = "6-digit hex RGB background fill")
	private String backgroundColor;

	public FormatRangeCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var format = CellFormat.builder()
		                       .numberFormat(numberFormat)
		                       .bold(bold ? Boolean.TRUE : null)
		                       .italic(italic ? Boolean.TRUE : null)
		                       .fontColor(fontColor)
		                       .backgroundColor(backgroundColor)
		                       .build();
		return facade.formatRange(file, sheet, fromCell, toCell, format).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}