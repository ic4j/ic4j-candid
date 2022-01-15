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

package org.ic4j.candid.parser;

import java.util.ArrayList;
import java.util.List;

import org.ic4j.candid.IDLBuilder;
import org.ic4j.candid.IDLDeserialize;

public final class IDLArgs {
	List<IDLValue> args;

	IDLArgs(List<IDLValue> args) {
		this.args = args;
	}

	public static IDLArgs create(List<IDLValue> args) {
		return new IDLArgs(args);
	}

	public byte[] toBytes() {
		IDLBuilder idl = new IDLBuilder();

		for (IDLValue arg : args) {
			idl.valueArg(arg);

		}

		return idl.serializeToVec();

	}
	
	public static IDLArgs fromBytes(byte[] bytes)
	{
		IDLDeserialize de = IDLDeserialize.create(bytes);
		
		List<IDLValue> args = new ArrayList<IDLValue>();
		
		while(!de.isDone())
		{
			IDLValue value = de.getValue(IDLValue.class);
			args.add(value);
		}
		
		de.done();	
		
		return new IDLArgs(args);			
	}
	
	public static IDLArgs fromBytes(byte[] bytes, IDLType[] idlTypes)
	{
		if(idlTypes == null)
			return fromBytes(bytes);
		
		IDLDeserialize de = IDLDeserialize.create(bytes);
		
		List<IDLValue> args = new ArrayList<IDLValue>();
		
		for(int i = 0; i < idlTypes.length; i++)
		{
			de.setExpectedType(idlTypes[i]);
			
			IDLValue value = de.getValue(IDLValue.class);
			args.add(value);
		}
		
		de.done();	
		
		return new IDLArgs(args);			
	}
	
	public List<IDLValue> getArgs()
	{
		return this.args;
	}
}
