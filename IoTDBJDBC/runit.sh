#!/bin/bash
java -Dmicronaut.config.files=configsecure/configsecure.yml,config/config.yml -jar target/IoTDBJDBC-*.jar 