#!/bin/bash

# run this in the DigitalTwin directory
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"

# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
COMPARTMENT_ID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`
echo "✅ Located compartment id: $COMPARTMENT_ID"

find_active_ids_by_display_name() {
  local display_name="$1"
  jq -r --arg display_name "$display_name" '.data[] | select(."display-name" == $display_name) | select((."lifecycle-state" // "") != "TERMINATED") | .id'
}

find_available_id_by_display_name() {
  local display_name="$1"
  jq -r --arg display_name "$display_name" 'first(.data[] | select(."display-name" == $display_name) | select((."lifecycle-state" // "") == "AVAILABLE") | .id) // empty'
}

find_bastion_ids_by_name() {
  local bastion_name="$1"
  jq -r --arg bastion_name "$bastion_name" '.data[] | select(.name == $bastion_name) | select((."lifecycle-state" // "") != "DELETED") | .id'
}

wait_until_deleted() {
  local resource_description="$1"
  shift

  local attempts=30
  local state

  while (( attempts > 0 ))
  do
    if ! state=$("$@" --query 'data."lifecycle-state"' --raw-output 2>/dev/null)
    then
      echo "✅ $resource_description deleted"
      return 0
    fi

    if [[ "$state" == "TERMINATED" || "$state" == "DELETED" ]]
    then
      echo "✅ $resource_description deleted"
      return 0
    fi

    echo "⏳ Waiting for $resource_description to delete (state: $state)..."
    sleep 10
    attempts=$((attempts - 1))
  done

  echo "⚠️ Timed out waiting for $resource_description to delete"
  return 1
}

delete_bastion_by_name() {
  local bastion_name="$1"
  local bastion_id

  oci bastion bastion list --compartment-id "$COMPARTMENT_ID" --name "$bastion_name" --all | find_bastion_ids_by_name "$bastion_name" | while read -r bastion_id
  do
    if [[ -n "$bastion_id" ]]
    then
      echo "🗑️ Deleting Bastion $bastion_name: $bastion_id"
      oci bastion bastion delete --bastion-id "$bastion_id" --force --wait-for-state SUCCEEDED
      wait_until_deleted "Bastion $bastion_name" oci bastion bastion get --bastion-id "$bastion_id"
    fi
  done
}

delete_subnet_by_name() {
  local subnet_name="$1"
  local subnet_id

  oci network subnet list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_active_ids_by_display_name "$subnet_name" | while read -r subnet_id
  do
    if [[ -n "$subnet_id" ]]
    then
      echo "🗑️ Deleting Subnet $subnet_name: $subnet_id"
      oci network subnet delete --subnet-id "$subnet_id" --force
      wait_until_deleted "Subnet $subnet_name" oci network subnet get --subnet-id "$subnet_id"
    fi
  done
}

delete_route_table_by_name() {
  local route_table_name="$1"
  local route_table_id

  oci network route-table list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_active_ids_by_display_name "$route_table_name" | while read -r route_table_id
  do
    if [[ -n "$route_table_id" ]]
    then
      echo "🗑️ Deleting Route Table $route_table_name: $route_table_id"
      oci network route-table delete --rt-id "$route_table_id" --force
      wait_until_deleted "Route Table $route_table_name" oci network route-table get --rt-id "$route_table_id"
    fi
  done
}

delete_security_list_by_name() {
  local security_list_name="$1"
  local security_list_id

  oci network security-list list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_active_ids_by_display_name "$security_list_name" | while read -r security_list_id
  do
    if [[ -n "$security_list_id" ]]
    then
      echo "🗑️ Deleting Security List $security_list_name: $security_list_id"
      oci network security-list delete --security-list-id "$security_list_id" --force
      wait_until_deleted "Security List $security_list_name" oci network security-list get --security-list-id "$security_list_id"
    fi
  done
}

delete_service_gateway_by_name() {
  local service_gateway_name="$1"
  local service_gateway_id

  oci network service-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_active_ids_by_display_name "$service_gateway_name" | while read -r service_gateway_id
  do
    if [[ -n "$service_gateway_id" ]]
    then
      echo "🗑️ Deleting Service Gateway $service_gateway_name: $service_gateway_id"
      oci network service-gateway delete --service-gateway-id "$service_gateway_id" --force
      wait_until_deleted "Service Gateway $service_gateway_name" oci network service-gateway get --service-gateway-id "$service_gateway_id"
    fi
  done
}

delete_internet_gateway_by_name() {
  local internet_gateway_name="$1"
  local internet_gateway_id

  oci network internet-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_active_ids_by_display_name "$internet_gateway_name" | while read -r internet_gateway_id
  do
    if [[ -n "$internet_gateway_id" ]]
    then
      echo "🗑️ Deleting Internet Gateway $internet_gateway_name: $internet_gateway_id"
      oci network internet-gateway delete --ig-id "$internet_gateway_id" --force
      wait_until_deleted "Internet Gateway $internet_gateway_name" oci network internet-gateway get --ig-id "$internet_gateway_id"
    fi
  done
}

delete_nat_gateway_by_name() {
  local nat_gateway_name="$1"
  local nat_gateway_id

  oci network nat-gateway list --compartment-id "$COMPARTMENT_ID" --vcn-id "$VCN_ID" --all | find_active_ids_by_display_name "$nat_gateway_name" | while read -r nat_gateway_id
  do
    if [[ -n "$nat_gateway_id" ]]
    then
      echo "🗑️ Deleting NAT Gateway $nat_gateway_name: $nat_gateway_id"
      oci network nat-gateway delete --nat-gateway-id "$nat_gateway_id" --force
      wait_until_deleted "NAT Gateway $nat_gateway_name" oci network nat-gateway get --nat-gateway-id "$nat_gateway_id"
    fi
  done
}

echo "🔍 Searching for resources associated with $VCN_NAME..."

# --- 1. Find the Instance and Terminate it ---
oci compute instance list --compartment-id "$COMPARTMENT_ID" --display-name "$VM_NAME" --all | find_active_ids_by_display_name "$VM_NAME" | while read -r instance_id
do
  if [[ -n "$instance_id" ]]
  then
    echo "🗑️ Terminating Instance and waiting for completion: $instance_id"
    oci compute instance terminate --instance-id "$instance_id" --preserve-boot-volume false --force --wait-for-state TERMINATED
  fi
done

# --- 2. Find the VCN ID to locate child resources ---
VCN_ID=$(oci network vcn list --compartment-id "$COMPARTMENT_ID" --all | find_available_id_by_display_name "$VCN_NAME")

if [[ -z "$VCN_ID" ]]
then
  echo "ℹ️ VCN $VCN_NAME not found. Nothing to delete."
  exit 0
fi

echo "📍 Found VCN: $VCN_ID. Cleaning up child resources..."

# --- 3. Delete the bastion before the target subnet ---
delete_bastion_by_name "$BASTION_NAME"

# --- 4. Delete Subnets before their route table and security list dependencies ---
delete_subnet_by_name "$IOT_CLIENT_VM_SUBNET"
delete_subnet_by_name "$IOT_APPS_SUBNET"
delete_subnet_by_name "IoT_Subnet"

# --- 5. Delete the route table before gateways referenced by its route rules ---
delete_route_table_by_name "Public_RT"

# --- 6. Delete the security list after subnets are gone ---
delete_security_list_by_name "VCN_Wide_DB_Security"

# --- 7. Delete gateways ---
delete_service_gateway_by_name "SG"
delete_internet_gateway_by_name "IGW"
delete_nat_gateway_by_name "NAT_Gateway"

# --- 8. Finally, Delete the VCN ---
echo "🗑️ Deleting VCN: $VCN_ID"
oci network vcn delete --vcn-id "$VCN_ID" --force
wait_until_deleted "VCN $VCN_NAME" oci network vcn get --vcn-id "$VCN_ID"

echo "✨ Cleanup Complete!"
