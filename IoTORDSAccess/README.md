## IoT ORDS Access

`IoTORDSAccess` is a Micronaut HTTP client project for reading Oracle IoT data exposed through ORDS. It contains:

- IDCS OAuth client code in `com.oracle.demo.timg.iot.iotordsaccess.idcs`.
- Low-level ORDS client and query builder classes in `com.oracle.demo.timg.iot.iotordsaccess.ords`.
- Higher-level IoT access helpers in `com.oracle.demo.timg.iot.iotordsaccess.iotaccess`.
- Response DTOs in `com.oracle.demo.timg.iot.iotordsaccess.data`.

The project is designed so configuration controls which Micronaut beans are available. Credentials, domain details, ORDS client endpoints, and optional demo retrieval loops are enabled by configuration properties rather than hard-coded construction in application code.

## Configuration options

Configuration is split across normal and secure files:

- `sample-config/services.yml` and `config/services.yml` define the Micronaut HTTP client base URLs.
- `sample-config-secure/idcs-oauth.yml` and `config-secure/idcs-oauth.yml` hold the IDCS, OAuth, and IoT domain values. Do not commit real credentials.
- `config/runner.yml` enables or disables the scheduled test/demo retrieval beans.

Typical startup passes these files to Micronaut:

```bash
./mvnw mn:run -Dmicronaut.config.files=sample-config/services.yml,config-secure/idcs-oauth.yml,config/runner.yml
```

Important properties:

| Property | Used by | Purpose |
| --- | --- | --- |
| `micronaut.http.services.idcsoauthtoken.url` | `IDCSOAuthClient` | Base URL for the IDCS OAuth token endpoint. The sample builds it from `ords.idcs.hostname`. |
| `micronaut.http.services.ordsapi.url` | `ORDSApiClient` | Base URL for the IoT ORDS API. The sample builds it from `iot.domaingroup.id` and `iot.region`. |
| `ords.idcs.hostname` | `services.yml` | IDCS host prefix used for the token service URL. The code and service URL expect `hostname`. |
| `ords.idcs.username` | `IDCSUserCredentials` | IDCS user used in the OAuth password grant. |
| `ords.idcs.password` | `IDCSUserCredentials` | IDCS password used in the OAuth password grant. |
| `ords.oauth.appid` | `IDCSApplicationCredentials`, `IDCSOAuthTokenRequestFilter` | OAuth client/application id used for Basic authentication when requesting a token. |
| `ords.oauth.appsecret` | `IDCSApplicationCredentials`, `IDCSOAuthTokenRequestFilter` | OAuth client/application secret used for Basic authentication when requesting a token. |
| `iot.region` | `services.yml` | OCI region segment used in the ORDS API host name. |
| `iot.domaingroup.id` | `IOTDomainGroupDetails`, `services.yml` | IoT domain group id. Also forms part of the OAuth scope. |
| `iot.domain.id` | `IOTDomainDetails`, `ORDSApiClient` | IoT domain id. Also forms part of the OAuth scope and ORDS path. |
| `iot.ordsapi.version` | `ORDSApiClient` | ORDS API version segment in `/ords/${iot.domain.id}/${iot.ordsapi.version}`. |
| `iot.ords.test-rawdata-retrieval` | `ORDSTestRawDataRetrieval` | Enables the scheduled raw data demo bean when set to `true`. |
| `iot.ords.test-snapshotdata-retrieval` | `ORDSTestSnapshotDataRetrieval` | Enables the scheduled snapshot data demo bean when set to `true`. |
| `iot.ords.test-historizeddata-retrieval` | `ORDSTestHistorizedDataRetrieval` | Enables the scheduled historized data demo bean when set to `true`. |
| `iot.ords.test-data-retrieval` | `ORDSTestDataRetrieval` | Enables the higher-level `IoTDataAccessor` demo bean when set to `true`. |
| `iot.idcs.test-token-retrieval` | `IDCSTestTokenRetrieval` | Enables the scheduled token retrieval demo bean when set to `true`. |
| `iot.ords.applyrequestfilter` | `config/runner.yml` | Present in the runner config, but no Java class currently reads this property. ORDS authentication is wired by the request filter startup logic described below. |
| `iot.dti.id` | `ORDSTestDataRetrieval` | Digital twin instance id used by the higher-level demo. |
| `iot.dti.content-path` | `ORDSTestDataRetrieval` | Content path used by the higher-level demo. |

The tester classes are annotated with `@Requires`, so setting one of the `iot.ords.test-*` or `iot.idcs.test-token-retrieval` values to `true` causes Micronaut to create that scheduled bean. Leaving the value unset or `false` keeps that tester out of the application context.

## Data types

The ORDS API exposes three data views through `ORDSApiClient`:

- Raw data: `/rawData`, represented by `RawDataResponse` and `RawDataEntry`. This is the raw incoming event view. Entries include `id`, `digital_twin_instance_id`, `time_received`, `endpoint`, and `content_type`.
- Snapshot data: `/snapshotData`, represented by `SnapshotDataResponseString` and `SnapshotDataEntryString`. This is the latest known value view for a digital twin instance and content path. Entries include `digital_twin_instance_id`, `content_path`, `value`, and `time_observed`.
- Historized data: `/historizedData`, represented by `HistorizedDataResponseString` and `HistorizedDataEntryString`. This is the time series view. Entries include `id`, `digital_twin_instance_id`, `content_path`, `value`, and `time_observed`.

Each response DTO also carries ORDS paging metadata: `hasMore`, `limit`, `offset`, `count`, and `links`. The value fields in the snapshot and historized DTOs are strings; the `iotaccess` package provides helpers that convert them into typed timestamp/value pairs.

## OAuth token retrieval

The token flow is built from Micronaut HTTP clients and request filters:

1. `IDCSOAuthClient` is a Micronaut client named `idcsoauthtoken`. It posts to `/oauth2/v1/token`.
2. `IDCSOAuthTokenRequestFilter` applies to `/oauth2/v1/token` and adds Basic authentication from `ords.oauth.appid` and `ords.oauth.appsecret`.
3. `IDCSOAuthApplicationTokenRetriever` uses `ords.idcs.username`, `ords.idcs.password`, `iot.domaingroup.id`, and `iot.domain.id` to request a password-grant token. The scope is built as `/{domainGroupId}/iot/{domainId}`.
4. The retriever caches the returned access token and renews it before expiry. The current code subtracts 60 seconds from the returned `expires_in` value to decide when to refresh.
5. `ORDSDataRequestFilter` applies to `/ords/**` requests and adds the cached token as a Bearer token before the ORDS request is sent.

`IDCSOAuthApplicationTokenRetriever` and the credential/config beans all use `@Requires`, so missing credential or domain properties prevent the token retriever from being created. The low-level `ORDSApiClient` is also guarded by `@Requires(property = "iot.domain.id")` and `@Requires(property = "iot.ordsapi.version")`.

The request filter currently receives the token retriever through `setIdcsoAuthApplicationTokenRequest(...)` during the `StartupEvent` in `IoTDataAccessor` and the tester classes. If you use `ORDSApiClient` directly from new code, make sure your code follows the same pattern or uses `IoTDataAccessor`, which already performs that wiring.

## Building ORDS queries

ORDS filtering is passed through the `q` query parameter as a JSON object string. The classes under `com.oracle.demo.timg.iot.iotordsaccess.ords.query` build that JSON string:

- `ValueEqualsString` creates a field equality test, for example `{"digital_twin_instance_id":"..."}`.
- `BooleanAnd` and `BooleanOr` group value tests under `$and` or `$or`.
- `OrderBy` adds a `$orderby` clause with an `OrderByDirection`.
- `Query` combines an optional boolean test and optional order by clause and returns the final `toQueryString()` value.

Example of building a query for the most recent values for a digital twin instance and content path:

```java
BooleanAnd filters = new BooleanAnd();
filters.addValueTest(ValueEqualsString.builder()
		.name("digital_twin_instance_id")
		.value(digitalTwinInstanceId)
		.build());
filters.addValueTest(ValueEqualsString.builder()
		.name("content_path")
		.value(contentPath)
		.build());

OrderBy orderBy = OrderBy.builder()
		.name("time_observed")
		.direction(OrderByDirection.DESC)
		.build();

String query = Query.builder()
		.booleanTest(filters)
		.orderBy(orderBy)
		.build()
		.toQueryString();

HistorizedDataResponseString data = ordsApiClient.getHistorizedData(query, 0, 10);
```

For common IoT queries, `IotQueryBuilder` in the `iotaccess` package wraps the same query classes:

- `buildSnapshotData(digitalTwinInstanceId)` returns all snapshot content paths for a digital twin instance, ordered by `content_path`.
- `buildSnapshotData(digitalTwinInstanceId, contentPath)` returns the snapshot value for one content path.
- `buildHistoryMostRecent(digitalTwinInstanceId, contentPath)` returns a query for history ordered by `time_observed` descending.

## Using the iotaccess package

`IoTDataAccessor` is the easiest entry point for application code. Inject it as a singleton dependency and call methods for either low-level response DTOs or typed timestamp/value wrappers:

```java
@Singleton
public class MyService {
	private final IoTDataAccessor accessor;

	public MyService(IoTDataAccessor accessor) {
		this.accessor = accessor;
	}

	public void readPower(String digitalTwinInstanceId) {
		List<String> contentPaths = accessor.getDigitalTwinInstanceContentPaths(digitalTwinInstanceId);
		TimestampDoubleValue currentPower = accessor.getDigitalTwinInstanceSnapshotAsTimestampDouble(
				digitalTwinInstanceId,
				"InverterPowerWattsPointInTime");
		List<TimestampDoubleValue> recentPower = accessor.getDigitalTwinInstanceHistoryAsTimestampDouble(
				digitalTwinInstanceId,
				"InverterPowerWattsPointInTime",
				10);
	}
}
```

The accessor provides:

- Snapshot methods returning `SnapshotDataResponseString` or one typed `TimestampStringValue`, `TimestampBooleanValue`, `TimestampDoubleValue`, or `TimestampLongValue`.
- History methods returning `HistorizedDataResponseString` or lists of typed timestamp/value wrappers.
- `getDigitalTwinInstanceContentPaths(...)`, which reads the snapshot data and returns the distinct content paths.

Numeric conversions are performed from the string `value` returned by ORDS. Snapshot numeric methods can throw `NumberFormatException`; history numeric methods skip entries that cannot be parsed.

## Using returned data

The DTOs in `com.oracle.demo.timg.iot.iotordsaccess.data` are annotated with `@Serdeable`, so Micronaut can deserialize ORDS JSON responses directly into Java objects.

When using `ORDSApiClient` directly, work from the response `items` list and check the paging fields if you requested a limited page:

```java
SnapshotDataResponseString snapshot = ordsApiClient.getSnapshotDataString(query, 0, 50);
for (SnapshotDataEntryString item : snapshot.getItems()) {
	String contentPath = item.getContent_path();
	String stringValue = item.getValue();
	ZonedDateTime observedAt = item.getTime_observed();
}

if (snapshot.isHasMore()) {
	// Request the next page using offset + limit.
}
```

When using `IoTDataAccessor`, most calling code can avoid the raw DTOs and consume the timestamp/value wrappers from the `iotaccess` package. Those wrappers keep the ORDS timestamp together with the converted value, which is usually the shape downstream code needs.

## Micronaut 4.10.3 Documentation

- [User Guide](https://docs.micronaut.io/4.10.3/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.3/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.3/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


## Feature maven-enforcer-plugin documentation

- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)


## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#nettyHttpClient)
