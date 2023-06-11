package org.ic4j.candid.test;


import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.types.Type;

public class TradingOfferType{	
	
	@Field(Type.RECORD)
	TradingOfferTypeData nonfungible;



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TradingOfferType other = (TradingOfferType) obj;
		if (nonfungible == null) {
			if (other.nonfungible != null)
				return false;
		} else if (!nonfungible.equals(other.nonfungible))
			return false;
		return true;
	}
	
	
	

}
