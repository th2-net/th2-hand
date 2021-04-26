/*
 *  Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.remotehand.sessions.SessionExchange;

import java.io.File;
import java.io.IOException;

public class HandSessionExchange implements SessionExchange {
	private static final HandSessionExchange stub = new HandSessionExchange();


	public static HandSessionExchange getStub() {
		return stub;
	}

	@Override
	public void sendResponse(int code, String message) throws IOException {

	}

	@Override
	public void sendFile(int code, File file, String type, String filename) throws IOException {

	}

	@Override
	public String getRemoteAddress() {
		return null;
	}
}
