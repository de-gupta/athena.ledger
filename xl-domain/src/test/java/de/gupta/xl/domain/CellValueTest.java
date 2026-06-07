package de.gupta.xl.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CellValue#infer")
final class CellValueTest
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("inferCases")
	@DisplayName("infers the correct type from raw string input")
	void infersTheCorrectTypeFromRawStringInput(final String as, final String input, final CellValue expected)
	{
		var result = CellValue.infer(input);

		assertThat(result).as("inferred type for %s", as).isEqualTo(expected);
	}

	private static Stream<Arguments> inferCases()
	{
		return Stream.of(
				Arguments.of("true lowercase", "true", new CellValue.Bool(true)),
				Arguments.of("TRUE uppercase", "TRUE", new CellValue.Bool(true)),
				Arguments.of("false lowercase", "false", new CellValue.Bool(false)),
				Arguments.of("FALSE uppercase", "FALSE", new CellValue.Bool(false)),
				Arguments.of("ISO date", "2026-06-07", new CellValue.Date(LocalDate.of(2026, 6, 7))),
				Arguments.of("integer number", "42", new CellValue.Num(42.0)),
				Arguments.of("decimal number", "42.5", new CellValue.Num(42.5)),
				Arguments.of("scientific notation", "1e5", new CellValue.Num(100000.0)),
				Arguments.of("negative number", "-7.3", new CellValue.Num(-7.3)),
				Arguments.of("plain string", "hello", new CellValue.Str("hello")),
				Arguments.of("invalid date month", "2026-13-01", new CellValue.Str("2026-13-01")),
				Arguments.of("invalid date format", "1.2.3", new CellValue.Str("1.2.3")),
				Arguments.of("mixed alphanumeric", "A1B2", new CellValue.Str("A1B2"))
		);
	}

	@Nested
	@DisplayName("when input is null")
	final class WhenInputIsNull
	{
		@Test
		@DisplayName("returns Empty")
		void returnsEmpty()
		{
			var result = CellValue.infer(null);

			assertThat(result).as("null input").isInstanceOf(CellValue.Empty.class);
		}
	}
}