package org.ic4j.candid;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.types.Principal;

public class IDLUtils {
	public static BigInteger UINT_MAX = BigInteger.valueOf(4294967295l + 1);
	
	public static long idlHash(String value) {
		BigInteger hash = BigInteger.ZERO;

		if (value != null) {
			byte[] bytes = value.getBytes();

			for (byte b : bytes)
			{	
				hash = hash.multiply(BigInteger.valueOf(223)).mod(UINT_MAX);				
				hash = hash.add(BigInteger.valueOf(Byte.toUnsignedInt(b))).mod(UINT_MAX);			
			}
		}

		return hash.longValue();
	}


	public static <T> T[] toArray(Class<T> clazz, Object[] sourceArray) {
		List<T> list = new ArrayList<T>();
		
		if (sourceArray.length == 0)
			return (T[]) list.toArray();

		for (int i = 0; i < sourceArray.length; i++)
			list.add((T) sourceArray[i]);

		Class arrayClazz = list.get(0).getClass();
		T[] array = (T[]) java.lang.reflect.Array.newInstance(arrayClazz, list.size());
		
		// if we need to convert BigInteger to different class
		if(BigInteger.class.isAssignableFrom(arrayClazz) && !BigInteger.class.isAssignableFrom(clazz.getComponentType()))
			for(int i = 0; i < sourceArray.length; i++)
				array[i] = (T) IDLUtils.bigIntToObject((BigInteger)sourceArray[i], clazz.getComponentType());
			
		// if we need to convert Principal to different class
		if(Principal.class.isAssignableFrom(arrayClazz) && !Principal.class.isAssignableFrom(clazz.getComponentType()))
			for(int i = 0; i < sourceArray.length; i++)
				array[i] = (T) IDLUtils.principalToObject((Principal)sourceArray[i], clazz.getComponentType());

		return list.toArray(array);

	}
	
	public static <T> T bigIntToObject(BigInteger value, Class<T> clazz)
	{
		if(value == null)
			return (T) value;		
		if(BigInteger.class.isAssignableFrom(clazz) )
			return (T)value;
		if(String.class.isAssignableFrom(clazz) )
			return (T)  value.toString();	
		if(clazz.isArray() && byte[].class.isAssignableFrom(clazz) )
			return (T)  value.toByteArray();		
		if(Long.class.isAssignableFrom(clazz) )
			return (T) Long.valueOf( value.longValue());
		if(Integer.class.isAssignableFrom(clazz) )
			return (T) Integer.valueOf( value.intValue());
		if(Short.class.isAssignableFrom(clazz) )
			return (T) Short.valueOf( value.shortValue());	
		if(Double.class.isAssignableFrom(clazz) )
			return (T) Double.valueOf( value.doubleValue());
		if(Float.class.isAssignableFrom(clazz) )
			return (T) Float.valueOf( value.floatValue());	
		if(Byte.class.isAssignableFrom(clazz) )
			return (T) Byte.valueOf( value.byteValue());		
		
		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Cannot convert INT to Java class " + clazz.getName());
	}
	
	public static BigInteger objectToBigInt(Object value)
	{
		if(value == null)
			return null;
		if(value instanceof BigInteger)
			return (BigInteger) value;
		
		if(value instanceof byte[])
			return new BigInteger((byte[]) value);
		
		return new BigInteger(value.toString());
	}
	
	public static <T> T bigDecimalToObject(BigDecimal value, Class<T> clazz)
	{
		if(value == null)
			return (T) value;		
		if(BigInteger.class.isAssignableFrom(clazz) )
			return (T)value;
		if(String.class.isAssignableFrom(clazz) )
			return (T)  value.toString();			
		if(Long.class.isAssignableFrom(clazz) )
			return (T) Long.valueOf( value.longValue());
		if(Integer.class.isAssignableFrom(clazz) )
			return (T) Integer.valueOf( value.intValue());
		if(Short.class.isAssignableFrom(clazz) )
			return (T) Short.valueOf( value.shortValue());	
		if(Double.class.isAssignableFrom(clazz) )
			return (T) Double.valueOf( value.doubleValue());
		if(Float.class.isAssignableFrom(clazz) )
			return (T) Float.valueOf( value.floatValue());	
		if(Byte.class.isAssignableFrom(clazz) )
			return (T) Byte.valueOf( value.byteValue());		
		
		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Cannot convert INT to Java class " + clazz.getName());
	}
	
	public static BigDecimal objectToBigDecimal(Object value)
	{
		if(value == null)
			return null;
		if(value instanceof BigDecimal)
			return (BigDecimal) value;
		
		return new BigDecimal(value.toString());
	}	
	
	public static <T> T principalToObject(Principal value, Class<T> clazz)
	{
		if(value == null)
			return (T) value;		
		if(Principal.class.isAssignableFrom(clazz) )
			return (T)value;
		if(String.class.isAssignableFrom(clazz) )
			return (T)  value.toString();	
		if(clazz.isArray() && byte[].class.isAssignableFrom(clazz) )
			return (T)  value.getValue();
		
		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Cannot convert PRINCIPAL to Java class " + clazz.getName());
	}
	
	public static Principal objectToPrincipal(Object value)
	{
		if(value == null)
			return null;
		if(value instanceof Principal)
			return (Principal) value;
		
		if(value instanceof byte[])
			return Principal.from((byte[]) value);
		
		if(value instanceof String)
			return Principal.fromString((String) value);
		
		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Cannot convert Java class " + value.getClass().getName() + " to PRINCIPAL");
		
	}	

}
