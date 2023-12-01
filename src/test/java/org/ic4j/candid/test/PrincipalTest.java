package org.ic4j.candid.test;

import org.ic4j.types.Principal;
import org.ic4j.types.PrincipalError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrincipalTest {
	static final Logger LOG = LoggerFactory.getLogger(PrincipalTest.class);

	@Test
	public void test() {

		Principal principal;

		try {
			principal = Principal.anonymous();
			
			Assertions.assertEquals(principal.toString(), "2vxsx-fae");
			
			principal = Principal.managementCanister();
			
			Assertions.assertEquals(principal.toString(), "aaaaa-aa");			
			
			principal = Principal.fromString("aaaaa-aa");

			Assertions.assertEquals(principal.toString(), "aaaaa-aa");
			
			principal = Principal.fromString("2vxsx-fae");

			Assertions.assertEquals(principal.toString(), "2vxsx-fae");			

			principal = Principal.fromString("rrkah-fqaaa-aaaaa-aaaaq-cai");

			Assertions.assertEquals(principal.toString(), "rrkah-fqaaa-aaaaa-aaaaq-cai");

			principal = Principal.fromString("w7x7r-cok77-xa");

			Assertions.assertEquals(principal.toString(), "w7x7r-cok77-xa");

			principal = Principal.fromString("22w4c-cyaaa-aaaab-qacka-cai");

			Assertions.assertEquals(principal.toString(), "22w4c-cyaaa-aaaab-qacka-cai");
			
			principal = Principal.fromString("aabzk-v6a3g-5u6a6-vcoqh-effzk-jb676-f47sd-bkqgg-afgmm-7wlka-hqe");

			Assertions.assertEquals(principal.toString(), "aabzk-v6a3g-5u6a6-vcoqh-effzk-jb676-f47sd-bkqgg-afgmm-7wlka-hqe");			
			
			
			Principal principal1 = Principal.fromString("22w4c-cyaaa-aaaab-qacka-cai");
			Principal principal2 = Principal.fromString("22w4c-cyaaa-aaaab-qacka-cai");
			Principal principal3 = Principal.fromString("aabzk-v6a3g-5u6a6-vcoqh-effzk-jb676-f47sd-bkqgg-afgmm-7wlka-hqe");
			
			Principal principal4 = null;
			
			Principal principal5 = Principal.from(null);
			
			Principal principal6 = Principal.from(null);			
			
			Assertions.assertTrue(principal1.equals(principal2));
			
			Assertions.assertFalse(principal1.equals(principal3));
			
			Assertions.assertTrue(principal5.equals(principal6));
			
			Assertions.assertFalse(principal1.equals(principal4));
			
			Assertions.assertFalse(principal1.equals(principal5));	
			
			LOG.info(Integer.toString(principal1.hashCode()));
			Assertions.assertEquals(-1752726076,principal1.hashCode());
			LOG.info(Integer.toString(principal3.hashCode()));
			Assertions.assertEquals(1713895155,principal3.hashCode());			
			LOG.info(Integer.toString(principal5.hashCode()));
			Assertions.assertEquals(32,principal5.hashCode());			

		} catch (PrincipalError e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}

		try {
			principal = Principal.fromString("RRkah-fqaaa-aaaaa-aaaaq-cai");

		} catch (PrincipalError e) {
			Assertions.assertEquals(e.getCode(), PrincipalError.PrincipalErrorCode.ABNORMAL_TEXTUAL_FORMAT);
		}

		try {
			principal = Principal.fromString("rr");

		} catch (PrincipalError e) {
			Assertions.assertEquals(e.getCode(), PrincipalError.PrincipalErrorCode.TEXT_TOO_SMALL);
		}

	}

}
