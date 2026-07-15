#README
the gateway proxy directory is used to hold configuration in formation for the IoTDemoProxyGateway project. this configuration information is held in this project (IoTSonnen), not IoTDemoProxyGateway  because it's linked to the setup scripts used here, and cross project would result in duplication and sync problems.

##How to use
the gateway-names.sh script contains all of the names, check that to see if there are things you want to change

You need to have setup the IoTCore, then run the CreateGatewayConfig.sh script,It will gather then various bits of info that the core has setup, then build on top of that

