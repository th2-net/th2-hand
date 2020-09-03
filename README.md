# TH2-HAND

TH2-HAND is used to transmit and interpret messages from TH2-ACT to Remotehand (selenium agent) and vice versa.

### Requirements

* JDK 8+ (OpenJDK 12 recommended)
* Gradle (Optional)
* Docker

### Build

This project is built by gradle.
You cat use gralde wrapper to build
``` shell script
./gradlew build
```
Or install gradle tool into your system.

To build docker image use Dockerfile.
eg
``` shell script
docker build -t th2-hand:1.1 -f Dockerfile .
``` 

### Configuration

This project uses environment variables as its settings

ENV VAR NAME | DEFAULT VALUE | DESCRIPTION
------------ | ------------- | -----------
RABBITMQ_EXCHANGE | | RabbitMq Exchange name setting
RABBITMQ_ROUTINGKEY | | Queue configured in Message storage as one for parsed messages
RABBITMQ_RAW_ROUTINGKEY | | Queue configured in Message storage as one for raw messages
RABBITMQ_HOST | | RabbitMq host setting
RABBITMQ_PORT | |RabbitMq port setting
RABBITMQ_VHOST | | RabbitMq Virtual Host setting
RABBITMQ_USER | | RabbitMq username
RABBITMQ_PASS | | RabbitMq password
RH_URL | http://localhost:8008 | Remotehand URL
GRPC_PORT | 8080 | TH2 GRPC Server port