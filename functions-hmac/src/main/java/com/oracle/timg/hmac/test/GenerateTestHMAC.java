package com.oracle.timg.hmac.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
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

import com.oracle.timg.hmac.VerifyHmacFunction;

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
	private static Option urlOption;
	private static Option hmacHeaderOption;
	private static Option httpRequestOption;

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
		// -u / --url the url to use as a test call if missing then no test call will be
		// -m / --hmacheader the http header to hold the calculated hmac when making a
		// test request defaults to hmac
		// -r / --httprequest the http request type defaults to POST
		// made
		// empty string)
		// these are
		VerifyHmacFunction vhm = new VerifyHmacFunction();
		CommandLine commandLine = getCommandLine(args);
		Map<String, String> headers = getDataOptions(commandLine, args, vhm);
		Map<String, String> fields = new HashMap<>(headers);
		// the fields map is used in the HMAC calculation, so add any required fields to
		// that
		addHMACCalculationEntries(commandLine, fields);
		log.info("Retrieved the following fields\n" + fields);
		String calculatedHmac = vhm.processRequest(fields);
		log.info("Calculated HMAC is " + calculatedHmac);
		if (commandLine.hasOption(urlOption)) {
			// this must have a value to have got here
			String url = commandLine.getOptionValue(urlOption);
			String hmacHeader = commandLine.getOptionValue(hmacHeaderOption, "hmac");
			headers.put(hmacHeader, calculatedHmac);
			String requestMethod = commandLine.getOptionValue(httpRequestOption, "POST");
			callUrl(requestMethod, url, headers, readFile(commandLine.getOptionValue(bodyOption, "body.txt")));
		}
	}

	private static void callUrl(String requestMethod, String url, Map<String, String> headers, String body) {
		log.info("Making " + requestMethod + "\nCalling " + url + "\nWith headers" + headers + "\nrequest body is\n"
				+ body);
		HttpClient client = HttpClient.newHttpClient();
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			log.error("Provided URL " + url + " does not parse properly because " + e.getLocalizedMessage());
			return;
		}
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.method(requestMethod, BodyPublishers.ofString(body)).uri(uri);
		headers.entrySet().stream().forEach(entry -> requestBuilder.header(entry.getKey(), entry.getValue()));
		HttpRequest request = requestBuilder.build();
		HttpResponse<String> response;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			log.error("Exception making the http call:" + e.getLocalizedMessage());
			return;
		}
		log.info("Response status code is " + response.statusCode());
		log.info("Responde body is:\n" + response.body());
	}

	private static CommandLine getCommandLine(String args[]) {
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
		return commandLine;
	}

	private static void addHMACCalculationEntries(CommandLine commandLine, Map<String, String> fields)
			throws IOException {
		fields.put("BODY", readFile(commandLine.getOptionValue(bodyOption, "body.txt")));
		fields.put("SALT", commandLine.getOptionValue(saltOption, ""));
	}

	private static Map<String, String> getDataOptions(CommandLine commandLine, String args[], VerifyHmacFunction vhm) {
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
		urlOption = Option.builder("u").longOpt("url").optionalArg(true).hasArg()
				.desc("URL To call to test request, if missing no test call will be made").build();
		hmacHeaderOption = Option.builder("m").longOpt("hmacheader").optionalArg(true).hasArg()
				.desc("Header to place the calculoated HMAC in when making a test request, defaults to hmac").build();
		httpRequestOption = Option.builder("r").longOpt("httprequest").optionalArg(true).hasArg()
				.desc("HTTP request method to call, defaults to POST").build();
		Options options = new Options().addOption(headerOption).addOption(bodyOption).addOption(saltOption)
				.addOption(fieldsOption).addOption(keyOption).addOption(algorithmOption).addOption(separatorOption)
				.addOption(urlOption).addOption(hmacHeaderOption).addOption(httpRequestOption);
		return options;
	}
}
