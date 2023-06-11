package org.ic4j.candid.test;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "pojo", namespace="http://ic4j.org/candid/test")
public class JAXBPojo {
	@XmlElement(name="bar", namespace="http://ic4j.org/candid/test", required=true)
	public Boolean bar;

	@XmlElement(name="foo", namespace="http://ic4j.org/candid/test" , required=true)
	public BigInteger foo;
	
	@XmlTransient
	public String dummy;
	
	// Just for testing purposes, JUnit uses equals
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JAXBPojo other = (JAXBPojo) obj;
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
		return true;
	}	

}
