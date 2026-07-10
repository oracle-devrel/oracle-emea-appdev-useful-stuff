# run this in the GatewayProxy directory
source ../OCISetup/common_names.sh
source ./gateway_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"
# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
IOT_COMPARTMENT_OCID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`


export IOT_DOMAIN_GROUP_OCID=`oci iot domain-group list --display-name $IOT_DOMAIN_GROUP_NAME --compartment-id $IOT_COMPARTMENT_OCID | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$IOT_DOMAIN_GROUP_OCID" ]]
then
  echo "Can not locate the domain group $IOT_DOMAIN_GROUP_NAME can not proceed"
  exit -1
else
  echo "Iot Domain Group $IOT_DOMAIN_GROUP_NAME located"
fi

# get the data host details
export IOT_DOMAIN_GROUP_DATA_HOST=`oci iot domain-group get --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data."data-host"'`
export IOT_DOMAIN_GROUP_SHORT_ID=`echo $IOT_DOMAIN_GROUP_DATA_HOST| tr '.' ' ' | awk '{print $1}'`

export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$IOT_DOMAIN_OCID" ]]
then
    echo "Can not locate IotDomain $IOT_DOMAIN_NAME can not proceed"
    exit 2
else
  echo "Iot Domain $IOT_DOMAIN_NAME located"
fi

#Get the host details we will need
export IOT_DOMAIN_HOST=`oci iot domain get --iot-domain-id $IOT_DOMAIN_OCID | jq -r '.data."device-host"'`
export IOT_DOMAIN_SHORT_ID=`echo $IOT_DOMAIN_HOST| tr '.' ' ' | awk '{print $1}'`

export GATEWAY_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_MODEL_NAME | jq  -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$GATEWAY_MODEL_ID" ]]
then
  echo Creating Digital Twin Model for gateway $GATEWAY_MODEL_NAME
  # Create the digtal twin model for the generic home battery, run from within the DigitalTwin folder
  echo oci iot digital-twin-model create --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_MODEL_NAME --spec $GATEWAY_MODEL_FILE --wait-for-state ACTIVE
  oci iot digital-twin-model create --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_MODEL_NAME --spec $GATEWAY_MODEL_FILE --wait-for-state ACTIVE
  # get the id of the model you just created
  export GATEWAY_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_MODEL_NAME | jq  -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
  echo Created Digital Twin Gateway Model $GATEWAY_MODEL_NAME
else
  echo "Digital twin model for gateway $GATEWAY_MODEL_NAME already exists"
fi

export GATEWAY_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $GATEWAY_ADAPTER_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$GATEWAY_ADAPTER_OCID" ]]
then
  echo Creating Digital Twin adapter $GATEWAY_ADAPTER_NAME
  # create the adaptor using the multiple routes file.
  # note that as the file contents is inlined into the JSON request then the structure of the JSON files MUST be valid as JSON and also meeting the spec - if not you will get missing param errors, even though you have specified all of the actual flags correctly
  echo oci iot digital-twin-adapter create --iot-domain-id $IOT_DOMAIN_OCID --digital-twin-model-id $GATEWAY_MODEL_ID  --inbound-routes $GATEWAY_ADAPTOR_ROUTE_MAPPINGS_FILE  --display-name $GATEWAY_ADAPTER_NAME  --inbound-envelope $GATEWAY_ADAPTOR_ENVELOPE_MAPPINGS_FILE   --wait-for-state ACTIVE
  oci iot digital-twin-adapter create --iot-domain-id $IOT_DOMAIN_OCID --digital-twin-model-id $GATEWAY_MODEL_ID  --inbound-routes $GATEWAY_ADAPTOR_ROUTE_MAPPINGS_FILE  --display-name $GATEWAY_ADAPTER_NAME  --inbound-envelope $GATEWAY_ADAPTOR_ENVELOPE_MAPPINGS_FILE   --wait-for-state ACTIVE
  # get the OCID of the new adaptor
  export GATEWAY_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $GATEWAY_ADAPTER_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
  echo Created Gateway adapter $GATEWAY_ADAPTER_NAME
else
  echo "Gateway adapter $GATEWAY_ADAPTER_NAME already exists"
fi

export GATEWAY_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_DISPLAY_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$GATEWAY_OCID" ]]
then
  echo "No existing gateway named $GATEWAY_DISPLAY_NAME will attempt to create it"
  export VAULT_OCID=`oci kms management vault list   --compartment-id $IOT_COMPARTMENT_OCID --all | jq -r ".data[] |  select (.\"display-name\" == \"$VAULT_NAME\") | select (.\"lifecycle-state\" == \"ACTIVE\") | .id"`
  if [[ -z "$VAULT_OCID" ]]
  then
    echo "Can't locate vault $VAULT_NAME unable to create a new gateway instance"
    exit -1
  fi
  echo "Located vault $VAULT_NAME"
  export FIXED_SECRET_OCID=`oci secrets secret-bundle get-secret-bundle-by-name  --vault-id $VAULT_OCID --secret-name $FIXED_SECRET_NAME | jq -r '.data."secret-id"'`
  if [[ -z "$FIXED_SECRET_OCID" ]]
  then
    echo "Can't locate secret $FIXED_SECRET_NAME in vault $VAULT_NAME unable to create a new gateway instance"
    exit -1
  else 
    echo "Located secret $FIXED_SECRET_NAME in vault $VAULT_NAME"
  fi
  echo  oci iot digital-twin-instance create --iot-domain-id $IOT_DOMAIN_OCID --connectivity-type GATEWAY --display-name $GATEWAY_DISPLAY_NAME --auth-id $FIXED_SECRET_OCID --digital-twin-adapter-id $GATEWAY_ADAPTER_OCID --external-key $GATEWAY_NAME --wait-for-state ACTIVE
  oci iot digital-twin-instance create --iot-domain-id $IOT_DOMAIN_OCID --connectivity-type GATEWAY --display-name $GATEWAY_DISPLAY_NAME --auth-id $FIXED_SECRET_OCID --digital-twin-adapter-id $GATEWAY_ADAPTER_OCID --external-key $GATEWAY_NAME --wait-for-state ACTIVE
  export GATEWAY_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_DISPLAY_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
  echo "Create gateway instance"
else
  echo "Found existing gateway instance"
fi
if [[ -z "$GATEWAY_OCID" ]]
then
  echo "Could not locate the digital twin instance, it may not have been created, exiting"
  exit -2  
fi

echo "Getting digital twin instance crededntials"
# get the external key and auth details
export GATEWAY_EXTERNAL_KEY=`oci iot digital-twin-instance get --digital-twin-instance-id  $GATEWAY_OCID | jq -r  '.data."external-key"'`
# if we just created the instance we have this, but not if we found an existing one
export GATEWAY_SECRET_OCID=`oci iot digital-twin-instance get --digital-twin-instance-id  $GATEWAY_OCID | jq -r  '.data."auth-id"'`
export GATEWAY_INSTANCE_SECRET_BASE64=`oci secrets secret-bundle get --secret-id $GATEWAY_SECRET_OCID --stage CURRENT | jq -r '.data."secret-bundle-content".content'`
export GATEWAY_INSTANCE_SECRET=`echo $GATEWAY_INSTANCE_SECRET_BASE64 | base64 --decode`
export GATEWAY_CREDENTIALS="$GATEWAY_EXTERNAL_KEY":"$GATEWAY_INSTANCE_SECRET"
echo "GATEWAY credentials $GATEWAY_CREDENTIALS"
echo
echo "To send the gateway stats test data using https for device $GATEWAY_NAME"
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z'
#echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`'
echo curl -u \"$GATEWAY_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/$GATEWAY_ENDPOINT_GATEWAY_STATS_PATH\" -H \'Content-Type: application/json\' -d \"{\\\"payload\\\": {\\\"haretrievesuccess\\\": 24.6,\\\"haretrievefail\\\": 1.4,\\\"uploadsuccess\\\": 24.6,\\\"uploadfail\\\": 1.4}, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}\"
echo
echo "To send the gateway config test data using https for device $GATEWAY_NAME"
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z'
echo curl -u \"$GATEWAY_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/$GATEWAY_ENDPOINT_GATEWAY_CONFIG_PATH\" -H \'Content-Type: application/json\' -d \"{\\\"payload\\\": {\\\"successfullharetrievetimewindow\\\": 14,\\\"failedharetrievetimewindow\\\": 1, \\\"successfulluploadtimewindow\\\": 12,\\\"faileduploadtimewindow\\\": 2}, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}\"
echo
echo "To send gateway stats test data for the configuration using mqttx for device $GATEWAY_NAME"
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z'
echo mqttx pub -t $GATEWAY_ENDPOINT_GATEWAY_STATS_PATH -ct application/json  -u $GATEWAY_EXTERNAL_KEY -P $GATEWAY_INSTANCE_SECRET  -h $IOT_DOMAIN_HOST -p 8883  -m   \"{\\\"payload\\\": {\\\"haretrievesuccess\\\": 24.6,\\\"haretrievefail\\\": 1.4,\\\"uploadsuccess\\\": 24.6,\\\"uploadfail\\\": 1.4}, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}\"
echo
echo "To send gateway config test data for the configuration using mqttx for device $GATEWAY_NAME"
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z'
echo mqttx pub -t $GATEWAY_ENDPOINT_GATEWAY_CONFIG_PATH -ct application/json  -u $GATEWAY_EXTERNAL_KEY -P $GATEWAY_INSTANCE_SECRET  -h $IOT_DOMAIN_HOST -p 8883  -m  \"{\\\"payload\\\": {\\\"successfullharetrievetimewindow\\\": 14,\\\"failedharetrievetimewindow\\\": 1, \\\"successfulluploadtimewindow\\\": 12,\\\"faileduploadtimewindow\\\": 2}, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}\"
echo
echo "To get the most recent normalized data for gateway $GATEWAY_NAME"
echo "oci iot digital-twin-instance get-content --digital-twin-instance-id  $GATEWAY_OCID"
# not generate a file containing the test data commands
echo "Gateway test data" >> $GATEWAY_TEST_DATA_FILE
echo "GATEWAY credentials $GATEWAY_CREDENTIALS" >> $GATEWAY_TEST_DATA_FILE
echo >> $GATEWAY_TEST_DATA_FILE
echo "To send the gateway stats test data using https for device $GATEWAY_NAME" >> $GATEWAY_TEST_DATA_FILE
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z' >> $GATEWAY_TEST_DATA_FILE
#echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`' >> $GATEWAY_TEST_DATA_FILE
echo curl -u \"$GATEWAY_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/$GATEWAY_ENDPOINT_GATEWAY_STATS_PATH\" -H \'Content-Type: application/json\' -d  \"{\\\"payload\\\": {\\\"haretrievesuccess\\\": 24.6,\\\"haretrievefail\\\": 1.4,\\\"uploadsuccess\\\": 24.6,\\\"uploadfail\\\": 1.4}, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}\" >> $GATEWAY_TEST_DATA_FILE
echo >> $GATEWAY_TEST_DATA_FILE
echo "To send the gateway config test data using https for device $GATEWAY_NAME" >> $GATEWAY_TEST_DATA_FILE
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z' >> $GATEWAY_TEST_DATA_FILE
echo curl -u \"$GATEWAY_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/$GATEWAY_ENDPOINT_GATEWAY_CONFIG_PATH\" -H \'Content-Type: application/json\' -d \"{\\\"payload\\\": {\\\"successfullharetrievetimewindow\\\": 14,\\\"failedharetrievetimewindow\\\": 1, \\\"successfulluploadtimewindow\\\": 12,\\\"faileduploadtimewindow\\\": 2}, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}\" >> $GATEWAY_TEST_DATA_FILE
echo >> $GATEWAY_TEST_DATA_FILE
echo "To send gateway stats test data for the configuration using mqttx for device $GATEWAY_NAME" >> $GATEWAY_TEST_DATA_FILE
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z' >> $GATEWAY_TEST_DATA_FILE
echo mqttx pub -t $GATEWAY_ENDPOINT_GATEWAY_STATS_PATH -ct application/json  -u $GATEWAY_EXTERNAL_KEY -P $GATEWAY_INSTANCE_SECRET  -h $IOT_DOMAIN_HOST -p 8883  -m   \"{\\\"payload\\\": {\\\"haretrievesuccess\\\": 24.6,\\\"haretrievefail\\\": 1.4,\\\"uploadsuccess\\\": 24.6,\\\"uploadfail\\\": 1.4}, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}\" >> $GATEWAY_TEST_DATA_FILE
echo >> $GATEWAY_TEST_DATA_FILE
echo "To send gateway config test data for the configuration using mqttx for device $GATEWAY_NAME" >> $GATEWAY_TEST_DATA_FILE
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z' >> $GATEWAY_TEST_DATA_FILE
echo mqttx pub -t $GATEWAY_ENDPOINT_GATEWAY_CONFIG_PATH -ct application/json  -u $GATEWAY_EXTERNAL_KEY -P $GATEWAY_INSTANCE_SECRET  -h $IOT_DOMAIN_HOST -p 8883  -m  \"{\\\"payload\\\": {\\\"successfullharetrievetimewindow\\\": 14,\\\"failedharetrievetimewindow\\\": 1, \\\"successfulluploadtimewindow\\\": 12,\\\"faileduploadtimewindow\\\": 2}, \\\"timestamp\\\": \\\"\$CURRENT_TS\\\"}\" >> $GATEWAY_TEST_DATA_FILE
echo >> $GATEWAY_TEST_DATA_FILE
echo "To get the most recent normalized data for gateway $GATEWAY_NAME" >> $GATEWAY_TEST_DATA_FILE
echo "oci iot digital-twin-instance get-content --digital-twin-instance-id  $GATEWAY_OCID" >> $GATEWAY_TEST_DATA_FILE
