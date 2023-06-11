package org.ic4j.candid.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.ArrayUtils;
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
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;
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
		// Ref Test
		
		RefPojo refPojo = new RefPojo();
		
		refPojo.bar = true;
		refPojo.foo =  BigInteger.valueOf(32);
		//refPojo.dummy = "dummy";
		
		refPojo.principal = Principal.fromString("w7x7r-cok77-xa");
		
		refPojo.service = new Service(Principal.fromString("w7x7r-cok77-xa"));
		
		refPojo.func0 = new Func(Principal.fromString("w7x7r-cok77-xa"),"a");
		refPojo.func1 = new Func(Principal.fromString("w7x7r-cok77-xa"),"b");
		refPojo.func2 = new Func(Principal.fromString("w7x7r-cok77-xa"),"c");
		refPojo.func3 = new Func(Principal.fromString("w7x7r-cok77-xa"),"d");
		refPojo.func4 = new Func(Principal.fromString("w7x7r-cok77-xa"),"e");
		
		IDLValue idlValue = IDLValue.create(refPojo, new PojoSerializer());
		
		List<IDLValue> args = new ArrayList<IDLValue>();
		args.add(idlValue);

		IDLArgs idlArgs = IDLArgs.create(args);

		byte[] buf = idlArgs.toBytes();
		
		RefPojo refPojoResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(new PojoDeserializer(), RefPojo.class);
		
		Assertions.assertEquals(refPojo, refPojoResult);
		
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

		idlValue = IDLValue.create(loanRequestArray, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
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
		
		ComplexPojo complexPojoValue2 = new ComplexPojo();
		complexPojoValue2.bar = new Boolean(true);
		complexPojoValue2.foo = BigInteger.valueOf(44);	
		
		complexPojoValue2.pojo = pojoValue;
		
		ComplexPojo[] complexPojoArrayValue = {complexPojoValue,complexPojoValue2};
		
		idlValue = IDLValue.create(complexPojoArrayValue, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		IDLType[] idlTypesArray = {idlValue.getIDLType()};
		
		outArgs = IDLArgs.fromBytes(buf,idlTypesArray);
		
		ComplexPojo[] complexPojoArrayValueResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), ComplexPojo[].class);
		
		Assertions.assertArrayEquals(complexPojoArrayValue, complexPojoArrayValueResult);
		
		ComplexOptionalPojo complexOptionalPojoValue = new ComplexOptionalPojo();
		complexOptionalPojoValue.bar = Optional.ofNullable(new Boolean(true));
		complexOptionalPojoValue.foo = Optional.ofNullable(BigInteger.valueOf(44));	
		
		complexOptionalPojoValue.pojo = Optional.ofNullable(pojoValue);
		
		idlValue = IDLValue.create(complexOptionalPojoValue, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		ComplexOptionalPojo complexOptionalPojoValueResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), ComplexOptionalPojo.class);
		
		Assertions.assertEquals(complexOptionalPojoValue, complexOptionalPojoValueResult);		
		
		ComplexPojo[] emptyComplexPojoArrayValue = {};
		
		idlValue = IDLValue.create(emptyComplexPojoArrayValue, new PojoSerializer());

		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		IDLType[] idlTypesEmptyArray = {idlValue.getIDLType()};
		
		outArgs = IDLArgs.fromBytes(buf,idlTypesEmptyArray);
		
		complexPojoArrayValueResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), ComplexPojo[].class);
		
		Assertions.assertArrayEquals(emptyComplexPojoArrayValue, complexPojoArrayValueResult);	
		
		Func func = new Func(Principal.fromString("w7x7r-cok77-xa"),"a");
		
		// test Func with no type defined
		idlValue = IDLValue.create(func, new PojoSerializer());
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		Func funcResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), Func.class);
		
		Assertions.assertEquals(func, funcResult);
		
		// test func with type defined
		List<IDLType> funcArgs = new ArrayList<IDLType>();
		List<IDLType> funcRets = new ArrayList<IDLType>();
		List<Mode> funcModes = new ArrayList<Mode>();
		
		funcArgs.add(IDLType.createType(Type.TEXT));
		funcRets.add(IDLType.createType(Type.NAT));
		funcModes.add(Mode.QUERY);
		
		idlValue = IDLValue.create(func,new PojoSerializer(), IDLType.createType(funcArgs,funcRets,funcModes));
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		funcResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), Func.class, IDLType.createType(funcArgs,funcRets,funcModes));
		
		Assertions.assertEquals(func, funcResult);
		
		// test func arrays no type defined
		
		Func func2 = new Func(Principal.fromString("rrkah-fqaaa-aaaaa-aaaaq-cai"),"b");
		
		Func[] funcArray = {func, func2};
		
		idlValue = IDLValue.create(funcArray, new PojoSerializer());
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		Func[] funcArrayResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), Func[].class);
		
		Assertions.assertArrayEquals(funcArray, funcArrayResult);
		
		// test func arrays type defined
		
		idlValue = IDLValue.create(funcArray,new PojoSerializer(), IDLType.createType(Type.VEC, IDLType.createType(funcArgs,funcRets,funcModes)));
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);

		funcArrayResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), Func[].class, IDLType.createType(Type.VEC, IDLType.createType(funcArgs,funcRets,funcModes)));
		
		Assertions.assertArrayEquals(funcArray, funcArrayResult);
		
		// test func Opt type not defined
		
		Optional<Func> funcOpt = Optional.ofNullable(func);
		
		idlValue = IDLValue.create(funcOpt, new PojoSerializer());
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		Optional<Func> funcOptResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), funcOpt.getClass());
		
		Assertions.assertEquals(funcOpt.get(), funcOptResult.get());
		
		
		// test func Opt type defined
		
		idlValue = IDLValue.create(funcOpt, new PojoSerializer(), IDLType.createType(Type.OPT, IDLType.createType(funcArgs,funcRets,funcModes)));
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		funcOptResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), funcOpt.getClass(), IDLType.createType(Type.OPT, IDLType.createType(funcArgs,funcRets,funcModes)));
		
		Assertions.assertEquals(funcOpt.get(), funcOptResult.get());
		
		// TEST services 
		
		// test Service with no type defined
		Service service = new Service(Principal.fromString("w7x7r-cok77-xa"));
		
		idlValue = IDLValue.create(service, new PojoSerializer());
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		Service serviceResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), Service.class);
		
		Assertions.assertEquals(service, serviceResult);
		
		// test service with type defined

		Map<String,IDLType> funcMap = new TreeMap<String,IDLType>();
		
		funcMap.put("foo", IDLType.createType(funcArgs,funcRets,funcModes));
		funcMap.put("foo2", IDLType.createType(funcArgs,funcRets,funcModes));		
		
		idlValue = IDLValue.create(service,new PojoSerializer(), IDLType.createType(funcMap));
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		serviceResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), Service.class, IDLType.createType(funcMap));
		
		Assertions.assertEquals(service, serviceResult);
		
		// test service array no type defined
		Service service2 = new Service(Principal.fromString("rrkah-fqaaa-aaaaa-aaaaq-cai"));	
		
		Service[] serviceArray = {service, service2};
		
		idlValue = IDLValue.create(serviceArray, new PojoSerializer());
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		Service[] serviceArrayResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), Service[].class);
		
		Assertions.assertArrayEquals(serviceArray, serviceArrayResult);
		
		// test service array with type defined
		idlValue = IDLValue.create(serviceArray, new PojoSerializer(), IDLType.createType(Type.VEC, IDLType.createType(funcMap)));
		
		args = new ArrayList<IDLValue>();
		args.add(idlValue);

		idlArgs = IDLArgs.create(args);

		buf = idlArgs.toBytes();
		
		outArgs = IDLArgs.fromBytes(buf);
		
		serviceArrayResult = outArgs.getArgs().get(0)
				.getValue(new PojoDeserializer(), Service[].class, IDLType.createType(Type.VEC, IDLType.createType(funcMap)));
		
		Assertions.assertArrayEquals(serviceArray, serviceArrayResult);
	
		try {
			BinaryPojo binaryValue = new BinaryPojo();
			binaryValue.primitive = getBinary(BINARY_IMAGE_FILE, "png");	
			binaryValue.object = ArrayUtils.toObject(getBinary(BINARY_IMAGE_FILE, "png"));
			
			idlValue = IDLValue.create(binaryValue, new PojoSerializer());

			args = new ArrayList<IDLValue>();
			args.add(idlValue);

			idlArgs = IDLArgs.create(args);

			buf = idlArgs.toBytes();
			
			BinaryPojo binaryResult = IDLArgs.fromBytes(buf).getArgs().get(0)
					.getValue(new PojoDeserializer(), BinaryPojo.class);
			
			Assertions.assertEquals(binaryValue, binaryResult);
			
		}catch(Exception e)
		{
			LOG.debug(e.getLocalizedMessage(), e);
			Assertions.fail(e.getLocalizedMessage());			
		}
	}
}
