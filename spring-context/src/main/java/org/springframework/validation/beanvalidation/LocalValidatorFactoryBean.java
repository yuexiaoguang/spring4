package org.springframework.validation.beanvalidation;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.bootstrap.ProviderSpecificBootstrap;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 这是Spring应用程序上下文中{@code javax.validation} (JSR-303)设置的中心类:
 * 它引导{@code javax.validation.ValidationFactory},
 * 并通过Spring {@link org.springframework.validation.Validator}接口
 * 以及JSR-303 {@link javax.validation.Validator}接口
 * 以及 {@link javax.validation.ValidatorFactory}接口公开它.
 *
 * <p>通过Spring或JSR-303 Validator接口与该bean的实例进行通信时, 将与底层ValidatorFactory的默认Validator进行通信.
 * 这非常方便, 因为不必在工厂执行另一个调用, 假设您几乎总是会使用默认的Validator.
 * 这也可以直接注入到{@link org.springframework.validation.Validator}类型的任何目标依赖项中!
 *
 * <p><b>从Spring 4.0开始, 这个类支持Bean Validation 1.0和1.1, 特别支持Hibernate Validator 4.3 和 5.x</b>
 * (see {@link #setValidationMessageSource}).
 *
 * <p>请注意, 不支持Bean Validation 1.1的{@code #forExecutables}方法:
 * 不希望应用程序代码调用该方法; 请考虑{@link MethodValidationInterceptor}.
 * 如果你真的需要程序化{@code #forExecutables}访问, 请将此类注入为{@link ValidatorFactory}并在其上调用{@link #getValidator()},
 * 然后在返回的本地{@link Validator}引用上调用{@code #forExecutables}, 而不是直接在此类上.
 * 或者, 调用{@code #unwrap(Validator.class)}, 它也将提供本机对象.
 *
 * <p>Spring的MVC配置命名空间也使用此类, 如果{@code javax.validation} API存在, 但未配置显式Validator.
 */
public class LocalValidatorFactoryBean extends SpringValidatorAdapter
		implements ValidatorFactory, ApplicationContextAware, InitializingBean, DisposableBean {

	// Bean Validation 1.1 close() method available?
	private static final Method closeMethod = ClassUtils.getMethodIfAvailable(ValidatorFactory.class, "close");


	@SuppressWarnings("rawtypes")
	private Class providerClass;

	private ValidationProviderResolver validationProviderResolver;

	private MessageInterpolator messageInterpolator;

	private TraversableResolver traversableResolver;

	private ConstraintValidatorFactory constraintValidatorFactory;

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private Resource[] mappingLocations;

	private final Map<String, String> validationPropertyMap = new HashMap<String, String>();

	private ApplicationContext applicationContext;

	private ValidatorFactory validatorFactory;


	/**
	 * 指定所需的提供器类.
	 * <p>如果未指定, 将使用JSR-303的默认搜索机制.
	 */
	@SuppressWarnings("rawtypes")
	public void setProviderClass(Class providerClass) {
		this.providerClass = providerClass;
	}

	/**
	 * 指定JSR-303 {@link ValidationProviderResolver}以引导选择的提供器, 作为{@code META-INF}驱动的解析方案的替代方案.
	 */
	public void setValidationProviderResolver(ValidationProviderResolver validationProviderResolver) {
		this.validationProviderResolver = validationProviderResolver;
	}

	/**
	 * 指定用于此ValidatorFactory及其公开的默认Validator的自定义MessageInterpolator.
	 */
	public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
		this.messageInterpolator = messageInterpolator;
	}

	/**
	 * 指定用于解析验证消息的自定义Spring MessageSource, 而不是依赖于类路径中JSR-303的默认"ValidationMessages.properties"包.
	 * 这可能是指Spring上下文的共享"messageSource" bean, 或者是一些特殊的MessageSource设置, 仅用于验证目的.
	 * <p><b>NOTE:</b> 此功能需要在类路径上使用Hibernate Validator 4.3或更高版本.
	 * 可以使用不同的验证提供程序, 但在配置期间必须可以访问Hibernate Validator的 {@link ResourceBundleMessageInterpolator}类.
	 * <p>指定此属性或{@link #setMessageInterpolator "messageInterpolator"}, 而不是两者都指定.
	 * 如果要构建自定义MessageInterpolator, 请考虑从Hibernate Validator的{@link ResourceBundleMessageInterpolator}派生,
	 * 并在构造插补器时传入基于Spring的{@code ResourceBundleLocator}.
	 * <p>为了仍然解析Hibernate的默认验证消息, 必须为可选解析配置{@link MessageSource} (通常是默认值).
	 * 特别是, 此处指定的{@code MessageSource}实例不应该应用
	 * {@link org.springframework.context.support.AbstractMessageSource#setUseCodeAsDefaultMessage "useCodeAsDefaultMessage"}行为.
	 * 请相应地仔细检查您的设置.
	 */
	public void setValidationMessageSource(MessageSource messageSource) {
		this.messageInterpolator = HibernateValidatorDelegate.buildMessageInterpolator(messageSource);
	}

	/**
	 * 指定要用于此ValidatorFactory及其公开的默认Validator的自定义TraversableResolver.
	 */
	public void setTraversableResolver(TraversableResolver traversableResolver) {
		this.traversableResolver = traversableResolver;
	}

	/**
	 * 指定用于此ValidatorFactory的自定义ConstraintValidatorFactory.
	 * <p>默认{@link SpringConstraintValidatorFactory}, 委托给包含ApplicationContext,
	 * 以创建自动装配的ConstraintValidator实例.
	 */
	public void setConstraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
		this.constraintValidatorFactory = constraintValidatorFactory;
	}

	/**
	 * 如果需要进行消息插值, 设置用于解析方法和构造函数参数名称的ParameterNameDiscoverer.
	 * <p>默认{@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 指定从中加载XML约束映射文件的资源位置.
	 */
	public void setMappingLocations(Resource... mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * 指定要传递给验证提供器的bean验证属性.
	 * <p>可以使用String "value" (通过PropertiesEditor解析) 或XML bean定义中的"props"元素填充.
	 */
	public void setValidationProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.validationPropertyMap);
	}

	/**
	 * 指定要传递给验证提供器的bean验证属性.
	 * <p>可以在XML bean定义中使用"map" 或 "props"元素填充.
	 */
	public void setValidationPropertyMap(Map<String, String> validationProperties) {
		if (validationProperties != null) {
			this.validationPropertyMap.putAll(validationProperties);
		}
	}

	/**
	 * 允许将可以访问bean验证属性的Map传递给验证提供器, 并提供添加或覆盖特定条目的选项.
	 * <p>用于直接指定条目, 例如通过"validationPropertyMap[myKey]".
	 */
	public Map<String, String> getValidationPropertyMap() {
		return this.validationPropertyMap;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void afterPropertiesSet() {
		Configuration<?> configuration;
		if (this.providerClass != null) {
			ProviderSpecificBootstrap bootstrap = Validation.byProvider(this.providerClass);
			if (this.validationProviderResolver != null) {
				bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
			}
			configuration = bootstrap.configure();
		}
		else {
			GenericBootstrap bootstrap = Validation.byDefaultProvider();
			if (this.validationProviderResolver != null) {
				bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
			}
			configuration = bootstrap.configure();
		}

		// Try Hibernate Validator 5.2's externalClassLoader(ClassLoader) method
		if (this.applicationContext != null) {
			try {
				Method eclMethod = configuration.getClass().getMethod("externalClassLoader", ClassLoader.class);
				ReflectionUtils.invokeMethod(eclMethod, configuration, this.applicationContext.getClassLoader());
			}
			catch (NoSuchMethodException ex) {
				// Ignore - no Hibernate Validator 5.2+ or similar provider
			}
		}

		MessageInterpolator targetInterpolator = this.messageInterpolator;
		if (targetInterpolator == null) {
			targetInterpolator = configuration.getDefaultMessageInterpolator();
		}
		configuration.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));

		if (this.traversableResolver != null) {
			configuration.traversableResolver(this.traversableResolver);
		}

		ConstraintValidatorFactory targetConstraintValidatorFactory = this.constraintValidatorFactory;
		if (targetConstraintValidatorFactory == null && this.applicationContext != null) {
			targetConstraintValidatorFactory =
					new SpringConstraintValidatorFactory(this.applicationContext.getAutowireCapableBeanFactory());
		}
		if (targetConstraintValidatorFactory != null) {
			configuration.constraintValidatorFactory(targetConstraintValidatorFactory);
		}

		if (this.parameterNameDiscoverer != null) {
			configureParameterNameProviderIfPossible(configuration);
		}

		if (this.mappingLocations != null) {
			for (Resource location : this.mappingLocations) {
				try {
					configuration.addMapping(location.getInputStream());
				}
				catch (IOException ex) {
					throw new IllegalStateException("Cannot read mapping resource: " + location);
				}
			}
		}

		for (Map.Entry<String, String> entry : this.validationPropertyMap.entrySet()) {
			configuration.addProperty(entry.getKey(), entry.getValue());
		}

		// 在实际构建ValidatorFactory之前允许自定义后处理.
		postProcessConfiguration(configuration);

		this.validatorFactory = configuration.buildValidatorFactory();
		setTargetValidator(this.validatorFactory.getValidator());
	}

	private void configureParameterNameProviderIfPossible(Configuration<?> configuration) {
		try {
			Class<?> parameterNameProviderClass =
					ClassUtils.forName("javax.validation.ParameterNameProvider", getClass().getClassLoader());
			Method parameterNameProviderMethod =
					Configuration.class.getMethod("parameterNameProvider", parameterNameProviderClass);
			final Object defaultProvider = ReflectionUtils.invokeMethod(
					Configuration.class.getMethod("getDefaultParameterNameProvider"), configuration);
			final ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
			Object parameterNameProvider = Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {parameterNameProviderClass}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					if (method.getName().equals("getParameterNames")) {
						String[] result = null;
						if (args[0] instanceof Constructor) {
							result = discoverer.getParameterNames((Constructor<?>) args[0]);
						}
						else if (args[0] instanceof Method) {
							result = discoverer.getParameterNames((Method) args[0]);
						}
						if (result != null) {
							return Arrays.asList(result);
						}
						else {
							try {
								return method.invoke(defaultProvider, args);
							}
							catch (InvocationTargetException ex) {
								throw ex.getTargetException();
							}
						}
					}
					else {
						// toString, equals, hashCode
						try {
							return method.invoke(this, args);
						}
						catch (InvocationTargetException ex) {
							throw ex.getTargetException();
						}
					}
				}
			});
			ReflectionUtils.invokeMethod(parameterNameProviderMethod, configuration, parameterNameProvider);

		}
		catch (Throwable ex) {
			// Bean Validation 1.1 API not available - simply not applying the ParameterNameDiscoverer
		}
	}

	/**
	 * 对给定的Bean Validation配置进行后处理, 添加或覆盖其任何设置.
	 * <p>在构建{@link ValidatorFactory}之前调用.
	 * 
	 * @param configuration Configuration对象, 预先填充由LocalValidatorFactoryBean属性驱动的设置
	 */
	protected void postProcessConfiguration(Configuration<?> configuration) {
	}


	@Override
	public Validator getValidator() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getValidator();
	}

	@Override
	public ValidatorContext usingContext() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.usingContext();
	}

	@Override
	public MessageInterpolator getMessageInterpolator() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getMessageInterpolator();
	}

	@Override
	public TraversableResolver getTraversableResolver() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getTraversableResolver();
	}

	@Override
	public ConstraintValidatorFactory getConstraintValidatorFactory() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getConstraintValidatorFactory();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if (type == null || !ValidatorFactory.class.isAssignableFrom(type)) {
			try {
				return super.unwrap(type);
			}
			catch (ValidationException ex) {
				// ignore - we'll try ValidatorFactory unwrapping next
			}
		}
		try {
			return this.validatorFactory.unwrap(type);
		}
		catch (ValidationException ex) {
			// ignore if just being asked for ValidatorFactory
			if (ValidatorFactory.class == type) {
				return (T) this.validatorFactory;
			}
			throw ex;
		}
	}


	public void close() {
		if (closeMethod != null && this.validatorFactory != null) {
			ReflectionUtils.invokeMethod(closeMethod, this.validatorFactory);
		}
	}

	@Override
	public void destroy() {
		close();
	}


	/**
	 * 内部类, 避免硬编码的Hibernate Validator依赖.
	 */
	private static class HibernateValidatorDelegate {

		public static MessageInterpolator buildMessageInterpolator(MessageSource messageSource) {
			return new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource));
		}
	}

}
