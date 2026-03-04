#IotGatewayDemoClient

This client code simplifies the test of sending test data to the IotGatewayDemo.

You will need to specify the miconaut config file location when running by adding `-Dmicronaut.config.files=config/config.yml` to the arguments when running the Application class.

In the config file specify the hostname (or IP address) of the machine running the IotDemoGateway, and the port it's running on.

Also specify the names of the compartment that the iot domain group and iot domain are in, along with the vault used to hold the secrets containing the instance credentials. You will also need to specify the iot domain group name and the iot domain name. These must have already been created.

You will also need the TimGUtils package (v 1.2.2 at a minimum) as that handles various IO functions, this is not currently held in the central Maven repo, but the source can be accessed from https://github.com/atimgraves/timg-utilities

Note that if you chose to use the JSON or XML upload options that these are using the IoT data structure specified in the IotDemoGateway test model, adapters and envelope. If you have not set those up then the IoT service will be unable to parse the data. 

Once running the application the options are self explanatory (I hope). When being asked for input in some cases a default value is given, you can just press return to use it. If n default is given suitable input must be provided.


## Micronaut 4.10.8 Documentation

- [User Guide](https://docs.micronaut.io/4.10.8/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.8/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.8/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#nettyHttpClient)


## Feature jackson-xml documentation

- [Micronaut Jackson XML serialization/deserialization documentation](https://micronaut-projects.github.io/micronaut-jackson-xml/latest/guide/index.html)

- [https://github.com/FasterXML/jackson-dataformat-xml](https://github.com/FasterXML/jackson-dataformat-xml)


## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


## Feature maven-enforcer-plugin documentation

- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)


