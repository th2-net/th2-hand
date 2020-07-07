/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactpro.th2.hand.services;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.IHandService;
import com.exactpro.th2.hand.grpc.BaseRequest;
import com.exactpro.th2.hand.grpc.BaseResponse;
import com.exactpro.th2.hand.grpc.HandBaseGrpc.HandBaseImplBase;
import com.exactpro.th2.hand.grpc.RhInfo;
import com.exactprosystems.clearth.connectivity.data.rhdata.RhResponseCode;
import com.exactprosystems.clearth.connectivity.data.rhdata.RhScriptResult;
import com.exactprosystems.clearth.connectivity.remotehand.RhClient;
import com.exactprosystems.clearth.connectivity.remotehand.RhException;
import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

public class HandBaseService extends HandBaseImplBase implements IHandService
{
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	private Config config;
	private RhClient rhConnection;

	@Override
	public void getRhInfo(Empty request, StreamObserver<RhInfo> responseObserver) {
		logger.info("Action: '{}'", "getRhInfo");
		RhInfo response = RhInfo.newBuilder().setSessionId(rhConnection.getSessionId())
				.setUserBrowser(rhConnection.getUsedBrowser()).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

    @Override
    public void runScript(BaseRequest request, StreamObserver<BaseResponse> responseObserver) {
        String scriptFile = config.getScriptsDir().resolve(request.getScriptName()).toString();
        Map<String, String> params = request.getParamsMap();
        int waitInSeconds = request.getWaitInSeconds();

        logger.info("Action: '{}', Script name: '{}', Parameters:'{}'", "runScript", request.getScriptName(), params);
        
        RhScriptResult scriptResult = new RhScriptResult();
        String errMsg = "";
        try {
            scriptResult = rhConnection.executeScript(scriptFile, params, waitInSeconds == 0 ? 10 : waitInSeconds);
        } 
        catch (RhException e ) {
            scriptResult.setCode(RhResponseCode.UNKNOWN.getCode());
            errMsg = "Error occured while fetching data from Remotehand";
            logger.warn(errMsg, e);
        } 
        catch (IOException e) {
            scriptResult.setCode(RhResponseCode.UNKNOWN.getCode());
            errMsg = String.format("Error occured while reading script file '%s'", scriptFile);
            logger.warn(errMsg, scriptFile, e);
        }
        
        BaseResponse response = BaseResponse.newBuilder()
                .setScriptResult(RhResponseCode.byCode(scriptResult.getCode()).toString())
                .setErrorMessage(isEmpty(errMsg) ? defaultIfEmpty(scriptResult.getErrorMessage(), "") : errMsg)
                .setSessionId(rhConnection.getSessionId())
                .addAllTextOut(scriptResult.getTextOutput())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
	public void init(Config config, RhClient rhConnection) {
		this.config = config;
		this.rhConnection = rhConnection;
	}
}
