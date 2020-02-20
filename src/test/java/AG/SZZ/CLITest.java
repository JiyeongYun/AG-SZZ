package AG.SZZ;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLITest {

	public static void main(String[] args) {
		try {
			CLITest cli = new CLITest();
			
			System.out.println(System.getProperty("user.dir"));
			
			// Definition Stage
			Options options = new Options();
			options.addOption(Option.builder("b")
									.hasArgs()
									.valueSeparator(' ') // ' ' is delimiter
									.build());

			// Parsing Stage
			CommandLineParser parser = new DefaultParser();
			CommandLine line = parser.parse(options, args);

			String[] fixCommits = line.getOptionValues("b");

			// TEST
			for (int i = 0; i < fixCommits.length; i++) {
				System.out.println(i + "th commit : " + fixCommits[i]);
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
