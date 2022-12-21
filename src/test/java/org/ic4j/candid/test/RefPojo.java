package org.ic4j.candid.test;


import java.math.BigInteger;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.annotations.Ignore;
import org.ic4j.candid.annotations.Modes;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;

public class RefPojo {
	@Field(Type.BOOL)
	@Name("bar")
	public Boolean bar;

	@Field(Type.INT)
	@Name("foo")
	public BigInteger foo;
	
	@Ignore
	public String dummy;
	
	@Field(Type.PRINCIPAL)
	@Name("princ")
	public Principal principal;
	
	@Field(Type.SERVICE)
	@Name("serv")
	public Service service;	
	
	public Func func0;
	
	@Field(Type.FUNC)
	@Name("func1")	
	public Func func1;	

	@Field(Type.FUNC)
	@Name("func2")	
	@Modes({Mode.ONEWAY})
	public Func func2;
	
	@Modes(Mode.ONEWAY)
	public Func func3;	
	
	@Field(Type.FUNC)
	@Name("func4")	
	@Modes({Mode.QUERY})
	public Func func4;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bar == null) ? 0 : bar.hashCode());
		result = prime * result + ((dummy == null) ? 0 : dummy.hashCode());
		result = prime * result + ((foo == null) ? 0 : foo.hashCode());
		result = prime * result + ((func0 == null) ? 0 : func0.hashCode());
		result = prime * result + ((func1 == null) ? 0 : func1.hashCode());
		result = prime * result + ((func2 == null) ? 0 : func2.hashCode());
		result = prime * result + ((func3 == null) ? 0 : func3.hashCode());
		result = prime * result + ((func4 == null) ? 0 : func4.hashCode());
		result = prime * result + ((principal == null) ? 0 : principal.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RefPojo other = (RefPojo) obj;
		if (bar == null) {
			if (other.bar != null)
				return false;
		} else if (!bar.equals(other.bar))
			return false;
		if (dummy == null) {
			if (other.dummy != null)
				return false;
		} else if (!dummy.equals(other.dummy))
			return false;
		if (foo == null) {
			if (other.foo != null)
				return false;
		} else if (!foo.equals(other.foo))
			return false;
		if (func0 == null) {
			if (other.func0 != null)
				return false;
		} else if (!func0.equals(other.func0))
			return false;
		if (func1 == null) {
			if (other.func1 != null)
				return false;
		} else if (!func1.equals(other.func1))
			return false;
		if (func2 == null) {
			if (other.func2 != null)
				return false;
		} else if (!func2.equals(other.func2))
			return false;
		if (func3 == null) {
			if (other.func3 != null)
				return false;
		} else if (!func3.equals(other.func3))
			return false;
		if (func4 == null) {
			if (other.func4 != null)
				return false;
		} else if (!func4.equals(other.func4))
			return false;
		if (principal == null) {
			if (other.principal != null)
				return false;
		} else if (!principal.equals(other.principal))
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		return true;
	}	
	
	
}
