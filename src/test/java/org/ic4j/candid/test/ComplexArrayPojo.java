package org.ic4j.candid.test;


import java.math.BigInteger;
import java.util.Arrays;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.types.Type;

public class ComplexArrayPojo {
	@Field(Type.BOOL)
	@Name("bar")
	public Boolean[] bar;
	
	@Field(Type.INT)
	@Name("foo")
	public BigInteger[] foo;
	
	@Field(Type.RECORD)
	@Name("pojo")
	public Pojo[] pojo;


	// Just for testing purposes, JUnit uses equals
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComplexArrayPojo other = (ComplexArrayPojo) obj;
		if (!Arrays.equals(bar, other.bar))
			return false;
		if (!Arrays.equals(foo, other.foo))
			return false;
		if (!Arrays.equals(pojo, other.pojo))
			return false;
		return true;
	}
	
}
