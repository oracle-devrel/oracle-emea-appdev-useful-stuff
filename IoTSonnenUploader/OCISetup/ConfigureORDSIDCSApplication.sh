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


if [[ "$IDCS_DOMAIN_REGION" == "home" ]]
then
  echo "IDCS Domain is in the home region"
  export IDCS_DOMAIN_REGION_CODE=`oci iam tenancy get --tenancy-id $TENANCY_OCID | jq -r '.data."home-region-key"'`
  echo "IDCS Domain is in the home region with code $IDCS_DOMAIN_REGION_CODE"
else
  export IDCS_DOMAIN_REGION_CODE=$IDCS_DOMAIN_REGION
  echo "IDCS Domain is in the specified region with code $IDCS_DOMAIN_REGION_CODE"
fi
export IDCS_DOMAIN_REGION_NAME=`oci iam region list | jq -r ".data[] | select (.key == \"$IDCS_DOMAIN_REGION_CODE\") | .name"`
echo "Region for IDCS Domain is $IDCS_DOMAIN_REGION_NAME"

# get the IDCS Domain compartment
export IDCS_DOMAIN_COMPARTMENT_OCID=`get_oci_compartment_ocid.sh $IDCS_DOMAIN_COMPARTMENT_PATH`

# get the IDCSDOmain compartment id
export IDCS_DOMAIN_OCID=`oci iam domain list --compartment-id $IDCS_DOMAIN_COMPARTMENT_OCID --all --display-name $IDCS_DOMAIN_NAME --lifecycle-state ACTIVE --region $IDCS_DOMAIN_REGION_NAME | jq -r '.data[0].id'`
if [[ -z "$IDCS_DOMAIN_OCID" ]]
then
  echo "Unable to locate IDCS domain $IDCS_DOMAIN_NAME in compartment $IDCS_DOMAIN_COMPARTMENT_PATH in region $IDCS_DOMAIN_REGION_NAME"
  exit 1
fi
echo "Located IDCS domain $IDCS_DOMAIN_NAME in compartment $IDCS_DOMAIN_COMPARTMENT_PATH in region $IDCS_DOMAIN_REGION_NAME"

# get the domain endpoint
export IDCS_DOMAIN_ENDPOINT=`oci iam domain get --domain-id $IDCS_DOMAIN_OCID --region $IDCS_DOMAIN_REGION_NAME | jq -r '.data."home-region-url"'`

export IDCS_DOMAIN_URL=`oci iam domain get --domain-id $IDCS_DOMAIN_OCID  | jq -r '.data.url'`

echo "Generic IDCS URL is $IDCS_DOMAIN_URL"

# work out the domain unique id
IDCS_UNIQUE_ID=`echo $IDCS_DOMAIN_URL | tr ':' ' ' | awk '{print $2}' | sed 's/\///g'`
echo "ITCS hostname / unique ID $IDCS_UNIQUE_ID"

IDCS_APP_SCHEMAS='["urn:ietf:params:scim:schemas:oracle:idcs:App","urn:ietf:params:scim:schemas:oracle:idcs:extension:OCITags"]'
IDCS_APP_PATCH_SCHEMAS='["urn:ietf:params:scim:api:messages:2.0:PatchOp"]'
IDCS_SEARCH_SCHEMAS='["urn:ietf:params:scim:api:messages:2.0:SearchRequest"]'

IDCS_DB_APP_TEMPLATE="{\"last-modified\": \"2019-04-29T06:34:03.000Z\",\"ref\": \"$IDCS_DOMAIN_ENDPOINT/admin/v1/AppTemplates/CustomWebAppTemplateId\",\"value\": \"CustomWebAppTemplateId\",\"well-known-id\": \"CustomWebAppTemplateId\"}"

# see if we have the app there
echo "Trying to locate application $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME "
export IDCS_ORDS_DB_ACCESS_APPLICATION_ID=`oci identity-domains apps search --filter "displayName eq \"$IDCS_ORDS_DB_ACCESS_APPLICATION_NAME\"" --schemas "$IDCS_SEARCH_SCHEMAS"  --endpoint $IDCS_DOMAIN_ENDPOINT  | jq -r '.data.resources[0].id'`
if [[ "$IDCS_ORDS_DB_ACCESS_APPLICATION_ID" = "null" ]]
then
  echo "Not found, creating"
  unset IDCS_ORDS_DB_ACCESS_APPLICATION_ID
  IDCS_ORDS_APP_EXISTS=no
else
  echo "Found integrated application $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME"
  echo "As this already exists this script will not modify what already exists"
  echo "If this integrated application is not configured for ORDS access then"
  echo "either delete it or change the name"
  echo "The script will continue to let you get the access information you may need"
  IDCS_ORDS_APP_EXISTS=yes
fi

if [[ "$IDCS_ORDS_APP_EXISTS" = "no" ]]
then
  export IDCS_ORDS_DB_ACCESS_APPLICATION_ID=`oci identity-domains app create  --endpoint $IDCS_DOMAIN_ENDPOINT --display-name $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME --client-type confidential --description "IOT Database access" --schemas "$IDCS_APP_SCHEMAS" --based-on-template "$IDCS_DB_APP_TEMPLATE" | jq -r '.data.id'`

  # make it active
  IDCS_APP_PATCH_ACTIVE_TO_TRUE="[{\"op\": \"replace\", \"path\": \"active\", \"value\": true}]"
  oci identity-domains app patch --endpoint $IDCS_DOMAIN_ENDPOINT --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID --schemas $IDCS_APP_PATCH_SCHEMAS --operations "$IDCS_APP_PATCH_ACTIVE_TO_TRUE"
  IDCS_APP_OAUTH_RESOURCE_SCOPES="[{\"description\": \"IOT DB App oauth scope\", \"display-name\": \"IOTDBAppScope\",\"fqs\": \"/$IOT_DOMAIN_GROUP_SHORT_ID/iot/$IOT_DOMAIN_SHORT_ID\",\"read-only\": null,\"requires-consent\": null,\"value\": \"/iot/$IOT_DOMAIN_SHORT_ID\"}]"
  IDCS_APP_OAUTH_CLIENT_GRANT_TYPES='["password","client_credentials","urn:ietf:params:oauth:grant-type:jwt-bearer","refresh_token"]'

  # become an oauth resource server
  oci identity-domains app put --endpoint $IDCS_DOMAIN_ENDPOINT --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID --display-name $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME --schemas "$IDCS_APP_SCHEMAS"  --based-on-template "$IDCS_DB_APP_TEMPLATE" \
    --is-o-auth-resource true --audience "/$IOT_DOMAIN_GROUP_SHORT_ID" --access-token-expiry 3600 --scopes "$IDCS_APP_OAUTH_RESOURCE_SCOPES" \
    --is-o-auth-client true --allowed-grants "$IDCS_APP_OAUTH_CLIENT_GRANT_TYPES"  --trust-scope Account --client-type confidential --force
  # seems to be a bug in the cli in that the allowed operations should be both a string and a json object so it cannot be set, let's just wait for the user to do it
  ALLOWED_OPPS=`oci identity-domains app get --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID  --endpoint $IDCS_DOMAIN_ENDPOINT | jq -r '.data."allowed-operations"'`
  while [[ ! "$ALLOWED_OPPS" =~ "introspect" ]]
  do
    echo "Until the OCI CLI buy on --allowed-operations is fixed you must go into the IDCS domaoin $IDCS_DOMAIN_NAME in region $IDCS_DOMAIN_REGION_NAME"
    echo "on the integrated applications tab search for an open the application $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME then on the OAuth tab"
    echo "edit the OAuth configuration, scroll down to the client configurtation section, under that is the  allowed operations options"
    echo "Make sure that the introspect option has been chosen."
    echo "This script will check again that this has been set in 15 seconds"
    sleep 15
    ALLOWED_OPPS=`oci identity-domains app get --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID  --endpoint $IDCS_DOMAIN_ENDPOINT | jq -r '.data."allowed-operations"'`
  done
  echo "Detected introspect in the allowed operations, continuing"
  
  echo "Granting user $IDCS_ORDS_APP_USER access to $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME"
  IDCS_ORDS_APP_USER_ID=`oci identity-domains users search  --endpoint $IDCS_DOMAIN_ENDPOINT  --filter "userName eq \"$IDCS_ORDS_APP_USER\"" --schemas "$IDCS_SEARCH_SCHEMAS" | jq -r '.data.resources[0].id'`
  if [[ "$IDCS_ORDS_APP_USER_ID" = "null" ]]
  then
    echo "User not found, cant proceed"
    exit -2
  else
    echo "Found user $IDCS_ORDS_APP_USER"
  fi
  echo "Located id for user, granting access"
  IDCS_GRANT_SCHEMA='["urn:ietf:params:scim:schemas:oracle:idcs:Grant"]'
  IDCS_APP_GRANT_USER="{\"type\": \"User\",\"value\": \"$IDCS_ORDS_APP_USER_ID\"}"
  IDCS_APP_GRANT_APP="{\"value\": \"$IDCS_ORDS_DB_ACCESS_APPLICATION_ID\"}"
  oci identity-domains grant create --endpoint $IDCS_DOMAIN_ENDPOINT --grant-mechanism ADMINISTRATOR_TO_USER --grantee "$IDCS_APP_GRANT_USER" --app "$IDCS_APP_GRANT_APP" --schemas "$IDCS_GRANT_SCHEMA" 

  echo "Granting group $IDCS_ORDS_APP_USERS_GROUP access to $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME"
  IDCS_APP_GROUP_ID=`oci identity-domains groups search --schemas "$IDCS_SEARCH_SCHEMAS" --endpoint $IDCS_DOMAIN_ENDPOINT  --filter "displayName eq \"$IDCS_ORDS_APP_USERS_GROUP\"" | jq -r '.data.resources[0].id'`
  if [[ "$IDCS_APP_GROUP_ID" = "null" ]]
  then
    echo "Group not found, cant proceed"
    exit -2
  else
    echo "Found group $IDCS_ORDS_APP_USERS_GROUP"
  fi
  IDCS_APP_GRANT_GROUP="{\"type\": \"Group\",\"value\": \"$IDCS_APP_GROUP_ID\"}"
  oci identity-domains grant create --endpoint $IDCS_DOMAIN_ENDPOINT --grant-mechanism ADMINISTRATOR_TO_GROUP --grantee "$IDCS_APP_GRANT_GROUP" --app "$IDCS_APP_GRANT_APP" --schemas "$IDCS_GRANT_SCHEMA" 

  echo "Waiting 5 mins for domain changes to propogate"
  sleep 60 
  echo "Waiting 4 mins for domain changes to propogate"
  sleep 60 
  echo "Waiting 3 mins for domain changes to propogate"
  sleep 60 
  echo "Waiting 2 mins for domain changes to propogate"
  sleep 60 
  echo "Waiting 1 min for domain changes to propogate"
  sleep 60 

  
  echo "Configuring IOT to use the IDCS setup"

  oci iot domain configure-ords-data-access --iot-domain-id $IOT_DOMAIN_OCID --db-allowed-identity-domain-host $IDCS_UNIQUE_ID --wait-for-state SUCCEEDED --wait-for-state FAILED

else
  echo "As the integrated app $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME already exisits it will not be modified and the IOT"
  echo "data access identify domain is assumed to have been configiured to use it"
  echo "If the iot domain has not been configured you will need to set it to use"
  echo "The identify domain host $IDCS_UNIQUE_ID"
fi

# get the OCID for the app incase we need it later - the app id is the domain level id, which is not the same as the OCID
export IDCS_DB_ACCESS_APPLICATION_OCID=`oci identity-domains app get --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID  --endpoint $IDCS_DOMAIN_ENDPOINT | jq -r '.data.ocid'`

export IDCS_APP_CLIENT_SECRET=`oci identity-domains app get --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID  --endpoint $IDCS_DOMAIN_ENDPOINT | jq -r '.data."client-secret"'`


# get some useful info for us later on
IDCS_CLIENT_OAUTH_ENDPOINT=$IDCS_DOMAIN_URL/oauth2/v1/token
IDCS_CLIENT_OAUTH_ID=`oci identity-domains app get --endpoint $IDCS_DOMAIN_URL --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID  | jq -r '.data.name'`
IDCS_CLIENT_OAUTH_SECRET=$IDCS_APP_CLIENT_SECRET
IDCS_CLIENT_OAUTH_SCOPE="scope=/$IOT_DOMAIN_GROUP_SHORT_ID/iot/$IOT_DOMAIN_SHORT_ID"
IDCS_CLIENT_OAUTH_GRANT_TYPE="grant_type=password"
IDCS_CLIENT_OAUTH_USER="username=$IDCS_ORDS_APP_USER"
IDCS_CLIENT_OAUTH_PASSWORD='password=$IDCS_USER_PASSWORD'


# this uses the above to get the token and so on
#export IOT_IDENTITY_DOMAIN_HOST_SHORT=`echo $IOT_IDENTITY_DOMAIN_HOST | tr '.' ' ' | awk '{print $1}'`
export IDCS_OAUTH_URL=https://$IDCS_UNIQUE_ID:443/oauth2/v1/token

# thes commands will get the token and set it IOT_DOMAIN_GROUP_SHORT_ID and IOT_DOMAIN_SHORT_ID were set in the Instructions.txt 
#export IOT_ORDS_AUTH_DETAILS=`curl -s --request POST --url "${IDCS_OAUTH_URL}" --header 'Content-Type: application/x-www-form-urlencoded'  --user "${IOT_ORDS_OAUTH_ID}:${IOT_ORDS_OAUTH_SECRET}"   --data "scope=/${IOT_DOMAIN_GROUP_SHORT_ID}/iot/${IOT_DOMAIN_SHORT_ID}"   --data "grant_type=password"   --data "username=${IOT_ORDS_IDCS_USERNAME}"  --data "password=${IOT_ORDS_IDCS_PASSWORD}"`

# export IOT_ORDS_AUTH_DETAILS=`curl -s --request POST --url "${IDCS_OAUTH_URL}" --header 'Content-Type: application/x-www-form-urlencoded'  --user "${IDCS_CLIENT_OAUTH_ID}:${IDCS_CLIENT_OAUTH_SECRET}"   --data "$IDCS_CLIENT_OAUTH_SCOPE"   --data "$IDCS_CLIENT_OAUTH_GRANT_TYPE"   --data "$IDCS_CLIENT_OAUTH_USER"  --data "$IDCS_CLIENT_OAUTH_PASSWORD"`
# export IOT_ORDS_AUTH_ACCESS_TOKEN=`echo $IOT_ORDS_AUTH_DETAILS | jq -r '.access_token'`
# export IOT_ORDS_AUTH_TOKEN_DURATION=`echo $IOT_ORDS_AUTH_DETAILS | jq -r '.expires_in'`
# export IOT_ORDS_AUTH_TOKEN_TYPE=`echo $IOT_ORDS_AUTH_DETAILS | jq -r '.token_type'`

echo "To get the token set IDCS_USER_PASSWORD to be your password, run the following command"
echo "curl -s --request POST --url \"${IDCS_OAUTH_URL}\" \\"
echo "  --header 'Content-Type: application/x-www-form-urlencoded' \\"
echo "  --user \"${IDCS_CLIENT_OAUTH_ID}:${IDCS_CLIENT_OAUTH_SECRET}\" \\"
echo "  --data \"$IDCS_CLIENT_OAUTH_SCOPE\" \\"
echo "  --data \"$IDCS_CLIENT_OAUTH_GRANT_TYPE\" \\"
echo "  --data \"$IDCS_CLIENT_OAUTH_USER\" \\"
echo "  --data \"$IDCS_CLIENT_OAUTH_PASSWORD\""

echo "To get the values in a format that can be used with curl run the following commands"

echo export IOT_ORDS_AUTH_DETAILS=\`curl -s --request POST --url \"${IDCS_OAUTH_URL}\" --header \"Content-Type: application/x-www-form-urlencoded\"  --user \"${IDCS_CLIENT_OAUTH_ID}:${IDCS_CLIENT_OAUTH_SECRET}\"   --data \"$IDCS_CLIENT_OAUTH_SCOPE\"   --data \"$IDCS_CLIENT_OAUTH_GRANT_TYPE\"   --data \"$IDCS_CLIENT_OAUTH_USER\"  --data \"$IDCS_CLIENT_OAUTH_PASSWORD\"\`
echo export IOT_ORDS_AUTH_ACCESS_TOKEN=\`echo \$IOT_ORDS_AUTH_DETAILS \| jq -r \'.access_token\'\`
echo export IOT_ORDS_AUTH_TOKEN_DURATION=\`echo \$IOT_ORDS_AUTH_DETAILS \| jq -r \'.expires_in\'\`
echo export IOT_ORDS_AUTH_TOKEN_TYPE=\`echo \$IOT_ORDS_AUTH_DETAILS \| jq -r \'.token_type\'\`
