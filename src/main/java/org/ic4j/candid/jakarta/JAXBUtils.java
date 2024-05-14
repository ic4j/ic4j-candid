/*
 * Copyright 2024 Exilor Inc.
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

package org.ic4j.candid.jakarta;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.Duration;

import org.apache.commons.lang3.ClassUtils;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;

public class JAXBUtils {
	
	public static IDLType getIDLType(Class valueClass) {
		
		// handle null values
		if (valueClass == null)
			return IDLType.createType(Type.NULL);
		
		
		if(JAXBElement.class.isAssignableFrom(valueClass) )
		{
			JAXBElement jaxbValue;
			try {
				jaxbValue = (JAXBElement) valueClass.newInstance();
				IDLType idlType  = getIDLType(jaxbValue.getDeclaredType());
				
				return idlType;
			} catch (InstantiationException | IllegalAccessException e) {
				IDLType idlType = IDLType.createType(Type.RESERVED, valueClass);
				return idlType;
			}
			
		}
		
		// handle BigDecimal like Double
		if (BigDecimal.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.FLOAT64, valueClass);		

		if (IDLType.isDefaultType(valueClass))
			return IDLType.createType(valueClass);

		if (Optional.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.OPT, valueClass);

		if (List.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.VEC, valueClass);

		// handle XMLGregorianCalendar like nanosecond timestamp
		if (XMLGregorianCalendar.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.INT, valueClass);		
		
		if(Duration.class.isAssignableFrom(valueClass))			
			return org.ic4j.types.Duration.getIDLType();
		

		Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();

		Field[] fields = IDLUtils.getAllFields(valueClass);

		for (Field field : fields) {
			if (field.isAnnotationPresent(XmlTransient.class))
				continue;

			if (field.isEnumConstant())
				continue;

			String name = field.getName();
			if (name.startsWith("this$"))
				continue;

			if (name.startsWith("$VALUES"))
				continue;

			if (name.startsWith("ENUM$VALUES"))
				continue;
			
			if (!field.isAnnotationPresent(XmlElement.class)  && !field.isAnnotationPresent(XmlElementRef.class) && !field.isAnnotationPresent(XmlAttribute.class) 
					&& !field.isAnnotationPresent(XmlAnyElement.class) && !field.isAnnotationPresent(XmlValue.class))
				continue;			

			Class fieldClass = field.getType();
			
			if(fieldClass.isPrimitive())
				fieldClass = ClassUtils.primitiveToWrapper(fieldClass);
			
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
			
			IDLType fieldType;
			
			boolean isRequired = true;
			
			if (field.isAnnotationPresent(XmlAnyElement.class))
				fieldType = IDLType.createType(Type.RESERVED);
			else		
				fieldType = getIDLType(fieldClass);

			if (field.isAnnotationPresent(XmlElement.class)) {
				XmlElement xmlElement = field.getAnnotation(XmlElement.class);

				String xmlName = xmlElement.name();

				if (xmlName != null)
					name = xmlName;
				
				if(!xmlElement.required() )
					isRequired = false;
			}
			
			if (field.isAnnotationPresent(XmlElementRef.class)) {
				XmlElementRef xmlElementRef = field.getAnnotation(XmlElementRef.class);

				String xmlName = xmlElementRef.name();
				

				if (xmlName != null)
					name = xmlName;
							
				if(!xmlElementRef.required())
					isRequired = false;
				
				if(JAXBElement.class.isAssignableFrom(fieldClass) )
				{
					JAXBElement jaxbValue;
					try {
						jaxbValue = (JAXBElement) fieldClass.newInstance();
						fieldClass = jaxbValue.getDeclaredType();
						
					} catch (InstantiationException | IllegalAccessException e) {

					}					
				}				
			}

			if (field.isAnnotationPresent(XmlAttribute.class)) {
				XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);

				String xmlName = xmlAttribute.name();

				if (xmlName != null)
					name = xmlName;
				
				if(!xmlAttribute.required() )
					isRequired = false;
			}
			
			if (field.isAnnotationPresent(XmlEnumValue.class)) {
				String xmlName = field.getAnnotation(XmlEnumValue.class).value();

				if (xmlName != null)
					name = xmlName;
			}			

			Label label = Label.createNamedLabel((String) name);

			if (IDLType.isDefaultType(fieldClass) || XMLGregorianCalendar.class.isAssignableFrom(fieldClass)
					|| BigDecimal.class.isAssignableFrom(fieldClass)) {
				// if we do not specify type in annotation and type is one of default
				if (isOptional && fieldType.getType() != Type.OPT) 
					// handle Optional
					fieldType = IDLType.createType(Type.OPT, fieldType);
				
				if (isArray) 	
					// handle arrays , not record types
					fieldType = IDLType.createType(Type.VEC, fieldType);
				
				if(!isRequired)
					// handle not required as Optional
					fieldType = IDLType.createType(Type.OPT, fieldType);
				
				fieldType.setJavaType(fieldClass);
				
				typeMap.put(label, fieldType);
				continue;
			} 

			// do nested type introspection if type is RECORD
			if (fieldType.getType() == Type.RECORD || fieldType.getType() == Type.VARIANT) {
				String className = fieldClass.getSimpleName();

				if (fieldClass.isAnnotationPresent(XmlType.class)) {
					XmlType xmlType = (XmlType) fieldClass.getAnnotation(XmlType.class);

					if (xmlType.name() != null)
						className = xmlType.name();
				}
				
				fieldType.setName(className);
				
				if (isOptional && fieldType.getType() != Type.OPT) 
					// handle Optional
					fieldType = IDLType.createType(Type.OPT, fieldType);

				// handle RECORD arrays
				if (isArray) 
					fieldType = IDLType.createType(Type.VEC, fieldType);	
								

			} else if (isArray) 
			{
				// array of optionals
				if(isOptional) 
					// handle Optional
					fieldType = IDLType.createType(Type.OPT, fieldType);
				// handle arrays , not record types
				fieldType = IDLType.createType(Type.VEC, fieldType);							
			}
			else if (isOptional && fieldType.getType() != Type.OPT)
			{	
				// handle Optional
				fieldType = IDLType.createType(Type.OPT, fieldType);			
			}	
			
			if(!isRequired)
				// handle not required as Optional
				fieldType = IDLType.createType(Type.OPT, fieldType);
			
			fieldType.setJavaType(fieldClass);
			typeMap.put(label, fieldType);

		}

		IDLType idlType;

		if (valueClass.isEnum()) {
			Class<Enum> enumClass = (Class<Enum>) valueClass;
			Enum[] constants = enumClass.getEnumConstants();

			for (Enum constant : constants) {

				String name = constant.name();

				try {
					if (enumClass.getField(name).isAnnotationPresent(XmlEnumValue.class))
					{
						name = enumClass.getField(name).getAnnotation(XmlEnumValue.class).value();	
					
						name = replaceSpecialChars(name);
					}
				} catch (NoSuchFieldException | SecurityException e) {
					continue;
				}
				
				Label namedLabel = Label.createNamedLabel(name);

				if (!typeMap.containsKey(namedLabel))
					typeMap.put(namedLabel, null);

			}
			idlType = IDLType.createType(Type.VARIANT, typeMap);
		} else
			idlType = IDLType.createType(Type.RECORD, typeMap);

		String className = valueClass.getSimpleName();

		if (valueClass.isAnnotationPresent(XmlType.class)) {
			XmlType xmlType = (XmlType) valueClass.getAnnotation(XmlType.class);

//			if (xmlType.name() != null)
//				className = xmlType.name();
		}

		idlType.setName(className);
		
		idlType.setJavaType(valueClass);

		return idlType;
	}	
	
	public static String replaceSpecialChars(String value)
	{
		return value.replace("/", "_").replace(".", "_").replace("-", "_").replace("-", "_").replace("+", "_");
	}
	

}
