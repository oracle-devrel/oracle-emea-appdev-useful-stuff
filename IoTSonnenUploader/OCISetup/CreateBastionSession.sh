#!/bin/bash
# this script is based on AI generated code, but with additions to get the OCIDs within the script from setting in common_names rather than them being set as constants
# the bastion and target instance need to have been setup and the target running
source ../OCISetup/common_names.sh
echo "Using OCI config profile $OCI_CLI_PROFILE"
# the get_oci_compartment_ocid.sh script is in the OCI setup folder, but I use it so much it's also in my bin directory
IOT_COMPARTMENT_OCID=`get_oci_compartment_ocid.sh $COMPARTMENT_PATH`


BASTION_OCID=`oci bastion bastion list --compartment-id "$IOT_COMPARTMENT_OCID" --name "$BASTION_NAME" --all --query "data[0].id" --raw-output`


echo "Bastion $BASTION_NAME in compartment $IOT_COMPARTMENT_OCID has $BASTION_OCID ocid"
# Retrieve Target Instance OCID by Name
VM_OCID=`oci compute instance list --compartment-id "$IOT_COMPARTMENT_OCID" --display-name "$VM_NAME" --query "data[0].id" --raw-output`

echo "Compute instance $VM_NAME in compartment $IOT_COMPARTMENT_OCID has $VM_OCID ocid"

if [ -z "$BASTION_OCID" ] || [ -z "$VM_OCID" ]; then
    echo "Error: Could not find Bastion or Target Instance."
    exit 1
fi

echo "Checking lifecycle state for target instance..."
MAX_ATTEMPTS=30 # 30 attempts * 10 seconds = 5 minutes
ATTEMPT=1
IS_RUNNING=false

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    # Retrieve current lifecycle state
    CURRENT_STATE=`oci compute instance get --instance-id "$VM_OCID" --query "data.\"lifecycle-state\"" --raw-output`
    
    if [ "$CURRENT_STATE" == "RUNNING" ]; then
        echo "Instance is RUNNING."
        IS_RUNNING=true
        break
    elif [ "$CURRENT_STATE" == "STOPPED" ]; then
        echo "Instance is STOPPED. Initiating START action..."
        oci compute instance action --instance-id "$VM_OCID" --action START --wait-for-state STARTING                                                                                                                                    
        
        # Instance will transition to STARTING then RUNNING; loop continues to monitor
    else
        echo "Check $ATTEMPT of $MAX_ATTEMPTS: Instance state is '$CURRENT_STATE'. Retrying in 10s..."
    fi
    
    sleep 10
    ((ATTEMPT++))
done
if [ $ATTEMPT = 1 ] ; then
    echo "VM Was already running"
else
    if [ "$IS_RUNNING" = true ]; then
        echo "VM reached RUNNING state after $ATTEMPT checks."
    else
        echo "Error: Instance failed to reach RUNNING state within 5 minutes."
        exit 1
    fi
fi

BASTIAN_CHECK_MAX_ITERATIONS=60
BASTIAN_SLEEP_SECONDS=10

echo "Checking Bastion plugin status for instance: $VM_OCID"

i=1
BASTION_AGENT_STATUS=""

while (( i <= BASTIAN_CHECK_MAX_ITERATIONS )); do
  BASTION_AGENT_STATUS=`oci instance-agent plugin list  --compartment-id "$IOT_COMPARTMENT_OCID" --instanceagent-id "$VM_OCID" | jq -r '.data[] | select (.name == "Bastion") | .status'`

  echo "Attempt $i/$BASTIAN_CHECK_MAX_ITERATIONS - Status: ${BASTION_AGENT_STATUS:-NOT_FOUND}"

  case "$BASTION_AGENT_STATUS" in
    RUNNING)
      echo "Bastion plugin is RUNNING."
      break
      ;;
    FAILED)
	#STOPPED|FAILED|INVALID)
      echo "Bastion plugin is in a failure state: $BASTION_AGENT_STATUS" >&2
      exit 1
      ;;
    *)
      echo "Bastion plugin not ready yet, continuing to wait..."
      ;;
  esac

  ((i++))

  if (( i <= BASTIAN_CHECK_MAX_ITERATIONS )); then
    sleep "$BASTIAN_SLEEP_SECONDS"
  fi
done

# Post-loop evaluation
if [[ "$BASTION_AGENT_STATUS" != "RUNNING" ]]; then
  echo "Bastion plugin did not reach RUNNING state after $BASTIAN_CHECK_MAX_ITERATIONS attempts." 
  exit 1
fi

echo "Bastion agent is running"

echo "Creating managed SSH session for $VM_USER_NAME on target instance... using public key $BASTION_PUB_KEY_PATH"
  
DISPLAY_NAME="ManagedSession_$(date +%F)"
BASTION_SESSION_TTL=10800
echo Creating bastion managed ssh session
# Create the managed SSH session removed --display-name "$DISPLAY_NAME" and   --wait-for-state SUCCEEDED
SESSION_JSON=`oci bastion session create-managed-ssh  --bastion-id "$BASTION_OCID"     --target-resource-id "$VM_OCID"    --target-os-username "$VM_USER_NAME"  --ssh-public-key-file "$BASTION_PUB_KEY_PATH"  --session-ttl $BASTION_SESSION_TTL ` # Waits until session is ready to use

# Extract the session OCID
SESSION_ID=`echo "$SESSION_JSON" | jq -r '.data.id'`
echo "Provisioning Bastion Session: $SESSION_ID"
SESS_ATTEMPT=1
IS_ACTIVE=false

while [ $SESS_ATTEMPT -le 30 ]; do
    SESS_STATE=`oci bastion session get --session-id "$SESSION_ID" --query "data.\"lifecycle-state\"" --raw-output`
    
    if [ "$SESS_STATE" == "ACTIVE" ]; then
        IS_ACTIVE=true
        break
    elif [ "$SESS_STATE" == "FAILED" ]; then
        echo "Session creation FAILED. Check if Bastion plugin is running on the VM."
        exit 1
    fi
    
    echo "Waiting for Session... ($SESS_STATE) Attempt $SESS_ATTEMPT/30"
    sleep 10
    ((SESS_ATTEMPT++))
done

# Output Connection String
if [ "$IS_ACTIVE" = true ]; then
    RAW_SSH_CMD=`oci bastion session get --session-id "$SESSION_ID" --query 'data."ssh-metadata".command' --raw-output`
    FINAL_CMD=`echo "$RAW_SSH_CMD" | sed "s|<privateKey>|$BASTION_PRIV_KEY_PATH|g"`
    echo "-------------------------------------------------------"
    echo "SUCCESS: Session is Active."
    echo "Connect with: "
    echo "$FINAL_CMD"
    echo "-------------------------------------------------------"
else
    echo "Timeout: Session did not become active within 5 minutes."
    exit 1
fi
