# IOT JDBC access
This is built using maven and the micronaut libraries. see below for details on micronaut

YOU MUST be running this on a compute resource on a private VCN that is connected to the IOT database, see the IOT documentation at https://docs.oracle.com/en-us/iaas/Content/internet-of-things/connect-database.htm for details on how to set this up.

## Dynamic source and handler selection

The application uses Micronaut dependency injection to decide which IoT data sources and output handlers are active at runtime. The code does not use a central switch statement. Instead, each optional reader, filter, processor, and output is a Micronaut `@Singleton` bean guarded with `@Requires` annotations. If the required configuration properties are present, and any `.enabled` property is set to `true`, Micronaut creates that bean. If the properties are missing or disabled, the bean is not created and is not injected anywhere else.

### Selecting input sources

All database and AQ readers implement `IoTDBClient`. `JDBCRunner` asks Micronaut for `List<IoTDBClient>`, so only the readers whose `@Requires` conditions pass are injected. The runner sorts the injected clients by each client's configured `getOrder()` value, calls `configureDBClient()`, then starts them with `startDBProcessing()`. This means one configuration file can enable one source, several sources, or none.

The available input clients are:

| Client | Data source | Main enable property | Order property |
| --- | --- | --- | --- |
| `IoTJDBCConnectionTestReader` | Reads sample rows from the `raw_data` table over JDBC. | `iotdatacache.jdbc.doconnectiontestread.enabled=true` | `iotdatacache.jdbc.doconnectiontestread.order` |
| `IoTAQNormalizedDataBatchReader` | Dequeues batches from the normalized data AQ queue and passes each message to the normalized data handler chain. | `iotdatacache.aq.normalizeddata.batchreader.enabled=true` | `iotdatacache.aq.normalizeddata.batchreader.order` |
| `IoTAQNormalizedDataIndividualReader` | Dequeues normalized AQ messages one at a time and passes them to the normalized data handler chain. | `iotdatacache.aq.normalizeddata.individualreader.enabled=true` | `iotdatacache.aq.normalizeddata.individualreader.order` |
| `IoTAQRawDataIndividualReader` | Dequeues raw data AQ messages one at a time and passes them to the raw data handler chain. | `iotdatacache.aq.rawdata.individualreader.enabled=true` | `iotdatacache.aq.rawdata.individualreader.order` |
| `IoTAQNormalizedDataListener` | Registers an AQ notification listener for normalized data. | `iotdatacache.aq.listener.enabled=true` | `iotdatacache.aq.listener.order` |

Readers can also have source-specific settings. For example, the batch reader uses `iotdatacache.aq.normalizeddata.batchreader.batchsize`, and the AQ readers use read timeout and subscriber-name properties. See `config/config.properties` for examples of the available settings.

The normalized batch, normalized individual, and raw individual AQ readers feed messages into the handler services. The notification listener currently dequeues and logs normalized messages through its core output method.

### AQ versus JDBC

The JDBC reader and the AQ readers both use database connections, but they use different database interaction models. The JDBC reader runs SQL directly against database tables, for example reading recent rows from `raw_data`. It is useful for direct queries, connection checks, diagnostics, and any case where the application wants to decide exactly which table rows to fetch.

AQ, or Advanced Queuing, treats incoming IoT data as queued messages rather than table rows to query. The AQ readers subscribe to the IoT queue, dequeue messages from it, convert the queue payload into `RawData` or `NormalizedData`, and then pass the converted object to the appropriate message handler service.

The current AQ readers can run as polling loops. For example, the individual and batch readers repeatedly call `dequeue` with a configured wait timeout. If no message is available before the timeout, the loop simply waits again. When a message is retrieved, the reader immediately hands it to `RawDataMessageHandlerService` or `NormalizedDataMessageHandlerService`. Because downstream code receives each retrieved message through the handler chain as soon as the polling loop obtains it, the rest of the application can treat the flow as listener-like even though the reader itself is implemented as a polling loop.

The AQ `dequeue` call is blocking up to the configured wait timeout. For a single-message dequeue, the call returns as soon as one message is available, or it times out if no message arrives. For a batch dequeue, the call requests up to the configured batch size and returns when the requested number of entries has been retrieved or when the wait timeout expires. That means a batch call may return a full batch, a partial batch, or no data if the queue stays empty until timeout.

Requesting one entry at a time keeps per-message latency low and makes error handling simple, because each dequeue result maps directly to one handler-chain invocation. It also commits progress frequently. The tradeoff is that it performs more database round trips when the queue is busy, so throughput can be lower.

Requesting multiple entries in one dequeue can improve throughput by reducing database round trips and allowing a batch reader to drain bursts of queued messages more efficiently. The tradeoff is that an early message in the batch may wait until the batch fills or the timeout fires, and processing/commit behavior is grouped around the batch read. Batch reads are usually better for sustained or bursty load; single-message reads are usually better when immediate processing and simpler operational behavior matter more than maximum throughput.

This gives the application two useful modes. Direct JDBC access is table/query oriented. AQ access is message/stream oriented and is better suited to continuous processing, filtering, transformation, and output handling.

### Selecting filters, processors, and outputs

Raw and normalized messages are handled by separate chains:

- `RawDataMessageHandlerService` receives a Micronaut-injected `List<RawDataMessageHandler>`.
- `NormalizedDataMessageHandlerService` receives a Micronaut-injected `List<NormalizedDataMessageHandler>`.

As with input clients, only handlers whose `@Requires` properties match the runtime configuration are created and injected. Each handler provides an order value from configuration, and the service sorts the injected handlers before processing messages. A handler returns an array of messages. Returning one or more messages passes those messages to the next handler in the chain; returning an empty array stops that branch. This allows the same mechanism to support filters, test processors, and final output handlers.

The raw data handler chain can include these active filters:

| Filter class | Configuration prefix | What it checks |
| --- | --- | --- |
| `RawDataContentTypeMessageFilter` | `messagehandler.filter.rawdata.contenttype.*` | The raw message content type. The configured `type` is compared ignoring case. |
| `RawDataEndpointMessageFilter` | `messagehandler.filter.rawdata.endpointfilter.*` | A regular expression against the raw message endpoint. `findoutcome=FOUND` keeps matching endpoints; `findoutcome=NOT_FOUND` keeps endpoints that do not match. |
| `RawDataDeviceModelsMessageFilter` | `messagehandler.filter.rawdata.devicemodelsfilter.*` | The device model display name for the raw message's digital twin instance. It accepts one or more configured `modelnames[...]` entries. |

It can also include outputs such as:

- text output using `messagehandler.output.rawdata.textoutput.*`
- HTTP output enabled with `messagehandler.output.rawdata.httpclient.enabled`; the current code reads its order and type from `messagehandler.output.rawdata.httpclient.enabled.order` and `messagehandler.output.rawdata.httpclient.enabled.type`
- NoSQL output settings under `messagehandler.output.rawdata.nosql.*`; the current code enables this bean with `messagehandler.filter.rawdata.nosql.enabled`

The normalized data handler chain can include these active filters and test processors:

| Filter or processor class | Configuration prefix | What it checks or does |
| --- | --- | --- |
| `NormalizedDataContentJsonTypeMessageFilter` | `messagehandler.filter.normalizeddata.contentjsontypefilter.*` | The Oracle JSON type of the normalized `value` payload, using `NormalizedData.getContentJsonType()`. `findoutcome=FOUND` keeps listed JSON types; `findoutcome=NOT_FOUND` keeps messages whose JSON type is not listed. |
| `NormalizedDataContentPathsMessageFilter` | `messagehandler.filter.normalizeddata.contentpathsfilter.*` | Exact content path membership using configured `matchingcontentpath[...]` entries. It supports optional case-insensitive comparison and `findoutcome=FOUND` or `NOT_FOUND`. |
| `NormalizedDataDeviceModelsMessageFilter` | `messagehandler.filter.normalizeddata.devicemodelsfilter.*` | The device model display name for the normalized message's digital twin instance. It accepts one or more configured `modelnames[...]` entries. |
| `NormalizedDataRandomMessageFilter` | `messagehandler.filter.normalizeddata.randomfilter.*` | Test-only random filtering. |
| `NormalizedDataDuplicatorMessageProcessor` | `messagehandler.processor.normalizeddata.duplicator.*` | Test-only message duplication before later handlers run. |

It can also include the diagnostic text output:

- `messagehandler.output.normalizeddata.textoutput.*`

The usual pattern is to set a handler's `.enabled` property to `true` and provide its `.order` property. Lower order values run earlier. Filters normally sit before outputs, and outputs can either pass the message through to later handlers or terminate that branch by returning no messages.

The current plural device-model filters replace the older single-model filter path. `RawDataDeviceModelMessageFilter`, `NormalizedDataDeviceModelMessageFilter`, and `NormalizedDataContentPathRegexpMessageFilter` are still present in the source, but their `@Requires` annotations use `.enabled.IGNORETHISONE`, so the normal `.enabled=true` property will not create those beans. Use the plural `devicemodelsfilter` entries and the exact-match `contentpathsfilter` entries described above.

Device-model filters use `DeviceModelMessageFilterCore` and `DeviceModelInstancesCache`. The cache opens a database connection, switches to `iotdatacache.schemaname`, and reads `digital_twin_models` and `digital_twin_instances` to map instance ids, external keys, model ids, and model display names. It keeps in-memory maps of successful lookups and sets of known missing ids so repeated messages for the same instance do not repeatedly query the database.

The cache needs these properties:

| Property | Required | Purpose |
| --- | --- | --- |
| `iotdatacache.schemaname` | Yes | IoT data-cache schema, normally `<IOT Domain Shortid>__iot`. The cache uses this as the current database schema. |
| `devicemodelinstancescache.preloadexistingmodels` | No, defaults to `true` | Preload all model ids and display names at handler configuration time. |
| `devicemodelinstancescache.preloadexistinginstances` | No, defaults to `true` | Preload all instance ids, external keys, and model ids at handler configuration time. |
| `datasources.default.url` | Yes for the current DB connection supplier | JDBC URL used to connect to the IoT database. The sample builds it from `iotdatacache.ociregion` and `iotdatacache.connectionname`. |
| `oci.dbtoken.scope` | Yes when using the access-token DB connection supplier | Scope used by `DBTokenRetriever` when creating the database access token. |
| `oci.auth.type` | Yes when using the access-token DB connection supplier | Selects the OCI auth provider. The current values are `InstancePrinciple` or `ConfigFile`. |
| `oci.auth.config.section` | No, defaults to `DEFAULT` | OCI config profile section when `oci.auth.type=ConfigFile`. |

Raw device-model filter example:

```properties
messagehandler.filter.rawdata.devicemodelsfilter.enabled=true
messagehandler.filter.rawdata.devicemodelsfilter.order=10
messagehandler.filter.rawdata.devicemodelsfilter.modelnames[0]=homeBattery
messagehandler.filter.rawdata.devicemodelsfilter.modelnames[1]=homeHeating
messagehandler.filter.rawdata.devicemodelsfilter.caseinsensitive=true

iotdatacache.schemaname=<IOT Domain Shortid>__iot
devicemodelinstancescache.preloadexistingmodels=true
devicemodelinstancescache.preloadexistinginstances=true
```

Normalized JSON value-type filter example:

```properties
messagehandler.filter.normalizeddata.contentjsontypefilter.enabled=true
messagehandler.filter.normalizeddata.contentjsontypefilter.order=10
messagehandler.filter.normalizeddata.contentjsontypefilter.contentype[0]=DECIMAL
messagehandler.filter.normalizeddata.contentjsontypefilter.contentype[1]=DOUBLE
messagehandler.filter.normalizeddata.contentjsontypefilter.findoutcome=FOUND
```

The `contentype` spelling in that example is deliberate: it is the property name the current constructor reads. The filter uses `findoutcome`; older sample comments that mention `filteronmatches` do not match the current code.

Example:

```properties
iotdatacache.aq.rawdata.individualreader.enabled=true
iotdatacache.aq.rawdata.individualreader.order=10
iotdatacache.aq.rawdata.individualreader.readtimeout=10

messagehandler.filter.rawdata.contenttype.enabled=true
messagehandler.filter.rawdata.contenttype.order=10
messagehandler.filter.rawdata.contenttype.type=application/json

messagehandler.output.rawdata.textoutput.enabled=true
messagehandler.output.rawdata.textoutput.order=20
messagehandler.output.rawdata.textoutput.passthrough=false
```

In this example, Micronaut creates the raw AQ reader, the content-type filter, and the text output handler. The reader receives raw AQ messages, the raw handler service runs the content-type filter first, and matching messages are then passed to the text output handler.

## Micronaut 4.10.10 Documentation

- [User Guide](https://docs.micronaut.io/4.10.10/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.10/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.10/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
## Feature serialization-jackson documentation


- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


## Feature lombok documentation


- [Micronaut Project Lombok documentation](https://docs.micronaut.io/latest/guide/index.html#lombok)


- [https://projectlombok.org/features/all](https://projectlombok.org/features/all)


## Feature data-jdbc documentation


- [Micronaut Data JDBC documentation](https://micronaut-projects.github.io/micronaut-data/latest/guide/index.html#jdbc)


## Feature jdbc-hikari documentation


- [Micronaut Hikari JDBC Connection Pool documentation](https://micronaut-projects.github.io/micronaut-sql/latest/guide/index.html#jdbc)


## Feature jul-to-slf4j documentation


- [https://www.slf4j.org/legacy.html#jul-to-slf4jBridge](https://www.slf4j.org/legacy.html#jul-to-slf4jBridge)


## Feature maven-enforcer-plugin documentation


- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)


## Feature micronaut-aot documentation


- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)
