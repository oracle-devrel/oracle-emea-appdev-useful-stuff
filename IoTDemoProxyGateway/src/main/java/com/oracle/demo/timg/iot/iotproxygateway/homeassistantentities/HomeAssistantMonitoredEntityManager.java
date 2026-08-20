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
package com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.ToString;
import lombok.extern.java.Log;

@Singleton
@Log
@Context
// are we going to retrieve data from home assistant ?
@Requires(property = PropertyNames.OPERATING_MODE_INPUT, value = "HOME_ASSISTANT", defaultValue = "HOME_ASSISTANT")
public class HomeAssistantMonitoredEntityManager {
	@ToString.Exclude
	private final TaskScheduler taskScheduler;
	@Inject
	private Collection<HomeAssistantMonitoredEntitySet> monitoredEntitySets;
	private List<ScheduledFuture<?>> scheduledFutures;

	public HomeAssistantMonitoredEntityManager(@Named(TaskExecutors.SCHEDULED) TaskScheduler taskScheduler) {
		log.info("In startup monitor constructor");
		this.taskScheduler = taskScheduler;
	}

	@PostConstruct
	void postConstruct() {
		log.fine("Startup has given us " + monitoredEntitySets.size() + " monitored entity sets which are "
				+ monitoredEntitySets);
	}

	@EventListener
	public void startup(StartupEvent event) {
		log.info("Starting scheduling of HA entity set monitoring");
		scheduledFutures = monitoredEntitySets.stream().map(monitoredEntity -> {
			log.fine(() -> "Starting monitored entity " + monitoredEntity);
			ScheduledFuture<?> sf = taskScheduler.scheduleAtFixedRate(monitoredEntity.getInitaldelay(),
					monitoredEntity.getRetrievalrate(), monitoredEntity);
			log.info(() -> "Started monitored entity " + monitoredEntity);
			return sf;
		}).collect(Collectors.toList());
	}

	@EventListener
	public void shutdown(ShutdownEvent event) {
		log.info("Stopping all home assistant entity set monitoring");
		scheduledFutures.stream().forEach(scheduledFuture -> scheduledFuture.cancel(false));
	}
}
