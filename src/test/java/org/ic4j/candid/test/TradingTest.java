package org.ic4j.candid.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.ic4j.candid.ByteUtils;
import org.ic4j.candid.dom.DOMDeserializer;
import org.ic4j.candid.dom.DOMUtils;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.pojo.PojoDeserializer;
import org.ic4j.candid.pojo.PojoSerializer;
import org.ic4j.types.Principal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public final class TradingTest extends CandidAssert {

	static {
		LOG = LoggerFactory.getLogger(TradingTest.class);
	}

	@Test
	public void test() {				
		// Listings
		
		TradingListing listing = new TradingListing();
		
		listing.offer = new TradingOffer();
		listing.offerType = new TradingOfferType();
		
		listing.offer.locked = Optional.empty();
		listing.offer.seller = Principal.fromString("22w4c-cyaaa-aaaab-qacka-cai");
		listing.offer.price = 44000000000l;
		
		listing.offerType.nonfungible = new TradingOfferTypeData();
		listing.offerType.nonfungible.metadata = Optional.empty();

		listing.id = 1024;
			
		TradingListing[] listings = {listing};

		IDLValue idlValue = IDLValue.create(listings, new PojoSerializer());

		List<IDLValue> args = new ArrayList<IDLValue>();
		args.add(idlValue);

		IDLArgs idlArgs = IDLArgs.create(args);

		byte[] buf = idlArgs.toBytes();
		
		int[] unsignedBuf = ByteUtils.toUnsignedIntegerArray(buf);
		
		TradingListing[] listingsResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(new PojoDeserializer(), TradingListing[].class);
		
		Assertions.assertArrayEquals(listings, listingsResult);
		
		DOMDeserializer domDeserializer = DOMDeserializer.create(idlValue.getIDLType())
				.rootElement("http://ic4j.org/candid/test", "data");
		domDeserializer = domDeserializer.setAttributes(true);

		Node domNodeResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(domDeserializer, Node.class);
		
		try {
			String result = DOMUtils.getStringFromDocument(domNodeResult.getOwnerDocument());
			LOG.debug(result);
		} catch (TransformerException e) {
			LOG.error(e.getLocalizedMessage(), e);
		}
	}
}
