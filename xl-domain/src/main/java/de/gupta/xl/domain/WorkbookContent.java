package de.gupta.xl.domain;

import java.nio.file.Path;
import java.util.List;

public record WorkbookContent(Path path, List<Sheet> sheets)
{
}
