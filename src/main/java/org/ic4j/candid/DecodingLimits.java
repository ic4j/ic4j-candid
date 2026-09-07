package org.ic4j.candid;

public final class DecodingLimits {
	public static final DecodingLimits DEFAULT = new DecodingLimits(16 * 1024 * 1024, 1000000, 2000000, 100000, 128);

	public final int maxMessageBytes;
	public final int maxCollectionLength;
	public final int maxTotalValues;
	public final int maxTypeEntries;
	public final int maxDepth;

	public DecodingLimits(int maxMessageBytes, int maxCollectionLength, int maxTotalValues,
			int maxTypeEntries, int maxDepth) {
		if (maxMessageBytes <= 0 || maxCollectionLength <= 0 || maxTotalValues <= 0
				|| maxTypeEntries <= 0 || maxDepth <= 0)
			throw new IllegalArgumentException("Decoding limits must be positive");
		this.maxMessageBytes = maxMessageBytes;
		this.maxCollectionLength = maxCollectionLength;
		this.maxTotalValues = maxTotalValues;
		this.maxTypeEntries = maxTypeEntries;
		this.maxDepth = maxDepth;
	}
}