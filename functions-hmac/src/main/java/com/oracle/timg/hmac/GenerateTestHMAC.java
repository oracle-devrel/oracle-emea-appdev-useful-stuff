package com.oracle.timg.hmac;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateTestHMAC {

	private static Option headerOption;
	private static Option bodyOption;
	private static Option saltOption;
	private static Option fieldsOption;
	private static Option keyOption;
	private static Option algorithmOption;
	private static Option separatorOption;

	public static void main(String[] args) throws IOException {
		// build the input string, the options are
		// -h / --header <name>:<value>[,<name:value>] one of more entries the name must
		// be a HTTP
		// header format in lower case with - not _
		// -b / --body <file> defaults to body.txt
		// -s / --salt <salt> defaults to empty string
		// -f / --fields <fields list> comma separated string of the fields to calculate
		// e.g. SALT,timestamp,BODY default to BODY
		// -k / --key the shared secret defaults to Secret
		// -a / --algorithm the algorithm name to use (default to HmacMD5)
		// -p / --separator the separator to use when building the input, defaults to
		// empty string)
		// these are
		VerifyHmacFunction vhm = new VerifyHmacFunction();
		Map<String, String> fields = getDataOptions(args, vhm);
		log.info("Retrieved the following fields\n" + fields);
		log.info("Calculated HMAC is " + vhm.processRequest(fields));
	}

	private static Map<String, String> getDataOptions(String args[], VerifyHmacFunction vhm) throws IOException {
		Options options = setupCommandLineOptions();
		DefaultParser parser = new DefaultParser();
		CommandLine commandLine = null;
		try {
			commandLine = parser.parse(options, args);
		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					GenerateTestHMAC.class.getSimpleName() + "Calculates an HMAC to use for the provide input",
					options);
			System.exit(-1);
		}
		// the fields to process
		Map<String, String> fields = new HashMap<>();
		// extract the headers
		String[] headers = commandLine.getOptionValues(headerOption);
		if (headers == null) {
			headers = new String[0];
		}
		for (String header : headers) {
			String headerEntry[] = header.split(":");
			fields.put(VerifyHmacFunction.mungeHeader(headerEntry[0]), headerEntry[1]);
		}
		fields.put("BODY", readFile(commandLine.getOptionValue(bodyOption, "body.txt")));
		fields.put("SALT", commandLine.getOptionValue(saltOption, ""));

		VerifyHmacFunction.FIELDS_TO_CALCULATE_HMAC_WITH = new ArrayList<>();
		Collections.addAll(VerifyHmacFunction.FIELDS_TO_CALCULATE_HMAC_WITH,
				commandLine.getOptionValue(fieldsOption, "").split(","));
		VerifyHmacFunction.HMAC_SECRET = commandLine.getOptionValue(keyOption, "Secret");
		VerifyHmacFunction.HMAC_ALGORITHM = commandLine.getOptionValue(algorithmOption, "HmacMD5");
		VerifyHmacFunction.HMAC_HMAC_CALCULATION_FIELD_SEPARATOR = commandLine.getOptionValue(separatorOption, "");

		return fields;
	}

	private static String readFile(String bodyFile) throws IOException {
		File file = new File(bodyFile);
		String body = "";
		try (Stream<String> linesStream = Files.lines(file.toPath())) {
			body = linesStream.collect(Collectors.joining("\n"));
		}
		return body;
	}

	private static Options setupCommandLineOptions() {
		headerOption = Option.builder("h").longOpt("header").optionalArg(true).hasArg().desc("A header to add").build();
		bodyOption = Option.builder("b").longOpt("body").optionalArg(true).hasArg()
				.desc("The file containign the text of the request body. defaults to body.txt").build();
		saltOption = Option.builder("s").longOpt("salt").optionalArg(true).hasArg()
				.desc("The salt to use, defaults to empty string.").build();
		fieldsOption = Option.builder("f").longOpt("fields").optionalArg(true).hasArg()
				.desc("The list of fields to use, defaults to BODY.").build();
		keyOption = Option.builder("k").longOpt("key").optionalArg(true).hasArg()
				.desc("The enctyprion key to use, defaults to Secret.").build();
		algorithmOption = Option.builder("a").longOpt("algorithm").optionalArg(true).hasArg()
				.desc("The HMAC algorithm key to use, defaults to HmacMD5.").build();
		separatorOption = Option.builder("p").longOpt("separator").optionalArg(true).hasArg()
				.desc("The field separator to use when building the data, defaults to empty string.").build();
		Options options = new Options().addOption(headerOption).addOption(bodyOption).addOption(saltOption)
				.addOption(fieldsOption).addOption(keyOption).addOption(algorithmOption).addOption(separatorOption);
		return options;
	}
}
