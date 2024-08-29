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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.IC4JLogging;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Numbers;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;

public final class ValueSerializer implements Serializer{
	static final IC4JLogging LOG = IC4JLogging.getIC4JLogger(ValueSerializer.class);

	byte[] value;
	
	ValueSerializer()
	{
		this.value = ArrayUtils.EMPTY_BYTE_ARRAY;
	}
	
	public void serializeNull() {		
	}	
	
	public final void serializeBool(Boolean value)
    {   
		this.value = ArrayUtils.add(this.value, (byte)(value?1:0));
    }	
	
	public final void serializeText(String value)
    {
		byte[] stringBytes = value.getBytes();
		
    	byte[] leb128 = Leb128.writeUnsigned(stringBytes.length);
    	
    	this.value = ArrayUtils.addAll(this.value,leb128);
    	
    	this.value = ArrayUtils.addAll(this.value,stringBytes); 	
    }
	
	public final void serializeNat(BigInteger value)
    { 
		if(value.compareTo(BigInteger.ZERO) < 0)
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, String.format("Invalid unsigned value %d", value));
		
		this.value = ArrayUtils.addAll(this.value, Numbers.encodeBigNat(value));
    }	
	
	public final void serializeNat8(Byte value)
    {   		
		ByteBuffer output = ByteBuffer.allocate(Byte.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.put(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }	
	
	public final void serializeNat16(Short value)
    { 
		ByteBuffer output = ByteBuffer.allocate(Short.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putShort(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	
	public final void serializeNat32(Integer value)
    { 		
		ByteBuffer output = ByteBuffer.allocate(Integer.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putInt(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	public final void serializeNat64(Long value)
    { 		
		ByteBuffer output = ByteBuffer.allocate(Long.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putLong(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	public final void serializeNat8(Short value)
    {   		
		ByteBuffer output = ByteBuffer.allocate(Short.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putShort(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	public final void serializeNat16(Integer value)
    { 
		ByteBuffer output = ByteBuffer.allocate(Integer.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putInt(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	
	public final void serializeNat32(Long value)
    { 		
		ByteBuffer output = ByteBuffer.allocate(Long.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putLong(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	public final void serializeNat64(BigInteger value)
    { 		
		this.serializeNat(value);
    }	
	
	public final void serializeInt(BigInteger value)
    {   		
		this.value = ArrayUtils.addAll(this.value, Numbers.encodeBigInt(value));
    }
	
	public final void serializeFloat64(Double value)
    {   
		ByteBuffer output = ByteBuffer.allocate(Double.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putDouble(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }		
	
	public final void serializeFloat32(Float value)
    {   
		ByteBuffer output = ByteBuffer.allocate(Float.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putFloat(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	public final void serializeInt8(Byte value)
    {   
		ByteBuffer output = ByteBuffer.allocate(Byte.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.put(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	public final void serializeInt16(Short value)
    {   
		ByteBuffer output = ByteBuffer.allocate(Short.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putShort(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	
	public final void serializeInt32(Integer value)
    {   
		ByteBuffer output = ByteBuffer.allocate(Integer.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putInt(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	public final void serializeInt64(Long value)
    {   
		ByteBuffer output = ByteBuffer.allocate(Long.BYTES);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.putLong(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }	
	
	public final void serializeOpt(Optional<?> value, IDLType idlType) {
		if(value.isPresent())
		{
			byte[] leb128 = Leb128.writeUnsigned(1);
	    	
	    	this.value = ArrayUtils.addAll(this.value,leb128);		
	    	
	    	Object obj = value.get();
	    	
    		IDLValue idlValue;
    		if(idlType == null)
    			idlValue = IDLValue.create(obj);
    		else
    			idlValue = IDLValue.create(obj, idlType.getInnerType());
	    	
	    	idlValue.idlSerialize(this);
		}
		else
		{
			byte[] leb128 = Leb128.writeUnsigned(0);
	    	
	    	this.value = ArrayUtils.addAll(this.value,leb128);
		}
		
	}

	public final <T> void serializeVec(T[] value, IDLType idlType) {
		byte[] leb128 = Leb128.writeUnsigned(value.length);
    	
    	this.value = ArrayUtils.addAll(this.value,leb128);
    	
    	for(Object element : value)
    	{
    		IDLValue idlValue;
    		if(idlType == null)
    			idlValue = IDLValue.create(element);
    		else
    			idlValue = IDLValue.create(element, idlType.getInnerType());
    		
    		idlValue.idlSerialize(this);
    	}
		
	}

	public final void serializePrincipal(Principal value) {
		this.value = ArrayUtils.addAll(this.value,(byte)1);
		
		byte[] leb128 = Leb128.writeUnsigned(value.getValue().length);
    	
    	this.value = ArrayUtils.addAll(this.value,leb128);
    	
    	this.value = ArrayUtils.addAll(this.value,value.getValue());
		
	}
	
	public final void serializeFunc(Func value) {
		this.value = ArrayUtils.addAll(this.value,(byte)1);
		this.value = ArrayUtils.addAll(this.value,(byte)1);
		
		if(value.getPrincipal() != null)
		{
			byte[] leb128 = Leb128.writeUnsigned(value.getPrincipal().getValue().length);
	    	
	    	this.value = ArrayUtils.addAll(this.value,leb128);
	    	
	    	this.value = ArrayUtils.addAll(this.value,value.getPrincipal().getValue());
		}
		else
		{
			byte[] leb128 = Leb128.writeUnsigned(0);
			this.value = ArrayUtils.addAll(this.value,leb128);
		}
    	
		if(value.getMethod() != null)
		{
			byte[] stringBytes = value.getMethod().getBytes();
			
	    	byte[] leb128 = Leb128.writeUnsigned(stringBytes.length);
	    	
	    	this.value = ArrayUtils.addAll(this.value,leb128);
	    	
	    	this.value = ArrayUtils.addAll(this.value,stringBytes); 
		}
		else
		{
			byte[] leb128 = Leb128.writeUnsigned(0);
			this.value = ArrayUtils.addAll(this.value,leb128);
		}		
	}
	
	public final void serializeService(Service value) {
		this.value = ArrayUtils.addAll(this.value,(byte)1);
		
		if(value.getPrincipal() != null)
		{
			byte[] leb128 = Leb128.writeUnsigned(value.getPrincipal().getValue().length);
	    	
	    	this.value = ArrayUtils.addAll(this.value,leb128);
	    	
	    	this.value = ArrayUtils.addAll(this.value,value.getPrincipal().getValue());
		}
		else
		{
			byte[] leb128 = Leb128.writeUnsigned(0);
			this.value = ArrayUtils.addAll(this.value,leb128);
		}
	}	
	
	public final void serializeBinary(byte[] value, IDLType idlType)
    {   
		byte[] leb128 = Leb128.writeUnsigned(value.length);
    	
    	this.value = ArrayUtils.addAll(this.value,leb128);
    	
		if(value == null || value.length == 0)
			return;
		
		ByteBuffer output = ByteBuffer.allocate(Byte.BYTES*value.length);
		output.order(ByteOrder.LITTLE_ENDIAN);
	    output.put(value);
	    
		this.value = ArrayUtils.addAll(this.value, output.array());
    }
	
	public final void serializeBinary(Byte[] value, IDLType idlType)
    {   		
		this.serializeBinary(ArrayUtils.toPrimitive(value), idlType);
    }

	@Override
	public void serializeRecord(Object value, IDLType idlType) {
		if(value instanceof Map)
			for(Label label : ((Map<Label,Object>) value).keySet())
			{		    			    	
				Object element = ((Map<Label,Object>) value).get(label);				
				
				IDLType nestedType = null;
				
				if(idlType != null)
					nestedType = idlType.getTypeMap().get(label);
				
				this.serializeElement(element, nestedType);	
			}
		
	}

	@Override
	public void serializeVariant(Object value, IDLType idlType) {
		int idx = 0;
		
		if(value instanceof Map)
			if(!((Map<Label,Object>) value).isEmpty())
			{
				byte[] leb128 = Leb128.writeUnsigned(idx);
		    	
		    	this.value = ArrayUtils.addAll(this.value,leb128);
		    	
		    	Label label = ((Map<Label,Object>)value).keySet().iterator().next();		    	
		    	
				Object element = ((Map<Label,Object>) value).get(label);				
				
				IDLType nestedType = null;
				
				if(idlType != null)
					nestedType = idlType.getTypeMap().get(label);
				
				this.serializeElement(element, nestedType);	
			}
	}
	
	byte[] getResult()
	{
		return this.value;
	}
	
	void serializeElement(Object value) {
		IDLValue idlValue = IDLValue.create(value);
		idlValue.idlSerialize(this);
	}
	
	void serializeElement(Object value, IDLType idlType) {
		IDLValue idlValue;
		
		if(idlType != null)
			idlValue = IDLValue.create(value, idlType);
		else
			idlValue = IDLValue.create(value);
		
		idlValue.idlSerialize(this);
	}

}
