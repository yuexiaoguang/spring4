package org.springframework.web.servlet.view;

/**
 * 模板视图解析器的抽象基类, 特别是Velocity和FreeMarker视图.
 *
 * <p>提供一种方便的方法来为请求属性, 会话属性和Spring的宏助手指定{@link AbstractTemplateView}的公开标志.
 */
public class AbstractTemplateViewResolver extends UrlBasedViewResolver {

	private boolean exposeRequestAttributes = false;

	private boolean allowRequestOverride = false;

	private boolean exposeSessionAttributes = false;

	private boolean allowSessionOverride = false;

	private boolean exposeSpringMacroHelpers = true;


	@Override
	protected Class<?> requiredViewClass() {
		return AbstractTemplateView.class;
	}

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
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		AbstractTemplateView view = (AbstractTemplateView) super.buildView(viewName);
		view.setExposeRequestAttributes(this.exposeRequestAttributes);
		view.setAllowRequestOverride(this.allowRequestOverride);
		view.setExposeSessionAttributes(this.exposeSessionAttributes);
		view.setAllowSessionOverride(this.allowSessionOverride);
		view.setExposeSpringMacroHelpers(this.exposeSpringMacroHelpers);
		return view;
	}

}
