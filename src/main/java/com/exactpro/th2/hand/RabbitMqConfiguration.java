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

package com.exactpro.th2.hand;

import org.apache.commons.lang3.ObjectUtils;

public class RabbitMqConfiguration {
	
	public static final String EXCHANGE_NAME_KEY = "RABBITMQ_EXCHANGE";
	public static final String ROUTING_KEY_KEY = "RABBITMQ_ROUTINGKEY";
	public static final String RABBIT_MQ_HOST_KEY = "RABBITMQ_HOST";
	public static final String RABBIT_MQ_PORT_KEY = "RABBITMQ_PORT";
	public static final String RABBIT_MQ_VIRTUALHOST_KEY = "RABBITMQ_VHOST";
	public static final String RABBIT_MQ_USERNAME_KEY = "RABBITMQ_USER";
	public static final String RABBIT_MQ_PASSWORD_KEY = "RABBITMQ_PASS";

	public static final String EXCHANGE_NAME_DEFAULT_VALUE = "";
	public static final String ROUTING_KEY_DEFAULT_VALUE = "";
	public static final String RABBIT_MQ_HOST_DEFAULT_VALUE = "";
	public static final int RABBIT_MQ_PORT_DEFAULT_VALUE = 0;
	public static final String RABBIT_MQ_VIRTUALHOST_DEFAULT_VALUE = "";
	public static final String RABBIT_MQ_USERNAME_DEFAULT_VALUE = "";
	public static final String RABBIT_MQ_PASSWORD_DEFAULT_VALUE = "";
	

	protected final String exchangeName;
	protected final String routingKey;
	protected final String rabbitMqHost;
	protected final int rabbitMqPort;
	protected final String rabbitMqVirtualHost;
	protected final String rabbitMqUsername;
	protected final String rabbitMqPassword;
	
	
	public RabbitMqConfiguration() {
		this.exchangeName = ObjectUtils.defaultIfNull(System.getenv(EXCHANGE_NAME_KEY), EXCHANGE_NAME_DEFAULT_VALUE);
		this.routingKey = ObjectUtils.defaultIfNull(System.getenv(ROUTING_KEY_KEY), ROUTING_KEY_DEFAULT_VALUE);
		this.rabbitMqHost = ObjectUtils.defaultIfNull(System.getenv(RABBIT_MQ_HOST_KEY), RABBIT_MQ_HOST_DEFAULT_VALUE);
		this.rabbitMqPort = this.getIntDefaultIfNull(System.getenv(RABBIT_MQ_PORT_KEY), RABBIT_MQ_PORT_DEFAULT_VALUE);
		this.rabbitMqVirtualHost = ObjectUtils.defaultIfNull(System.getenv(RABBIT_MQ_VIRTUALHOST_KEY), RABBIT_MQ_VIRTUALHOST_DEFAULT_VALUE);
		this.rabbitMqUsername = ObjectUtils.defaultIfNull(System.getenv(RABBIT_MQ_USERNAME_KEY), RABBIT_MQ_USERNAME_DEFAULT_VALUE);
		this.rabbitMqPassword = ObjectUtils.defaultIfNull(System.getenv(RABBIT_MQ_PASSWORD_KEY), RABBIT_MQ_PASSWORD_DEFAULT_VALUE);
	}

	protected int getIntDefaultIfNull(String value, int defaultValue) {
		try {
			String getenv = System.getenv(RABBIT_MQ_PORT_KEY);
			return Integer.parseInt(getenv);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public String getExchangeName() {
		return exchangeName;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public String getRabbitMqHost() {
		return rabbitMqHost;
	}

	public int getRabbitMqPort() {
		return rabbitMqPort;
	}

	public String getRabbitMqVirtualHost() {
		return rabbitMqVirtualHost;
	}

	public String getRabbitMqUsername() {
		return rabbitMqUsername;
	}

	public String getRabbitMqPassword() {
		return rabbitMqPassword;
	}
}
