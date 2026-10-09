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
