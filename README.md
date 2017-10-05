# MLR-Gateway
[![Build Status](https://travis-ci.org/USGS-CIDA/MLR-Gateway.svg?branch=master)](https://travis-ci.org/USGS-CIDA/MLR-Gateway) [![Coverage Status](https://coveralls.io/repos/github/USGS-CIDA/MLR-Gateway/badge.svg?branch=master)](https://coveralls.io/github/USGS-CIDA/MLR-Gateway?branch=master)

Monitoring Location Gateway

## Running the Application
Copy the src/main/resources/application.yml file to you project root directory and change the substitution variables as needed.
Open a terminal window and navigate to the project's root directory.

Use the maven command ```mvn spring-boot:run``` to run the application.

It will be available at http://localhost:8080 in your browser.

Swagger API Documentation is available at http://localhost:8080/swagger-ui.html

ctrl-c will stop the application.

## Using Docker
To build the image you will need to provide the location of the jar within 
https://cida.usgs.gov/artifactory/mlr-maven/gov/usgs/wma/mlrgateway as follows:
``` 
% docker build --build-arg=0.1-SNAPSHOT/mlrgateway-0.1-SNAPSHOT.jar .
```

To run the image, you will need to provide as environment variables the substitution variables in the application.yml. The application
will be available on part 8080 within the container.

## Substitution Variables
* mlrServicePassword - password for the monitoring user (deprecated)
* ddotServers - comma separated list of url(s) for the D dot Ingester Microservice
* legacyTransformerServers - comma separated list of url(s) for the Legacy Transformer Microservice
* legacyValidatorServers - comma separated list of url(s) for the Legacy Validator Microservice
* legacyCruServers - comma separated list of url(s) for the Legacy CRU Microservice
* fileExportServers - comma separated list of url(s) for the File Export Microservice
* notificationServers - comma separated list of url(s) for the Notification Microservice
* ribbonMaxAutoRetries - maximum number of times to retry connecting to a microservice
* ribbonConnectTimeout - maximum milliseconds to wait for a connection to a microservice
* ribbonReadTimeout - maximum milliseconds to wait for a response from a microservice
* hystrixThreadTimeout - maximum milliseconds for a request to process
* springFrameworkLogLevel - log level for org.springframework
