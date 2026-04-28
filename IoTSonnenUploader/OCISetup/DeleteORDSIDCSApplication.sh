
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
echo "IDCS hostname / unique ID $IDCS_UNIQUE_ID"

IDCS_APP_SCHEMAS='["urn:ietf:params:scim:schemas:oracle:idcs:App","urn:ietf:params:scim:schemas:oracle:idcs:extension:OCITags"]'
IDCS_APP_PATCH_SCHEMAS='["urn:ietf:params:scim:api:messages:2.0:PatchOp"]'
IDCS_SEARCH_SCHEMAS='["urn:ietf:params:scim:api:messages:2.0:SearchRequest"]'

IDCS_DB_APP_TEMPLATE="{\"last-modified\": \"2019-04-29T06:34:03.000Z\",\"ref\": \"$IDCS_DOMAIN_ENDPOINT/admin/v1/AppTemplates/CustomWebAppTemplateId\",\"value\": \"CustomWebAppTemplateId\",\"well-known-id\": \"CustomWebAppTemplateId\"}"

# see if we have the app there
echo "Trying to locate application $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME "
export IDCS_ORDS_DB_ACCESS_APPLICATION_ID=`oci identity-domains apps search --filter "displayName eq \"$IDCS_ORDS_DB_ACCESS_APPLICATION_NAME\"" --schemas "$IDCS_SEARCH_SCHEMAS"  --endpoint $IDCS_DOMAIN_ENDPOINT  | jq -r '.data.resources[0].id'`
if [[ "$IDCS_ORDS_DB_ACCESS_APPLICATION_ID" = "null" ]]
then
  echo "Not found, exiting"
  exit -1
fi
echo "Found integrated application $IDCS_ORDS_DB_ACCESS_APPLICATION_NAME"
echo "Deactivating it"
# make it active
IDCS_APP_PATCH_ACTIVE_TO_FALSE="[{\"op\": \"replace\", \"path\": \"active\", \"value\": false}]"
oci identity-domains app patch --endpoint $IDCS_DOMAIN_ENDPOINT --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID --schemas $IDCS_APP_PATCH_SCHEMAS --operations "$IDCS_APP_PATCH_ACTIVE_TO_FALSE"
echo "Deactivated it, now deleting it"
oci identity-domains app delete --endpoint $IDCS_DOMAIN_ENDPOINT  --app-id $IDCS_ORDS_DB_ACCESS_APPLICATION_ID --force

echo "Deleted - this may take a few mins to propogate"