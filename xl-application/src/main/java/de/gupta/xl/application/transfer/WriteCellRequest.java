package de.gupta.xl.application.transfer;

import de.gupta.xl.domain.CellValue;

import java.nio.file.Path;

public record WriteCellRequest(Path file, String sheet, String cellReference, CellValue value)
{
}
