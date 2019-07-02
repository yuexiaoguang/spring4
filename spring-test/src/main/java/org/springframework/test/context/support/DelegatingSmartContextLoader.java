package org.springframework.test.context.support;

import org.springframework.beans.BeanUtils;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.ClassUtils;

/**
 * {@code DelegatingSmartContextLoader}是{@link AbstractDelegatingSmartContextLoader}的具体实现,
 * 它委托给{@link GenericXmlContextLoader} (或{@link GenericGroovyXmlContextLoader}, 如果类路径中存在Groovy)
 * 和{@link AnnotationConfigContextLoader}.
 */
public class DelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

	private static final String GROOVY_XML_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.support.GenericGroovyXmlContextLoader";

	private static final boolean groovyPresent = ClassUtils.isPresent("groovy.lang.Closure",
		DelegatingSmartContextLoader.class.getClassLoader())
			&& ClassUtils.isPresent(GROOVY_XML_CONTEXT_LOADER_CLASS_NAME,
				DelegatingSmartContextLoader.class.getClassLoader());

	private final SmartContextLoader xmlLoader;
	private final SmartContextLoader annotationConfigLoader;


	public DelegatingSmartContextLoader() {
		if (groovyPresent) {
			try {
				Class<?> loaderClass = ClassUtils.forName(GROOVY_XML_CONTEXT_LOADER_CLASS_NAME,
					DelegatingSmartContextLoader.class.getClassLoader());
				this.xmlLoader = (SmartContextLoader) BeanUtils.instantiateClass(loaderClass);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to enable support for Groovy scripts; "
						+ "could not load class: " + GROOVY_XML_CONTEXT_LOADER_CLASS_NAME, ex);
			}
		}
		else {
			this.xmlLoader = new GenericXmlContextLoader();
		}

		this.annotationConfigLoader = new AnnotationConfigContextLoader();
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
