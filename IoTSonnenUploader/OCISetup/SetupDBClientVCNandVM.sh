#!/bin/bash
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"
# --- Configuration ---
# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
COMPARTMENT_ID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`
echo "✅ Located all compartment id:  $COMPARTMENT_ID"
# --- 1. Network Infrastructure ---
SERVICE_ID=$(oci network service list --query "data[?contains(name, 'All ')].id | [0]" --raw-output)
echo "✅ Located all services id:  $SERVICE_ID"
VCN_ID=$(oci network vcn create --compartment-id "$COMPARTMENT_ID" --display-name "$VCN_NAME" --cidr-block "$VCN_CIDR" --query "data.id" --raw-output)
echo "✅ VCN Gateway Created: $VCN_ID"
IG_ID=$(oci network internet-gateway create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "IGW" --is-enabled true --query "data.id" --raw-output)
echo "✅ Internet Gateway Created: $ID_ID"
# Create NAT Gateway for outgoing traffic from private resources
NAT_ID=$(oci network nat-gateway create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "NAT_Gateway" --query "data.id" --raw-output)
echo "✅ NAT Gateway Created: $NAT_ID"
SG_ID=$(oci network service-gateway create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "SG" --services "[{\"serviceId\":\"$SERVICE_ID\"}]" --query "data.id" --raw-output)
echo "✅ Service Gateway Created: $SG_ID"

# --- 2. Security List with VCN-Wide DB Egress ---
SL_ID=$(oci network security-list create \
    --compartment-id "$COMPARTMENT_ID" \
    --vcn-id "$VCN_ID" \
    --display-name "VCN_Wide_DB_Security" \
    --egress-security-rules "[
        {\"destination\":\"0.0.0.0/0\",\"protocol\":\"all\",\"description\":\"Allow all outbound for internet/updates\"},
        {\"destination\":\"$VCN_CIDR\",\"protocol\":\"6\",\"tcpOptions\":{\"destinationPortRange\":{\"max\":1522,\"min\":1521}},\"description\":\"Allow DB access to any resource in the VCN\"}
    ]" \
    --ingress-security-rules "[
        {\"source\":\"0.0.0.0/0\",\"protocol\":\"6\",\"tcpOptions\":{\"destinationPortRange\":{\"max\":22,\"min\":22}},\"description\":\"Public SSH Access\"},
        {\"source\":\"$SUBNET_CIDR\",\"protocol\":\"6\",\"tcpOptions\":{\"destinationPortRange\":{\"max\":1522,\"min\":1521}},\"description\":\"Allow DB traffic from this subnet\"}
    ]" --query "data.id" --raw-output)
echo "✅ Security liss Created: $SL_ID"
# --- 3. Routing and Subnet ---
echo creating route table as oci network route-table create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "Public_RT" --route-rules "[{\"cidrBlock\":\"0.0.0.0/0\",\"networkEntityId\":\"$NAT_ID\"}]" --query "data.id" --raw-output
PUB_RT_ID=$(oci network route-table create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "Public_RT" --route-rules "[{\"cidrBlock\":\"0.0.0.0/0\",\"networkEntityId\":\"$NAT_ID\"}]" --query "data.id" --raw-output)
echo "✅ Public route table Created: $PUB_RT_ID"
echo Creating subnet as oci network subnet create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "IoT_Subnet" --cidr-block "$SUBNET_CIDR" --route-table-id "$PUB_RT_ID" --security-list-ids "[\"$SL_ID\"]" --query "data.id" --raw-output
PUB_SUBNET_ID=$(oci network subnet create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "IoT_Subnet" --cidr-block "$SUBNET_CIDR" --route-table-id "$PUB_RT_ID" --security-list-ids "[\"$SL_ID\"]" --query "data.id" --raw-output)
echo "✅ Subnet Created: $PUB_SUBNET_ID"
# --- 4. Compute Instance ---
AD_NAME=$(oci iam availability-domain list --compartment-id "$COMPARTMENT_ID" --query "data[0].name" --raw-output)
echo "Located ad name"
IMAGE_ID=$(oci compute image list --compartment-id "$COMPARTMENT_ID" --operating-system "Oracle Linux" --operating-system-version "8" --shape "VM.Standard.E5.Flex" --query "data[0].id" --raw-output)
echo "Located image id"
echo "🛰️  Launching VM.Standard.E5.Flex (1 OCPU, 8GB RAM)..."

INSTANCE_ID=$(oci compute instance launch \
    --compartment-id "$COMPARTMENT_ID" \
    --availability-domain "$AD_NAME" \
    --display-name "$VM_NAME" \
    --image-id "$IMAGE_ID" \
    --shape "VM.Standard.E5.Flex" \
    --shape-config "{\"ocpus\":1,\"memoryInGBs\":8}" \
    --subnet-id "$PUB_SUBNET_ID" \
    --assign-public-ip true \
    --ssh-authorized-keys-file "$SSH_PUBLIC_KEY_PATH" \
    --query "data.id" --raw-output)

# --- 3. Final Output ---
echo "⏳ Waiting for Public IP..."
sleep 20
PUBLIC_IP=$(oci compute instance list-vnics --instance-id "$INSTANCE_ID" --query "data[0].\"public-ip\"" --raw-output)

echo "------------------------------------------------"
echo "✅ Infrastructure Deployed"
echo "Public IP: $PUBLIC_IP"
echo "Shape: VM.Standard.E5.Flex (1 OCPU / 1GB RAM)"
echo "------------------------------------------------"
echo "✅ Deployment Complete. The VM can now initiate connections to any DB on ports 1521-1522 within $VCN_CIDR."