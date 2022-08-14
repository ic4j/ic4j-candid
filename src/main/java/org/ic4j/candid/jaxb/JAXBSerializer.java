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

package org.ic4j.candid.jaxb;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.ObjectSerializer;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;

public final class JAXBSerializer implements ObjectSerializer {

	public static JAXBSerializer create() {
		JAXBSerializer deserializer = new JAXBSerializer();
		return deserializer;
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

		// handle XMLGregorianCalendar like nanosecond timestamp
		if (value instanceof XMLGregorianCalendar) {
			value = ((XMLGregorianCalendar) value).toGregorianCalendar().getTimeInMillis() * 1000000;

			return IDLValue.create(value, Type.INT);
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

		Field[] fields = valueClass.getDeclaredFields();

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

			if (!field.isAnnotationPresent(XmlElement.class) && !field.isAnnotationPresent(XmlAttribute.class)
					&& !field.isAnnotationPresent(XmlAnyElement.class) && !field.isAnnotationPresent(XmlValue.class))
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
			if (item != null && item instanceof List) {
				item = ((List) item).toArray();

				typeClass = item.getClass();
			}

			// handle BigDecimal like Double
			if (item != null && BigDecimal.class.isAssignableFrom(typeClass))
				item = ((BigDecimal) item).doubleValue();

			IDLType fieldType;

			if (field.isAnnotationPresent(XmlElement.class)) {
				XmlElement xmlElement = field.getAnnotation(XmlElement.class);

				String xmlName = xmlElement.name();

				if (xmlName != null)
					name = xmlName;
				
				if(!xmlElement.required())
					item = Optional.ofNullable(item);
			}

			if (field.isAnnotationPresent(XmlAttribute.class)) {
				XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);

				String xmlName = xmlAttribute.name();

				if (xmlName != null)
					name = xmlName;
				
				if(!xmlAttribute.required())
					item = Optional.ofNullable(item);				
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

				typeMap.put(label, fieldType);
				valueMap.put(Label.createNamedLabel((String) name), item);
				continue;
			}

			boolean isArray = typeClass.isArray();
			boolean isOptional = Optional.class.isAssignableFrom(typeClass);

			if (IDLType.isDefaultType(typeClass)) {
				// if we do not specify type in annotation and type is one of default
				fieldType = IDLType.createType(item);
				typeMap.put(label, fieldType);
				valueMap.put(label, item);
				continue;
			} else if (typeClass.isEnum())
				fieldType = IDLType.createType(Type.VARIANT);
			else	
				fieldType = IDLType.createType(Type.RECORD);

			// do nested type introspection if type is RECORD
			if (fieldType.getType() == Type.RECORD || fieldType.getType() == Type.VARIANT) {

				// handle RECORD arrays
				if (isArray) {
					Object[] nestedArray = (Object[]) item;
					List<Object> arrayValue = new ArrayList();

					fieldType = this.getIDLType(typeClass.getComponentType());
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
					if (item != null && Optional.class.isAssignableFrom(typeClass))
						nestedValue = ((Optional) item).orElse(null);

					IDLValue nestedIdlValue = this.getIDLValue(nestedValue, fieldType.getType());

					fieldType = nestedIdlValue.getIDLType();

					item = nestedIdlValue.getValue();
				}
			} else if (isArray) {
				// handle arrays , not record types
				fieldType = IDLType.createType(Type.VEC, fieldType);
			} else if (isOptional) {
				// handle Optional, not record types
				fieldType = IDLType.createType(Type.OPT, fieldType);
			}

			if (fieldType.getType() == Type.NAT || fieldType.getType() == Type.INT)
				item = IDLUtils.objectToBigInt(item);

			if (fieldType.getType() == Type.PRINCIPAL)
				item = IDLUtils.objectToPrincipal(item);

			typeMap.put(label, fieldType);
			valueMap.put(label, item);

		}

		if(parentType == null || parentType != Type.VARIANT)
			parentType = Type.RECORD;

		// handle Enum to VARIANT
		if (valueClass.isEnum()) {
			parentType = Type.VARIANT;
			Enum enumValue = (Enum) value;

			String name = enumValue.name();

			try {
				if (valueClass.getField(name).isAnnotationPresent(XmlEnumValue.class)) 
					name = valueClass.getField(name).getAnnotation(XmlEnumValue.class).value();
				
				Label enumLabel = Label.createNamedLabel(name);
				// if there is no Enum value, set it to null
				if (!valueMap.containsKey(enumLabel)) {
						typeMap.put(enumLabel, IDLType.createType(Type.NULL));
						valueMap.put(enumLabel, null);
				}
				

			} catch (NoSuchFieldException | SecurityException e) {

			}

		}
		
	

		IDLType idlType = IDLType.createType(parentType, typeMap);
		IDLValue idlValue = IDLValue.create(valueMap, idlType);

		return idlValue;
	}

	public IDLType getIDLType(Class valueClass) {
		
		// handle null values
		if (valueClass == null)
			return IDLType.createType(Type.NULL);
		
		// handle BigDecimal like Double
		if (BigDecimal.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.FLOAT64);		

		if (IDLType.isDefaultType(valueClass))
			return IDLType.createType(valueClass);

		if (Optional.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.OPT);

		if (List.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.VEC);

		// handle XMLGregorianCalendar like nanosecond timestamp
		if (XMLGregorianCalendar.class.isAssignableFrom(valueClass))
			return IDLType.createType(Type.INT);
		

		Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();

		Field[] fields = valueClass.getDeclaredFields();

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
			
			if (!field.isAnnotationPresent(XmlElement.class) && !field.isAnnotationPresent(XmlAttribute.class) 
					&& !field.isAnnotationPresent(XmlAnyElement.class) && !field.isAnnotationPresent(XmlValue.class))
				continue;			

			Class typeClass = field.getType();
			
			boolean isArray = typeClass.isArray();
			boolean isOptional = Optional.class.isAssignableFrom(typeClass);			
			
			IDLType fieldType;
			
			if (field.isAnnotationPresent(XmlAnyElement.class))
				fieldType = IDLType.createType(Type.RESERVED);
			else		
				fieldType = this.getIDLType(typeClass);

			if (field.isAnnotationPresent(XmlElement.class)) {
				XmlElement xmlElement = field.getAnnotation(XmlElement.class);

				String xmlName = xmlElement.name();

				if (xmlName != null)
					name = xmlName;
				
				if(!xmlElement.required())
					isOptional = true;
			}

			if (field.isAnnotationPresent(XmlAttribute.class)) {
				XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);

				String xmlName = xmlAttribute.name();

				if (xmlName != null)
					name = xmlName;
				
				if(!xmlAttribute.required())
						isOptional = true;
			}
			
			if (field.isAnnotationPresent(XmlEnumValue.class)) {
				String xmlName = field.getAnnotation(XmlEnumValue.class).value();

				if (xmlName != null)
					name = xmlName;
			}			

			Label label = Label.createNamedLabel((String) name);

			if (IDLType.isDefaultType(typeClass) || XMLGregorianCalendar.class.isAssignableFrom(typeClass)
					|| BigDecimal.class.isAssignableFrom(typeClass)) {
				// if we do not specify type in annotation and type is one of default
				if (isOptional && fieldType.getType() != Type.OPT) 
					// handle Optional
					fieldType = IDLType.createType(Type.OPT, fieldType);
				typeMap.put(label, fieldType);
				continue;
			} else if (List.class.isAssignableFrom(typeClass)) {
				isArray = true;
				typeClass = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

				fieldType = this.getIDLType(typeClass);
			}

			// do nested type introspection if type is RECORD
			if (fieldType.getType() == Type.RECORD || fieldType.getType() == Type.VARIANT) {
				String className = typeClass.getSimpleName();

				if (typeClass.isAnnotationPresent(XmlType.class)) {
					XmlType xmlType = (XmlType) typeClass.getAnnotation(XmlType.class);

					if (xmlType.name() != null)
						className = xmlType.name();
				}
				
				fieldType.setName(className);

				// handle RECORD arrays
				if (isArray) 
					fieldType = IDLType.createType(Type.VEC, fieldType);		

			} else if (isArray) 
				// handle arrays , not record types
				fieldType = IDLType.createType(Type.VEC, fieldType);
			
			if (isOptional && fieldType.getType() != Type.OPT) 
				// handle Optional
				fieldType = IDLType.createType(Type.OPT, fieldType);
			

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
						name = enumClass.getField(name).getAnnotation(XmlEnumValue.class).value();	
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

			if (xmlType.name() != null)
				className = xmlType.name();
		}

		idlType.setName(className);

		return idlType;
	}

}
