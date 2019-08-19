package org.springframework.web.servlet.view.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleTransformErrorListener;
import org.springframework.util.xml.TransformerUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.util.WebUtils;

/**
 * XSLT驱动的View, 允许响应上下文作为XSLT转换的结果呈现.
 *
 * <p>XSLT Source对象作为模型中的参数提供, 然后在响应呈现期间{@link #locateSource 检测}.
 * 用户可以通过{@link #setSourceKey sourceKey}属性在模型中指定特定条目, 或者让Spring找到Source对象.
 * 此类还提供对象到Source实现的基本转换.
 * See {@link #getSourceTypes() here} for more details.
 *
 * <p>所有模型参数都作为参数传递给XSLT Transformer.
 * 此外, 用户可以配置{@link #setOutputProperties 输出属性}以传递给Transformer.
 */
public class XsltView extends AbstractUrlBasedView {

	private Class<? extends TransformerFactory> transformerFactoryClass;

	private String sourceKey;

	private URIResolver uriResolver;

	private ErrorListener errorListener = new SimpleTransformErrorListener(logger);

	private boolean indent = true;

	private Properties outputProperties;

	private boolean cacheTemplates = true;

	private TransformerFactory transformerFactory;

	private Templates cachedTemplates;


	/**
	 * 指定要使用的XSLT TransformerFactory类.
	 * <p>将调用指定类的默认构造函数来为此视图构建TransformerFactory.
	 */
	public void setTransformerFactoryClass(Class<? extends TransformerFactory> transformerFactoryClass) {
		this.transformerFactoryClass = transformerFactoryClass;
	}

	/**
	 * 设置表示XSLT源的model属性的名称.
	 * 如果未指定, 将搜索模型Map以查找匹配的值类型.
	 * <p>开箱即用支持以下源类型:
	 * {@link Source}, {@link Document}, {@link Node}, {@link Reader}, {@link InputStream} 和 {@link Resource}.
	 */
	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	/**
	 * 设置转换中使用的URIResolver.
	 * <p>URIResolver处理对XSLT {@code document()}函数的调用.
	 */
	public void setUriResolver(URIResolver uriResolver) {
		this.uriResolver = uriResolver;
	}

	/**
	 * 设置{@link javax.xml.transform.ErrorListener}接口的实现, 以自定义转换错误和警告的处理.
	 * <p>如果未设置, 则使用默认的{@link org.springframework.util.xml.SimpleTransformErrorListener},
	 * 它只使用视图类的记录器实例记录警告, 并重新抛出错误以停止XML转换.
	 */
	public void setErrorListener(ErrorListener errorListener) {
		this.errorListener = (errorListener != null ? errorListener : new SimpleTransformErrorListener(logger));
	}

	/**
	 * 设置XSLT转换器在输出结果树时是否可以添加额外的空格.
	 * <p>默认值为{@code true} (on); 将此设置为{@code false} (off) 以不指定"indent"键, 将选择保存到样式表.
	 */
	public void setIndent(boolean indent) {
		this.indent = indent;
	}

	/**
	 * 设置要应用于样式表的任意变换器输出属性.
	 * <p>此处指定的任何值都将覆盖此视图以编程方式设置的默认值.
	 */
	public void setOutputProperties(Properties outputProperties) {
		this.outputProperties = outputProperties;
	}

	/**
	 * 打开/关闭XSLT {@link Templates}实例的缓存.
	 * <p>默认为"true". 仅在开发中将其设置为"false", 其中缓存不会严重影响性能.
	 */
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}


	/**
	 * 初始化这个XsltView的TransformerFactory.
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		this.transformerFactory = newTransformerFactory(this.transformerFactoryClass);
		this.transformerFactory.setErrorListener(this.errorListener);
		if (this.uriResolver != null) {
			this.transformerFactory.setURIResolver(this.uriResolver);
		}
		if (this.cacheTemplates) {
			this.cachedTemplates = loadTemplates();
		}
	}

	/**
	 * 为此视图实例化一个新的TransformerFactory.
	 * <p>默认实现调用
	 * {@link javax.xml.transform.TransformerFactory#newInstance()}.
	 * 如果显式指定了{@link #setTransformerFactoryClass "transformerFactoryClass"}, 则将调用指定类的默认构造函数.
	 * <p>可以在子类中重写.
	 * 
	 * @param transformerFactoryClass 指定的工厂类
	 * 
	 * @return 新的TransactionFactory实例
	 */
	protected TransformerFactory newTransformerFactory(Class<? extends TransformerFactory> transformerFactoryClass) {
		if (transformerFactoryClass != null) {
			try {
				return transformerFactoryClass.newInstance();
			}
			catch (Exception ex) {
				throw new TransformerFactoryConfigurationError(ex, "Could not instantiate TransformerFactory");
			}
		}
		else {
			return TransformerFactory.newInstance();
		}
	}

	/**
	 * 返回此XsltView使用的TransformerFactory.
	 * 
	 * @return TransformerFactory (never {@code null})
	 */
	protected final TransformerFactory getTransformerFactory() {
		return this.transformerFactory;
	}


	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Templates templates = this.cachedTemplates;
		if (templates == null) {
			templates = loadTemplates();
		}

		Transformer transformer = createTransformer(templates);
		configureTransformer(model, response, transformer);
		configureResponse(model, response, transformer);
		Source source = null;
		try {
			source = locateSource(model);
			if (source == null) {
				throw new IllegalArgumentException("Unable to locate Source object in model: " + model);
			}
			transformer.transform(source, createResult(response));
		}
		finally {
			closeSourceIfNecessary(source);
		}
	}

	/**
	 * 创建用于呈现转换结果的XSLT {@link Result}.
	 * <p>默认实现创建一个{@link StreamResult}包装提供的HttpServletResponse的
	 * {@link HttpServletResponse#getOutputStream() OutputStream}.
	 * 
	 * @param response 当前的HTTP响应
	 * 
	 * @return 要使用的XSLT结果
	 * @throws Exception 如果无法构建结果
	 */
	protected Result createResult(HttpServletResponse response) throws Exception {
		return new StreamResult(response.getOutputStream());
	}

	/**
	 * <p>在提供的模型中找到{@link Source}对象, 根据需要转换对象.
	 * 在尝试查找{@link #getSourceTypes() 支持的类型}的对象之前, 默认实现首先尝试在配置的{@link #setSourceKey 源键}下查看.
	 * 
	 * @param model 合并的模型Map
	 * 
	 * @return XSLT Source对象 (如果没有找到, 则为{@code null})
	 * @throws Exception 如果在查找源期间发生错误
	 */
	protected Source locateSource(Map<String, Object> model) throws Exception {
		if (this.sourceKey != null) {
			return convertSource(model.get(this.sourceKey));
		}
		Object source = CollectionUtils.findValueOfType(model.values(), getSourceTypes());
		return (source != null ? convertSource(source) : null);
	}

	/**
	 * 返回转换为XSLT {@link Source}时支持的{@link Class Classes}数组.
	 * <p>当前支持{@link Source}, {@link Document}, {@link Node}, {@link Reader}, {@link InputStream} 和 {@link Resource}.
	 * 
	 * @return 支持的源类型
	 */
	protected Class<?>[] getSourceTypes() {
		return new Class<?>[] {Source.class, Document.class, Node.class, Reader.class, InputStream.class, Resource.class};
	}

	/**
	 * 如果{@link #getSourceTypes() 支持}{@link Object}类型, 则将提供的{@link Object}转换为XSLT {@link Source}.
	 * 
	 * @param source 原始源对象
	 * 
	 * @return 适配的 XSLT Source
	 * @throws IllegalArgumentException 如果给定的Object不是受支持的类型
	 */
	protected Source convertSource(Object source) throws Exception {
		if (source instanceof Source) {
			return (Source) source;
		}
		else if (source instanceof Document) {
			return new DOMSource(((Document) source).getDocumentElement());
		}
		else if (source instanceof Node) {
			return new DOMSource((Node) source);
		}
		else if (source instanceof Reader) {
			return new StreamSource((Reader) source);
		}
		else if (source instanceof InputStream) {
			return new StreamSource((InputStream) source);
		}
		else if (source instanceof Resource) {
			Resource resource = (Resource) source;
			return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
		}
		else {
			throw new IllegalArgumentException("Value '" + source + "' cannot be converted to XSLT Source");
		}
	}

	/**
	 * 配置提供的{@link Transformer}实例.
	 * <p>默认实现将模型中的参数复制到Transformer的{@link Transformer#setParameter 参数集}.
	 * 此实现还将{@link #setOutputProperties 输出属性}复制到{@link Transformer} {@link Transformer#setOutputProperty 输出属性}.
	 * 还设置缩进属性.
	 * 
	 * @param model 合并的输出 Map (never {@code null})
	 * @param response 当前的HTTP响应
	 * @param transformer 目标转换器
	 */
	protected void configureTransformer(Map<String, Object> model, HttpServletResponse response, Transformer transformer) {
		copyModelParameters(model, transformer);
		copyOutputProperties(transformer);
		configureIndentation(transformer);
	}

	/**
	 * 配置提供的{@link Transformer}的缩进设置.
	 * 
	 * @param transformer 目标转换器
	 */
	protected final void configureIndentation(Transformer transformer) {
		if (this.indent) {
			TransformerUtils.enableIndenting(transformer);
		}
		else {
			TransformerUtils.disableIndenting(transformer);
		}
	}

	/**
	 * 将配置的输出{@link Properties}复制到提供的{@link Transformer}的{@link Transformer#setOutputProperty 输出属性集}中.
	 * 
	 * @param transformer 目标转换器
	 */
	protected final void copyOutputProperties(Transformer transformer) {
		if (this.outputProperties != null) {
			Enumeration<?> en = this.outputProperties.propertyNames();
			while (en.hasMoreElements()) {
				String name = (String) en.nextElement();
				transformer.setOutputProperty(name, this.outputProperties.getProperty(name));
			}
		}
	}

	/**
	 * 将提供的Map中的所有条目复制到所提供的{@link Transformer}的{@link Transformer#setParameter(String, Object) 参数集}中.
	 * 
	 * @param model 合并的输出 Map (never {@code null})
	 * @param transformer 目标转换器
	 */
	protected final void copyModelParameters(Map<String, Object> model, Transformer transformer) {
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			transformer.setParameter(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 配置提供的{@link HttpServletResponse}.
	 * <p>此方法的默认实现从{@link Transformer}中指定的"media-type" 和 "encoding" 输出属性
	 * 设置{@link HttpServletResponse#setContentType 内容类型}和{@link HttpServletResponse#setCharacterEncoding 编码}.
	 * 
	 * @param model 合并的输出Map (never {@code null})
	 * @param response 当前的HTTP响应
	 * @param transformer 目标转换器
	 */
	protected void configureResponse(Map<String, Object> model, HttpServletResponse response, Transformer transformer) {
		String contentType = getContentType();
		String mediaType = transformer.getOutputProperty(OutputKeys.MEDIA_TYPE);
		String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);
		if (StringUtils.hasText(mediaType)) {
			contentType = mediaType;
		}
		if (StringUtils.hasText(encoding)) {
			// 仅在指定了内容类型但不包含charset子句时才应用编码.
			if (contentType != null && !contentType.toLowerCase().contains(WebUtils.CONTENT_TYPE_CHARSET_PREFIX)) {
				contentType = contentType + WebUtils.CONTENT_TYPE_CHARSET_PREFIX + encoding;
			}
		}
		response.setContentType(contentType);
	}

	/**
	 * 在配置的位置加载样式表的{@link Templates}实例.
	 */
	private Templates loadTemplates() throws ApplicationContextException {
		Source stylesheetSource = getStylesheetSource();
		try {
			Templates templates = this.transformerFactory.newTemplates(stylesheetSource);
			if (logger.isDebugEnabled()) {
				logger.debug("Loading templates '" + templates + "'");
			}
			return templates;
		}
		catch (TransformerConfigurationException ex) {
			throw new ApplicationContextException("Can't load stylesheet from '" + getUrl() + "'", ex);
		}
		finally {
			closeSourceIfNecessary(stylesheetSource);
		}
	}

	/**
	 * 创建用于优先XSLT转换的{@link Transformer}实例.
	 * <p>默认实现只调用{@link Templates#newTransformer()}, 并使用自定义{@link URIResolver}配置{@link Transformer}.
	 * 
	 * @param templates 用于创建Transformer的XSLT Templates实例
	 * 
	 * @return Transformer对象
	 * @throws TransformerConfigurationException 创建失败
	 */
	protected Transformer createTransformer(Templates templates) throws TransformerConfigurationException {
		Transformer transformer = templates.newTransformer();
		if (this.uriResolver != null) {
			transformer.setURIResolver(this.uriResolver);
		}
		return transformer;
	}

	/**
	 * 在{@link #setUrl 配置的URL}下获取XSLT模板的XSLT {@link Source}.
	 * 
	 * @return Source对象
	 */
	protected Source getStylesheetSource() {
		String url = getUrl();
		if (logger.isDebugEnabled()) {
			logger.debug("Loading XSLT stylesheet from '" + url + "'");
		}
		try {
			Resource resource = getApplicationContext().getResource(url);
			return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
		}
		catch (IOException ex) {
			throw new ApplicationContextException("Can't load XSLT stylesheet from '" + url + "'", ex);
		}
	}

	/**
	 * 关闭由提供的{@link Source}管理的底层资源.
	 * <p>仅适用于{@link StreamSource StreamSources}.
	 * 
	 * @param source 要关闭的XSLT源 (may be {@code null})
	 */
	private void closeSourceIfNecessary(Source source) {
		if (source instanceof StreamSource) {
			StreamSource streamSource = (StreamSource) source;
			if (streamSource.getReader() != null) {
				try {
					streamSource.getReader().close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
			if (streamSource.getInputStream() != null) {
				try {
					streamSource.getInputStream().close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}
}
