# run this in the DigitalTwin directory
# these should not be shared outside oracle
source ../OCISetup/OCIDs.sh

# change these names as required
export IOT_DOMAIN_GROUP_NAME=iot-domain-group-timg
export IOT_DOMAIN_NAME=iot-domain-timg


export DIGITAL_TWIN_MODEL_NAME=testmodel
export DIGITAL_TWIN_MODEL_FILE_NAME=testmodel.json
export DIGITAL_TWIN_MODEL_FILE=file://$DIGITAL_TWIN_MODEL_FILE_NAME

export DIGITAL_TWIN_ADAPTOR_ROUTE_MAPPINGS_FILE=file://testadapter.json
export DIGITAL_TWIN_ADAPTOR_ENVELOPE_MAPPINGS_FILE=file://testenvelope.json
export DIGITAL_TWIN_ADAPTER_NAME=testadapter

if [[ -z "$IOT_COMPARTMENT_OCID" ]]
then
  echo "IOT_COMPARTMENT_OCID is not set, this should contain the compartment ocid that contains the IOT domain group and other resources"
fi

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
  # get the data host
  export IOT_DOMAIN_GROUP_DATA_HOST=`oci iot domain-group get --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data."data-host"'`
  export IOT_DOMAIN_GROUP_SHORT_ID=`echo $IOT_DOMAIN_GROUP_DATA_HOST| tr '.' ' ' | awk '{print $1}'`
  echo Created IotDomainGroup $IOT_DOMAIN_GROUP_NAME
else
  echo "Iot Domain Group $IOT_DOMAIN_GROUP_NAME already exists"
fi

export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$IOT_DOMAIN_OCID" ]]
then
  echo Creating IotDomain $IOT_DOMAIN_NAME
  #Create the IoT domain within the group
  echo oci iot domain create --compartment-id $IOT_COMPARTMENT_OCID --display-name $IOT_DOMAIN_NAME --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID --wait-for-state SUCCEEDED --wait-for-state FAILED
  oci iot domain create --compartment-id $IOT_COMPARTMENT_OCID --display-name $IOT_DOMAIN_NAME --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID --wait-for-state SUCCEEDED --wait-for-state FAILED
  # this will return on success or fail, also this may start a work request, which runs in the background, use this command to see if it's created 
  oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID
  #Get the OCID we will need
  export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
  export IOT_DOMAIN_HOST=`oci iot domain get --iot-domain-id $IOT_DOMAIN_OCID | jq -r '.data."device-host"'`
  export IOT_DOMAIN_SHORT_ID=`echo $IOT_DOMAIN_HOST| tr '.' ' ' | awk '{print $1}'`
  echo Created IotDomain $IOT_DOMAIN_NAME
else
  echo "Iot Domain $IOT_DOMAIN_NAME already exists"
fi


export DIGITAL_TWIN_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $DIGITAL_TWIN_MODEL_NAME | jq  -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
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
# and the models name
export DIGITAL_TWIN_MODEL_IDENTIFIER=`cat $DIGITAL_TWIN_MODEL_FILE_NAME | jq -r '.["@id"]'`


export DIGITAL_TWIN_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $DIGITAL_TWIN_ADAPTER_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$DIGITAL_TWIN_ADAPTER_OCID" ]]
then
  echo Creating Digital Twin adapter $DIGITAL_TWIN_ADAPTER_NAME
  # create the adaptor using the multiple routes file.
  # note that as the file contents is inlined into the JSON request then the stricture of the JSON files MUST be OK (if not you'll get missing param errors, even though you have specifried all fo the actual flags correctly
  echo oci iot digital-twin-adapter create --iot-domain-id $IOT_DOMAIN_OCID --digital-twin-model-id $DIGITAL_TWIN_MODEL_ID  --inbound-routes $DIGITAL_TWIN_ADAPTOR_ROUTE_MAPPINGS_FILE  --display-name $DIGITAL_TWIN_ADAPTER_NAME  --inbound-envelope $DIGITAL_TWIN_ADAPTOR_ENVELOPE_MAPPINGS_FILE   --wait-for-state ACTIVE
  oci iot digital-twin-adapter create --iot-domain-id $IOT_DOMAIN_OCID --digital-twin-model-id $DIGITAL_TWIN_MODEL_ID  --inbound-routes $DIGITAL_TWIN_ADAPTOR_ROUTE_MAPPINGS_FILE  --display-name $DIGITAL_TWIN_ADAPTER_NAME  --inbound-envelope $DIGITAL_TWIN_ADAPTOR_ENVELOPE_MAPPINGS_FILE   --wait-for-state ACTIVE
  # get the OCID of the new adaptor
  export DIGITAL_TWIN_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $DIGITAL_TWIN_ADAPTER_NAME | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
  echo Created Digital Twin adapter $DIGITAL_TWIN_ADAPTER_NAME
else
  echo "Digital twin adapter $DIGITAL_TWIN_ADAPTER_NAME already exists"
fi
echo Remember, no digital twin instances have been created
