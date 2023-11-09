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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.Deserialize;
import org.ic4j.candid.Deserializer;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.ObjectDeserializer;
import org.ic4j.candid.ObjectSerializer;
import org.ic4j.candid.Serializer;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;

public final class IDLValue implements Deserialize{
	Optional<?> value;
	IDLType idlType;
	
	public static IDLValue create(Object value, Type type)
	{
		IDLValue idlValue = new IDLValue();
		
		idlValue.idlType = IDLType.createType(value, type);
			
		idlValue.value = Optional.ofNullable(value);
		
		return idlValue;
	}	
	
	public static IDLValue create(Object value, IDLType idlType)
	{
		IDLValue idlValue = new IDLValue();
		
		idlValue.idlType = idlType;
			
		idlValue.value = Optional.ofNullable(value);
		
		return idlValue;
	}	
    
	public static IDLValue create(Object value, Map<Label,IDLType> typeMap)
	{
		IDLValue idlValue = new IDLValue();
		
		idlValue.value = Optional.ofNullable(value);
		
		idlValue.idlType = IDLType.createType(value);

		idlValue.idlType.typeMap = typeMap;
		
		return idlValue;
	}
	
	public static IDLValue create(Object value)
	{
		IDLValue idlValue = new IDLValue();
		
		idlValue.value = Optional.ofNullable(value);
		
		idlValue.idlType = IDLType.createType(value);	
		
		return idlValue;
	}	
	
	public static IDLValue create(Object value, ObjectSerializer objectSerializer)
	{	
		return objectSerializer.serialize(value);
	}
	
	public static IDLValue create(Object value, ObjectSerializer objectSerializer, IDLType idlType)
	{	
		IDLValue idlValue = objectSerializer.serialize(value);
		
		if((idlType.type == Type.VEC || idlType.type == Type.OPT) && idlType.getInnerType() == null)
			idlValue.idlType.type = idlType.type;
		else if((idlType.type == Type.RECORD || idlType.type == Type.VARIANT) && idlType.typeMap.isEmpty())
			idlValue.idlType.type = idlType.type;
		else
			idlValue.idlType = idlType;
		return idlValue;
	}
	
	public static IDLValue create(Object value, ObjectSerializer objectSerializer, Type type)
	{	
		IDLType idlType = IDLType.createType(type);
		
		return IDLValue.create(value, objectSerializer, idlType);
	}	
	
	public void idlSerialize(Serializer serializer)
	{
		if(!value.isPresent())
		{
			serializer.serializeNull();
			return;
		}
		
		switch(this.idlType.type)
		{
		case NULL:
			serializer.serializeNull();
			break;		
		case BOOL:
			serializer.serializeBool((Boolean) value.get());
			break;
		case NAT:
			serializer.serializeNat((BigInteger) value.get());
			break;
		case NAT8:			
			serializer.serializeNat8((Byte) value.get());
			break;
		case NAT16:
			serializer.serializeNat16((Short) value.get());
			break;
		case NAT32:
			serializer.serializeNat32((Integer)value.get());
			break;
		case NAT64:	
			if(value.isPresent() && value.get() instanceof BigInteger)
			{
				Long longValue = ((BigInteger)value.get()).longValue();
				serializer.serializeNat64((Long) longValue);
			}
			else
				serializer.serializeNat64((Long) value.get());
			break;			
		case INT:
			serializer.serializeInt((BigInteger) IDLUtils.objectToBigInt(value.get()));			
			break;
		case INT8:
			serializer.serializeInt8((Byte) value.get());
			break;	
		case INT16:
			serializer.serializeInt16((Short) value.get());
			break;
		case INT32:
			serializer.serializeInt32((Integer) value.get());
			break;
		case INT64:
			if(value.isPresent() && value.get() instanceof BigInteger)
			{
				Long longValue = ((BigInteger)value.get()).longValue();
				serializer.serializeInt64((Long) longValue);
			}
			else
			serializer.serializeInt64((Long) value.get());
			break;			
		case FLOAT32:
			if(value.isPresent() && value.get() instanceof Double)
			{
				Float floatValue = ((Double)value.get()).floatValue();
				serializer.serializeFloat32((Float) floatValue);
			}
			else
				serializer.serializeFloat32((Float) value.get());	
			break;
		case FLOAT64:
			if(value.isPresent() && value.get() instanceof BigDecimal)
			{
				Double doubleValue = ((BigDecimal)value.get()).doubleValue();
				serializer.serializeFloat64((Double) doubleValue);
			} 
			else if(value.isPresent() && value.get() instanceof Float)
			{
				Double doubleValue = ((Float)value.get()).doubleValue();
				serializer.serializeFloat64((Double) doubleValue);
			}
			else			
				serializer.serializeFloat64((Double) value.get());
			break;			
		case TEXT:
			serializer.serializeText((String) value.get());
			break;	
		case OPT:
			serializer.serializeOpt((Optional) value.get(), this.idlType);
			break;
		case VEC:
			if(value.isPresent() && value.get() instanceof byte[])
				serializer.serializeBinary((byte[])value.get(), this.idlType);	
			else if(value.isPresent() && value.get() instanceof Byte[])
				serializer.serializeBinary((Byte[])value.get(), this.idlType);			
			else
				serializer.serializeVec((Object[])value.get(), this.idlType);
			break;
		case RECORD:
			serializer.serializeRecord(value.get(), this.idlType);
			break;	
		case VARIANT:
			serializer.serializeVariant(value.get(), this.idlType);
			break;
		case PRINCIPAL:
			serializer.serializePrincipal((Principal) IDLUtils.objectToPrincipal(value.get()));
			break;	
		case FUNC:
			serializer.serializeFunc((Func) value.get());
			break;	
		case SERVICE:
			serializer.serializeService((Service) value.get());
			break;			
		}

	}
	
	public IDLType getIDLType()
	{
		return this.idlType;	
	}	
	
	public Type getType()
	{
		return this.idlType.type;	
	}

	public <T> T getValue()
	{
		if(this.value.isPresent())
		{
			T value = (T) this.value.get();
			return value;
		}
		else
			return null;
	}
	
	public <T> T getValue(IDLType expectedIdlType)
	{
		this.idlType =expectedIdlType;
		
		if(this.value.isPresent())
		{
			T value = (T) this.value.get();
			return value;
		}
		else
			return null;
	}
	
	public <T> T getValue(ObjectDeserializer objectDeserializer, Class<T> clazz)
	{
		return (T) objectDeserializer.deserialize(this, clazz);
	}
	
	public <T> T getValue(ObjectDeserializer objectDeserializer, Class<T> clazz,  IDLType expectedIdlType)
	{
		this.idlType =expectedIdlType;
		return (T) objectDeserializer.deserialize(this, clazz);
	}	
	
	public void deserialize(Deserializer deserializer) {
		IDLValue idlValue = deserializer.deserializeAny();	
		
		this.idlType = idlValue.idlType;
		this.value = idlValue.value;
	}	

}
