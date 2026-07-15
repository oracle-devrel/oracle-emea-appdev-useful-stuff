#!/bin/bash -f
# make sure wildcard expansion is enabled
set +f
shopt -s nullglob

if [ $# -eq 1 ]
then
  INSTANCE_FILE=$1
else
  echo "DeleteInstance.sh only one instance file argument is supported and it must be present"
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

export INSTANCE_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $INSTANCE_DISPLAY_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$INSTANCE_OCID" ]]
then
  echo "No existing instance names $INSTANCE_DISPLAY_NAME found"
else
  echo "Found existing entity instance for $INSTANCE_DISPLAY_NAME attempting to delete it"
  oci iot digital-twin-instance delete --digital-twin-instance-id $INSTANCE_OCID --force --wait-for-state DELETED
  echo "Deleted entity instance $INSTANCE_DISPLAY_NAME"
fi
