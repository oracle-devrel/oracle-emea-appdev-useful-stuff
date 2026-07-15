#Gateway vs device adapters

ICD = indirectly connected device 

Note that a gateway MUST have an adapter with an envelopeMapping as the system requires it to have a target and cpntentRoute in the envelopeMapping. if that's nto the case (or they are missing) then trying to create the gateway device will  result in an error `Digital twin adapter [ocid1.iotdigitaltwinadapter.oc1.......] does not support gateway connectivity`

The gateway MUST have a endpointMappings field with a target and contentRoot entries.

The *target* entry must map to the external key of the ICD, or if the data is for the gateway itself it should map to null

Note that currently the ONLY place you can determine the target / contentRoot is in the envelopeMapping, and there can only be one of these in the adapter (and of course only one adapter per device) Though mechanisms for multiple target / contenRoute based on the end point are being investigated (no commitments of course !)

It's up to the gateway and adapter author how the target determines the external key, but suggestions are it can be in the incoming endpoint, or it could be a field in the incoming data

If the target is non null (i.e. it points to the external key) then the message is "handed off" to the adapter for the digital twin instance with that external key. If it's null then it's handled by the adaptor for the gateway itself. Currently it's unclear at what point the endpointMappings for the ICD / GW are done, this may have an impact on things like setting timeObserved if using a non $ contentRoot (see later)

If the target is null then the normal envelopeMappings are applied (e.g setting timeObserved if you need to, note that at this point the mappings are based on the entire input data (i.e. before contentRoot below) and it's then handed to the routes part of the gateway adapter. Unclear if setting timeObserved at this point will apply when it gets to the indirectly connected device of it the adaptors there will need to do that based on the data they get.

The contentRoot is used to specify where in the gateway level input the actual content data exists, for example the gateway may have some meta data and then encapsulate the actual device data in a field (e.g $.payload) and you only want that payload to be handed to the ICD adapters OR the telemetry route handling for the gateway itself (i.e. when it is receiving data as a directly connected device)

Using a non $ content root at the GW level may mean that normal "root" level data (e.g. timestamps) must be in the new content root when it gets to the ICDs.

Gotchas
Note that the endpoint *is not modified* when it's passed to the ICD adapter, so a gateway of home/ha/gateway/entities/lux arriving at the GW level will be the same when passed to the ICD level, so if using multiple routes then that full path needs to be considered (of course * would still work if there is only one route as the adapter level is tied to the device) This probably only matters if you're trying to use the same adapter to connect to the device model for both ICD and directly connected devices, in practice I suspect you'd not do that.