#!/bin/bash
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"

# --- Configuration ---
# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
COMPARTMENT_ID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`
echo "✅ Located compartment id: $COMPARTMENT_ID"

find_active_id_by_display_name() {
  local display_name="$1"
  jq -r --arg display_name "$display_name" 'first(.data[] | select(."display-name" == $display_name) | select((."lifecycle-state" // "") != "TERMINATED") | .id) // empty'
}

find_available_id_by_display_name() {
  local display_name="$1"
  jq -r --arg display_name "$display_name" 'first(.data[] | select(."display-name" == $display_name) | select((."lifecycle-state" // "") == "AVAILABLE") | .id) // empty'
}

find_active_bastion_id_by_name() {
  local bastion_name="$1"
  jq -r --arg bastion_name "$bastion_name" 'first(.data[] | select(.name == $bastion_name) | select((."lifecycle-state" // "") == "ACTIVE") | .id) // empty'
}

IOT_APPS_SUBNET_CIDR=${IOT_APPS_SUBNET_CIDR:-$SUBNET_CIDR}
IOT_CLIENT_VM_SUBNET_CIDR=${IOT_CLIENT_VM_SUBNET_CIDR:-10.0.2.0/24}
BASTION_CLIENT_CIDR_LIST=${BASTION_CLIENT_CIDR_LIST:-'["0.0.0.0/0"]'}
DB_CLIENT_VM_CLOUD_INIT_FILE=${DB_CLIENT_VM_CLOUD_INIT_FILE:-../OCISetup/DBClientVMCloudInit.sh}
INSTANCE_OPTIONS=$(jq -cn '{areLegacyImdsEndpointsDisabled: true}')
SECURITY_LIST_EGRESS_RULES=$(jq -cn '[
  {
    "destination": "0.0.0.0/0",
    "protocol": "all",
    "description": "Allow all outbound traffic"
  }
]')
SECURITY_LIST_INGRESS_RULES=$(jq -cn --arg vcn_cidr "$VCN_CIDR" '[
  {
    "source": $vcn_cidr,
    "protocol": "6",
    "tcpOptions": {
      "destinationPortRange": {
        "min": 1521,
        "max": 1522
      }
    },
    "description": "Allow Oracle Database access from VMs in the VCN"
  },
  {
    "source": $vcn_cidr,
    "protocol": "6",
    "tcpOptions": {
      "destinationPortRange": {
        "min": 22,
        "max": 22
      }
    },
    "description": "Allow SSH access from the VCN"
  },
  {
    "source": $vcn_cidr,
    "protocol": "6",
    "tcpOptions": {
      "destinationPortRange": {
        "min": 80,
        "max": 80
      }
    },
    "description": "Allow HTTP access from the VCN"
  },
  {
    "source": $vcn_cidr,
    "protocol": "6",
    "tcpOptions": {
      "destinationPortRange": {
        "min": 443,
        "max": 443
      }
    },
    "description": "Allow HTTPS access from the VCN"
  }
]')

# --- 1. Network Infrastructure ---
SERVICE_ID=$(oci network service list --query "data[?contains(name, 'All ')].id | [0]" --raw-output)
echo "✅ Located all services id: $SERVICE_ID"

VCN_ID=$(oci network vcn list --compartment-id "$COMPARTMENT_ID" --all | find_available_id_by_display_name "$VCN_NAME")
if [[ -n "$VCN_ID" ]]
then
  echo "✅ Reusing existing VCN: $VCN_ID"
else
  VCN_ID=$(oci network vcn create --compartment-id "$COMPARTMENT_ID" --display-name "$VCN_NAME" --cidr-block "$VCN_CIDR" --query "data.id" --raw-output)
  echo "✅ VCN Created: $VCN_ID"
fi

IG_ID=$(oci network internet-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_available_id_by_display_name "IGW")
if [[ -n "$IG_ID" ]]
then
  echo "✅ Reusing existing Internet Gateway: $IG_ID"
else
  IG_ID=$(oci network internet-gateway create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "IGW" --is-enabled true --query "data.id" --raw-output)
  echo "✅ Internet Gateway Created: $IG_ID"
fi

NAT_ID=$(oci network nat-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_available_id_by_display_name "NAT_Gateway")
if [[ -n "$NAT_ID" ]]
then
  echo "✅ Reusing existing NAT Gateway: $NAT_ID"
else
  NAT_ID=$(oci network nat-gateway create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "NAT_Gateway" --query "data.id" --raw-output)
  echo "✅ NAT Gateway Created: $NAT_ID"
fi

SG_ID=$(oci network service-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_available_id_by_display_name "SG")
if [[ -n "$SG_ID" ]]
then
  echo "✅ Reusing existing Service Gateway: $SG_ID"
else
  SG_ID=$(oci network service-gateway create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "SG" --services "[{\"serviceId\":\"$SERVICE_ID\"}]" --query "data.id" --raw-output)
  echo "✅ Service Gateway Created: $SG_ID"
fi

# --- 2. Security List ---
SL_ID=$(oci network security-list list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_available_id_by_display_name "VCN_Wide_DB_Security")
if [[ -n "$SL_ID" ]]
then
  echo "✅ Reusing existing Security List: $SL_ID"
else
  SL_ID=$(oci network security-list create \
      --compartment-id "$COMPARTMENT_ID" \
      --vcn-id "$VCN_ID" \
      --display-name "VCN_Wide_DB_Security" \
      --egress-security-rules "$SECURITY_LIST_EGRESS_RULES" \
      --ingress-security-rules "$SECURITY_LIST_INGRESS_RULES" \
      --query "data.id" --raw-output)
  echo "✅ Security List Created: $SL_ID"
fi

oci network security-list update \
    --security-list-id "$SL_ID" \
    --egress-security-rules "$SECURITY_LIST_EGRESS_RULES" \
    --ingress-security-rules "$SECURITY_LIST_INGRESS_RULES" \
    --force >/dev/null
echo "✅ Security List rules configured for DB, SSH, HTTP, and HTTPS access from $VCN_CIDR, and all outbound traffic"

# --- 3. Routing and Private Subnets ---
PUB_RT_ID=$(oci network route-table list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_available_id_by_display_name "Public_RT")
if [[ -n "$PUB_RT_ID" ]]
then
  echo "✅ Reusing existing Public route table: $PUB_RT_ID"
else
  echo creating route table as oci network route-table create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "Public_RT" --route-rules "[{\"cidrBlock\":\"0.0.0.0/0\",\"networkEntityId\":\"$NAT_ID\"}]" --query "data.id" --raw-output
  PUB_RT_ID=$(oci network route-table create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "Public_RT" --route-rules "[{\"cidrBlock\":\"0.0.0.0/0\",\"networkEntityId\":\"$NAT_ID\"}]" --query "data.id" --raw-output)
  echo "✅ Public route table Created: $PUB_RT_ID"
fi

IOT_APPS_SUBNET_ID=$(oci network subnet list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_available_id_by_display_name "$IOT_APPS_SUBNET")
if [[ -n "$IOT_APPS_SUBNET_ID" ]]
then
  echo "✅ Reusing existing apps subnet: $IOT_APPS_SUBNET_ID"
else
  echo Creating apps subnet as oci network subnet create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "$IOT_APPS_SUBNET" --cidr-block "$IOT_APPS_SUBNET_CIDR" --route-table-id "$PUB_RT_ID" --security-list-ids "[\"$SL_ID\"]" --prohibit-public-ip-on-vnic true --query "data.id" --raw-output
  IOT_APPS_SUBNET_ID=$(oci network subnet create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "$IOT_APPS_SUBNET" --cidr-block "$IOT_APPS_SUBNET_CIDR" --route-table-id "$PUB_RT_ID" --security-list-ids "[\"$SL_ID\"]" --prohibit-public-ip-on-vnic true --query "data.id" --raw-output)
  echo "✅ Apps subnet Created: $IOT_APPS_SUBNET_ID"
fi

IOT_CLIENT_VM_SUBNET_ID=$(oci network subnet list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_available_id_by_display_name "$IOT_CLIENT_VM_SUBNET")
if [[ -n "$IOT_CLIENT_VM_SUBNET_ID" ]]
then
  echo "✅ Reusing existing client VM subnet: $IOT_CLIENT_VM_SUBNET_ID"
else
  echo Creating client VM subnet as oci network subnet create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "$IOT_CLIENT_VM_SUBNET" --cidr-block "$IOT_CLIENT_VM_SUBNET_CIDR" --route-table-id "$PUB_RT_ID" --security-list-ids "[\"$SL_ID\"]" --prohibit-public-ip-on-vnic true --query "data.id" --raw-output
  IOT_CLIENT_VM_SUBNET_ID=$(oci network subnet create --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --display-name "$IOT_CLIENT_VM_SUBNET" --cidr-block "$IOT_CLIENT_VM_SUBNET_CIDR" --route-table-id "$PUB_RT_ID" --security-list-ids "[\"$SL_ID\"]" --prohibit-public-ip-on-vnic true --query "data.id" --raw-output)
  echo "✅ Client VM subnet Created: $IOT_CLIENT_VM_SUBNET_ID"
fi

# --- 4. Bastion ---
BASTION_ID=$(oci bastion bastion list --compartment-id "$COMPARTMENT_ID" --name "$BASTION_NAME" --all | find_active_bastion_id_by_name "$BASTION_NAME")
if [[ -n "$BASTION_ID" ]]
then
  echo "✅ Reusing existing Bastion: $BASTION_ID"
  oci bastion bastion update \
      --bastion-id "$BASTION_ID" \
      --client-cidr-list "$BASTION_CLIENT_CIDR_LIST" \
      --force \
      --wait-for-state SUCCEEDED >/dev/null
  echo "✅ Bastion client CIDR allow list configured: $BASTION_CLIENT_CIDR_LIST"
else
  echo "Creating Bastion $BASTION_NAME for client VM subnet $IOT_CLIENT_VM_SUBNET_ID"
  oci bastion bastion create \
      --compartment-id "$COMPARTMENT_ID" \
      --name "$BASTION_NAME" \
      --bastion-type standard \
      --target-subnet-id "$IOT_CLIENT_VM_SUBNET_ID" \
      --client-cidr-list "$BASTION_CLIENT_CIDR_LIST" \
      --wait-for-state SUCCEEDED >/dev/null
  BASTION_ID=$(oci bastion bastion list --compartment-id "$COMPARTMENT_ID" --name "$BASTION_NAME" --all | find_active_bastion_id_by_name "$BASTION_NAME")
  echo "✅ Bastion Created: $BASTION_ID"
fi

# --- 5. Compute Instance ---
INSTANCE_ID=$(oci compute instance list --compartment-id "$COMPARTMENT_ID" --display-name "$VM_NAME" --all | find_active_id_by_display_name "$VM_NAME")
if [[ -n "$INSTANCE_ID" ]]
then
  echo "✅ Reusing existing compute instance: $INSTANCE_ID"
  oci compute instance update \
      --instance-id "$INSTANCE_ID" \
      --instance-options "$INSTANCE_OPTIONS" \
      --force >/dev/null
  echo "✅ Disabled legacy instance metadata endpoints"
  echo "ℹ️ Existing compute instance reused; cloud-init runs only when a new VM is created"
else
  if [[ ! -f "$DB_CLIENT_VM_CLOUD_INIT_FILE" ]]
  then
    echo "❌ Cloud-init script not found: $DB_CLIENT_VM_CLOUD_INIT_FILE"
    exit 1
  fi

  AD_NAME=$(oci iam availability-domain list --compartment-id "$COMPARTMENT_ID" --query "data[0].name" --raw-output)
  echo "Located ad name"
  IMAGE_ID=$(oci compute image list \
      --compartment-id "$COMPARTMENT_ID" \
      --operating-system "Oracle Linux" \
      --operating-system-version "9" \
      --shape "VM.Standard.E5.Flex" \
      --sort-by TIMECREATED \
      --sort-order DESC \
      --query "data[0].id" --raw-output)
  echo "Located image id"
  echo "🛰️  Launching VM.Standard.E5.Flex with Oracle Linux 9 (1 OCPU, 8GB RAM)..."

  INSTANCE_ID=$(oci compute instance launch \
      --compartment-id "$COMPARTMENT_ID" \
      --availability-domain "$AD_NAME" \
      --display-name "$VM_NAME" \
      --image-id "$IMAGE_ID" \
      --shape "VM.Standard.E5.Flex" \
      --shape-config "{\"ocpus\":1,\"memoryInGBs\":8}" \
      --subnet-id "$IOT_CLIENT_VM_SUBNET_ID" \
      --assign-public-ip false \
      --instance-options "$INSTANCE_OPTIONS" \
      --user-data-file "$DB_CLIENT_VM_CLOUD_INIT_FILE" \
      --ssh-authorized-keys-file "$SSH_PUBLIC_KEY_PATH" \
      --query "data.id" --raw-output)

  echo "⏳ Waiting for VNIC..."
  sleep 20
fi

# --- 6. Final Output ---
VNIC_JSON=$(oci compute instance list-vnics --instance-id "$INSTANCE_ID")
PRIVATE_IP=$(echo "$VNIC_JSON" | jq -r '(first(.data[]) | ."private-ip") // empty')
PUBLIC_IP=$(echo "$VNIC_JSON" | jq -r '(first(.data[]) | ."public-ip") // empty')
if [[ -z "$PUBLIC_IP" || "$PUBLIC_IP" == "null" ]]
then
  PUBLIC_IP="none (private subnet)"
fi

echo "------------------------------------------------"
echo "✅ Infrastructure Ready"
echo "Apps Subnet: $IOT_APPS_SUBNET_ID"
echo "Client VM Subnet: $IOT_CLIENT_VM_SUBNET_ID"
echo "Bastion: $BASTION_ID"
echo "Cloud Init Script: $DB_CLIENT_VM_CLOUD_INIT_FILE"
echo "Private IP: $PRIVATE_IP"
echo "Public IP: $PUBLIC_IP"
echo "Shape: VM.Standard.E5.Flex (1 OCPU / 8GB RAM)"
echo "------------------------------------------------"
echo "✅ Deployment Complete. The VM can now initiate connections to any DB on ports 1521-1522 within $VCN_CIDR."
