package org.ic4j.candid.types;

import org.ic4j.candid.CandidError;
import org.ic4j.candid.IDLUtils;

public class Label implements Comparable<Label> {
	Object value;
	LabelType type;

	public static Label createIdLabel(Long id) {
		Label label = new Label();
		label.type = LabelType.ID;
		label.value = id;

		return label;
	}

	public static Label createNamedLabel(String id) {
		Label label = new Label();
		label.type = LabelType.NAMED;
		label.value = id;

		return label;
	}

	public static Label createUnnamedLabel(Long id) {
		Label label = new Label();
		label.type = LabelType.UNNAMED;
		label.value = id;

		return label;
	}

	public Object getValue() {
		return this.value;
	}
	
	public LabelType getType()
	{
		return this.type;
	}

	public Long getId() {
		switch (this.type) {
		case ID:
			return (Long) this.value;
		case UNNAMED:
			return (Long) this.value;
		case NAMED:
			return IDLUtils.idlHash((String) this.value);
		default:
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Unrecognized Label Type");
		}

	}
	
	public enum LabelType {
		ID, NAMED, UNNAMED;
	}	

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;

		if (other instanceof Label)
			return (this.getId() == ((Label) other).getId());
		else if (other instanceof Integer)
			return (this.getId() == other);
		else if (other instanceof String)
			return (this.getId() == IDLUtils.idlHash((String) other));
		else
			return false;
	}

	@Override
	public int hashCode() {
		return (int)this.getId().longValue();
	}


	@Override
	public int compareTo(Label other) {
		return Long.compare(this.getId(), other.getId());
	}

	@Override
	public String toString() {
		if (value == null)
			return null;
		else
			return value.toString();
	}

}
