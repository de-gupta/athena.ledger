package de.gupta.xl.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CellReference#of")
final class CellReferenceTest
{
	@Test
	@DisplayName("equals two references with same column and row")
	void equalsTwoReferencesWithSameColumnAndRow()
	{
		var first = CellReference.of("B3");
		var second = CellReference.of("b3");

		assertThat(first).as("B3 equals b3").isEqualTo(second);
	}

	@Nested
	@DisplayName("when notation is valid")
	final class WhenNotationIsValid
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("validNotationCases")
		@DisplayName("parses column and row indices correctly")
		void parsesColumnAndRowIndicesCorrectly(final String as, final ValidNotationCase notationCase)
		{
			var reference = CellReference.of(notationCase.input());

			assertThat(reference.columnIndex())
					.as("column index for %s", notationCase.input())
					.isEqualTo(notationCase.expectedColumn());
			assertThat(reference.rowIndex())
					.as("row index for %s", notationCase.input())
					.isEqualTo(notationCase.expectedRow());
		}

		private static Stream<Arguments> validNotationCases()
		{
			return Stream.of(
					new ValidNotationCase("A1 → column 0, row 0", "A1", 0, 0),
					new ValidNotationCase("a1 lowercase → same", "a1", 0, 0),
					new ValidNotationCase("B3 → column 1, row 2", "B3", 1, 2),
					new ValidNotationCase("Z1 → column 25, row 0", "Z1", 25, 0),
					new ValidNotationCase("AA1 → column 26, row 0", "AA1", 26, 0),
					new ValidNotationCase("AB1 → column 27, row 0", "AB1", 27, 0),
					new ValidNotationCase("ZZ99 → column 701, row 98", "ZZ99", 701, 98)
			).map(testCase -> Arguments.of(testCase.as(), testCase));
		}

		private record ValidNotationCase(String as, String input, int expectedColumn, int expectedRow)
		{
		}
	}

	@Nested
	@DisplayName("when notation is invalid")
	final class WhenNotationIsInvalid
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("invalidNotationCases")
		@DisplayName("throws IllegalArgumentException")
		void throwsIllegalArgumentException(final String as, final String input)
		{
			assertThatThrownBy(() -> CellReference.of(input))
					.as("parsing %s", as)
					.isInstanceOf(IllegalArgumentException.class);
		}

		private static Stream<Arguments> invalidNotationCases()
		{
			return Stream.of(
					Arguments.of("empty string", ""),
					Arguments.of("digits only", "123"),
					Arguments.of("letters only", "ABC"),
					Arguments.of("digits before letters", "1A"),
					Arguments.of("row zero", "A0"),
					Arguments.of("null", null)
			);
		}
	}
}