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

package org.ic4j.candid.jaxb.jakarta;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.ObjectSerializer;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;

public final class JAXBSerializer implements ObjectSerializer {
	Optional<IDLType> idlType = Optional.empty();
	
	public static JAXBSerializer create() {
		JAXBSerializer deserializer = new JAXBSerializer();
		return deserializer;
	}
	
	public void setIDLType(IDLType idlType)
	{
		this.idlType = Optional.ofNullable(idlType);
	}	

	@Override
	public IDLValue serialize(Object value) {

		// handle null values
		if (value == null)
			return IDLValue.create(value, Type.NULL);
		

		if (value instanceof List) {
			value = ((List) value).toArray();
		}

		boolean isArray = value.getClass().isArray();
		boolean isOptional = Optional.class.isAssignableFrom(value.getClass());	

		// handle default types
		if (IDLType.isDefaultType(value.getClass()) && !isArray && !isOptional)
			return IDLValue.create(value);

		IDLValue idlValue;

		// handle arrays
		if (value.getClass().isArray()) {
			List<Map<Label, Object>> arrayValue = new ArrayList();

			IDLType idlType = IDLType.createType(Type.VEC);

			if (value instanceof Byte[]) {

				idlType = IDLType.createType(Type.VEC, Type.NAT8);
				idlValue = IDLValue.create(value, idlType);
			} else if (value instanceof byte[]) {
				idlType = IDLType.createType(Type.VEC, Type.NAT8);
				idlValue = IDLValue.create(ArrayUtils.toObject((byte[]) value), idlType);
			} else {
				Object[] array = (Object[]) value;

				for (int i = 0; i < array.length; i++) {
					Object item = array[i];

					IDLValue itemIDLValue = this.getIDLValue(item, Type.VEC);

					arrayValue.add(itemIDLValue.getValue());

					idlType = IDLType.createType(Type.VEC, itemIDLValue.getIDLType());
				}

				idlValue = IDLValue.create(arrayValue.toArray(), idlType);
			}
		} else {
			idlValue = this.getIDLValue(value, null);
		}

		return idlValue;
	}

	// Introspect JAXB Class
	IDLValue getIDLValue(Object value, Type parentType) {
		// handle null values
		if (value == null)
			return IDLValue.create(value, Type.NULL);
		
		// handle float as double, Motoko does not support Float32		
		if(value instanceof Float)
		{
			if(value.getClass().isPrimitive())
				value = new Float((float) value);
			
			value = ((Float)value).doubleValue();
			
		}
		
		if(value instanceof JAXBElement)
			value = ((JAXBElement) value).getValue();
		
		// handle null values
		if (value == null)
			return IDLValue.create(value, Type.NULL);

		// handle XMLGregorianCalendar like nanosecond timestamp
		if (value instanceof XMLGregorianCalendar) {
			value = ((XMLGregorianCalendar) value).toGregorianCalendar().getTimeInMillis() * 1000000;

			return IDLValue.create(value, Type.INT);
		}
		
		// handle XML Duration 
		if (value instanceof Duration) {
			return org.ic4j.types.Duration.serializeXML((Duration) value);
		}		
		
		// handle BigDecimal like Double
		if (value instanceof BigDecimal) {
			value = ((BigDecimal) value).doubleValue();

			return IDLValue.create(value, Type.FLOAT64);
		}
				

		Class valueClass = value.getClass();

		if (Optional.class.isAssignableFrom(valueClass)) {
			Optional optionalValue = (Optional) value;

			if (optionalValue.isPresent()) {
				Object nestedValue = optionalValue.get();

				if (IDLType.isDefaultType(nestedValue.getClass()))
					return IDLValue.create(optionalValue);
				else {
					IDLValue nestedIdlValue = this.getIDLValue(nestedValue, Type.OPT);

					IDLType nestedIdlType = nestedIdlValue.getIDLType();
					return IDLValue.create(Optional.ofNullable(nestedIdlValue.getValue()),
							IDLType.createType(Type.OPT, nestedIdlType));
				}
			} else
				return IDLValue.create(optionalValue);
		}

		Map<Label, Object> valueMap = new TreeMap<Label, Object>();
		Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();

		Field[] fields = IDLUtils.getAllFields(valueClass);

		for (Field field : fields) {
			if (field.isAnnotationPresent(XmlTransient.class))
				continue;

			if (field.isEnumConstant())
				continue;

			field.setAccessible(true);

			String name = field.getName();
			if (name.startsWith("this$"))
				continue;

			if (name.startsWith("$VALUES"))
				continue;

			if (name.startsWith("ENUM$VALUES"))
				continue;

			if (!field.isAnnotationPresent(XmlElement.class) && !field.isAnnotationPresent(XmlElementRef.class) && !field.isAnnotationPresent(XmlAttribute.class) 
					&& !field.isAnnotationPresent(XmlAnyElement.class) && !field.isAnnotationPresent(XmlValue.class))
				continue;

			Class fieldClass = field.getType();
			
			if(fieldClass.isPrimitive())
				fieldClass = ClassUtils.primitiveToWrapper(fieldClass);

			Object item = null;
			try {
				item = field.get(value);
			} catch (IllegalArgumentException e) {
				continue;
			} catch (IllegalAccessException e) {
				continue;
			}		
			
			boolean isArray = false;
			
			// handle List like array
			if (List.class.isAssignableFrom(fieldClass))
			{
				fieldClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
				isArray = true;
				
				if (item != null)
					item = ((List) item).toArray();
			}
			else			
				isArray = fieldClass.isArray();
			

			boolean isOptional = false;
			if(Optional.class.isAssignableFrom(fieldClass))
			{
				fieldClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
				isOptional = true;
			}

			
			boolean isRequired = true;
			
			// handle BigDecimal like Double
			if (item != null && BigDecimal.class.isAssignableFrom(fieldClass))
				item = ((BigDecimal) item).doubleValue();

			IDLType fieldType;

			if (field.isAnnotationPresent(XmlElement.class)) {
				XmlElement xmlElement = field.getAnnotation(XmlElement.class);

				String xmlName = xmlElement.name();

				if (xmlName != null)
					name = xmlName;
				
				if(!xmlElement.required())
					isRequired = false;

			}
			
			if (field.isAnnotationPresent(XmlElementRef.class)) {
				XmlElementRef xmlElementRef = field.getAnnotation(XmlElementRef.class);

				String xmlName = xmlElementRef.name();

				if (xmlName != null)
					name = xmlName;
				
				if(!xmlElementRef.required() )
					isRequired = false;
				
				if(item != null && item instanceof JAXBElement)
				{					
					fieldClass = ((JAXBElement)item).getDeclaredType();	
					item = ((JAXBElement) item).getValue();
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

			if (item == null) {
				// set NULL value
				fieldType = IDLType.createType(Type.NULL);		
				
				
				if(!isRequired)
				{	
					fieldType = IDLType.createType(Type.OPT, fieldType);
					item = Optional.ofNullable(item);
				}

				typeMap.put(label, fieldType);
				valueMap.put(Label.createNamedLabel((String) name), item);
				continue;
			}


			if (IDLType.isDefaultType(fieldClass)) {			

				// handle float as double, Motoko does not support Float32		
				if(item instanceof Float)
				{
					if(item.getClass().isPrimitive())
						item = new Float((float) item);
					
					item = ((Float)item).doubleValue();
					
					fieldClass = Double.class;
				}				
				
				// if we do not specify type in annotation and type is one of default
				if(isOptional)
					fieldType = IDLType.createType(Type.OPT, IDLType.createType(fieldClass));
				else
					fieldType = IDLType.createType(fieldClass);				
				
				if(isArray)
					fieldType = IDLType.createType(Type.VEC, fieldType);
				
				if(!isRequired)
				{	
					fieldType = IDLType.createType(Type.OPT, fieldType);
					item = Optional.ofNullable(item);
				}
								
				typeMap.put(label, fieldType);
				valueMap.put(label, item);
				continue;
			} else if (fieldClass.isEnum())
				fieldType = IDLType.createType(Type.VARIANT);
			else	
				fieldType = IDLType.createType(Type.RECORD);

			// do nested type introspection if type is RECORD
			if (fieldType.getType() == Type.RECORD || fieldType.getType() == Type.VARIANT) {

				// handle RECORD arrays
				if (isArray) {
					Object[] nestedArray = (Object[]) item;
					List<Object> arrayValue = new ArrayList();

					fieldType = JAXBUtils.getIDLType(fieldClass.getComponentType());
					for (int i = 0; i < nestedArray.length; i++) {
						Object nestedValue = nestedArray[i];
						// if nested RECORD or VARIANT is Optional
						if (nestedValue != null && Optional.class.isAssignableFrom(nestedValue.getClass()))
							nestedValue = ((Optional) nestedValue).orElse(null);

						IDLValue nestedIdlValue = this.getIDLValue(nestedValue, fieldType.getType());

						fieldType = nestedIdlValue.getIDLType();

						arrayValue.add(nestedIdlValue.getValue());
					}

					fieldType = IDLType.createType(Type.VEC, fieldType);
					item = arrayValue.toArray();
				} else {
					Object nestedValue = item;
					// if nested RECORD or VARIANT is Optional
					if (item != null && Optional.class.isAssignableFrom(fieldClass))
						nestedValue = ((Optional) item).orElse(null);

					IDLValue nestedIdlValue = this.getIDLValue(nestedValue, fieldType.getType());

					fieldType = nestedIdlValue.getIDLType();

					item = nestedIdlValue.getValue();
				}
			} else if (isArray) {
				// handle arrays , not record types
				if (isOptional)
					fieldType = IDLType.createType(Type.OPT, fieldType);
				
				fieldType = IDLType.createType(Type.VEC, fieldType);
			} else if (isOptional) {
				// handle Optional, not record types
				fieldType = IDLType.createType(Type.OPT, fieldType);
			}

			if (fieldType.getType() == Type.NAT || fieldType.getType() == Type.INT)
				item = IDLUtils.objectToBigInt(item);

			if (fieldType.getType() == Type.PRINCIPAL)
				item = IDLUtils.objectToPrincipal(item);
			
			if(!isRequired)
			{	
				fieldType = IDLType.createType(Type.OPT, fieldType);
				item = Optional.ofNullable(item);
			}

			typeMap.put(label, fieldType);
			valueMap.put(label, item);

		}

		if(parentType == null || parentType != Type.VARIANT)
			parentType = Type.RECORD;

		// handle Enum to VARIANT
		mainloop:
		if (valueClass.isEnum()) {
			parentType = Type.VARIANT;
			Enum enumValue = (Enum) value;

			String name = enumValue.name();
			
			
			if(name == null)
				break mainloop;
								
			try {
				Field enumField = valueClass.getDeclaredField(name);
				
				if(enumField == null)
					break mainloop;
				
				if (enumField.isAnnotationPresent(XmlEnumValue.class)) 
				{
					name = enumField.getAnnotation(XmlEnumValue.class).value();	
				
					name = JAXBUtils.replaceSpecialChars(name);
				}
				
				Label enumLabel = Label.createNamedLabel(name);
				// if there is no Enum value, set it to null
				if (!valueMap.containsKey(enumLabel)) {
						typeMap.put(enumLabel, IDLType.createType(Type.NULL));
						valueMap.put(enumLabel, null);
				}
				

			} catch ( SecurityException | NoSuchFieldException e) {

			}

		}
		
		IDLType idlType = IDLType.createType(parentType, typeMap);
		
		IDLValue idlValue;
		
		if(parentType.equals(Type.VARIANT) && valueMap.isEmpty())
			idlValue = IDLValue.create(null);
		else	
			idlValue = IDLValue.create(valueMap, idlType);

		return idlValue;
	}

}
