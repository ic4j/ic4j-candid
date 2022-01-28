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

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.ic4j.candid.CandidError;
import org.ic4j.candid.ObjectDeserializer;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Principal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JacksonDeserializer implements ObjectDeserializer {
	Optional<IDLType> idlType = Optional.empty();
	ObjectMapper mapper = new ObjectMapper();	
	
	public static JacksonDeserializer create(IDLType idlType)
	{
		JacksonDeserializer deserializer = new JacksonDeserializer();
		deserializer.idlType = Optional.ofNullable(idlType);
		return deserializer;
		
	}
	
	public static JacksonDeserializer create() {
		JacksonDeserializer deserializer = new JacksonDeserializer();
		return deserializer; 
	}

	@Override
	public <T> T deserialize(IDLValue value, Class<T> clazz) {
		if(clazz != null)
		{
			
			JsonNode jsonNode = this.getValue(value.getIDLType(), this.idlType, value.getValue());
			if(JsonNode.class.isAssignableFrom(clazz))
				return (T) jsonNode;
			else
			{
				try {
					return (T) mapper.treeToValue(jsonNode, clazz);
				} catch (JsonProcessingException | IllegalArgumentException e) {
					throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,e,e.getLocalizedMessage());
				}
			}
		}
		else
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,  "Class is not defined" );
	}
	
	JsonNode getPrimitiveValue(Type type, Object value) {
		JsonNode result = NullNode.getInstance();
		
		if(value == null)
			return result;
		
		switch(type)
		{
		case BOOL:
			result = BooleanNode.valueOf((Boolean) value);
			break;
		case INT:
			result = BigIntegerNode.valueOf((BigInteger) value);
			break;	
		case INT8:
			result = ShortNode.valueOf((Byte) value);
			break;	
		case INT16:
			result = ShortNode.valueOf((Short) value);
			break;	
		case INT32:
			result = IntNode.valueOf((Integer) value);
			break;	
		case INT64:
			result = LongNode.valueOf((Long) value);
		case NAT:
			result = BigIntegerNode.valueOf((BigInteger) value);			
			break;
		case NAT8:
			result = ShortNode.valueOf((Byte) value);
			break;	
		case NAT16:
			result = ShortNode.valueOf((Short) value);
			break;	
		case NAT32:
			result = IntNode.valueOf((Integer) value);
			break;	
		case NAT64:
			result = LongNode.valueOf((Long) value);
			break;			
		case FLOAT32:
			result = FloatNode.valueOf((Float) value);
			break;	
		case FLOAT64:
			result = DoubleNode.valueOf((Double) value);
			break;			
		case TEXT:
			result = TextNode.valueOf((String) value);
			break;
		case EMPTY:
			result = JsonNodeFactory.instance.objectNode();
			break;				
		case PRINCIPAL:
			Principal principal = (Principal) value;
			result = TextNode.valueOf(principal.toString());
			break;						
		}
		
		return result;
	}
	
	JsonNode getValue(IDLType idlType, Optional<IDLType> expectedIdlType, Object value) {
		JsonNode result = NullNode.getInstance();
		
		if(value == null)
			return result;		
		
		Type type = Type.NULL;
		
		if(expectedIdlType.isPresent())
		{
			type = expectedIdlType.get().getType();
			if(idlType != null)		
				idlType = expectedIdlType.get();
		}
		
		if(type.isPrimitive())
			return this.getPrimitiveValue(type,value);
		
		// handle VEC
		if(type == Type.VEC)
		{
			IDLType expectedInnerIDLType = null;
			IDLType innerIdlType = idlType.getInnerType();
			
			if(expectedIdlType.isPresent())
			{	
				expectedInnerIDLType = expectedIdlType.get().getInnerType();
				innerIdlType = expectedInnerIDLType;
			}
		
			// handle byte array
			if(innerIdlType.getType() == Type.INT8 ||innerIdlType.getType() == Type.NAT8)
				return BinaryNode.valueOf((byte[]) value);
			else 
			{						
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
				
				Object[] arrayValue = (Object[]) value;
				
				for(Object item : arrayValue)
						arrayNode.add(this.getValue(idlType.getInnerType(), Optional.ofNullable(expectedInnerIDLType), item));
				
				return arrayNode;
			}
		}
		
		// handle OPT
		if(type == Type.OPT)
		{
			Optional optionalValue = (Optional) value;
			
			if(optionalValue.isPresent())
			{
				IDLType expectedInnerIDLType = null;
				
				if(expectedIdlType.isPresent())
					expectedInnerIDLType = expectedIdlType.get().getInnerType();
				
				return this.getValue(idlType.getInnerType(), Optional.ofNullable(expectedInnerIDLType), optionalValue.get());
			}
			else 
				return result;
		}
		
		
		if(type == Type.RECORD || type == Type.VARIANT)
		{
			ObjectNode treeNode = JsonNodeFactory.instance.objectNode();
			
			Map<Label,Object> valueMap = (Map<Label, Object>) value;
			
			Map<Label,IDLType> typeMap = idlType.getTypeMap();
			
			Map<Label,IDLType> expectedTypeMap = new TreeMap<Label,IDLType>();
			
			if(expectedIdlType.isPresent() && expectedIdlType.get().getTypeMap() != null)
				 expectedTypeMap = expectedIdlType.get().getTypeMap();
			
			Set<Label> labels = valueMap.keySet();
			
			Map<Long,Label> expectedLabels = new TreeMap<Long,Label>();
			
			for(Label entry : expectedTypeMap.keySet())
				expectedLabels.put(entry.getId(), entry);
			
			for(Label label : labels)
			{
				String fieldName;
				
				IDLType itemIdlType = typeMap.get(label);
				
				IDLType expectedItemIdlType = null;			
				
				if(expectedTypeMap.containsKey(label))
				{
					expectedItemIdlType = expectedTypeMap.get(label);
					
					Label expectedLabel = expectedLabels.get(label.getId());
					
					fieldName = expectedLabel.toString();
				}
				else
					fieldName = label.toString();
				
				JsonNode itemNode = this.getValue(itemIdlType, Optional.ofNullable(expectedItemIdlType), valueMap.get(label));
				
				treeNode.set(fieldName, itemNode);
			}
			
			return treeNode;
		}		
		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
				"Cannot convert type " + type.name());
	}

}
