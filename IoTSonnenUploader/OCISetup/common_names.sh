# core stuff
if [[ -z "$OCI_CLI_PROFILE" ]]
then
  export OCI_CLI_PROFILE=DEFAULT
fi
export COMPARTMENT_PATH=/domain-specialists/tim.graves/iot
#Identity stuff
# The IDCS instance holding the DG must exist and you mush have rights to create / modify applications in it
export IDCS_DOMAIN_NAME=iotdomain
export IDCS_DOMAIN_COMPARTMENT_PATH=/domain-specialists/tim.graves/iot
# use home for the home region (the domain must be replicated to the region the IOT Domain group / iot domain is in)
# otherwise the of the region the IDCS Domain is hosted in (it can be replicated to the current region or exist in the current region)
# e.g. FRA or LHR
export IDCS_DOMAIN_REGION=home
# this will "hold" the application access
export IDCS_ORDS_DB_ACCESS_APPLICATION_NAME=IOTORDSApp
#The user to add to the app 
export IDCS_ORDS_APP_USER='tim.graves@oracle.com'
# this is the users group within the IDCS
export IDCS_ORDS_APP_USERS_GROUP=IOTORDSUsers
# The Dynamic group for the compute instances in the IDCS instance
export IDCS_DYNAMIC_GROUP_NAME=TGIoTDBAccessDG

# change these names as required
export IOT_DOMAIN_GROUP_NAME=iot-domain-group-timg
export IOT_DOMAIN_NAME=iot-domain-timg
export VAULT_NAME=iotvault

#used for handling digital twin instance secrets
export FIXED_SECRET_NAME='iot-instance-secret'
export GENERATED_SECRET_PREFIX_NAME='iot-gateway-generated-secret*'

# The name of the vault secret that holds the password you want to use for APEX
export APEX_PASSWORD_SECRET_NAME='iot-apex-password'

# values of the various models
export DIGITAL_TWIN_MODEL_NAME=homebattery
export DIGITAL_TWIN_MODEL_FILE_NAME=HomeBatteryDTMI.json
# note that this is a relative path
export DIGITAL_TWIN_MODEL_FILE=file://$DIGITAL_TWIN_MODEL_FILE_NAME
# the models name we extract from the model file
export DIGITAL_TWIN_MODEL_IDENTIFIER=`cat $DIGITAL_TWIN_MODEL_FILE_NAME | jq -r '.["@id"]'`
# values for the adapters
export DIGITAL_TWIN_ADAPTOR_ROUTE_MAPPINGS_FILE=file://sonnen-to-generic-mapping-multiple-routes.json
export DIGITAL_TWIN_ADAPTOR_ENVELOPE_MAPPINGS_FILE=file://sonnen-to-generic-mapping-envelope.json
export DIGITAL_TWIN_ADAPTER_NAME=sonnen-multiple-routes

# this is the id of the digital twin iinstance, it will be created if needed, but you can setup your own of course
export DIGITAL_TWIN_INSTANCE_DEVICE_NAME=timssonnen

#these are for the  DB direct connection VCN and VM
export SSH_PUBLIC_KEY_PATH="$HOME/.ssh/id_rsa.pub" 
export VCN_NAME="IoT_DBDirectConnect_VCN"
export VCN_CIDR="10.0.0.0/16"
export SUBNET_CIDR="10.0.1.0/24"

# vm details
export VM_NAME="IotDBVM"