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

package org.ic4j.candid.jackson;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.ic4j.candid.CandidError;
import org.ic4j.candid.ObjectSerializer;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Principal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JacksonSerializer implements ObjectSerializer {
	Optional<IDLType> idlType = Optional.empty();
	
	ObjectMapper mapper = new ObjectMapper();

	public static JacksonSerializer create(IDLType idlType) {
		JacksonSerializer deserializer = new JacksonSerializer();
		deserializer.idlType = Optional.ofNullable(idlType);
		return deserializer;

	}

	public static JacksonSerializer create() {
		JacksonSerializer deserializer = new JacksonSerializer();
		return deserializer;
	}
	
	public void setIDLType(IDLType idlType)
	{
		this.idlType = Optional.ofNullable(idlType);
	}	

	@Override
	public IDLValue serialize(Object value) {
		if (value == null)
			return IDLValue.create(value);

		if (JsonNode.class.isAssignableFrom(value.getClass()))
			return this.getIDLValue(this.idlType, (JsonNode) value);
		else
		{
			try {
				JsonNode jsonNode = mapper.convertValue(value, JsonNode.class);
				return this.getIDLValue(this.idlType, jsonNode);				
			}catch (Exception e)
			{
				throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,e,
						e.getLocalizedMessage());
			}
		}	
	}

	IDLValue getPrimitiveIDLValue(Type type, JsonNode value) {
		IDLValue result = IDLValue.create(null);

		if (value == null)
			return result;

		switch (type) {
		case BOOL:
			result = IDLValue.create(value.booleanValue(), type);
			break;
		case INT:
			result = IDLValue.create(value.bigIntegerValue(), type);
			break;
		case INT8:
			result = IDLValue.create(Short.valueOf(value.shortValue()).byteValue(), type);
			break;
		case INT16:
			result = IDLValue.create(value.shortValue(), type);
			break;
		case INT32:
			result = IDLValue.create(value.intValue(), type);
			break;
		case INT64:
			result = IDLValue.create(value.longValue(), type);
			break;
		case NAT:
			result = IDLValue.create(value.bigIntegerValue(), type);
			break;
		case NAT8:
			result = IDLValue.create(Short.valueOf(value.shortValue()).byteValue(), type);
			break;
		case NAT16:
			result = IDLValue.create(value.shortValue(), type);
			break;
		case NAT32:
			result = IDLValue.create(value.intValue(), type);
			break;
		case NAT64:
			result = IDLValue.create(value.longValue(), type);
			break;
		case FLOAT32:
			result = IDLValue.create(value.floatValue(), type);
			break;
		case FLOAT64:
			result = IDLValue.create(value.doubleValue(), type);
			break;
		case TEXT:
			result = IDLValue.create(value.textValue(), type);
			break;
		case PRINCIPAL:
			result = IDLValue.create(Principal.fromString(value.textValue()));
			break;
		case EMPTY:
			result = IDLValue.create(null, type);
		case NULL:
			result = IDLValue.create(null, type);
			break;
		}

		return result;
	}

	Type getType(JsonNode value) {
		if (value == null)
			return Type.NULL;

		if (value.isBoolean())
			return Type.BOOL;
		else if (value.isShort())
			return Type.INT16;
		else if (value.isInt())
			return Type.INT32;
		else if (value.isLong())
			return Type.INT64;
		else if (value.isBigInteger())
			return Type.INT;
		else if (value.isFloat())
			return Type.FLOAT32;
		else if (value.isDouble())
			return Type.FLOAT64;
		else if (value.isTextual())
			return Type.TEXT;
		else if (value.isArray() || value.isBinary())
			return Type.VEC;
		else if (value.isObject() || value.isPojo())
			return Type.RECORD;
		else if (value.isEmpty())
			return Type.EMPTY;
		else if (value.isNull())
			return Type.NULL;
		else
			return Type.NULL;
	}

	IDLValue getIDLValue(Optional<IDLType> expectedIdlType, JsonNode value) {
		// handle null values
		if (value == null)
			return IDLValue.create(value, Type.NULL);

		Type type;
		if (expectedIdlType.isPresent())
			type = expectedIdlType.get().getType();
		else
			type = this.getType(value);
		
		if(type == Type.NULL || type == Type.EMPTY)
			return IDLValue.create(null, type);

		// handle primitives

		if (type.isPrimitive())
			return this.getPrimitiveIDLValue(type, value);

		// handle arrays
		if (type == Type.VEC) {
			IDLType innerIDLType = IDLType.createType(Type.NULL);

			if (expectedIdlType.isPresent())
				innerIDLType = expectedIdlType.get().getInnerType();

			if (value.isBinary()) {
				if (!expectedIdlType.isPresent())
					innerIDLType = IDLType.createType(Type.INT8);

				return IDLValue.create(value, IDLType.createType(type, innerIDLType));
			}

			if (value.isArray()) {
				ArrayNode arrayNode = (ArrayNode) value;
				Object[] arrayValue = new Object[arrayNode.size()];

				for (int i = 0; i < arrayNode.size(); i++) {
					IDLValue item = this.getIDLValue(Optional.ofNullable(innerIDLType), arrayNode.get(i));

					arrayValue[i] = item.getValue();
					if (!expectedIdlType.isPresent())
						innerIDLType = item.getIDLType();
				}

				IDLType idlType;

				if (expectedIdlType.isPresent())
					idlType = expectedIdlType.get();
				else
					idlType = IDLType.createType(Type.VEC, innerIDLType);

				return IDLValue.create(arrayValue, idlType);
			}

			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
					"Cannot convert class " + value.getClass().getName() + " to VEC");

		}

		// handle Objects
		if (type == Type.RECORD || type == Type.VARIANT) {			
			Map<Label, Object> valueMap = new TreeMap<Label, Object>();
			Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();
			Map<Label, IDLType> expectedTypeMap = new TreeMap<Label, IDLType>();

			if (expectedIdlType.isPresent())
				expectedTypeMap = expectedIdlType.get().getTypeMap();

			if(value.isArray())
			{
				ArrayNode arrayNode = (ArrayNode) value;
				for (int i = 0; i < arrayNode.size(); i++) {
					JsonNode item = arrayNode.get(i);
					IDLType expectedItemIdlType;
					
					if (expectedTypeMap != null && expectedIdlType.isPresent())
						expectedItemIdlType = expectedTypeMap.get(Label.createUnnamedLabel((long)i));
					else
						expectedItemIdlType = IDLType.createType(this.getType(item));
	
					if (expectedItemIdlType == null)
						continue;
	
					IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedItemIdlType), item);
	
					typeMap.put(Label.createUnnamedLabel((long)i), itemIdlValue.getIDLType());
					valueMap.put(Label.createUnnamedLabel((long)i), itemIdlValue.getValue());
				}								
			}			
			else
			{
				ObjectNode objectNode = (ObjectNode) value;
				Iterator<String> fieldNames = objectNode.fieldNames();
	
				while (fieldNames.hasNext()) {
					String name = fieldNames.next();
	
					JsonNode item = objectNode.get(name);
	
					IDLType expectedItemIdlType;
	
					if (expectedTypeMap != null && expectedIdlType.isPresent())
						expectedItemIdlType = expectedTypeMap.get(Label.createNamedLabel(name));
					else
						expectedItemIdlType = IDLType.createType(this.getType(item));
	
					if (expectedItemIdlType == null)
						continue;
	
					IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedItemIdlType), item);
	
					typeMap.put(Label.createNamedLabel((String) name), itemIdlValue.getIDLType());
					valueMap.put(Label.createNamedLabel((String) name), itemIdlValue.getValue());
				}
			}

			IDLType idlType = IDLType.createType(type, typeMap);
			IDLValue idlValue = IDLValue.create(valueMap, idlType);

			return idlValue;
		}
		
		if (type == Type.OPT)
		{
			if (expectedIdlType.isPresent())
			{
				if(value.isArray() && value.isEmpty())
					return IDLValue.create(Optional.empty(), expectedIdlType.get());
				
				IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedIdlType.get().getInnerType()), value);
				
				return IDLValue.create(Optional.ofNullable(itemIdlValue.getValue()), expectedIdlType.get());
			}
			else
			{
				if(value.isNull())
					return IDLValue.create(Optional.empty(), IDLType.createType(Type.OPT));
				
				if(value.isArray() && value.isEmpty())
					return IDLValue.create(Optional.empty(), IDLType.createType(Type.OPT));
				
				IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable( IDLType.createType(Type.OPT)), value);
				
				return IDLValue.create(Optional.ofNullable(itemIdlValue.getValue()), IDLType.createType(Type.OPT));							
			}							
		}		

		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Cannot convert type " + type.name());

	}
	
	public static IDLType getIDLType(Class valueClass)
	{
		// handle null values
		if(valueClass == null)
			return IDLType.createType(Type.NULL);
		
		if(IDLType.isDefaultType(valueClass))
			return IDLType.createType(valueClass);		
		
		if(Optional.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.OPT);
		
		if(List.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.VEC);		
		
		if(GregorianCalendar.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.INT);
		
		if(Date.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.INT);		

		Map<Label,IDLType> typeMap = new TreeMap<Label,IDLType>();

		Field[] fields = valueClass.getDeclaredFields();		
		
		for(Field field : fields)
		{
			if(field.isAnnotationPresent(JsonIgnore.class))
				continue;
			
			if(field.isEnumConstant())
				continue;			
			
			String name = field.getName();
			if(name.startsWith("this$"))
				continue;
			
			if(name.startsWith("$VALUES"))
				continue;			
			
			if(name.startsWith("ENUM$VALUES"))
				continue;			
			
			Class typeClass = field.getType();	
			
			IDLType fieldType = getIDLType(typeClass);
			
			if(field.isAnnotationPresent(JsonProperty.class))
				name = field.getAnnotation(JsonProperty.class).value();
			
			Label label = Label.createNamedLabel((String)name);			
						
			boolean isArray = typeClass.isArray();
			boolean isOptional = Optional.class.isAssignableFrom(typeClass);
			
			if(IDLType.isDefaultType(typeClass) || GregorianCalendar.class.isAssignableFrom(typeClass) || Date.class.isAssignableFrom(typeClass))
			{
				// if we do not specify type in annotation and type is one of default
				typeMap.put(label, fieldType);	
				continue;
			}
			else if(List.class.isAssignableFrom(typeClass)) 
			{	
				isArray = true;
				typeClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
				
				fieldType = getIDLType(typeClass);
			}			
			
			// do nested type introspection if type is RECORD		
			if(fieldType.getType() == Type.RECORD || fieldType.getType() == Type.VARIANT)
			{
				String className = typeClass.getSimpleName();
				
				// handle RECORD arrays
				if(isArray)
				{
					fieldType.setName(className);
					fieldType = IDLType.createType(Type.VEC, fieldType);
				}
				else
					fieldType.setName(className);

			}else if(isArray)
			{
				// handle arrays , not record types
				fieldType = IDLType.createType(Type.VEC, fieldType);
			}else if(isOptional)
			{
				// handle Optional, not record types
				
				fieldType = IDLType.createType(Type.OPT, fieldType);
			}
			
			typeMap.put(label, fieldType);	

		}	
		
		IDLType idlType;
		
		if(valueClass.isEnum()) 
		{
			Class<Enum> enumClass = (Class<Enum>)valueClass;
			Enum[] constants = enumClass.getEnumConstants();
			
			for (Enum constant : constants) {
				String name = constant.name();
				
				try {
					if (enumClass.getField(name).isAnnotationPresent(JsonProperty.class))
						name = enumClass.getField(name).getAnnotation(JsonProperty.class).value();					
				} catch (NoSuchFieldException | SecurityException e) {
					continue;
				}
				
				Label namedLabel = Label.createNamedLabel(name);

				if (!typeMap.containsKey(namedLabel))
					typeMap.put(namedLabel, null);
			}			
			idlType = IDLType.createType(Type.VARIANT, typeMap);
		}
		else
			idlType = IDLType.createType(Type.RECORD, typeMap);
		
		idlType.setName(valueClass.getSimpleName());
		
		return idlType;		
	}	
}
