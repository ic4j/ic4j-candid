/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.candid.types;

import org.ic4j.candid.CandidError;

public enum Mode {
	QUERY(1),ONEWAY(2);
	
	public int value;

	Mode(int value) {
		this.value = value;
	}
	
	public static Mode from(Integer value)
	{
		switch (value) {
		case 1:
			return QUERY;
		case 2:
			return ONEWAY;
		default:
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, String.format("Unknown method mode %d", value));
		}			
	}
}