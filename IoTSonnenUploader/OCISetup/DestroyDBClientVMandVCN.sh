#!/bin/bash

# run this in the DigitalTwin directory
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"
# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
COMPARTMENT_ID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`

echo "🔍 Searching for resources associated with $VCN_NAME..."

# --- 1. Find the Instance and Terminate it ---
INSTANCE_ID=$(oci compute instance list --compartment-id "$COMPARTMENT_ID" --display-name "$VM_NAME" --lifecycle-state RUNNING --query "data[0].id" --raw-output)

if [ -n "$INSTANCE_ID" ] && [ "$INSTANCE_ID" != "null" ]; then
    echo "🗑️ Terminating Instance and waititing for completion: $INSTANCE_ID"
    oci compute instance terminate --instance-id "$INSTANCE_ID" --preserve-boot-volume false --force --wait-for-state TERMINATED
    #echo "⏳ Waiting for instance to fully terminate..."
    #teroci compute instance wait-for-state --instance-id "$INSTANCE_ID" --status TERMINATED
else
    echo "ℹ️ No running instance found with name $VM_NAME."
fi

# --- 2. Find the VCN ID to locate child resources ---
VCN_ID=$(oci network vcn list --compartment-id "$COMPARTMENT_ID" --display-name "$VCN_NAME" --query "data[0].id" --raw-output)

if [ -z "$VCN_ID" ] || [ "$VCN_ID" == "null" ]; then
    echo "❌ VCN $VCN_NAME not found. Nothing to delete."
    exit 0
fi

echo "📍 Found VCN: $VCN_ID. Cleaning up child resources..."

# --- 3. Delete Subnets ---

for SUBNET in $(oci network subnet list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --query "data[*].id" --raw-output); do
    echo "🗑️ Deleting Subnet: $SUBNET"
    oci network subnet delete --subnet-id "$SUBNET" --force
done

# --- 4. Delete Route Tables (Skipping the default one) ---
for RT in $(oci network route-table list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --query "data[?\"is-default\" == \`false\`].id" --raw-output); do
    echo "🗑️ Deleting Route Table: $RT"
    oci network route-table delete --rt-id "$RT" --force
done

# --- 5. Delete Security Lists (Skipping the default one) ---
for SL in $(oci network security-list list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --query "data[?\"is-default\" == \`false\`].id" --raw-output); do
    echo "🗑️ Deleting Security List: $SL"
    oci network security-list delete --security-list-id "$SL" --force
done

# --- 6. Delete Gateways ---
# Service Gateway
for SG in $(oci network service-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --query "data[*].id" --raw-output); do
    echo "🗑️ Deleting Service Gateway: $SG"
    oci network service-gateway delete --service-gateway-id "$SG" --force
done

# Internet Gateway
for IG in $(oci network internet-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --query "data[*].id" --raw-output); do
    echo "🗑️ Deleting Internet Gateway: $IG"
    oci network internet-gateway delete --ig-id "$IG" --force
done

# NAT Gateway
for NG in $(oci network nat-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --query "data[*].id" --raw-output); do
    echo "🗑️ Deleting NAT Gateway: $NG"
    oci network nat-gateway delete ----nat-gateway-id "$NG" --force
done

# --- 7. Finally, Delete the VCN ---
echo "🗑️ Deleting VCN: $VCN_ID"
oci network vcn delete --vcn-id "$VCN_ID" --force

echo "✨ Cleanup Complete!"