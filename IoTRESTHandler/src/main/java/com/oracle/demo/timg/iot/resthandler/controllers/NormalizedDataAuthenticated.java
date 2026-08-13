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
package com.oracle.demo.timg.iot.resthandler.controllers;

import java.util.Base64;

import com.oracle.demo.timg.iot.resthandler.jsondata.NormalizedDataTransfer;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Controller("/api/v1/iotdata/normalizeddata/authenticated")
@Log
@ExecuteOn(TaskExecutors.BLOCKING)
@Singleton
public class NormalizedDataAuthenticated {

	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(SecurityRule.IS_AUTHENTICATED)
	@Post(value = "/jsonobject/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	public String postNormalizedDataAsJson(@PathVariable("digitaltwinid") String digitaltwinid,
			@PathVariable("contentpath") String contentpath, @PathVariable("timestamp") String timestamp,
			@Body NormalizedDataTransfer ndt) {
		String resp = "NormalizedDataAuthenticated Received json request for digitaltwinid=" + digitaltwinid
				+ ", contentpath=" + contentpath + ", timestamp=" + timestamp + " with body json contents of "
				+ ndt.toString();
		log.info(resp);
		return resp;
	}

	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	@Secured(SecurityRule.IS_AUTHENTICATED)
	@Post(value = "/string/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public String postNormalizedDataAsString(@PathVariable("digitaltwinid") String digitaltwinid,
			@PathVariable("contentpath") String contentpath, @PathVariable("timestamp") String timestamp,
			@Body String content) {
		String resp = "NormalizedDataAuthenticated Received string request for digitaltwinid=" + digitaltwinid
				+ ", contentpath=" + contentpath + ", timestamp=" + timestamp + " with body contents of " + content;
		log.info(resp);
		return resp;
	}

	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	@Secured(SecurityRule.IS_AUTHENTICATED)
	@Post(value = "/base64/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public String postNormalizedDataAsBase64(@PathVariable("digitaltwinid") String digitaltwinid,
			@PathVariable("contentpath") String contentpath, @PathVariable("timestamp") String timestamp,
			@Body String base64content) {
		String content = new String(Base64.getDecoder().decode(base64content));
		String resp = "NormalizedDataAuthenticated Received base64 request for digitaltwinid=" + digitaltwinid
				+ ", contentpath=" + contentpath + ", timestamp=" + timestamp + " with decoded body contents of "
				+ content;
		log.info(resp);
		return resp;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Controller built");
	}
}
