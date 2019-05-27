package org.springframework.mock.web.portlet;

import javax.portlet.CacheControl;

/**
 * Mock implementation of the {@link javax.portlet.CacheControl} interface.
 */
public class MockCacheControl implements CacheControl {

	private int expirationTime = 0;

	private boolean publicScope = false;

	private String etag;

	private boolean useCachedContent = false;


	@Override
	public int getExpirationTime() {
		return this.expirationTime;
	}

	@Override
	public void setExpirationTime(int time) {
		this.expirationTime = time;
	}

	@Override
	public boolean isPublicScope() {
		return this.publicScope;
	}

	@Override
	public void setPublicScope(boolean publicScope) {
		this.publicScope = publicScope;
	}

	@Override
	public String getETag() {
		return this.etag;
	}

	@Override
	public void setETag(String token) {
		this.etag = token;
	}

	@Override
	public boolean useCachedContent() {
		return this.useCachedContent;
	}

	@Override
	public void setUseCachedContent(boolean useCachedContent) {
		this.useCachedContent = useCachedContent;
	}

}
