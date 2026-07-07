source ../OCISetup/common_names.sh
source ./gateway_names.sh
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
export IOT_DOMAIN_GROUP_OCID=`oci iot domain-group list --display-name $IOT_DOMAIN_GROUP_NAME --compartment-id $IOT_COMPARTMENT_OCID | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`

if [[ -n  "$IOT_DOMAIN_GROUP_OCID" ]]
then
  echo "Located domain group $IOT_DOMAIN_GROUP_NAME, looking for domain"
else 
  echo "Can't locate IOT domain group $IOT_DOMAIN_GROUP_NAME can't continue"
  exit -1
fi

export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r 'first(.data.items[]| select (."lifecycle-state" == "ACTIVE" or ."lifecycle-state" == "FAILED") | ."id") // empty'`
if [[ -n  "$IOT_DOMAIN_OCID" ]]
then
  echo "Located domain $IOT_DOMAIN_NAME, looking for digital twin instance"
else 
  echo "Can't locate IOT domain group $IOT_DOMAIN_NAME can't continue"
  exit -2
fi

# try to locate and delete the instance
export GATEWAY_INSTANCE_OCID=`oci iot digital-twin-instance list --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_DISPLAY_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -n  "$GATEWAY_INSTANCE_OCID" ]]
then
  echo "Located gateway instance $GATEWAY_DISPLAY_NAME, deleting it"
  oci iot digital-twin-instance delete --digital-twin-instance-id $GATEWAY_INSTANCE_OCID --force --wait-for-state DELETED
  echo "Deleted gateway instance $GATEWAY_DISPLAY_NAME"
else 
  echo "Can't locate gateway instance $GATEWAY_DISPLAY_NAME"
fi
export GATEWAY_ADAPTER_OCID=`oci iot digital-twin-adapter  list --iot-domain-id $IOT_DOMAIN_OCID  --display-name $GATEWAY_ADAPTER_NAME | jq -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -n  "$GATEWAY_ADAPTER_OCID" ]]
then
  echo "Located gateway adapter $GATEWAY_ADAPTER_NAME, deleting it"
  oci iot digital-twin-adapter delete --digital-twin-adapter-id $GATEWAY_ADAPTER_OCID --force --wait-for-state DELETED
  echo "Deleted gateway adapter $GATEWAY_ADAPTER_NAME"
else 
  echo "Can't locate gateway adapter $GATEWAY_ADAPTER_NAME"
fi

export GATEWAY_MODEL_ID=`oci iot digital-twin-model list --iot-domain-id $IOT_DOMAIN_OCID --display-name $GATEWAY_MODEL_NAME | jq  -r 'first(.data.items[] | select(."lifecycle-state" == "ACTIVE") | ."id") // empty'`
if [[ -n  "$GATEWAY_MODEL_ID" ]]
then
  echo "Located digital twin gateway model $GATEWAY_MODEL_NAME, deleting it"
  oci iot digital-twin-model delete --digital-twin-model-id $GATEWAY_MODEL_ID --force --wait-for-state DELETED
  echo "Deleted digital twin gateway model $GATEWAY_MODEL_NAME"
else 
  echo "Can't locate digital twin gateway model $GATEWAY_MODEL_NAME"
fi