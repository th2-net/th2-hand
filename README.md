# th2 hand (4.0.0)

th2-hand is used to interpret and transmit commands from th2-act to Selenium or Windows Application Driver and vice versa.
All incoming and outgoing data is stored in Cradle as messages.

## Requirements

* JDK 8+ (OpenJDK 11 is recommended)
* Gradle (Optional)
* Docker

## Build

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

## Configuration

This project uses the Schema API to get its settings.
For local run it needs `custom.json`, `grpc.json`, `rabbitMQ.json` and `mq.json` files.

The `custom.json` file contains RemoteHand URLs map and has the following format:
- **session-alias == "th2-hand" by default**
```
{
    "session-alias": "aliasName",
	"driversMapping": {
			"first" : {
				"type" : "web",
				"url" : "http://localhost:4444"
			},
			"second" : {
				"type" : "windows",
				"url" : "http://localhost:4445"
			}
		},
	"rhOptions": {
		"Browser" : "Chrome"
	}
}
```

th2-hand gRPC server port to run is configured in `grpc.json`:
```
{
	"server":{
		"port" : 8080
	}
}
```

The `mq.json` file must have two configured queues. 

The first queue configured in Message storage is for raw messages. It must have "publish" and "raw" mandatory attributes. 

The second queue configured in Message storage is for parsed messages. It must have "publish" and "parsed" mandatory attributes.

Example of `mq.json`:
```
{
	"queues": {
		"send_raw" : {
			"name": "default_general_decode_in",
			"queue": "send_raw_queue",
			"exchange": "default_general_exchange",
			"attributes": ["raw", "publish"]
		}
	}
}
```

The `rabbitMQ.json` file must contain settings to connect to RabbitMQ.

Example of `rabbitMQ.json`:
```
{
	"host": "mq_host",
	"vHost": "mq_vhost",
	"port": 32600,
	"username": "mq_username",
	"password": "mq_pass"
}
```

## Release Notes

### 4.0.0

+ Migrated to Books & Pages concept

### 3.9.0

+ added 'SelectFrame' web action

### 3.8.1

+ fixed SwitchActiveWindow action

### 3.1.0

+ reads dictionaries from the /var/th2/config/dictionary folder
+ uses mq_router, grpc_router, cradle_manager optional JSON configs from the /var/th2/config folder
+ tries to load log4j.properties files from sources in order: '/var/th2/config', '/home/etc', configured path via cmd, default configuration
+ update Cradle version. Introduce async API for storing events
+ removed gRPC event loop handling
+ fixed dictionary reading