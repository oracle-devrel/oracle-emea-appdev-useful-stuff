# these define the various names for the gateway objects
export GATEWAY_MODEL_NAME=hagateway
export GATEWAY_MODEL_FILE=file://GatewayDTMI.json
export GATEWAY_ADAPTOR_ROUTE_MAPPINGS_FILE=file://GatewayProxyRoutes.json
export GATEWAY_ADAPTOR_ENVELOPE_MAPPINGS_FILE=file://GatewayProxyEnvelope.json
export GATEWAY_ADAPTER_NAME=gateway-adapter
# do not change these endpoint paths without also altering the Java code and the DTML route files in the adapter
export GATEWAY_ENDPOINT_PREFIX=house/homeassistant
export GATEWAY_ENDPOINT_GATEWAY_PATH=$GATEWAY_ENDPOINT_PREFIX/gateway
export GATEWAY_ENDPOINT_GATEWAY_STATS_PATH=$GATEWAY_ENDPOINT_GATEWAY_PATH/stats
export GATEWAY_ENDPOINT_GATEWAY_CONFIG_PATH=$GATEWAY_ENDPOINT_GATEWAY_PATH/config
export GATEWAY_ENDPOINT_ENTITIES_PATH=$GATEWAY_ENDPOINT_PREFIX/entities
export GATEWAY_NAME=hagateway
export GATEWAY_DISPLAY_NAME=HomeAssistantGateway