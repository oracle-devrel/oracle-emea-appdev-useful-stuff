# run this in the DigitalTwin directory
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"
# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
IOT_COMPARTMENT_OCID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`


export IOT_DOMAIN_GROUP_OCID=`oci iot domain-group list --display-name $IOT_DOMAIN_GROUP_NAME --compartment-id $IOT_COMPARTMENT_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$IOT_DOMAIN_GROUP_OCID" ]]
then
  echo Creating IotDomainGroup $IOT_DOMAIN_GROUP_NAME
  # Create the IoT Domain Group imstructions at https://docs.oracle.com/en-us/iaas/Content/internet-of-things/create-domain-group.htm
  # to make sure it's in a ready state
  echo   oci iot domain-group create --compartment-id $IOT_COMPARTMENT_OCID --display-name $IOT_DOMAIN_GROUP_NAME --wait-for-state SUCCEEDED --wait-for-state FAILED
  oci iot domain-group create --compartment-id $IOT_COMPARTMENT_OCID --display-name $IOT_DOMAIN_GROUP_NAME --wait-for-state SUCCEEDED --wait-for-state FAILED
  # get the domain group ocid (only run this once' it's created)
  export IOT_DOMAIN_GROUP_OCID=`oci iot domain-group list --display-name $IOT_DOMAIN_GROUP_NAME --compartment-id $IOT_COMPARTMENT_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
  echo Created IotDomainGroup $IOT_DOMAIN_GROUP_NAME
else
  echo "Iot Domain Group $IOT_DOMAIN_GROUP_NAME already exists"
fi

# get the data host details
export IOT_DOMAIN_GROUP_DATA_HOST=`oci iot domain-group get --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data."data-host"'`
export IOT_DOMAIN_GROUP_SHORT_ID=`echo $IOT_DOMAIN_GROUP_DATA_HOST| tr '.' ' ' | awk '{print $1}'`

export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$IOT_DOMAIN_OCID" ]]
then
  echo Creating IotDomain $IOT_DOMAIN_NAME
  #Create the IoT domain within the group
  echo oci iot domain create --compartment-id $IOT_COMPARTMENT_OCID --display-name $IOT_DOMAIN_NAME --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID --wait-for-state SUCCEEDED --wait-for-state FAILED
  oci iot domain create --compartment-id $IOT_COMPARTMENT_OCID --display-name $IOT_DOMAIN_NAME --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID --wait-for-state SUCCEEDED --wait-for-state FAILED
  # this will return on success or fail, also this may start a work request, which runs in the background, use this command to see if it's created 
  oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID
  export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
  echo Created IotDomain $IOT_DOMAIN_NAME
else
  echo "Iot Domain $IOT_DOMAIN_NAME already exists"
fi

#Get the host details we will need
export IOT_DOMAIN_HOST=`oci iot domain get --iot-domain-id $IOT_DOMAIN_OCID | jq -r '.data."device-host"'`
export IOT_DOMAIN_SHORT_ID=`echo $IOT_DOMAIN_HOST| tr '.' ' ' | awk '{print $1}'`

export DIGITAL_TWIN_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $DIGITAL_TWIN_MODEL_NAME | jq  -r '.data.items[] | select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$DIGITAL_TWIN_MODEL_ID" ]]
then
  echo Creating Digital Twin Model $DIGITAL_TWIN_MODEL_NAME
  # Create the digtal twin model for the generic home battery, run from within the DigitalTwin folder
  echo oci iot digital-twin-model create --iot-domain-id $IOT_DOMAIN_OCID --display-name $DIGITAL_TWIN_MODEL_NAME --spec $DIGITAL_TWIN_MODEL_FILE --wait-for-state ACTIVE
  oci iot digital-twin-model create --iot-domain-id $IOT_DOMAIN_OCID --display-name $DIGITAL_TWIN_MODEL_NAME --spec $DIGITAL_TWIN_MODEL_FILE --wait-for-state ACTIVE
  # get the id of the model you just created
  export DIGITAL_TWIN_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $DIGITAL_TWIN_MODEL_NAME | jq  -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
  echo Created Digital Twin Model $DIGITAL_TWIN_MODEL_NAME
else
  echo "Digital twin model $DIGITAL_TWIN_MODEL_NAME already exists"
fi

export DIGITAL_TWIN_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $DIGITAL_TWIN_ADAPTER_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$DIGITAL_TWIN_ADAPTER_OCID" ]]
then
  echo Creating Digital Twin adapter $DIGITAL_TWIN_ADAPTER_NAME
  # create the adaptor using the multiple routes file.
  # note that as the file contents is inlined into the JSON request then the structure of the JSON files MUST be valid as JSON and also meeting the spec - if not you will get missing param errors, even though you have specified all of the actual flags correctly
  echo oci iot digital-twin-adapter create --iot-domain-id $IOT_DOMAIN_OCID --digital-twin-model-id $DIGITAL_TWIN_MODEL_ID  --inbound-routes $DIGITAL_TWIN_ADAPTOR_ROUTE_MAPPINGS_FILE  --display-name $DIGITAL_TWIN_ADAPTER_NAME  --inbound-envelope $DIGITAL_TWIN_ADAPTOR_ENVELOPE_MAPPINGS_FILE   --wait-for-state ACTIVE
  oci iot digital-twin-adapter create --iot-domain-id $IOT_DOMAIN_OCID --digital-twin-model-id $DIGITAL_TWIN_MODEL_ID  --inbound-routes $DIGITAL_TWIN_ADAPTOR_ROUTE_MAPPINGS_FILE  --display-name $DIGITAL_TWIN_ADAPTER_NAME  --inbound-envelope $DIGITAL_TWIN_ADAPTOR_ENVELOPE_MAPPINGS_FILE   --wait-for-state ACTIVE
  # get the OCID of the new adaptor
  export DIGITAL_TWIN_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $DIGITAL_TWIN_ADAPTER_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
  echo Created Digital Twin adapter $DIGITAL_TWIN_ADAPTER_NAME
else
  echo "Digital twin adapter $DIGITAL_TWIN_ADAPTER_NAME already exists"
fi

export DIGITAL_TWIN_INSTANCE_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $DIGITAL_TWIN_INSTANCE_DEVICE_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$DIGITAL_TWIN_INSTANCE_OCID" ]]
then
  echo "No existing digital twin instance named $DIGITAL_TWIN_INSTANCE_DEVICE_NAME will attempt to create it"
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
  oci iot digital-twin-instance create --iot-domain-id $IOT_DOMAIN_OCID --auth-id $FIXED_SECRET_OCID --display-name $DIGITAL_TWIN_INSTANCE_DEVICE_NAME --digital-twin-adapter-id $DIGITAL_TWIN_ADAPTER_OCID --wait-for-state ACTIVE
  export DIGITAL_TWIN_INSTANCE_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $DIGITAL_TWIN_INSTANCE_DEVICE_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
  echo "Create digital twin instance"
else
  echo "Found existing digital twin instance"
fi
if [[ -z "$DIGITAL_TWIN_INSTANCE_OCID" ]]
then
  echo "Could not locate the digital twin instance, it may not have been created, exiting"
  exit -2  
fi
echo "Getting digital twin instance crededntials"
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
echo curl -u \"$DIGITAL_TWIN_INSTANCE_CREDENTIALS\" \"https://$IOT_DOMAIN_HOST/house/sonnenconfiguration/$DIGITAL_TWIN_INSTANCE_DEVICE_NAME\" -H \'Content-Type: application/json\' -d \"{\\\"softwareVersion\\\": \\\"1.18.4\\\",\\\"time\\\": \$CURRENT_TS}\"
echo "To send text data for the status"
echo 'export CURRENT_TS=`date +%s%N | cut -b1-13`'
calen

 
