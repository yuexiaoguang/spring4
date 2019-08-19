package org.springframework.web.servlet;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;

/**
 * Web MVC框架中的Model和View的保存器.
 * 请注意, 这些完全不同. 这个类只是为了使控制器能够在单个返回值中返回模型和视图.
 *
 * <p>表示由处理器返回的模型和视图, 由DispatcherServlet解析.
 * 视图可以采用String视图名称的形式, 需要由ViewResolver对象解析;
 * 或者, 可以直接指定View对象.
 * 该模型是一个Map, 允许一个名称对应多个对象.
 */
public class ModelAndView {

	/** View实例或视图名称String */
	private Object view;

	/** 模型Map */
	private ModelMap model;

	/** 响应的可选HTTP状态 */
	private HttpStatus status;

	/** 通过调用{@link #clear()}指示是否已清除此实例 */
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
	 * @param viewName 要呈现的视图的名称, 由DispatcherServlet的ViewResolver解析
	 */
	public ModelAndView(String viewName) {
		this.view = viewName;
	}

	/**
	 * 没有要公开的模型数据时方便的构造函数.
	 * 也可以与{@code addObject}一起使用.
	 * 
	 * @param view 要呈现的View对象
	 */
	public ModelAndView(View view) {
		this.view = view;
	}

	/**
	 * @param viewName 要呈现的视图的名称, 由DispatcherServlet的ViewResolver解析
	 * @param model 模型名称 (Strings)到模型对象 (Objects)的Map.
	 * 模型条目可能不是{@code null}, 但如果没有模型数据, 模型Map可能是 {@code null}.
	 */
	public ModelAndView(String viewName, Map<String, ?> model) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * <emphasis>Note: 提供的模型数据将复制到此类的内部存储中.
	 * 在将其提供给此类后, 不应该考虑修改提供的Map</emphasis>
	 * 
	 * @param view 要渲染的View对象
	 * @param model 模型名称 (Strings)到模型对象 (Objects)的Map.
	 * 模型条目可能不是{@code null}, 但如果没有模型数据, 模型Map可能是 {@code null}.
	 */
	public ModelAndView(View view, Map<String, ?> model) {
		this.view = view;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * @param viewName 要呈现的视图的名称, 由DispatcherServlet的ViewResolver解析
	 * @param status 用于响应的HTTP状态码 (在View渲染之前设置)
	 */
	public ModelAndView(String viewName, HttpStatus status) {
		this.view = viewName;
		this.status = status;
	}

	/**
	 * @param viewName 要呈现的视图的名称, 由DispatcherServlet的ViewResolver解析
	 * @param model 模型名称 (Strings)到模型对象 (Objects)的Map.
	 * 模型条目可能不是{@code null}, 但如果没有模型数据, 模型Map可能是 {@code null}.
	 * @param status 用于响应的HTTP状态码 (在View渲染之前设置)
	 */
	public ModelAndView(String viewName, Map<String, ?> model, HttpStatus status) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
		this.status = status;
	}

	/**
	 * @param viewName 要呈现的视图的名称, 由DispatcherServlet的ViewResolver解析
	 * @param modelName 模型中单个条目的名称
	 * @param modelObject 单个模型对象
	 */
	public ModelAndView(String viewName, String modelName, Object modelObject) {
		this.view = viewName;
		addObject(modelName, modelObject);
	}

	/**
	 * @param view 要渲染的View对象
	 * @param modelName 模型中单个条目的名称
	 * @param modelObject 单个模型对象
	 */
	public ModelAndView(View view, String modelName, Object modelObject) {
		this.view = view;
		addObject(modelName, modelObject);
	}


	/**
	 * 设置此ModelAndView的视图名称, 由DispatcherServlet通过ViewResolver解析.
	 * 将覆盖任何预先存在的视图名称或View.
	 */
	public void setViewName(String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回要由DispatcherServlet通过ViewResolver解析的视图名称, 如果使用View对象, 则返回{@code null}.
	 */
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 设置View对象. 将覆盖任何预先存在的视图名称或View.
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * 返回View对象, 如果使用视图名称由DispatcherServlet通过ViewResolver解析, 则为{@code null}.
	 */
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * 指示此{@code ModelAndView}是否具有视图, 可以是视图名称或直接{@link View}实例.
	 */
	public boolean hasView() {
		return (this.view != null);
	}

	/**
	 * 返回是否使用视图引用, i.e. {@code true}如果已通过名称指定视图, 由DispatcherServlet通过ViewResolver解析.
	 */
	public boolean isReference() {
		return (this.view instanceof String);
	}

	/**
	 * 返回模型Map. 可以返回{@code null}.
	 * 由DispatcherServlet调用以评估模型.
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
	 * 返回模型Map. 永远不会返回{@code null}.
	 * 由应用程序代码调用以修改模型.
	 */
	public Map<String, Object> getModel() {
		return getModelMap();
	}

	/**
	 * 设置用于响应的HTTP状态.
	 * <p>响应状态在View渲染之前设置.
	 */
	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	/**
	 * 返回响应的已配置HTTP状态.
	 */
	public HttpStatus getStatus() {
		return this.status;
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
	 * @param modelMap attributeName的Map -> attributeValue对
	 */
	public ModelAndView addAllObjects(Map<String, ?> modelMap) {
		getModelMap().addAllAttributes(modelMap);
		return this;
	}


	/**
	 * 清除此ModelAndView对象的状态.
	 * 之后该对象将为空.
	 * <p>可用于在HandlerInterceptor的{@code postHandle}方法中禁止呈现给定的ModelAndView对象.
	 */
	public void clear() {
		this.view = null;
		this.model = null;
		this.cleared = true;
	}

	/**
	 * 返回此ModelAndView对象是否为空, i.e. 它是否不包含任何视图和模型.
	 */
	public boolean isEmpty() {
		return (this.view == null && CollectionUtils.isEmpty(this.model));
	}

	/**
	 * 返回调用{@link #clear}时此ModelAndView对象是否为空, i.e. 它是否不包含任何视图和模型.
	 * <p>如果在调用{@link #clear}<strong>之后</strong>, 将任何其他状态添加到实例, 则返回{@code false}.
	 */
	public boolean wasCleared() {
		return (this.cleared && isEmpty());
	}


	/**
	 * 返回有关此模型和视图的诊断信息.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ModelAndView: ");
		if (isReference()) {
			sb.append("reference to view with name '").append(this.view).append("'");
		}
		else {
			sb.append("materialized View is [").append(this.view).append(']');
		}
		sb.append("; model is ").append(this.model);
		return sb.toString();
	}

}
