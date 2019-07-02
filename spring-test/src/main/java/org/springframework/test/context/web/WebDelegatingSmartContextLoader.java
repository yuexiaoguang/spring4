package org.springframework.test.context.web;

import org.springframework.beans.BeanUtils;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.support.AbstractDelegatingSmartContextLoader;
import org.springframework.util.ClassUtils;

/**
 * {@code WebDelegatingSmartContextLoader}是{@link AbstractDelegatingSmartContextLoader}的具体实现,
 * 它委托给{@link GenericXmlWebContextLoader} (如果类路径中存在Groovy, 则为{@link GenericGroovyXmlWebContextLoader})
 * 和{@link AnnotationConfigWebContextLoader}.
 */
public class WebDelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

	private static final String GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.web.GenericGroovyXmlWebContextLoader";

	private static final boolean groovyPresent = ClassUtils.isPresent("groovy.lang.Closure",
		WebDelegatingSmartContextLoader.class.getClassLoader())
			&& ClassUtils.isPresent(GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME,
				WebDelegatingSmartContextLoader.class.getClassLoader());

	private final SmartContextLoader xmlLoader;
	private final SmartContextLoader annotationConfigLoader;


	public WebDelegatingSmartContextLoader() {
		if (groovyPresent) {
			try {
				Class<?> loaderClass = ClassUtils.forName(GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME,
					WebDelegatingSmartContextLoader.class.getClassLoader());
				this.xmlLoader = (SmartContextLoader) BeanUtils.instantiateClass(loaderClass);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to enable support for Groovy scripts; "
						+ "could not load class: " + GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME, ex);
			}
		}
		else {
			this.xmlLoader = new GenericXmlWebContextLoader();
		}

		this.annotationConfigLoader = new AnnotationConfigWebContextLoader();
	}

	@Override
	protected SmartContextLoader getXmlLoader() {
		return this.xmlLoader;
	}

	@Override
	protected SmartContextLoader getAnnotationConfigLoader() {
		return this.annotationConfigLoader;
	}

}
