package org.ic4j.candid;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.pojo.PojoDeserializer;
import org.ic4j.candid.types.Label;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReflectionSecurityTest {
	public static class Target {
		private static String shared = "unchanged";
		private final String fixed = new String("unchanged");
		private transient String local = "unchanged";
		private String instance = "original";
	}

	@Test
	void onlyMutableInstanceFieldsAreAssigned() {
		Map<Label, Object> fields = new TreeMap<Label, Object>();
		for (String name : Arrays.asList("shared", "fixed", "local", "instance"))
			fields.put(Label.createNamedLabel(name), "decoded");
		IDLValue value = IDLValue.create(fields);
		for (ObjectDeserializer deserializer : Arrays.asList(PojoDeserializer.create(),
				org.ic4j.candid.jaxb.javax.JAXBDeserializer.create(),
				org.ic4j.candid.jaxb.jakarta.JAXBDeserializer.create())) {
			Target target = deserializer.deserialize(value, Target.class);
			Assertions.assertEquals("unchanged", Target.shared);
			Assertions.assertEquals("unchanged", target.fixed);
			Assertions.assertEquals("unchanged", target.local);
			Assertions.assertEquals("decoded", target.instance);
		}
	}
}