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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters.normalizeddata.consolidator;

import java.time.Duration;
import java.time.Instant;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import lombok.ToString;
import lombok.extern.java.Log;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;
import oracle.sql.json.OracleJsonValue.OracleJsonType;

@Log
@ToString(onlyExplicitlyIncluded = true)
public class NormalizedDataLastUpdatedTimestamp {
	public final static String CONSOLIDATED_CONTENT_PATH_DEFAULT = "consolidated";
	private final static OracleJsonFactory ojf = new OracleJsonFactory();
	@ToString.Include
	private final OracleJsonObject jsonObject = ojf.createObject();
	@ToString.Include
	private final String timeObserved;
	@ToString.Include
	private final String contentPathToOutput;
	private final String digitalTwinInstanceOCID;
	@ToString.Include
	private final String generatedKey;
	@ToString.Include
	private Instant lastUpdatedTs = Instant.now();

	/**
	 * set the internal json data to be the value in the normalizedData and it's key
	 * to be the content path, save the timeObserved, set the conmtent path of
	 * JsonObjects we will generate to be consolidated, set the last updated to the
	 * current instant
	 * 
	 * @param normalizedData
	 */
	public NormalizedDataLastUpdatedTimestamp(NormalizedData normalizedData) {
		this(normalizedData, CONSOLIDATED_CONTENT_PATH_DEFAULT);
	}

	/**
	 * set the internal json data to be the value in the normalizedData and it's key
	 * to be the content path, save the timeObserved, set the content path of
	 * JsonObjects we will generate to contentPath, set the last updated to the
	 * current instant
	 * 
	 * @param normalizedData
	 */
	public NormalizedDataLastUpdatedTimestamp(NormalizedData normalizedData, String contentPathToOutput) {
		// stash the constants
		this.timeObserved = normalizedData.getTimeObserved();
		this.contentPathToOutput = contentPathToOutput;
		this.digitalTwinInstanceOCID = normalizedData.getDigitalTwinInstanceId();
		this.generatedKey = getKey(normalizedData);
		// add the content we've just been given
		this.updateWith(normalizedData);
	}

	/**
	 * determines if the timestamps are the same
	 * 
	 * @param other
	 * @return
	 */
	public boolean isTimestampIdentical(NormalizedDataLastUpdatedTimestamp other) {
		return this.timeObserved.equals(other.timeObserved);
	}

	/**
	 * determines if the timestamps are the same
	 * 
	 * @param other
	 * @return
	 */
	public boolean isTimestampIdentical(NormalizedData other) {
		return this.timeObserved.equals(other.getTimeObserved());
	}

	public String getKey() {
		return generatedKey;
	}

	public static String getKey(NormalizedData normalizedData) {
		return normalizedData.getDigitalTwinInstanceId();// + "/" + normalizedData.getContentPath();
	}

	/**
	 * if the last update of the object was more than durationBeforeNow form the
	 * current time return true, else false
	 * 
	 * @param durationBeforeNow
	 * @return
	 */
	public boolean lastUpdatedBefore(Duration durationBeforeNow) {
		return lastUpdatedBefore(Instant.now().minus(durationBeforeNow));
	}

	/**
	 * if the last update was before timestampToCheck return true, else false
	 * 
	 * @param timestampToCheck
	 * @return
	 */
	public boolean lastUpdatedBefore(Instant timestampToCheck) {
		return lastUpdatedTs.isBefore(timestampToCheck);
	}

	/**
	 * generate a Normalized data based on the consolidated info, this will have the
	 * timeObserved of the normalizedData provided when this instance was created,
	 * and the contentPath set to the one provided when constructed (or the defaut
	 * of CONSOLIDATED_CONTENT_PATH_DEFAULT) the contentType will be a Object
	 * 
	 * @return
	 */
	public NormalizedData retrieveNormalizedData() {
		String jsonContentsAsString = jsonObject.toString();
		return NormalizedData.builder().digitalTwinInstanceId(digitalTwinInstanceOCID).timeObserved(timeObserved)
				.contentPath(contentPathToOutput).contentJsonType(OracleJsonType.OBJECT).contentJsonValue(jsonObject)
				.content(jsonContentsAsString).contentType(OracleJsonType.OBJECT.toString()).build();
	}

	/**
	 * adds the value to the internal representation using the contentPath as the
	 * key, updates the lastUpdated timestamp to be the current time (so we can
	 * later on look for instances of this object that have "timed out")
	 * 
	 * @param normalizedData
	 */
	public void updateWith(NormalizedData normalizedData) {
		this.updateWith(normalizedData.getContentPath(), normalizedData.getContentJsonValue());
	}

	/**
	 * adds the value to the internal representation using the contentPath as the
	 * key, updates the lastUpdated timestamp to be the current time (so we can
	 * later on look for instances of this object that have "timed out")
	 * 
	 * @param normalizedData
	 */
	public void updateWith(String contentPathOfValue, OracleJsonValue value) {
		log.info(() -> "Adding key " + contentPathOfValue + " with value " + value);
		if (jsonObject.containsKey(contentPathOfValue)) {
			log.severe("Rejecting attempt to add duplicate content path of " + contentPathOfValue
					+ " to Normalized data event for instance key " + generatedKey + " with observed time of "
					+ timeObserved);
		} else {
			jsonObject.put(contentPathOfValue, value);
			this.lastUpdatedTs = Instant.now();
		}
	}
}
