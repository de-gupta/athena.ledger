package de.gupta.xl.adapter.cli;

import de.gupta.xl.adapter.cli.command.AddSheetCommand;
import de.gupta.xl.adapter.cli.command.CopySheetCommand;
import de.gupta.xl.adapter.cli.command.CreateCommand;
import de.gupta.xl.adapter.cli.command.DeleteSheetCommand;
import de.gupta.xl.adapter.cli.command.ReadCommand;
import de.gupta.xl.adapter.cli.command.SheetsCommand;
import de.gupta.xl.adapter.cli.command.WriteCommand;
import de.gupta.xl.adapter.poi.PoiWorkbookRepository;
import de.gupta.xl.application.facade.XlFacadeImpl;
import de.gupta.xl.application.port.in.XlFacade;
import de.gupta.xl.application.service.WorkbookService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "xl", mixinStandardHelpOptions = true)
public final class XlCommand implements Runnable
{
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
				.addSubcommand(new WriteCommand(facade))
				.addSubcommand(new CreateCommand(facade))
				.addSubcommand(new AddSheetCommand(facade))
				.addSubcommand(new DeleteSheetCommand(facade))
				.addSubcommand(new CopySheetCommand(facade));
	}

	@Override
	public void run()
	{
		CommandLine.usage(this, System.out);
	}

	private static XlFacade buildFacade()
	{
		var repository = new PoiWorkbookRepository();
		var service = new WorkbookService(repository);
		return new XlFacadeImpl(service);
	}
}