/*Copyright (c) 2026 Oracle and/or its affiliates.

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
package com.oracle.timg.demo.iot.demogateway.iotupload;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import com.oracle.timg.demo.iot.demogateway.ociinterations.IotServiceDetails;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.java.Log;

@Singleton
@Log
@Requires(property = "gateway.iotservice.uploadmechanism", value = "HTTPS")
@Requires(property = "gateway.iotservice.digitaltwinadapter.pathprefix")
public class IotServiceClientHttps implements IotServiceClient {
	@SuppressWarnings("unused")
	private final IotServiceDetails iotServiceDetails;
	private final String devicePath;
	private final String iotDomainHost;

	// the devicePath is not retrieved from the iot service as an adaptor can have
	// multiple paths
	@Inject
	public IotServiceClientHttps(IotServiceDetails iotServiceDetails,
			@Property(name = "gateway.iotservice.digitaltwinadapter.pathprefix") String devicePath) {
		this.iotServiceDetails = iotServiceDetails;
		this.devicePath = devicePath;
		this.iotDomainHost = iotServiceDetails.getIotDomain().getDeviceHost();
	}

	@Override
	public Boolean sendEvent(@NonNull String externalKey, @NonNull String deviceSecret, String eventText) {
		String url = "https://" + iotDomainHost + devicePath + "/" + externalKey;
		String auth = externalKey + ":" + deviceSecret;
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		String authHeader = "Basic " + encodedAuth;
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
				.header("Authorization", authHeader).POST(HttpRequest.BodyPublishers.ofString(eventText)).build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return ((response.statusCode() >= 200) && (response.statusCode() <= 299));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getConfig() {
		return "iotDomainHost=" + iotDomainHost + ", devicePath=" + devicePath;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("IotServiceClientHttps config " + getConfig());
	}
}
