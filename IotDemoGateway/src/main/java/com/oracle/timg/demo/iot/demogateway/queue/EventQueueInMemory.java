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
package com.oracle.timg.demo.iot.demogateway.queue;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = "gateway.eventqueue.type", value = "IN_MEMORY", defaultValue = "unknown")

/*
 * a buffer to hold incoming events, this has a confih
 * 
 */
public class EventQueueInMemory implements EventQueue {
	private final ArrayBlockingQueue<EventQueueData> queue;
	private final int queueCapacity;

	public EventQueueInMemory(
			@Property(name = "gateway.eventqueue.inmemory.size", defaultValue = "1024") int queueCapacity) {
		this.queueCapacity = queueCapacity;
		queue = new ArrayBlockingQueue<>(queueCapacity);
		log.info("Configured event queue with max size of " + this.queueCapacity + ", it's current size is "
				+ queue.size());
	}

	/**
	 * add to the queue, this is non blocking and if the data can't be added will
	 * return false instead
	 * 
	 * @param data
	 * @return
	 */
	@Override
	public boolean addToQueue(EventQueueData data) {
		boolean added = queue.offer(data);
		return added;
	}

	/**
	 * add all elements in the list to the queue, this is non blocking and if the
	 * data can't be added will return false instead
	 * 
	 * @param data
	 * @return
	 */
	@Override
	public boolean addToQueue(List<EventQueueData> data) {
		boolean added = queue.addAll(data);
		return added;
	}

	/**
	 * This is blocking if the queue is empty so always call it in from a thread
	 * which is not the main executions as seen externally
	 * 
	 * @return
	 */
	@Override
	public EventQueueData getNext() throws InterruptedException {
		EventQueueData retrieved = queue.take();
		return retrieved;

	}

	@PostConstruct
	public void postConstruct() {
		log.info(getStatus());
	}

	/**
	 * 
	 */
	@Override
	public String getStatus() {
		return "EventQueueInMemory has remaining capacity of " + queue.remainingCapacity() + " it's current size is "
				+ queue.size();
	}

	@Override
	public int getQueueSize() {
		return queue.size();
	}
}
