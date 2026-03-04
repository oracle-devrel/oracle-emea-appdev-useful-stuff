
# change these names as required
source ../OCISetup/OCIDs.sh
export IOT_DOMAIN_GROUP_NAME=iot-domain-group-timg
export IOT_DOMAIN_NAME=iot-domain-timg
export GENERATED_SECRET_PREFIX_NAME='iot-gateway-generated-secret*'

if [[ -z "$IOT_COMPARTMENT_OCID" ]]
then
  echo "IOT_COMPARTMENT_OCID is not set, this should contain the compartment ocid that contains the IOT domain group and other resources"
fi

echo Getting OCIDs
export IOT_DOMAIN_GROUP_OCID=`oci iot domain-group list --display-name $IOT_DOMAIN_GROUP_NAME --compartment-id $IOT_COMPARTMENT_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
if [[ -n  "$IOT_DOMAIN_GROUP_OCID" ]]
then
  echo "Got iotdomaingroup ocid, looking for domain"
  export IOT_DOMAIN_OCID=`oci iot domain list --display-name $IOT_DOMAIN_NAME --compartment-id $IOT_COMPARTMENT_OCID --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID | jq -r '.data.items[]| select (."lifecycle-state" == "ACTIVE") | ."id"'`
else 
  echo "Can't locate IOT domain group so can't locate iot domain"
fi

if [[ -n "$IOT_DOMAIN_OCID" ]]
then
  echo "Deleting IotDomain $IOT_DOMAIN_NAME"
  oci iot domain delete --iot-domain-id  $IOT_DOMAIN_OCID --force --wait-for-state SUCCEEDED --wait-for-state FAILED
  echo "Deleted IotDomain $IOT_DOMAIN_NAME"
else
  echo "No iot domain to delete"
fi

if [[ -n "$IOT_DOMAIN_GROUP_OCID" ]]
then
  echo "Deleting IotDomainGroup $IOT_DOMAIN_GROUP_NAME"
  oci iot domain-group delete --iot-domain-group-id $IOT_DOMAIN_GROUP_OCID  --force --wait-for-state SUCCEEDED --wait-for-state FAILED
  echo "Deleted IotDomainGroup $IOT_DOMAIN_GROUP_NAME"
else
  echo "No iot domain group to delete"
fi



# get the list of secrets in the vault
echo "Looking for any generated secrets to delete"
for secretname in `oci vault secret list --compartment-id $IOT_COMPARTMENT_OCID --all --lifecycle-state ACTIVE | jq -r '.data[] | ."secret-name"'` ; do
  if [[ $secretname = $GENERATED_SECRET_PREFIX_NAME ]]
  then
  	echo "$secretname is a generated secret, getting ocid"
  	SECRET_OCID=`oci vault secret list --compartment-id $IOT_COMPARTMENT_OCID --name $secretname --lifecycle-state ACTIVE --all | jq -r '.data[0].id'`
    SECRET_DELETE_TIME=`date -j -v+1d -v+5M +"%Y-%m-%dT%H:%M:%S%z"`
    echo "Scheduling deletion of $secretname with ocid $SECRET_OCID for $SECRET_DELETE_TIME"
    oci vault secret schedule-secret-deletion --secret-id $SECRET_OCID --time-of-deletion $SECRET_DELETE_TIME
  else
    echo "no match, ignoring $secretname"
  fi
done

