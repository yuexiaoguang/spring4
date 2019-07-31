package org.springframework.web.context.request;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Web请求的通用接口.
 * 主要用于通用Web请求拦截器, 使其可以访问一般请求元数据, 而不是实际处理请求.
 */
public interface WebRequest extends RequestAttributes {

	/**
	 * 返回给定名称的请求header, 或{@code null}.
	 * <p>在多值header的情况下检索第一个header值.
	 */
	String getHeader(String headerName);

	/**
	 * 返回给定header名称的请求header值, 或{@code null}.
	 * <p>单个值的header将作为具有单个元素的数组公开.
	 */
	String[] getHeaderValues(String headerName);

	/**
	 * 返回请求header名称的Iterator.
	 */
	Iterator<String> getHeaderNames();

	/**
	 * 返回给定名称的请求参数, 或{@code null}.
	 * <p>在多值参数的情况下检索第一个参数值.
	 */
	String getParameter(String paramName);

	/**
	 * 返回给定参数名称的请求参数值, 或{@code null}.
	 * <p>单个值的参数将作为具有单个元素的数组公开.
	 */
	String[] getParameterValues(String paramName);

	/**
	 * 返回请求参数名称的Iterator.
	 */
	Iterator<String> getParameterNames();

	/**
	 * 返回请求参数的不可变Map, 参数名称为映射键, 参数值为映射值.
	 * <p>单个值的参数将作为具有单个元素的数组公开.
	 */
	Map<String, String[]> getParameterMap();

	/**
	 * 返回此请求的主要Locale.
	 */
	Locale getLocale();

	/**
	 * 返回此请求的上下文路径 (通常是当前Web应用程序映射到的基本路径).
	 */
	String getContextPath();

	/**
	 * 返回此请求的远程用户.
	 */
	String getRemoteUser();

	/**
	 * 返回此请求的用户主体.
	 */
	Principal getUserPrincipal();

	/**
	 * 确定用户是否有此请求的给定角色.
	 */
	boolean isUserInRole(String role);

	/**
	 * 返回此请求是否通过安全传输机制(例如 SSL)发送.
	 */
	boolean isSecure();

	/**
	 * 根据提供的last-modified时间戳 (由应用程序确定)检查是否已修改所请求的资源.
	 * <p>这也将在适用时透明地设置"Last-Modified"响应header和HTTP状态.
	 * <p>Typical usage:
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest webRequest, Model model) {
	 *   long lastModified = // application-specific calculation
	 *   if (request.checkNotModified(lastModified)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于条件GET/HEAD请求, 但也适用于条件POST/PUT/DELETE请求.
	 * <p><strong>Note:</strong> 可以使用这个{@code #checkNotModified(long)}方法; 或{@link #checkNotModified(String)}.
	 * 如果要按照HTTP规范的建议强制强实体标记和Last-Modified值, 则应使用{@link #checkNotModified(String, long)}.
	 * <p>如果设置了"If-Modified-Since" header但无法解析为日期值, 则此方法将忽略header, 并继续设置响应的last-modified时间戳.
	 * 
	 * @param lastModifiedTimestamp 应用程序为底层资源确定的last-modified时间戳, 以毫秒为单位
	 * 
	 * @return 请求是否符合未修改的条件, 允许中止请求处理并依赖响应, 告知客户端内容未被修改
	 */
	boolean checkNotModified(long lastModifiedTimestamp);

	/**
	 * 根据应用程序确定的提供的{@code ETag} (实体标记), 检查所请求的资源是否已被修改.
	 * <p>这也将在适用时透明地设置 "ETag"响应 header和HTTP状态.
	 * <p>典型用法:
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest webRequest, Model model) {
	 *   String eTag = // application-specific calculation
	 *   if (request.checkNotModified(eTag)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p><strong>Note:</strong> 可以使用这个{@code #checkNotModified(String)}方法; 或{@link #checkNotModified(long)}.
	 * 如果要按照HTTP规范的建议强制强实体标记和Last-Modified值, 则应使用{@link #checkNotModified(String, long)}.
	 * 
	 * @param etag 应用程序为底层资源确定的实体标记.
	 * 如有必要, 此参数将用引号 (")填充.
	 * 
	 * @return true 如果请求不需要进一步处理.
	 */
	boolean checkNotModified(String etag);

	/**
	 * 根据提供的{@code ETag} (实体标签)和last-modified时间戳, 检查所请求的资源是否已被修改.
	 * <p>这也将透明地设置"ETag"和"Last-Modified"响应header, 以及HTTP状态.
	 * <p>典型用法:
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest webRequest, Model model) {
	 *   String eTag = // application-specific calculation
	 *   long lastModified = // application-specific calculation
	 *   if (request.checkNotModified(eTag, lastModified)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于条件GET/HEAD请求, 但也适用于条件POST/PUT/DELETE请求.
	 * <p><strong>Note:</strong> HTTP规范建议设置ETag和Last-Modified值,
	 * 但也可以使用{@code #checkNotModified(String)}或{@link #checkNotModified(long)}.
	 * 
	 * @param etag 应用程序为底层资源确定的实体标记. 如有必要, 此参数将用引号 (")填充.
	 * @param lastModifiedTimestamp 应用程序为底层资源确定的last-modified时间戳, 以毫秒为单位
	 * 
	 * @return true 如果请求不需要进一步处理.
	 */
	boolean checkNotModified(String etag, long lastModifiedTimestamp);

	/**
	 * 获取此请求的简短描述, 通常包含请求URI和会话ID.
	 * 
	 * @param includeClientInfo 是否包含特定于客户端的信息, 例如会话ID和用户名
	 * 
	 * @return 请求的描述
	 */
	String getDescription(boolean includeClientInfo);

}
