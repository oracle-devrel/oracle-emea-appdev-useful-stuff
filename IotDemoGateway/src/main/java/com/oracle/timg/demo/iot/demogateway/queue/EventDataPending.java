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

import java.util.concurrent.Executor;

import jakarta.inject.Singleton;

@Singleton
/**
 * this class is used to hold additional events that have arrived while the
 * device for the event is being created, this should reduce (but I don't think
 * can guarantee) the chance that event data would be sent in that window
 * 
 */
public interface EventDataPending {
	/**
	 * if there is a pending registration add the event to the queue of pending
	 * calls for that device and return true (indicating that the caller should not
	 * proceed, if there is no pending registration return false
	 * 
	 * @param data
	 * @return
	 */
	public boolean ifPendingRegistrationAddToList(EventQueueData eventData);

	/**
	 * this indicates that we need to setup a pending registration so subsequent
	 * calls to the ifPendingRegistrationAddToList will know to buffer the events
	 * 
	 * @param eventData
	 * @return
	 */

	public boolean createPendingEntry(EventQueueData eventData);

	/**
	 * This is used to re-submit the specified event as well as any pending events
	 * already on hold back into the event queue. The intended use case is when
	 * registration has had a temporary failure and the origional and any pending
	 * events need to be resubmitted
	 * 
	 * @param eventData  a "inflight" event to be sent first and is used to figure
	 *                   out what pending events (if any) to transfer over
	 * @param eventQueue the queue to transfer any pending events to
	 * @param executor   used to generate the thread to do the transfer
	 * @return -1 if there was no list of pending events (probably a coding or
	 *         timing error) or the number of events that are going to be transfered
	 *         to the events queue
	 */
	public int resubmitPendingRegistrationEvents(EventQueueData eventData, EventQueue eventQueue, Executor executor);

	/**
	 * In the situation that there is a permanent failure of a registration then
	 * this is used to just clear the data out of the lists so we don;t end up with
	 * a memory leak
	 * 
	 * @param eventData
	 * @return
	 */
	public int deletePendingregistrationEvents(EventQueueData eventData);

	/**
	 * @param eventData  is used to figure out what pending events (if any) to
	 *                   transfer over
	 * @param eventQueue the queue to transfer any pending events to
	 * @param executor   used to generate the thread to do the transfer
	 * @return -1 if there was no list of pending events (probably a coding or
	 *         timing error) or the number of events that are going to be transfered
	 *         to the events queue
	 */
	public int clearPendingRegistrationEvents(EventQueueData eventData, EventQueue eventQueue, Executor executor);

	/**
	 * returns a string describing the current configuration
	 * 
	 * @return
	 */
	public String getConfig();

	/**
	 * returns a string describing the current status
	 * 
	 * @return
	 */
	public String getStatus();
}
