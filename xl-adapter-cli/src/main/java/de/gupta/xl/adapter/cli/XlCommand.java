package de.gupta.xl.adapter.cli;

import de.gupta.xl.adapter.cli.command.*;
import de.gupta.xl.adapter.poi.PoiWorkbookRepository;
import de.gupta.xl.application.facade.XlFacadeImpl;
import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.service.WorkbookService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

@Command(name = "xl", mixinStandardHelpOptions = true)
public final class XlCommand implements Runnable
{
	@Spec
	private CommandLine.Model.CommandSpec specification;

	public static void main(final String[] arguments)
	{
		System.exit(buildCommandLine().execute(arguments));
	}

	static CommandLine buildCommandLine()
	{
		var facade = buildFacade();
		return new CommandLine(new XlCommand())
				.addSubcommand(new SheetsCommand(facade))
				.addSubcommand(new ReadCommand(facade))
				.addSubcommand(new ReadRangeCommand(facade))
				.addSubcommand(new WriteCommand(facade))
				.addSubcommand(new WriteRangeCommand(facade))
				.addSubcommand(new CreateCommand(facade))
				.addSubcommand(new AddSheetCommand(facade))
				.addSubcommand(new RenameSheetCommand(facade))
				.addSubcommand(new DeleteSheetCommand(facade))
				.addSubcommand(new CopySheetCommand(facade))
				.addSubcommand(new MoveSheetCommand(facade))
				.addSubcommand(new DeleteColumnCommand(facade))
				.addSubcommand(new TabColorCommand(facade))
				.addSubcommand(new ReadRowCommand(facade))
				.addSubcommand(new ReadColumnCommand(facade))
				.addSubcommand(new EvaluateCellCommand(facade))
				.addSubcommand(new InsertRowCommand(facade))
				.addSubcommand(new DeleteRowCommand(facade))
				.addSubcommand(new SetColumnWidthCommand(facade))
				.addSubcommand(new AutoFitCommand(facade))
				.addSubcommand(new ImportCsvCommand(facade))
				.addSubcommand(new FindColumnCommand(facade))
				.addSubcommand(new InsertColumnCommand(facade))
				.addSubcommand(new AppendColumnCommand(facade))
				.addSubcommand(new RangeStatsCommand(facade))
				.addSubcommand(new FindRowCommand(facade))
				.addSubcommand(new DimsCommand(facade))
				.addSubcommand(new ExportCsvCommand(facade));
	}

	@Override
	public void run()
	{
		specification.commandLine().usage(System.out);
	}

	private static XlFacade buildFacade()
	{
		var repository = new PoiWorkbookRepository();
		var service = new WorkbookService(repository);
		return new XlFacadeImpl(service);
	}
}