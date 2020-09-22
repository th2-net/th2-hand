# TH2-Hand

TH2-Hand is used to interpret and transmit commands from TH2-Act to RemoteHand (Selenium agent) and vice versa.
All incoming and outgoing data is stored in Cradle as messages.

### Requirements

* JDK 8+ (OpenJDK 11 is recommended)
* Gradle (Optional)
* Docker

### Build

This project is built by Gradle.
You cat use Gradle wrapper to build it:
``` shell script
./gradlew build
```
To build a Docker image use Dockerfile, 
e.g.
``` shell script
docker build -t <image name>:<version> -f Dockerfile .
``` 

### Configuration

This project uses environment variables as its settings

ENV VAR NAME | DEFAULT VALUE | DESCRIPTION
------------ | ------------- | -----------
RABBITMQ_EXCHANGE | | RabbitMQ Exchange name setting
RABBITMQ_ROUTINGKEY | | Queue configured in Message storage as one for parsed messages
RABBITMQ_RAW_ROUTINGKEY | | Queue configured in Message storage as one for raw messages
RABBITMQ_HOST | | RabbitMQ host setting
RABBITMQ_PORT | |RabbitMQ port setting
RABBITMQ_VHOST | | RabbitMQ Virtual Host setting
RABBITMQ_USER | | RabbitMQ username
RABBITMQ_PASS | | RabbitMQ password
RH_URLS | first=http://localhost:8008;second=http://localhost:8009 | RemoteHand URLs map
GRPC_PORT | 8080 | TH2-Hand gRPC Server port to run on