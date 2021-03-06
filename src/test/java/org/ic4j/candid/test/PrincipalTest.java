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
