package org.ic4j.candid.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.ic4j.candid.ByteUtils;
import org.ic4j.candid.dom.DOMDeserializer;
import org.ic4j.candid.dom.DOMSerializer;
import org.ic4j.candid.dom.DOMUtils;
import org.ic4j.candid.jackson.JacksonDeserializer;
import org.ic4j.candid.jackson.JacksonSerializer;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.pojo.PojoDeserializer;
import org.ic4j.candid.pojo.PojoSerializer;
import org.ic4j.candid.types.Label;
import org.ic4j.types.Principal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public final class PojoTest extends CandidAssert {

	static {
		LOG = LoggerFactory.getLogger(PojoTest.class);
	}

	@Test
	public void test() {			
		// Loan Offer Request		
		
		LoanOfferRequest loanRequest = new LoanOfferRequest();
		
		loanRequest.userId = Principal.fromString("ubgwl-msd3g-gr5yh-cwpic-elony-lnexo-5f3wf-atisx-hxeyt-ffmfu-tqe");
		loanRequest.amount = (double) 20000.00;
		loanRequest.applicationId = new BigInteger("11");
		loanRequest.term = 48;
		loanRequest.rating = 670;
		loanRequest.zipcode = "95134";
		loanRequest.created = new BigInteger("0");
		
		LoanOfferRequest[] loanRequestArray = {loanRequest};

		IDLValue idlValue = IDLValue.create(loanRequestArray, new PojoSerializer());

		List<IDLValue> args = new ArrayList<IDLValue>();
		args.add(idlValue);

		IDLArgs idlArgs = IDLArgs.create(args);

		byte[] buf = idlArgs.toBytes();
		
		int[] unsignedBuf = ByteUtils.toUnsignedIntegerArray(buf);
		
		LoanOfferRequest[] loanRequestArrayResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(new PojoDeserializer(), LoanOfferRequest[].class);
		
		Assertions.assertArrayEquals(loanRequestArray, loanRequestArrayResult);
		
		// Loan Offer Request		
		
		LoanOffer loan = new LoanOffer();
		
		loan.userId = Principal.fromString("ubgwl-msd3g-gr5yh-cwpic-elony-lnexo-5f3wf-atisx-hxeyt-ffmfu-tqe");
		loan.apr = (double) 3.4;
		loan.applicationId = 11;
		loan.providerName = "United Loan";
		loan.providerId = "zrakb-eaaaa-aaaab-qacaq-cai";
		loan.created = new BigInteger("0");
		
		LoanOffer[] loanArray = {loan};

		idlValue = IDLValue.create(loanArray, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		
		LoanOffer[] loanArrayResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(new PojoDeserializer(), LoanOffer[].class);
		
		Assertions.assertArrayEquals(loanArray, loanArrayResult);	
		
		// Loan Applications	
		
		LoanApplication loanApplication = new LoanApplication();
		loanApplication.firstName = "John";
		loanApplication.lastName = "Doe";
		loanApplication.ssn = "111-11-1111";
		loanApplication.term = 48;
		loanApplication.zipcode = "95134";		
		loanApplication.amount = (double) 20000.00;
		loanApplication.id = new BigInteger("11");
		loanApplication.created = new BigInteger("0");
		
		LoanApplication[] loanApplicationArray = {loanApplication};

		idlValue = IDLValue.create(loanApplicationArray, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		LoanApplication[] loanApplicationArrayResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(new PojoDeserializer(), LoanApplication[].class);
		
		Assertions.assertArrayEquals(loanApplicationArray, loanApplicationArrayResult);		
		
		// Record POJO

		Pojo pojoValue = new Pojo();

		pojoValue.bar = new Boolean(true);
		pojoValue.foo = BigInteger.valueOf(42);

		idlValue = IDLValue.create(pojoValue, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		assertBytes("DIDL\\01\\6c\\02\\d3\\e3\\aa\\02\\7e\\86\\8e\\b7\\02\\7c\\01\\00\\01\\2a", buf);

		IDLArgs outArgs = IDLArgs.fromBytes(buf);

		Pojo pojoResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(new PojoDeserializer(), Pojo.class);

		Assertions.assertEquals(pojoValue, pojoResult);
		// Pojo OPT
		Optional<Pojo> optionalPojoValue = Optional.of(pojoValue);
		idlValue = IDLValue.create(optionalPojoValue, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		Pojo optionalPojoResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(new PojoDeserializer(), Pojo.class);

		Assertions.assertEquals(pojoValue, optionalPojoResult);

		// Pojo Array VEC

		Pojo pojoValue2 = new Pojo();

		pojoValue2.bar = new Boolean(false);
		pojoValue2.foo = BigInteger.valueOf(43);

		Pojo[] pojoArray = { pojoValue, pojoValue2 };

		idlValue = IDLValue.create(pojoArray, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		Pojo[] pojoArrayResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(new PojoDeserializer(), Pojo[].class);

		Assertions.assertArrayEquals(pojoArray, pojoArrayResult);

		ArrayNode arrayNode = IDLArgs.fromBytes(buf).getArgs().get(0)
				.getValue(JacksonDeserializer.create(idlValue.getIDLType()), ArrayNode.class);

		JsonNode jsonNode = IDLArgs.fromBytes(buf).getArgs().get(0)
				.getValue(JacksonDeserializer.create(idlValue.getIDLType()), JsonNode.class);

		idlValue = IDLValue.create(jsonNode, JacksonSerializer.create(idlValue.getIDLType()));
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		JsonNode jsonNodeResult = IDLArgs.fromBytes(buf).getArgs().get(0)
				.getValue(JacksonDeserializer.create(idlValue.getIDLType()), JsonNode.class);

		Assertions.assertEquals(jsonNode, jsonNodeResult);

		DOMDeserializer domDeserializer = DOMDeserializer.create(idlValue.getIDLType())
				.rootElement("http://scaleton.com/dfinity/candid", "root");
		// domDeserializer = domDeserializer.setAttributes(true);

		Node domNode = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(domDeserializer, Node.class);

		try {
			String domString = DOMUtils.getStringFromDocument(domNode.getOwnerDocument());
		} catch (TransformerException e) {

		}

		idlValue = IDLValue.create(domNode, DOMSerializer.create());

		// Complex RECORD Pojo

		ComplexPojo complexPojoValue = new ComplexPojo();
		complexPojoValue.bar = new Boolean(true);
		complexPojoValue.foo = BigInteger.valueOf(42);

		complexPojoValue.pojo = pojoValue2;

		idlValue = IDLValue.create(complexPojoValue, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		unsignedBuf  = ByteUtils.toUnsignedIntegerArray(buf);

		IDLType[] idlTypes = { idlValue.getIDLType() };

		outArgs = IDLArgs.fromBytes(buf, idlTypes);

		ComplexPojo complexPojoResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(PojoDeserializer.create(),
				ComplexPojo.class);

		Assertions.assertEquals(complexPojoValue, complexPojoResult);

		jsonNode = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(JacksonDeserializer.create(idlValue.getIDLType()),
				JsonNode.class);

		idlValue = IDLValue.create(jsonNode, JacksonSerializer.create(idlValue.getIDLType()));

		domDeserializer = DOMDeserializer.create(idlValue.getIDLType())
				.rootElement("http://scaleton.com/dfinity/candid", "data");
		// domDeserializer = domDeserializer.setAttributes(true);

		domNode = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(domDeserializer, Node.class);

		try {
			String domString = DOMUtils.getStringFromDocument(domNode.getOwnerDocument());
		} catch (TransformerException e) {

		}

		idlValue = IDLValue.create(domNode, DOMSerializer.create());

		// Complex Array RECORD Pojo

		ComplexArrayPojo complexArrayPojoValue = new ComplexArrayPojo();

		Boolean[] barArray = { new Boolean(true), new Boolean(false) };
		complexArrayPojoValue.bar = barArray;

		BigInteger[] fooArray = { new BigInteger("100000000"), new BigInteger("200000000"),
				new BigInteger("300000000") };
		complexArrayPojoValue.foo = fooArray;

		Pojo[] pojoArray2 = { pojoValue, pojoValue2 };

		complexArrayPojoValue.pojo = pojoArray2;

		idlValue = IDLValue.create(complexArrayPojoValue, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();

		ComplexArrayPojo complexPojoArrayResult = IDLArgs.fromBytes(buf).getArgs().get(0)
				.getValue(new PojoDeserializer(), ComplexArrayPojo.class);

		Assertions.assertEquals(complexArrayPojoValue, complexPojoArrayResult);
	}
}
