package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver}实现,
 * 允许将异常类名称映射到视图名称, 无论是一组给定处理器, 还是DispatcherServlet中的所有处理器.
 *
 * <p>错误视图类似于错误页面JSP, 但可以与任何类型的异常一起使用,
 * 包括任何受检异常, 具有针对特定处理程序的细粒度映射.
 */
public class SimpleMappingExceptionResolver extends AbstractHandlerExceptionResolver {

	/** 异常属性的默认名称: "exception". */
	public static final String DEFAULT_EXCEPTION_ATTRIBUTE = "exception";


	private Properties exceptionMappings;

	private Class<?>[] excludedExceptions;

	private String defaultErrorView;

	private Integer defaultStatusCode;

	private Map<String, Integer> statusCodes = new HashMap<String, Integer>();

	private String exceptionAttribute = DEFAULT_EXCEPTION_ATTRIBUTE;


	/**
	 * 设置异常类名称和错误视图名称之间的映射.
	 * 异常类名称可以是子字符串, 目前没有通配符支持.
	 * 例如"ServletException"将匹配{@code javax.servlet.ServletException}及其子类.
	 * <p><b>NB:</b> 仔细考虑模式的具体程度, 以及是否包含包信息 (这不是强制性的).
	 * 例如, "Exception"几乎可以匹配任何内容, 并且可能会隐藏其他规则.
	 * 如果"Exception"用于为所有受检异常定义规则, 则"java.lang.Exception"将是正确的.
	 * 使用更多不寻常的异常名称, 例如"BaseBusinessException", 不需要使用FQN.
	 * 
	 * @param mappings 异常模式 (也可以是完全限定的类名)作为键, 错误视图名称作为值
	 */
	public void setExceptionMappings(Properties mappings) {
		this.exceptionMappings = mappings;
	}

	/**
	 * 设置要从异常映射中排除的一个或多个异常.
	 * 首先检查排除的异常, 如果其中一个等于实际异常, 则异常将保持未解析状态.
	 * 
	 * @param excludedExceptions 一个或多个排除的异常类型
	 */
	public void setExcludedExceptions(Class<?>... excludedExceptions) {
		this.excludedExceptions = excludedExceptions;
	}

	/**
	 * 设置默认错误视图的名称.
	 * 如果未找到特定映射, 则将返回此视图.
	 * <p>默认无.
	 */
	public void setDefaultErrorView(String defaultErrorView) {
		this.defaultErrorView = defaultErrorView;
	}

	/**
	 * 设置此异常解析器将应用于给定解析的错误视图的HTTP状态码.
	 * 键是视图名称; 值是状态码.
	 * <p>请注意, 此错误码仅在顶级请求的情况下才会应用.
	 * include请求不会设置, 因为无法从include内修改HTTP状态.
	 * <p>如果未指定, 将应用默认状态码.
	 */
	public void setStatusCodes(Properties statusCodes) {
		for (Enumeration<?> enumeration = statusCodes.propertyNames(); enumeration.hasMoreElements();) {
			String viewName = (String) enumeration.nextElement();
			Integer statusCode = Integer.valueOf(statusCodes.getProperty(viewName));
			this.statusCodes.put(viewName, statusCode);
		}
	}

	/**
	 * {@link #setStatusCodes(Properties)}的替代方法, 用于基于Java的配置.
	 */
	public void addStatusCode(String viewName, int statusCode) {
		this.statusCodes.put(viewName, statusCode);
	}

	/**
	 * 返回通过{@link #setStatusCodes(Properties)}提供的HTTP状态码.
	 * 键是视图名称; 值是状态码.
	 */
	public Map<String, Integer> getStatusCodesAsMap() {
		return Collections.unmodifiableMap(statusCodes);
	}

	/**
	 * 设置此异常解析器应用的默认HTTP状态码, 在解析错误视图且未定义状态代码映射时.
	 * <p>请注意, 此错误码仅在顶级请求的情况下才会应用.
	 * include请求不会设置, 因为无法从include内修改HTTP状态.
	 * <p>如果未指定, 则不会应用任何状态码, 将其保留到控制器或视图, 或保持servlet引擎的默认值为 200 (OK).
	 * 
	 * @param defaultStatusCode HTTP状态码值,
	 * 例如500 ({@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR}) 或404 ({@link HttpServletResponse#SC_NOT_FOUND})
	 */
	public void setDefaultStatusCode(int defaultStatusCode) {
		this.defaultStatusCode = defaultStatusCode;
	}

	/**
	 * 设置应该公开异常的model属性的名称.
	 * 默认为"exception".
	 * <p>这可以设置为不同的属性名称, 也可以设置为{@code null}, 以便根本不暴露异常属性.
	 */
	public void setExceptionAttribute(String exceptionAttribute) {
		this.exceptionAttribute = exceptionAttribute;
	}


	/**
	 * 实际解析在处理器执行期间抛出的给定异常, 返回表示特定错误页面的ModelAndView.
	 * <p>可以在子类中重写, 以便应用特定的异常检查.
	 * 请注意, 在检查此解析是否适用<i>之后</i>("mappedHandlers" etc),
	 * 将调用此模板方法, 因此实现可能只是继续其实际的异常处理.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null}, 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发到的相应{@code ModelAndView}, 或{@code null}以进行解析链中的默认处理
	 */
	@Override
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

		// 为所选的错误视图公开ModelAndView.
		String viewName = determineViewName(ex, request);
		if (viewName != null) {
			// 如果指定, 为错误视图应用HTTP状态码.
			// 仅在处理顶级请求时才应用它.
			Integer statusCode = determineStatusCode(request, viewName);
			if (statusCode != null) {
				applyStatusCodeIfPossible(request, response, statusCode);
			}
			return getModelAndView(viewName, ex, request);
		}
		else {
			return null;
		}
	}

	/**
	 * 确定给定异常的视图名称, 首先检查{@link #setExcludedExceptions(Class[]) "excludedExecptions"},
	 * 然后搜索{@link #setExceptionMappings "exceptionMappings"},
	 * 最后使用{@link #setDefaultErrorView "defaultErrorView"}作为后备.
	 * 
	 * @param ex 在处理器执行期间抛出的异常
	 * @param request 当前HTTP请求 (对获取元数据很有用)
	 * 
	 * @return 已解析的视图名称, 或{@code null} 如果已排除或未找到
	 */
	protected String determineViewName(Exception ex, HttpServletRequest request) {
		String viewName = null;
		if (this.excludedExceptions != null) {
			for (Class<?> excludedEx : this.excludedExceptions) {
				if (excludedEx.equals(ex.getClass())) {
					return null;
				}
			}
		}
		// 检查特定的异常映射.
		if (this.exceptionMappings != null) {
			viewName = findMatchingViewName(this.exceptionMappings, ex);
		}
		// 如果已定义, 则返回默认的错误视图.
		if (viewName == null && this.defaultErrorView != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving to default view '" + this.defaultErrorView + "' for exception of type [" +
						ex.getClass().getName() + "]");
			}
			viewName = this.defaultErrorView;
		}
		return viewName;
	}

	/**
	 * 在给定的异常映射中查找匹配的视图名称.
	 * 
	 * @param exceptionMappings 异常类名称和错误视图名称之间的映射
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 视图名称, 或{@code null}
	 */
	protected String findMatchingViewName(Properties exceptionMappings, Exception ex) {
		String viewName = null;
		String dominantMapping = null;
		int deepest = Integer.MAX_VALUE;
		for (Enumeration<?> names = exceptionMappings.propertyNames(); names.hasMoreElements();) {
			String exceptionMapping = (String) names.nextElement();
			int depth = getDepth(exceptionMapping, ex);
			if (depth >= 0 && (depth < deepest || (depth == deepest &&
					dominantMapping != null && exceptionMapping.length() > dominantMapping.length()))) {
				deepest = depth;
				dominantMapping = exceptionMapping;
				viewName = exceptionMappings.getProperty(exceptionMapping);
			}
		}
		if (viewName != null && logger.isDebugEnabled()) {
			logger.debug("Resolving to view '" + viewName + "' for exception of type [" + ex.getClass().getName() +
					"], based on exception mapping [" + dominantMapping + "]");
		}
		return viewName;
	}

	/**
	 * 返回匹配的超类的深度.
	 * <p>0表示ex完全匹配. 如果没有匹配则返回-1.
	 * 否则, 返回深度. 最低深度胜利.
	 */
	protected int getDepth(String exceptionMapping, Exception ex) {
		return getDepth(exceptionMapping, ex.getClass(), 0);
	}

	private int getDepth(String exceptionMapping, Class<?> exceptionClass, int depth) {
		if (exceptionClass.getName().contains(exceptionMapping)) {
			// Found it!
			return depth;
		}
		// 如果走得尽可能远, 却找不到它...
		if (exceptionClass == Throwable.class) {
			return -1;
		}
		return getDepth(exceptionMapping, exceptionClass.getSuperclass(), depth + 1);
	}

	/**
	 * 确定要应用于给定错误视图的HTTP状态码.
	 * <p>默认实现返回给定视图名称的状态码 (通过{@link #setStatusCodes(Properties) statusCodes}属性指定),
	 * 如果没有匹配则返回{@link #setDefaultStatusCode defaultStatusCode}.
	 * <p>在自定义子类中重写此选项以自定义此行为.
	 * 
	 * @param request 当前的HTTP请求
	 * @param viewName 错误视图的名称
	 * 
	 * @return 要使用的HTTP状态码, 或{@code null}表示servlet容器的默认值 (如果是标准错误视图, 则为200)
	 */
	protected Integer determineStatusCode(HttpServletRequest request, String viewName) {
		if (this.statusCodes.containsKey(viewName)) {
			return this.statusCodes.get(viewName);
		}
		return this.defaultStatusCode;
	}

	/**
	 * 如果可能, 将指定的HTTP状态码应用于给定的响应 (即, 如果未在include请求中执行).
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param statusCode 要应用的状态码
	 */
	protected void applyStatusCodeIfPossible(HttpServletRequest request, HttpServletResponse response, int statusCode) {
		if (!WebUtils.isIncludeRequest(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying HTTP status code " + statusCode);
			}
			response.setStatus(statusCode);
			request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, statusCode);
		}
	}

	/**
	 * 返回给定请求, 视图名称和异常的ModelAndView.
	 * <p>默认实现委托给{@link #getModelAndView(String, Exception)}.
	 * 
	 * @param viewName 错误视图的名称
	 * @param ex 在处理器执行期间抛出的异常
	 * @param request 当前的HTTP请求 (用于获取元数据)
	 * 
	 * @return ModelAndView 实例
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex, HttpServletRequest request) {
		return getModelAndView(viewName, ex);
	}

	/**
	 * 返回给定视图名称和异常的ModelAndView.
	 * <p>默认实现添加指定的异常属性. 可以在子类中重写.
	 * 
	 * @param viewName 错误视图的名称
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return ModelAndView 实例
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex) {
		ModelAndView mv = new ModelAndView(viewName);
		if (this.exceptionAttribute != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Exposing Exception as model attribute '" + this.exceptionAttribute + "'");
			}
			mv.addObject(this.exceptionAttribute, ex);
		}
		return mv;
	}
}
