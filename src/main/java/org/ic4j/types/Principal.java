/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.types;

import java.util.Arrays;
import java.util.Optional;
import java.util.zip.CRC32;

//import org.apache.commons.codec.binary.Base32;
//import org.apache.commons.codec.digest.DigestUtils;
//import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_224;

import org.bouncycastle.util.encoders.Base32;
import org.bouncycastle.jcajce.provider.digest.SHA224;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


@JsonSerialize(using = PrincipalSerializer.class)
@JsonDeserialize(using = PrincipalDeserializer.class)
public final class Principal implements Cloneable {
	static final byte ID_ANONYMOUS_BYTES = PrincipalClass.ANONYMOUS.value.byteValue();
	
	static Base32 codec = new Base32();
	
	PrincipalInner principalInner;
	Optional<byte[]> value;
	
	Principal()
	{
	}

	Principal(PrincipalInner principalInner) {
		this.principalInner = principalInner;
		
		if(principalInner == PrincipalInner.MANAGEMENT_CANISTER)
		{
			byte[] value = {};
			this.value = Optional.of(value);
		}
		else if(principalInner == PrincipalInner.ANONYMOUS)
		{
			byte[] value = {ID_ANONYMOUS_BYTES};
			this.value = Optional.of(value);			
		}
		else
			this.value = Optional.empty();
	}

	Principal(PrincipalInner principalInner, byte[] value) {
		this.principalInner = principalInner;
		this.value = Optional.of(value);
	}

	public static Principal managementCanister() {
		byte[] value = {};
		return new Principal(PrincipalInner.MANAGEMENT_CANISTER, value);
	}

    // An anonymous Principal.
	public static Principal anonymous() {
		byte[] value = {Principal.ID_ANONYMOUS_BYTES};
		return new Principal(PrincipalInner.ANONYMOUS,value);
	}

    // Right now we are enforcing a Twisted Edwards Curve 25519 point
    // as the public key.
	public static Principal selfAuthenticating(byte[] publicKey) {
		//DigestUtils digestUtils = new DigestUtils(SHA_224);
		
		SHA224.Digest digestUtils = new SHA224.Digest();
		
		byte[] value = digestUtils.digest(publicKey);
		
		// Now add a suffix denoting the identifier as representing a
        // self-authenticating principal.
		value = ArrayUtils.add(value,PrincipalClass.SELF_AUTHENTICATING.value.byteValue());
		
		return new Principal(PrincipalInner.SELF_AUTHENTICATING,value);
	}	

    // Parse the text format for canister IDs (e.g., `jkies-sibbb-ap6`).
    // The text format follows the public spec (see Textual IDs section).	
	public static Principal fromString(String text) throws PrincipalError {

		//String value = makeAsciiLowerCase(text);
		String value = makeAsciiUpperCase(text);
		value = value.replace("-", "");
		
		value = addPadding(value);

		Optional<byte[]> bytes = Optional.ofNullable(Base32.decode(value));
		
		if (bytes.isPresent()) {
			if (bytes.get().length < 4) {
				throw PrincipalError.create(PrincipalError.PrincipalErrorCode.TEXT_TOO_SMALL);
			}

			Principal result = from(Arrays.copyOfRange(bytes.get(), 4, bytes.get().length));

			String expected = result.toString();

			if (text.equals(expected))
				return result;
			else
				throw PrincipalError.create(PrincipalError.PrincipalErrorCode.ABNORMAL_TEXTUAL_FORMAT, expected);
		} else
			throw PrincipalError.create(PrincipalError.PrincipalErrorCode.INVALID_TEXTUAL_FORMAT_NOT_BASE32);
		// Principal principal = new
		// Principal(PrincipalInner.ANONYMOUS,value.getBytes());

	}
	
	public byte[] getValue()
	{
		if (value.isPresent())
			return value.get();
		else 
			throw PrincipalError.create(PrincipalError.PrincipalErrorCode.EXTERNAL_ERROR,"Value is empty");			
	}

	/*
	public String toString() {
		if (value.isPresent()) {
			CRC32 hasher = new CRC32();

			hasher.update(value.get());

			// initializing byte array
			byte[] checksum = new byte[] { 0, 0, 0, 0 };

			if (hasher.getValue() > 0)
				checksum = BigInteger.valueOf(Long.valueOf(hasher.getValue()).intValue()).toByteArray();

			byte[] bytes = concatByteArrays(checksum, value.get());

			String output = codec.encodeAsString(bytes);

			output = makeAsciiLowerCase(output);

			// remove padding
			output = StringUtils.stripEnd(output, "=");
			output = output.replaceAll("(.{5})", "$1-");
			return output;
		} else
			return new String();
	}
	*/
	
    public String toString() {
        if (value.isPresent()) {
            final byte[] valueBytes = value.get();
            final byte[] checksum = toChecksumBytes(valueBytes);
            final byte[] bytes = concatByteArrays(checksum, valueBytes);

            //String output = codec.encodeAsString(bytes);
            
            String output = Base32.toBase32String(bytes);
            
            output = makeAsciiLowerCase(output);

            // remove padding
            output = StringUtils.stripEnd(output, "=");
            output = output.replaceAll("(.{5})", "$1-");
            return output;
        } else {
            return "";
        }
    }	
	
	public Principal clone()
	{
		Principal clone = new Principal();
		
		clone.principalInner = this.principalInner;
		clone.value = this.value;
		
		return clone;
		
	}
	

	static byte[] concatByteArrays(byte[] first, byte[] second) {
		byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	static String makeAsciiLowerCase(String input) {
		String output = new String();

		int i, n;
		char c;

		for (i = 0; i < input.length(); i++) {
			c = input.charAt(i);
			if (c >= 65 && c <= 90) // If ASCII values represent LowerCase, changing to Upper Case
			{
				n = c + 32;
				c = (char) n;
			}
			output = output + c;
		}

		return output;
	}
	
	public static String makeAsciiUpperCase(String input) {
	    StringBuilder output = new StringBuilder(input.length());

	    for (int i = 0; i < input.length(); i++) {
	        char c = input.charAt(i);
	        if (c >= 97 && c <= 122) { // If character is lowercase
	            c = (char) (c - 32); // Convert to uppercase
	        }
	        output.append(c);
	    }

	    return output.toString();
	}	
	
    public static String addPadding(String base32Encoded) {
        int length = base32Encoded.length();
        int remainder = length % 8;

        if (remainder != 0) {
            int paddingLength = 8 - remainder;
            StringBuilder padded = new StringBuilder(base32Encoded);
            for (int i = 0; i < paddingLength; i++) {
                padded.append('=');
            }
            return padded.toString();
        }

        return base32Encoded; // Already correctly padded
    }	

	public static Principal from(byte[] bytes) throws PrincipalError {
		if (Optional.ofNullable(bytes).isPresent() && bytes.length > 0) {
			Byte lastByte = bytes[bytes.length - 1];

			switch (PrincipalClass.from(lastByte)) {
			case OPAQUE_ID:
				return new Principal(PrincipalInner.OPAQUE_ID, bytes);
			case SELF_AUTHENTICATING:
				return new Principal(PrincipalInner.SELF_AUTHENTICATING, bytes);
			case DERIVED_ID:
				return new Principal(PrincipalInner.DERIVED_ID, bytes);
			case ANONYMOUS:
				if (bytes.length == 1)
					return new Principal(PrincipalInner.ANONYMOUS);
				else
					throw PrincipalError.create(PrincipalError.PrincipalErrorCode.BUFFER_TOO_LONG);

			case UNASSIGNED:
				return new Principal(PrincipalInner.UNASSIGNED, bytes);
			default:
				throw PrincipalError.create(PrincipalError.PrincipalErrorCode.ABNORMAL_TEXTUAL_FORMAT);
			}
		} else
			return new Principal(PrincipalInner.MANAGEMENT_CANISTER);
	}
	
    private byte[] toChecksumBytes(byte[] valueBytes) {
        final CRC32 hasher = new CRC32();
        hasher.update(valueBytes);

        final long hasherValue = hasher.getValue();
        if (hasherValue > 0) {
            return to4Bytes((int) hasherValue);
        }
        return new byte[]{0, 0, 0, 0};
    }

    private byte[] to4Bytes(int value) {
        final byte[] bytes = new byte[4];
        bytes[0] = (byte) ((value >>> 24) & 0xFF);
        bytes[1] = (byte) ((value >>> 16) & 0xFF);
        bytes[2] = (byte) ((value >>> 8) & 0xFF);
        bytes[3] = (byte) ((value) & 0xFF);
        return bytes;
    }	

	enum PrincipalInner {
		MANAGEMENT_CANISTER, OPAQUE_ID, SELF_AUTHENTICATING, DERIVED_ID, ANONYMOUS, UNASSIGNED;
	}

	enum PrincipalClass {
		UNASSIGNED(0), OPAQUE_ID(1), SELF_AUTHENTICATING(2), DERIVED_ID(3), ANONYMOUS(4);

		final Integer value;

		PrincipalClass(Integer value) {
			this.value = value;
		}

		static PrincipalClass from(Byte value) {
			switch (value) {
			case 1:
				return OPAQUE_ID;
			case 2:
				return SELF_AUTHENTICATING;
			case 3:
				return DERIVED_ID;
			case 4:
				return ANONYMOUS;
			default:
				return UNASSIGNED;
			}
		}

	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null  || !value.isPresent()) ? 0 : Arrays.hashCode(value.get()));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Principal other = (Principal) obj;
		if (value == null) {
			if (other.value == null)
				return true;
			else
				return false;
		} else if (other.value != null)
			return Arrays.equals(value.orElse(null), other.value.orElse(null));

		return false;		
	}	

}
