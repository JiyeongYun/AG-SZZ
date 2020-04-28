package hgu.csee.isel.alinew.szz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class AGSZZRunner {
	private String GIT_URL;
	private String issueKeyFilePath;
	private boolean debug;
	private boolean help;

	public static void main(String[] args) {
		AGSZZRunner agSZZRunner = new AGSZZRunner();
		try {
			
			agSZZRunner.run(args);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Definition Stage
	private Options createOptions() {
		Options options = new Options();

		options.addOption(Option.builder("u")
								.longOpt("url")
								.desc("Git URL (ex. https://github.com/SukJinKim/AG-SZZ)")
								.hasArg()
								.required(true)
								.build());

		options.addOption(Option.builder("b")
								.longOpt("bugFix")
								.desc("Path of file that has bug fix issue keys")
								.hasArg()
								.required(true)
								.build());

		options.addOption(Option.builder("d")
								.longOpt("debug")
								.desc("Debug Mode")
								.build());

		options.addOption(Option.builder("h")
								.longOpt("help")
								.desc("Help")
								.build());

		return options;
	}

	// Parsing Stage
	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);

			GIT_URL = cmd.getOptionValue('u');
			issueKeyFilePath = cmd.getOptionValue('b');
			debug = cmd.hasOption('d');
			help = cmd.hasOption('h');

		} catch (ParseException e) {
			printHelp(options);
			return false;
		}

		return true;
	}

	// Interrogation Stage
	private void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();

		String header = "AG-SZZ finds lines that introduce bug for given BFC.\n\n";
		String footer = "\nPlease report issues at https://github.com/SukJinKim/AG-SZZ\n\n";

		formatter.printHelp("AGSZZ", header, options, footer, true);
	}

	private void run(String[] args) throws IOException {
		Options options = createOptions();

		if (parseOptions(options, args)) {
			if (help) {
				printHelp(options);
				return;
			}

			// Input Info
			System.out.println("\nInput Info");
			System.out.println("\tGIT URL : " + GIT_URL);
			System.out.println("\tIssue Key File path : " + issueKeyFilePath);
			System.out.println("\tDebug mode : " + debug);
			
			AGSZZ agSZZ = new AGSZZ(GIT_URL, issueKeyFilePath, debug);
			agSZZ.run();

		}
	}
}
