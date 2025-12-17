/*Copyright (c) 2025 Oracle and/or its affiliates.

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
package com.oracle.demo.timg.iot.iotordsaccess.testers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.oracle.demo.timg.iot.iotordsaccess.idcs.IDCSOAuthApplicationTokenRetriever;
import com.oracle.demo.timg.iot.iotordsaccess.idcs.IDCSOAuthTokenRetrievalException;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = "iot.idcs.test-token-retrieval", value = "true", defaultValue = "false")
public class IDCSTestTokenRetrieval {
	@Inject
	private IDCSOAuthApplicationTokenRetriever idcsoAuthApplicationTokenRequest;

	private int counter = 0;
	@Inject
	private IDCSOAuthApplicationTokenRetriever idcsoAuthApplicationTokenRetriever;

	@ExecuteOn(TaskExecutors.IO)
	@Scheduled(fixedRate = "10s", initialDelay = "10s")
	public void testGetToken() {
		if (counter++ > 12) {
			System.exit(0);
		}
		String token;
		try {
			token = idcsoAuthApplicationTokenRetriever.getToken();
		} catch (IDCSOAuthTokenRetrievalException e) {
			log.warning("Problem getting token, " + e.getLocalizedMessage());
			return;
		}
		String tokenType = idcsoAuthApplicationTokenRetriever.getTokenType();
		LocalDateTime ldt = idcsoAuthApplicationTokenRetriever.getCurrentTokenRenewTime();
		log.info("Loop :" + counter + " expiry=" + ldt.format(DateTimeFormatter.ISO_DATE_TIME) + ", type=" + tokenType
				+ ", token=" + token);
		if (counter == 3) {
			log.info("Forcing complete token data reset");
			idcsoAuthApplicationTokenRetriever.deleteTokenDetails();
		}
		if ((counter == 6) || (counter == 9)) {
			log.info("Forcing  token timout after next retrieval");
			idcsoAuthApplicationTokenRetriever.forceTokenRetrievalAfter(Duration.ofSeconds(15));
			log.info("current time is " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
					+ ", new expiry time is " + idcsoAuthApplicationTokenRetriever.getCurrentTokenRenewTime()
							.format(DateTimeFormatter.ISO_DATE_TIME));
		}
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for IDCSTestTokenRetrieval");
	}
}
