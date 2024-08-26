package org.ic4j.candid.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ic4j.candid.jaxb.javax.JAXBDeserializer;
import org.ic4j.candid.jaxb.javax.JAXBSerializer;
import org.ic4j.candid.jaxb.javax.JAXBUtils;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.prowidesoftware.swift.model.mx.AppHdr;
import com.prowidesoftware.swift.model.mx.MxPacs00800107;
import com.prowidesoftware.swift.model.mx.MxPacs00900109;
import com.prowidesoftware.swift.model.mx.MxPain00100103;
import com.prowidesoftware.swift.model.mx.dic.ActiveCurrencyAndAmount;
import com.prowidesoftware.swift.model.mx.dic.CustomerCreditTransferInitiationV03;
import com.prowidesoftware.swift.model.mx.dic.FIToFICustomerCreditTransferV07;
import com.prowidesoftware.swift.model.mx.dic.FinancialInstitutionCreditTransferV09;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader70;
import com.prowidesoftware.swift.model.mx.dic.TaxRecordPeriod1Code;
import com.prowidesoftware.swift.utils.Lib;


public final class JavaxJAXBTest extends CandidAssert {
	static final String SIMPLE_NODE_FILE = "SimpleNode.xml";
	static final String COMPLEX_NODE_FILE = "ComplexNode.xml";
	static final String TRADE_ARRAY_NODE_FILE = "TradeArrayNode.xml";	
	static final String SWIFT_XML_NODE_FILE = "CustomerCreditTransferInitiationV03.xml";


	static {
		LOG = LoggerFactory.getLogger(JavaxJAXBTest.class);
	}

	@Test
	public void test() {		

		JavaxJAXBPojo pojo;
		ComplexJAXBPojo complexPojo;
		try {
			
		    JAXBContext context = JAXBContext.newInstance(JavaxJAXBPojo.class);
		    pojo =  (JavaxJAXBPojo) context.createUnmarshaller()		
		      .unmarshal(new File(getClass().getClassLoader().getResource(SIMPLE_NODE_FILE).getFile()));
			
			IDLValue idlValue = IDLValue.create(pojo, JAXBSerializer.create());
			List<IDLValue> args = new ArrayList<IDLValue>();
			args.add(idlValue);

			IDLArgs idlArgs = IDLArgs.create(args);
			
			byte[] buf = idlArgs.toBytes();	
			
			JavaxJAXBPojo pojoResult = IDLArgs.fromBytes(buf).getArgs().get(0)
					.getValue(JAXBDeserializer.create(), JavaxJAXBPojo.class);

			Assertions.assertEquals(pojo, pojoResult);
			
		    context = JAXBContext.newInstance(ComplexJAXBPojo.class);
		    complexPojo =  (ComplexJAXBPojo) context.createUnmarshaller()		
		      .unmarshal(new File(getClass().getClassLoader().getResource(COMPLEX_NODE_FILE).getFile()));
			
			idlValue = IDLValue.create(complexPojo, JAXBSerializer.create());
			args = new ArrayList<IDLValue>();
			args.add(idlValue);

			idlArgs = IDLArgs.create(args);
			
			buf = idlArgs.toBytes();	
			
			ComplexJAXBPojo complexPojoResult = IDLArgs.fromBytes(buf).getArgs().get(0)
					.getValue(JAXBDeserializer.create(), ComplexJAXBPojo.class);

			Assertions.assertEquals(complexPojo, complexPojoResult);
			
			JAXBSerializer serializer =JAXBSerializer.create();
			
			IDLType type = JAXBUtils.getIDLType(GroupHeader70.class);
			
			type = JAXBUtils.getIDLType(TaxRecordPeriod1Code.class);
					
			type = JAXBUtils.getIDLType(FIToFICustomerCreditTransferV07.class);
			
		       // parse the XML message content from a resource file
	        MxPacs00800107 mx = MxPacs00800107.parse(Lib.readResource("pacs.008.001.07.xml"));

	        // access message header data from the java model
	        LOG.info("Header from: " + mx.getAppHdr().from());
	        LOG.info("Header to: " + mx.getAppHdr().to());
	        LOG.info("Header reference: " + mx.getAppHdr().reference());

	        // notice the from/to methods in the generic model will only return values when the header BIC option is
	        // present. For other structure options such as reading a ClrSysMmbId you can further cast this to a specific
	        // AppHdr implementation. The AppHdr is just an interface.

	        // access message document data from the java model
	        
	        FIToFICustomerCreditTransferV07 creditTransfer = mx.getFIToFICstmrCdtTrf();
	        ActiveCurrencyAndAmount amount = creditTransfer.getCdtTrfTxInf().get(0).getIntrBkSttlmAmt();
	        
	        LOG.info("Amount: " + amount.getCcy() + " " + amount.getValue());
	        
			idlValue = IDLValue.create(creditTransfer, JAXBSerializer.create());
			args = new ArrayList<IDLValue>();
			args.add(idlValue);

			idlArgs = IDLArgs.create(args);
			
			buf = idlArgs.toBytes();
			
			FIToFICustomerCreditTransferV07 creditTransferResult = IDLArgs.fromBytes(buf).getArgs().get(0)
					.getValue(JAXBDeserializer.create(), FIToFICustomerCreditTransferV07.class);

			//Verify XML Content
	        String xmlContentBefore = mx.document();
	          
	        LOG.info(xmlContentBefore);	        

	        mx.setFIToFICstmrCdtTrf(creditTransferResult);
	           
	        //Verify XML Content
	        String xmlContentAfter = mx.document();
	        
	        LOG.info(xmlContentAfter);	   
	        
	        Assertions.assertEquals(xmlContentBefore, xmlContentAfter);	
	        
	        // parse the XML message content from a resource file
	        MxPacs00900109 mx2 = MxPacs00900109.parse(Lib.readResource("pacs.009.001.09.xml"));
	        
	        FinancialInstitutionCreditTransferV09 financialInstitutionTransfer = mx2.getFICdtTrf();
	        
	        amount = financialInstitutionTransfer.getCdtTrfTxInf().get(0).getIntrBkSttlmAmt();
	        
	        LOG.info("Amount: " + amount.getCcy() + " " + amount.getValue());
	        
			idlValue = IDLValue.create(financialInstitutionTransfer, JAXBSerializer.create());
			args = new ArrayList<IDLValue>();
			args.add(idlValue);

			idlArgs = IDLArgs.create(args);
			
			buf = idlArgs.toBytes();
			
			
			FinancialInstitutionCreditTransferV09 financialInstitutionTransferResult = IDLArgs.fromBytes(buf).getArgs().get(0)
					.getValue(JAXBDeserializer.create(), FinancialInstitutionCreditTransferV09.class);

			//Verify XML Content
	        xmlContentBefore = mx2.document();
	          
	        LOG.info(xmlContentBefore);	        

	        mx2.setFICdtTrf(financialInstitutionTransferResult);
	           
	        //Verify XML Content
	        xmlContentAfter = mx2.document();
	        
	        LOG.info(xmlContentAfter);	   
	        
	        Assertions.assertEquals(xmlContentBefore, xmlContentAfter);	
	        
			MxPain00100103 mx3 = MxPain00100103.parse(Lib.readResource("pain.001.001.03.xml"));
			
			CustomerCreditTransferInitiationV03 customerTransfer = mx3.getCstmrCdtTrfInitn(); 
			
			idlValue = IDLValue.create(customerTransfer, JAXBSerializer.create());
			args = new ArrayList<IDLValue>();
			args.add(idlValue);

			idlArgs = IDLArgs.create(args);
			
			buf = idlArgs.toBytes();
			
					
		} catch (JAXBException | IOException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}

	}

}
