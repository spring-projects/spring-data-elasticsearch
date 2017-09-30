package org.springframework.data.elasticsearch.annotations;

/**
 * @author Nikita Klimov
 */
public enum TermVector {
	/**
	 * No term vectors are stored. (default)
	 */
	NO("no"),

	/**
	 * Just the terms in the field are stored
	 */
	YES("yes"),

	/**
	 * Terms and positions are stored
	 */
	WITH_POSITIONS("with_positions"),

	/**
	 * Terms and character offsets are stored
	 */
	WITH_OFFSETS("with_offsets"),

	/**
	 * Terms, positions, and character offsets are stored
	 */
	WITH_POSITIONS_OFFSETS("with_positions_offsets");

	private String termVectorType;

	TermVector(String termVectorType) {
		this.termVectorType = termVectorType;
	}

	@Override
	public String toString() {
		return this.termVectorType;
	}
}
