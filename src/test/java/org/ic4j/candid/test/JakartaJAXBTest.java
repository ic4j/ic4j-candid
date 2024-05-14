package org.ic4j.candid.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.ic4j.candid.jakarta.JAXBDeserializer;
import org.ic4j.candid.jakarta.JAXBSerializer;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;


public final class JakartaJAXBTest extends CandidAssert {
	static final String SIMPLE_NODE_FILE = "SimpleNode.xml";
	static final String COMPLEX_NODE_FILE = "ComplexNode.xml";
	static final String TRADE_ARRAY_NODE_FILE = "TradeArrayNode.xml";	
	static final String SWIFT_XML_NODE_FILE = "CustomerCreditTransferInitiationV03.xml";


	static {
		LOG = LoggerFactory.getLogger(JakartaJAXBTest.class);
	}

	@Test
	public void test() {		

		JakartaJAXBPojo pojo;
		ComplexJakartaJAXBPojo complexPojo;
		try {
			
		    JAXBContext context = JAXBContext.newInstance(JakartaJAXBPojo.class);
		    pojo =  (JakartaJAXBPojo) context.createUnmarshaller()		
		      .unmarshal(new File(getClass().getClassLoader().getResource(SIMPLE_NODE_FILE).getFile()));
			
			IDLValue idlValue = IDLValue.create(pojo, JAXBSerializer.create());
			List<IDLValue> args = new ArrayList<IDLValue>();
			args.add(idlValue);

			IDLArgs idlArgs = IDLArgs.create(args);
			
			byte[] buf = idlArgs.toBytes();	
			
			JakartaJAXBPojo pojoResult = IDLArgs.fromBytes(buf).getArgs().get(0)
					.getValue(JAXBDeserializer.create(), JakartaJAXBPojo.class);

			Assertions.assertEquals(pojo, pojoResult);
			
		    context = JAXBContext.newInstance(ComplexJakartaJAXBPojo.class);
		    complexPojo =  (ComplexJakartaJAXBPojo) context.createUnmarshaller()		
		      .unmarshal(new File(getClass().getClassLoader().getResource(COMPLEX_NODE_FILE).getFile()));
			
			idlValue = IDLValue.create(complexPojo, JAXBSerializer.create());
			args = new ArrayList<IDLValue>();
			args.add(idlValue);

			idlArgs = IDLArgs.create(args);
			
			buf = idlArgs.toBytes();	
			
			ComplexJakartaJAXBPojo complexPojoResult = IDLArgs.fromBytes(buf).getArgs().get(0)
					.getValue(JAXBDeserializer.create(), ComplexJakartaJAXBPojo.class);

			Assertions.assertEquals(complexPojo, complexPojoResult);
			

			
					
		} catch (JAXBException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}

	}

}
