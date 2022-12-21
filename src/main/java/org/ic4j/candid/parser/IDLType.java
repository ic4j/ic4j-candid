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

package org.ic4j.candid.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;

import org.ic4j.candid.CandidError;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;

public final class IDLType {
	Type type;
	IDLType innerType;
	
	Map<Label,IDLType> typeMap = new TreeMap<Label,IDLType>();
	
	public List<IDLType> args = new ArrayList<IDLType>();
	public List<IDLType> rets = new ArrayList<IDLType>();
	public List<Mode> modes = new ArrayList<Mode>();
	Map<String,IDLType> meths = new TreeMap<String,IDLType>();

	String name;
	String description;
	

	void addInnerTypes(Object value) {
		if (value == null)
			return;
		if (value instanceof Optional)
		{
			if( ((Optional) value).isPresent())
				this.innerType = IDLType.createType(((Optional) value).get());
		}
		else if (value.getClass().isArray()) {
			//Class clazz = ((Object[]) value).getClass().getComponentType();
			
			Object[] arrayValue = (Object[]) value;
			
			if(arrayValue.length > 0)
				this.innerType = IDLType.createType(arrayValue[0]);
			else
			{	
				Class clazz = value.getClass().getComponentType();
				this.innerType = IDLType.createType(clazz);
			}
		}
		else if (value instanceof Map)
		{
			
			this.typeMap = new TreeMap<Label,IDLType>();
			
			for(Object key : ((Map) value).keySet())
			{
				Label label;
				if(key instanceof Label)
					label = (Label)key;								
				else
					throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Invalid Map Key");
				
				IDLType idlType = IDLType.createType(((Map) value).get(key));
				
				this.typeMap.put(label, idlType);	
			}
		}		

	}

	void addInnerType(Class clazz) {
		if (clazz == null)
			return;
		else if (clazz.isArray()) {
			clazz = clazz.getComponentType();
			this.innerType = IDLType.createType(clazz);
		}

	}

	public static IDLType createType(Type type) {
		IDLType idlType = new IDLType();

		idlType.type = type;

		return idlType;
	}
	
	public static IDLType createType(List<IDLType> args, List<IDLType> rets, List<Mode> modes) {
		IDLType idlType = new IDLType();

		idlType.type = Type.FUNC;
		idlType.args = args;
		idlType.rets = rets;
		idlType.modes = modes;	
		return idlType;
	}
	
	public static IDLType createType(Map<String,IDLType> meths) {
		IDLType idlType = new IDLType();

		idlType.type = Type.SERVICE;
		idlType.meths = meths;
	
		return idlType;
	}	

	public static IDLType createType(Type type, IDLType innerType) {
		IDLType idlType = new IDLType();

		idlType.type = type;
		
		if(type == Type.OPT || type == Type.VEC)
			if(innerType != null)
				idlType.innerType = innerType;

		return idlType;
	}
	
	
	public static IDLType createType(Type type, Map<Label,IDLType> typeMap) {
		IDLType idlType = new IDLType();

		idlType.type = type;
		
		
		if(type == Type.RECORD || type == Type.VARIANT)
			if(typeMap != null)
				idlType.typeMap = typeMap;

		return idlType;
	}
	
	public static IDLType createType(Object value, Type type) {
		IDLType idlType = new IDLType();

		idlType.type = type;
		
		idlType.addInnerTypes(value);

		return idlType;
	}

	public static IDLType createType(Object value) {
		IDLType idlType = new IDLType();

		idlType.type = Type.NULL;

		if (value == null)
			return idlType;

		if (value instanceof Boolean)
			idlType.type = Type.BOOL;
		else if (value instanceof BigInteger)
			idlType.type = Type.INT;
		else if (value instanceof Byte)
			idlType.type = Type.INT8;
		else if (value instanceof Short)
			idlType.type = Type.INT16;
		else if (value instanceof Integer)
			idlType.type = Type.INT32;
		else if (value instanceof Long)
			idlType.type = Type.INT64;
		else if (value instanceof Float)
			idlType.type = Type.FLOAT32;
		else if (value instanceof Double)
			idlType.type = Type.FLOAT64;
		else if (value instanceof String)
			idlType.type = Type.TEXT;
		else if (value instanceof Optional)
			idlType.type = Type.OPT;
		else if (value.getClass().isArray())
			idlType.type = Type.VEC;
		else if (value instanceof Map)
			idlType.type = Type.RECORD;		
		else if (value instanceof Principal)
			idlType.type = Type.PRINCIPAL;
		else if (value instanceof Func)
			idlType.type = Type.FUNC;		
		else if (value instanceof Service)
			idlType.type = Type.SERVICE;
		idlType.addInnerTypes(value);

		return idlType;

	}

	public static IDLType createType(Class clazz) {
		IDLType idlType = new IDLType();

		idlType.type = Type.NULL;

		if (clazz == Boolean.class)
			idlType.type = Type.BOOL;
		else if (clazz == BigInteger.class)
			idlType.type = Type.INT;
		else if (clazz == Byte.class)
			idlType.type = Type.INT8;
		else if (clazz == Short.class)
			idlType.type = Type.INT16;
		else if (clazz == Integer.class)
			idlType.type = Type.INT32;
		else if (clazz == Long.class)
			idlType.type = Type.INT64;
		else if (clazz == Float.class)
			idlType.type = Type.FLOAT32;
		else if (clazz == Double.class)
			idlType.type = Type.FLOAT64;
		else if (clazz == String.class)
			idlType.type = Type.TEXT;
		else if (clazz == Optional.class)
			idlType.type = Type.OPT;
		else if (clazz.isArray())
			idlType.type = Type.VEC;
		else if (Map.class.isAssignableFrom(clazz))
			idlType.type = Type.RECORD;			
		else if (clazz == Principal.class)
			idlType.type = Type.PRINCIPAL;
		else if (clazz == Func.class)
			idlType.type = Type.FUNC;	
		else if (clazz == Service.class)
			idlType.type = Type.SERVICE;		

		idlType.addInnerType(clazz);

		return idlType;

	}
	
	public static boolean isDefaultType(Class clazz)
	{
		if( Number.class.isAssignableFrom(clazz) ||  Boolean.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz) || Optional.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz) || Principal.class.isAssignableFrom(clazz) )
			return true;
		else
			return false;
	}
	
	public static boolean isPrimitiveType(Class clazz)
	{
		if( Number.class.isAssignableFrom(clazz) ||  Boolean.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz) || Principal.class.isAssignableFrom(clazz) )
			return true;
		else
			return false;
	}	
	
	public Map<String, IDLType> flatten()
	{
		Map<String, IDLType> flattenType = new LinkedHashMap<String, IDLType>();
		
		if(this.innerType != null)
		{
			Map<String, IDLType> flattenInnerType = this.innerType.flatten();
			
			for(String name : flattenInnerType.keySet())
				flattenType.put(name, flattenInnerType.get(name));
		}
		
		if(this.typeMap != null)
		{
			for(Label label : this.typeMap.keySet())
			{
				IDLType itemType = this.typeMap.get(label);
				
				if(itemType != null)
				{
					Map<String, IDLType> flattenItemType = itemType.flatten();
					
					for(String name : flattenItemType.keySet())
						flattenType.put(name, flattenItemType.get(name));
				}
			}
		}
		
		if(this.name != null)
			flattenType.put(name, this);
		
		return flattenType;
	}
	
	public Type getType() {
		return this.type;
	}

	public IDLType getInnerType() {
		return this.innerType;
	}
	
	public Map<Label,IDLType> getTypeMap()
	{
		return this.typeMap;
	}

	/**
	 * @return the args
	 */
	public List<IDLType> getArgs() {
		return args;
	}

	/**
	 * @return the rets
	 */
	public List<IDLType> getRets() {
		return rets;
	}

	/**
	 * @return the modes
	 */
	public List<Mode> getModes() {
		return modes;
	}

	/**
	 * @return the meths
	 */
	public Map<String,IDLType> getMeths() {
		return meths;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	


}
