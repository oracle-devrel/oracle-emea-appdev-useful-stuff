# Model data for Illuminance
# display name of the model (its file name is worked out based on the suffix 
# DTMI.json)
export ENTITY_MODEL_NAME=HADailyGridEnergyFlowsModel
# display name of the adapter (its envelope and route file names are worked 
# out based on the suffixs Envelope.json and Rutes.json respetivly)
export ENTITY_ADAPTER_NAME=HADailyGridEnergyFlowsAdapter
# the last part of the endpoint, used for the sample data commands, if the 
# routes file has a condition based on the endpoint then this must match 
export ENTITY_ENDPOINT_NAME=dailygridenergyflows
# sample payload for the sample data commands, be very carefull about 
# escaping, and for the timestamp field use the variable $CURRENT_TS
# as the sample code will generate a line to set it. The sample generator
# will also produce the wrapper json which specifies the devicekey
# for each instance
export ENTITY_SAMPLE_DATA="{\\\"importkwh\\\": 1.15, \\\"exportkwh\\\": 5.5,\\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}"