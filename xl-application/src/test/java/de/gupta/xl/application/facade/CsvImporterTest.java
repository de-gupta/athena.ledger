package de.gupta.xl.application.facade;

import de.gupta.xl.domain.CellValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CsvImporter")
final class CsvImporterTest
{
	@Nested
	@DisplayName("toCellGrid from string content")
	final class ToCellGridFromString
	{
		@Test
		@DisplayName("parses header and data rows as separate rows in the grid")
		void parsesHeaderAndDataRowsAsSeparateRowsInTheGrid()
		{
			var csv = "Name,Score\nAlice,95\nBob,87";

			var grid = CsvImporter.toCellGrid(csv, ',');

			assertThat(grid.rowCount()).as("row count").isEqualTo(3);
			assertThat(grid.rows().get(0)).as("header row")
			                              .containsExactly(new CellValue.Str("Name"), new CellValue.Str("Score"));
			assertThat(grid.rows().get(1)).as("data row 1")
			                              .containsExactly(new CellValue.Str("Alice"), new CellValue.Num(95));
			assertThat(grid.rows().get(2)).as("data row 2")
			                              .containsExactly(new CellValue.Str("Bob"), new CellValue.Num(87));
		}

		@Test
		@DisplayName("applies type inference to each cell")
		void appliesTypeInferenceToEachCell()
		{
			var csv = "label,amount,active,date\nhello,42.5,true,2026-06-07";

			var grid = CsvImporter.toCellGrid(csv, ',');

			var dataRow = grid.rows().get(1);
			assertThat(dataRow.get(0)).as("string").isInstanceOf(CellValue.Str.class);
			assertThat(dataRow.get(1)).as("number").isEqualTo(new CellValue.Num(42.5));
			assertThat(dataRow.get(2)).as("boolean").isEqualTo(new CellValue.Bool(true));
			assertThat(dataRow.get(3)).as("date").isInstanceOf(CellValue.Date.class);
		}

		@Test
		@DisplayName("handles quoted fields containing commas")
		void handlesQuotedFieldsContainingCommas()
		{
			var csv = "\"Smith, John\",42";

			var grid = CsvImporter.toCellGrid(csv, ',');

			assertThat(grid.rows().getFirst().getFirst()).as("quoted field")
			                                             .isEqualTo(new CellValue.Str("Smith, John"));
		}

		@Test
		@DisplayName("handles escaped double-quotes inside quoted fields")
		void handlesEscapedDoubleQuotesInsideQuotedFields()
		{
			var csv = "\"say \"\"hello\"\"\",ok";

			var grid = CsvImporter.toCellGrid(csv, ',');

			assertThat(grid.rows().getFirst().getFirst()).as("embedded quotes")
			                                             .isEqualTo(new CellValue.Str("say \"hello\""));
		}

		@Test
		@DisplayName("supports semicolon delimiter")
		void supportsSemicolonDelimiter()
		{
			var csv = "Name;Score\nAlice;95";

			var grid = CsvImporter.toCellGrid(csv, ';');

			assertThat(grid.rows().get(0)).as("header")
			                              .containsExactly(new CellValue.Str("Name"), new CellValue.Str("Score"));
			assertThat(grid.rows().get(1).get(1)).as("numeric cell").isEqualTo(new CellValue.Num(95));
		}

		@Test
		@DisplayName("supports tab delimiter (TSV input)")
		void supportsTabDelimiter()
		{
			var csv = "A\tB\n1\t2";

			var grid = CsvImporter.toCellGrid(csv, '\t');

			assertThat(grid.rowCount()).as("row count").isEqualTo(2);
			assertThat(grid.rows().get(1).getFirst()).as("first cell").isEqualTo(new CellValue.Num(1));
		}

		@Test
		@DisplayName("handles CRLF line endings")
		void handlesCrlfLineEndings()
		{
			var csv = "A,B\r\n1,2\r\n3,4";

			var grid = CsvImporter.toCellGrid(csv, ',');

			assertThat(grid.rowCount()).as("row count").isEqualTo(3);
		}

		@Test
		@DisplayName("skips blank lines")
		void skipsBlankLines()
		{
			var csv = "A,B\n1,2\n\n3,4";

			var grid = CsvImporter.toCellGrid(csv, ',');

			assertThat(grid.rowCount()).as("row count ignoring blank line").isEqualTo(3);
		}

		@Test
		@DisplayName("returns empty grid for blank content")
		void returnsEmptyGridForBlankContent()
		{
			var grid = CsvImporter.toCellGrid("", ',');

			assertThat(grid.isEmpty()).as("empty grid").isTrue();
		}
	}
}