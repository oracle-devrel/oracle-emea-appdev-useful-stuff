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

export IOT_DOMAIN_HOST=`oci iot domain get --iot-domain-id $IOT_DOMAIN_OCID | jq -r '.data."device-host"'`
export IOT_DOMAIN_SHORT_ID=`echo $IOT_DOMAIN_HOST| tr '.' ' ' | awk '{print $1}'`


VCN_ID=$(oci network vcn list --compartment-id "$IOT_COMPARTMENT_OCID" --display-name "$VCN_NAME" --query "data[0].id" --raw-output)
if [[ -z "$VCN_ID" ]]
then
  echo "Can't locate VCN named  $VCN_NAME in compartment $COMPARTMENT_PATH"
  exit -1
fi

echo "Configuring iot domain group to connect to VCN [\"$VCN_ID\"]"
oci iot domain-group configure-data-access --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID  --db-allow-listed-vcn-ids "[\"$VCN_ID\"]" --wait-for-state SUCCEEDED --wait-for-state FAILED
echo "Completed iot domain group vcn setup"

echo "Configuring iot domain with identity group name [\"$TENANCY_OCID:$IOT_CLIENT_VM_DYNAMIC_GROUP_IDCS_NAME/$IOT_CLIENT_VM_DYNAMIC_GROUP_NAME\"]"
oci iot domain configure-direct-data-access --iot-domain-id $IOT_DOMAIN_OCID --db-allow-listed-identity-group-names "[\"$TENANCY_OCID:$IOT_CLIENT_VM_DYNAMIC_GROUP_IDCS_NAME/$IOT_CLIENT_VM_DYNAMIC_GROUP_NAME\"]"   --wait-for-state SUCCEEDED --wait-for-state FAILED
echo "Completed iot domain identity group setup"

export DB_TOKEN_SCOPE=`oci iot domain-group  get --iot-domain-group-id  $IOT_DOMAIN_GROUP_OCID | jq -r '.data."db-token-scope"'`
export DB_CONNECTION_STRING=`oci iot domain-group  get --iot-domain-group-id  $IOT_DOMAIN_GROUP_OCID | jq -r '.data."db-connection-string"'`
echo "OCI Command to get the DB token (on the VM only)"
echo oci iam db-token get --scope \"$DB_TOKEN_SCOPE\" --auth instance_principal

echo "SQL CLI command (on vm) to access database"
echo sql /@\"jdbc:oracle:thin:@$DB_CONNECTION_STRING\&TOKEN_AUTH=OCI_TOKEN\"

echo "SQL (on VM) to set the current scheme to the IOT schema"
echo alter session set current_schema="$IOT_DOMAIN_SHORT_ID"__iot \;

echo "Java command to run the test jar file in the IOTDB project folder (once mvn clean package is completed)"
echo java -Dmicronaut.config.files=configsecure/configsecure.properties,config/config.properties -jar target/IoTDBJDBC-\*.jar

OCI_CONFIG_REGION=$(awk -F= -v profile="$OCI_CLI_PROFILE" '
  $0 ~ "\\["profile"\\]" {found=1; next}
  /^\[/ {found=0}
  found && $1 ~ /region/ {print $2; exit}
' ~/.oci/config | xargs)

DB_CONNECTION_NAME=`echo $DB_CONNECTION_STRING | awk -F '/' '{print $2}' | awk -F '.' '{print $1}'`
DB_COMPARTMENT_NAME=`echo $DB_TOKEN_SCOPE | awk -F ':' '{print $7}'`
echo "Config settings for the IoTDBJDBC"
echo iotdatacache.schemaname="$IOT_DOMAIN_SHORT_ID"__iot
echo iotdatacache.ociregion=$OCI_CONFIG_REGION
echo iotdatacache.connectionname=$DB_CONNECTION_NAME
echo oci.dbtoken.compartment=$DB_COMPARTMENT_NAME
echo oci.auth.type=InstancePrinciple