package org.springframework.web.servlet;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用于Web交互的MVC View.
 * 实现负责呈现内容并公开模型. 单个视图公开多个模型属性.
 *
 * <p>This class and the MVC approach associated with it is discussed in Chapter 12 of
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 *
 * <p>视图实现可能有很大不同. 一个明显的实现是基于JSP的.
 * 其他实现可能是基于XSLT的, 或使用HTML生成库.
 * 此接口旨在避免限制可能的实现的范围.
 *
 * <p>View应该是bean. 它们很可能被ViewResolver实例化为bean.
 * 由于此接口是无状态的, 因此视图实现应该是线程安全的.
 */
public interface View {

	/**
	 * 包含响应状态码的{@link HttpServletRequest}属性的名称.
	 * <p>Note: 所有View实现都不需要此属性.
	 */
	String RESPONSE_STATUS_ATTRIBUTE = View.class.getName() + ".responseStatus";

	/**
	 * 包含带路径变量的Map的{@link HttpServletRequest}属性的名称.
	 * 该映射包含基于String的URI模板变量名称作为键及其对应的基于Object的值 -- 从URL的段中提取并转换的类型.
	 * <p>Note: 所有View实现都不需要此属性.
	 */
	String PATH_VARIABLES = View.class.getName() + ".pathVariables";

	/**
	 * 在内容协商期间选择{@link org.springframework.http.MediaType}, 这可能比View配置的更具体.
	 * 例如: "application/vnd.example-v1+xml" vs "application/*+xml".
	 */
	String SELECTED_CONTENT_TYPE = View.class.getName() + ".selectedContentType";


	/**
	 * 如果预先确定, 则返回视图的内容类型.
	 * <p>可用于预先检查视图的内容类型, i.e. 在实际渲染尝试之前.
	 * 
	 * @return 内容类型字符串 (可选地包括字符集), 或{@code null} 如果不能预先确定
	 */
	String getContentType();

	/**
	 * 渲染指定模型的视图.
	 * <p>第一步是准备请求: 在JSP情况下, 这意味着将模型对象设置为请求属性.
	 * 第二步是视图的实际呈现, 例如通过RequestDispatcher包含JSP.
	 * 
	 * @param model 名称字符串作为键, 相应的模型对象作为值的Map (对于空模型, Map也可以是{@code null})
	 * @param request 当前的HTTP请求
	 * @param response 正在构建的HTTP响应
	 * 
	 * @throws Exception 如果渲染失败
	 */
	void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
