package org.ic4j.candid.test;

import java.io.Reader;
import java.io.StringReader;

import org.ic4j.candid.parser.idl.value.IDLValueGrammar;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.parser.idl.value.SimpleNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IDLValueParserTest {
	static Logger LOG;

	static final String IDL_SIMPLE_ARGS = "(true : bool, null, -42 : int, 42., +42.42, -42e5, 42.42e-5)";
	static final String IDL_SIMPLE_ARRAY_ARGS = "(vec{1;2;3;4})";
	
	static final String IDL_SIMPLE_OPT_ARGS = "(opt -42 : int,opt 42.,opt +42.42)";
	
	static final String IDL_STRING_ARGS = "(opt \"hello\" : text,opt \"motoko\", \"icp\" : text, \"ic4j\")";
	
	static final String IDL_BLOB_ARGS = "(blob \"DIDL\\\\00\\\\01\\\\76\\\\00\\\\00\")";
	
	static final String IDL_COMPLEX_ARGS = "(opt record {}, record { 1=42;44=\"test\"; 2=false }, variant { 5=null })";


	static {
		LOG = LoggerFactory.getLogger(IDLValueParserTest.class);
	}

	@Test
	public void test() {

		try {
			this.testIDLValue(IDL_SIMPLE_ARGS);
			this.parseIDLValue(IDL_SIMPLE_ARGS);
			this.testIDLValue(IDL_SIMPLE_ARRAY_ARGS);
			this.parseIDLValue(IDL_SIMPLE_ARRAY_ARGS);
			this.testIDLValue(IDL_SIMPLE_OPT_ARGS);
			this.parseIDLValue(IDL_SIMPLE_OPT_ARGS);			
			this.testIDLValue(IDL_STRING_ARGS);
			this.parseIDLValue(IDL_STRING_ARGS);
			this.testIDLValue(IDL_BLOB_ARGS);
			this.parseIDLValue(IDL_BLOB_ARGS);	
			this.testIDLValue(IDL_COMPLEX_ARGS);
			this.parseIDLValue(IDL_COMPLEX_ARGS);				

		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getLocalizedMessage());
		}

	}
	
	void parseIDLValue(String input) throws Exception {
		if (input != null) {
			LOG.info("Reading from IDL input " + input);
			try {				
				IDLArgs args = IDLArgs.fromIDL(input);
				
				for(IDLValue value : args.getArgs())
				{					
					IDLType type = value.getIDLType();
					
					if(value.getValue() == null)
						LOG.info("Value: null");
					else	
						LOG.info("Value:" + value.getValue().toString());
					LOG.info("Type:" + type.getType().name());
				}
				

			} catch (Exception e) {
				throw e;
			}
		} else {
			throw new Exception("Input not found.");
		}

	}	

	void testIDLValue(String input) throws Exception {
		IDLValueGrammar parser;
		if (input != null) {
			LOG.info("Reading from IDL input " + input);
			try {
				Reader reader = new StringReader(input);
				parser = new IDLValueGrammar(reader);
			} catch (Exception e) {
				throw e;
			}
		} else {
			throw new Exception("Input not found.");
		}
		try {
			SimpleNode node = parser.Start();
			node.dump("idlValue");
			LOG.info("Done");

			this.parseNode(node);

		} catch (Exception e) {
			LOG.error(e.getMessage());
			Assertions.fail(e.getLocalizedMessage());
		}
	}

	void parseNode(SimpleNode node) {
		LOG.info(Integer.toString(node.jjtGetNumChildren()));
		LOG.info("Name:" + node.toString());

		if (node.jjtGetValue() != null)
			LOG.info("Value:" + node.jjtGetValue().toString());

		for (int i = 0; i < node.jjtGetNumChildren(); i++)
			this.parseNode((SimpleNode) node.jjtGetChild(i));

	}
}
