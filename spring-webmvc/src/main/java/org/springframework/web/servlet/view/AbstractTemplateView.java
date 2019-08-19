package org.springframework.web.servlet.view;

import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.support.RequestContext;

/**
 * 适用于基于模板的视图技术(如Velocity和FreeMarker)的适配器基类, 能够在其模型中使用请求和会话属性,
 * 以及为Spring的Velocity/FreeMarker宏库公开辅助对象的选项.
 *
 * <p>JSP/JSTL和其他视图技术自动访问HttpServletRequest对象, 从而可以访问当前用户的请求/会话属性.
 * 此外, 他们能够创建和缓存辅助对象作为请求属性本身.
 */
public abstract class AbstractTemplateView extends AbstractUrlBasedView {

	/**
	 * 模板模型中RequestContext实例的变量名, 可用于Spring的宏: e.g. 用于创建BindStatus对象.
	 */
	public static final String SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE = "springMacroRequestContext";


	private boolean exposeRequestAttributes = false;

	private boolean allowRequestOverride = false;

	private boolean exposeSessionAttributes = false;

	private boolean allowSessionOverride = false;

	private boolean exposeSpringMacroHelpers = true;


	/**
	 * 设置是否应在与模板合并之前将所有请求属性添加到模型中.
	 * 默认为"false".
	 */
	public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
		this.exposeRequestAttributes = exposeRequestAttributes;
	}

	/**
	 * 设置是否允许HttpServletRequest属性覆盖 (隐藏) 控制器生成的同名模型属性.
	 * 默认为"false", 如果找到与模型属性同名的请求属性, 则会引发异常.
	 */
	public void setAllowRequestOverride(boolean allowRequestOverride) {
		this.allowRequestOverride = allowRequestOverride;
	}

	/**
	 * 设置是否应在与模板合并之前将所有HttpSession属性添加到模型中.
	 * 默认为"false".
	 */
	public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
		this.exposeSessionAttributes = exposeSessionAttributes;
	}

	/**
	 * 设置是否允许HttpSession属性覆盖(隐藏)控制器生成的同名模型属性.
	 * 默认为"false", 如果找到与模型属性同名的会话属性, 则会引发异常.
	 */
	public void setAllowSessionOverride(boolean allowSessionOverride) {
		this.allowSessionOverride = allowSessionOverride;
	}

	/**
	 * 设置是否公开一个RequestContext供Spring的宏库使用, 名称为"springMacroRequestContext".
	 * 默认为"true".
	 * <p>目前需要Spring的Velocity和FreeMarker默认宏.
	 * 请注意, 对于使用HTML表单的模板, 这是不需要的, 除非希望利用Spring助手宏.
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		if (this.exposeRequestAttributes) {
			for (Enumeration<String> en = request.getAttributeNames(); en.hasMoreElements();) {
				String attribute = en.nextElement();
				if (model.containsKey(attribute) && !this.allowRequestOverride) {
					throw new ServletException("Cannot expose request attribute '" + attribute +
						"' because of an existing model object of the same name");
				}
				Object attributeValue = request.getAttribute(attribute);
				if (logger.isDebugEnabled()) {
					logger.debug("Exposing request attribute '" + attribute +
							"' with value [" + attributeValue + "] to model");
				}
				model.put(attribute, attributeValue);
			}
		}

		if (this.exposeSessionAttributes) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				for (Enumeration<String> en = session.getAttributeNames(); en.hasMoreElements();) {
					String attribute = en.nextElement();
					if (model.containsKey(attribute) && !this.allowSessionOverride) {
						throw new ServletException("Cannot expose session attribute '" + attribute +
							"' because of an existing model object of the same name");
					}
					Object attributeValue = session.getAttribute(attribute);
					if (logger.isDebugEnabled()) {
						logger.debug("Exposing session attribute '" + attribute +
								"' with value [" + attributeValue + "] to model");
					}
					model.put(attribute, attributeValue);
				}
			}
		}

		if (this.exposeSpringMacroHelpers) {
			if (model.containsKey(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE)) {
				throw new ServletException(
						"Cannot expose bind macro helper '" + SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE +
						"' because of an existing model object of the same name");
			}
			// 公开Spring宏的RequestContext实例.
			model.put(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE,
					new RequestContext(request, response, getServletContext(), model));
		}

		applyContentType(response);

		renderMergedTemplateModel(model, request, response);
	}

	/**
	 * 将此视图的"contentType" bean属性中指定的内容类型应用于给定的响应.
	 * <p>如果之前没有在响应上设置内容类型, 则仅应用视图的contentType.
	 * 这允许处理器预先覆盖默认内容类型.
	 * 
	 * @param response 当前的HTTP响应
	 */
	protected void applyContentType(HttpServletResponse response)	{
		if (response.getContentType() == null) {
			response.setContentType(getContentType());
		}
	}

	/**
	 * 子类必须实现此方法才能实际呈现视图.
	 * 
	 * @param model 组合输出Map, 如果需要, 将请求属性和会话属性合并到其中
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @throws Exception 如果渲染失败
	 */
	protected abstract void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
