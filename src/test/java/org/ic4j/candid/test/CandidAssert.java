package org.ic4j.candid.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Type;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;

abstract class CandidAssert {
	static Logger LOG;
	
	protected static String BINARY_IMAGE_FILE = "dfinity.png";

	static byte[] getBytes(String input) throws DecoderException {
		if (input == null)
			throw new Error("Invalid input value");

		if (input.isEmpty())
			return input.getBytes();

		int i = input.indexOf('\\');

		if (i < 0)
			return input.getBytes();

		String prefix = input.substring(0, i);

		String data = input.substring(input.indexOf('\\')).replace("\\", "");

		return ArrayUtils.addAll(prefix.getBytes(), Hex.decodeHex(data.toCharArray()));
	}

	static byte[] getBytes(String input, String value) throws DecoderException {
		if (value == null)
			return getBytes(input);
		else
			return ArrayUtils.addAll(getBytes(input), value.getBytes());
	}
	
	static byte[] getBinary(String fileName, String type) throws Exception{
		InputStream binaryInputStream = CandidAssert.class.getClassLoader().getResourceAsStream(fileName);

		BufferedImage bImage = ImageIO.read(binaryInputStream);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(bImage, type, bos );
		byte [] data = bos.toByteArray();
		      
		return data;
	}

	static void assertBytes(String input, byte[] value) {
		try {
			byte[] bytes = getBytes(input);

			Assertions.assertArrayEquals(bytes, value);

		} catch (DecoderException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}
	}

	static void assertValue(String input, Object value) {
		IDLValue idlValue = IDLValue.create(value);

		try {
			byte[] bytes = getBytes(input);
			assertValue(bytes, input, value, idlValue);

		} catch (DecoderException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}
	}

	static void assertValue(String input, Object value, Type type) {
		IDLValue idlValue = IDLValue.create(value, type);

		try {

			byte[] bytes = getBytes(input);

			assertValue(bytes, input, value, idlValue);

		} catch (DecoderException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}

	}

	static void assertValue(String input, String stringValue, Object value) {
		IDLValue idlValue = IDLValue.create(value);

		try {
			byte[] bytes = getBytes(input, stringValue);
			assertValue(bytes, input, value, idlValue);

		} catch (DecoderException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}
	}

	static void assertValue(String input, String stringValue, Object value, Type type) {
		IDLValue idlValue = IDLValue.create(value, type);

		try {

			byte[] bytes = getBytes(input, stringValue);

			assertValue(bytes, input, value, idlValue);

		} catch (DecoderException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}

	}

	static void assertValue(byte[] bytes, String input, Object value, IDLValue idlValue) {
		List<IDLValue> args = new ArrayList<IDLValue>();

		args.add(idlValue);

		IDLArgs idlArgs = IDLArgs.create(args);

		byte[] buf = idlArgs.toBytes();

		if (value != null)
			LOG.info(value.toString() + ":" + input);
		else
			LOG.info("null" + ":" + input);

		Assertions.assertArrayEquals(buf, bytes);

		IDLArgs outArgs;

		if (idlValue.getType() == Type.RECORD || idlValue.getType() == Type.VARIANT) {
			IDLType[] idlTypes = { idlValue.getIDLType() };

			outArgs = IDLArgs.fromBytes(bytes, idlTypes);
		} else
			outArgs = IDLArgs.fromBytes(bytes);

		if (value != null)
			LOG.info(input + ":" + value.toString());
		else
			LOG.info(input + ":" + "null");

		Assertions.assertEquals(value, outArgs.getArgs().get(0).getValue());

	}

	static void testEncode(String input, Object value) {
		try {
			testEncode(getBytes(input), value);
		} catch (DecoderException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}
	}

	static void testEncode(byte[] bytes, Object value) {
		IDLValue idlValue = IDLValue.create(value);

		testEncode(bytes, idlValue);
	}

	static void testEncode(byte[] bytes, Object value, Type type) {
		IDLValue idlValue = IDLValue.create(value, type);

		testEncode(bytes, idlValue);

	}

	static void testEncode(byte[] bytes, IDLValue idlValue) {
		List<IDLValue> args = new ArrayList<IDLValue>();

		args.add(idlValue);

		IDLArgs idlArgs = IDLArgs.create(args);

		byte[] buf = idlArgs.toBytes();

		Assertions.assertArrayEquals(buf, bytes);
	}

	static void testDecode(Object value, byte[] bytes) {
		IDLArgs outArgs = IDLArgs.fromBytes(bytes);

		Assertions.assertEquals(value, outArgs.getArgs().get(0).getValue());

	}

	static void assertFail(String input, Class exClass, String message) {
		try {
			byte[] bytes = getBytes(input);
			IDLArgs outArgs = IDLArgs.fromBytes(bytes);
			Assertions.fail(message);
		} catch (DecoderException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		} catch (AssertionFailedError e) {
			throw e;
		} catch (Throwable t) {
			LOG.info(t.getLocalizedMessage());
			LOG.info(message);
			Assertions.assertTrue(t.getClass() == exClass);
		}
	}
}
