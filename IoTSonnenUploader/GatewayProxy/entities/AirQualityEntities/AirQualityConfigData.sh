# Model data for Illuminance
# display name of the model (its file name is worked out based on the suffix 
# DTMI.json)
export ENTITY_MODEL_NAME=HAAirQualityModel
# display name of the adapter (its envelope and route file names are worked 
# out based on the suffixs Envelope.json and Rutes.json respetivly)
export ENTITY_ADAPTER_NAME=HAAirQualityAdapter
# the last part of the endpoint, used for the sample data commands, if the 
# routes file has a condition based on the endpoint then this must match 
export ENTITY_ENDPOINT_NAME=airqualityruuvi 
export ENTITY_ENDPOINT_NAME_SECONDARY=airqualityalpstuga
# sample payload for the sample data commands, be very carefull about 
# escaping, and for the timestamp field use the variable $CURRENT_TS
# as the sample code will generate a line to set it. The sample generator
# will also produce the wrapper json which specifies the devicekey
# for each instance
export ENTITY_SAMPLE_DATA="{\\\"qualityscore\\\": 92, \\\"co2ppm\\\": 549, \\\"pm2_5_ugm3\\\": 2.0, \\\"noxindex\\\":1.0, \\\"vocindex\\\": 137, \\\"airpressure\\\": 1013.25, \\\"relativehumidity\\\": 53.4, \\\"temperature\\\": 22.1, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}"
export ENTITY_SAMPLE_DATA_SECONDARY="{\\\"qualitydescription\\\": \\\"Good\\\", \\\"co2ppm\\\": 549, \\\"pm2_5_ugm3\\\": 2.0, \\\"relativehumidity\\\": 53.4, \\\"temperature\\\": 22.1, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}"