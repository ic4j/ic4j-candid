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

package org.ic4j.candid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Opcode;
import org.ic4j.candid.types.Type;

public final class TypeSerialize {
	Map<IDLType, Integer> typeMap;
	List<byte[]> typeTable;
	List<IDLType> args;

	byte[] result;

	TypeSerialize() {
		this.typeMap = new HashMap<IDLType, Integer>();
		this.typeTable = new ArrayList<byte[]>();
		this.args = new ArrayList<IDLType>();
		this.result = ArrayUtils.EMPTY_BYTE_ARRAY;
	}

	void pushType(IDLType type) {
		this.args.add(type);
		this.buildType(type);
	}

	void buildType(IDLType type) {
		if(type == null)
			type= IDLType.createType(Type.NULL);
		
		if (this.typeMap.containsKey(type))
			return;

		IDLType actualType = type;

		if (actualType.getType().isPrimitive())
			return;

		Integer idx = this.typeTable.size();
		
		byte[] buf = ArrayUtils.EMPTY_BYTE_ARRAY;		
		if(actualType.getType() == Type.SERVICE)
		{
			buf = ArrayUtils.addAll(buf, this.encodeServiceType(actualType, idx));
			idx = this.typeTable.size();
			this.typeMap.put(type, idx);
			this.typeTable.add(buf);
			return;
		}	

		this.typeTable.add(ArrayUtils.EMPTY_BYTE_ARRAY);

		switch (actualType.getType()) {
		case OPT:
			buf = ArrayUtils.addAll(buf, this.encodeComplexType(actualType));
			break;
		case VEC:
			buf = ArrayUtils.addAll(buf, this.encodeComplexType(actualType));
			break;
		case RECORD:
			buf = ArrayUtils.addAll(buf, this.encodeElementType(actualType));
			break;	
		case VARIANT:
			buf = ArrayUtils.addAll(buf, this.encodeElementType(actualType));
			break;
		case FUNC:			
			buf = ArrayUtils.addAll(buf, this.encodeFuncType(actualType));			
			break;	
		default:
			break;			
		}					

		this.typeMap.put(type, idx);
		
		this.typeTable.set(idx, buf);
	}
	
	byte[] encodeFuncType(IDLType idlType)
	{
		byte[] buf = ArrayUtils.EMPTY_BYTE_ARRAY;
		buf = ArrayUtils.addAll(buf, Leb128.writeSigned(Opcode.FUNC.value));
		
		List<IDLType> args = idlType.getArgs();
		List<IDLType> rets  = idlType.getRets();
		List<Mode> modes  = idlType.getModes();
		
		buf = ArrayUtils.addAll(buf, Leb128.writeUnsigned(args.size()));

		for(IDLType argType : args)	
			buf = ArrayUtils.addAll(buf, Leb128.writeSigned(argType.getType().intValue()));

		buf = ArrayUtils.addAll(buf, Leb128.writeUnsigned(rets.size()));

		for(IDLType retType : rets)	
			buf = ArrayUtils.addAll(buf, Leb128.writeSigned(retType.getType().intValue()));	
		
		buf = ArrayUtils.addAll(buf, Leb128.writeUnsigned(modes.size()));
		
		for(Mode mode : modes)	
			buf = ArrayUtils.add(buf, (byte) mode.value);
		
		return buf;
	}
	
	byte[] encodeServiceType(IDLType idlType, Integer idx)
	{
		byte[] buf = ArrayUtils.EMPTY_BYTE_ARRAY;
		
		buf = ArrayUtils.addAll(buf, Leb128.writeSigned(Opcode.SERVICE.value));
		
		Map<String, IDLType> meths = idlType.getMeths();
		
		buf = ArrayUtils.addAll(buf, Leb128.writeUnsigned(meths.size()));

		Iterator<String> names = meths.keySet().iterator();
		
		List<IDLTypeBuf> methTypes = new ArrayList<IDLTypeBuf>();
		while( names.hasNext())
		{
			String name  = names.next();
			if(name != null)
			{
				byte[] nameBytes = name.getBytes();
		
				IDLType methType = meths.get(name);
				
				if(methType.getType().isPrimitive())
				{
					buf = ArrayUtils.addAll(buf, Leb128.writeUnsigned(nameBytes.length));
					buf = ArrayUtils.addAll(buf, nameBytes);
					buf = ArrayUtils.addAll(buf, Leb128.writeSigned(methType.getType().intValue()));
				}
				else
				{	
					if(methType.getType() == Type.FUNC)
					{
						byte[] funcBuf = this.encodeFuncType(methType);
						
						IDLTypeBuf bufWrapper = new IDLTypeBuf(methType,funcBuf);
						
						int methIdx = methTypes.indexOf(bufWrapper);
						if(methIdx == -1)
						{
							methTypes.add(bufWrapper);
							
							methIdx = methTypes.size() - 1;
						}
						buf = ArrayUtils.addAll(buf, Leb128.writeUnsigned(nameBytes.length));
						buf = ArrayUtils.addAll(buf, nameBytes);
						buf = ArrayUtils.addAll(buf, Leb128.writeSigned(idx + methIdx));
					}					
				}
			}	
		}
		
		for(IDLTypeBuf funcBuf : methTypes)
		{
			this.typeMap.put(funcBuf.idlType, idx);
			this.typeTable.add(funcBuf.buf);
			idx++;
		}
		
		return buf;
	}
	
	byte[] encodeComplexType(IDLType idlType)
	{
		byte[] buf = ArrayUtils.EMPTY_BYTE_ARRAY;
		IDLType innerType =idlType.getInnerType();
		this.buildType(innerType);
		buf = ArrayUtils.addAll(buf, Leb128.writeSigned(idlType.getType().intValue()));
		buf = ArrayUtils.addAll(buf, this.encode(innerType));
		
		return buf;
	}
	
	byte[] encodeElementType(IDLType idlType)
	{
		byte[] buf = ArrayUtils.EMPTY_BYTE_ARRAY;
		Map<Label,IDLType> typeMap = idlType.getTypeMap();
		
		for(IDLType idlSubType : typeMap.values())
			this.buildType(idlSubType);
			
		buf = ArrayUtils.addAll(buf, Leb128.writeSigned(idlType.getType().intValue()));
		buf = ArrayUtils.addAll(buf, Leb128.writeUnsigned(typeMap.size()));
	
		for(Label label : typeMap.keySet())	
		{
			buf = ArrayUtils.addAll(buf, Leb128.writeUnsigned(label.getId()));
			
			IDLType idlSubType = typeMap.get(label);
			buf = ArrayUtils.addAll(buf, this.encode(idlSubType));			
		}
		
		return buf;
	}

	byte[] encode(IDLType idlType) {
		if(idlType == null)
			return Leb128.writeSigned(Opcode.NULL.value);
		
		Type type = idlType.getType();
		switch (type) {
		case NULL:
			return Leb128.writeSigned(Opcode.NULL.value);
		case BOOL:
			return Leb128.writeSigned(Opcode.BOOL.value);
		case NAT:
			return Leb128.writeSigned(Opcode.NAT.value);
		case INT:
			return Leb128.writeSigned(Opcode.INT.value);
		case NAT8:
			return Leb128.writeSigned(Opcode.NAT8.value);
		case NAT16:
			return Leb128.writeSigned(Opcode.NAT16.value);
		case NAT32:
			return Leb128.writeSigned(Opcode.NAT32.value);
		case NAT64:
			return Leb128.writeSigned(Opcode.NAT64.value);
		case INT8:
			return Leb128.writeSigned(Opcode.INT8.value);
		case INT16:
			return Leb128.writeSigned(Opcode.INT16.value);
		case INT32:
			return Leb128.writeSigned(Opcode.INT32.value);
		case INT64:
			return Leb128.writeSigned(Opcode.INT64.value);
		case FLOAT32:
			return Leb128.writeSigned(Opcode.FLOAT32.value);
		case FLOAT64:
			return Leb128.writeSigned(Opcode.FLOAT64.value);
		case RESERVED:
			return Leb128.writeSigned(Opcode.RESERVED.value);
		case TEXT:
			return Leb128.writeSigned(Opcode.TEXT.value);		
		case EMPTY:
			return Leb128.writeSigned(Opcode.EMPTY.value);
		case PRINCIPAL:
			return Leb128.writeSigned(Opcode.PRINCIPAL.value);			
		default:
			Integer idx = this.typeMap.getOrDefault(idlType, -1);
			if (idx != -1)
				return Leb128.writeSigned(idx);
			else
				throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, String.format("Type %s not found", type));
		}
	}

	void serialize() {
		this.result = ArrayUtils.addAll(this.result, Leb128.writeUnsigned(this.typeTable.size()));

		// TODO serialize content of type table
		for (byte[] type : this.typeTable)
			this.result = ArrayUtils.addAll(this.result, type);

		this.result = ArrayUtils.addAll(this.result, Leb128.writeUnsigned(this.args.size()));

		byte[] tyEncode = ArrayUtils.EMPTY_BYTE_ARRAY;

		for (IDLType idlType : args) {
			tyEncode = ArrayUtils.addAll(tyEncode, this.encode(idlType));
		}

		this.result = ArrayUtils.addAll(this.result, tyEncode);
	}

	byte[] getResult() {
		return this.result;
	}
	
	class IDLTypeBuf{
		byte[] buf;
		IDLType idlType;
		
		IDLTypeBuf(IDLType idlType,byte[] buf)
		{
			this.idlType = idlType;
			this.buf = buf;
		}
		
		@Override
		public boolean equals(Object value) {
			if(value == null)
				return false;
			
			if(!(value instanceof IDLTypeBuf))
				return false;
			
			return Arrays.equals(buf, ((IDLTypeBuf)value).buf);
		}
		
	}
}
