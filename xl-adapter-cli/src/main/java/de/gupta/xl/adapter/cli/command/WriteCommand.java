package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.transfer.WriteCellRequest;
import de.gupta.xl.domain.CellValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.Callable;

@Command(name = "write", mixinStandardHelpOptions = true, description = "Write a value to a cell.")
public final class WriteCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0", description = "Excel file path")
	private Path file;
	@Parameters(index = "1", description = "Sheet name")
	private String sheet;
	@Parameters(index = "2", description = "Cell reference (e.g. A1)")
	private String cellReference;
	@Parameters(index = "3", description = "Value to write")
	private String value;
	@Option(names = "--type", description = "Cell type: STR, NUM, BOOL, DATE, FORMULA")
	private String type;

	public WriteCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		var cellValue = parseCellValue();
		var request = new WriteCellRequest(file, sheet, cellReference, cellValue);
		return facade.writeCell(request).fold(
				_ -> 0,
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}

	private CellValue parseCellValue()
	{
		if (type == null)
		{
			return CellValue.infer(value);
		}
		return switch (type.toUpperCase())
		{
			case "STR" -> new CellValue.Str(value);
			case "NUM" -> new CellValue.Num(Double.parseDouble(value));
			case "BOOL" -> new CellValue.Bool(Boolean.parseBoolean(value));
			case "DATE" -> new CellValue.Date(LocalDate.parse(value));
			case "FORMULA" -> new CellValue.Formula(value.startsWith("=") ? value.substring(1) : value);
			default -> throw new IllegalArgumentException("Unknown type: " + type);
		};
	}
}