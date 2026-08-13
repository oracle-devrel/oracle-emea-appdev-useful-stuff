#Sample commands

## normalized unauthenticated string
curl  http://localhost:8192/api/v1/iotdata/normalizeddata/unauthenticated/string/ocid/homepower/ts -d 'Im a chunk of unauthenticated normalized data' -H "content-type: text/plain"
## normalized auauthenticated string
curl -u 'un:pw' http://localhost:8192/api/v1/iotdata/normalizeddata/authenticated/string/ocid/homepower/ts -d 'Im a chunk of authenticated normalized data' -H "content-type: text/plain"
## raw unauthenticated string
curl  http://localhost:8192/api/v1/iotdata/rawdata/unauthenticated/string/ocid/homepower/ts -d 'Im a chunk of unauthenticated raw data' -H "content-type: text/plain"
## raw auauthenticated string
curl -u 'un:pw' http://localhost:8192/api/v1/iotdata/rawdata/authenticated/string/ocid/homepower/ts -d 'Im a chunk of authenticated raw data' -H "content-type: text/plain"

## normalized unauthenticated base 64 string
curl  http://localhost:8192/api/v1/iotdata/normalizeddata/unauthenticated/base64/ocid/homepower/ts -d 'SW0gYSBjaHVuayBvZiBiYXNlNjQgdW5hdXRoZW50aWNhdGVkIG5vcm1hbGl6ZWQgZGF0YQo=' -H "content-type: text/plain"
## normalized auauthenticated base 64 string
curl -u 'un:pw' http://localhost:8192/api/v1/iotdata/normalizeddata/authenticated/base64/ocid/homepower/ts -d 'SW0gYSBjaHVuayBvZiBiYXNlNjQgYXV0aGVudGljYXRlZCBub3JtYWxpemVkIGRhdGEK' -H "content-type: text/plain"
## raw unauthenticated base 64 string
curl  http://localhost:8192/api/v1/iotdata/rawdata/unauthenticated/base64/ocid/homepower/ts -d 'SW0gYSBjaHVuayBvZiBiYXNlNjQgdW5hdXRoZW50aWNhdGVkIHJhdyBkYXRhCg==' -H "content-type: text/plain"
## raw auauthenticated base 64 string
curl -u 'un:pw' http://localhost:8192/api/v1/iotdata/rawdata/authenticated/base64/ocid/homepower/ts -d 'SW0gYSBjaHVuayBvZiBiYXNlNjQgYXV0aGVudGljYXRlZCByYXcgZGF0YQo=' -H "content-type: text/plain"

## Micronaut 4.10.17 Documentation

- [User Guide](https://docs.micronaut.io/4.10.17/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.17/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.17/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
## Feature json-schema documentation


- [Micronaut JSON Schema documentation](https://micronaut-projects.github.io/micronaut-json-schema/latest/guide/)


- [https://json-schema.org/learn/getting-started-step-by-step](https://json-schema.org/learn/getting-started-step-by-step)


## Feature jul-to-slf4j documentation


- [https://www.slf4j.org/legacy.html#jul-to-slf4jBridge](https://www.slf4j.org/legacy.html#jul-to-slf4jBridge)


## Feature annotation-api documentation


- [https://jakarta.ee/specifications/annotations/](https://jakarta.ee/specifications/annotations/)


## Feature maven-enforcer-plugin documentation


- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)


## Feature micronaut-aot documentation


- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


## Feature lombok documentation


- [Micronaut Project Lombok documentation](https://docs.micronaut.io/latest/guide/index.html#lombok)


- [https://projectlombok.org/features/all](https://projectlombok.org/features/all)


