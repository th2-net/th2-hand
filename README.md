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

This project uses the schema API to get its settings.
For local run it needs `custom.json`, `grpc.jsom,` `rabbitMQ.json` and `mq.json` files.

The `custom.json` file contains RemoteHand URLs map and has next format:
```
{
	"rhUrls":{
		"first" : "http://localhost:8008",
		"second" : "http://localhost:8008"
		}
}
```
The TH2-Hand gRPC server port to run is configured in `grpc.json`
```
{
    "server":{
            "port" : 8080
            }
}
```
The `mq.json` file must have two configured queues. 

The first queue configured in Message storage as one for raw messages. It must nave two "publish", "raw" mandatory atributes. 

The second queue configured in Message storage as one for parsed messages. It must nave two "publish", "parsed" mandatory atributes.

Like this:
```
{
     "queues": {
       "send_raw" : {
         "name": "default_general_decode_in",
         "queue": "send_raw_queue",
         "exchange": "default_general_exchange",
         "attributes": ["raw", "publish"]
       },
       "send_parsed" : {
         "name" : "default_general_decode_in",
         "queue": "send_parsed_queue",
         "exchange": "default_general_exchange",
         "attributes": ["parsed", "publish"]
       }
     }
}
```

The `rabbitMQ.json` must contain settings to connect to Rabbitmq exchange.

Like this:
```
{
  "host": "mq_host",
  "vHost": "mq_vhost",
  "port": 32600,
  "username": "mq_username",
  "password": "mq_pass"
}
```