package org.ic4j.candid;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DecoderValidationTest {
	@Test
	void validatesTypeReferences() {
		Assertions.assertDoesNotThrow(() -> TypeTable.validateTypeRange(-1, 0));
		Assertions.assertDoesNotThrow(() -> TypeTable.validateTypeRange(-24, 0));
		Assertions.assertDoesNotThrow(() -> TypeTable.validateTypeRange(0, 1));
		Assertions.assertThrows(CandidError.class, () -> TypeTable.validateTypeRange(1, 1));
		Assertions.assertThrows(CandidError.class, () -> TypeTable.validateTypeRange(-18, 1));
	}

	@Test
	void rejectsLengthsOutsideJavaRange() {
		Bytes bytes = Bytes.from(Leb128.writeUnsigned((long) Integer.MAX_VALUE + 1));
		Assertions.assertThrows(CandidError.class, bytes::readLength);
		Assertions.assertThrows(CandidError.class, () -> Bytes.from(new byte[0]).parseBytes(-1));
	}

	@Test
	void signedIntegersRoundTripAtBoundaries() {
		for (int value : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
			byte[] encoded = Leb128.writeSigned(value);
			Assertions.assertEquals(value, Leb128.readSigned(ByteBuffer.wrap(encoded)));
			Assertions.assertEquals(value, Leb128.readSigned(encoded));
		}
	}

	@Test
	void rejectsOutOfRangeSignedIntegers() {
		byte[] encoded = Leb128.writeUnsigned((long) Integer.MAX_VALUE + 1);
		Assertions.assertThrows(CandidError.class, () -> Leb128.readSigned(ByteBuffer.wrap(encoded)));
		Assertions.assertThrows(CandidError.class, () -> Leb128.readSigned(encoded));
		byte[] unterminated = Leb128.writeSigned(Integer.MIN_VALUE);
		unterminated[unterminated.length - 1] |= 0x80;
		Assertions.assertThrows(CandidError.class, () -> Leb128.readSigned(ByteBuffer.wrap(unterminated)));
		Assertions.assertThrows(CandidError.class, () -> Leb128.readSigned(unterminated));
	}
}