package de.gupta.xl.domain;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public sealed interface CellValue
		permits CellValue.Str, CellValue.Num, CellValue.Bool,
		CellValue.Date, CellValue.Formula, CellValue.Empty
{
	static CellValue infer(final String raw)
	{
		if (raw == null)
		{
			return new Empty();
		}
		if (raw.equalsIgnoreCase("true"))
		{
			return new Bool(true);
		}
		if (raw.equalsIgnoreCase("false"))
		{
			return new Bool(false);
		}
		try
		{
			return new Date(LocalDate.parse(raw));
		}
		catch (DateTimeParseException ignored)
		{
		}
		try
		{
			return new Num(Double.parseDouble(raw));
		}
		catch (NumberFormatException ignored)
		{
		}
		return new Str(raw);
	}

	default String displayValue()
	{
		return switch (this)
		{
			case Str(var string) -> string;
			case Num(var number) -> number % 1 == 0
					? String.valueOf((long) number)
					: String.valueOf(number);
			case Bool(var bool) -> String.valueOf(bool);
			case Date(var date) -> date.toString();
			case Formula(var expression) -> expression;
			case Empty() -> "";
		};
	}

	record Str(String value) implements CellValue
	{
	}

	record Num(double value) implements CellValue
	{
	}

	record Bool(boolean value) implements CellValue
	{
	}

	record Date(LocalDate value) implements CellValue
	{
	}

	record Formula(String expression) implements CellValue
	{
	}

	record Empty() implements CellValue
	{
	}
}