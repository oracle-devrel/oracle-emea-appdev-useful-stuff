#!/bin/bash -f
# run this in the DigitalTwin directory
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"


# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
export IOT_COMPARTMENT_OCID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`

export IOT_DOMAIN_GROUP_OCID=`oci iot domain-group list --display-name $IOT_DOMAIN_GROUP_NAME --compartment-id $IOT_COMPARTMENT_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$IOT_DOMAIN_GROUP_OCID" ]]
then
  echo "Can't locate IOT Domain group $IOT_DOMAIN_GROUP_NAME unable to proceed"
  exit 1
fi
echo "Located IOT Domain group $IOT_DOMAIN_GROUP_NAME"

export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -z "$IOT_DOMAIN_OCID" ]]
then
  echo "Can't locate IOT Domain $IOT_DOMAIN_NAME unable to proceed"
  exit 1
fi
echo "Located IOT Domain $IOT_DOMAIN_NAME"

# get the dtails of the data setuip
export IOT_DOMAIN_GROUP_DATA_HOST=`oci iot domain-group get --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data."data-host"'`
export IOT_DOMAIN_GROUP_SHORT_ID=`echo $IOT_DOMAIN_GROUP_DATA_HOST| tr '.' ' ' | awk '{print $1}'`
export IOT_DOMAIN_HOST=`oci iot domain get --iot-domain-id $IOT_DOMAIN_OCID | jq -r '.data."device-host"'`
export IOT_DOMAIN_SHORT_ID=`echo $IOT_DOMAIN_HOST| tr '.' ' ' | awk '{print $1}'`


export VAULT_OCID=`oci kms management vault list   --compartment-id $IOT_COMPARTMENT_OCID --all | jq -r ".data[] |  select (.\"display-name\" == \"$VAULT_NAME\") | select (.\"lifecycle-state\" == \"ACTIVE\") | .id"`
if [[ -z "$VAULT_OCID" ]]
then
  echo "Can't locate vault $VAULT_NAME unable to configure IOT APEX Access"
  exit -1
fi
echo "Located vault $VAULT_NAME"
export APEX_PASSWORD_SECRET_OCID=`oci secrets secret-bundle get-secret-bundle-by-name  --vault-id $VAULT_OCID --secret-name $APEX_PASSWORD_SECRET_NAME | jq -r '.data."secret-id"'`
if [[ -z "$APEX_PASSWORD_SECRET_OCID" ]]
then
  echo "Can't locate secret $APEX_PASSWORD_SECRET_NAME in vault $VAULT_NAME unable to IOT APEX access"
  exit -1
fi
echo "Located secret $APEX_PASSWORD_SECRET_NAME getting it's contents"
IOT_APEX_INITIAL_PASSWORD_BASE64=`oci secrets secret-bundle get --secret-id $APEX_PASSWORD_SECRET_OCID --stage CURRENT | jq -r '.data."secret-bundle-content".content'`
export IOT_APEX_INITIAL_PASSWORD=`echo $DIGITAL_TWIN_INSTANCE_SECRET_BASE64 | base64 --decode`
echo "Got secret $APEX_PASSWORD_SECRET_NAME contents"

# There does not seem to be a way to determine if this is already configured, so go for it anyway, worst case it will fail
echo "Configuring APEX access to domain"
oci iot domain configure-apex-data-access --iot-domain-id $IOT_DOMAIN_OCID --db-workspace-admin-initial-password $IOT_APEX_INITIAL_PASSWORD  --wait-for-state SUCCEEDED --wait-for-state FAILED



#  usefull commands
echo "To access data uploaded using the SQL workbench this command can get the most recent 10 raw data rows"
echo 'select TIME_RECEIVED, utl_raw.cast_to_varchar2(dbms_lob.substr(content)) from RAW_DATA order by TIME_RECEIVED FETCH FIRST 10 ROWS ONLY'