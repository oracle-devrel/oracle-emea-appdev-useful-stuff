#IOTDemoGateway

This is example code of how a gateway might be implemented in front of the Oracle IOT Cloud service (new version). It used the Java simple wrappers around the OCI API to handle the OCI interactions (Note that these are not in a public Maven repo, you'll need to get them and build them yourself from https://github.com/oracle-devrel/oci-java-sdk-simple-wrappers )

This is example code only, it is not in any way an Oracle product, supported, and in no way is Oracle liable for any problems you may encounter directly or indirectly from using it. See the copyright statements in the source code files for more details.

##What is does

The short version is that it listens for data requests coming in, and will send them to the iot service using the https connection, if required creating a new digital twin instance. There are pluggable mechanisms for transforming the incoming device id and payload if needed. While the code does try and ensure that digital twin instance (and associated resources) creation along with data upload does happen in the right order there are some possible race situations when a large number of events are submitted in a sort time on a digital twin that needs creating, so use of this code in a production environment is not a good idea. Additionally it holds the various queues it uses internally in memory, so in the event of a failure of the application or JVM that datw will be lost, it's recommended that anyone implementing a real gateway themselves use some form of genuine persistence mechanism to ensure that data is not lost in the event of a failure of the code or it's runtime.

##Testing
There is an associated project called IotDemoGatewayClient that provides a simple way to send test data to this demo gateway (using some demo digital twin models / adapters). This uses 

##How it works
Please see the configuration properties section later on to understand the various settings in the configuration file.

This makes use of the Micronaut AOP approach to dynamically construct and inject instances of classes under the control of property files. For example in this code the device id transforms implement the InstanceKeyTransformer interface, but the creation of an instance can be controlled using the `@Required` annotation to only create an instance if a property is set to enable it. A list of create instances is then injected into a management class, which orders them as required (in the examples provided this is done using a configuration property to allow you to change the order easily) then when a transformation is needed they instance key transformers are called in the specified order, for example converting to lower case and then from my_key_identifier to myKeyIdentifier (reversing the order would result in a different outcome of mykeyidentifier). By using this approach it's possible for other developers to add additional transformer and insert them into the sequence in the right order for your use case without modifying anything other than the config.yml file and ensuring that their transformer classes are in the class path.

To minimize the number of round trips to the OCI API several caches are used, in practice this is not likely to be an issue, but people using this as an example should be aware that there is a potential for cache inconsistency if for example the secret holving the devices credentials is changes and code it not implemented to update (or invalidate) the cache entries. Also if a highly secure environment is needed caching credentials in memory in the JVM is probably not the best option form a security perspective (But for a demo showing a concept it's fine as long as you're aware of it).

##How to run this.
The main class is `com.oracle.timg.demo.iot.demogateway.Application` you must provide config properties somewhere (see the config properties in each class documentation for details on their possible values) If you are not using the default Micronaut properties location this can be specified using 

The build includes support for YAML and Java properties files

##Digital twin instance creation
The demo used the transformed instanceId as the external key for the instance in the IOT service, this means that there is a simple and easy direct mapping from for example a MAC address of the device and significantly reduces the effort involved in setting up and maintaining mappings from the devices embedded characteristics to the IOT.

As Iot Digital Twin Instances require security credentials to authenticate themselves along with the device id (external key) the code allows the re-use of a common secret or the dynamic creation of a new secret per device. This is done using the Micronaut injection capability along with it's instance creation mechanisms (`@Required`) to dynamically chose the auth id creation mechanism. This also means that it's possible to easily add additional option if needed without re-writing the existing code. See the NewInstanceSecretProviderReuseSecret.java class for an example of how to control the creation of classes that implement the NewInstanceSecretProvider.java and ensure that only one instance of this interface is every created.

##Event upload
Once the digital twin instances had been located or created then event data can be uploaded to it. As with the instance id there is a transformer chain mechanism (this time based on the EventDataTransformer interface) to allow the reformatting of data. An example showing how to transform a XML payload into a JSON payload is included, but be aware that this is pretty simple and may well not work with complex payloads.

##General Package Structure

###com.oracle.timg.demo.iot.demogateway
This is the main package for this project.

####Application
This is the runnable class, it starts the Micronaut environment which will then setup everything else. 

Note that though the this does not use lazy instantiation that some classes (including the EventQueueProcessor) are configured to be instantiated at the Micronaut context start up as they operate asynchronously(and this it's the best way to start them while avoiding dependency loops). Thus the threads that pull data off the event queue and their associated classes (actually most of the code) is started when the application starts, it's just the front end threads that are not.

#####Configuration
`micronaut.server.port`  - specifies the port for the incoming events REST api server to listen on

###com.oracle.timg.demo.iot.demogateway.caches
Both of these caches use the OCI Details objects in the com.oracle.timg.demo.iot.demogateway.ociinteractions package to hold the OCI processors that do the actual work.

####AuthIdToAuthValueCache
Cache to convert the digital twin instance auth id to it's value (required for the https authentication). If there isn't a cached value it will use the wrappers around the OCI Java SDK to get one. It's needed as the Vault APIs are rate limited and we don't want to be hitting them every time an event comes in from a device. This currently is based on using secrets to hold the security credentials of the device. The cache does no re-check the contents so at some point a mechanism to identify any failed authentication and invalidate that cache entry ad well as re-submit the event would be a good idea.

#####Configuration
No direct configuration, but please see the VaultServiceDetails configuration below as that is used by this class

####IdToInstanceMapping
Once the incoming instance identifier has been transformed this cache is used to hold the mapping to the OCI IOT data structure for the digital twin instance. This cache is also nto automatically invalidated. When it starts up it will load the details of all of the exiting digital twin instances.

#####Configuration
`gateway.iotservice.digitaltwinmodel.name`  - the name of the digital twin model to be used when locating digital twin instances

Please see the IotServiceDetails configuration below as that is used by this class


###com.oracle.timg.demo.iot.demogateway.controllers
####IncommingDataReciever
This is the REST api controller for the incoming traffic, it's a standard Micronaut rest controller that takes the events, wraps them up in a holding object and adds them to the eventqueue for later asynchronous processing.

###com.oracle.timg.demo.iot.demogateway.eventdatatransformer
#### Exception classes
These are used to indicate exceptions relating to a problem in the incoming event data structure itself (EventDataIncommingFormatException) or a problem actually doing the conversion (EventDataTransformConversionException)

####EventDataTransformer
This is the interface used to indicate to Micronaut to create instances (potentially, depending on any requirements applied to the implementing classes) and add them to the list of event data transformers in the EventDataTransformService class. The interface required a getOrder method to indicate where in the transformer sequence the transform should be applied, it's up to the implementing class where to get this order information but it's recommended to use the Micronaut config system as that would enable users to enable / disable and change the order by simple configuration changes and not require code changes.

An example implementation that just passes the data back (EventDataTransformerPassthrough) is provided along with one that will convert XML input into JSON (EventDataTransformerXmlInputToJsonOutput) which may be useful as the IOT service uses JSON payloads for event data.

#####Configuration for EventDataTransformerPassthrough
`gateway.eventdatatransformer.passthrough.enabled` must be set to true for this class to be instantiated, if the property is missing it will not be instantiated
`gateway.eventdatatransformer.passthrough.order` can be optionally set to override the default ordering of 10 in the transformer chain sorting

#####Configuration for EventDataTransformerXmlInputToJsonOutput
`gateway.eventdatatransformer.xmlinputtojsonoutput.enabled` must be set to true for this class to be instantiated, if the property is missing it will not be instantiated
`gateway.eventdatatransformer.xmlinputtojsonoutput.order` can be optionally set to override the default ordering of 20 in the transformer chain sorting

####EventDataTransformService
This class has a list of available EventDataTransformers injected into it, it then sorts them into order and when provided with an input it will apply each transformer in turn (i.e. output of the previous one is sent to the input of the next one) to get the final transformation of the event data. If no transformers are available then the input event data is simply handed back as the output.

###com.oracle.timg.demo.iot.demogateway.instancekeytransformer
#### Exception classes
These are used to indicate exceptions relating to a problem in the incoming instance key itself (InstanceKeyIncommingFormatException) or a problem actually doing the conversion (InstanceKeyTransformConversionException)

####InstanceKeyTransformer
This is the interface used to indicate to Micronaut to create instances (potentially, depending on any requirements applied to the implementing classes) and add them to the list of instance key transformers in the InstanceKeyTransformService class. The interface required a getOrder method to indicate where in the transformer sequence the transform should be applied, it's up to the implementing class where to get this order information but it's recommended to use the Micronaut config system as that would enable users to enable / disable and change the order by simple configuration changes and not require code changes.


An example implementation that just passes the data back (InstanceKeyTransformerPassthrough) is provided along with one that will convert any upper case characters to lower case (InstanceKeyTransformerToLowerCase) and anothert that converts snake_case to camelCase (InstanceKeyTransformerSnakeCaseToCamelCase). These latter two are provided to demonstrate the use of the ordering capability, as changing the order will result in different output (e.g. my_identifier -> myIdentifier if the lower case is done first but my_identifier -> myidentifier if the lower case it done later in the transform sequence)

#####Configuration for InstanceKeyTransformerPassthrough
`gateway.instance.keytransformer.passthrough.enabled`  must be set to true for this class to be instantiated, if the property is missing it will not be instantiated
`gateway.instance.keytransformer.passthrough.order` can be optionally set to override the default ordering of 10 in the transformer chain sorting


#####Configuration for InstanceKeyTransformerToLowerCase
`gateway.instance.keytransformer.tolowercase.enabled`  must be set to true for this class to be instantiated, if the property is missing it will not be instantiated
`gateway.instance.keytransformer.tolowercase.order` can be optionally set to override the default ordering of 20 in the transformer chain sorting


#####Configuration for InstanceKeyTransformerSnakeCaseToCamelCase
`gateway.instance.keytransformer.snakecasetocamelcase.enabled`  must be set to true for this class to be instantiated, if the property is missing it will not be instantiated
`gateway.instance.keytransformer.snakecasetocamelcase.order` can be optionally set to override the default ordering of 30 in the transformer chain sorting

####InstanceKeyTransformService
This class has a list of available InstanceKeyTransformers injected into it, it then sorts them into order and when provided with an input it will apply each transformer in turn (i.e. output of the previous one is sent to the input of the next one) to get the final transformation of the instance key. If no transformers are available then the input instance key is simply handed back as the output

###com.oracle.timg.demo.iot.demogateway.iotupload
####IotServiceClient
Interface describing the uploader of events to the OCI iot cloud service. There must be one and only instance of this interface instantiated at any time, it's recommended that this is controlled using the `gateway.iotservice.uploadmechanism` property and the Micronaut @Requires annotation on implementing classes

####IotServiceClientHttps
Only instantiated if `gateway.iotservice.uploadmechanism` is set to HTTPS.

This actually uploads the transformed data to the iot service using https. This does not use the Micronaut http client as each instance will have a unique id and may have unique credentials.

#####Configuration
`gateway.iotservice.uploadmechanism`  must be set to "HTTPS" for this class to be instantiated, if the property is missing it will not be instantiated.

`gateway.iotservice.digitaltwinadapter.pathprefix` must be set to the path prefix to be used when sending data to the IOT services, for example /home/myrobots all events will have that applied. This is need to differentiate between multiple adapters in the IOT service.
###com.oracle.timg.demo.iot.demogateway.ociinteractions
These classes call the OCI API to get information about existing resources and to create new ones

####Exceptions
The MissingOciResourceException is used to indicate that a requested resource (e.g. compartment, vault etc.) cannot be located, this is usually because the name provided is incorrect or refers to a resource in another compartment.

#####The "Details" classes
These classes act as holders for the associated OCI simple wrappers enabling properties to be specified when using the wrapper classes (which do not support the injection of properties directly), They can also use these properties to specify the names of OCI resources and convert them into the data structures used by the OCI APIs, for example the compartment "path" can be specified textually avoiding the need to specify OCIDs directly.

#####Configuration AuthenticationProcessorDetails
`gateway.oci.config.sectionname` - if you need to use a section in the OCI config file that is other than DEFAULT set that here

#####Configuration IotServiceDetails
Note that this class uses the  AuthenticationProcessorDetails so see the configuration for that class as well.

`gateway.iotservice.domaingroup.compartment` - must be set to the compartment path containing the iot domain group e.g. /projects/iot

`gateway.iotservice.domaingroup.name` - must be set to the display name of the iot domain group in the compartment

`gateway.iotservice.domain.name` - must be set to the display name of the iot domain in the iot domain group

`gateway.iotservice.digitaltwinmodel.name` - must be set to the name of the digital twin model in the iot domain

`gateway.iotservice.digitaltwinadapter.name` - must be set to the name of the digital twin adapter in the iot domain

#####Configuration VaultServiceDetails
`gateway.instance.secret.vault.compartment` - must be set to the "path" of the compartment the vault is in, this should start with / e.g. /projects/iot

`gateway.instance.secret.vault.name` - must be set to the name of the vault in the compartment, if there are multiple vaults with that display name the first returned will be used.
####Secret Providers
To create a new digital twin instances in the OCI IoT service authentication needs to be specified, this can be MTLS, but for simplicity this code uses OCI Vault Secrets.

These secrets can be reused across multiple digital twin instances, or a new secrets created for the instance, the NewInstanceSecretProvider defines an interface that is used to get the secret details. There must be one and only one implementation of this interface instantiated and it's recommended to use the Micronaut config property `gateway.instance.secret.newinstancesecretmode` along with the `@Requires` annotation to control which implementation is instantiated.

#####NewInstanceSecretProviderReuseSecret
This implementation of NewInstanceSecretProvider is used when the secret is shared, it gets the OCID of a specified vault secret and returns it on request. This will only be instantiated if the Micronaut property `gateway.instance.secret.newinstancesecretmode` is set to REUSE_SECRET

#####Configuration
Note that this class uses the VaultSecretDetails and AuthenticationProcessorDetails to see the configuration for those classes as well.

`gateway.instance.secret.newinstancesecretmode` - must be set to "REUSE_SECRET" for this class to be instantiated, if the property is missing it will not be instantiated.

`gateway.instance.secret.reuse.secret` - set to the name of the secret in the vault that contains the password to be used when creating a new digital twin instance.

#####NewInstanceSecretProviderGenerateUniqueSecret.java
This implementation of NewInstanceSecretProvider is used when a new secret, will create a new secret containing a string of ransom characters in the named vault encrypted using the named key. This will only be instantiated if the Micronaut property `gateway.instance.secret.newinstancesecretmode` is set to GENERATE_UNIQUE_SECRET. Note that creating a secret can take some time (10 - 30 seconds usually) and this code will wait for the secret to become active before returning, so calling code needs to be able to handle this (and in particular any other events from the same device that may arrive during the creation process).

#####Configuration
Note that this class uses the VaultSecretDetails and AuthenticationProcessorDetails so see the configuration for those classes as well.

`gateway.instance.secret.newinstancesecretmode` - must be set to "GENERATE_UNIQUE_SECRET" for this class to be instantiated, if the property is missing it will not be instantiated.

`gateway.instance.secret.generate.key` - must be set to the name of the master encryption key in the vault which will be used to encrypt generated secrets

`gateway.instance.secret.generate.secretprefix` - optional (default iot-gateway-generated-secret) used as a prefix to generated secrets to identify them as coming from the gateway

`gateway.instance.secret.generate.secretlength` - optional (defaultValue 16) how long the generated secret should be, minimum 14 and maximum 32

`gateway.instance.secret.generate.secretchecktime` - optional (defaultValue 300) how many seconds to wait for the secret to be created before erroring 

####com.oracle.timg.demo.iot.demogateway.queue
The classes in this package do most of the heavy lifting, especially the EventQueueProcessor.

#####EventQueueData
Packages up the event instance id and the received payload so they can be handled as one.

#####EventQueue
This interface defines a queue to allow the receipt of events to be stored (as an EventQueueData object) and asynchronously uploaded removing blocking in the incoming events processor. There must be one and only one implementation of this interface instantiated and it's recommended to use the Micronaut config property `gateway.eventqueue.type` along with the `@Requires` annotation to control which implementation is instantiated.

#####EventQueueInMemory
This is an implementation of the EventQueue interface, This will only be instantiated if the Micronaut property `gateway.eventqueue.type` is set to IN_MEMORY. Note that this queue does not provide persistence outside the JVM. It is also capacity limited.

#####Configuration 
`gateway.eventqueue.type` must be set to IN_MEMORY for this class to be instantiated

`gateway.eventqueue.inmemory.size` - optional (default 1024) Size of the queue to use, if this limit is reached events will be dropped.

#####EventQueuePending
This interface is used by the event queue processor to hold events for digital twin instances that are currently unavailable, this is primarily because the creation of a digital twin instance can take time, especially if a new secret needs creating to hold the devices credentials.  There must be one and only one implementation of this interface instantiated and it's recommended to use the Micronaut config property `gateway.eventdatapending.type` along with the `@Requires` annotation to control which implementation is instantiated.

#####EventQueuePendingInMemory
This is an implementation of the EventQueuePending interface, This will only be instantiated if the Micronaut property `gateway.eventdatapending.type` is set to IN_MEMORY. Note that this does not provide persistence outside the JVM. WHile it is not explicitly capacity limited it is of course subject to the JVM memory limitations. 

#####Configuration 
`gateway.eventdatapending.type` must be set to IN_MEMORY for this class to be instantiated

#####EventQueueProcessor
This does the majority of the work. It pulls events from the event queue and if needed will create the digital twin instance. If an event relates to a device that's currently being created it will be saved for processing once that has completed.

The incoming event source and payload are run through the transformer chains as above before being uploaded to the OCI IoT Service.

Processing can be handled in a single thread or a separate thread used for each event

#####Configuration
This makes use of the IotServiceDetails so see that class for it's required configuration

`gateway.uploaddata` optional (default true) set to false if you want to just test out the data capture, but not the data upload or digital twin instance creation. This will however read all of the data from OCI (e.g. compartments, vaults etc.) so those will need to be set. It true (the default)or missing events will be uploaded

`gateway.multithreaduploads` optional (default true) set to false if you want to use a single thread for the event processing, potential device creation and uploading data, if true (the default) or missing will use a separate thread for each queued event.

`gateway.instance.oninstancecreationgeterrorresubmit` optional (default true) if a problem occurs in the creation of the instance then if true the initial event that triggered the creation as well as any new events that have arrived during that process will be added back to the event queue (in the order they were received) which will trigger another attempt immediately - this is useful for temporary problems. If however it's false then the initial event and any received during a failed digital twin creation will be discarded - note that a subsequent event after this point in time will trigger the creation process again.