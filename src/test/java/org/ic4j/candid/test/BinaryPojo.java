package org.ic4j.candid.test;


import java.util.Arrays;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.types.Type;

public class BinaryPojo {
	@Field(Type.NAT8)
	@Name("object")
	public Byte[] object;
	
	@Field(Type.NAT8)
	@Name("primitive")
	public byte[] primitive;

	
	// only for testing purposes
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BinaryPojo other = (BinaryPojo) obj;
		if (!Arrays.equals(object, other.object))
			return false;
		if (!Arrays.equals(primitive, other.primitive))
			return false;
		return true;
	}
	
}
