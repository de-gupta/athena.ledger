package de.gupta.xl.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CellRangeReference#of")
final class CellRangeReferenceTest
{
	@Test
	@DisplayName("equals two ranges with the same corners")
	void equalsTwoRangesWithTheSameCorners()
	{
		assertThat(CellRangeReference.of("A1", "C3"))
				.as("equal ranges")
				.isEqualTo(CellRangeReference.of("A1", "C3"));
	}

	@Nested
	@DisplayName("when corners are valid")
	final class WhenCornersAreValid
	{
		@Test
		@DisplayName("computes row and column counts correctly")
		void computesRowAndColumnCountsCorrectly()
		{
			var range = CellRangeReference.of("B2", "D5");

			assertThat(range.rowCount()).as("row count").isEqualTo(4);
			assertThat(range.columnCount()).as("column count").isEqualTo(3);
		}

		@Test
		@DisplayName("accepts a single-cell range")
		void acceptsASingleCellRange()
		{
			var range = CellRangeReference.of("C3", "C3");

			assertThat(range.rowCount()).as("row count").isEqualTo(1);
			assertThat(range.columnCount()).as("column count").isEqualTo(1);
		}

		@Test
		@DisplayName("exposes topLeft and bottomRight references")
		void exposesTopLeftAndBottomRightReferences()
		{
			var range = CellRangeReference.of("A1", "B2");

			assertThat(range.topLeft()).as("top-left")
			                           .isEqualTo(CellReference.of("A1"));
			assertThat(range.bottomRight()).as("bottom-right")
			                               .isEqualTo(CellReference.of("B2"));
		}
	}

	@Nested
	@DisplayName("when corners are invalid")
	final class WhenCornersAreInvalid
	{
		@Test
		@DisplayName("throws when top-left row is below bottom-right row")
		void throwsWhenTopLeftRowIsBelowBottomRightRow()
		{
			assertThatThrownBy(() -> CellRangeReference.of("A5", "A1"))
					.as("inverted rows")
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("throws when top-left column is to the right of bottom-right column")
		void throwsWhenTopLeftColumnIsToTheRightOfBottomRightColumn()
		{
			assertThatThrownBy(() -> CellRangeReference.of("D1", "A1"))
					.as("inverted columns")
					.isInstanceOf(IllegalArgumentException.class);
		}
	}
}