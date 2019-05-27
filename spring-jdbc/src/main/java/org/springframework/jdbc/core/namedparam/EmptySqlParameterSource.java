package org.springframework.jdbc.core.namedparam;

/**
 * {@link SqlParameterSource}接口的简单空实现.
 */
public class EmptySqlParameterSource implements SqlParameterSource {

	/**
	 * {@link EmptySqlParameterSource}的共享实例.
	 */
	public static final EmptySqlParameterSource INSTANCE = new EmptySqlParameterSource();


	@Override
	public boolean hasValue(String paramName) {
		return false;
	}

	@Override
	public Object getValue(String paramName) throws IllegalArgumentException {
		throw new IllegalArgumentException("This SqlParameterSource is empty");
	}

	@Override
	public int getSqlType(String paramName) {
		return TYPE_UNKNOWN;
	}

	@Override
	public String getTypeName(String paramName) {
		return null;
	}

}
