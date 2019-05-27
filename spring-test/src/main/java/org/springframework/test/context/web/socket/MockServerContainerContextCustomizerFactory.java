package org.springframework.test.context.web.socket;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} which creates a {@link MockServerContainerContextCustomizer}
 * if WebSocket support is present in the classpath and the test class is annotated
 * with {@code @WebAppConfiguration}.
 */
class MockServerContainerContextCustomizerFactory implements ContextCustomizerFactory {

	private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME =
			"org.springframework.test.context.web.WebAppConfiguration";

	private static final String MOCK_SERVER_CONTAINER_CONTEXT_CUSTOMIZER_CLASS_NAME =
			"org.springframework.test.context.web.socket.MockServerContainerContextCustomizer";

	private static final boolean webSocketPresent = ClassUtils.isPresent("javax.websocket.server.ServerContainer",
			MockServerContainerContextCustomizerFactory.class.getClassLoader());


	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		if (webSocketPresent && isAnnotatedWithWebAppConfiguration(testClass)) {
			try {
				Class<?> clazz = ClassUtils.forName(MOCK_SERVER_CONTAINER_CONTEXT_CUSTOMIZER_CLASS_NAME,
						getClass().getClassLoader());
				return (ContextCustomizer) BeanUtils.instantiateClass(clazz);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to enable WebSocket test support; could not load class: " +
						MOCK_SERVER_CONTAINER_CONTEXT_CUSTOMIZER_CLASS_NAME, ex);
			}
		}

		// Else, nothing to customize
		return null;
	}

	private static boolean isAnnotatedWithWebAppConfiguration(Class<?> testClass) {
		return (AnnotatedElementUtils.findMergedAnnotationAttributes(testClass,
				WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME, false, false) != null);
	}

}
