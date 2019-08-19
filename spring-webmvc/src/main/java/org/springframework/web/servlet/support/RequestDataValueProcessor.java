package org.springframework.web.servlet.support;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * 在视图呈现之前或重定向之前检查并可能修改请求数据值(如URL查询参数或表单字段值)的约定.
 *
 * <p>实现可以使用此约定作为解决方案的一部分, 以提供数据完整性, 机密性, 防止跨站点请求伪造 (CSRF)等,
 * 或者用于其他任务, 例如自动向所有表单和URL添加隐藏字段.
 *
 * <p>查看支持此约定的技术可以通过{@link RequestContext#getRequestDataValueProcessor()}获取要委托的实例.
 */
public interface RequestDataValueProcessor {

	/**
	 * 在呈现新表单操作时调用.
	 * 
	 * @param request 当前的请求
	 * @param action 表单动作
	 * @param httpMethod 表单HTTP方法
	 * 
	 * @return 要使用的动作, 可能已修改
	 */
	String processAction(HttpServletRequest request, String action, String httpMethod);

	/**
	 * 在呈现表单字段值时调用.
	 * 
	 * @param request 当前的请求
	 * @param name 表单字段名称
	 * @param value 表单字段值
	 * @param type 表单字段类型 ("text", "hidden", etc.)
	 * 
	 * @return 要使用的表单字段值, 可能已修改
	 */
	String processFormFieldValue(HttpServletRequest request, String name, String value, String type);

	/**
	 * 在渲染完所有表单字段后调用.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 要添加的其他隐藏表单字段, 或{@code null}
	 */
	Map<String, String> getExtraHiddenFields(HttpServletRequest request);

	/**
	 * 在即将呈现或重定向到URL时调用.
	 * 
	 * @param request 当前的请求
	 * @param url URL值
	 * 
	 * @return 要使用的URL, 可能已修改
	 */
	String processUrl(HttpServletRequest request, String url);

}
