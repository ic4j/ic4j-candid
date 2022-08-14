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
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.ic4j.candid.annotations.Id;
import org.ic4j.candid.annotations.Ignore;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.ObjectSerializer;

public final class PojoSerializer implements ObjectSerializer {
	
	public static PojoSerializer create() {
		PojoSerializer deserializer = new PojoSerializer();
		return deserializer; 
	}	

	@Override
	public IDLValue serialize(Object value) {
		
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
		
		IDLValue idlValue;
		
		// handle arrays
		if(value.getClass().isArray())
		{
			List<Map<Label, Object>> arrayValue = new ArrayList();
			
			
			IDLType idlType = IDLType.createType(Type.VEC);

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
					
					IDLValue itemIDLValue = this.getIDLValue(item, Type.VEC);
					
					arrayValue.add(itemIDLValue.getValue());
					
					idlType = IDLType.createType(Type.VEC, itemIDLValue.getIDLType());
				}
				
				
				idlValue = IDLValue.create(arrayValue.toArray(), idlType);
			}
		}
		else
		{			
			idlValue = this.getIDLValue(value, null);
		}

		return idlValue;
	}
	
	// Introspect POJO Class
	IDLValue getIDLValue(Object value, Type parentType)
	{
		// handle null values
		if(value == null)
			return IDLValue.create(value,Type.NULL);
			
		Class valueClass = value.getClass();
		
		// handle GregorianCalendar like nanosecond timestamp
		if(value instanceof GregorianCalendar)
		{
			value = ((GregorianCalendar)value).getTimeInMillis()*1000000;
			
			return IDLValue.create(value,Type.INT);
		}
		
		// handle Date like nanosecond timestamp
		if(value instanceof Date)
		{
			value = ((Date)value).getTime()*1000000;
			
			return IDLValue.create(value,Type.INT);
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
					IDLValue nestedIdlValue = this.getIDLValue(nestedValue, Type.OPT);
					
					IDLType nestedIdlType = nestedIdlValue.getIDLType();
					return IDLValue.create(Optional.ofNullable(nestedIdlValue.getValue()), IDLType.createType(Type.OPT, nestedIdlType));
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
			
			boolean isArray = typeClass.isArray();
			boolean isOptional = Optional.class.isAssignableFrom(typeClass);
			
			if(field.isAnnotationPresent(org.ic4j.candid.annotations.Field.class))
				fieldType = IDLType.createType(field.getAnnotation(org.ic4j.candid.annotations.Field.class).value());
			else if(IDLType.isDefaultType(typeClass))
			{
				// if we do not specify type in annotation and type is one of default
				fieldType = IDLType.createType(item);
				typeMap.put(label, fieldType);	
				valueMap.put(label, item);
				continue;
			}
			else
				fieldType = IDLType.createType(Type.RECORD);
			
			// do nested type introspection if type is RECORD		
			if(fieldType.getType() == Type.RECORD || fieldType.getType() == Type.VARIANT)
			{
				
				// handle RECORD arrays
				if(isArray)
				{
					Object[] nestedArray = (Object[])item;
					List<Object> arrayValue = new ArrayList();
					
					fieldType = this.getIDLType(typeClass.getComponentType());
					for(int i = 0; i < nestedArray.length; i++)
					{
						Object nestedValue = nestedArray[i];
						// if nested RECORD or VARIANT is Optional 
						if(nestedValue != null && Optional.class.isAssignableFrom(nestedValue.getClass()))
							nestedValue = ((Optional)nestedValue).orElse(null);
						
						IDLValue nestedIdlValue = this.getIDLValue(nestedValue, fieldType.getType());
						
						fieldType = nestedIdlValue.getIDLType();
						
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
					
					IDLValue nestedIdlValue = this.getIDLValue(nestedValue, fieldType.getType());
					
					fieldType = nestedIdlValue.getIDLType();
					
					item = nestedIdlValue.getValue();
				}
			}else if(isArray)
			{
				// handle arrays , not record types
				fieldType = IDLType.createType(Type.VEC, fieldType);
			}else if(isOptional)
			{
				// handle Optional, not record types			
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
			
			Label enumLabel = Label.createNamedLabel(name);
			// if there is no Enum value, set it to null
			if(!valueMap.containsKey(enumLabel))
			{
				typeMap.put(enumLabel, IDLType.createType(Type.NULL));
				valueMap.put(enumLabel, null);
			}
				
		}
		
		IDLType idlType = IDLType.createType(parentType, typeMap);
		IDLValue idlValue = IDLValue.create(valueMap, idlType);
		
		return idlValue;
	}
	
	public IDLType getIDLType(Class valueClass)
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
			if(field.isAnnotationPresent(Ignore.class))
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
			
			IDLType fieldType = this.getIDLType(typeClass);
			
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
						
			boolean isArray = typeClass.isArray();
			boolean isOptional = Optional.class.isAssignableFrom(typeClass);
			
			if(field.isAnnotationPresent(org.ic4j.candid.annotations.Field.class))
				fieldType = IDLType.createType(field.getAnnotation(org.ic4j.candid.annotations.Field.class).value());
			else if(IDLType.isDefaultType(typeClass) || GregorianCalendar.class.isAssignableFrom(typeClass) || Date.class.isAssignableFrom(typeClass))
			{
				// if we do not specify type in annotation and type is one of default
				typeMap.put(label, fieldType);	
				continue;
			}
			else if(List.class.isAssignableFrom(typeClass)) 
			{	
				isArray = true;
				typeClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
				
				fieldType = this.getIDLType(typeClass);
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
