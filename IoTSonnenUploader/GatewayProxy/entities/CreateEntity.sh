#!/bin/bash -f
# make sure wildcard expansion is enabled
set +f
shopt -s nullglob
# this assumes that all of the other settings have been completed, AND that we are running in the folder for the model / adaptor and entity data
# load in the entity settings
echo "Processing in $entitydir"
source ./*"$ENTITY_DATA_FILE_SUFFIX"
# make sure that we have the right variables set
if [[ -z "$ENTITY_MODEL_NAME" ]] 
then
  echo "variable ENTITY_MODEL_NAME is not set, cannot create entities in directory $entitydir"
  exit -2
else 
  echo "Model name is $ENTITY_MODEL_NAME"
fi
if [[ -z "$ENTITY_ADAPTER_NAME" ]] 
then
  echo "variable ENTITY_ADAPTER_NAME is not set, cannot create entities in directory $entitydir"
  exit -2
else 
  echo "Adapter name is $ENTITY_ADAPTER_NAME"
fi
# we know that the file names must exist but let's make sure there's only one
# the model file
matches=(*"$ENTITY_MODEL_SUFFIX")
if [ "${#matches[@]}" -eq 1 ] && [ -e "${matches[0]}" ]; then
  ENTITY_MODEL_FILE_NAME="${matches[0]}"
else
  echo "Expected exactly one match for *$ENTITY_MODEL_SUFFIX, found ${#matches[@]}"
  exit 1
fi
ENTITY_MODEL_FILE=file://$ENTITY_MODEL_FILE_NAME

# the routes file
matches=(*"$ENTITY_ROUTES_SUFFIX")
if [ "${#matches[@]}" -eq 1 ] && [ -e "${matches[0]}" ]; then
  ENTITY_ROUTES_FILE_NAME="${matches[0]}"
else
  echo "Expected exactly one match for *$ENTITY_ROUTES_FILE_NAME, found ${#matches[@]}"
  exit 1
fi
ENTITY_ROUTES_FILE=file://$ENTITY_ROUTES_FILE_NAME

# the envelope file
matches=(*"$ENTITY_ENVELOPE_SUFFIX")
if [ "${#matches[@]}" -eq 1 ] && [ -e "${matches[0]}" ]; then
  ENTITY_ENVELOPE_FILE_NAME="${matches[0]}"
else
  echo "Expected exactly one match for *$ENTITY_ENVELOPE_FILE_NAME, found ${#matches[@]}"
  exit 1
fi
ENTITY_ENVELOPE_FILE=file://$ENTITY_ENVELOPE_FILE_NAME

echo "Working with model $ENTITY_MODEL_FILE Route $ENTITY_ROUTES_FILE and Envelope $ENTITY_ENVELOPE_FILE"
export ENTITY_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $ENTITY_MODEL_NAME | jq  -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$ENTITY_MODEL_ID" ]]
then
  echo Creating Digital Twin Model for entity $ENTITY_MODEL_NAME
  # Create the digtal twin model for the entiry
  echo oci iot digital-twin-model create --iot-domain-id $IOT_DOMAIN_OCID --display-name $ENTITY_MODEL_NAME --spec "$ENTITY_MODEL_FILE" --wait-for-state ACTIVE
  oci iot digital-twin-model create --iot-domain-id $IOT_DOMAIN_OCID --display-name $ENTITY_MODEL_NAME --spec "$ENTITY_MODEL_FILE" --wait-for-state ACTIVE
  # get the id of the model you just created
  export ENTITY_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $ENTITY_MODEL_NAME | jq  -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
  echo Created Digital Twin entity Model $ENTITY_MODEL_NAME
else
  echo "Digital twin model for entity $ENTITY_MODEL_NAME already exists"
fi

export ENTITY_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $ENTITY_ADAPTER_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$ENTITY_ADAPTER_OCID" ]]
then
  echo Creating Digital Twin entity adapter $ENTITY_ADAPTER_NAME
  # create the adaptor using the multiple routes file.
  # note that as the file contents is inlined into the JSON request then the structure of the JSON files MUST be valid as JSON and also meeting the spec - if not you will get missing param errors, even though you have specified all of the actual flags correctly
  echo oci iot digital-twin-adapter create --iot-domain-id $IOT_DOMAIN_OCID --digital-twin-model-id $ENTITY_MODEL_ID  --inbound-routes "$ENTITY_ROUTES_FILE"  --display-name $ENTITY_ADAPTER_NAME  --inbound-envelope "$ENTITY_ENVELOPE_FILE"   --wait-for-state ACTIVE
  oci iot digital-twin-adapter create --iot-domain-id $IOT_DOMAIN_OCID --digital-twin-model-id $ENTITY_MODEL_ID  --inbound-routes "$ENTITY_ROUTES_FILE"  --display-name $ENTITY_ADAPTER_NAME  --inbound-envelope "$ENTITY_ENVELOPE_FILE"   --wait-for-state ACTIVE
  # get the OCID of the new adaptor
  export ENTITY_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $ENTITY_ADAPTER_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
  echo Created entity adapter $ENTITY_ADAPTER_NAME
else
  echo "Entity adapter $ENTITY_ADAPTER_NAME already exists"
fi
# reset the entiry info file
echo "Entity test commands" > $ENTITY_TEST_DATA_FILE
echo "Looking for instance data files"
# lets try and create the actual instances
for instancefile in *"$ENTITY_INSTANCE_SUFFIX"; do
  [ -f "$instancefile" ] || continue
  echo "Instance file $instancefile"
  ../CreateInstance.sh $instancefile
done