package org.ic4j.candid.test;


import java.math.BigInteger;
import java.util.Optional;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.types.Type;

public class ComplexOptionalPojo {
	@Field(Type.BOOL)
	@Name("bar")
	public Optional<Boolean> bar;
	
	@Field(Type.INT)
	@Name("foo")
	public Optional<BigInteger> foo;
	
	@Field(Type.RECORD)
	@Name("pojo")
	public Optional<Pojo> pojo;

	// Just for testing purposes, JUnit uses equals
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComplexOptionalPojo other = (ComplexOptionalPojo) obj;
		if (bar == null) {
			if (other.bar != null)
				return false;
		} else if (!bar.equals(other.bar))
			return false;
		if (foo == null) {
			if (other.foo != null)
				return false;
		} else if (!foo.equals(other.foo))
			return false;
		if (pojo == null) {
			if (other.pojo != null)
				return false;
		} else if (!pojo.equals(other.pojo))
			return false;
		return true;
	}
	
	
}
