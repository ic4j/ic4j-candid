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

package org.ic4j.candid.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.ic4j.candid.annotations.Id;
import org.ic4j.candid.annotations.Ignore;
import org.ic4j.candid.annotations.Modes;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Func;
import org.ic4j.types.Service;
import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.ObjectSerializer;

public final class PojoSerializer implements ObjectSerializer {
	Optional<IDLType> idlType = Optional.empty();
	
	public static PojoSerializer create() {
		PojoSerializer serializer = new PojoSerializer();
		return serializer; 
	}
	
	public void setIDLType(IDLType idlType)
	{
		this.idlType = Optional.ofNullable(idlType);
	}	

	@Override
	public IDLValue serialize(Object value) {
		
		//handle primitives
		if(this.idlType.isPresent())
		{
			if(this.idlType.get().getType().isPrimitive())
				return IDLValue.create(value,this.idlType.get().getType());
		}
		
		// handle null values
		if(value == null)
			return IDLValue.create(value,Type.NULL);
		
		// handle BigDecimal like Double
		if(value instanceof BigDecimal)
			value = ((BigDecimal)value).doubleValue();		
		
		boolean isArray = value.getClass().isArray();
		boolean isOptional = Optional.class.isAssignableFrom(value.getClass());	
		
		// handle default types
		if(IDLType.isDefaultType(value.getClass()) && !isArray && !isOptional)
			return IDLValue.create(value);
		
		// manage Func and Service types
		if(value instanceof Func || value instanceof Service)
			return IDLValue.create(value);
		
		IDLValue idlValue;
		
		// handle List like array
		if(value instanceof List)
			value = ((List)value).toArray();
			
		
		// handle arrays
		if(value.getClass().isArray())
		{
			List <Object> arrayValue = new ArrayList<Object>();
			
			IDLType idlType = IDLType.createType(Type.VEC);

			// handle binaries
			if(value instanceof Byte[])
			{			
				idlType = IDLType.createType(Type.VEC, Type.NAT8);
				idlValue = IDLValue.create(value, idlType);
			}
			else if (value instanceof byte[])
			{
				idlType = IDLType.createType(Type.VEC, Type.NAT8);
				idlValue = IDLValue.create(ArrayUtils.toObject((byte[])value), idlType);				
			}
			else
			{
				Object[] array = (Object[]) value;
				
				for(int i = 0; i < array.length; i++)
				{
					Object item = array[i];
								
					
					if(this.idlType.isPresent() && this.idlType.get().getType() == Type.VEC)
					{
						idlType = this.idlType.get();	
						IDLValue itemIDLValue = this.getIDLValue(item,Type.VEC, idlType.getInnerType());
						
						arrayValue.add(itemIDLValue.getValue());
					}
					else 
					{	
						IDLValue itemIDLValue = this.getIDLValue(item, Type.VEC, null);
						
						arrayValue.add(itemIDLValue.getValue());
						idlType = IDLType.createType(Type.VEC, itemIDLValue.getIDLType());
					}
				}
				
				
				idlValue = IDLValue.create(arrayValue.toArray(), idlType);
			}
		}
		else
		{			
			if(this.idlType.isPresent())
					idlValue = this.getIDLValue(value,null, this.idlType.get());
			else		
				idlValue = this.getIDLValue(value, null, null);
		}

		return idlValue;
	}
	
	// Introspect POJO Class
	IDLValue getIDLValue(Object value,Type parentType, IDLType idlType)
	{

		//handle primitives, func and service types
		if(idlType != null)
			if(idlType.getType().isPrimitive() || idlType.getType() == Type.FUNC || idlType.getType() == Type.SERVICE )
				return IDLValue.create(value,idlType.getType());
			
		
		// handle null values
		if(value == null)
			return IDLValue.create(value,Type.NULL);
			
		Class valueClass = value.getClass();

		if(!valueClass.isArray())
		{
			// handle GregorianCalendar like nanosecond timestamp
			if(value instanceof GregorianCalendar)
			{
				value = ((GregorianCalendar)value).getTimeInMillis()*1000000;
				
				return IDLValue.create(value,Type.INT);
			}
			
			// handle Duration 
			if(value instanceof Duration)
			{			
				return org.ic4j.types.Duration.serialize((Duration) value);
			}			
			
			// handle Date like nanosecond timestamp
			if(value instanceof Date)
			{
				value = ((Date)value).getTime()*1000000;
				
				return IDLValue.create(value,Type.INT);
			}				
	
			if(IDLType.isPrimitiveType(valueClass))
				return IDLValue.create(value);
			
			// manage Func and Service types
			if(value instanceof Func || value instanceof Service)
				return IDLValue.create(value);
		}
		
		if(Optional.class.isAssignableFrom(valueClass))
		{
			Optional optionalValue = (Optional)value;
			
			if(optionalValue.isPresent())
			{
				Object nestedValue = optionalValue.get();
				
				if(IDLType.isDefaultType(nestedValue.getClass()))
					return IDLValue.create(optionalValue);
				else
				{
					IDLValue nestedIdlValue;
					if(idlType != null && idlType.getType() == Type.OPT && idlType.getInnerType() != null)
					{	
						nestedIdlValue = this.getIDLValue(nestedValue, Type.OPT, idlType.getInnerType());
						return IDLValue.create(Optional.ofNullable(nestedIdlValue.getValue()), idlType);
					}
					else
					{	
						nestedIdlValue = this.getIDLValue(nestedValue,Type.OPT, null);
						IDLType nestedIdlType = nestedIdlValue.getIDLType();						
						return IDLValue.create(Optional.ofNullable(nestedIdlValue.getValue()), IDLType.createType(Type.OPT, nestedIdlType));
					}
				}
			}else return IDLValue.create(optionalValue);
		}
		
		Map<Label, Object> valueMap =  new TreeMap<Label, Object>();
		Map<Label,IDLType> typeMap = new TreeMap<Label,IDLType>();
		
		Field[] fields = valueClass.getDeclaredFields();
		
		for(Field field : fields)
		{			
			if(field.isAnnotationPresent(Ignore.class))
				continue;
			
			if(field.isEnumConstant())
				continue;
			
			field.setAccessible(true);
			
			String name = field.getName();
			if(name.startsWith("this$"))
				continue;
			
			if(name.startsWith("$VALUES"))
				continue;			
			
			if(name.startsWith("ENUM$VALUES"))
				continue;			
			
			Class typeClass = field.getType();
			
			Object item = null;
			try {
				item = field.get(value);
			} catch (IllegalArgumentException e) {
				continue;
			} catch (IllegalAccessException e) {
				continue;
			}
			
			// handle List like array
			if(item != null && item instanceof List)
			{
				item = ((List)item).toArray();
				
				typeClass = item.getClass();
			}
			
			// handle BigDecimal like Double
			if(item != null && BigDecimal.class.isAssignableFrom(typeClass))
				item = ((BigDecimal)item).doubleValue();					
			
			IDLType fieldType;
			
			if(field.isAnnotationPresent(Name.class))
				name = field.getAnnotation(Name.class).value();
			
			Label label;
			if (field.isAnnotationPresent(Id.class))
			{
				int id = field.getAnnotation(Id.class).value();
				label = Label.createIdLabel((long) id);
			}
			else
				label = Label.createNamedLabel((String)name);
			
			if(item == null)
			{
				// set NULL value
				fieldType = IDLType.createType(Type.NULL);
				
				typeMap.put(label,fieldType);	
				valueMap.put(Label.createNamedLabel((String)name), item);
				continue;
			}
			
			boolean isArray = false;
			
			// handle List like array
			if (List.class.isAssignableFrom(typeClass))
			{
				typeClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
				isArray = true;
				
				if (item != null)
					item = ((List) item).toArray();
			}
			else			
				isArray = typeClass.isArray();
			
			boolean isOptional = false;
			if(Optional.class.isAssignableFrom(typeClass))
			{
				typeClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
				isOptional = true;
			}
			
			if(field.isAnnotationPresent(org.ic4j.candid.annotations.Field.class))
				fieldType = IDLType.createType(field.getAnnotation(org.ic4j.candid.annotations.Field.class).value());
			else if(IDLType.isDefaultType(typeClass) || Func.class.isAssignableFrom(typeClass) || Service.class.isAssignableFrom(typeClass))
			{
				// if type is defined in IDLType
				if(idlType != null && (idlType.getType() == Type.RECORD || idlType.getType() == Type.VARIANT))
					fieldType = idlType.getTypeMap().get(label);
				else				
					// if we do not specify type in annotation and type is one of default
					fieldType = IDLType.createType(item);
				
				// handle Func type
				if(fieldType.getType() == Type.FUNC)
				{
					if (field.isAnnotationPresent(Modes.class))
					{
						Mode[] modes = field.getAnnotation(Modes.class).value();
						
						if(modes.length > 0)
						{
							fieldType.modes.add(modes[0]);
						}
					}
				}
				typeMap.put(label, fieldType);	
				valueMap.put(label, item);
				continue;
			}
			else
				fieldType = IDLType.createType(Type.RECORD);
			
			// handle Func type
			if(fieldType.getType() == Type.FUNC)
			{
				if (field.isAnnotationPresent(Modes.class))
				{
					Mode[] modes = field.getAnnotation(Modes.class).value();
					
					if(modes.length > 0)
					{
						fieldType.modes.add(modes[0]);
					}
				}
			}	
			
			// do nested type introspection if type is RECORD		
			if(fieldType.getType() == Type.RECORD || fieldType.getType() == Type.VARIANT)
			{
				
				// handle RECORD arrays
				if(isArray)
				{
					Object[] nestedArray = (Object[])item;
					List<Object> arrayValue = new ArrayList();
					
					IDLType innerIDLType = null;
					
					if(idlType != null && idlType.getType() == Type.VEC && idlType.getInnerType() != null)
						innerIDLType = idlType.getInnerType();
					
					
					fieldType = PojoUtils.getIDLType(typeClass.getComponentType());
					for(int i = 0; i < nestedArray.length; i++)
					{
						Object nestedValue = nestedArray[i];
						// if nested RECORD or VARIANT is Optional 
						if(nestedValue != null && Optional.class.isAssignableFrom(nestedValue.getClass()))
							nestedValue = ((Optional)nestedValue).orElse(null);
						
						IDLValue nestedIdlValue;
						
						if(innerIDLType != null && (innerIDLType.getType() == Type.RECORD || innerIDLType.getType() == Type.VARIANT))
						{
							fieldType = innerIDLType.getTypeMap().get(label);
							nestedIdlValue = this.getIDLValue(nestedValue, innerIDLType.getType(), fieldType);
						}
						else
						{	
							nestedIdlValue = this.getIDLValue(nestedValue, fieldType.getType(), null);
							fieldType = nestedIdlValue.getIDLType();
						}
						
						arrayValue.add(nestedIdlValue.getValue());
					}
					
					fieldType = IDLType.createType(Type.VEC, fieldType);
					item = arrayValue.toArray();
				}
				else
				{
					Object nestedValue = item;
					// if nested RECORD or VARIANT is Optional 
					if(item != null && Optional.class.isAssignableFrom(typeClass))
						nestedValue = ((Optional)item).orElse(null);
					
					IDLType innerIDLType = null;
					if(idlType != null && idlType.getType() == Type.OPT && idlType.getInnerType() != null)
						innerIDLType = idlType.getInnerType();
					
					IDLValue nestedIdlValue;
					
					if(innerIDLType != null && (innerIDLType.getType() == Type.RECORD || innerIDLType.getType() == Type.VARIANT))
					{
						fieldType = innerIDLType.getTypeMap().get(label);
						nestedIdlValue = this.getIDLValue(nestedValue, innerIDLType.getType(), fieldType);
					}
					else
					{	
						nestedIdlValue = this.getIDLValue(nestedValue, fieldType.getType(), null);
						fieldType = nestedIdlValue.getIDLType();
					}										
					
					item = nestedIdlValue.getValue();
				}
			}else if(isArray)
			{
				// handle arrays , not record types
				
				if(isOptional)
					fieldType = IDLType.createType(Type.OPT, fieldType);
				
				if(idlType != null && idlType.getType() == Type.VEC && idlType.getInnerType() != null)
					fieldType = idlType.getInnerType();
				else
					fieldType = IDLType.createType(Type.VEC, fieldType);
			}else if(isOptional)
			{
				// handle Optional, not record types	
				if(idlType != null && idlType.getType() == Type.OPT && idlType.getInnerType() != null)
					fieldType = idlType.getInnerType();
				else
					fieldType = IDLType.createType(Type.OPT, fieldType);
			}

			if(fieldType.getType() == Type.NAT || fieldType.getType() == Type.INT)
				item = IDLUtils.objectToBigInt(item);
			
			if(fieldType.getType() == Type.PRINCIPAL)
				item = IDLUtils.objectToPrincipal(item);
			
			typeMap.put(label, fieldType);	
			valueMap.put(label, item);
				
		}	
		
		if(parentType == null || parentType != Type.VARIANT)
			parentType = Type.RECORD;
		
		
		// handle Enum to VARIANT
		if(valueClass.isEnum())
		{
			parentType = Type.VARIANT;
			Enum enumValue = (Enum)value;
			
			String name = enumValue.name();
			
			try {
				if (valueClass.getField(name).isAnnotationPresent(Name.class))
					name = valueClass.getField(name).getAnnotation(Name.class).value();					
			} catch (NoSuchFieldException | SecurityException e) {
			}
			
			Label enumLabel = Label.createNamedLabel(name);
			// if there is no Enum value, set it to null
			if(!valueMap.containsKey(enumLabel))
			{
				typeMap.put(enumLabel, IDLType.createType(Type.NULL));
				valueMap.put(enumLabel, null);
			}
				
		}
		
		if(idlType == null)
			idlType = IDLType.createType(parentType, typeMap);
		
		IDLValue idlValue = IDLValue.create(valueMap, idlType);
		
		return idlValue;
	}
	
}
