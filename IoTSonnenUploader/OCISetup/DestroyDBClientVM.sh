#!/bin/bash

# Removes only the DB client VM and its boot volume. The VCN, subnets, route
# table, security list, bastion, and gateways are intentionally left in place.
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"

# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
COMPARTMENT_ID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`
echo "✅ Located compartment id: $COMPARTMENT_ID"

find_active_ids_by_display_name() {
  local display_name="$1"
  jq -r --arg display_name "$display_name" '.data[] | select(."display-name" == $display_name) | select((."lifecycle-state" // "") != "TERMINATED") | .id'
}

wait_for_instance_terminated() {
  local instance_id="$1"
  local attempts=60
  local state

  while (( attempts > 0 ))
  do
    if ! state=$(oci compute instance get --instance-id "$instance_id" --query 'data."lifecycle-state"' --raw-output 2>/dev/null)
    then
      echo "✅ Instance no longer returned by OCI: $instance_id"
      return 0
    fi

    if [[ "$state" == "TERMINATED" ]]
    then
      echo "✅ Instance terminated: $instance_id"
      return 0
    fi

    echo "⏳ Waiting for instance $instance_id to terminate (state: $state)..."
    sleep 10
    attempts=$((attempts - 1))
  done

  echo "⚠️ Timed out waiting for instance $instance_id to terminate"
  return 1
}

delete_boot_volume_if_present() {
  local boot_volume_id="$1"
  local state

  if ! state=$(oci bv boot-volume get --boot-volume-id "$boot_volume_id" --query 'data."lifecycle-state"' --raw-output 2>/dev/null)
  then
    echo "✅ Boot volume already removed: $boot_volume_id"
    return 0
  fi

  if [[ "$state" == "TERMINATED" ]]
  then
    echo "✅ Boot volume already terminated: $boot_volume_id"
    return 0
  fi

  echo "🗑️ Deleting remaining boot volume: $boot_volume_id"
  oci bv boot-volume delete --boot-volume-id "$boot_volume_id" --force --wait-for-state TERMINATED
}

report_network_security_groups() {
  local instance_id="$1"
  local nsg_ids

  nsg_ids=$(oci compute instance list-vnics --instance-id "$instance_id" --all | jq -r '.data[] | (."nsg-ids" // [])[]' | sort -u)
  if [[ -z "$nsg_ids" ]]
  then
    echo "ℹ️ No Network Security Group attachments found on VM VNICs."
    return 0
  fi

  echo "ℹ️ VM VNICs are attached to these Network Security Groups:"
  echo "$nsg_ids"
  echo "ℹ️ No NSG rule changes are required; OCI removes VNIC membership from NSGs when the VM VNICs are deleted."
}

echo "🔍 Searching for VM instances named $VM_NAME..."

INSTANCE_IDS=$(oci compute instance list --compartment-id "$COMPARTMENT_ID" --display-name "$VM_NAME" --all | find_active_ids_by_display_name "$VM_NAME")
if [[ -z "$INSTANCE_IDS" ]]
then
  echo "ℹ️ No active instance found with name $VM_NAME."
  exit 0
fi

while read -r INSTANCE_ID
do
  if [[ -z "$INSTANCE_ID" ]]
  then
    continue
  fi

  echo "📍 Found instance: $INSTANCE_ID"
  report_network_security_groups "$INSTANCE_ID"

  BOOT_VOLUME_IDS=$(oci compute boot-volume-attachment list --compartment-id "$COMPARTMENT_ID" --instance-id "$INSTANCE_ID" --all | jq -r '.data[] | select((."lifecycle-state" // "") != "DETACHED") | ."boot-volume-id"')

  echo "🗑️ Terminating instance and deleting boot volume: $INSTANCE_ID"
  oci compute instance terminate --instance-id "$INSTANCE_ID" --preserve-boot-volume false --force --wait-for-state SUCCEEDED
  wait_for_instance_terminated "$INSTANCE_ID"

  while read -r BOOT_VOLUME_ID
  do
    if [[ -n "$BOOT_VOLUME_ID" ]]
    then
      delete_boot_volume_if_present "$BOOT_VOLUME_ID"
    fi
  done <<< "$BOOT_VOLUME_IDS"
done <<< "$INSTANCE_IDS"

echo "✨ VM cleanup complete. Network resources were left in place for SetupDBClientVCNandVM.sh to reuse."
