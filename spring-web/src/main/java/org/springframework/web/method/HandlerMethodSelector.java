package org.springframework.web.method;

import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.core.MethodIntrospector;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * Defines the algorithm for searching handler methods exhaustively including interfaces and parent
 * classes while also dealing with parameterized methods as well as interface and class-based proxies.
 *
 * @deprecated as of Spring 4.2.3, in favor of the generalized and refined {@link MethodIntrospector}
 */
@Deprecated
public abstract class HandlerMethodSelector {

	/**
	 * Select handler methods for the given handler type.
	 * <p>Callers define handler methods of interest through the {@link MethodFilter} parameter.
	 * @param handlerType the handler type to search handler methods on
	 * @param handlerMethodFilter a {@link MethodFilter} to help recognize handler methods of interest
	 * @return the selected methods, or an empty set
	 */
	public static Set<Method> selectMethods(Class<?> handlerType, MethodFilter handlerMethodFilter) {
		return MethodIntrospector.selectMethods(handlerType, handlerMethodFilter);
	}

}
