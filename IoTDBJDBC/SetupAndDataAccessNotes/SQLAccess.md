#SQL Access
## Why ?
Though the primary access will be through promQL that is more for monitoring access. For broader access, for example combining business and IoT data (assuming both are in the same DB) there is also the option to access the data using "normal" SQL which allow access to the individual (not time series) data
##How
You need to connect to the database using your "normal" DB connect mechanisms (JDBC, SQLDeveloper, SQLPlus etc.) then access the TELEMETRY_METRICS data 

'select * from TELEMETRY_METRICS  order by metric_time_epoch desc fetch first 10 rows only'

|METRIC_NAME|METRIC_TAGS|METRIC_VALUE|METRIC_EPOCH_TIMESTAMP|
|:-----:|:-----:|:-----:|:-----:|
|iot.normalized.inverter_power_watts_point_in_time|{"iot_content_path":"InverterPowerWattsPointInTime","iot_content_type":"DECIMAL","iot_digital_twin_instance_id":"ocid1.iotdigitaltwininstance.oc1.uk-london-1.amaaaaaauevftmr7h6t75eifevhfihvbwazwi43ndgrauaawue22d6svdpna","iot_digital_twin_model_id":"ocid1.iotdigitaltwinmodel.oc1.uk-london-1.amaaaaaauevftmr7wuslrtqim6zflcxhqaprmzib74p76ev64g4zqy7edbva","iot_digital_twin_model_name":"homebattery","service_name":"IoTDBJDBC"}|-1947|1783075316.509|
|iot.normalized.discharge_power	|{"iot_content_path":"DischargePower","iot_content_type":"DECIMAL","iot_digital_twin_instance_id":"ocid1.iotdigitaltwininstance.oc1.uk-london-1.amaaaaaauevftmr7h6t75eifevhfihvbwazwi43ndgrauaawue22d6svdpna","iot_digital_twin_model_id":"ocid1.iotdigitaltwinmodel.oc1.uk-london-1.amaaaaaauevftmr7wuslrtqim6zflcxhqaprmzib74p76ev64g4zqy7edbva","iot_digital_twin_model_name":"homebattery","service_name":"IoTDBJDBC"}|	0	|1783075316.509|
|iot.normalized.capacity_remaining	|{"iot_content_path":"CapacityRemaining","iot_content_type":"DECIMAL","iot_digital_twin_instance_id":"ocid1.iotdigitaltwininstance.oc1.uk-london-1.amaaaaaauevftmr7h6t75eifevhfihvbwazwi43ndgrauaawue22d6svdpna","iot_digital_twin_model_id":"ocid1.iotdigitaltwinmodel.oc1.uk-london-1.amaaaaaauevftmr7wuslrtqim6zflcxhqaprmzib74p76ev64g4zqy7edbva","iot_digital_twin_model_name":"homebattery","service_name":"IoTDBJDBC"}|	8406	|1783075316.509|
|iot.normalized.reserved_charge_percentage	|{"iot_content_path":"ReservedChargePercentage","iot_content_type":"DECIMAL","iot_digital_twin_instance_id":"ocid1.iotdigitaltwininstance.oc1.uk-london-1.amaaaaaauevftmr7h6t75eifevhfihvbwazwi43ndgrauaawue22d6svdpna","iot_digital_twin_model_id":"ocid1.iotdigitaltwinmodel.oc1.uk-london-1.amaaaaaauevftmr7wuslrtqim6zflcxhqaprmzib74p76ev64g4zqy7edbva","iot_digital_twin_model_name":"homebattery","service_name":"IoTDBJDBC"}	|5	|1783075316.509|


Important to note. Open Telemetry recommends the use of dot separated namespaces with the names in snake_case, for example 'iot.normalized.inverter_power_watts_point_in_time' the IoTDBJDBC upload code supports this, currently however there is a "feature" in the time series DB code that on ingest converts the names of tags (also known as attributes) into only snake_case (e.g. iot.digital_twin.instance_id, to iot_digital_twin_instance_id) This was implemented to support PromQL (which seems to only like the use of _) but it means that when querying the data the tags may not be what you expect. This is being fixed, but for now when using the tags (which you will need to use to identify the specific devices) remember it's iot_digital_twin_instance_id

Also note that the upload code tries to do C like boolean mappings, so true is 1 and false is 0;