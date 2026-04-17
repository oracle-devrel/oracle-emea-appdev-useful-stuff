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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "normalizeddata.handler.devicemodelfilter.enabled", value = "true", defaultValue = "false")
@Requires(property = "normalizeddata.handler.devicemodelfilter.order")
@Requires(property = "normalizeddata.handler.devicemodelfilter.modelname")
@Requires(property = "iotdatacache.schemaname")
@Log
public class NormalizedDataDeviceModelMessageFilter implements NormalizedDataMessageHandler {
	private static final String INSTANCE_ID_COLUMN_NAME = "instanceid";
	private static final String MODEL_ID_COLUMN_NAME = "modelid";
	public final static String SELECT_MODEL_ID_BY_MODEL_NAME = "SELECT JSON_VALUE (dtm.data, '$._id' ) AS modelid FROM digital_twin_models dtm WHERE JSON_VALUE(dtm.data, '$.displayName' ) = ";
	public final static String SELECT_MODEL_ID_BY_INSTANCE_ID = "SELECT JSON_VALUE (dti.data, '$.digitalTwinModelId' ) AS modelid FROM digital_twin_instances dti WHERE JSON_VALUE(dti.data,  '$._id'  ) = ?";
	public final static String SELECT_MODEL_ID_AND_INSTANCE_ID = "SELECT JSON_VALUE (dti.data, '$._id' ) AS instanceid, JSON_VALUE (dti.data, '$.digitalTwinModelId' ) AS modelid FROM digital_twin_instances dti";

	private final String schemaName;
	private final int order;
	private final String modelName;
	private final Set<String> matchingInstances = new HashSet<>();
	private final Set<String> nonMatchingInstances = new HashSet<>();
	private final DBConnectionSupplier dbConnectionSupplier;
	private final boolean preloadExisting;
	private Connection connection;
	private String modelId = "Not yet retrieved";
	private PreparedStatement selectModelIdByInstanceIdPS;

	@Inject
	public NormalizedDataDeviceModelMessageFilter(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "normalizeddata.handler.devicemodelfilter.order") int order,
			@Property(name = "normalizeddata.handler.devicemodelfilter.modelname") @NotNull @NotBlank String modelName,
			@Property(name = "normalizeddata.handler.devicemodelfilter.preloadexistinginstances", defaultValue = "true") boolean preloadExisting) {
		this.dbConnectionSupplier = dbConnectionSupplier;

		this.schemaName = schemaName;
		this.order = order;
		this.modelName = modelName;
		this.preloadExisting = preloadExisting;
	}

	@Override
	public void configure() throws Exception {
		log.fine("Getting connection");
		connection = dbConnectionSupplier.getNewConnection(schemaName);
		// get the model ID
		log.fine(() -> "Locating model id for model " + modelName);
		modelId = getModelId();
		if (modelId == null) {
			log.warning(() -> "Can't locate modelid for model named " + modelName);
			throw new Exception("Can't locate the model id for model named " + modelName);
		} else {
			log.info("Located model name " + modelName + " with id " + modelId);
		}
		// try to pre-load the current instances data if we've been asked to
		if (preloadExisting) {
			log.info("Pre-loading existing instances");
			preloadExistingInstances();
		} else {
			log.info("Pre-loading existing instances is disabled, they will be loaded on demand");
		}
		// set this up so we can re-use it later if we need to query for an instance we
		// didn't know about
		log.fine("Creating prepared statement");
		selectModelIdByInstanceIdPS = connection.prepareStatement(SELECT_MODEL_ID_BY_INSTANCE_ID);
		log.fine("Prepared statement created");
		log.info(getConfig());
	}

	private String getModelId() throws SQLException {
		// build the
		String getModelIdSql = SELECT_MODEL_ID_BY_MODEL_NAME + "'" + modelName + "'";
		try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(getModelIdSql)) {
			// try to get the first result
			if (rs.next()) {
				// there is one, it should be the modelId
				return rs.getString(MODEL_ID_COLUMN_NAME);
			} else {
				// nothing found, give up
				return null;
			}
		} catch (SQLException e) {
			log.severe("SQLException getting modelId, " + e.getLocalizedMessage());
			throw e;
		}
	}

	private void preloadExistingInstances() throws SQLException {
		// get all of the results
		try (Statement s = connection.createStatement();
				ResultSet rs = s.executeQuery(SELECT_MODEL_ID_AND_INSTANCE_ID)) {
			while (rs.next()) {
				String modelIdExistingInstance = rs.getString(MODEL_ID_COLUMN_NAME);
				String instanceIdExistingInstance = rs.getString(INSTANCE_ID_COLUMN_NAME);
				if (modelIdExistingInstance.equals(modelId)) {
					log.info("Pre-load instance " + instanceIdExistingInstance + " matched model id "
							+ modelIdExistingInstance);
					matchingInstances.add(instanceIdExistingInstance);
				} else {
					log.info("Pre-load instance " + instanceIdExistingInstance + " did not match model id "
							+ modelIdExistingInstance);
					nonMatchingInstances.add(instanceIdExistingInstance);
				}
			}
		} catch (SQLException e) {
			log.severe("SQLException getting existing model / instance mappings, " + e.getLocalizedMessage());
			throw e;
		}
	}

	@Override
	public void unconfigure() throws Exception {
		if (connection != null) {
			if (!connection.isClosed()) {
				log.info("Closing connection");
				connection.close();
			}
			connection = null;
		}
		log.info("Clearing old cached results");
		// just in case we are called multiple times reset the sets
		matchingInstances.clear();
		nonMatchingInstances.clear();
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		String instanceId = input.getDigitalTwinInstanceId();
		// have we checked and determined it's not a match before ?
		if (nonMatchingInstances.contains(instanceId)) {
			log.fine("instance is already in the non matching set, " + input.getDigitalTwinInstanceId());
			return new NormalizedData[0];
		}
		if (matchingInstances.contains(instanceId)) {
			log.fine("instance is already in the matching set, " + input.getDigitalTwinInstanceId());
			NormalizedData result[] = new NormalizedData[1];
			result[0] = input;
			return result;
		}
		log.fine("instance is unknown retrieving its model, " + instanceId);
		// we don't know about it, using the device ID query the DB to get the model id
		String instanceModelId;
		try {
			instanceModelId = getModelIdFromInstanceId(instanceId);
		} catch (SQLException e) {
			log.warning("SQLException locating instances model id for model " + instanceId + ", "
					+ e.getLocalizedMessage());
			instanceModelId = null;
		}
		log.fine("instance had model id, " + instanceModelId);
		if (instanceModelId == null) {
			// no model id found, this I guess is possible for an instance that is not
			// connected to a model, but we are dealing with normalized data here, which
			// should always have a model, add to the non matching for future use
			nonMatchingInstances.add(instanceId);
			log.severe("Error, was handed instance id that does not have a model id, " + instanceId);
			return new NormalizedData[0];
		} else if (instanceModelId.equals(modelId)) {
			// it matches, stash the result for later and carry on with it
			log.fine("previously unknown instance " + instanceId + "has model id " + instanceModelId
					+ " that matches model id " + modelId);
			matchingInstances.add(instanceId);
			NormalizedData result[] = new NormalizedData[1];
			result[0] = input;
			return result;
		} else {
			// no match, remember that
			log.fine("previously unknown instance " + instanceId + "has model id " + instanceModelId
					+ " that does not matche model id " + modelId);
			nonMatchingInstances.add(instanceId);
			return new NormalizedData[0];
		}
	}

	private String getModelIdFromInstanceId(String instanceId) throws SQLException {
		selectModelIdByInstanceIdPS.setString(1, instanceId);
		try (ResultSet rs = selectModelIdByInstanceIdPS.executeQuery()) {
			if (rs.next()) {
				return rs.getString(MODEL_ID_COLUMN_NAME);
			} else {
				return null;
			}
		}
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "Device Model filter";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " currently has " + matchingInstances.size()
				+ " matches with model name " + modelName + " and " + nonMatchingInstances.size()
				+ " known non matches, the model has OCID " + modelId;
	}

	@Override
	public String toString() {
		return getName() + "(" + getConfig() + ")";
	}
}
