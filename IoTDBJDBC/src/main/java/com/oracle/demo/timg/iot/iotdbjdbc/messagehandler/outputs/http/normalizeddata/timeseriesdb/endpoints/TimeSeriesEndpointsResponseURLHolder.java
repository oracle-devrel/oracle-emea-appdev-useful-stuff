package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Serdeable
@NoArgsConstructor
public class TimeSeriesEndpointsResponseURLHolder {
	@Getter
	private String url;
	private Map<String, String> queryParams;

	public String getQueryParam(String name) throws URISyntaxException {
		if (queryParams == null) {
			getQueryParams();
		}
		return queryParams.get(name);
	}

	// get the first query param that matches the provided name.
	private void getQueryParams() throws URISyntaxException {
		URI uri = new URI(url);
		queryParams = new HashMap<>();
		String query = uri.getRawQuery();
		if (query == null || query.isEmpty()) {
			return;
		}
		for (String pair : query.split("&")) {
			String[] keyValue = pair.split("=", 2);
			String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
			String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
			queryParams.put(key, value);
		}
		return;
	}
}