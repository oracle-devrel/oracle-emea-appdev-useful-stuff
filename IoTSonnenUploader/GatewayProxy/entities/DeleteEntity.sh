#!/bin/bash -f
# make sure wildcard expansion is enabled
set +f
shopt -s nullglob
# this assumes that all of the other settings have been completed, AND that we are running in the folder for the model / adaptor and entity data
# load in the entity settings
echo "Processing in $entitydir"
source ./*"$ENTITY_DATA_FILE_SUFFIX"
# lets try and delete the actual instances
for instancefile in *"$ENTITY_INSTANCE_SUFFIX"; do
  [ -f "$instancefile" ] || continue
  echo "Instance file $instancefile"
  ../DeleteInstance.sh $instancefile
done

# make sure that we have the right variables set
if [[ -z "$ENTITY_MODEL_NAME" ]] 
then
  echo "variable ENTITY_MODEL_NAME is not set, cannot delete entities in directory $entitydir"
  exit -2
fi
if [[ -z "$ENTITY_ADAPTER_NAME" ]] 
then
  echo "variable ENTITY_ADAPTER_NAME is not set, cannot delete entities in directory $entitydir"
  exit -2
fi
export ENTITY_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $ENTITY_ADAPTER_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -n  "$ENTITY_ADAPTER_OCID" ]]
then
  echo "Located entity adapter $ENTITY_ADAPTER_NAME, deleting it"
  oci iot digital-twin-adapter delete --digital-twin-adapter-id $ENTITY_ADAPTER_OCID --force --wait-for-state DELETED
  echo "Deleted entity adapter $ENTITY_ADAPTER_NAME"
else 
  echo "Can't locate entity adapter $ENTITY_ADAPTER_NAME"
fi

export ENTITY_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $ENTITY_MODEL_NAME | jq  -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -n  "$ENTITY_MODEL_ID" ]]
then
  echo "Located digital twin entity model $ENTITY_MODEL_NAME, deleting it"
  oci iot digital-twin-model delete --digital-twin-model-id $ENTITY_MODEL_ID --force --wait-for-state DELETED
  echo "Deleted digital twin entity model $ENTITY_MODEL_NAME"
else 
  echo "Can't locate digital twin entity model $ENTITY_MODEL_NAME"
fi
# make sure its there, then delete it
touch $ENTITY_TEST_DATA_FILE
rm $ENTITY_TEST_DATA_FILE