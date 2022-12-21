package org.ic4j.candid.test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ic4j.candid.jackson.JacksonDeserializer;
import org.ic4j.candid.jackson.JacksonSerializer;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JacksonTest extends CandidAssert {
	static final String SIMPLE_NODE_FILE = "SimpleNode.json";
	static final String SIMPLE_ARRAY_NODE_FILE = "SimpleArrayNode.json";
	static final String TRADE_ARRAY_NODE_FILE = "TradeArrayNode.json";

	ObjectMapper mapper = new ObjectMapper();

	static {
		LOG = LoggerFactory.getLogger(JacksonTest.class);
	}

	@Test
	public void test() {

		Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();

		typeMap.put(Label.createNamedLabel("bar"), IDLType.createType(Type.BOOL));
		typeMap.put(Label.createNamedLabel("foo"), IDLType.createType(Type.INT));

		this.testJson(SIMPLE_NODE_FILE, IDLType.createType(Type.RECORD, typeMap));

		IDLType idlType = IDLType.createType(Type.VEC, IDLType.createType(Type.RECORD, typeMap));

		this.testJson(SIMPLE_ARRAY_NODE_FILE, idlType);
		
		Map<Label,IDLType> rootRecord = new TreeMap<Label,IDLType>();
		rootRecord.put(Label.createUnnamedLabel(0l), IDLType.createType(Type.NAT32));
		
		Map<Label,IDLType> offerRecord = new TreeMap<Label,IDLType>();
		offerRecord.put(Label.createNamedLabel("locked"), IDLType.createType(Type.OPT));
		offerRecord.put(Label.createNamedLabel("seller"), IDLType.createType(Type.PRINCIPAL));
		offerRecord.put(Label.createNamedLabel("price"), IDLType.createType(Type.NAT64));
		
		rootRecord.put(Label.createUnnamedLabel(1l), IDLType.createType(Type.RECORD, offerRecord));
		
		Map<Label,IDLType> typeVariant = new TreeMap<Label,IDLType>();
		
		Map<Label,IDLType> nonfungibleRecord = new TreeMap<Label,IDLType>();
		nonfungibleRecord.put(Label.createNamedLabel("metadata"), IDLType.createType(Type.OPT));
		
		typeVariant.put(Label.createNamedLabel("nonfungible"), IDLType.createType(Type.RECORD,nonfungibleRecord));
		
		rootRecord.put(Label.createUnnamedLabel(2l), IDLType.createType(Type.VARIANT, typeVariant));
		
		idlType = IDLType.createType(Type.VEC, IDLType.createType(Type.RECORD, rootRecord));
		
		this.testJson(TRADE_ARRAY_NODE_FILE, idlType);		

		JacksonPojo pojo = new JacksonPojo();

		pojo.bar = true;
		pojo.foo = BigInteger.valueOf(42);

		IDLValue idlValue = IDLValue.create(pojo, JacksonSerializer.create());
		List<IDLValue> args = new ArrayList<IDLValue>();
		args.add(idlValue);

		IDLArgs idlArgs = IDLArgs.create(args);

		byte[] buf = idlArgs.toBytes();

		JacksonPojo pojoResult = IDLArgs.fromBytes(buf).getArgs().get(0)
				.getValue(JacksonDeserializer.create(), JacksonPojo.class);

		Assertions.assertEquals(pojo, pojoResult);
	}

	void testJson(String fileName, IDLType idlType) {
		try {
			JsonNode jsonValue = readNode(fileName);

			IDLValue idlValue;
			
			if(idlType == null)
				idlValue = IDLValue.create(jsonValue, JacksonSerializer.create());
			else
				idlValue = IDLValue.create(jsonValue, JacksonSerializer.create(idlType));
			
			List<IDLValue> args = new ArrayList<IDLValue>();
			args.add(idlValue);

			IDLArgs idlArgs = IDLArgs.create(args);

			byte[] buf = idlArgs.toBytes();

			JsonNode jsonResult = IDLArgs.fromBytes(buf).getArgs().get(0)
				.getValue(JacksonDeserializer.create(idlValue.getIDLType()), JsonNode.class);				

			JSONAssert.assertEquals(jsonValue.asText(), jsonResult.asText(), JSONCompareMode.LENIENT);

		} catch (IOException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		} catch (JSONException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}
	}

	JsonNode readNode(String fileName) throws JsonProcessingException, IOException {
		byte[] input = Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(fileName).getPath()));

		JsonNode rootNode = (JsonNode) mapper.readTree(input);

		return rootNode;
	}
}
