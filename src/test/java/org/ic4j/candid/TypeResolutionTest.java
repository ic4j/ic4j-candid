package org.ic4j.candid;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TypeResolutionTest {
	private TypeTable table(List<Long>... entries) {
		return new TypeTable(Arrays.asList(entries), new LinkedList<Long>(), new LinkedList<Long>());
	}

	@Test
	void recursiveFunctionTypesReuseTheirIdentity() {
		TypeTable table = table(Arrays.asList(-22L, 1L, 0L, 0L, 0L));
		IDLType resolved = table.resolveType(0, new DecodingBudget(DecodingLimits.DEFAULT), 0);
		Assertions.assertSame(resolved, resolved.getArgs().get(0));
	}

	@Test
	void recursiveFunctionDecodesThroughPublicApi() {
		byte[] message = {'D', 'I', 'D', 'L', 1, 0x6a, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0};
		IDLType resolved = IDLArgs.fromBytes(message).getArgs().get(0).getIDLType();
		Assertions.assertSame(resolved, resolved.getArgs().get(0));
	}

	@Test
	void invalidFunctionReferencesAreRejected() {
		byte[] message = {'D', 'I', 'D', 'L', 1, 0x6a, 1, 1, 0, 0, 0};
		Assertions.assertThrows(CandidError.class, () -> IDLArgs.fromBytes(message));
	}

	@Test
	void recursiveServiceTypesReuseTheirIdentity() {
		TypeTable table = table(Arrays.asList(-23L, 1L, 1L, 109L, 1L), Arrays.asList(-22L, 1L, 0L, 0L, 0L));
		IDLType resolved = table.resolveType(0, new DecodingBudget(DecodingLimits.DEFAULT), 0);
		Assertions.assertSame(resolved, resolved.getMeths().get("m").getArgs().get(0));
	}

	@Test
	void acyclicTypeDepthIsBounded() {
		TypeTable table = table(Arrays.asList(-22L, 1L, 1L, 0L, 0L), Arrays.asList(-22L, 0L, 0L, 0L));
		DecodingBudget budget = new DecodingBudget(new DecodingLimits(1024, 10, 20, 20, 1));
		Assertions.assertThrows(CandidError.class, () -> table.resolveType(0, budget, 0));
	}

	private byte[] variant(Label label, Object value) {
		Map<Label, Object> values = new TreeMap<Label, Object>();
		values.put(label, value);
		return IDLArgs.create(Arrays.asList(IDLValue.create(values, Type.VARIANT))).toBytes();
	}

	@Test
	void preservesSelectedVariantLabel() {
		Map<Label, IDLType> fields = new TreeMap<Label, IDLType>();
		fields.put(Label.createNamedLabel("Ok"), IDLType.createType(Type.TEXT));
		fields.put(Label.createNamedLabel("Err"), IDLType.createType(Type.TEXT));
		IDLType expected = IDLType.createType(Type.VARIANT, fields);
		for (Label label : fields.keySet()) {
			IDLValue result = IDLArgs.fromBytes(variant(label, "value"), new IDLType[] {expected}).getArgs().get(0);
			Map<Label, Object> decoded = result.getValue();
			Assertions.assertEquals(label, decoded.keySet().iterator().next());
			Assertions.assertEquals("value", decoded.get(label));
		}
	}

	@Test
	void rejectsUnknownVariantLabelsAndPayloadTypes() {
		Map<Label, IDLType> fields = new TreeMap<Label, IDLType>();
		Label label = Label.createNamedLabel("Ok");
		fields.put(label, IDLType.createType(Type.TEXT));
		IDLType[] expected = {IDLType.createType(Type.VARIANT, fields)};
		Assertions.assertThrows(CandidError.class,
				() -> IDLArgs.fromBytes(variant(Label.createNamedLabel("Err"), "value"), expected));
		Assertions.assertThrows(CandidError.class, () -> IDLArgs.fromBytes(variant(label, 1), expected));
		fields.put(label, IDLType.createType(Type.RECORD));
		Assertions.assertThrows(CandidError.class, () -> IDLArgs.fromBytes(variant(label, 1), expected));
	}

	@Test
	void rejectsOutOfRangeVariantIndex() {
		byte[] message = {'D', 'I', 'D', 'L', 1, 0x6b, 1, 0, 0x7f, 1, 0, 1};
		Assertions.assertThrows(CandidError.class, () -> IDLArgs.fromBytes(message));
	}
}