package org.springframework.web.servlet.mvc.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.servlet.handler.AbstractDetectingUrlHandlerMapping;

/**
 * {@link org.springframework.web.servlet.HandlerMapping}实现的基类,
 * 根据特定控制器类型的约定派生URL路径.
 *
 * @deprecated 从4.3开始, 使用注解驱动的处理器方法
 */
@Deprecated
public abstract class AbstractControllerUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping  {

	private ControllerTypePredicate predicate = new AnnotationControllerTypePredicate();

	private Set<String> excludedPackages = Collections.singleton("org.springframework.web.servlet.mvc");

	private Set<Class<?>> excludedClasses = Collections.emptySet();


	/**
	 * Set whether to activate or deactivate detection of annotated controllers.
	 */
	public void setIncludeAnnotatedControllers(boolean includeAnnotatedControllers) {
		this.predicate = (includeAnnotatedControllers ?
				new AnnotationControllerTypePredicate() : new ControllerTypePredicate());
	}

	/**
	 * Specify Java packages that should be excluded from this mapping.
	 * Any classes in such a package (or any of its subpackages) will be
	 * ignored by this HandlerMapping.
	 * <p>Default is to exclude the entire "org.springframework.web.servlet.mvc"
	 * package, including its subpackages, since none of Spring's out-of-the-box
	 * Controller implementations is a reasonable candidate for this mapping strategy.
	 * Such controllers are typically handled by a separate HandlerMapping,
	 * e.g. a {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping},
	 * alongside this ControllerClassNameHandlerMapping for application controllers.
	 */
	public void setExcludedPackages(String... excludedPackages) {
		this.excludedPackages = (excludedPackages != null) ?
				new HashSet<String>(Arrays.asList(excludedPackages)) : new HashSet<String>();
	}

	/**
	 * Specify controller classes that should be excluded from this mapping.
	 * Any such classes will simply be ignored by this HandlerMapping.
	 */
	public void setExcludedClasses(Class<?>... excludedClasses) {
		this.excludedClasses = (excludedClasses != null) ?
				new HashSet<Class<?>>(Arrays.asList(excludedClasses)) : new HashSet<Class<?>>();
	}


	/**
	 * This implementation delegates to {@link #buildUrlsForHandler},
	 * provided that {@link #isEligibleForMapping} returns {@code true}.
	 */
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		Class<?> beanClass = getApplicationContext().getType(beanName);
		if (isEligibleForMapping(beanName, beanClass)) {
			return buildUrlsForHandler(beanName, beanClass);
		}
		else {
			return null;
		}
	}

	/**
	 * Determine whether the specified controller is excluded from this mapping.
	 * @param beanName the name of the controller bean
	 * @param beanClass the concrete class of the controller bean
	 * @return whether the specified class is excluded
	 * @see #setExcludedPackages
	 * @see #setExcludedClasses
	 */
	protected boolean isEligibleForMapping(String beanName, Class<?> beanClass) {
		if (beanClass == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
						"because its bean type could not be determined");
			}
			return false;
		}
		if (this.excludedClasses.contains(beanClass)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
						"because its bean class is explicitly excluded: " + beanClass.getName());
			}
			return false;
		}
		String beanClassName = beanClass.getName();
		for (String packageName : this.excludedPackages) {
			if (beanClassName.startsWith(packageName)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
							"because its bean class is defined in an excluded package: " + beanClass.getName());
				}
				return false;
			}
		}
		return isControllerType(beanClass);
	}

	/**
	 * Determine whether the given bean class indicates a controller type
	 * that is supported by this mapping strategy.
	 * @param beanClass the class to introspect
	 */
	protected boolean isControllerType(Class<?> beanClass) {
		return this.predicate.isControllerType(beanClass);
	}

	/**
	 * Determine whether the given bean class indicates a controller type
	 * that dispatches to multiple action methods.
	 * @param beanClass the class to introspect
	 */
	protected boolean isMultiActionControllerType(Class<?> beanClass) {
		return this.predicate.isMultiActionControllerType(beanClass);
	}


	/**
	 * Abstract template method to be implemented by subclasses.
	 * @param beanName the name of the bean
	 * @param beanClass the type of the bean
	 * @return the URLs determined for the bean
	 */
	protected abstract String[] buildUrlsForHandler(String beanName, Class<?> beanClass);

}
