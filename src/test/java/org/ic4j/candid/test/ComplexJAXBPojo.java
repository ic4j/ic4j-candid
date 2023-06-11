package org.ic4j.candid.test;


import java.math.BigInteger;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "data", namespace="http://ic4j.org/candid/test")
public class ComplexJAXBPojo {
	@XmlElement(name="bar", namespace="http://ic4j.org/candid/test", required=true)
	public Boolean bar;
	
	@XmlElement(name="foo", namespace="http://ic4j.org/candid/test", required=true)
	public BigInteger foo;
	
	@XmlElement(name="pojo", namespace="http://ic4j.org/candid/test", required=true)
	public JAXBPojo pojo;

	// Just for testing purposes, JUnit uses equals
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComplexJAXBPojo other = (ComplexJAXBPojo) obj;
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
