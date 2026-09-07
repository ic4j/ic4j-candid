package org.ic4j.candid;

import java.util.Arrays;

import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DecodingLimitsTest {
	private byte[] encode(IDLValue... values) {
		return IDLArgs.create(Arrays.asList(values)).toBytes();
	}

	@Test
	void collectionLimitRejectsOnlyOversizedCollections() {
		DecodingLimits limits = new DecodingLimits(1024, 2, 20, 20, 8);
		byte[] allowed = encode(IDLValue.create(new Integer[] {1, 2}));
		byte[] rejected = encode(IDLValue.create(new Integer[] {1, 2, 3}));
		Assertions.assertDoesNotThrow(() -> IDLArgs.fromBytes(allowed, null, limits));
		Assertions.assertThrows(CandidError.class, () -> IDLArgs.fromBytes(rejected, null, limits));
	}

	@Test
	void aggregateCollectionLimitAppliesAcrossArguments() {
		byte[] message = encode(IDLValue.create(new Integer[] {1, 2}), IDLValue.create(new Integer[] {3, 4}));
		DecodingLimits limits = new DecodingLimits(1024, 2, 5, 20, 8);
		Assertions.assertThrows(CandidError.class, () -> IDLArgs.fromBytes(message, null, limits));
	}

	@Test
	void messageLimitIsCheckedBeforeParsing() {
		byte[] message = encode(IDLValue.create("bounded"));
		DecodingLimits limits = new DecodingLimits(message.length - 1, 10, 20, 20, 8);
		Assertions.assertThrows(CandidError.class, () -> IDLArgs.fromBytes(message, null, limits));
	}

	@Test
	void valueDepthIsBoundedAndResetsAcrossSiblings() {
		byte[] message = encode(IDLValue.create(new Integer[] {1, 2}));
		Assertions.assertThrows(CandidError.class,
				() -> IDLArgs.fromBytes(message, null, new DecodingLimits(1024, 10, 20, 20, 1)));
		Assertions.assertDoesNotThrow(
				() -> IDLArgs.fromBytes(message, null, new DecodingLimits(1024, 10, 20, 20, 2)));
	}

	@Test
	void typeTableBudgetIsEnforced() {
		byte[] message = encode(IDLValue.create(new Integer[] {1}));
		Assertions.assertThrows(CandidError.class,
				() -> IDLArgs.fromBytes(message, null, new DecodingLimits(1024, 10, 20, 1, 8)));
	}

	@Test
	void oversizedWireCountsAreRejectedBeforeAllocation() {
		byte[] prefix = {'D', 'I', 'D', 'L', 1, 0x6d, 0x7f, 1, 0};
		for (long length : new long[] {Integer.MAX_VALUE, 1L << 32}) {
			byte[] count = Leb128.writeUnsigned(length);
			byte[] message = Arrays.copyOf(prefix, prefix.length + count.length);
			System.arraycopy(count, 0, message, prefix.length, count.length);
			Assertions.assertThrows(CandidError.class, () -> IDLArgs.fromBytes(message));
		}
	}

	@Test
	void recursiveOptionalValuesRespectDepthLimit() {
		byte[] prefix = {'D', 'I', 'D', 'L', 1, 0x6e, 0, 1, 0};
		byte[] message = Arrays.copyOf(prefix, prefix.length + 10);
		Arrays.fill(message, prefix.length, message.length - 1, (byte) 1);
		Assertions.assertThrows(CandidError.class,
				() -> IDLArgs.fromBytes(message, null, new DecodingLimits(1024, 10, 20, 20, 8)));
		Assertions.assertDoesNotThrow(
				() -> IDLArgs.fromBytes(message, null, new DecodingLimits(1024, 10, 20, 20, 12)));
	}
}