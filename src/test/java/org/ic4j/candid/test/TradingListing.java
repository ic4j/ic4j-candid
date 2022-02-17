package org.ic4j.candid.test;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.annotations.Id;
import org.ic4j.candid.types.Type;

public class TradingListing {
	@Id(0)
	@Field(Type.NAT32)
	public Integer id;
	
	@Id(1)
	@Field(Type.RECORD)	
	public TradingOffer offer;
	
	@Id(2)
	@Field(Type.VARIANT)	
	public TradingOfferType offerType;


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TradingListing other = (TradingListing) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (offer == null) {
			if (other.offer != null)
				return false;
		} else if (!offer.equals(other.offer))
			return false;
		if (offerType == null) {
			if (other.offerType != null)
				return false;
		} else if (!offerType.equals(other.offerType))
			return false;
		return true;
	}	
	
	
	
}
