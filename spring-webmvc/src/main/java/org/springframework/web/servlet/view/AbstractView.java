package org.springframework.web.servlet.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.ContextExposingHttpServletRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;

/**
 * {@link org.springframework.web.servlet.View}实现的抽象基类.
 * 子类应该是JavaBeans, 以便于配置为Spring管理的bean实例.
 *
 * <p>通过各种方式指定静态属性, 为视图提供静态属性支持.
 * 静态属性将与每个渲染操作的给定动态属性 (控制器返回的模型)合并.
 *
 * <p>扩展{@link WebApplicationObjectSupport}, 这对某些视图很有帮助. 子类只需要实现实际渲染.
 */
public abstract class AbstractView extends WebApplicationObjectSupport implements View, BeanNameAware {

	/** 默认内容类型. 作为bean属性可覆盖. */
	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=ISO-8859-1";

	/** 临时输出字节数组的初始大小 */
	private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 4096;


	private String contentType = DEFAULT_CONTENT_TYPE;

	private String requestContextAttribute;

	private final Map<String, Object> staticAttributes = new LinkedHashMap<String, Object>();

	private boolean exposePathVariables = true;

	private boolean exposeContextBeansAsAttributes = false;

	private Set<String> exposedContextBeanNames;

	private String beanName;


	/**
	 * 设置此视图的内容类型.
	 * 默认为"text/html;charset=ISO-8859-1".
	 * <p>如果假定视图本身设置内容类型, 则子类可以忽略, e.g. 在JSP的情况下.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回此视图的内容类型.
	 */
	@Override
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置此视图的RequestContext属性的名称.
	 * 默认无.
	 */
	public void setRequestContextAttribute(String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回此视图的RequestContext属性的名称.
	 */
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 将静态属性设置为CSV字符串.
	 * 格式为: attname0={value1},attname1={value1}
	 * <p>"Static" 属性是在View实例配置中指定的固定属性.
	 * "Dynamic" 属性是作为模型的一部分传入的值.
	 */
	public void setAttributesCSV(String propString) throws IllegalArgumentException {
		if (propString != null) {
			StringTokenizer st = new StringTokenizer(propString, ",");
			while (st.hasMoreTokens()) {
				String tok = st.nextToken();
				int eqIdx = tok.indexOf('=');
				if (eqIdx == -1) {
					throw new IllegalArgumentException("Expected = in attributes CSV string '" + propString + "'");
				}
				if (eqIdx >= tok.length() - 2) {
					throw new IllegalArgumentException(
							"At least 2 characters ([]) required in attributes CSV string '" + propString + "'");
				}
				String name = tok.substring(0, eqIdx);
				String value = tok.substring(eqIdx + 1);

				// 删除值的第一个和最后一个字符: { 和 }
				value = value.substring(1);
				value = value.substring(0, value.length() - 1);

				addStaticAttribute(name, value);
			}
		}
	}

	/**
	 * 从{@code java.util.Properties}对象设置此视图的静态属性.
	 * <p>"Static" 属性是在View实例配置中指定的固定属性.
	 * "Dynamic" 属性是作为模型的一部分传入的值.
	 * <p>这是设置静态属性最方便的方法.
	 * 请注意, 如果模型中包含具有相同名称的值, 则静态属性可以被动态属性覆盖.
	 * <p>可以使用String "value" (通过PropertiesEditor解析) 或XML bean定义中的"props"元素填充.
	 */
	public void setAttributes(Properties attributes) {
		CollectionUtils.mergePropertiesIntoMap(attributes, this.staticAttributes);
	}

	/**
	 * 从Map设置此视图的静态属性.
	 * 这允许设置任何类型的属性值, 例如bean引用.
	 * <p>"Static" 属性是在View实例配置中指定的固定属性.
	 * "Dynamic" 属性是作为模型的一部分传入的值.
	 * <p>可以在XML bean定义中使用"map" 或 "props"元素填充.
	 * 
	 * @param attributes 使用名称将字符串映射为键, 将属性对象映射为值
	 */
	public void setAttributesMap(Map<String, ?> attributes) {
		if (attributes != null) {
			for (Map.Entry<String, ?> entry : attributes.entrySet()) {
				addStaticAttribute(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * 允许Map访问此视图的静态属性, 并可选择添加或覆盖特定条目.
	 * <p>用于直接指定条目, 例如通过"attributesMap[myKey]".
	 * 这对于在子视图定义中添加或覆盖条目特别有用.
	 */
	public Map<String, Object> getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * 将静态数据添加到此视图, 在每个视图中公开.
	 * <p>"Static" 属性是在View实例配置中指定的固定属性.
	 * "Dynamic" 属性是作为模型的一部分传入的值.
	 * <p>必须在调用{@code render}之前调用.
	 * 
	 * @param name 要公开的属性的名称
	 * @param value 要公开的属性值
	 */
	public void addStaticAttribute(String name, Object value) {
		this.staticAttributes.put(name, value);
	}

	/**
	 * 返回此视图的静态属性. 方便测试.
	 * <p>返回一个不可修改的Map, 因为它不是用于操作Map而是用于检查内容.
	 * 
	 * @return 此视图中的静态属性
	 */
	public Map<String, Object> getStaticAttributes() {
		return Collections.unmodifiableMap(this.staticAttributes);
	}

	/**
	 * 指定是否将路径变量添加到模型.
	 * <p>路径变量通常通过{@code @PathVariable}注解绑定到URI模板变量.
	 * 它们实际上是URI模板变量, 应用了类型转换来派生类型化的Object值.
	 * 在构建到相同URL和其他URL的链接的视图中经常需要这样的值.
	 * <p>添加到模型的路径变量会覆盖静态属性 (see {@link #setAttributes(Properties)}), 但不会覆盖模型中已存在的属性.
	 * <p>默认为{@code true}. 具体视图类型可以覆盖它.
	 * 
	 * @param exposePathVariables {@code true} 公开路径变量, 否则{@code false}
	 */
	public void setExposePathVariables(boolean exposePathVariables) {
		this.exposePathVariables = exposePathVariables;
	}

	/**
	 * 返回是否将路径变量添加到模型中.
	 */
	public boolean isExposePathVariables() {
		return this.exposePathVariables;
	}

	/**
	 * 设置是否可以将应用程序上下文中的所有Spring bean作为请求属性访问, 一旦访问属性, 通过延迟检查.
	 * <p>这将使所有这些bean在JSP 2.0页面中的普通 {@code ${...}}表达式以及JSTL的{@code c:out}值表达式中可访问.
	 * <p>默认为"false". 切换此标志以透明地公开请求属性命名空间中的所有Spring bean.
	 * <p><b>NOTE:</b> 上下文bean将覆盖已手动添加的任何同名的自定义请求或会话属性.
	 * 但是, 同名的模型属性 (显式地暴露给此视图) 将始终覆盖上下文bean.
	 */
	public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
		this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
	}

	/**
	 * 指定上下文中应该公开的bean的名称.
	 * 如果非null, 则只有指定的bean才有资格作为属性公开.
	 * <p>如果在应用程序上下文中公开所有Spring bean,
	 * 打开{@link #setExposeContextBeansAsAttributes "exposeContextBeansAsAttributes"}标志,
	 * 但不要列出此属性的特定bean名称.
	 */
	public void setExposedContextBeanNames(String... exposedContextBeanNames) {
		this.exposedContextBeanNames = new HashSet<String>(Arrays.asList(exposedContextBeanNames));
	}

	/**
	 * 设置视图的名称. 有助于追溯.
	 * <p>框架代码在构造视图时必须调用它.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 返回视图的名称. 如果视图配置正确, 则永远不应该是{@code null}.
	 */
	public String getBeanName() {
		return this.beanName;
	}


	/**
	 * 如果需要, 准备指定模型的视图, 将其与静态属性和RequestContext属性合并.
	 * 委托给renderMergedOutputModel进行实际渲染.
	 */
	@Override
	public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view with name '" + this.beanName + "' with model " + model +
				" and static attributes " + this.staticAttributes);
		}

		Map<String, Object> mergedModel = createMergedOutputModel(model, request, response);
		prepareResponse(request, response);
		renderMergedOutputModel(mergedModel, getRequestToExpose(request), response);
	}

	/**
	 * 创建包含动态值和静态属性的组合输出Map (never {@code null}).
	 * 动态值优先于静态属性.
	 */
	protected Map<String, Object> createMergedOutputModel(Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) {

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (this.exposePathVariables ?
				(Map<String, Object>) request.getAttribute(View.PATH_VARIABLES) : null);

		// 合并静态和动态模型属性.
		int size = this.staticAttributes.size();
		size += (model != null ? model.size() : 0);
		size += (pathVars != null ? pathVars.size() : 0);

		Map<String, Object> mergedModel = new LinkedHashMap<String, Object>(size);
		mergedModel.putAll(this.staticAttributes);
		if (pathVars != null) {
			mergedModel.putAll(pathVars);
		}
		if (model != null) {
			mergedModel.putAll(model);
		}

		// Expose RequestContext?
		if (this.requestContextAttribute != null) {
			mergedModel.put(this.requestContextAttribute, createRequestContext(request, response, mergedModel));
		}

		return mergedModel;
	}

	/**
	 * 创建一个RequestContext以在指定的属性名称下公开.
	 * <p>默认实现为给定的请求和模型创建标准的RequestContext实例.
	 * 可以在子类中重写以自定义实例.
	 * 
	 * @param request 当前的HTTP请求
	 * @param model 合并的输出Map (never {@code null}), 动态值优先于静态属性
	 * 
	 * @return RequestContext实例
	 */
	protected RequestContext createRequestContext(
			HttpServletRequest request, HttpServletResponse response, Map<String, Object> model) {

		return new RequestContext(request, response, getServletContext(), model);
	}

	/**
	 * 准备给定的渲染响应.
	 * <p>默认实现在通过HTTPS发送下载内容时IE bug的变通方法.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 */
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		if (generatesDownloadContent()) {
			response.setHeader("Pragma", "private");
			response.setHeader("Cache-Control", "private, must-revalidate");
		}
	}

	/**
	 * 返回此视图是否生成下载内容 (通常是PDF或Excel文件等二进制内容).
	 * <p>默认实现返回{@code false}.
	 * 如果子类知道他们正在生成需要在客户端进行临时缓存的下载内容, 通常通过响应OutputStream, 则鼓励子类返回{@code true}.
	 */
	protected boolean generatesDownloadContent() {
		return false;
	}

	/**
	 * 获取请求句柄以向{@link #renderMergedOutputModel}公开, i.e. 向视图公开.
	 * <p>默认实现包装原始请求, 用于将Spring bean公开为请求属性.
	 * 
	 * @param originalRequest 引擎提供的原始servlet请求
	 * 
	 * @return 包装的请求, 或原始请求
	 */
	protected HttpServletRequest getRequestToExpose(HttpServletRequest originalRequest) {
		if (this.exposeContextBeansAsAttributes || this.exposedContextBeanNames != null) {
			return new ContextExposingHttpServletRequest(
					originalRequest, getWebApplicationContext(), this.exposedContextBeanNames);
		}
		return originalRequest;
	}

	/**
	 * 子类必须实现此方法才能实际呈现视图.
	 * <p>第一步是准备请求: 在JSP情况下, 这意味着将模型对象设置为请求属性.
	 * 第二步是视图的实际呈现, 例如通过RequestDispatcher包含JSP.
	 * 
	 * @param model 组合的输出 Map (never {@code null}), 动态值优先于静态属性
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @throws Exception 如果渲染失败
	 */
	protected abstract void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;


	/**
	 * 将给定Map中的模型对象公开为请求属性.
	 * 名称将从模型Map中获取.
	 * 此方法适用于{@link javax.servlet.RequestDispatcher}可访问的所有资源.
	 * 
	 * @param model 要公开的模型对象的Map
	 * @param request 当前的HTTP请求
	 */
	protected void exposeModelAsRequestAttributes(Map<String, Object> model, HttpServletRequest request) throws Exception {
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			String modelName = entry.getKey();
			Object modelValue = entry.getValue();
			if (modelValue != null) {
				request.setAttribute(modelName, modelValue);
				if (logger.isDebugEnabled()) {
					logger.debug("Added model object '" + modelName + "' of type [" + modelValue.getClass().getName() +
							"] to request in view with name '" + getBeanName() + "'");
				}
			}
			else {
				request.removeAttribute(modelName);
				if (logger.isDebugEnabled()) {
					logger.debug("Removed model object '" + modelName +
							"' from request in view with name '" + getBeanName() + "'");
				}
			}
		}
	}

	/**
	 * 为此视图创建临时OutputStream.
	 * <p>这通常用作IE解决方法, 用于在将内容实际写入HTTP响应之前从临时流设置内容长度header.
	 */
	protected ByteArrayOutputStream createTemporaryOutputStream() {
		return new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
	}

	/**
	 * 将给定的临时OutputStream写入HTTP响应.
	 * 
	 * @param response 当前的HTTP响应
	 * @param baos 要写入的临时OutputStream
	 * 
	 * @throws IOException 如果写入/刷新失败
	 */
	protected void writeToResponse(HttpServletResponse response, ByteArrayOutputStream baos) throws IOException {
		// 写入内容类型和长度 (通过字节数组确定).
		response.setContentType(getContentType());
		response.setContentLength(baos.size());

		// 将字节数组刷新到servlet输出流.
		ServletOutputStream out = response.getOutputStream();
		baos.writeTo(out);
		out.flush();
	}

	/**
	 * 将响应的内容类型设置为配置的{@link #setContentType(String) 内容类型},
	 * 除非{@link View#SELECTED_CONTENT_TYPE}请求属性存在并设置为具体的媒体类型.
	 */
	protected void setResponseContentType(HttpServletRequest request, HttpServletResponse response) {
		MediaType mediaType = (MediaType) request.getAttribute(View.SELECTED_CONTENT_TYPE);
		if (mediaType != null && mediaType.isConcrete()) {
			response.setContentType(mediaType.toString());
		}
		else {
			response.setContentType(getContentType());
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		if (getBeanName() != null) {
			sb.append(": name '").append(getBeanName()).append("'");
		}
		else {
			sb.append(": unnamed");
		}
		return sb.toString();
	}

}
