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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.java.Log;

@Log
@Singleton
public class DeviceModelInstancesCache {
	private static final String INSTANCE_ID_COLUMN_NAME = "instanceid";
	private static final String EXTERNAL_KEY_COLUMN_NAME = "externalkey";
	private static final String MODEL_ID_COLUMN_NAME = "modelid";
	private static final String MODEL_NAME_COLUMN_NAME = "modelname";
	// these are used for bulk pre-loading of entries
	public final static String SELECT_MODEL_IDS_AND_MODEL_NAMES = "SELECT JSON_VALUE (dtm.data, '$._id' ) AS modelid, JSON_VALUE(dtm.data, '$.displayName' ) AS modelname FROM digital_twin_models dtm";
	public final static String SELECT_MODEL_ID_EXTERNAL_KEY_AND_INSTANCE_ID = "SELECT JSON_VALUE (dti.data, '$._id' ) AS instanceid, JSON_VALUE (dti.data, '$.digitalTwinModelId' ) AS modelid, JSON_VALUE (dti.data, '$.externalKey' ) AS externalkey FROM digital_twin_instances dti";
	// we we don't know about this then we will try an individual load
	public final static String SELECT_MODEL_NAME_BY_MODEL_ID = "SELECT JSON_VALUE(dtm.data, '$.displayName' ) AS modelname FROM digital_twin_models dtm WHERE JSON_VALUE (dtm.data, '$._id' ) = ? ";
	public final static String SELECT_MODEL_ID_BY_MODEL_NAME = "SELECT  JSON_VALUE (dtm.data, '$._id' )  AS modelid FROM digital_twin_models dtm WHERE JSON_VALUE(dtm.data, '$.displayName' ) = ? ";
	public final static String SELECT_MODEL_ID_AND_EXTERNAL_KEY_BY_INSTANCE_ID = "SELECT JSON_VALUE (dti.data, '$.digitalTwinModelId' ) AS modelid, JSON_VALUE (dti.data, '$.externalKey' ) AS externalkey FROM digital_twin_instances dti WHERE JSON_VALUE(dti.data,  '$._id'  ) = ?";

	private final String schemaName;
	private final DBConnectionSupplier dbConnectionSupplier;
	private Connection connection;

	private final Map<String, String> instanceIdToModelId = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, String> instanceIdToExternalKey = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, String> instanceIdToModelName = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, String> externalKeyToInstanceId = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, String> modelIdToModelName = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, String> modelNameToModelId = Collections.synchronizedMap(new HashMap<>());
	private final Set<String> foundMissingModelIds = Collections.synchronizedSet(new HashSet<>());
	private final Set<String> foundMissingModelNames = Collections.synchronizedSet(new HashSet<>());
	private final Set<String> foundMissingInstanceIds = Collections.synchronizedSet(new HashSet<>());
	private final Set<String> foundMissingExternalKeys = Collections.synchronizedSet(new HashSet<>());

	private PreparedStatement selectModelIdByInstanceIdPS;
	private PreparedStatement selectModelNameByModelIdPS;
	private PreparedStatement selectModelIdByModelNamePS;
	private final boolean preloadExistingModels;
	private final boolean preloadExistingInstances;

	private boolean configured = false;

	@Inject
	// a lot of this needs to be wrapped up into a higher level class
	public DeviceModelInstancesCache(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "devicemodelinstancescache.preloadexistingmodels", defaultValue = "true") boolean preloadExistingModels,
			@Property(name = "devicemodelinstancescache.preloadexistinginstances", defaultValue = "true") boolean preloadExistingInstances) {
		this.dbConnectionSupplier = dbConnectionSupplier;
		this.schemaName = schemaName;
		this.preloadExistingModels = preloadExistingModels;
		this.preloadExistingInstances = preloadExistingInstances;
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for DeviceModelInstancesCache");
		try {
			configure();
		} catch (Exception e) {
			log.severe("Problem configuring the DeviceModelInstancesCache, " + e.getLocalizedMessage());
			return;
		}
		log.info(getConfig());
	}

	@EventListener
	public void onShutdown(ShutdownEvent event) {
		log.info("Shutdown event received for DeviceModelInstancesCache");
		try {
			unconfigure();
		} catch (Exception e) {
			log.severe("Problem unconfiguring the DeviceModelInstancesCache, " + e.getLocalizedMessage());
			return;
		}
		log.info("Unconfiguered");
	}

	public void configure() throws Exception {
		synchronized (this) {
			if (configured) {
				log.info("Already configured");
				return;
			}
			log.fine("Getting connection");
			connection = dbConnectionSupplier.getNewConnection(schemaName);

			// set this up so we can re-use it later if we need to query for an instance we
			// didn't know about
			log.fine("Creating prepared statements");
			selectModelIdByInstanceIdPS = connection.prepareStatement(SELECT_MODEL_ID_AND_EXTERNAL_KEY_BY_INSTANCE_ID);
			selectModelNameByModelIdPS = connection.prepareStatement(SELECT_MODEL_NAME_BY_MODEL_ID);
			selectModelIdByModelNamePS = connection.prepareStatement(SELECT_MODEL_ID_BY_MODEL_NAME);
			log.fine("Prepared statements created");
			// try to pre-load the existing
			if (preloadExistingModels) {
				log.info("Pre-loading existing models");
				preloadModelDetails();
			} else {
				log.info("Pre-loading existing models is disabled, they will be loaded on demand");
			}
			// try to pre-load the current instances data if we've been asked to
			if (preloadExistingInstances) {
				log.info("Pre-loading existing instances");
				preloadExistingInstances();
			} else {
				log.info("Pre-loading existing instances is disabled, they will be loaded on demand");
			}
			configured = true;
			log.info(getConfig());
		}
	}

	private void preloadModelDetails() throws SQLException {
		// get the model names and ids form the iot db
		try (Statement s = connection.createStatement();
				ResultSet rs = s.executeQuery(SELECT_MODEL_IDS_AND_MODEL_NAMES)) {
			// iterate over the results
			while (rs.next()) {
				String modelId = rs.getString(MODEL_ID_COLUMN_NAME);
				String modelName = rs.getString(MODEL_NAME_COLUMN_NAME);
				modelIdToModelName.put(modelId, modelName);
				log.info("Added id " + modelId + " to name " + modelName + " mapping");
				if ((modelName != null) && (!modelName.isBlank())) {
					modelNameToModelId.put(modelName, modelId);
					log.info("Adding name " + modelName + " to id " + modelId + " mapping");
				}
				log.finer(() -> "Loaded model " + modelName + " with id " + modelId);
			}
		} catch (SQLException e) {
			log.severe("SQLException getting modelId, " + e.getLocalizedMessage());
			throw e;
		}
	}

	/**
	 * try to get the modelId from the cache or if there is no cachedata
	 * 
	 * @param modelId             the model name to locate
	 * @param cacheMissingResults if true and we already have looked but not found
	 *                            this then don't look again
	 * @return the modelName, this could be non or blank, this is what's returned
	 *         from the
	 * @throws MissingModelException throws an exception if we can't locate the
	 *                               model (either in the known missing cache if
	 *                               cacheMissingResults is true, or in the IoT
	 *                               service otherwise)
	 * @throws SQLException          if there was a problem querying the iot service
	 */
	public String getModelNameByModelId(@NotNull @NotEmpty String modelId, boolean cacheMissingResults)
			throws MissingModelException, SQLException {
		boolean knownMissing = foundMissingModelIds.contains(modelId);
		// are we looking at the cache ?
		if (cacheMissingResults && knownMissing) {
			// we know it's missing, and we are not checking an other time
			throw new MissingModelException("No model found in cache and not checking again for modelid " + modelId);
		}
		// do we already have the info ? note that empty string and null are valid
		// responses here.
		if (modelIdToModelName.containsKey(modelId)) {
			// we have the key, the name could be a string, null blank etc if one hasn't
			// been set, but that's still valid.
			return modelIdToModelName.get(modelId);
		}
		// we don;t have a cached version
		// let's try and locate it
		try {
			String modelName = loadModelByModelId(modelId);
			// OK we have something, was it previously tagged as knownMissing ? if so remove
			// the id
			if (knownMissing) {
				foundMissingModelIds.remove(modelId);
			}
			return modelName;
		} catch (MissingModelException e) {
			// cache the missing result for later use
			foundMissingModelIds.add(modelId);
			// then throw the exception
			throw e;
		}
	}

	/**
	 * try to get the modelId from the cache or if there is no cachedata
	 * 
	 * @param modelName           the model name to locate
	 * @param cacheMissingResults if true and we already have looked but not found
	 *                            this then don't look again
	 * @return the modelId, while this code allows it to be blank or null it really
	 *         should now be
	 * @throws MissingModelException throws an exception if we can't locate the
	 *                               model (either in the known missing cache if
	 *                               cacheMissingResults is true, or in the IoT
	 *                               service otherwise)
	 * @throws SQLException          if there was a problem querying the iot service
	 */
	public String getModelIdByModelName(@NotNull @NotEmpty String modelName, boolean cacheMissingResults)
			throws MissingModelException, SQLException {
		boolean knownMissing = foundMissingModelNames.contains(modelName);
		// are we looking at the cache ?
		if (cacheMissingResults && knownMissing) {
			// we know it's missing, and we are not checking an other time
			throw new MissingModelException("No model found in cache and not checking again for modelname" + modelName);
		}
		// do we already have the info ? note that empty string and null are valid
		// responses here.
		if (modelNameToModelId.containsKey(modelName)) {
			// we have the key, the name could be a string, null blank etc if one hasn't
			// been set, but that's still valid.
			return modelNameToModelId.get(modelName);
		}
		// we don;t have a cached version
		// let's try and locate it
		try {
			String modelId = loadModelByModelName(modelName);
			// OK we have something, was it previously tagged as knownMissing ? if so remove
			// the id
			if (knownMissing) {
				foundMissingModelNames.remove(modelName);
			}
			return modelId;
		} catch (MissingModelException e) {
			// cache the missing result for later use
			foundMissingModelNames.add(modelName);
			// then throw the exception
			throw e;
		}
	}

	/**
	 * on demand load an entry in the mode details cache
	 * 
	 * @param modelId
	 * @throws SQLException
	 * @throws MissingModelException
	 * @return The located modelName (this may be null if no name is set in the IoT
	 *         service)
	 */
	private String loadModelByModelId(@NotNull @NotEmpty String modelId) throws SQLException, MissingModelException {
		synchronized (selectModelNameByModelIdPS) {
			selectModelNameByModelIdPS.setString(1, modelId);
			try (ResultSet rs = selectModelNameByModelIdPS.executeQuery()) {
				if (rs.next()) {
					String modelName = rs.getString(MODEL_NAME_COLUMN_NAME);
					// we know we always have the modelId
					modelIdToModelName.put(modelId, modelName);
					if ((modelName != null) && (!modelName.isBlank())) {
						modelNameToModelId.put(modelName, modelId);
					}
					return modelName;
				} else {
					throw new MissingModelException("No model found for modelid " + modelId);
				}
			} catch (SQLException e) {
				log.severe(() -> "SQLException getting model name from model id, " + e.getLocalizedMessage());
				throw e;
			}
		}
	}

	/**
	 * on demand load an entry in the mode details cache
	 * 
	 * @param modelName
	 * @throws SQLException
	 * @throws MissingModelException
	 * @return the located modelId, there should always be one of these if the model
	 *         exists by that name
	 */
	private String loadModelByModelName(@NotNull @NotEmpty String modelName)
			throws SQLException, MissingModelException {
		synchronized (selectModelIdByModelNamePS) {
			selectModelIdByModelNamePS.setString(1, modelName);
			try (ResultSet rs = selectModelIdByModelNamePS.executeQuery()) {
				if (rs.next()) {
					String modelId = rs.getString(MODEL_ID_COLUMN_NAME);
					modelIdToModelName.put(modelId, modelName);
					modelNameToModelId.put(modelName, modelId);
					return modelId;
				} else {
					throw new MissingModelException("No model found for modelName " + modelName);
				}
			} catch (SQLException e) {
				log.severe(() -> "SQLException getting model name from model id, " + e.getLocalizedMessage());
				throw e;
			}
		}
	}

	private void preloadExistingInstances() throws SQLException {
		// get all of the results
		try (Statement s = connection.createStatement();
				ResultSet rs = s.executeQuery(SELECT_MODEL_ID_EXTERNAL_KEY_AND_INSTANCE_ID)) {
			while (rs.next()) {
				String modelIdExistingInstance = rs.getString(MODEL_ID_COLUMN_NAME);
				String instanceIdExistingInstance = rs.getString(INSTANCE_ID_COLUMN_NAME);
				String externalKeyExistingInstance = rs.getString(EXTERNAL_KEY_COLUMN_NAME);
				String modelName = modelIdToModelName.get(modelIdExistingInstance);
				instanceIdToModelId.put(instanceIdExistingInstance, modelIdExistingInstance);
				log.info("Added instance id " + instanceIdExistingInstance + " to modelId " + modelIdExistingInstance
						+ " mapping");
				instanceIdToModelName.put(instanceIdExistingInstance, modelName);
				log.info("Added instance id " + instanceIdExistingInstance + " to modelName " + modelName + " mapping");
				instanceIdToExternalKey.put(instanceIdExistingInstance, externalKeyExistingInstance);
				log.info("Added instance id " + instanceIdExistingInstance + " to externalKey "
						+ externalKeyExistingInstance + " mapping");
				log.finer(() -> "Loaded instance " + instanceIdExistingInstance + " with model id "
						+ modelIdExistingInstance + " which mapes to model name " + modelName);
			}
		} catch (SQLException e) {
			log.severe("SQLException getting existing model / instance mappings, " + e.getLocalizedMessage());
			throw e;
		}
	}

	/**
	 * try to get the modelId from the cache or if there is no cachedata
	 * 
	 * @param instanceId          the instance to locate
	 * @param cacheMissingResults if true and we already have looked but not found
	 *                            this then don't look again
	 * @return the modelId, while this code allows it to be blank or null it really
	 *         should now be
	 * @throws MissingInstanceException throws an exception if we can't locate the
	 *                                  instance (either in the known missing cache
	 *                                  if cacheMissingResults is true, or in the
	 *                                  IoT service otherwise)
	 * @throws SQLException             if there was a problem querying the iot
	 *                                  service
	 */
	public String getModelIdByInstanceId(@NotNull @NotEmpty String instanceId, boolean cacheMissingResults)
			throws MissingInstanceException, SQLException {
		boolean knownMissing = foundMissingInstanceIds.contains(instanceId);
		// are we looking at the cache ?
		if (cacheMissingResults && knownMissing) {
			// we know it's missing, and we are not checking an other time
			throw new MissingInstanceException(
					"No instance found in cache and not checking again for instanceid" + instanceId);
		}
		// do we already have the info ? note that empty string and null are valid
		// responses here.
		if (instanceIdToModelId.containsKey(instanceId)) {
			// we have the key, the model could be a string, null blank etc if one hasn't
			// been set, but that's still valid.
			return instanceIdToModelId.get(instanceId);
		}
		// we don't have a cached version
		// let's try and locate it
		try {
			InstanceKeyInfo ike = loadInstanceByInstanceId(instanceId);
			// OK we have something, was it previously tagged as knownMissing ? if so remove
			// the id
			if (knownMissing) {
				foundMissingInstanceIds.remove(instanceId);
			}
			return ike.getModelId();
		} catch (MissingInstanceException e) {
			// cache the missing result for later use
			foundMissingInstanceIds.add(instanceId);
			// then throw the exception
			throw e;
		}
	}

	/**
	 * try to get the external key from the cache or if there is no cachedata
	 * 
	 * @param instanceId          the instance to locate
	 * @param cacheMissingResults if true and we already have looked but not found
	 *                            this then don't look again
	 * @return the modelId, while this code allows it to be blank or null it really
	 *         should now be
	 * @throws MissingInstanceException throws an exception if we can't locate the
	 *                                  instance (either in the known missing cache
	 *                                  if cacheMissingResults is true, or in the
	 *                                  IoT service otherwise)
	 * @throws SQLException             if there was a problem querying the iot
	 *                                  service
	 */
	public String getExternalKeyByInstanceId(@NotNull @NotEmpty String instanceId, boolean cacheMissingResults)
			throws MissingInstanceException, SQLException {
		boolean knownMissing = foundMissingInstanceIds.contains(instanceId);
		// are we looking at the cache ?
		if (cacheMissingResults && knownMissing) {
			// we know it's missing, and we are not checking an other time
			throw new MissingInstanceException(
					"No instance found in cache and not checking again for instanceid" + instanceId);
		}
		// do we already have the info ? note that empty string and null are valid
		// responses here.
		if (instanceIdToExternalKey.containsKey(instanceId)) {
			// we have the key, the model could be a string, null blank etc if one hasn't
			// been set, but that's still valid.
			return instanceIdToExternalKey.get(instanceId);
		}
		// we don't have a cached version
		// let's try and locate it
		try {
			InstanceKeyInfo ike = loadInstanceByInstanceId(instanceId);
			// OK we have something, was it previously tagged as knownMissing ? if so remove
			// the id
			if (knownMissing) {
				foundMissingInstanceIds.remove(instanceId);
			}
			return ike.getExternalKey();
		} catch (MissingInstanceException e) {
			// cache the missing result for later use
			foundMissingInstanceIds.add(instanceId);
			// then throw the exception
			throw e;
		}
	}

	/**
	 * try to get the modelName from the cache or if there is no cachedata
	 * 
	 * @param instanceId          the instance to locate
	 * @param cacheMissingResults if true and we already have looked but not found
	 *                            this then don't look again
	 * @return the modelId, while this code allows it to be blank or null it really
	 *         should now be
	 * @throws MissingInstanceException throws an exception if we can't locate the
	 *                                  instance (either in the known missing cache
	 *                                  if cacheMissingResults is true, or in the
	 *                                  IoT service otherwise)
	 * @throws MissingModelException    throws an exception if we can't locate the
	 *                                  model (either in the known missing cache if
	 *                                  cacheMissingResults is true, or in the IoT
	 *                                  service otherwise)
	 * @throws SQLException             if there was a problem querying the iot
	 *                                  service
	 */
	public String getModelNameByInstanceId(@NotNull @NotEmpty String instanceId, boolean cacheMissingResults)
			throws MissingInstanceException, MissingModelException, SQLException {
		// first we need the model Id, this may load data from the cache, get data from
		// the IoT instance or throw errors
		String modelId = getModelIdByInstanceId(instanceId, cacheMissingResults);
		// if the modelId is not null and not empty then try to get the model name, if
		// it is then return null (this is valid for instances not connected to a
		// modelid
		if ((modelId == null) || modelId.isEmpty()) {
			return null;
		}
		// try to get the model name, this may throw exceptions if it can;t get the
		// details
		return getModelNameByModelId(modelId, cacheMissingResults);
	}

	/**
	 * on demand load an entry in the mode details cache
	 * 
	 * @param instanceId
	 * @throws SQLException
	 * @throws MissingInstanceException
	 * @return the located modelId, null for instances with no model
	 */
	private InstanceKeyInfo loadInstanceByInstanceId(@NotNull @NotEmpty String instanceId)
			throws SQLException, MissingInstanceException {
		synchronized (selectModelIdByInstanceIdPS) {
			selectModelIdByInstanceIdPS.setString(1, instanceId);
			// get all of the results
			try (ResultSet rs = selectModelIdByInstanceIdPS.executeQuery()) {
				if (rs.next()) {
					String modelId = rs.getString(MODEL_ID_COLUMN_NAME);
					String externalKey = rs.getString(EXTERNAL_KEY_COLUMN_NAME);
					String modelName = modelIdToModelName.get(modelId);
					instanceIdToModelId.put(instanceId, modelId);
					instanceIdToModelName.put(instanceId, modelName);
					instanceIdToExternalKey.put(instanceId, externalKey);
					externalKeyToInstanceId.put(externalKey, instanceId);
					return new InstanceKeyInfo(instanceId, modelId, externalKey);
				} else {
					throw new MissingInstanceException("No instance found for instance id " + instanceId);
				}
			} catch (SQLException e) {
				log.severe("SQLException getting existing model / instance mappings, " + e.getLocalizedMessage());
				throw e;
			}
		}
	}

	@Data
	@AllArgsConstructor
	private class InstanceKeyInfo {
		String instanceId;
		String modelId;
		String externalKey;
	}

	public void unconfigure() throws Exception {
		synchronized (this) {
			// this will close all prepared statements, result sets etc. that originated
			// from it
			if (connection != null) {
				if (!connection.isClosed()) {
					log.info("Closing connection");
					connection.close();
				}
				connection = null;
			}
			log.info("Clearing old cached results");
			// just in case we are called multiple times reset the sets
			instanceIdToModelId.clear();
			instanceIdToModelName.clear();
			instanceIdToExternalKey.clear();
			modelIdToModelName.clear();
			modelNameToModelId.clear();
			foundMissingModelIds.clear();
			foundMissingModelNames.clear();
			foundMissingInstanceIds.clear();
			foundMissingExternalKeys.clear();
			configured = false;
		}
	}

	public String getName() {
		return "Device Model Names / Id / Instance Id cache";
	}

	public String getConfig() {
		// this needs fixing
		return getName() + " currently has schema " + schemaName + ", " + instanceIdToModelName.size() + " ( "
				+ instanceIdToModelName + ")" + " instance ids, " + modelIdToModelName.size() + " ( "
				+ modelIdToModelName + ")" + " model ids, " + modelNameToModelId.size() + " ( " + modelNameToModelId
				+ ")" + " model names, " + foundMissingModelIds.size() + " found missing model ids"
				+ foundMissingModelNames.size() + " found missing model names, " + foundMissingInstanceIds.size()
				+ " found missing instance ids. preloadExistingModels=" + preloadExistingModels
				+ ", preloadExistingInstances=" + preloadExistingInstances;
	}

	@Override
	public String toString() {
		return getName() + "(" + getConfig() + ")";
	}
}
