#FROM alpine:3.9
FROM selenium/standalone-chrome

ENV TH2_HAND_DIR="th2-hand"\
	GRPC_PORT=8080\
	RH_URL="http://localhost:8008"

WORKDIR /home
ADD th2-hand.tar .
ADD remotehand.zip ./remotehand/
RUN cd ./remotehand && sudo unzip remotehand.zip && sudo rm remotehand.zip && cd ..
ADD log4j.properties ./remotehand/
ADD config.ini ./remotehand/
ADD formParser.properties ./remotehand/
CMD ["sh","-c","cd /home/remotehand/ && java -jar remotehand.jar -httpserver & /home/th2-hand/bin/th2-hand"]