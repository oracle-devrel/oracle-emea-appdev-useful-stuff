package com.oracle.demo.timg.iot.iotjdbc.dataread;

import java.util.List;

import com.oracle.demo.timg.iot.iotjdbc.dbschema.RawData;
import com.oracle.demo.timg.iot.iotjdbc.dbschema.RawDataRepository;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton

/*
 * this class does business logic stuff needed to retrieve the data from the IOT
 * instance
 * 
 */
public class IoTJDBCReader {
	private RawDataRepository rawDataRepository;

	@Inject
	public IoTJDBCReader(RawDataRepository rawDataRepository) {
		this.rawDataRepository = rawDataRepository;
	}

	public List<RawData> getRawData() {
		return rawDataRepository.findAll();
	}

}
