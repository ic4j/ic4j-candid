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
import java.time.Duration;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.lang3.ClassUtils;
import org.ic4j.candid.annotations.Id;
import org.ic4j.candid.annotations.Ignore;
import org.ic4j.candid.annotations.Modes;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Func;
import org.ic4j.types.Service;

public class PojoUtils {
	
	
	public static IDLType getIDLType(Class valueClass)
	{
		// handle null values
		if(valueClass == null)
			return IDLType.createType(Type.NULL);
		
		if(IDLType.isDefaultType(valueClass))
			return IDLType.createType(valueClass);	
		
		if(Service.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.SERVICE,valueClass);
		
		if(Func.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.FUNC,valueClass);		
		
		if(Optional.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.OPT,valueClass);
		
		if(List.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.VEC,valueClass);		
		
		if(GregorianCalendar.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.INT);
		
		if(Date.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.INT,valueClass);	
		
		if(Duration.class.isAssignableFrom(valueClass))			
			return org.ic4j.types.Duration.getIDLType();	

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
			
			Class fieldClass = field.getType();	
			
			if(fieldClass.isPrimitive())
				fieldClass = ClassUtils.primitiveToWrapper(fieldClass);
			
			IDLType fieldType = getIDLType(fieldClass);
			
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
						
			boolean isArray = false;
			
			if (List.class.isAssignableFrom(fieldClass))
			{
				fieldClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
				isArray = true;
			}
			else			
				isArray = fieldClass.isArray();
			
			boolean isOptional = false;
			if(Optional.class.isAssignableFrom(fieldClass))
			{
				fieldClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
				isOptional = true;
			}
			
			if(field.isAnnotationPresent(org.ic4j.candid.annotations.Field.class))
				fieldType = IDLType.createType(field.getAnnotation(org.ic4j.candid.annotations.Field.class).value());
			else if(IDLType.isDefaultType(fieldClass) || GregorianCalendar.class.isAssignableFrom(fieldClass) || Date.class.isAssignableFrom(fieldClass)
					|| Func.class.isAssignableFrom(fieldClass) || Service.class.isAssignableFrom(fieldClass))
			{
				// if we do not specify type in annotation and type is one of default
						
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
				
				fieldType.setJavaType(fieldClass);
				
				typeMap.put(label, fieldType);	
				
				continue;
			}
			
			
			// do nested type introspection if type is RECORD		
			if(fieldType.getType() == Type.RECORD || fieldType.getType() == Type.VARIANT)
			{
				String className = fieldClass.getSimpleName();
				
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
				if(isOptional)
					fieldType = IDLType.createType(Type.OPT, fieldType);
				// handle arrays , not record types
				fieldType = IDLType.createType(Type.VEC, fieldType);
			}else if(isOptional)
			{
				// handle Optional, not record types
				
				fieldType = IDLType.createType(Type.OPT, fieldType);
			}
			
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
			
			fieldType.setJavaType(fieldClass);
			
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
					if (enumClass.getField(name).isAnnotationPresent(Name.class))
						name = enumClass.getField(name).getAnnotation(Name.class).value();					
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
		
		idlType.setJavaType(valueClass);
		
		return idlType;		
	}
}
