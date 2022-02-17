package org.ic4j.candid.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.ic4j.candid.dom.DOMDeserializer;
import org.ic4j.candid.dom.DOMSerializer;
import org.ic4j.candid.dom.DOMUtils;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public final class DOMTest extends CandidAssert {
	static final String SIMPLE_NODE_FILE = "SimpleNode.xml";
	static final String SIMPLE_ARRAY_NODE_FILE = "ComplexNode.xml";
	static final String TRADE_ARRAY_NODE_FILE = "TradeArrayNode.xml";
	
	// Instantiate the Factory
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

	static {
		LOG = LoggerFactory.getLogger(DOMTest.class);
	}

	@Test
	public void test() {
		dbf.setNamespaceAware(true);
		dbf.setIgnoringElementContentWhitespace(true);

		this.testDom(SIMPLE_NODE_FILE, null, false);
		this.testDom(SIMPLE_ARRAY_NODE_FILE, null,false);
		this.testDom(TRADE_ARRAY_NODE_FILE, null,true);
	}

	void testDom(String fileName, IDLType idlType, boolean attributes) {
		try {
			Node domNode = this.readNode(fileName);

			IDLValue idlValue;

			if (idlType == null)
				idlValue = IDLValue.create(domNode, DOMSerializer.create());
			else
				idlValue = IDLValue.create(domNode, DOMSerializer.create(idlType));

			List<IDLValue> args = new ArrayList<IDLValue>();
			args.add(idlValue);

			IDLArgs idlArgs = IDLArgs.create(args);

			byte[] buf = idlArgs.toBytes();

			DOMDeserializer domDeserializer = DOMDeserializer.create(idlValue.getIDLType())
					.rootElement("http://ic4j.org/candid/test", "data").setAttributes(attributes);

			Node domNodeResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(domDeserializer, Node.class);
			
			try {
				String result = DOMUtils.getStringFromDocument(domNodeResult.getOwnerDocument());
				LOG.debug(result);
			} catch (TransformerException e) {
				LOG.error(e.getLocalizedMessage(), e);
			}

//			Assertions.assertTrue(domNode.isEqualNode(domNodeResult));

		} catch (SAXException | IOException | ParserConfigurationException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}
	}

	Node readNode(String fileName) throws SAXException, IOException, ParserConfigurationException {
		// parse XML file
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document doc = db.parse(getClass().getClassLoader().getResource(fileName).getFile());

		try {
			String domString = DOMUtils.getStringFromDocument(doc);
		} catch (TransformerException e) {

		}
		return doc.getDocumentElement();
	}
}
