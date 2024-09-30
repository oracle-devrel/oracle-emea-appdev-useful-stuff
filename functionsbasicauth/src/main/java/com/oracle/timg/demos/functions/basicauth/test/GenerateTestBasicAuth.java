/*Copyright (c) 2023 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.oracle.timg.demos.functions.basicauth.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpStatus;

import com.oracle.timg.demos.functions.basicauth.AuthRequest;
import com.oracle.timg.demos.functions.basicauth.BasicAuthFunction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateTestBasicAuth {

	private static Option urlOption;
	private static Option usernameOption;
	private static Option passwordOption;

	public static void main(String[] args) throws IOException {
		// build the input string, the options are
		// -p / --password the password to use for the request
		// -r / --url the url to use as a test call if missing then no test call will be
		// -n / --username the username to use for the test
		// test request defaults to hmac
		CommandLine commandLine = getCommandLine(args);
		AuthRequest request = new AuthRequest();
		String authString = commandLine.getOptionValue("n") + ":" + commandLine.getOptionValue("p");
		String authBase64 = new String(Base64.getEncoder().encode(authString.getBytes()));
		String authHeader = BasicAuthFunction.BASIC_AUTH_TYPE + " " + authBase64;
		request.getData().put(BasicAuthFunction.INCOMMING_AUTH_HEADER, authHeader);
		request.setType("USER_DEFINED");

		String requestBody = "{\"type\":\"USER_DEFINED\", \"data\": {";
		requestBody += request.getData().entrySet().stream().map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
				.collect(Collectors.joining(","));
		requestBody += "}}";
		log.info("Generated data is " + requestBody);
		if (commandLine.hasOption(urlOption)) {
			// this must have a value to have got here
			String url = commandLine.getOptionValue(urlOption);
			callUrl("POST", url, new HashMap<>(), requestBody, 1, 1, true);
		}
	}

	private static void callUrl(String requestMethod, String url, Map<String, String> headers, String body, int count,
			int delay, boolean exitOnFail) {
		log.info("Making " + requestMethod + "\nCalling " + url + "\nWith headers" + headers + "\nrequest body is\n"
				+ body);
		for (int i = 1; i <= count; i++) {
			HttpClient client = HttpClient.newBuilder().build();
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
			log.info("Try " + i + " Response status code is " + response.statusCode());
			log.info("Try " + i + " Response body is:\n" + response.body());
			if (exitOnFail && response.statusCode() != HttpStatus.SC_OK) {
				log.info("Non OK response, stopping");
				return;
			}
			if (i < count) {
				try {
					log.info("Completed try " + i + " waitign " + delay + " seconds ");
					Thread.sleep((long) (1000 * delay));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
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
					GenerateTestBasicAuth.class.getSimpleName() + " sends a request to the server for auth test",
					options);
			System.exit(-1);
		}
		return commandLine;
	}

	private static Options setupCommandLineOptions() {
		usernameOption = Option.builder("n").longOpt("username").required().hasArg().desc("username to use").build();
		passwordOption = Option.builder("p").longOpt("password").required().hasArg().desc("the password to use")
				.build();
		urlOption = Option.builder("u").longOpt("url").optionalArg(true).hasArg().desc(
				"URL To call to test request, if missing no test call will be made, just output what the request would be")
				.build();
		Options options = new Options().addOption(usernameOption).addOption(passwordOption).addOption(urlOption);
		return options;
	}
}