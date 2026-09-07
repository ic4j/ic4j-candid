package org.ic4j.candid;

final class DecodingBudget {
	final DecodingLimits limits;
	private int values;
	private int elements;
	private int typeEntries;
	private int depth;

	DecodingBudget(DecodingLimits limits) {
		this.limits = java.util.Objects.requireNonNull(limits, "limits");
	}

	void enterValue() {
		if (depth >= limits.maxDepth || values >= limits.maxTotalValues)
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Decoding depth or value limit exceeded");
		depth++;
		values++;
	}

	void exitValue() {
		depth--;
	}

	void reserveElements(int count) {
		if (count < 0 || count > limits.maxCollectionLength || count > limits.maxTotalValues - elements)
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Decoded collection limit exceeded");
		elements += count;
	}

	void reserveTypeEntries(int count) {
		if (count < 0 || count > limits.maxTypeEntries - typeEntries)
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Type table limit exceeded");
		typeEntries += count;
	}

	int readTypeCount(Bytes bytes) {
		int count = bytes.readLength();
		reserveTypeEntries(count);
		if (count > bytes.data.remaining())
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Unexpected end of type table");
		return count;
	}
}