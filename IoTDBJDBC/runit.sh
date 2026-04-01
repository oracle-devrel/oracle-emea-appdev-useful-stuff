#!/bin/bash
java -Dmicronaut.config.files=config/configsecure.yml,config/config.yml -jar target/IoTDBJDBC-*.jar 