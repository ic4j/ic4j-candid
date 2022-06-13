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
import java.math.BigInteger;
import java.util.ArrayList;
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
import org.ic4j.types.Principal;
import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.CandidError;
import org.ic4j.candid.IDLUtils;
import org.ic4j.candid.ObjectDeserializer;

public final class PojoDeserializer implements ObjectDeserializer {

	public static PojoDeserializer create() {
		PojoDeserializer deserializer = new PojoDeserializer();
		return deserializer;
	}

	@Override
	public <T> T deserialize(IDLValue idlValue, Class<T> clazz) {

		if (idlValue == null)
			return null;

		if (idlValue.getType().isPrimitive()) {
			return idlValue.getValue();
		}

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

				if (!IDLType.isDefaultType(typeClass))
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
			String name = constant.name();

			Label namedLabel = Label.createNamedLabel(name);

			if (label.equals(namedLabel))
				return Enum.valueOf(clazz, name);
		}
		// cannot find variant
		return null;
	}

}
