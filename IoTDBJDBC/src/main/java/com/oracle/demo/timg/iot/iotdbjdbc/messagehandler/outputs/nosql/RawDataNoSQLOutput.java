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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.nosql;

import java.security.InvalidAlgorithmParameterException;
import java.sql.Timestamp;
import java.time.Instant;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.RawDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.NotARegionProviderException;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.OCIAuthProvider;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;
import oracle.nosql.driver.NoSQLException;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.NoSQLHandleConfig;
import oracle.nosql.driver.NoSQLHandleFactory;
import oracle.nosql.driver.TableNotFoundException;
import oracle.nosql.driver.iam.SignatureProvider;
import oracle.nosql.driver.ops.GetTableRequest;
import oracle.nosql.driver.ops.PutRequest;
import oracle.nosql.driver.ops.PutResult;
import oracle.nosql.driver.ops.TableLimits;
import oracle.nosql.driver.ops.TableLimits.CapacityMode;
import oracle.nosql.driver.ops.TableRequest;
import oracle.nosql.driver.ops.TableResult;
import oracle.nosql.driver.values.MapValue;

@Singleton
@Requires(property = "messagehandler.filter.rawdata.nosql.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.output.rawdata.nosql.order")
@Requires(property = "messagehandler.output.rawdata.nosql.compartmentid")
@Log
public class RawDataNoSQLOutput implements RawDataMessageHandler {
	private final int order;
	private final String region;
	private final String compartmentOCID;
	private final String noSqlTableName;
	private final NoSQLReturnMode returnMode;
	private final boolean createTableIfNotExisting;
	private final int createTableReadUnits;
	private final int createTableWriteUnits;
	private final int createTableStorageGb;
	private final int createTableTimeout;
	private final int createTablePollingInterval;
	private NoSQLHandle handle;
	public final static String DIGITAL_TWIN_INSTANCE_ID_NAME = "digitaltwininstanceid";
	public final static String ENDPOINT_NAME = "ENDPOINT_NAME";
	public final static String TIME_RECEIVED_NAME = "timereceived";
	public final static String CONTENTS_NAME = "contents";

	@Inject
	public RawDataNoSQLOutput(OCIAuthProvider ociAuthProvider,
			@Property(name = "messagehandler.output.rawdata.nosql.order") int order,
			@Property(name = "messagehandler.output.rawdata.nosql.compartmentid") String compartmentOCID,
			@Property(name = "messagehandler.output.rawdata.nosql.tablename", defaultValue = "IoTRawData") String noSqlTableName,
			@Property(name = "messagehandler.output.rawdata.nosql.returnmode", defaultValue = "ALL") NoSQLReturnMode returnMode,
			@Property(name = "messagehandler.output.rawdata.nosql.createtableifnotexisting", defaultValue = "false") boolean createTableIfNotExisting,
			@Property(name = "messagehandler.output.rawdata.nosql.createtablereadunits", defaultValue = "1") int createTableReadUnits,
			@Property(name = "messagehandler.output.rawdata.nosql.createtablewriteunits", defaultValue = "1") int createTableWriteUnits,
			@Property(name = "messagehandler.output.rawdata.nosql.createtablestoragegb", defaultValue = "1") int createTableStorageGb,
			@Property(name = "messagehandler.output.rawdata.nosql.createtabletimeoutinms", defaultValue = "120000") int createTableTimeout,
			@Property(name = "messagehandler.output.rawdata.nosql.createtablepolingintervalinms", defaultValue = "1000") int createTablePollingInterval)
			throws NotARegionProviderException {
		this.order = order;
		BasicAuthenticationDetailsProvider authProvider = ociAuthProvider.getAuthProvider();
		if (!(authProvider instanceof RegionProvider regionProvider)) {
			throw new NotARegionProviderException("OCI auth provider " + ociAuthProvider.getAuthProviderType()
					+ ".getAuthProvider() must return a BasicAuthenticationDetailsProvider that implements RegionProvider");
		}
		Region r = regionProvider.getRegion();
		if (r == null) {
			throw new NotARegionProviderException("OCI auth provider " + ociAuthProvider.getAuthProviderType()
					+ ".getAuthProvider() returned a null region");
		}
		this.region = r.getRegionId();
		this.compartmentOCID = compartmentOCID;
		this.noSqlTableName = noSqlTableName;
		this.returnMode = returnMode;
		this.createTableIfNotExisting = createTableIfNotExisting;
		this.createTableReadUnits = createTableReadUnits;
		this.createTableWriteUnits = createTableWriteUnits;
		this.createTableStorageGb = createTableStorageGb;
		this.createTableTimeout = createTableTimeout;
		this.createTablePollingInterval = createTablePollingInterval;
	}
	/*
	 * this will use a table along the lines of the following SQL CREATE TABLE IF
	 * NOT EXISTS <noSqlTableName> AS (digitaltwininstanceid STRING , endpoint
	 * STRING , timestamp TIMESTAMP(9), contents JSON, PRIMARY
	 * KEY(digitaltwininstanceid, endpoint, timestamp) The table under the provided
	 * table name is assumed to already exist in the specified compartment, if not
	 * it will try and create it if the create table flag is set
	 */

	@Override
	public void configure() throws Exception {
		handle = generateNoSQLHandleCloud(region, compartmentOCID);
		boolean tableExists = doesTableExist(noSqlTableName);
		if (!tableExists) {
			log.info("NoSQL Table " + noSqlTableName + " not found in compartment " + compartmentOCID);
			if (createTableIfNotExisting) {
				createNoSQLTable();
			} else {
				throw new TableNotFoundException("NoSQL Table " + noSqlTableName
						+ " is not found, create it manually or set messagehandler.output.rawdata.nosql.createtableifnotexisting to true");
			}
		} else {
			log.info("NoSQL Table " + noSqlTableName + " located");
		}
	}

	private void createNoSQLTable() throws NoSQLException {
		// use constants for the name to ensure consistency
		String createTableDDL = "CREATE TABLE IF NOT EXISTS " + noSqlTableName + " AS (" + DIGITAL_TWIN_INSTANCE_ID_NAME
				+ " STRING , " + ENDPOINT_NAME + " STRING , " + TIME_RECEIVED_NAME + " TIMESTAMP(9), " + CONTENTS_NAME
				+ " JSON, PRIMARY KEY(" + DIGITAL_TWIN_INSTANCE_ID_NAME + ", " + ENDPOINT_NAME + ", "
				+ TIME_RECEIVED_NAME + ")";
		// note that this will create a provisioned table as that's cheapest for low
		// data volumes
		TableLimits limits = new TableLimits(createTableReadUnits, createTableWriteUnits, createTableStorageGb,
				CapacityMode.PROVISIONED);
		TableRequest treq = new TableRequest().setStatement(createTableDDL).setTableLimits(limits);
		TableResult result = handle.doTableRequest(treq, createTableTimeout, createTablePollingInterval);
	}

	/*
	 * Create a NoSQL handle to access the cloud service, note that this is assuming
	 * an instance principle for now, so there must be a DG setup and policies using
	 * that, at some point this could be updated to be a more dynamic process.
	 */
	private NoSQLHandle generateNoSQLHandleCloud(String regionName, String compId) throws Exception {
		SignatureProvider ap = SignatureProvider.createWithInstancePrincipal();
		NoSQLHandleConfig config = new NoSQLHandleConfig(region, ap);
		// set your default compartment
		config.setDefaultCompartment(compId);
		handle = NoSQLHandleFactory.createNoSQLHandle(config);
		return handle;
	}

	/**
	 * @param compId
	 */
	protected boolean doesTableExist(String noSqlTableName) throws NoSQLException {
		GetTableRequest getTableRequest = new GetTableRequest();
		getTableRequest.setTableName(noSqlTableName);
		try {
			handle.getTable(getTableRequest);
			return true;
		} catch (TableNotFoundException e) {
			return false;
		}
	}

	@Override
	public void unconfigure() throws Exception {
		try {
			handle.close();
		} catch (IllegalArgumentException e) {
			log.info("NoSQL handle has already been closed");
		}
	}

	@Override
	public RawData[] processRawData(RawData input) throws Exception {
		// we can only work on JSON data for now
		if (!input.getContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
			throw new NotARegionProviderException("Content type " + input.getContentType() + " is not supported");
		}
		// need the timestamp to be in a format that can be compared
		Instant timeReceivedInstant = Instant.parse(input.getTimeReceived());
		// Convert Instant to java.sql.Timestamp
		Timestamp timeReceivedTimestamp = Timestamp.from(timeReceivedInstant);
		// the raw data to no sql supports 4 fields
		MapValue mapValue = new MapValue(5);
		mapValue.put(DIGITAL_TWIN_INSTANCE_ID_NAME, input.getDigitalTwinInstanceId());
		mapValue.put(ENDPOINT_NAME, input.getEndpoint());
		mapValue.put(TIME_RECEIVED_NAME, timeReceivedTimestamp);
		mapValue.putFromJson(CONTENTS_NAME, input.getContentString(), null);
		// prepare the write
		PutRequest putRequest = new PutRequest().setValue(mapValue).setTableName(noSqlTableName);
		// write it
		PutResult putResult = handle.put(putRequest);
		// handle the result
		RawData result[];
		boolean sucessfulWrite = putResult.getVersion() != null;
		switch (returnMode) {
		case ALL: {
			result = new RawData[1];
			result[0] = input;
			break;
		}
		case FAILED: {
			if (sucessfulWrite) {
				result = new RawData[0];
			} else {
				result = new RawData[1];
				result[0] = input;
			}
			break;
		}
		case WRITTEN: {
			if (sucessfulWrite) {
				result = new RawData[1];
				result[0] = input;
			} else {
				result = new RawData[0];
			}
			break;
		}
		case NONE: {
			result = new RawData[0];
			break;
		}
		default:
			throw new InvalidAlgorithmParameterException(
					"Return mode option " + returnMode + " is not supported, code error");
		}
		return result;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "NoSQL output";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " tableName " + noSqlTableName + " compartmentid " + compartmentOCID
				+ ", createTableIfNotExisting " + createTableIfNotExisting + ", createTableReadUnits "
				+ createTableReadUnits + ", createTableWriteUnits " + createTableWriteUnits + ", createTableStorageGb "
				+ createTableStorageGb + ", createTableTimeout " + createTableTimeout + ", createTablePollingInterval "
				+ createTablePollingInterval;
	}

}
