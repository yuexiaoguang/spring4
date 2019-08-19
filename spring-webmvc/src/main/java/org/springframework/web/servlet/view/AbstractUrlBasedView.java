package org.springframework.web.servlet.view;

import java.util.Locale;

import org.springframework.beans.factory.InitializingBean;

/**
 * 基于URL的视图的抽象基类.
 * 提供以"url" bean属性的形式保存View包装的URL的一致方法.
 */
public abstract class AbstractUrlBasedView extends AbstractView implements InitializingBean {

	private String url;


	protected AbstractUrlBasedView() {
	}

	/**
	 * @param url 转发到的URL
	 */
	protected AbstractUrlBasedView(String url) {
		this.url = url;
	}


	/**
	 * 设置此视图包装的资源的URL.
	 * URL必须适合具体的View实现.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * 返回此视图包装的资源的URL.
	 */
	public String getUrl() {
		return this.url;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isUrlRequired() && getUrl() == null) {
			throw new IllegalArgumentException("Property 'url' is required");
		}
	}

	/**
	 * 返回是否需要'url'属性.
	 * <p>默认实现返回{@code true}.
	 * 可以在子类中重写.
	 */
	protected boolean isUrlRequired() {
		return true;
	}

	/**
	 * 检查配置的URL指向的底层资源是否实际存在.
	 * 
	 * @param locale 正在寻找的所需的区域设置
	 * 
	 * @return {@code true} 如果资源存在 (或假设存在); {@code false} 如果已知它不存在
	 * @throws Exception 如果资源存在但无效 (e.g. 无法解析)
	 */
	public boolean checkResource(Locale locale) throws Exception {
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + "; URL [" + getUrl() + "]";
	}

}
