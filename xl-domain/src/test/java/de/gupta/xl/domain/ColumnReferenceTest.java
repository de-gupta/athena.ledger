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

@DisplayName("ColumnReference#of")
final class ColumnReferenceTest
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("validNotationCases")
	@DisplayName("parses to the correct 0-based index")
	void parsesToCorrect0BasedIndex(final String as, final String input, final int expectedIndex)
	{
		assertThat(ColumnReference.of(input).index())
				.as("index for %s", as)
				.isEqualTo(expectedIndex);
	}

	private static Stream<Arguments> validNotationCases()
	{
		return Stream.of(
				Arguments.of("letter A", "A", 0),
				Arguments.of("letter B", "B", 1),
				Arguments.of("letter Z", "Z", 25),
				Arguments.of("letter AA", "AA", 26),
				Arguments.of("lowercase a", "a", 0),
				Arguments.of("1-based 1", "1", 0),
				Arguments.of("1-based 2", "2", 1),
				Arguments.of("1-based 26", "26", 25),
				Arguments.of("1-based 27", "27", 26),
				Arguments.of("A equals 1", "A", ColumnReference.of("1").index())
		);
	}

	@Nested
	@DisplayName("when notation is invalid")
	final class WhenNotationIsInvalid
	{
		@Test
		@DisplayName("throws for null")
		void throwsForNull()
		{
			assertThatThrownBy(() -> ColumnReference.of(null))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("throws for zero-based integer zero")
		void throwsForIntegerZero()
		{
			assertThatThrownBy(() -> ColumnReference.of("0"))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("throws for mixed alphanumeric")
		void throwsForMixedAlphanumeric()
		{
			assertThatThrownBy(() -> ColumnReference.of("A1"))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}
}