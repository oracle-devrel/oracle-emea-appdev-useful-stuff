#!/bin/bash
java -Dmicronaut.config.files=configsecure/configsecure.yml,config/config.yml -Ddatasources.default.enabled=false -jar target/IoTDBJDBC-*.jar 