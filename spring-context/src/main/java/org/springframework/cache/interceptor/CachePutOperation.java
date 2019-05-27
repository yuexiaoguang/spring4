package org.springframework.cache.interceptor;

/**
 * 描述缓存'put'操作的类.
 */
public class CachePutOperation extends CacheOperation {

	private final String unless;


	/**
	 * @since 4.3
	 */
	public CachePutOperation(CachePutOperation.Builder b) {
		super(b);
		this.unless = b.unless;
	}


	public String getUnless() {
		return this.unless;
	}


	/**
	 * @since 4.3
	 */
	public static class Builder extends CacheOperation.Builder {

		private String unless;

		public void setUnless(String unless) {
			this.unless = unless;
		}

		@Override
		protected StringBuilder getOperationDescription() {
			StringBuilder sb = super.getOperationDescription();
			sb.append(" | unless='");
			sb.append(this.unless);
			sb.append("'");
			return sb;
		}

		public CachePutOperation build() {
			return new CachePutOperation(this);
		}
	}

}
