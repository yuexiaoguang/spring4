package org.springframework.web.servlet;

/**
 * 提供有关View的其他信息, 例如它是否执行重定向.
 */
public interface SmartView extends View {

	/**
	 * 视图是否执行重定向.
	 */
	boolean isRedirectView();

}
