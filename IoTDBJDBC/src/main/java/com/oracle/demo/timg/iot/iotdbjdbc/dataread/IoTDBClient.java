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
package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

public interface IoTDBClient extends Comparable<IoTDBClient> {
	/**
	 * do any one off preparation to connect to the DB, for example registering the
	 * client, note that any client registration must ensure they use a name that is
	 * unique across this processing instance. This may also setup thread pools and
	 * the like. no filter will be configured
	 * 
	 * This method or the configureAQ(String filteringRule) should only be called
	 * once
	 * 
	 * @throws Exception
	 */
	public default void configureDBClient() throws Exception {
		configureDBClient(null);
	}

	/**
	 * do any one off preparation to connect to the DB, for example registering the
	 * client, note that any client registration must ensure they use a name that is
	 * unique across this processing instance. This may also setup thread pools and
	 * the like. If rule is not null then is will be applied to the queue
	 * 
	 * This method or the configureAQ() should only be called once
	 * 
	 * @throws Exception
	 */
	public void configureDBClient(String filteringRule) throws Exception;

	/**
	 * start the DB data processing, this MUST start the actual data retrieval in a
	 * separate thread and this must be non blocking.
	 * 
	 * The configuration must be capable restarting if the start method is called a
	 * second time or after the stopProcessingDB method
	 * 
	 * @throws Exception
	 */
	public void startDBProcessing() throws Exception;

	/**
	 * Do any shutdown processes, this must NOT result in a configuration that
	 * cannot be started again. The shutdown can happen a synchronusly
	 * 
	 * @throws Exception
	 */
	public void stopDBProcessing() throws Exception;

	/**
	 * do any one off tidy up work to unconfigure AQ, for example removing the
	 * client details from the queue, and destroying any resources like thread pools
	 * created by the configureAQ method.
	 * 
	 * This does not have to be re-enterable
	 * 
	 * 
	 * If the stopProcessingAQ method does asynchronous operations then this method
	 * (which may be called immediately after the stopProcessingAQ is called) should
	 * correctly handle that and if that takes time then this method should not
	 * block but itself operate asynchronously exiting asd soon as the stop and
	 * unconfigure processes are complete
	 * 
	 * @throws Exception
	 */
	public void unconfigureDBClient() throws Exception;

	/**
	 * return the client name so we can identify it
	 * 
	 * @return
	 */
	public default String getName() {
		return this.getClass().getName();
	}

	/**
	 * return any config details to help debugging
	 * 
	 * @return
	 */
	public default String getConfig() {
		return "No config details";
	}

	/**
	 * returns the order to use when starting the client
	 * 
	 * @return
	 */
	public int getOrder();

	@Override
	public default int compareTo(IoTDBClient otherIoTDBClient) {
		if (otherIoTDBClient == null) {
			throw new NullPointerException("otherIoTDBClient must not be null");
		}
		return Integer.compare(this.getOrder(), otherIoTDBClient.getOrder());
	}
}
