package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.transfer.CellFormat;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "format-cell", mixinStandardHelpOptions = true,
		description = "Apply formatting to a single cell. Existing style is preserved for unspecified options.")
public final class FormatCellCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Cell reference (e.g. A1)")
	private String cell;
	@Option(names = "--number-format", description = "Excel number format code (e.g. '0.0000', '$#,##0.00')")
	private String numberFormat;
	@Option(names = "--bold", description = "Make text bold")
	private boolean bold;
	@Option(names = "--italic", description = "Make text italic")
	private boolean italic;
	@Option(names = "--font-color", description = "Font colour as 6-digit hex RGB (e.g. FF0000)")
	private String fontColor;
	@Option(names = "--bg-color", description = "Background fill colour as 6-digit hex RGB")
	private String backgroundColor;

	public FormatCellCommand(final XlFacade facade)
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
		return facade.formatCell(file, sheet, cell, format).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}