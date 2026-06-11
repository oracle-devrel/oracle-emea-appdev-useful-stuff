## IoT Sonnen Uploader

This project reads data from a Sonnen battery and uploads it to Oracle IoT by using Micronaut scheduled jobs, HTTP clients, and MQTT publishers/subscribers. The same codebase can also capture Sonnen data to files and replay captured data later.

## Micronaut dependency injection

Most optional behavior is controlled by Micronaut dependency injection. Classes that represent optional inputs, uploaders, MQTT helpers, capture jobs, and replay jobs are annotated with `@Singleton`, `@MqttPublisher`, or `@MqttSubscriber`, and are guarded with `@Requires` annotations. If the required configuration properties are present and enabled, Micronaut creates the bean. If the properties are missing or disabled, the bean is not created, so its scheduled methods or MQTT subscriptions are not active.

This means the application is assembled from configuration rather than from a central runtime switch. For example, setting `iotservicehttps.statusupload.enabled=true` creates the HTTPS status uploader; setting it to `false` or omitting it prevents that uploader from existing.

## Input options

The live Sonnen battery input is provided by `SonnenBatteryClient`. This Micronaut HTTP client is enabled when `sonnenbattery.authToken` is present and uses the named HTTP service `sonnenbattery`.

Required live battery settings:

```properties
micronaut.http.services.sonnenbattery.url=http://<sonnen-ip-address>:80
sonnenbattery.authToken=<sonnen-api-token>
```

Live data is pulled by the enabled scheduled jobs. The HTTPS uploaders, MQTT uploaders, and data capture jobs all inject `SonnenBatteryClient` and call either `/api/v2/status` or `/api/v2/configurations`.

The saved-data replay input is provided by the `sonnendatareplayer` package. Replay does not read from the Sonnen battery. It reads captured JSON-line files created by the capture package, rewrites timestamps, and publishes the replayed entries through the MQTT publisher.

The MQTT command input is provided by `MqttCommandHandler`. It subscribes to `house/sonnencommand/${device.id}` when MQTT is enabled, command handling is enabled, and the MQTT client settings are present.

## Upload targets

### HTTPS upload

The `uploaderhttps` package sends live Sonnen readings directly to the IoT service over HTTPS.

| Bean | Enables | Required enable property | Main schedule properties |
| --- | --- | --- | --- |
| `StatusUploaderHttps` | Live status upload | `iotservicehttps.statusupload.enabled=true` | `iotservicehttps.statusupload.frequency`, `iotservicehttps.statusupload.initialdelay` |
| `ConfigurationUploaderHttps` | Live configuration upload | `iotservicehttps.configurationupload.enabled=true` | `iotservicehttps.configurationupload.frequency`, `iotservicehttps.configurationupload.initialdelay` |

Both HTTPS uploaders rely on:

- `SonnenBatteryClient`, so `sonnenbattery.authToken` and `micronaut.http.services.sonnenbattery.url` must be configured.
- `IoTServiceClientHttps`, so `device.id`, `iotservicehttps.username`, `iotservicehttps.password`, and `micronaut.http.services.iotservicehttps.url` must be configured.
- `iotservicehttps.statusupload.sendtype` or `iotservicehttps.configurationupload.sendtype`, with `PLAIN` or `JSON`.

The IoT service password is expected to be base64 encoded in configuration; `IoTServiceRequestFilter` decodes it before adding HTTP Basic Authentication.

Example:

```yaml
device:
  id: <device-id>

iotservicehttps:
  statusupload:
    enabled: true
    frequency: "10s"
    initialdelay: "10s"
    sendtype: JSON
  configurationupload:
    enabled: true
    frequency: "120s"
    initialdelay: "5s"
    sendtype: JSON
  username: <digital-twin-external-key>
  password: <base64-device-secret>

micronaut:
  http:
    services:
      iotservicehttps:
        url: https://<iot-device-host>
```

### MQTT upload

The `uploadermqtt` package publishes live Sonnen readings to MQTT topics.

| Bean | Enables | Required enable property | Main schedule properties |
| --- | --- | --- | --- |
| `StatusUploaderMqtt` | Live status publication to MQTT | `mqtt.statusupload.enabled=true` | `mqtt.statusupload.frequency`, `mqtt.statusupload.initialdelay` |
| `ConfigurationUploaderMqtt` | Live configuration publication to MQTT | `mqtt.configurationupload.enabled=true` | `mqtt.configurationupload.frequency`, `mqtt.configurationupload.initialdelay` |

Both MQTT uploaders rely on:

- `SonnenBatteryClient`, so the Sonnen URL and auth token must be configured.
- `MqttSonnenBatteryPublisher`, so `device.id`, `mqtt.client.client-id`, `mqtt.client.user-name`, `mqtt.client.password`, and `mqtt.client.server-uri` must be configured. `mqtt.enabled` defaults to `true` in the publisher, but setting `mqtt.enabled=false` disables MQTT publishers and subscribers.

The publisher sends to:

- `house/sonnenstatus/${device.id}`
- `house/sonnenconfiguration/${device.id}`

Optional MQTT helper beans are also controlled by configuration:

- `mqtt.commandhandler.enabled=true` enables command subscription on `house/sonnencommand/${device.id}` and response publication on `house/sonnencommandresponse/${device.id}`.
- `mqtt.monitorreference.enabled=true` logs raw MQTT messages for reference.
- `mqtt.monitoruploads.enabled=true` logs deserialized upload messages.

Example:

```yaml
mqtt:
  enabled: true
  broker:
    host: <broker-host>
    port: 8883
    protocol: ssl
  client:
    client-id: ${device.id}
    server-uri: ${mqtt.broker.protocol}://${mqtt.broker.host}:${mqtt.broker.port}
    user-name: <digital-twin-external-key>
    password: <device-secret>
  statusupload:
    enabled: true
    frequency: "10s"
  configurationupload:
    enabled: true
    frequency: "120s"
```

## Capture and replay

### Capturing Sonnen data

The `sonnendatacapture` package captures live Sonnen status and configuration data to local files. Capture is useful for recording real battery behavior and replaying it later without needing the battery to be available.

Capture has three cooperating beans:

- `CaptureTimerController` controls the capture window and is enabled by `datacapture.enabled=true`.
- `StatusDataCapture` writes status records when `datacapture.statussave.enabled=true`.
- `ConfigurationDataCapture` writes configuration records when `datacapture.configurationsave.enabled=true`.

All capture jobs require `datacapture.starttimestamp` and `datacapture.captureduration`. In the current code, `CaptureTimerController` injects both `StatusDataCapture` and `ConfigurationDataCapture`, so a normal timed capture run should enable both `datacapture.statussave.enabled` and `datacapture.configurationsave.enabled` along with `datacapture.enabled`. The timer waits until the start timestamp, writes a capture metadata header to the output files, enables the status/configuration writers, then stops the application after the configured duration.

Capture also relies on the live Sonnen input settings:

```properties
micronaut.http.services.sonnenbattery.url=http://<sonnen-ip-address>:80
sonnenbattery.authToken=<sonnen-api-token>
```

Example:

```yaml
datacapture:
  enabled: true
  starttimestamp: "2026-01-01T17:00:00.000000Z[Europe/London]"
  captureduration: "1d"
  configurationsave:
    enabled: true
    frequency: "120s"
    filename: "./saveddata/configuration.json"
    overwrite: true
  statussave:
    enabled: true
    frequency: "10s"
    filename: "./saveddata/status.json"
    overwrite: true
```

### Replaying captured data

The `sonnendatareplayer` package reads captured files and replays status or configuration records through MQTT. Replay is enabled by `datareplay.enabled=true`, then separately for status and configuration:

- `datareplay.statusreplay.enabled=true`
- `datareplay.configurationreplay.enabled=true`

Replay reads the filenames configured under the capture properties:

- `datacapture.statussave.filename`
- `datacapture.configurationsave.filename`

The first line of each file is the capture metadata written by `DataCaptureConfig`. The replay beans use that metadata to preserve the relative timing of the original capture while adjusting timestamps for the replay run.

Important replay controls:

- `datareplay.replayrate`: `1` means real-time replay, larger values speed up replay, and `0` schedules each next record as soon as possible.
- `datareplay.uploadtimestarts.now`: start rewritten upload timestamps from now.
- `datareplay.uploadtimestarts.offsettofinishnow`: shift the replay window so the selected data finishes at the current time.
- `datareplay.uploadtimestarts.relative` and `datareplay.uploadtimestart.relativeoffset`: apply an offset to the chosen upload start.
- `datareplay.startafterinputtimestamp.*`: skip records before a chosen point in the captured input.
- `datareplay.stopafterinputtimestamp.*`: stop replay after a chosen point in the captured input.
- `datareplay.statusreplay.actuallyupload` and `datareplay.configurationreplay.actuallyupload`: if `false`, the replay logs what it would publish but does not send to MQTT.

Because replay publishes through `MqttSonnenBatteryPublisher`, MQTT client settings must be configured even if live MQTT uploaders are disabled. To replay without the live MQTT polling jobs, leave `mqtt.statusupload.enabled=false` and `mqtt.configurationupload.enabled=false` while enabling `datareplay.*`.

Example:

```yaml
datareplay:
  enabled: true
  replayrate: 10
  uploadtimestarts:
    now: true
    offsettofinishnow: false
    relative: false
  startafterinputtimestamp:
    enabled: false
  stopafterinputtimestamp:
    enabled: false
  statusreplay:
    enabled: true
    actuallyupload: true
  configurationreplay:
    enabled: false
    actuallyupload: true
```

## OCISetup shell scripts

The `OCISetup` directory contains helper scripts for creating, testing, and cleaning up the OCI IoT resources used by the demo. Most scripts expect to be run from the `DigitalTwin` directory and source `../OCISetup/common_names.sh`. Review `common_names.sh` first, because it defines the compartment path, IoT domain group/domain names, vault and secret names, digital twin model/adapter names, VM/VCN names, and bastion settings.

These scripts assume the OCI CLI is configured, `jq` is available, and the current user has permissions to create or modify the referenced OCI resources.

| Script/file | Purpose |
| --- | --- |
| `common_names.sh` | Shared names and paths used by the setup scripts. Edit this before running the scripts. |
| `get_oci_compartment_ocid.sh` | Resolves a compartment path such as `/projects/iot` to an OCID. |
| `CreateConfig.sh` | Creates or locates the IoT domain group, IoT domain, digital twin model, adapter, and primary digital twin instance; prints test `curl` and `mqttx` commands. |
| `DeleteConfig.sh` | Deletes the IoT domain and domain group and schedules deletion of generated secrets matching the configured prefix. |
| `CreateAdditionalDigitalTwinInstance.sh` | Creates or locates an additional digital twin instance using the existing model/adapter and prints test commands for it. |
| `ConfigureAPEXAccess.sh` | Enables APEX data access for the IoT domain and prints APEX workspace/schema details plus useful SQL queries. |
| `ConfigureIOTForDirectDBAccess.sh` | Configures direct database access for an allowed VCN and dynamic group, then prints DB token and IoTDBJDBC connection settings. |
| `ConfigureORDSIDCSApplication.sh` | Creates or locates an IDCS integrated application for ORDS/OAuth database access and grants user/group access. |
| `DeleteORDSIDCSApplication.sh` | Deactivates and deletes the configured ORDS IDCS application. |
| `SetupDBClientVCNandVM.sh` | Creates a VCN, subnet, gateways, security list, and Oracle Linux VM for direct DB client access. |
| `DestroyDBClientVMandVCN.sh` | Terminates the DB client VM and removes the VCN resources created by the setup script. |
| `CreateBastionSession.sh` | Starts the target VM if needed, waits for the Bastion plugin, creates a managed SSH bastion session, and prints the SSH command. |
| `SQLDBAccessClientSetupVM.txt` | Manual notes for installing Java, Maven, Git, and SQLcl on the DB client VM and using OCI DB tokens. |
| `IoTCacheDBConnect.txt` | Example JDBC/SQL*Net connection descriptor for token-authenticated IoT cache database access. |

Be careful with the destructive scripts: `DeleteConfig.sh`, `DeleteORDSIDCSApplication.sh`, and `DestroyDBClientVMandVCN.sh` remove cloud resources or schedule secret deletion.

## Micronaut 4.10.1 Documentation

- [User Guide](https://docs.micronaut.io/4.10.1/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.1/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.1/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
## Feature maven-enforcer-plugin documentation

- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)


## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


## Feature test-resources documentation

- [Micronaut Test Resources documentation](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/)


## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#nettyHttpClient)


## Feature mqtt documentation

- [Micronaut MQTT v5 Messaging documentation](https://micronaut-projects.github.io/micronaut-mqtt/latest/guide/index.html)
