package org.ic4j.candid.test;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.ArrayUtils;
import org.ic4j.candid.CandidError;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Meths;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public final class CandidTest extends CandidAssert{
	
	static
	{
		LOG = LoggerFactory.getLogger(CandidTest.class);
	}
	
	@Test
	public void test() {
		// testing function, move it after
						
		try {
			//IDLArgs.fromBytes(getBytes("DIDL\\02\\6a\\01\\71\\01\\7d\\00\\69\\02\\03foo\\00\\04foo\\32\\00\\01\\01\\01\\03\\ca\\ff\\ee"));
			
			byte[] bytes = ArrayUtils.addAll(getBytes("DIDL\\02\\6a\\01\\71\\01\\7d\\00\\69\\02\\03"), "foo".getBytes());
			bytes = ArrayUtils.addAll(bytes,getBytes("\\00\\04"));
			 
			bytes = ArrayUtils.addAll(bytes	,"foo".getBytes());
			bytes = ArrayUtils.addAll(bytes,getBytes("\\32\\00\\01\\01\\01\\03\\ca\\ff\\ee"));
			
			IDLArgs idlArgs = IDLArgs.fromBytes(bytes);
			
			byte[] buf = idlArgs.toBytes();
			
			idlArgs = IDLArgs.fromBytes(buf);
			
			Service service = new Service(Principal.fromString("w7x7r-cok77-xa"));
			
			List<IDLType> args = new ArrayList<IDLType>();
			List<IDLType> rets = new ArrayList<IDLType>();
			List<Mode> modes = new ArrayList<Mode>();
			
			IDLValue idlValue = IDLValue.create(service, IDLType.createType(new TreeMap<String,IDLType>()));
			assertValue("DIDL\\01\\69\\00\\01\\00\\01\\03\\ca\\ff\\ee", idlValue);
			
			Map<String,IDLType> funcMap = new TreeMap<String,IDLType>();
			
			args.add(IDLType.createType(Type.TEXT));
			rets.add(IDLType.createType(Type.NAT));

			funcMap.put("foo", IDLType.createType(args,rets,modes));
			funcMap.put("foo2", IDLType.createType(args,rets,modes));
			
			idlValue = IDLValue.create(service, IDLType.createType(funcMap));
			
			assertValue(bytes, "DIDL\\02\\6a\\01\\71\\01\\7d\\00\\69\\02\\03foo\\00\\04foo\\32\\00\\01\\01\\01\\03\\ca\\ff\\ee",service, idlValue);
			
			Func func = new Func(Principal.fromString("w7x7r-cok77-xa"),"a");
			
			args = new ArrayList<IDLType>();
			rets = new ArrayList<IDLType>();
			modes = new ArrayList<Mode>();
			
			idlValue = IDLValue.create(func, IDLType.createType(args,rets,modes));
			
			assertValue("DIDL\\01\\6a\\00\\00\\00\\01\\00\\01\\01\\03\\ca\\ff\\ee\\01\\61", idlValue);
			
			func = new Func(Principal.fromString("w7x7r-cok77-xa"),"foo");
			
			args.add(IDLType.createType(Type.TEXT));
			rets.add(IDLType.createType(Type.NAT));
			modes.add(Mode.QUERY);
			
			idlValue = IDLValue.create(func, IDLType.createType(args,rets,modes));
			
			assertValue("DIDL\\01\\6a\\01\\71\\01\\7d\\01\\01\\01\\00\\01\\01\\03\\ca\\ff\\ee\\03","foo", idlValue);
		} catch (Exception e) {
			LOG.debug(e.getLocalizedMessage(), e);
			Assertions.fail(e.getLocalizedMessage());
		}		

		// fundamentaly wrong
		assertFail("", CandidError.class, "empty");
		assertFail("\\00\\00", CandidError.class, "no magic bytes");
		assertFail("DADL", CandidError.class, "wrong magic bytes");
		assertFail("DADL\\00\\00", CandidError.class, "wrong magic bytes");

		// Compared with Rust implementation, no exception thrown there. We need to
		// investigate correct handling
		// assertFail("DIDL\\80\\00\\00", CandidError.class, "overlong typ table length"
		// );
		// assertFail("DIDL\\00\\80\\00", CandidError.class, "overlong arg length" );

		// Null
		assertValue("DIDL\\00\\01\\7f", null);
		assertFail("DIDL\\00\\01\\7e", CandidError.class, "wrong type");
		assertFail("DIDL\\00\\01\\7f\\00", CandidError.class, "null: too long");

		// Let's see if we can match it with null value, otherwise it is nullary
		// assertFail("DIDL\\00\\00", CandidError.class, "null: extra null values" );

		// Boolean
		assertValue("DIDL\\00\\01\\7e\\00", new Boolean(false));
		assertValue("DIDL\\00\\01\\7e\\01", new Boolean(true));
		assertFail("DIDL\\00\\01\\7e", CandidError.class, "bool: missing");
		assertFail("DIDL\\00\\01\\02", CandidError.class, "bool: out of range");
		assertFail("DIDL\\00\\01\\ff", CandidError.class, "bool: out of range");

		// Unsigned Integer (Nat)
		assertValue("DIDL\\00\\01\\7d\\00", new BigInteger("0"), Type.NAT);
		assertValue("DIDL\\00\\01\\7d\\01", new BigInteger("1"), Type.NAT);
		assertValue("DIDL\\00\\01\\7d\\7f", new BigInteger("127"), Type.NAT);
		assertValue("DIDL\\00\\01\\7d\\80\\01", new BigInteger("128"), Type.NAT);
		assertValue("DIDL\\00\\01\\7d\\ff\\7f", new BigInteger("16383"), Type.NAT);
		assertFail("DIDL\\00\\01\\7d\\80", CandidError.class, "nat: leb too short");

		// TODO only for decoding, I assume
		// assertValue("DIDL\\00\\01\\7d\\80\\00",new BigInteger("0"), Type.NAT);
		// assertValue("DIDL\\00\\01\\7d\\ff\\00",new BigInteger("127"), Type.NAT );
		assertValue("DIDL\\00\\01\\7d\\80\\80\\98\\f4\\e9\\b5\\ca\\6a", new BigInteger("60000000000000000"), Type.NAT);

		// Integer
		assertValue("DIDL\\00\\01\\7c\\00", new BigInteger("0"));
		assertValue("DIDL\\00\\01\\7c\\01", new BigInteger("1"));
		assertValue("DIDL\\00\\01\\7c\\7f", new BigInteger("-1"));
		assertValue("DIDL\\00\\01\\7c\\40", new BigInteger("-64"));
		assertValue("DIDL\\00\\01\\7c\\80\\01", new BigInteger("128"));
		assertFail("DIDL\\00\\01\\7c\\80", CandidError.class, "int: leb too short");
		assertValue("DIDL\\00\\01\\7c\\80\\80\\98\\f4\\e9\\b5\\ca\\ea\\00", new BigInteger("60000000000000000"));
		assertValue("DIDL\\00\\01\\7c\\80\\80\\e8\\8b\\96\\ca\\b5\\95\\7f", new BigInteger("-60000000000000000"));

		// Byte Unsigned Int 8 (nat8)
		assertValue("DIDL\\00\\01\\7b\\00", new Byte((byte) 0), Type.NAT8);
		assertValue("DIDL\\00\\01\\7b\\01", new Byte((byte) 1), Type.NAT8);
		assertValue("DIDL\\00\\01\\7b\\ff", new Byte((byte) 255), Type.NAT8);
		assertFail("DIDL\\00\\01\\7b", CandidError.class, "nat8: leb too short");

		// Short Unsigned Int 16 (nat16)
		assertValue("DIDL\\00\\01\\7a\\00\\00", new Short((short) 0), Type.NAT16);
		assertValue("DIDL\\00\\01\\7a\\01\\00", new Short((short) 1), Type.NAT16);
		assertValue("DIDL\\00\\01\\7a\\ff\\00", new Short((short) 255), Type.NAT16);
		assertValue("DIDL\\00\\01\\7a\\00\\01", new Short((short) 256), Type.NAT16);
		assertValue("DIDL\\00\\01\\7a\\ff\\ff", new Short((short) 65535), Type.NAT16);
		assertFail("DIDL\\00\\01\\7a", CandidError.class, "nat16: leb too short");
		assertFail("DIDL\\00\\01\\7a\\00", CandidError.class, "nat16: leb too short");

		// Integer Unsigned Int 32 (nat32)
		assertValue("DIDL\\00\\01\\79\\00\\00\\00\\00", new Integer(0), Type.NAT32);
		assertValue("DIDL\\00\\01\\79\\01\\00\\00\\00", new Integer(1), Type.NAT32);
		assertValue("DIDL\\00\\01\\79\\ff\\00\\00\\00", new Integer(255), Type.NAT32);
		assertValue("DIDL\\00\\01\\79\\00\\01\\00\\00", new Integer(256), Type.NAT32);
		assertValue("DIDL\\00\\01\\79\\ff\\ff\\00\\00", new Integer(65535), Type.NAT32);
		assertValue("DIDL\\00\\01\\79\\ff\\ff\\ff\\ff", Integer.parseUnsignedInt("4294967295"), Type.NAT32);
		assertFail("DIDL\\00\\01\\79", CandidError.class, "nat32: leb too short");
		assertFail("DIDL\\00\\01\\79\\00", CandidError.class, "nat32: leb too short");
		assertFail("DIDL\\00\\01\\79\\00\\00", CandidError.class, "nat32: leb too short");
		assertFail("DIDL\\00\\01\\79\\00\\00\\00", CandidError.class, "nat32: leb too short");

		// Long Unsigned Int 64 (nat64)
		assertValue("DIDL\\00\\01\\78\\00\\00\\00\\00\\00\\00\\00\\00", new Long(0), Type.NAT64);
		assertValue("DIDL\\00\\01\\78\\01\\00\\00\\00\\00\\00\\00\\00", new Long(1), Type.NAT64);
		assertValue("DIDL\\00\\01\\78\\ff\\00\\00\\00\\00\\00\\00\\00", new Long(255), Type.NAT64);
		assertValue("DIDL\\00\\01\\78\\00\\01\\00\\00\\00\\00\\00\\00", new Long(256), Type.NAT64);
		assertValue("DIDL\\00\\01\\78\\ff\\ff\\00\\00\\00\\00\\00\\00", new Long(65535), Type.NAT64);
		assertValue("DIDL\\00\\01\\78\\ff\\ff\\ff\\ff\\00\\00\\00\\00", new Long(4294967295l), Type.NAT64);
		assertValue("DIDL\\00\\01\\78\\ff\\ff\\ff\\ff\\ff\\ff\\ff\\ff", Long.parseUnsignedLong("18446744073709551615"),
				Type.NAT64);
		assertFail("DIDL\\00\\01\\78", CandidError.class, "nat64: leb too short");
		assertFail("DIDL\\00\\01\\78\\00", CandidError.class, "nat64: leb too short");
		assertFail("DIDL\\00\\01\\78\\00\\00", CandidError.class, "nat64: leb too short");
		assertFail("DIDL\\00\\01\\78\\00\\00\\00\\00", CandidError.class, "nat64: leb too short");
		assertFail("DIDL\\00\\01\\78\\00\\00\\00\\00\\00", CandidError.class, "nat64: leb too short");
		assertFail("DIDL\\00\\01\\78\\00\\00\\00\\00\\00\\00", CandidError.class, "nat64: leb too short");
		assertFail("DIDL\\00\\01\\78\\00\\00\\00\\00\\00\\00\\00", CandidError.class, "nat64: leb too short");

		// Byte (int8)
		assertValue("DIDL\\00\\01\\77\\00", new Byte((byte) 0));
		assertValue("DIDL\\00\\01\\77\\01", new Byte((byte) 1));
		assertValue("DIDL\\00\\01\\77\\ff", new Byte((byte) -1));
		assertFail("DIDL\\00\\01\\77", CandidError.class, "int8: leb too short");

		// Short (int16)
		assertValue("DIDL\\00\\01\\76\\00\\00", new Short((short) 0));
		assertValue("DIDL\\00\\01\\76\\01\\00", new Short((short) 1));
		assertValue("DIDL\\00\\01\\76\\ff\\00", new Short((short) 255));
		assertValue("DIDL\\00\\01\\76\\00\\01", new Short((short) 256));
		assertValue("DIDL\\00\\01\\76\\ff\\ff", new Short((short) -1));
		assertFail("DIDL\\00\\01\\76", CandidError.class, "int16: leb too short");
		assertFail("DIDL\\00\\01\\76\\00", CandidError.class, "int16: leb too short");

		// Integer (int32)
		assertValue("DIDL\\00\\01\\75\\00\\00\\00\\00", new Integer(0));
		assertValue("DIDL\\00\\01\\75\\01\\00\\00\\00", new Integer(1));
		assertValue("DIDL\\00\\01\\75\\ff\\00\\00\\00", new Integer(255));
		assertValue("DIDL\\00\\01\\75\\00\\01\\00\\00", new Integer(256));
		assertValue("DIDL\\00\\01\\75\\ff\\ff\\00\\00", new Integer(65535));
		assertValue("DIDL\\00\\01\\75\\ff\\ff\\ff\\ff", new Integer(-1));
		assertFail("DIDL\\00\\01\\75", CandidError.class, "int32: leb too short");
		assertFail("DIDL\\00\\01\\75\\00", CandidError.class, "int32: leb too short");
		assertFail("DIDL\\00\\01\\75\\00\\00", CandidError.class, "int32: leb too short");
		assertFail("DIDL\\00\\01\\75\\00\\00\\00", CandidError.class, "int32: leb too short");

		// Long (int64)
		assertValue("DIDL\\00\\01\\74\\00\\00\\00\\00\\00\\00\\00\\00", new Long(0));
		assertValue("DIDL\\00\\01\\74\\01\\00\\00\\00\\00\\00\\00\\00", new Long(1));
		assertValue("DIDL\\00\\01\\74\\ff\\00\\00\\00\\00\\00\\00\\00", new Long(255));
		assertValue("DIDL\\00\\01\\74\\00\\01\\00\\00\\00\\00\\00\\00", new Long(256));
		assertValue("DIDL\\00\\01\\74\\ff\\ff\\00\\00\\00\\00\\00\\00", new Long(65535));
		assertValue("DIDL\\00\\01\\74\\ff\\ff\\ff\\ff\\00\\00\\00\\00", new Long(4294967295l));
		assertValue("DIDL\\00\\01\\74\\ff\\ff\\ff\\ff\\ff\\ff\\ff\\ff", new Long(-1));
		assertFail("DIDL\\00\\01\\74", CandidError.class, "int64: leb too short");
		assertFail("DIDL\\00\\01\\74\\00", CandidError.class, "int64: leb too short");
		assertFail("DIDL\\00\\01\\74\\00\\00", CandidError.class, "int64: leb too short");
		assertFail("DIDL\\00\\01\\74\\00\\00\\00\\00", CandidError.class, "int64: leb too short");
		assertFail("DIDL\\00\\01\\74\\00\\00\\00\\00\\00", CandidError.class, "int64: leb too short");
		assertFail("DIDL\\00\\01\\74\\00\\00\\00\\00\\00\\00", CandidError.class, "int64: leb too short");
		assertFail("DIDL\\00\\01\\74\\00\\00\\00\\00\\00\\00\\00", CandidError.class, "int64: leb too short");

		// Float (float32)
		assertValue("DIDL\\00\\01\\73\\00\\00\\00\\00", new Float(0.0));
		assertValue("DIDL\\00\\01\\73\\00\\00\\40\\40", new Float(3.0));
		assertValue("DIDL\\00\\01\\73\\00\\00\\00\\3f", new Float(0.5));
		assertValue("DIDL\\00\\01\\73\\00\\00\\00\\bf", new Float(-0.5));
		assertFail("DIDL\\00\\01\\73\\00\\00", CandidError.class, "float32: too short");

		// Double (float464)
		assertValue("DIDL\\00\\01\\72\\00\\00\\00\\00\\00\\00\\00\\00", new Double(0.0));
		assertValue("DIDL\\00\\01\\72\\00\\00\\00\\00\\00\\00\\08\\40", new Double(3.0));
		assertValue("DIDL\\00\\01\\72\\00\\00\\00\\00\\00\\00\\e0\\3f", new Double(0.5));
		assertValue("DIDL\\00\\01\\72\\00\\00\\00\\00\\00\\00\\e0\\bf", new Double(-0.5));
		assertFail("DIDL\\00\\01\\72\\00\\00\\00\\00", CandidError.class, "float64: too short");

		// String (text)
		assertValue("DIDL\\00\\01\\71\\00", new String(""));
		assertValue("DIDL\\00\\01\\71\\06", "Motoko", new String("Motoko"));
		assertValue("DIDL\\00\\01\\71\\03\\e2\\98\\83", new String("☃"));

		// Principal
		assertValue("DIDL\\00\\01\\68\\01\\00", Principal.fromString("aaaaa-aa"));
		assertValue("DIDL\\00\\01\\68\\01\\03\\ca\\ff\\ee", Principal.fromString("w7x7r-cok77-xa"));
		assertValue("DIDL\\00\\01\\68\\01\\09\\ef\\cd\\ab\\00\\00\\00\\00\\00\\01",
				Principal.fromString("2chl6-4hpzw-vqaaa-aaaaa-c"));
//		assertValue("DIDL\\00\\01\\68\\01\\02\\ca\\ff",Principal.fromString("w7x7r-cok77-xa"));	
		assertFail("DIDL\\00\\01\\68\\03\\ca\\ff\\ee", CandidError.class, "principal: no tag");
		assertFail("DIDL\\00\\01\\68\\01\\03\\ca\\ff", CandidError.class, "principal: too short");
		assertFail("DIDL\\00\\01\\68\\01\\03\\ca\\ff\\ee\\ee", CandidError.class, "principal: too long");
		assertFail("DIDL\\01\\68\\01\\00\\01\\03\\ca\\ff\\ee", CandidError.class, "principal: not construct");

		// Opt
		assertValue("DIDL\\00\\01\\7f", null);

		// TODO fix this, this is tricky, because we cannot identify type of empty inner
		// class
		// assertValue("DIDL\\01\\6e\\6f\\01\\00\\00",Optional.empty());
		assertValue("DIDL\\01\\6e\\7e\\01\\00\\01\\00", Optional.of(new Boolean(false)));
		assertValue("DIDL\\01\\6e\\7e\\01\\00\\01\\01", Optional.of(new Boolean(true)));
		assertFail("DIDL\\01\\6e\\7e\\01\\00\\01\\02", CandidError.class, "opt: parsing invalid bool at opt bool");
		// assertValue("DIDL\\01\\6e\\7e\\01\\00\\00",Optional.empty());

		// Record
		Map<Label, Object> mapValue = new HashMap<Label, Object>();

		assertValue("DIDL\\01\\6c\\00\\01\\00", mapValue);

		mapValue.put(Label.createNamedLabel("bar"), new Boolean(true));

		mapValue.put(Label.createNamedLabel("foo"), BigInteger.valueOf(42));

		assertValue("DIDL\\01\\6c\\02\\d3\\e3\\aa\\02\\7e\\86\\8e\\b7\\02\\7c\\01\\00\\01\\2a", mapValue);

		Map<Label, Object> intMapValue = new HashMap<Label, Object>();

		intMapValue.put(Label.createIdLabel(1l), BigInteger.valueOf(42));

		assertValue("DIDL\\01\\6c\\01\\01\\7c\\01\\00\\2a", intMapValue);
		
		// Record with VEC
		
		mapValue = new HashMap<Label, Object>();
		
		BigInteger[] nestedArray = { new BigInteger("100000000"), new BigInteger("200000000"),
				new BigInteger("300000000") };
		
		mapValue.put(Label.createNamedLabel("foo"),  nestedArray);
		
		IDLValue idlValue = IDLValue.create(mapValue);
		
		List<IDLValue> args = new ArrayList<IDLValue>();
		args.add(idlValue);
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		IDLArgs idlArgs = IDLArgs.create(args);
		
		byte[] buf = idlArgs.toBytes();		
		
		IDLArgs outArgs = IDLArgs.fromBytes(buf);

		// Variant

		Map<Label, Object> variantValue = new HashMap<Label, Object>();

		variantValue.put(Label.createIdLabel(3303859l), "value");

		idlValue = IDLValue.create(variantValue, Type.VARIANT);

		args = new ArrayList<IDLValue>();

		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		IDLType[] types = { idlValue.getIDLType() };

		outArgs = IDLArgs.fromBytes(buf, types);

//		Assertions.assertEquals(variantValue, outArgs.getArgs().get(0).getValue());

		// test big integer argument
		args = new ArrayList<IDLValue>();

		BigInteger bigintValue = new BigInteger("1234567890");

		args.add(IDLValue.create(bigintValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(bigintValue, outArgs.getArgs().get(0).getValue());

		// test integer argument
		args = new ArrayList<IDLValue>();

		Integer intValue = new Integer(10000);

		args.add(IDLValue.create(intValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(intValue, outArgs.getArgs().get(0).getValue());

		// test Principal argument

		Principal principal = Principal.fromString("rrkah-fqaaa-aaaaa-aaaaq-cai");

		args = new ArrayList<IDLValue>();

		args.add(IDLValue.create(principal));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		Principal principalResult = (Principal) outArgs.getArgs().get(0).getValue();

		LOG.info(principalResult.toString());

		Assertions.assertEquals(principal.toString(), principalResult.toString());

		// test Array argument Integer
		args = new ArrayList<IDLValue>();

		Integer[] array = { 10000, 20000, 30000 };

		args.add(IDLValue.create(array));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		Assertions.assertArrayEquals(array, outArgs.getArgs().get(0).getValue());

		// test Array argument BigInteger
		args = new ArrayList<IDLValue>();

		BigInteger[] bigarray = { new BigInteger("100000000"), new BigInteger("200000000"),
				new BigInteger("300000000") };

		args.add(IDLValue.create(bigarray));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		Assertions.assertArrayEquals(bigarray, outArgs.getArgs().get(0).getValue());

		// test Optional argument
		args = new ArrayList<IDLValue>();

		args.add(IDLValue.create(Optional.of(intValue)));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		Optional optionalResult = (Optional) outArgs.getArgs().get(0).getValue();

		LOG.info(optionalResult.get().toString());
		Assertions.assertEquals(Optional.of(intValue), optionalResult);

		// test String argument

		args = new ArrayList<IDLValue>();

		String stringValue = "Hello";

		args.add(IDLValue.create(stringValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(stringValue, outArgs.getArgs().get(0).getValue());

		// test Boolean argument

		args = new ArrayList<IDLValue>();

		Boolean boolValue = true;

		args.add(IDLValue.create(boolValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(boolValue, outArgs.getArgs().get(0).getValue());

		// test Short argument

		args = new ArrayList<IDLValue>();

		Short shortValue = 64;

		args.add(IDLValue.create(shortValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(shortValue, outArgs.getArgs().get(0).getValue());

		// test Long argument

		args = new ArrayList<IDLValue>();

		Long longValue = 64000000000L;

		args.add(IDLValue.create(longValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(longValue, outArgs.getArgs().get(0).getValue());

		// test Byte argument

		args = new ArrayList<IDLValue>();

		Byte byteValue = (byte) 64;

		args.add(IDLValue.create(byteValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(byteValue, outArgs.getArgs().get(0).getValue());

		// test Double argument

		args = new ArrayList<IDLValue>();

		Double doubleValue = 42.42;

		args.add(IDLValue.create(doubleValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(doubleValue, outArgs.getArgs().get(0).getValue());

		// test Float argument

		args = new ArrayList<IDLValue>();

		Float floatValue = 42.42f;

		args.add(IDLValue.create(floatValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(floatValue, outArgs.getArgs().get(0).getValue());

		// test multiple arguments

		args = new ArrayList<IDLValue>();

		args.add(IDLValue.create(intValue));
		args.add(IDLValue.create(stringValue));
		args.add(IDLValue.create(boolValue));
		args.add(IDLValue.create(doubleValue));
		args.add(IDLValue.create(floatValue));

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		outArgs = IDLArgs.fromBytes(buf);

		LOG.info(outArgs.getArgs().get(0).getValue().toString());
		Assertions.assertEquals(intValue, outArgs.getArgs().get(0).getValue());

		LOG.info(outArgs.getArgs().get(1).getValue().toString());
		Assertions.assertEquals(stringValue, outArgs.getArgs().get(1).getValue());

		LOG.info(outArgs.getArgs().get(2).getValue().toString());
		Assertions.assertEquals(boolValue, outArgs.getArgs().get(2).getValue());

		LOG.info(outArgs.getArgs().get(3).getValue().toString());
		Assertions.assertEquals(doubleValue, outArgs.getArgs().get(3).getValue());

		LOG.info(outArgs.getArgs().get(4).getValue().toString());
		Assertions.assertEquals(floatValue, outArgs.getArgs().get(4).getValue());
		
		// test Binary argument
		try {
			args = new ArrayList<IDLValue>();

			byte[] binaryValue = getBinary(BINARY_IMAGE_FILE, "png");			

			args.add(IDLValue.create(binaryValue, IDLType.createType(org.ic4j.candid.types.Type.VEC, IDLType.createType(org.ic4j.candid.types.Type.NAT8))));

			idlArgs = IDLArgs.create(args);

			buf = idlArgs.toBytes();
			
			outArgs = IDLArgs.fromBytes(buf);

			Byte[] binaryResponse = (Byte[]) outArgs.getArgs().get(0).getValue();

			LOG.info(Integer.toString(binaryResponse.length));
			Assertions.assertTrue(binaryValue.length == binaryResponse.length);

			Assertions.assertArrayEquals(ArrayUtils.toObject(binaryValue), binaryResponse);

		} catch (Throwable ex) {
			LOG.debug(ex.getLocalizedMessage(), ex);
			Assertions.fail(ex.getLocalizedMessage());
		}
		
		// test Binary argument
		try {
			args = new ArrayList<IDLValue>();

			Byte[] binaryValue = ArrayUtils.toObject(getBinary(BINARY_IMAGE_FILE, "png"));	

			args.add(IDLValue.create(binaryValue, IDLType.createType(org.ic4j.candid.types.Type.VEC, IDLType.createType(org.ic4j.candid.types.Type.NAT8))));

			idlArgs = IDLArgs.create(args);

			buf = idlArgs.toBytes();
			
			outArgs = IDLArgs.fromBytes(buf);

			Byte[] binaryResponse = (Byte[]) outArgs.getArgs().get(0).getValue();

			LOG.info(Integer.toString(binaryResponse.length));
			Assertions.assertTrue(binaryValue.length == binaryResponse.length);

			Assertions.assertArrayEquals(binaryValue, binaryResponse);

		} catch (Throwable ex) {
			LOG.debug(ex.getLocalizedMessage(), ex);
			Assertions.fail(ex.getLocalizedMessage());
		}
		
	}
	
}
