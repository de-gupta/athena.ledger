package de.gupta.xl.application.transfer;

import de.gupta.xl.domain.CellGrid;

import java.nio.file.Path;

public record WriteRangeRequest(
		Path file,
		String sheet,
		String startCell,
		CellGrid grid,
		boolean overwrite
)
{
}