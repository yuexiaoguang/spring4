package org.springframework.web.method.support;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.SimpleSessionStatus;

/**
 * 在调用控制器方法的过程中, 记录{@link HandlerMethodArgumentResolver}
 * 和{@link HandlerMethodReturnValueHandler}做出的模型和视图相关决策.
 *
 * <p>{@link #setRequestHandled}标志可用于指示请求已直接处理且不需要查看解析.
 *
 * <p>在实例化时自动创建默认的{@link Model}.
 * 可以通过{@link #setRedirectModel}提供备用模型实例, 以用于重定向场景.
 * 当{@link #setRedirectModelScenario}设置为{@code true}表示重定向场景时,
 * {@link #getModel()}将返回重定向模型而不是默认模型.
 */
public class ModelAndViewContainer {

	private boolean ignoreDefaultModelOnRedirect = false;

	private Object view;

	private final ModelMap defaultModel = new BindingAwareModelMap();

	private ModelMap redirectModel;

	private boolean redirectModelScenario = false;

	private HttpStatus status;

	private final Set<String> noBinding = new HashSet<String>(4);

	private final Set<String> bindingDisabled = new HashSet<String>(4);

	private final SessionStatus sessionStatus = new SimpleSessionStatus();

	private boolean requestHandled = false;


	/**
	 * 默认情况下, 在呈现和重定向场景期间使用"default"模型的内容.
	 * 或者, 控制器方法可以声明{@code RedirectAttributes}类型的参数, 并使用它来提供属性以准备重定向URL.
	 * <p>将此标志设置为{@code true}可确保即使未声明RedirectAttributes参数, 也不会在重定向场景中使用"default"模型.
	 * 将其设置为{@code false}意味着如果控制器方法未声明RedirectAttributes参数, 则可以在重定向中使用"default"模型.
	 * <p>默认{@code false}.
	 */
	public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
		this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
	}

	/**
	 * 设置要由DispatcherServlet通过ViewResolver解析的视图名称.
	 * 将覆盖任何预先存在的视图名称或视图.
	 */
	public void setViewName(String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回由DispatcherServlet通过ViewResolver解析的视图名称, 或{@code null}, 如果设置了View对象.
	 */
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 设置DispatcherServlet使用的View对象.
	 * 将覆盖任何预先存在的视图名称或视图.
	 */
	public void setView(Object view) {
		this.view = view;
	}

	/**
	 * 返回View对象, 或{@code null}, 如果使用视图名称由DispatcherServlet通过ViewResolver解析.
	 */
	public Object getView() {
		return this.view;
	}

	/**
	 * 视图是否是通过名称指定的视图引用, 以便DispatcherServlet通过ViewResolver解析.
	 */
	public boolean isViewReference() {
		return (this.view instanceof String);
	}

	/**
	 * 返回要使用的模型 -- "default"或"redirect"模型.
	 * 如果{@code redirectModelScenario=false}或没有重定向模型 (i.e. RedirectAttributes未声明为方法参数),
	 * 并且{@code ignoreDefaultModelOnRedirect=false}, 则使用默认模型.
	 */
	public ModelMap getModel() {
		if (useDefaultModel()) {
			return this.defaultModel;
		}
		else {
			if (this.redirectModel == null) {
				this.redirectModel = new ModelMap();
			}
			return this.redirectModel;
		}
	}

	/**
	 * 使用默认模型还是重定向模型.
	 */
	private boolean useDefaultModel() {
		return (!this.redirectModelScenario || (this.redirectModel == null && !this.ignoreDefaultModelOnRedirect));
	}

	/**
	 * 返回在实例化时创建的"default"模型.
	 * <p>一般情况下, 建议使用{@link #getModel()}来返回"default"模型 (模板呈现)或"redirect"模型 (重定向URL准备).
	 * 对于需要访问"default"模型的高级情况, 可能需要使用此方法, e.g. 保存通过{@code @SessionAttributes}指定的模型属性.
	 * 
	 * @return 默认模型 (never {@code null})
	 */
	public ModelMap getDefaultModel() {
		return this.defaultModel;
	}

	/**
	 * 提供单独的模型实例以在重定向场景中使用.
	 * 但是, 除非{@link #setRedirectModelScenario(boolean)}设置为{@code true}以表示重定向场景, 否则不会使用提供的附加模型.
	 */
	public void setRedirectModel(ModelMap redirectModel) {
		this.redirectModel = redirectModel;
	}

	/**
	 * 控制器是否已返回重定向指令, e.g. "redirect:"前缀视图名称, RedirectView实例等.
	 */
	public void setRedirectModelScenario(boolean redirectModelScenario) {
		this.redirectModelScenario = redirectModelScenario;
	}

	/**
	 * 提供将用于视图呈现的{@code ModelAndView}传递的HTTP状态.
	 */
	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	/**
	 * 返回配置的HTTP状态.
	 */
	public HttpStatus getStatus() {
		return this.status;
	}

	/**
	 * 以编程方式注册不应数据绑定的属性, 甚至不是后续的{@code @ModelAttribute}声明.
	 * 
	 * @param attributeName 属性名称
	 */
	public void setBindingDisabled(String attributeName) {
		this.bindingDisabled.add(attributeName);
	}

	/**
	 * 是否对给定的模型属性禁用绑定.
	 */
	public boolean isBindingDisabled(String name) {
		return (this.bindingDisabled.contains(name) || this.noBinding.contains(name));
	}

	/**
	 * 注册是否应对相应的模型属性进行数据绑定, 对应于{@code @ModelAttribute(binding=true/false)}声明.
	 * <p>Note: 虽然{@link #isBindingDisabled}会考虑此标志, 但硬{@link #setBindingDisabled}声明将始终覆盖它.
	 * 
	 * @param attributeName 属性名称
	 */
	public void setBinding(String attributeName, boolean enabled) {
		if (!enabled) {
			this.noBinding.add(attributeName);
		}
		else {
			this.noBinding.remove(attributeName);
		}
	}

	/**
	 * 返回要使用的{@link SessionStatus}实例, 该实例可用于表示会话处理已完成.
	 */
	public SessionStatus getSessionStatus() {
		return this.sessionStatus;
	}

	/**
	 * 请求是否已在处理器中完全处理, e.g. {@code @ResponseBody}方法, 因此不需要视图解析.
	 * 当控制器方法声明{@code ServletResponse}或{@code OutputStream}类型的参数时, 也可以设置此标志.
	 * <p>默认{@code false}.
	 */
	public void setRequestHandled(boolean requestHandled) {
		this.requestHandled = requestHandled;
	}

	/**
	 * 请求是否已在处理器中完全处理.
	 */
	public boolean isRequestHandled() {
		return this.requestHandled;
	}

	/**
	 * 将提供的属性添加到底层模型.
	 * {@code getModel().addAttribute(String, Object)}的快捷方式.
	 */
	public ModelAndViewContainer addAttribute(String name, Object value) {
		getModel().addAttribute(name, value);
		return this;
	}

	/**
	 * 将提供的属性添加到底层模型.
	 * {@code getModel().addAttribute(Object)}的快捷方式.
	 */
	public ModelAndViewContainer addAttribute(Object value) {
		getModel().addAttribute(value);
		return this;
	}

	/**
	 * 将所有属性复制到底层模型.
	 * {@code getModel().addAllAttributes(Map)}的快捷方式.
	 */
	public ModelAndViewContainer addAllAttributes(Map<String, ?> attributes) {
		getModel().addAllAttributes(attributes);
		return this;
	}

	/**
	 * 复制提供的{@code Map}中的属性, 使用相同名称的现有对象优先 (i.e. 不被替换).
	 * {@code getModel().mergeAttributes(Map<String, ?>)}的快捷方式.
	 */
	public ModelAndViewContainer mergeAttributes(Map<String, ?> attributes) {
		getModel().mergeAttributes(attributes);
		return this;
	}

	/**
	 * 从模型中删除给定的属性.
	 */
	public ModelAndViewContainer removeAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				getModel().remove(key);
			}
		}
		return this;
	}

	/**
	 * 底层模型是否包含给定的属性名称.
	 * {@code getModel().containsAttribute(String)}的快捷方式.
	 */
	public boolean containsAttribute(String name) {
		return getModel().containsAttribute(name);
	}


	/**
	 * 返回诊断信息.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ModelAndViewContainer: ");
		if (!isRequestHandled()) {
			if (isViewReference()) {
				sb.append("reference to view with name '").append(this.view).append("'");
			}
			else {
				sb.append("View is [").append(this.view).append(']');
			}
			if (useDefaultModel()) {
				sb.append("; default model ");
			}
			else {
				sb.append("; redirect model ");
			}
			sb.append(getModel());
		}
		else {
			sb.append("Request handled directly");
		}
		return sb.toString();
	}

}
