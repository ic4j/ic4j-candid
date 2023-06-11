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

import java.util.List;

import org.ic4j.candid.CandidError;

public enum Opcode {
	NULL(-1), BOOL(-2), NAT(-3), INT(-4), NAT8(-5), NAT16(-6), NAT32(-7), NAT64(-8), INT8(-9), INT16(-10), INT32(-11), INT64(-12), FLOAT32(-13), FLOAT64(-14),
	TEXT(-15), RESERVED(-16), EMPTY(-17), OPT(-18), VEC(-19),RECORD(-20), VARIANT(-21), FUNC(-22), SERVICE(-23), PRINCIPAL(-24);

	public int value;
	
	public List<Integer> args;
	public List<Integer> rets;
	public List<Mode> modes;
	public List<Meths> meths;
	

	Opcode(int value) {
		this.value = value;
	}
	

	public static Opcode from(Integer value) {
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
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, String.format("Unknown opcode %d", value));
		}
	}
	
	public static Opcode func(List<Integer> args, List<Integer> rets, List<Mode> modes)
	{
		Opcode opcode = FUNC;
		
		opcode.args = args;
		opcode.rets = rets;
		opcode.modes = modes;
		
		return opcode;
		
	}
	
	public static Opcode service( List<Meths> meths)
	{
		Opcode opcode = SERVICE;
		
		opcode.meths = meths;
		
		return opcode;
		
	}	
	
}
