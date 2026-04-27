#!/bin/bash -f
source ../OCISetup/common_names.sh

if [ $# -lt 1 ]
then
  echo "Using default display name for new digital twin instance of $ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME"
else
  ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME=$1
  echo "Using specified display name for new digital twin instance of $ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME"
fi
echo "Using OCI config profile $OCI_CLI_PROFILE"
# change these names as required
# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
IOT_COMPARTMENT_OCID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`
VAULT_OCID=$(oci kms management vault list \
    --compartment-id "$IOT_COMPARTMENT_OCID" \
    --all \
    --query "data[?\"display-name\"=='$VAULT_NAME'].id | [0]" \
    --raw-output)


echo Getting OCIDs
export IOT_DOMAIN_GROUP_OCID=`oci iot domain-group list --display-name $IOT_DOMAIN_GROUP_NAME --compartment-id $IOT_COMPARTMENT_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -n  "$IOT_DOMAIN_GROUP_OCID" ]]
then
  echo "Got iotdomaingroup ( $IOT_DOMAIN_GROUP_NAME ) ocid, looking for domain"
  export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE" or ."lifecycle-state" == "FAILED") | ."id"'`
else 
  echo "Can't locate IOT domain group so can't locate iot domain"
  exit -1
fi

if [[ -n "$IOT_DOMAIN_OCID" ]]
then
  echo "Got IotDomain $IOT_DOMAIN_NAME"
else
  echo "Can't locate IOT domain so can't create digital twin instance"
  exit -2
fi


#Get the host details we will need
export IOT_DOMAIN_HOST=`oci iot domain get --iot-domain-id $IOT_DOMAIN_OCID | jq -r '.data."device-host"'`
export IOT_DOMAIN_SHORT_ID=`echo $IOT_DOMAIN_HOST| tr '.' ' ' | awk '{print $1}'`


export DIGITAL_TWIN_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $DIGITAL_TWIN_MODEL_NAME | jq  -r '.data.items[] | select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$DIGITAL_TWIN_MODEL_ID" ]]
then
  echo Cannot locate Digital Twin Model $DIGITAL_TWIN_MODEL_NAME
  exit -3
else
  echo "Digital twin model $DIGITAL_TWIN_MODEL_NAME located"
fi

export DIGITAL_TWIN_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $DIGITAL_TWIN_ADAPTER_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$DIGITAL_TWIN_ADAPTER_OCID" ]]
then
  echo Cannot locate Digital Twin adapter $DIGITAL_TWIN_ADAPTER_NAME
  exit -4
else
  echo "Digital twin adapter $DIGITAL_TWIN_ADAPTER_NAME located"
fi

export DIGITAL_TWIN_INSTANCE_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$DIGITAL_TWIN_INSTANCE_OCID" ]]
then
  echo "No existing digital twin instance named $ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME will attempt to create it"
  export VAULT_OCID=`oci kms management vault list   --compartment-id $IOT_COMPARTMENT_OCID --all | jq -r ".data[] |  select (.\"display-name\" == \"$VAULT_NAME\") | select (.\"lifecycle-state\" == \"ACTIVE\") | .id"`
  if [[ -z "$VAULT_OCID" ]]
  then
    echo "Can't locate vault $VAULT_NAME unable to create a new digital twin instance"
    exit -1
  fi
  echo "Located vault $VAULT_NAME"
  export FIXED_SECRET_OCID=`oci secrets secret-bundle get-secret-bundle-by-name  --vault-id $VAULT_OCID --secret-name $FIXED_SECRET_NAME | jq -r '.data."secret-id"'`
  if [[ -z "$FIXED_SECRET_OCID" ]]
  then
    echo "Can't locate secret $FIXED_SECRET_NAME in vault $VAULT_NAME unable to create a new digital twin instance"
    exit -1
  fi
  oci iot digital-twin-instance create --iot-domain-id $IOT_DOMAIN_OCID --auth-id $FIXED_SECRET_OCID --display-name $ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME --digital-twin-adapter-id $DIGITAL_TWIN_ADAPTER_OCID --wait-for-state ACTIVE
  export DIGITAL_TWIN_INSTANCE_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
  echo "Created digital twin instance"
else
  echo "Found existing digital twin instance $ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME"
fi
if [[ -z "$DIGITAL_TWIN_INSTANCE_OCID" ]]
then
  echo "Could not locate the digital twin instance, it may not have been created, exiting"
  exit -2  
fi

echo "Getting additional digital twin instance crededntials"
# get the external key and auth details
export DIGITAL_TWIN_INSTANCE_EXTERNAL_KEY=`oci iot digital-twin-instance get --digital-twin-instance-id  $DIGITAL_TWIN_INSTANCE_OCID | jq -r  '.data."external-key"'`
# if we just created the instance we have this, but not if we found an existing one
export DIGITAL_TWIN_INSTANCE_SECRET_OCID=`oci iot digital-twin-instance get --digital-twin-instance-id  $DIGITAL_TWIN_INSTANCE_OCID | jq -r  '.data."auth-id"'`
export DIGITAL_TWIN_INSTANCE_SECRET_BASE64=`oci secrets secret-bundle get --secret-id $DIGITAL_TWIN_INSTANCE_SECRET_OCID --stage CURRENT | jq -r '.data."secret-bundle-content".content'`
export DIGITAL_TWIN_INSTANCE_SECRET=`echo $DIGITAL_TWIN_INSTANCE_SECRET_BASE64 | base64 --decode`
export DIGITAL_TWIN_INSTANCE_CREDENTIALS="$DIGITAL_TWIN_INSTANCE_EXTERNAL_KEY":"$DIGITAL_TWIN_INSTANCE_SECRET"
echo "Digital twin instance credentials $DIGITAL_TWIN_INSTANCE_CREDENTIALS"
echo "To send test data for the configuration"
echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`'
echo curl -u \"$DIGITAL_TWIN_INSTANCE_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/house/sonnenconfiguration/$ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME\" -H \'Content-Type: application/json\' -d \"{\\\"softwareVersion\\\": \\\"1.18.4\\\",\\\"time\\\": \$CURRENT_TS}\"
echo "To send text data for the status"
echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`'
echo curl -u \"$DIGITAL_TWIN_INSTANCE_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/house/sonnenstatus/$ADDITIONAL_DIGITAL_TWIN_INSTANCE_DEVICE_NAME\" -H \'Content-Type: application/json\' -d \"{\\\"batteryCharging\\\": false,\\\"consumptionAvgWattsLastMinute\\\": 339,\\\"currentBatteryCapacityPercentage\\\": 59,\\\"operatingMode\\\": 2,\\\"reservedBatteryCapacityPercentage\\\": 5,\\\"solarProductionWattsPointInTime\\\": 131,\\\"time\\\": \$CURRENT_TS}\"

echo "To send test data for the configuration using mqttx but no device specific endpoint"
echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`'
echo mqttx pub -t house/sonnenconfiguration -ct application/json  -u $DIGITAL_TWIN_INSTANCE_EXTERNAL_KEY -P $DIGITAL_TWIN_INSTANCE_SECRET  -h $IOT_DOMAIN_HOST -p 8883  -m  \"{\\\"softwareVersion\\\": \\\"1.18.8\\\",\\\"time\\\": \$CURRENT_TS}\"
echo "To send text data for the status"
echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`'
echo mqttx pub -t house/sonnenstatus -ct application/json  -u $DIGITAL_TWIN_INSTANCE_EXTERNAL_KEY -P $DIGITAL_TWIN_INSTANCE_SECRET  -h $IOT_DOMAIN_HOST -p 8883  -m  \"{\\\"batteryCharging\\\": false,\\\"consumptionAvgWattsLastMinute\\\": 339,\\\"currentBatteryCapacityPercentage\\\": 59,\\\"operatingMode\\\": 2,\\\"reservedBatteryCapacityPercentage\\\": 5,\\\"solarProductionWattsPointInTime\\\": 131,\\\"time\\\": \$CURRENT_TS}\" 
