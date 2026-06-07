package de.gupta.xl.adapter.cli.command.support;

import de.gupta.xl.domain.CellValue;

public final class CellValueFormatter
{
	public static String raw(final CellValue value)
	{
		return value.displayValue();
	}

	public static String typed(final CellValue value)
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

	private CellValueFormatter()
	{
	}
}