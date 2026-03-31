#!/bin/bash

# Usage: ./get_oci_compartment_ocid.sh "/TopLevel/Marketing/Production"
PATH_INPUT=$1

# 1. Strip leading slash if present to avoid empty first array element
PATH_STRING="${PATH_INPUT#/}"

# 2. Get the Tenancy OCID (The Root)
# This assumes your CLI is configured with a default profile and that there is at least one compartement in the tenenacy root
# using all is the only way to suppress pages still remaining options, even theough we only care about the first value returned :-(
TENANCY_OCID=`oci iam compartment list --all | jq -r '.data[0]."compartment-id"'`
#TENANCY_OCID=$(oci iam compartment get --compartment-id $(oci os ns get | jq -r '.data') --query 'data.id' --raw-output 2>/dev/null || oci iam tenancy get --tenancy-id $(oci iam compartment list --all --compartment-id-in-subtree true --query "data[0].\"compartment-id\"" --raw-output) --query "data.id" --raw-output)
#TENANCY_OCID=$(oci iam tenancy get --query "data.id" --raw-output)

if [ -z "$TENANCY_OCID" ]; then
    echo "Error: Could not retrieve Tenancy OCID. Check your OCI CLI configuration."
    exit 1
fi

current_parent_id=$TENANCY_OCID

# 3. Split the path by '/' and iterate
IFS='/' read -ra ADDR <<< "$PATH_STRING"

for i in "${ADDR[@]}"; do
  # Skip empty strings (handles trailing slashes or double slashes)
  if [ -z "$i" ]; then continue; fi

  # Find the child compartment matching the name under the current parent
  current_parent_id=$(oci iam compartment list \
    --compartment-id "$current_parent_id" \
    --lifecycle-state ACTIVE \
    --query "data[?name=='$i'].id | [0]" \
    --raw-output)

  if [ "$current_parent_id" == "null" ] || [ -z "$current_parent_id" ]; then
    echo "Error: Compartment '$i' not found in the current path."
    exit 1
  fi
done

echo "$current_parent_id"