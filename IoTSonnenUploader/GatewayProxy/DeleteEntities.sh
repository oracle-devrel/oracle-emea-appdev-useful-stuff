#!/bin/bash -f
# make sure wildcard expansion is enabled
set +f
shopt -s nullglob
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

export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$IOT_DOMAIN_OCID" ]]
then
    echo "Can not locate IotDomain $IOT_DOMAIN_NAME can not proceed"
    exit 2
else
  echo "Iot Domain $IOT_DOMAIN_NAME located"
fi
echo "Creating entity models and instances"
# we have setup everything, now we need to go through the entities in the entities folder creating the models and adapters
pushd $GATEWAY_ENTITIES_DIRECTORY > /dev/null
# Load the entities specific settings
source ./entity_names.sh
for entitydir in *"$ENTITY_DIRECTORY_SUFFIX"; do
  [ -d "$entitydir" ] || continue
  echo "Entities directory $entitydir"
  pushd $entitydir > /dev/null
  # make sure that the entries exist to create the model and adapter
  if compgen -G "*${ENTITY_DATA_FILE_SUFFIX}" > /dev/null; then
    echo "All required files exist to process deletions in model directory $entitydir"
    # lets run it
    export entitydir
    ../DeleteEntity.sh
  else
    echo "Missing Config data ($ENTITY_DATA_FILE_SUFFIX)"
    echo "Cannot delete entities in directory $entitydir"
  fi
  popd > /dev/null
done
# final move out back to the starting directory
popd > /dev/null