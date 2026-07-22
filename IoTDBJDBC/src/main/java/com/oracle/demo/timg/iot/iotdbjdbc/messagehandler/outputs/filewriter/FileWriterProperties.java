package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.filewriter;

public class FileWriterProperties {
	public final static String NORMALIZED_DATA_FILE_OUTPUT = "messagehandler.output.normalizeddata.fileoutput";
	public final static String NORMALIZED_DATA_FILE_OUTPUT_ENABLED = NORMALIZED_DATA_FILE_OUTPUT + ".enabled";
	public final static String NORMALIZED_DATA_FILE_OUTPUT_ORDER = NORMALIZED_DATA_FILE_OUTPUT + ".order";
	public final static String NORMALIZED_DATA_FILE_OUTPUT_DURATION = NORMALIZED_DATA_FILE_OUTPUT + ".duration";
	public final static String NORMALIZED_DATA_FILE_OUTPUT_TARGET_FILE = NORMALIZED_DATA_FILE_OUTPUT + ".target_file";

	public final static String RAW_DATA_FILE_OUTPUT = "messagehandler.output.rawdata.fileoutput";
	public final static String RAW_DATA_FILE_OUTPUT_ENABLED = RAW_DATA_FILE_OUTPUT + ".enabled";
	public final static String RAW_DATA_FILE_OUTPUT_ORDER = RAW_DATA_FILE_OUTPUT + ".order";
	public final static String RAW_DATA_FILE_OUTPUT_DURATION = RAW_DATA_FILE_OUTPUT + ".duration";
	public final static String RAW_DATA_FILE_OUTPUT_TARGET_FILE = RAW_DATA_FILE_OUTPUT + ".target_file";
}
