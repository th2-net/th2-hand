/*
 *  Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.th2.hand.services;

import com.exactpro.th2.hand.RabbitMqConfiguration;
import com.exactpro.th2.infra.grpc.Message;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMqConnectionWrapper implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(RabbitMqConnectionWrapper.class);
	
	private final Channel channel;
	private final Connection connection;
	
	private final String exchangeName;
	private final String routingKey;

	public RabbitMqConnectionWrapper(RabbitMqConfiguration configuration) throws Exception {
		this.exchangeName = configuration.getExchangeName();
		this.routingKey = configuration.getRoutingKey();
		
		ConnectionFactory connectionFactory = this.buildFactory(configuration);
		this.connection = connectionFactory.newConnection();
		this.channel = this.connection.createChannel();
		this.channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT);
		
		logger.debug("MQ channel was opened");
	}
	
	private ConnectionFactory buildFactory(RabbitMqConfiguration configuration) {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(configuration.getRabbitMqHost());
		factory.setPort(configuration.getRabbitMqPort());
		factory.setVirtualHost(configuration.getRabbitMqVirtualHost());
		factory.setUsername(configuration.getRabbitMqUsername());
		factory.setPassword(configuration.getRabbitMqPassword());
		return factory;
	}
	
	public void sendMessage(Message message) throws Exception {
		byte[] bytes = message.toByteArray();
		channel.basicPublish(exchangeName, routingKey, null, bytes);
		logger.debug("Array with size {} bytes was sent to {} {}", bytes.length, exchangeName, routingKey);
	}

	@Override
	public void close() throws Exception {
		Exception exception = null;
		try {
			this.channel.close();
		} catch (Exception e) {
			exception = e;
			logger.error("Cannot close RabbitMQ channel", e);
		}
		try {
			this.connection.close();
		} catch (Exception e) {
			if (exception == null) {
				exception = e;
			}
			logger.error("Cannot close RabbitMQ connection", e);
		}
		if (exception != null) {
			throw exception;
		}
		logger.debug("MQ channel was closed");
	}
}
