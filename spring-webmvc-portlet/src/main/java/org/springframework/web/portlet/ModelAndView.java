package org.springframework.web.portlet;

import java.util.Map;

import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;

/**
 * Web MVC框架中的Model和View的保存器.
 * 请注意, 这些完全不同.
 * 这个类只是为了使控制器能够在单个返回值中返回模型和视图.
 *
 * <p>表示由处理器返回的模型和视图, 由DispatcherPortlet解析.
 * 视图可以采用String视图名称的形式, 需要由ViewResolver对象解析; 或者, 可以直接指定视图对象.
 * 该模型是一个Map, 允许使用名称作为键的多个对象.
 */
public class ModelAndView {

	/** View 实例或视图名称String */
	private Object view;

	/** Model Map */
	private ModelMap model;

	/**
	 * 通过调用{@link #clear()}指示是否已清除此实例.
	 */
	private boolean cleared = false;


	/**
	 * bean样式用法的默认构造函数: 填充bean属性, 而不是传入构造函数参数.
	 */
	public ModelAndView() {
	}

	/**
	 * 没有要公开的模型数据时方便的构造函数.
	 * 也可以与{@code addObject}一起使用.
	 * 
	 * @param viewName 要呈现的View的名称, 由DispatcherPortlet的ViewResolver解析
	 */
	public ModelAndView(String viewName) {
		this.view = viewName;
	}

	/**
	 * 没有要公开的模型数据时方便的构造函数.
	 * 也可以与{@code addObject}一起使用.
	 * 
	 * @param view 要呈现的View的名称 (通常是一个Servlet MVC View对象)
	 */
	public ModelAndView(Object view) {
		this.view = view;
	}

	/**
	 * 给定视图名称和模型, 创建一个新的ModelAndView.
	 * 
	 * @param viewName 要呈现的View的名称, 由DispatcherPortlet的ViewResolver解析
	 * @param model 模型名称 (String)到模型对象 (Object)的映射.
	 * 模型条目可能不是{@code null}, 但如果没有模型数据, 模型Map可能是{@code null}.
	 */
	public ModelAndView(String viewName, Map<String, ?> model) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * @param view 要呈现的View的名称 (通常是一个Servlet MVC View对象)
	 * @param model 模型名称 (String)到模型对象 (Object)的映射.
	 * 模型条目可能不是{@code null}, 但如果没有模型数据, 模型Map可能是{@code null}.
	 */
	public ModelAndView(Object view, Map<String, ?> model) {
		this.view = view;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * @param viewName 要呈现的View的名称, 由DispatcherPortlet的ViewResolver解析
	 * @param modelName 模型中单个条目的名称
	 * @param modelObject 单个模型对象
	 */
	public ModelAndView(String viewName, String modelName, Object modelObject) {
		this.view = viewName;
		addObject(modelName, modelObject);
	}

	/**
	 * @param view 要呈现的View的名称 (通常是一个Servlet MVC View对象)
	 * @param modelName 模型中单个条目的名称
	 * @param modelObject 单个模型对象
	 */
	public ModelAndView(Object view, String modelName, Object modelObject) {
		this.view = view;
		addObject(modelName, modelObject);
	}


	/**
	 * 设置此ModelAndView的视图名称, 由DispatcherPortlet通过ViewResolver解析.
	 * 将覆盖任何预先存在的视图名称或视图.
	 */
	public void setViewName(String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回要由DispatcherPortlet通过ViewResolver解析的视图名称, 如果使用视图对象, 则返回{@code null}.
	 */
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 设置此ModelAndView的View对象. 将覆盖任何预先存在的视图名称或视图.
	 * <p>给定的View对象通常是Servlet MVC View对象.
	 * 但是, 它仍然作为Object类型, 以避免Portlet ModelAndView类中的Servlet API依赖性.
	 */
	public void setView(Object view) {
		this.view = view;
	}

	/**
	 * 返回View对象, 如果使用视图名称由DispatcherPortlet通过ViewResolver解析, 则返回{@code null}.
	 */
	public Object getView() {
		return (!(this.view instanceof String) ? this.view : null);
	}

	/**
	 * 指示此{@code ModelAndView}是否具有视图, 视图名称或直接视图实例.
	 */
	public boolean hasView() {
		return (this.view != null);
	}

	/**
	 * 返回是否使用视图引用, i.e. {@code true} 如果已通过名称指定视图, 由DispatcherPortlet通过ViewResolver解析.
	 */
	public boolean isReference() {
		return (this.view instanceof String);
	}

	/**
	 * 返回模型 Map. 可以返回{@code null}.
	 * 由DispatcherPortlet调用以评估模型.
	 */
	protected Map<String, Object> getModelInternal() {
		return this.model;
	}

	/**
	 * 返回底层{@code ModelMap}实例 (never {@code null}).
	 */
	public ModelMap getModelMap() {
		if (this.model == null) {
			this.model = new ModelMap();
		}
		return this.model;
	}

	/**
	 * 返回模型Map. Never returns {@code null}.
	 * 由应用程序代码调用以修改模型.
	 */
	public Map<String, Object> getModel() {
		return getModelMap();
	}


	/**
	 * 向模型添加属性.
	 * 
	 * @param attributeName 要添加到模型的对象的名称
	 * @param attributeValue 要添加到模型的对象 (never {@code null})
	 */
	public ModelAndView addObject(String attributeName, Object attributeValue) {
		getModelMap().addAttribute(attributeName, attributeValue);
		return this;
	}

	/**
	 * 使用参数名称生成向模型添加属性.
	 * 
	 * @param attributeValue 要添加到模型的对象 (never {@code null})
	 */
	public ModelAndView addObject(Object attributeValue) {
		getModelMap().addAttribute(attributeValue);
		return this;
	}

	/**
	 * 将提供的Map中包含的所有属性添加到模型中.
	 * 
	 * @param modelMap a Map of attributeName -> attributeValue pairs
	 */
	public ModelAndView addAllObjects(Map<String, ?> modelMap) {
		getModelMap().addAllAttributes(modelMap);
		return this;
	}


	/**
	 * 清除此ModelAndView对象的状态.
	 * 之后该对象将为空.
	 * <p>可用于在HandlerInterceptor的{@code postHandleRender}方法中禁止呈现给定的ModelAndView对象.
	 */
	public void clear() {
		this.view = null;
		this.model = null;
		this.cleared = true;
	}

	/**
	 * 返回此ModelAndView对象是否为空, i.e. 它是否不包含任何视图且不包含模型.
	 */
	public boolean isEmpty() {
		return (this.view == null && CollectionUtils.isEmpty(this.model));
	}

	/**
	 * 返回调用{@link #clear}时此ModelAndView对象是否为空, i.e. 它是否不包含任何视图且不包含模型.
	 * 如果在调用{@link #clear}<strong>之后</strong>, 将任何其他状态添加到实例, 则返回{@code false}.
	 */
	public boolean wasCleared() {
		return (this.cleared && isEmpty());
	}


	/**
	 * 返回有关此模型和视图的诊断信息.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("ModelAndView: ");
		if (isReference()) {
			result.append("reference to view with name '").append(this.view).append("'");
		}
		else {
			result.append("materialized View is [").append(this.view).append(']');
		}
		result.append("; model is ").append(this.model);
		return result.toString();
	}

}
