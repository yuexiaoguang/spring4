package org.springframework.web.bind.support;

/**
 * {@link SessionStatus}接口的简单实现, 将{@code complete}标志保持为实例变量.
 */
public class SimpleSessionStatus implements SessionStatus {

	private boolean complete = false;

	@Override
	public void setComplete() {
		this.complete = true;
	}

	@Override
	public boolean isComplete() {
		return this.complete;
	}
}
