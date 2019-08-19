package org.springframework.web.servlet.view.xslt;

import java.util.Properties;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver}实现,
 * 它通过将提供的视图名称转换为XSLT样式表的URL来解析{@link XsltView}的实例.
 */
public class XsltViewResolver extends UrlBasedViewResolver {

	private String sourceKey;

	private URIResolver uriResolver;

	private ErrorListener errorListener;

	private boolean indent = true;

	private Properties outputProperties;

	private boolean cacheTemplates = true;


	public XsltViewResolver() {
		setViewClass(requiredViewClass());
	}


	@Override
	protected Class<?> requiredViewClass() {
		return XsltView.class;
	}

	/**
	 * 设置表示XSLT源的model属性的名称.
	 * 如果未指定, 将搜索模型Map以查找匹配的值类型.
	 * <p>开箱即用支持以下源类型:
	 * {@link javax.xml.transform.Source}, {@link org.w3c.dom.Document},
	 * {@link org.w3c.dom.Node}, {@link java.io.Reader}, {@link java.io.InputStream}
	 * 和{@link org.springframework.core.io.Resource}.
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
	 * 设置{@link javax.xml.transform.ErrorListener}接口的实现, 以自定义处理转换错误和警告.
	 * <p>如果未设置, 则使用默认的{@link org.springframework.util.xml.SimpleTransformErrorListener},
	 * 它只使用视图类的记录器实例记录警告, 并重新抛出错误以停止XML转换.
	 */
	public void setErrorListener(ErrorListener errorListener) {
		this.errorListener = errorListener;
	}

	/**
	 * 设置XSLT转换器在输出结果树时是否可以添加额外的空格.
	 * <p>默认为{@code true} (on); 将此设置为{@code false} (off) 以不指定"indent"键, 将选择保存到样式表.
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
	 * 打开/关闭XSLT模板的缓存.
	 * <p>默认为"true". 仅在开发中将其设置为"false", 其中缓存不会严重影响性能.
	 */
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		XsltView view = (XsltView) super.buildView(viewName);
		view.setSourceKey(this.sourceKey);
		if (this.uriResolver != null) {
			view.setUriResolver(this.uriResolver);
		}
		if (this.errorListener != null) {
			view.setErrorListener(this.errorListener);
		}
		view.setIndent(this.indent);
		view.setOutputProperties(this.outputProperties);
		view.setCacheTemplates(this.cacheTemplates);
		return view;
	}

}
