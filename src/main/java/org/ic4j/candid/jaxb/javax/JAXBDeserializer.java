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

package org.ic4j.candid.jaxb.javax;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Principal;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.ic4j.candid.CandidError;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.ObjectDeserializer;

public final class JAXBDeserializer implements ObjectDeserializer {
	Optional<IDLType> idlType = Optional.empty();
	
	public static JAXBDeserializer create() {
		JAXBDeserializer deserializer = new JAXBDeserializer();
		return deserializer;
	}
	
	public void setIDLType(IDLType idlType)
	{
		this.idlType = Optional.ofNullable(idlType);
	}
	
	
	public Class<?> getDefaultResponseClass() {
		return Object.class;
	}

	@Override
	public <T> T deserialize(IDLValue idlValue, Class<T> clazz) {

		if (idlValue == null)
			return null;
		

		// handle OPT
		if (idlValue.getType() == Type.OPT) {
			Optional optionalValue = (Optional) idlValue.getValue();

			if (Optional.class.isAssignableFrom(clazz))
				return (T) optionalValue;
			// if innerType is primitive
			if (idlValue.getIDLType().getInnerType().getType().isPrimitive())
				return (T) optionalValue.orElse(null);
			else {
				T result = null;

				if (optionalValue.isPresent())
					result = this.getValue(optionalValue.get(), clazz);

				return result;
			}

		}

		// handle arrays
		if (idlValue.getType() == Type.VEC) {
			// if innerType is primitive
			if (IDLType.isPrimitiveType(clazz))
				return idlValue.getValue();
			else if (idlValue.getValue().getClass().isArray()) {
				Object[] array = idlValue.getValue();

				if (clazz.isAssignableFrom(byte[].class))
					return (T) ArrayUtils.toPrimitive((Byte[]) array);

				if (clazz.isAssignableFrom(Byte[].class))
					return (T) (Byte[]) array;

				Object[] result = (Object[]) Array.newInstance(clazz.getComponentType(), array.length);

				for (int i = 0; i < array.length; i++) {
					result[i] = this.getValue(array[i], clazz.getComponentType());
				}

				return (T) result;
			}
		}

		// handle RECORD and VARIANT
		if (idlValue.getType() == Type.RECORD || idlValue.getType() == Type.VARIANT) {
			return (T) this.getValue(idlValue.getValue(), clazz);
		}

		return idlValue.getValue();
	}

	<T> T getValue(Object value, Class<T> clazz) {
		if (value == null)
			return null;
		
		if(clazz == null)
			clazz = (Class<T>) value.getClass();
		
		if(JAXBElement.class.isAssignableFrom(clazz) )
		{
			try {
				JAXBElement jaxbValue = (JAXBElement) clazz.newInstance();
				value = this.getValue(value, jaxbValue.getDeclaredType());
				jaxbValue.setValue(value);
				return (T) jaxbValue;
			} catch (InstantiationException | IllegalAccessException e) {
				return null;
			}
		}

		// handle XMLGregorianCalendar like nanosecond timestamp
		if (XMLGregorianCalendar.class.isAssignableFrom(clazz)) {
			long ts;
			if (value instanceof BigInteger)
				ts = ((BigInteger) value).longValue();
			else
				ts = (long) value;

			Date date = new Date(ts / 1000000);
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(date);
			try {
				return (T) DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
			} catch (DatatypeConfigurationException e) {
				return null;
			}
		}
		
		// handle Duration
		if (Duration.class.isAssignableFrom(clazz)) {
			return (T) org.ic4j.types.Duration.deserializeXML((Map<Label, Object>) value);
		}
		
		// handle float as double, Motoko does not support Float32	
		if(Float.class.isAssignableFrom(clazz) && value instanceof Double)
		{
			value = ((Double)value).floatValue();
		}		

		if (IDLType.isPrimitiveType(value.getClass()))
			return (T) value;

		// handle arrays
		if (value.getClass().isArray()) {
			List<T> arrayValue = new ArrayList();

			Object[] array = (Object[]) value;

			for (Object item : array) {
				arrayValue.add((T) this.getValue(item, clazz.getComponentType()));
			}

			return (T) arrayValue.toArray();
		}

		// handle Optional
		if (Optional.class.isAssignableFrom(value.getClass())) {
			Optional optionalValue = (Optional) value;

			if (Optional.class.isAssignableFrom(clazz))
				return (T) optionalValue;

			if (optionalValue.isPresent()) {
				value = this.getValue(optionalValue.get(), clazz);
				return (T) Optional.ofNullable(value);
			} else
				return (T) optionalValue;
		}

		// handle Map, match with clazz type
		if (Map.class.isAssignableFrom(value.getClass())) {
			Map<Label, Object> valueMap = (Map<Label, Object>) value;
			T pojoValue = null;
			// if the output is Enum
			if (Enum.class.isAssignableFrom(clazz)) {
				// no enum value
				if (valueMap.isEmpty())
					return null;

				Label label = valueMap.keySet().iterator().next();

				pojoValue = (T) this.initializeEnum(label, (Class<Enum>) clazz);

				if (pojoValue == null)
					return null;
			} else {
				if (clazz.getConstructors().length == 0)
					throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
							"Cannot instantiate class " + clazz.getCanonicalName() + ". Missing constructor.");
				try {
					for (Constructor constructor : clazz.getConstructors()) {
						if (constructor.getParameterCount() == 0) {
							constructor.setAccessible(true);
							pojoValue = (T) constructor.newInstance();
							continue;
						}
					}
					if (pojoValue == null)
						throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
								"Cannot instantiate class " + clazz.getCanonicalName() + ".");

				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, e, e.getLocalizedMessage());
				}
			}

			Field[] fields = IDLUtils.getAllFields(clazz);

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

				Class fieldClass = field.getType();
				
				if(fieldClass.isPrimitive())
					fieldClass = ClassUtils.primitiveToWrapper(fieldClass);
				
				boolean isOptional = false;
				if(Optional.class.isAssignableFrom(fieldClass))
				{
					fieldClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
					isOptional = true;
				}
				
				boolean isRequired = true;

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
					
					if(!xmlElementRef.required())
						isRequired = false;
				}

				if (field.isAnnotationPresent(XmlAttribute.class)) {
					XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);

					String xmlName = xmlAttribute.name();

					if (xmlName != null)
						name = xmlName;
					
					if(!xmlAttribute.required())
						isRequired = false;				
				}

				Label label = Label.createNamedLabel(name);

				Object item = valueMap.get(label);
				
				if(!isRequired && item != null && item instanceof Optional)
					item = ((Optional)item).orElse(null);
				
				if(isOptional && !Optional.class.isAssignableFrom(fieldClass) && item instanceof Optional)
					item = ((Optional)item).orElse(null);
				
				// handle BigDecimal like Double
				if (item != null && BigDecimal.class.isAssignableFrom(fieldClass)) 	
					item = BigDecimal.valueOf((double) item).stripTrailingZeros();

				if (List.class.isAssignableFrom(fieldClass)) {
					if (item != null && item.getClass().isArray()) {
						Class nestedClass = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
						
						List arrayValue = new ArrayList();
						
						Object[] array = (Object[]) item;

						for (Object arrayItem : array) {
							arrayValue.add((T) this.getValue(arrayItem, nestedClass));
						}
						
						item = arrayValue;
					}

				}else if (Optional.class.isAssignableFrom(fieldClass)) {
					Class nestedClass = (Class) ((ParameterizedType) field.getGenericType())
							.getActualTypeArguments()[0];
					item = this.getValue(item, nestedClass);
					
					if (!IDLType.isDefaultType(nestedClass))
						item = Optional.ofNullable(item);
				} else if (!IDLType.isDefaultType(fieldClass))
					item = this.getValue(item, fieldClass);

				try {
					// convert to proper type
					if (item != null) {

						if (item.getClass().isArray()) {
							item = IDLUtils.toArray(fieldClass, (Object[]) item);
							// handle binary
							if (fieldClass.isAssignableFrom(byte[].class))
								item = ArrayUtils.toPrimitive((Byte[]) item);

						} else {
							if (item.getClass().isAssignableFrom(BigInteger.class)
									&& !fieldClass.isAssignableFrom(BigInteger.class))
								item = IDLUtils.bigIntToObject((BigInteger) item, fieldClass);
							if (item.getClass().isAssignableFrom(Principal.class)
									&& !fieldClass.isAssignableFrom(Principal.class))
								item = IDLUtils.principalToObject((Principal) item, fieldClass);
						}
					}
					// handle float as double, Motoko does not support Float32		
					if(fieldClass.isAssignableFrom(Float.class) && item instanceof Double)
					{
						item = ((Double)item).floatValue();
					}
					
					field.set(pojoValue, item);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					continue;
				}
			}

			return (T) pojoValue;

		}

		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Undefined type " + clazz.getName());
	}

	Enum initializeEnum(Label label, Class<Enum> clazz) {
		Enum[] constants = clazz.getEnumConstants();

		for (Enum constant : constants) {
			String enumName = constant.name();
			
			String name = enumName;
			
			if(name == null)
				continue;

			try {			
				Field enumField = clazz.getDeclaredField(name);
						
				if(enumField == null)
					continue;
			

				if (enumField.isAnnotationPresent(XmlEnumValue.class))
				{
					name = enumField.getAnnotation(XmlEnumValue.class).value();	
				
					name = JAXBUtils.replaceSpecialChars(name);
				}					
			} catch (SecurityException | NoSuchFieldException e) {
			}

			Label namedLabel = Label.createNamedLabel(name);

			if (label.equals(namedLabel))
				return Enum.valueOf(clazz, enumName);
		}
		// cannot find variant
		return null;
	}

}
