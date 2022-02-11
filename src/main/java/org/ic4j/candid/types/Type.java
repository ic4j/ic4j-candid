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

import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.CandidError;

public enum Type {
	NULL, BOOL, NAT, INT, NAT8, NAT16, NAT32, NAT64, INT8, INT16, INT32, INT64, FLOAT32, FLOAT64, TEXT, RESERVED, EMPTY,
	OPT, VEC, RECORD, VARIANT, FUNC, SERVICE, PRINCIPAL;

	
	public boolean isPrimitive()
	{
		Type[] primitives = {NULL,BOOL,NAT,INT,NAT8,NAT16,NAT32,NAT64,INT8,INT16,INT32,INT64,FLOAT32,FLOAT64,TEXT,RESERVED,EMPTY,PRINCIPAL};
		
		return ArrayUtils.contains(primitives, this);
	}
	
	public static Type from(Integer value) {
		switch (value) {
		case -1:
			return NULL;
		case -2:
			return BOOL;
		case -3:
			return NAT;
		case -4:
			return INT;
		case -5:
			return NAT8;
		case -6:
			return NAT16;
		case -7:
			return NAT32;
		case -8:
			return NAT64;			
		case -9:
			return INT8;
		case -10:
			return INT16;
		case -11:
			return INT32;
		case -12:
			return INT64;
		case -13:
			return FLOAT32;
		case -14:
			return FLOAT64;
		case -15:
			return TEXT;
		case -16:
			return RESERVED;
		case -17:
			return EMPTY;
		case -18:
			return OPT;	
		case -19:
			return VEC;			
		case -20:
			return RECORD;
		case -21:
			return VARIANT;	
		case -22:
			return FUNC;	
		case -23:
			return SERVICE;	
		case -24:
			return PRINCIPAL;				
		default:
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, String.format("Unknown type %d", value));
		}
	}

}
