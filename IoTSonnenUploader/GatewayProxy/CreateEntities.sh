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

export GATEWAY_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_DISPLAY_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -z "$GATEWAY_OCID" ]]
then
  echo "No existing gateway named $GATEWAY_DISPLAY_NAME found, cannot continue"
  exit -1
else 
  echo "Located existing gateway named $GATEWAY_DISPLAY_NAME"
fi
# try and get the gateway credentials
export GATEWAY_EXTERNAL_KEY=`oci iot digital-twin-instance get --digital-twin-instance-id  $GATEWAY_OCID | jq -r  '.data."external-key"'`
export GATEWAY_SECRET_OCID=`oci iot digital-twin-instance get --digital-twin-instance-id  $GATEWAY_OCID | jq -r  '.data."auth-id"'`
export GATEWAY_INSTANCE_SECRET_BASE64=`oci secrets secret-bundle get --secret-id $GATEWAY_SECRET_OCID --stage CURRENT | jq -r '.data."secret-bundle-content".content'`
export GATEWAY_INSTANCE_SECRET=`echo $GATEWAY_INSTANCE_SECRET_BASE64 | base64 --decode`
export GATEWAY_CREDENTIALS="$GATEWAY_EXTERNAL_KEY":"$GATEWAY_INSTANCE_SECRET"
echo "GATEWAY credentials $GATEWAY_CREDENTIALS"

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
 if compgen -G "*${ENTITY_MODEL_SUFFIX}" > /dev/null \
  && compgen -G "*${ENTITY_ROUTES_SUFFIX}" > /dev/null \
  && compgen -G "*${ENTITY_ENVELOPE_SUFFIX}" > /dev/null \
  && compgen -G "*${ENTITY_DATA_FILE_SUFFIX}" > /dev/null; then
    echo "All required files exist for entity creation in model directory $entitydir"
    # lets run it
    export entitydir
    ../CreateEntity.sh
  else
    echo "Missing one or more required files from Model ($ENTITY_MODEL_SUFFIX), Routes ($ENTITY_ROUTES_SUFFIX), Envolope ($ENTITY_ENVELOPE_SUFFIX), or Config data ($ENTITY_DATA_FILE_SUFFIX)"
    echo "Cannot create entities in directory $entitydir"
  fi
  popd > /dev/null
done
# final move out back to the starting directory
popd > /dev/null