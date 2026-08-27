#How to use this Proxy

The basic operating approach is data is read from an input source, it is formatted into the IoT format and then uploaded. Multiple items can be retrieved and combined into a single IoT upload (e.g. air pressure, temperature & humidity may make sens to be combined as a point in time data item).

The code uses the IoT Gateway functionality to upload the data using the gateway credentials, this means that the individual devices do not need to have credentials themselves, they **do** however need to be known to the IoT service. Data relating to the gateway itself (e.g. upload rates) is also processed and this will be associated with the gateway once uploaded to IoT.

Though this *can* be run standalone it's probably best to run it alongside the scripts in the IoTSonnenUploader project GatewayProxy directory that create the digital twin models, adapters and instances that match the config file samples for this project, at least if uploading pre-recorded data from my home assistant setup in the files I make available to Oracle staff. If you are going to record your own data then you will need to setup your own configuration, though you may find that the contents of the IoTSonnenUploader project GatewayProxy directory useful as a basis.

##Operating modes
There are multiple options for operating modes, these break down into two categories, input and output.

###Input modes
The input modes control where the IoT data origionates from. You set an input mode using the property `operatingmodes.inputmode` and the default is HOME_ASSISTANT.

Supported operating modes are :

**operatingmodes.inputmode=HOME_ASSISTANT** If you want live data from Home Assistant (for upload to IoT or to record) from home assistant. You will need to configure the homeassistant properties (see below)

**operatingmodes.inputmode=REPLAY** If you want to replay a previusly recorded data file (for upload to IoT or just text output). You will need to configure the replay properties (see below)

###Output Modes
 
The output modes control where the IoT data is sent to. You set an output mode using the property `operatingmodes.outputmode` and the default is MQTT
 
**operatingmodes.outputmode=MQTT** If you want the data to be sent to the IoT cloud service, you will need to configure the mqtt properties (see below)
     
**operatingmodes.outputmode=RECORDER** If you want the data to be recorded to a file for later replay, you will need to configure the replay properties (see below)
     
**operatingmodes.outputmode=NONE** If you want the data to just be output, there are no properties to set.

Note that at this point support for uploading using HTTPS is not implemented, though it could be if needed, it would just require the creation of suitable publishers, upload handlers and tracking data.

##Properties

In addition to the **operatingmodes** properties (above) most of the modes require their own properties to be set. You are strongly advised to start form the sample configs and adjust them rather than create your own (with the exception of the monitored entity sets / entities if you are making your own recordings as those will be specific to your situation)

Note that in many cases, these need to match values either set for the IoT cloud service or the Home assistant entities. The ones relating to the IoT service in particular in the samples files match the values set by the scripts in the **IoTSonnenUploader** project. Properties that need to match are marked with `@@`. Properties which probably do not need changing (i.e. they are reasonable defaults) are marked with `$$`

In the details below a property hierarchy is used to reduce the need for replicated names, this a property **home-assistant.api.auth-token** will be in the **home-assistant** section, then the **api** section, then described as **auth-token**, it's assumed that the reader can put these together (and also it kind of matches the hierarchical structure of yaml which is often used in the sample files. If using the properties file approach then you will need to place a `.` between the elements of the hierarchy (which is not shown in the names below) Basically I'm assuming a level of familiarity with properties and readers intelligence!

Note that if you are not using an input or output then in general you do not need to set the properties relating to that mode. The exception is the **home-assistant.managed-entity-sets** which are required for input mode HOME_ASSISTANT and output mode REPLAY. Gateway properties are required if processing the configuration and stats for the gateway instance itself.

UInless a default value is specified for a property then if you are using that mode you MUST set the property, in most cases if a required property is not set (e.g. the **recorder.output.file** property when recording an event stream) then the associated input / output mode will not be started. In virtually all cases that will result in the program halting, usually with copious error messages.

###Properties for the gateway itself,under `gateway`

Note that if using the scripts in the IoTSonnenUploader project GatewayProxy directoty then the following settings can basically be unchanged.

####Properties for the gateway identity under `identity`

**name** The name of the gateway, it's nice if it matched the display name of the gateway in IoT

**devicekey** The device key / external key of the gateway in IoT basically this is the "username" when uploading

####Properties for the gateway endpoints under `endpoints`

**base** The first part of the IoT endpoint (i.e. topic / url path) e.g. house/homeassistant

**gatewaystatssubpath** the next part of the endpoint indicating an event for the stats for the gateway e.g. gateway/stats which when uploading would result in a combined topic for the gateway statistics events of house/homeassistant/gateway/stats

**gatewayconfigsubpath** the next part of the endpoint indicating an event for the config for the gateway e.g. gateway/config which when uploading would result in a combined topic for the gateway configuration events of house/homeassistant/gateway/config

**entitiessubpath** the third part of the endppint indicating an event for an entity, e.g. entities. If the actual monitored entity set has an endpoint of airquality then the combined topic would be house/homeassistant/entities/airquality

####Propertties for the gateway configuration under `config`

Currently the configuration events reflect the time windows for statistics, really just to show willing

**enabled** set to true to periodically sent configuration events to the output, false if you don't want them

**publishrate** How often to publish a gateway configuration event, can use other time intervals like 5m or 2h. Defaults to 1200s (20m mins)

**initialdelay** How long to wait before sending the first configuration event, e.g. 5s will send it 5 seconds after the application is up and running. Defaults to 5s

####Properties for the gateway configuration under `status`

The status reports on the number of successful and failed retrieves from home assistant and successful and failed uploads to IoT

**enabled** set to true to periodically sent status events to the output, false if you don't want them

**publishrate** How often to publish a gateway status event, can use other time intervals like 5m or 2h defaults to 120s (2 mins)

**initialdelay** How long to wait before sending the first status event, e.g. 15s will send it 15 seconds after the application is up and running. Defaults to 30s


###Properties for input mode HOME_ASSISTANT under `home-assistant`

You need to specify the properties that allow the connection to Home Assistant as well as the entity sets and individual entities to retrieve along with their IoT upload info (the deviceKey and upload path).

####Accessing Home assistant API under `api`

Note that the **home-assistant.api** properties are only needed when retrieving data from home assistant, if you are replaying you can ignore these, though you will need the properties under **home-assistant.monitored-entity-sets** to identify the details of entity sets to be uploaded.

**auth_token** - this is the long lived token you need to access the home assistant API's. Go to your profile in HA, then security tab, then long-lived access tokens, see the Home Assistant documentation for more details.

**port** $$ this is the port the home assistant instance lives on. Historically the default was 8123 but recently Home Assistant changed it to 80 for new home assistant installations (on Raspberry PI's at least)

**host** this is the host IP address or host name for the home assistant instance. the IP will be specific to your network, but you may find that using *homeassistant.local* works (at least when running Home assistant on a raspberry pi)

**protocol** $$ http or https to access the home assistant API, unless you have specifically set home assistant up with a certificate this will be http.

**url** This builds the URL for the home assistant API from the previous API settings. You should not change this unless you really know what you are doing.

####The monitored entity sets under `monitored-entity-sets`

If you are replaying recorded data you will need the properties under **home-assistant.monitored-entity-sets** to identify the details of entity sets to be uploaded. The names will need to match the names the data was recorded under.

The monitored entity sets defines a group of connected home assistant entities that will be uploaded together. There can be many monitored entity sets in this list.

For each monitored entity set the following properties can be defined

**name** The human readable name of the entity set e.g. "Grid Energy Flows", used for display purposes and also for matching recorded data when uploading (in-case you have different device keys between the system used for recording and replay). 

**doupload** $$ If the retrieved entity set should be uploaded to IoT (or recorded) defaults to true, if set to false will be retrieved but not handed to the output, this allows you to test out the retrieval of Home assistant entities.

**initialdelay** $$ How long to wait before starting to retrieve the home assistant entities in this set, primarily used to avoid overloading the home assistant instance with many simultaneous requests at startup. Is represented as a Java Duration, e.g. 'PT30s' means 30 seconds and 'PT1m' is a minute. Defaults to 5 seconds

**retrievalrate** $$ How long to wait between retrieving the home assistant entities in this set. Is represented as a Java Duration, e.g. 'PT30s' means 30 seconds and 'PT1m' is a minute. Defaults to 10 seconds, but should be adjusted depending on how frequently the home assistant entity will update (no point in getting it every second if it only updates in home assistant every hour!)

**devicekey** @@ The device key for this digital twin instance, this must match the external key in the IoT cloud service. As you can specify this (and if you use them the scripts in the IoTSonnenUploader that set up the instances do) it's best to make sure it is a name that makes sense (for testing) if however you let the IoT service generate a random external key you will need to make sure that this is set and matches to get the data to the right digital twin instance.

**endpoint** @@ The endpoint (or to be precise the last part, see the gateway properties for more details on the earlier part)to be used to select the IoT digital twin routes (part of the envelope setup) this needs to match the last part of the endpoint in the IoT envelope routes and envelope mapping for the digital twin model.

**timestampmode** $$ When there are multiple entities in the entity set how to handle the individual timestamps when calculating the time observed for the overall event being sent to IoT. Options are EARLIEST (the earliest time stamp across all of the retrieved entities metadata in this set) LATEST  (the latest time stamp across all of the retrieved entities metadata in this set) or AVERAGE (calculated across all of the retrieved entities metadata in this set) The default is LATEST

##### The ltst of Home assistant entities to monitor under `monitoredentities` 
This is a list of monitored entities representing the individual entities to be retrieved from Home assistant and combined when sending the event from the monitored entity set.  Below are the properties for each entity.

**name** The name to use in debug output and the like.

**entityid** The id of the entity in Home assistant, e.g. sensor.ground_lounge_ruuvi_351e_temperature. This must match exactly the name in home assistant.

**iottype** The type of the data, Home assistant stored everything in a String, so it needs ot be converted, this specifies the conversions as well as a default field name (i.e. the name given to this entities data when uploading the entity set event) and a potential default value to use if the entities data cannot be retrieved (usually this is an "impossible" number like -274 for temperature as absolute zero is -273.15c. There are many possible values for this data see the IoTTypes enum for the full set of options, but when this readme was written known data types were :

    AIR_QUALITY_CO2_PPM, AIR_QUALITY_NOX_INDEX, AIR_QUALITY_VOC_INDEX, AIR_QUALITY_PM_2_5, AIR_PRESSURE,	BINARY_ON_OFF, BOOLEAN, COST, ENERGY_KILO_WATT_HOURS, ENERGY_WATT_HOURS, LUMINANCE, MATTER_DOOR, MATTER_OCCUPANCY, MATTER_WINDOW, NUMBER, PERCENT, POWER_WATTS, POWER_KILO_WATTS, RELATIVE_HUMIDITY, SWITCH, STRING, and TEMPERATURE
    
Please see the enum **com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTType** for full details including the default values and default field names. Note that the default field names are reasonable defaults for a single entity managed set, but they generally describe the iottype, so you may well need to override it (see **fieldname** below)

**fieldname** This specified the file name to use in the payload for this entity when uploaded as part of the entity set event, for example a field name of `coretemp` will result in the generated JSON having a field of coretemp` and if the iottype was set to Temperature it will be represented as a number. If the same field name is used twice then the actual value assigned will be undefined, and may well vary across multiple event uploads (it's based on whoever added the data last and as this is heavily mulltithreaded that can't be determined).

**sendmode** When to upload data, the default is ALWAYS but if it's set to ON_CHANGE the entity will only be sent if the data has changed cince it was last retrieved, if it's set to ON_REPORT it will be sent if home assistant has retrieved the data for the source, even if the value is unchanged. ALWAYS is probably the best option.

**dontsendifunavailable** Defaults to true. if the data cannot be retrieved then if this is false the default value will be added to the entity set event, if it's true then the entity set event will not contain and entry for this entity


###Properties for input mode replay under `replay`

These control the replay of a pre-recorded file (see output mode recording later on)

**inputfile** the path (relative or absolute) to the data file containing the recorded data. You can generate that file by hand but it's far better to use the recorder output mode.

**replayStartOffset** How long from the first timestamp in the input file is the first entry you want to upload. For example 1h would skip the first hours worth of data in the input file (based on the timestamps in that file) Defaults to 0 seconds. Note that if the start offset is such that the end of the file is reached first then no events will be output.

####Properties for input mode REPLAY high speed under `highspeed`

**duration** For how long after any start offset (both based on the input file timestamps)  should the data be sent to the output in high speed mode. The default is 0s. This is basically a bulk upload mechanism, with the timestamps adjusted so that the last entry sent in high speed mode should be timestamped at the current time. The idea is that if doing a demo you can easily build up a data history prior to sending the data as it it were being generated in real time. Alternatively if you want to show some form of data mining then you may chose to upload all of the recorded data in high speed mode. Note that if the high speed duration is such that the end of the file is reached first then processing will stop at that point (as you'd expect with no data remaining !)

**delay** How long between each event output should the replay code wait before sending the next event. This is provided to avoid overloading or hitting throttling in the IoT or connected services. The default is 0s, but if you are going any form of real time extraction of data form the IoT service you should carefully consider increasing this (If using OIC for example it's charged based on throughput, so you probably don't want a very sudden large batch of data that only lasts for a short time). The delay is taken into account when setting timestamps on entities that are output

####Properties for input mode REPLAY real time under `realtime`

**duration** How long data after the high speed upload ends (both based on the input file timestamps) should be output in the real time mode stop. In this mode each event is sent based on the recorded timestamp intervals and output based on those, so if there was a 12 second delay bewettn two events when recorded there will be a realtime 12 second delay between the events in this mode. The timestamps are adjusted to reflect the current time (note that if the event was based on unchanged data in home assistant the underlying data timestamp will reflect when the data was originally changed, even thoguh the timestamps for generating the event are current time based. See the timestamp mode options in recording) Note that if the realtime duration is such that the end of the file is reached first then processing will stop at that point (as you'd expect with no data remaining !)

###Properties for output mode NONE

This output mode just prints out what would otherwise have been sent, as such there are no properties to set.

###Properties for output mode MQTT under `mqtt`

This is the mode that uploads to the IoT service. It is important to understand that this code does not use Eclipse Paho (the default MQTT client library) but instead used the Hive client libraries. This is because Hive handles MT far better than Paho, especially if handling messages back to the devices via the gateway (That is not currently implemented, but using Hive allows it to be done in the future)

Most of the properties below are actually "standard" Micronaut MQTT properties, for full details you should look at the [Micronaut documentation.](https://micronaut-projects.github.io/micronaut-mqtt/latest/guide/)

####Properties for MQTT broker under `broker`
These specify the MQTT broker that the output will connect to.

**host** This is the hostname of the IoT MQTT service (obtained via the OCI console page for the IoT domain). In the sample files this references the private setting **private.devicehost** (from the configsecure/iotsecure.yml file) which allows the separation of confidential info, for example is using an OCI vault secret of Kubernrtes secret

**port** The port the mqtt server listens on. For the OIC IoT service this is 8883 

**protocol** The underlying protocol the mqtt client will use. For the OCI IoT service this is ssl

####Properties for the MQTT client under `client`

These describe the client settings, for example it's username. 

**client-id** This is basically the name of the gateway, in the example settings this is set to the **gateway.identity.name** so it only needs to be set there. It;s used byt he client to identify the connection.

**server-uri** The URI used by the mqtt client library to connect to the broker, it's a combination of protocol, port and host name. In the sample mqtt.yml it is constructed from those properties.

**user-name** This is the username of the gateway in the IoT Cloud service, basically it's external key. In the sampel properties files this used the property **gateway.identity.devicekey** which is set in the gateway.yml file

**password** This is used as the password to authenticate the gateway connection. In the OCI IoT Cloud Service this is stored in a vault key that is specified when the gateway is created. However in a production system a certificate would be used (that is not supported here). To allow separation of public and private data in the sample mqtt.yml file this points to **private.mqtt.client.passwordoracle** in the configsecure/iotservice.yml file and so should be set there if you are basing your configration on those sample properties files.


The following properties are [documented in the Micronaut MQTT Hive information](https://micronaut-projects.github.io/micronaut-mqtt/latest/guide/#config), please refer to them for more details. If you are using the sample properties files you can leave them set as they are.

**clean-session**, **automatic-reconnect**, **connection-timeout**, **keep-alive-interval**, **will-message** (and sub properties)

Note that to reduce the changes needed when modifying things like the gateway name the **will-message** properties section in the sample mqtt.yml file references other properties.

###Properties for output mode RECORDER under `record`

These control the recording output mode. Note that the recording mode will always record the event data stream, but the gateway config and stats stream can be enabled / disabled using their properties (see gateway properties above).

**duration** How long the recording will last once it's started (see **startat** below) Once it's completed then the file will be closed.

**exitafterrecordingstop** Once the recording is stopped if true the application is shutdown, if false then data will continue to be retrieved, but it will not be written to the file. Defaults to true.

**startat** This is optional, but if present must contain a data / time group of the format "uuuu-MM-dd'T'HH-mm-ss.SSSSSSX" Note that the timezone (the X field) MUST be in GMT (and yes that's laziness on my part). The recording process will start at that time and continue after that for the duration specified. Note that the data will still be collected form the input source. This is provided because in some situations you may want to start the recording process at a time when it's not convenient for you to be there in person to kick off the application (e.g. in the middle of the night). As an example 2026-11-16T00-00-00.000000Z means that recording will start at midnight GMT on the 16th November 2026. Note that all 6 microsecond digits need to be specified. 

###Properties for outputs for the output mode RECORDER under `output`

**file** The name of the output file *within the output directory* , e.g. recording.txt (Though the data is written in a line by line JSON format, the entire file does not contain one overall JSON object, so .txt is more appropriate than .json). Note that theoutput file can be prefixed wiht the date / time group (see prefixwithdtg below) If the file exists it will be overwritten and you must have premission to do that.

**directory** The directory where you want the output file to be written. Of course you must have permission to create (and delete) files in that directory. This can be a relative or absolute path.

**prefixwithdtg** If true then the specified file name will be prefixed wiht the data / time group as of the start of the recording. shit is in the format "uuuu-MM-dd'T'HH-mm-ss.SSSSSSX" so for example id the output directory was savedata and the file name is recording.txt then the resulting data may be written to a file named savedata/2026-08-26T13-29-17.248217Z-recordeddata.txt of course the timestamp will vary based on when you run the program. This is recommended to avoid accidentally overwriting an existing file. Defaults to true.

##Sample properties files
This git repo contains a number of sample properties files, each file contains a set of properties that relate to and functionality described by the name (e.g. gateway.yml contains details of the gateway itself and mqtt.yml contains the properties for sending to the mqtt server)

##Running the application with a config based on the sample files.

Exactly how you run this program will depend on your environment of course, but assuming you have build it into a jar file called IoTDemoProxyGateway.v2.0.0 in the target directory (this is what Maven will do, though the version number may differ) and you have used the same directories / configuration file names then the following will run the application

`java -Dmicronaut.config.files=configsecure/iotsecure.yml,configsecure/homeassistantsecure.yml,config/gateway.yml,config/homeassistant.yml,config/mqtt.yml,config/record.properties,config/replay.properties,config/operatingmodes.properties  -jar target/IoTDemoProxyGateway.v2.0.0.jar`
If you want you can set some (or all if you're making things hard for yourself) properties on the command like, in reality this is likely to be the input / output modes, so for example the following will force the use of the replay input sending to mqtt (though as mqtt is the default that's not really required unless you've overwritten it in a different config file from the operatingmodes.properties).

`java -Doperatingmodes.inputmode=HOME_ASSISTANT -Doperatingmodes.outputmode=MQTT -Dmicronaut.config.files=configsecure/iotsecure.yml,configsecure/homeassistantsecure.yml,config/gateway.yml,config/homeassistant.yml,config/mqtt.yml,config/record.properties,config/replay.properties  -jar target/IoTDemoProxyGateway.v2.0.0.jar`

(note that config/operatingmodes.properties has been removed from the configuration files list as that sets the input / output modes, though the command line options will override and file base options it's just cleaner to do this)

Its also possible to set properties within micronaut through environment variables of many other sources, see the [Micronaut configuration documentation](https://docs.micronaut.io/latest/guide/#propertySource) for more details.



