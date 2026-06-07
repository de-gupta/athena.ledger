package de.gupta.xl.adapter.cli.command;

import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.domain.CellValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "evaluate", mixinStandardHelpOptions = true,
		description = {"Read a cell's computed value. For formula cells, evaluates the formula.",
				"For non-formula cells, behaves identically to 'read'. Output: TYPE:value."})
public final class EvaluateCellCommand implements Callable<Integer>
{
	private final XlFacade facade;
	@Parameters(index = "0")
	private Path file;
	@Parameters(index = "1")
	private String sheet;
	@Parameters(index = "2", description = "Cell reference (e.g. A1)")
	private String cellReference;

	public EvaluateCellCommand(final XlFacade facade)
	{
		this.facade = facade;
	}

	@Override
	public Integer call()
	{
		return facade.evaluateCell(file, sheet, cellReference).fold(
				value ->
				{
					System.out.println(format(value));
					return 0;
				},
				caught ->
				{
					System.err.println(caught.getMessage());
					return 1;
				}
		);
	}

	private static String format(final CellValue value)
	{
		return switch (value)
		{
			case CellValue.Str(var string) -> "STR:" + string;
			case CellValue.Num(var number) -> "NUM:" + number;
			case CellValue.Bool(var bool) -> "BOOL:" + bool;
			case CellValue.Date(var date) -> "DATE:" + date;
			case CellValue.Formula(var expression) -> "FORMULA:" + expression;
			case CellValue.Empty() -> "EMPTY:";
		};
	}
}