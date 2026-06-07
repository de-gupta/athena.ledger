package de.gupta.xl.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RowReference#of")
final class RowReferenceTest
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("validCases")
	@DisplayName("converts 1-based row number to 0-based index")
	void converts1BasedRowNumberTo0BasedIndex(final String as, final String input, final int expectedIndex)
	{
		assertThat(RowReference.of(input).index())
				.as("index for %s", as)
				.isEqualTo(expectedIndex);
	}

	@Test
	@DisplayName("throws for row zero")
	void throwsForRowZero()
	{
		assertThatThrownBy(() -> RowReference.of("0"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("throws for negative row")
	void throwsForNegativeRow()
	{
		assertThatThrownBy(() -> RowReference.of("-1"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("throws for non-numeric input")
	void throwsForNonNumericInput()
	{
		assertThatThrownBy(() -> RowReference.of("A"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("throws for null")
	void throwsForNull()
	{
		assertThatThrownBy(() -> RowReference.of(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static Stream<Arguments> validCases()
	{
		return Stream.of(
				Arguments.of("row 1 → index 0", "1", 0),
				Arguments.of("row 2 → index 1", "2", 1),
				Arguments.of("row 100 → index 99", "100", 99)
		);
	}
}