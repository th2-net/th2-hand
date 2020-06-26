/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactpro.th2.hand.services;

import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.IHandService;
import com.exactpro.th2.hand.grpc.HandBaseGrpc;
import com.exactpro.th2.hand.grpc.RhInfo;
import com.exactprosystems.clearth.connectivity.remotehand.RhClient;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class HandBaseService extends HandBaseGrpc.HandBaseImplBase implements IHandService
{
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	private Config config;
	private RhClient rhConnection;

	@Override
	public void getRhInfo(Empty request, StreamObserver<RhInfo> responseObserver)
	{
		logger.info("Action: '{}'", "getRhInfo");
		RhClient connection = rhConnection;
		RhInfo response = RhInfo.newBuilder().setSessionId(connection.getSessionId())
				.setUserBrowser(connection.getUsedBrowser()).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void init(Config config, RhClient rhConnection)
	{
		this.config = config;
		this.rhConnection = rhConnection;
	}
}
