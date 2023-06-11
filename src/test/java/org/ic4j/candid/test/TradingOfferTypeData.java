package org.ic4j.candid.test;

import java.util.Optional;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.types.Type;

public class TradingOfferTypeData {

		@Field(Type.TEXT)
		Optional<String> metadata;


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TradingOfferTypeData other = (TradingOfferTypeData) obj;
			if (metadata == null) {
				if (other.metadata != null)
					return false;
			} else if (!metadata.equals(other.metadata))
				return false;
			return true;
		}
}
