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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ic4j.candid.annotations.Id;
import org.ic4j.candid.annotations.Ignore;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;
import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.CandidError;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.ObjectDeserializer;

public final class PojoDeserializer implements ObjectDeserializer {
	Optional<IDLType> idlType = Optional.empty();

	public static PojoDeserializer create() {
		PojoDeserializer deserializer = new PojoDeserializer();
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

		// handle BigDecimal like Double
		if (BigDecimal.class.isAssignableFrom(clazz))
			return (T) BigDecimal.valueOf((double) idlValue.getValue());

		if (idlValue.getType().isPrimitive())
			return idlValue.getValue();
		
		// manage Func and Service types
		if (idlValue.getType() == Type.FUNC || idlValue.getType() == Type.SERVICE)
			return idlValue.getValue();

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
				
				Class componentTypeClass;
				if(List.class.isAssignableFrom(clazz))
				{	
					final ParameterizedType genericSuperclass = (ParameterizedType) clazz.getGenericSuperclass();
					componentTypeClass = (Class) genericSuperclass.getActualTypeArguments()[0];
				}
				else
					componentTypeClass = clazz.getComponentType();

				Object[] result = (Object[]) Array.newInstance(componentTypeClass, array.length);

				for (int i = 0; i < array.length; i++) {
					result[i] = this.getValue(array[i], componentTypeClass);
				}
				
				if(List.class.isAssignableFrom(clazz))
					return (T) Arrays.asList(result);
				else	
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

		// handle GregorianCalendar like nanosecond timestamp
		if (GregorianCalendar.class.isAssignableFrom(clazz)) {
			long ts;
			if (value instanceof BigInteger)
				ts = ((BigInteger) value).longValue();
			else
				ts = (long) value;

			Date date = new Date(ts / 1000000);
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(date);
			return (T) cal;
		}
		
		// handle Duration
		if (Duration.class.isAssignableFrom(clazz)) {
			return (T) org.ic4j.types.Duration.deserialize((Map<Label, Object>) value);
		}		

		// handle Date like nanosecond timestamp
		if (Date.class.isAssignableFrom(clazz)) {
			long ts;
			if (value instanceof BigInteger)
				ts = ((BigInteger) value).longValue();
			else
				ts = (long) value;

			Date date = new Date(ts / 1000000);
			return (T) date;
		}
		
		if (Service.class.isAssignableFrom(clazz))
			return (T) value;
		
		if (Func.class.isAssignableFrom(clazz))
			return (T) value;		

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

			Field[] fields = clazz.getDeclaredFields();

			for (Field field : fields) {
				if (field.isAnnotationPresent(Ignore.class))
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

				Class typeClass = field.getType();

				if (field.isAnnotationPresent(Name.class))
					name = field.getAnnotation(Name.class).value();

				Label label;
				if (field.isAnnotationPresent(Id.class)) {
					int id = field.getAnnotation(Id.class).value();
					label = Label.createIdLabel((long) id);
				} else
					label = Label.createNamedLabel(name);

				Object item = valueMap.get(label);

				// handle BigDecimal like Double
				if (item != null && BigDecimal.class.isAssignableFrom(typeClass))
					item = BigDecimal.valueOf((double) item);

				if (List.class.isAssignableFrom(typeClass)) {
					if (item != null && item.getClass().isArray()) {
						Class nestedClass = (Class) ((ParameterizedType) field.getGenericType())
								.getActualTypeArguments()[0];

						List arrayValue = new ArrayList();
						
						Object[] array = (Object[]) item;

						for (Object arrayItem : array) {
							arrayValue.add((T) this.getValue(arrayItem, nestedClass));
						}
						
						item = arrayValue;
					}

				} else if (Optional.class.isAssignableFrom(typeClass)) {
					Class nestedClass = (Class) ((ParameterizedType) field.getGenericType())
							.getActualTypeArguments()[0];
					item = this.getValue(item, nestedClass);
					
					//if (!IDLType.isDefaultType(nestedClass))
					//	item = Optional.ofNullable(item);
				} else if (!IDLType.isDefaultType(typeClass))
					item = this.getValue(item, typeClass);

				try {
					// convert to proper type
					if (item != null) {

						if (item.getClass().isArray()) {
							item = IDLUtils.toArray(typeClass, (Object[]) item);
							// handle binary
							if (typeClass.isAssignableFrom(byte[].class))
								item = ArrayUtils.toPrimitive((Byte[]) item);

						} else {
							if (item.getClass().isAssignableFrom(BigInteger.class)
									&& !typeClass.isAssignableFrom(BigInteger.class))
								item = IDLUtils.bigIntToObject((BigInteger) item, typeClass);
							if (item.getClass().isAssignableFrom(Principal.class)
									&& !typeClass.isAssignableFrom(Principal.class))
								item = IDLUtils.principalToObject((Principal) item, typeClass);
						}
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
			
			try {
				if (clazz.getField(name).isAnnotationPresent(Name.class))
					name = clazz.getField(name).getAnnotation(Name.class).value();					
			} catch (NoSuchFieldException | SecurityException e) {
			}

			Label namedLabel = Label.createNamedLabel(name);

			if (label.equals(namedLabel))
				return Enum.valueOf(clazz, enumName);
		}
		// cannot find variant
		return null;
	}

}
