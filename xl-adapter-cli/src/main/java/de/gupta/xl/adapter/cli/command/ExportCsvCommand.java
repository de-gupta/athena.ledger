package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "export-csv", mixinStandardHelpOptions = true,
		description = {"Export a full sheet to a CSV file. Inverse of import-csv.",
				"Sheet bounds are discovered automatically. RFC 4180 quoting is applied."})
public final class ExportCsvCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Output CSV file path")
	private Path csvFile;
	@Option(names = "--delimiter", defaultValue = ",",
			description = "Field separator (default: comma). Use ';' for European CSVs or '\\t' for TSV.")
	private String delimiter;

	public ExportCsvCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var delimiterChar = delimiter == null || delimiter.isEmpty() ? ','
				: "\\t".equals(delimiter) ? '\t'
				  : delimiter.charAt(0);
		return facade.exportCsv(file, sheet, csvFile, delimiterChar).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}
}