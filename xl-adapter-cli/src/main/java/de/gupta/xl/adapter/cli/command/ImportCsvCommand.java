package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
		name = "import-csv",
		mixinStandardHelpOptions = true,
		description = {
				"Import a CSV file into a sheet, starting at a given cell.",
				"All rows including the header are written. Type inference is applied per cell.",
				"Without --overwrite, existing non-empty cells are preserved."
		}
)
public final class ImportCsvCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Target Excel file")
	private Path file;
	@Parameters(index = "1", description = "Target sheet name")
	private String sheet;
	@Parameters(index = "2", description = "CSV file to import")
	private Path csvFile;
	@Option(names = "--start-cell", defaultValue = "A1",
			description = "Top-left cell to start writing at (default: A1)")
	private String startCell;
	@Option(names = "--overwrite",
			description = "Overwrite existing non-empty cells (default: preserve)")
	private boolean overwrite;
	@Option(names = "--delimiter", defaultValue = ",",
			description = "Field delimiter character (default: comma). Use ';' for European CSVs or '\\t' for TSV.")
	private String delimiter;

	public ImportCsvCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var delimiterChar = parseDelimiter(delimiter);
		return facade.importCsv(file, sheet, csvFile, startCell, overwrite, delimiterChar).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}

	private static char parseDelimiter(final String value)
	{
		if ("\\t".equals(value) || "\t".equals(value))
		{
			return '\t';
		}
		if (value == null || value.isEmpty())
		{
			return ',';
		}
		return value.charAt(0);
	}
}