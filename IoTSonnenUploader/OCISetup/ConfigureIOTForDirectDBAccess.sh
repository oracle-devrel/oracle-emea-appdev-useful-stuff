#!/bin/bash -f
# run this in the DigitalTwin directory
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"

#Get the tenancy OCID
TENANCY_OCID=`oci iam compartment list --all | jq -r '.data[0]."compartment-id"'`
if [[ -z "$TENANCY_OCID" ]]
then
  echo "Can't locate Tenancy OCID"
  exit -1
fi

# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
IOT_COMPARTMENT_OCID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`

if [[ -z "$IOT_COMPARTMENT_OCID" ]]
then
  echo "Can't locate Compartment  $COMPARTMENT_PATH"
  exit -1
fi

export IOT_DOMAIN_GROUP_OCID=`oci iot domain-group list --display-name $IOT_DOMAIN_GROUP_NAME --compartment-id $IOT_COMPARTMENT_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$IOT_DOMAIN_GROUP_OCID" ]]
then
  echo "Can't locate IotDomainGroup $IOT_DOMAIN_GROUP_NAME"
  exit -1
fi

echo "Found Iot Domain Group $IOT_DOMAIN_GROUP_NAME"

export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$IOT_DOMAIN_OCID" ]]
then
  echo "Can't locate IOTDomain named  $IOT_DOMAIN_NAME in compartment $COMPARTMENT_PATH and iot domain group named $IOT_DOMAIN_GROUP_NAME"
  exit -1
fi

VCN_ID=$(oci network vcn list --compartment-id "$IOT_COMPARTMENT_OCID" --display-name "$VCN_NAME" --query "data[0].id" --raw-output)
if [[ -z "$VCN_ID" ]]
then
  echo "Can't locate VCN named  $VCN_NAME in compartment $COMPARTMENT_PATH"
  exit -1
fi

oci iot domain-group configure-data-access --db-allow-listed-vcn-ids "[\"$VCN_ID\"]" --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID  --wait-for-state SUCCEEDED --wait-for-state FAILED

oci iot domain configure-direct-data-access --iot-domain-id $IOT_DOMAIN_OCID --db-allow-listed-identity-group-names "[\"$TENANCY_OCID:$IDCS_NAME/<identity-group-name>\"]"
