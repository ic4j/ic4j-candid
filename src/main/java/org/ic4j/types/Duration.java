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

package org.ic4j.types;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;

public class Duration {
	static final String SECS_PART = "seconds";
	static final String NANOS_PART = "nanoseconds";
	static final String TYPE_NAME = "Duration";
	
	
	public static IDLType getIDLType()
	{
		Map<Label,IDLType> durationType = new HashMap<Label,IDLType>();
		
		durationType.put(Label.createNamedLabel(SECS_PART), IDLType.createType(Type.NAT));
		durationType.put(Label.createNamedLabel(NANOS_PART), IDLType.createType(Type.NAT));	
		
		IDLType idlType = IDLType.createType(Type.VARIANT, durationType);	
		
		idlType.setJavaType(java.time.Duration.class);
		
		idlType.setName(TYPE_NAME);
		
		return idlType;
	}
	
	public static IDLValue serialize(java.time.Duration duration)
	{
		long secs = duration.getSeconds();
		int nanos = duration.getNano();

		Map<Label, Object> durationValue = new HashMap<Label, Object>();
		
		durationValue.put(Label.createNamedLabel(SECS_PART), BigInteger.valueOf(secs));
		durationValue.put(Label.createNamedLabel(NANOS_PART), BigInteger.valueOf(nanos));
		
		IDLValue idlValue = IDLValue.create(durationValue, getIDLType());
		
		return idlValue;		
	}
	
	public static java.time.Duration deserialize(Map<Label, Object> durationValue)
	{		
		long secs = ((BigInteger) durationValue.get(Label.createNamedLabel(SECS_PART))).longValue();
		int nanos = ((BigInteger) durationValue.get(Label.createNamedLabel(NANOS_PART))).intValue();		
		
		return java.time.Duration.ofSeconds(secs, nanos);	
	}
	
	public static IDLValue serializeXML(javax.xml.datatype.Duration xmlDuration)
	{
		java.time.Duration duration = java.time.Duration.parse(xmlDuration.toString());
		
		return serialize(duration);
	}
	
	public static javax.xml.datatype.Duration deserializeXML(Map<Label, Object> durationValue)

	{			
		try {
			return DatatypeFactory.newInstance().newDuration(deserialize(durationValue).toMillis());
		} catch (DatatypeConfigurationException e) {
			return null;
		}

	}	

}
