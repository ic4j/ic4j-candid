package org.ic4j.candid.test;

import java.util.Optional;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Principal;

public class TradingOffer{
	@Field(Type.BOOL)
	public Optional<Boolean> locked;
	
	@Field(Type.PRINCIPAL)
	Principal seller;
	
	@Field(Type.NAT64)
	Long price;


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TradingOffer other = (TradingOffer) obj;
		if (locked == null) {
			if (other.locked != null)
				return false;
		} else if (!locked.equals(other.locked))
			return false;
		if (price == null) {
			if (other.price != null)
				return false;
		} else if (!price.equals(other.price))
			return false;
		if (seller == null) {
			if (other.seller != null)
				return false;
		} else if (!seller.equals(other.seller))
			return false;
		return true;
	}
	
	
	
}
