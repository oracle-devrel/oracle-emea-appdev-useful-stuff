#!/bin/bash -f
# make sure wildcard expansion is enabled
set +f
shopt -s nullglob

if [ $# -eq 1 ]
then
  INSTANCE_FILE=$1
else
  echo "CreateInstance.sh only one instance file argument is supported and it must be present"
fi
if [ ! -f "$INSTANCE_FILE" ]; then
  echo "Bad argument Instance file $INSTANCE_FILE in entity directory $entitydir is missing" >&2
  exit 1
fi
# this assumes that all of the other settings have been completed, AND that we are running in the folder for the model / adaptor and entity data
# load in the entity settings
echo "Processing instance file $INSTANCE_FILE in entiry directory $entitydir"
source ./$INSTANCE_FILE
# make sure that we have the right variables set
if [[ -z "$INSTANCE_DISPLAY_NAME" ]] 
then
  echo "variable INSTANCE_DISPLAY_NAME is not set, cannot create instance in directory $entitydir"
  exit -2
else 
  echo "Instance name is $INSTANCE_DISPLAY_NAME"
fi
if [[ -z "$INSTANCE_KEY" ]] 
then
  echo "variable INSTANCE_KEY is not set, cannot create entities in directory $entitydir"
  exit -2
else 
  echo "Instance key (provisional) is $INSTANCE_KEY"
fi

export INSTANCE_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $INSTANCE_DISPLAY_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$INSTANCE_OCID" ]]
then
  echo "No existing instance names $INSTANCE_DISPLAY_NAME found, will try to create it connected to gateway named $GATEWAY_DISPLAY_NAME"
  echo   oci iot digital-twin-instance create --iot-domain-id $IOT_DOMAIN_OCID --connectivity-type INDIRECT --display-name $INSTANCE_DISPLAY_NAME --gateways "[\"$GATEWAY_OCID\"]" --digital-twin-adapter-id $ENTITY_ADAPTER_OCID --external-key $INSTANCE_KEY --wait-for-state ACTIVE
  oci iot digital-twin-instance create --iot-domain-id $IOT_DOMAIN_OCID --connectivity-type INDIRECT --display-name $INSTANCE_DISPLAY_NAME --gateways "[\"$GATEWAY_OCID\"]" --digital-twin-adapter-id $ENTITY_ADAPTER_OCID --external-key $INSTANCE_KEY --wait-for-state ACTIVE
  export INSTANCE_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $INSTANCE_DISPLAY_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
  export INSTANCE_KEY=`oci iot digital-twin-instance get --digital-twin-instance-id  $INSTANCE_OCID | jq -r  '.data."external-key"'`
  echo "Create entity instance $INSTANCE_DISPLAY_NAME using key $INSTANCE_KEY"
else
  export INSTANCE_KEY=`oci iot digital-twin-instance get --digital-twin-instance-id  $INSTANCE_OCID | jq -r  '.data."external-key"'`
  echo "Found existing instance for $INSTANCE_DISPLAY_NAME with key $INSTANCE_KEY"
fi

ENDPOINT=$ENDPOINT_MAIN/$ENTITY_ENDPOINT_NAME
echo
echo "To send the instance test data using https for device $INSTANCE_DISPLAY_NAME"
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z'
#echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`'
echo curl -u \"$GATEWAY_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/$ENDPOINT\" -H \'Content-Type: application/json\' -d \"{\\\"payload\\\": $ENTITY_SAMPLE_DATA, \\\"devicekey\\\":\\\"$INSTANCE_KEY\\\"}\"
echo
echo "To send instance test data using mqttx for device $INSTANCE_DISPLAY_NAME"
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z'
#echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`'
echo mqttx pub -t $ENDPOINT -ct application/json  -u $GATEWAY_EXTERNAL_KEY -P $GATEWAY_INSTANCE_SECRET  -h $IOT_DOMAIN_HOST -p 8883  -m  \"{\\\"payload\\\": $ENTITY_SAMPLE_DATA, \\\"devicekey\\\":\\\"$INSTANCE_KEY\\\"}\"
echo
echo "To get the most recent normalized data for instance $INSTANCE_DISPLAY_NAME"
echo "oci iot digital-twin-instance get-content --digital-twin-instance-id  $INSTANCE_OCID"

# now save to the entiry file

echo >> $ENTITY_TEST_DATA_FILE
echo >> $ENTITY_TEST_DATA_FILE
echo >> $ENTITY_TEST_DATA_FILE
echo "Testing commands for instance $INSTANCE_DISPLAY_NAME of model $ENTITY_MODEL_NAME" >> $ENTITY_TEST_DATA_FILE
echo >> $ENTITY_TEST_DATA_FILE
echo "To send the instance test data using https for device $INSTANCE_DISPLAY_NAME" >> $ENTITY_TEST_DATA_FILE
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z' >> $ENTITY_TEST_DATA_FILE
echo curl -u \"$GATEWAY_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/$ENDPOINT\" -H \'Content-Type: application/json\' -d \"{\\\"payload\\\": $ENTITY_SAMPLE_DATA, \\\"devicekey\\\":\\\"$INSTANCE_KEY\\\"}\" >> $ENTITY_TEST_DATA_FILE
echo >> $ENTITY_TEST_DATA_FILE
echo "To send instance test data using mqttx for device $INSTANCE_DISPLAY_NAME" >> $ENTITY_TEST_DATA_FILE
echo 'export CURRENT_TS=`date -u +"%Y-%m-%dT%H:%M:%S"`.`date +%N | cut -b 1-3`Z' >> $ENTITY_TEST_DATA_FILE
echo mqttx pub -t $ENDPOINT -ct application/json  -u $GATEWAY_EXTERNAL_KEY -P $GATEWAY_INSTANCE_SECRET  -h $IOT_DOMAIN_HOST -p 8883  -m  \"{\\\"payload\\\": $ENTITY_SAMPLE_DATA, \\\"devicekey\\\":\\\"$INSTANCE_KEY\\\"}\" >> $ENTITY_TEST_DATA_FILE
echo >> $ENTITY_TEST_DATA_FILE
echo "To get the most recent normalized data for instance $INSTANCE_DISPLAY_NAME" >> $ENTITY_TEST_DATA_FILE
echo "oci iot digital-twin-instance get-content --digital-twin-instance-id  $INSTANCE_OCID" >> $ENTITY_TEST_DATA_FILE