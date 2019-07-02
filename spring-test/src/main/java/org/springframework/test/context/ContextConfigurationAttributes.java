package org.springframework.test.context;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code ContextConfigurationAttributes}封装了通过{@link ContextConfiguration @ContextConfiguration}声明的上下文配置属性.
 */
public class ContextConfigurationAttributes {

	private static final String[] EMPTY_LOCATIONS = new String[0];

	private static final Class<?>[] EMPTY_CLASSES = new Class<?>[0];


	private static final Log logger = LogFactory.getLog(ContextConfigurationAttributes.class);

	private final Class<?> declaringClass;

	private Class<?>[] classes;

	private String[] locations;

	private final boolean inheritLocations;

	private final Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers;

	private final boolean inheritInitializers;

	private final String name;

	private final Class<? extends ContextLoader> contextLoaderClass;


	/**
	 * @param declaringClass 显式或隐式声明{@code @ContextConfiguration}的测试类
	 */
	@SuppressWarnings("unchecked")
	public ContextConfigurationAttributes(Class<?> declaringClass) {
		this(declaringClass, EMPTY_LOCATIONS, EMPTY_CLASSES, false, (Class[]) EMPTY_CLASSES, true, ContextLoader.class);
	}

	/**
	 * @param declaringClass 声明{@code @ContextConfiguration}的测试类
	 * @param contextConfiguration 从中检索属性的注解
	 */
	public ContextConfigurationAttributes(Class<?> declaringClass, ContextConfiguration contextConfiguration) {
		this(declaringClass, contextConfiguration.locations(), contextConfiguration.classes(),
				contextConfiguration.inheritLocations(), contextConfiguration.initializers(),
				contextConfiguration.inheritInitializers(), contextConfiguration.name(), contextConfiguration.loader());
	}

	/**
	 * @param declaringClass 声明{@code @ContextConfiguration}的测试类
	 * @param annAttrs 从中检索属性的注解属性
	 */
	@SuppressWarnings("unchecked")
	public ContextConfigurationAttributes(Class<?> declaringClass, AnnotationAttributes annAttrs) {
		this(declaringClass, annAttrs.getStringArray("locations"), annAttrs.getClassArray("classes"), annAttrs.getBoolean("inheritLocations"),
				(Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[]) annAttrs.getClassArray("initializers"),
				annAttrs.getBoolean("inheritInitializers"), annAttrs.getString("name"), (Class<? extends ContextLoader>) annAttrs.getClass("loader"));
	}

	/**
	 * @param declaringClass 声明{@code @ContextConfiguration}的测试类
	 * @param locations 通过{@code @ContextConfiguration}声明的资源位置
	 * @param classes 通过{@code @ContextConfiguration}声明的带注解的类
	 * @param inheritLocations 通过{@code @ContextConfiguration}声明的{@code inheritLocations}标志
	 * @param initializers 通过{@code @ContextConfiguration}声明的上下文初始化器
	 * @param inheritInitializers 通过{@code @ContextConfiguration}声明的{@code inheritInitializers}标志
	 * @param contextLoaderClass 通过{@code @ContextConfiguration}声明的{@code ContextLoader}类
	 * 
	 * @throws IllegalArgumentException 如果{@code declaringClass}或{@code contextLoaderClass}是{@code null}
	 */
	public ContextConfigurationAttributes(
			Class<?> declaringClass, String[] locations, Class<?>[] classes, boolean inheritLocations,
			Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers,
			boolean inheritInitializers, Class<? extends ContextLoader> contextLoaderClass) {

		this(declaringClass, locations, classes, inheritLocations, initializers, inheritInitializers, null,
				contextLoaderClass);
	}

	/**
	 * @param declaringClass 声明{@code @ContextConfiguration}的测试类
	 * @param locations 通过{@code @ContextConfiguration}声明的资源位置
	 * @param classes 通过{@code @ContextConfiguration}声明的带注解的类
	 * @param inheritLocations 通过{@code @ContextConfiguration}声明的{@code inheritLocations}标志
	 * @param initializers 通过{@code @ContextConfiguration}声明的上下文初始化器
	 * @param inheritInitializers 通过{@code @ContextConfiguration}声明的{@code inheritInitializers}标志
	 * @param name 上下文层次结构中的级别名称, 如果不适用, 则为{@code null}
	 * @param contextLoaderClass 通过{@code @ContextConfiguration}声明的{@code ContextLoader}类
	 * 
	 * @throws IllegalArgumentException 如果{@code declaringClass}或{@code contextLoaderClass}是{@code null}
	 */
	public ContextConfigurationAttributes(
			Class<?> declaringClass, String[] locations, Class<?>[] classes, boolean inheritLocations,
			Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers,
			boolean inheritInitializers, String name, Class<? extends ContextLoader> contextLoaderClass) {

		Assert.notNull(declaringClass, "declaringClass must not be null");
		Assert.notNull(contextLoaderClass, "contextLoaderClass must not be null");

		if (!ObjectUtils.isEmpty(locations) && !ObjectUtils.isEmpty(classes) && logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Test class [%s] has been configured with @ContextConfiguration's 'locations' (or 'value') %s " +
					"and 'classes' %s attributes. Most SmartContextLoader implementations support " +
					"only one declaration of resources per @ContextConfiguration annotation.",
					declaringClass.getName(), ObjectUtils.nullSafeToString(locations),
					ObjectUtils.nullSafeToString(classes)));
		}

		this.declaringClass = declaringClass;
		this.locations = locations;
		this.classes = classes;
		this.inheritLocations = inheritLocations;
		this.initializers = initializers;
		this.inheritInitializers = inheritInitializers;
		this.name = (StringUtils.hasText(name) ? name : null);
		this.contextLoaderClass = contextLoaderClass;
	}


	/**
	 * 获取明确或隐式声明{@link ContextConfiguration @ContextConfiguration}注解的{@linkplain Class class}.
	 * 
	 * @return 声明类 (never {@code null})
	 */
	public Class<?> getDeclaringClass() {
		return this.declaringClass;
	}

	/**
	 * 设置<em>处理的</em>带注解的类, 有效地覆盖通过{@link ContextConfiguration @ContextConfiguration}声明的原始值.
	 */
	public void setClasses(Class<?>... classes) {
		this.classes = classes;
	}

	/**
	 * 获取通过{@link ContextConfiguration @ContextConfiguration}声明的带注解的类.
	 * <p>Note: 这是一个可变的属性.
	 * 因此, 返回的值可能表示<em>已处理的</em>值, 该值与通过{@link ContextConfiguration @ContextConfiguration}声明的原始值不匹配.
	 * 
	 * @return 带注解的类; 可能是{@code null} 或 <em>empty</em>
	 */
	public Class<?>[] getClasses() {
		return this.classes;
	}

	/**
	 * 确定此{@code ContextConfigurationAttributes}实例是否具有基于类的资源.
	 * 
	 * @return {@code true} 如果{@link #getClasses() classes}数组不为空
	 */
	public boolean hasClasses() {
		return !ObjectUtils.isEmpty(getClasses());
	}

	/**
	 * 设置<em>已处理的</em>资源位置, 有效地覆盖通过{@link ContextConfiguration @ContextConfiguration}声明的原始值.
	 */
	public void setLocations(String... locations) {
		this.locations = locations;
	}

	/**
	 * 获取通过{@link ContextConfiguration @ContextConfiguration}声明的资源位置.
	 * <p>Note: 这是一个可变的属性.
	 * 因此, 返回的值可能表示<em>已处理的</em>值, 该值与通过{@link ContextConfiguration @ContextConfiguration}声明的原始值不匹配.
	 * 
	 * @return 资源位置; 可能是{@code null} 或<em>empty</em>
	 */
	public String[] getLocations() {
		return this.locations;
	}

	/**
	 * 确定此{@code ContextConfigurationAttributes}实例是否具有基于路径的资源位置.
	 * 
	 * @return {@code true} 如果{@link #getLocations() locations}数组不为空
	 */
	public boolean hasLocations() {
		return !ObjectUtils.isEmpty(getLocations());
	}

	/**
	 * 确定此{@code ContextConfigurationAttributes}实例是否具有基于路径的资源位置或基于类的资源.
	 * 
	 * @return {@code true} 如果{@link #getLocations() locations}或{@link #getClasses() classes}数组不为空
	 */
	public boolean hasResources() {
		return (hasLocations() || hasClasses());
	}

	/**
	 * 获取通过{@link ContextConfiguration @ContextConfiguration}声明的{@code inheritLocations}标志.
	 * 
	 * @return {@code inheritLocations}标志
	 */
	public boolean isInheritLocations() {
		return this.inheritLocations;
	}

	/**
	 * 获取通过{@link ContextConfiguration @ContextConfiguration}声明的{@code ApplicationContextInitializer}类.
	 * 
	 * @return {@code ApplicationContextInitializer}类
	 */
	public Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] getInitializers() {
		return this.initializers;
	}

	/**
	 * 获取通过{@link ContextConfiguration @ContextConfiguration}声明的{@code inheritInitializers}标志.
	 * 
	 * @return {@code inheritInitializers}标志
	 */
	public boolean isInheritInitializers() {
		return this.inheritInitializers;
	}

	/**
	 * 获取通过{@link ContextConfiguration @ContextConfiguration}声明的上下文层次结构级别的名称.
	 * 
	 * @return 上下文层次结构级别的名称, 或{@code null}
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 获取通过{@link ContextConfiguration @ContextConfiguration}声明的{@code ContextLoader}类.
	 * 
	 * @return {@code ContextLoader}类
	 */
	public Class<? extends ContextLoader> getContextLoaderClass() {
		return this.contextLoaderClass;
	}


	/**
	 * 通过比较两个对象的
	 * {@linkplain #getDeclaringClass() 声明类},
	 * {@linkplain #getLocations() 位置},
	 * {@linkplain #getClasses() 注解类},
	 * {@linkplain #isInheritLocations() inheritLocations flag},
	 * {@linkplain #getInitializers() 上下文初始化器类},
	 * {@linkplain #isInheritInitializers() inheritInitializers flag}, 和
	 * {@link #getContextLoaderClass() ContextLoader class}
	 * 来确定提供的对象是否等于此{@code ContextConfigurationAttributes}实例.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ContextConfigurationAttributes)) {
			return false;
		}
		ContextConfigurationAttributes otherAttr = (ContextConfigurationAttributes) other;
		return (ObjectUtils.nullSafeEquals(this.declaringClass, otherAttr.declaringClass) &&
				Arrays.equals(this.classes, otherAttr.classes)) &&
				Arrays.equals(this.locations, otherAttr.locations) &&
				this.inheritLocations == otherAttr.inheritLocations &&
				Arrays.equals(this.initializers, otherAttr.initializers) &&
				this.inheritInitializers == otherAttr.inheritInitializers &&
				ObjectUtils.nullSafeEquals(this.name, otherAttr.name) &&
				ObjectUtils.nullSafeEquals(this.contextLoaderClass, otherAttr.contextLoaderClass);
	}

	/**
	 * 为{@code ContextConfigurationAttributes}实例的所有属性生成唯一的哈希码, 不包括{@linkplain #getName() name}.
	 */
	@Override
	public int hashCode() {
		int result = this.declaringClass.hashCode();
		result = 31 * result + Arrays.hashCode(this.classes);
		result = 31 * result + Arrays.hashCode(this.locations);
		result = 31 * result + Arrays.hashCode(this.initializers);
		return result;
	}

	/**
	 * 提供上下文配置属性和声明类的String表示.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("declaringClass", this.declaringClass.getName())
				.append("classes", ObjectUtils.nullSafeToString(this.classes))
				.append("locations", ObjectUtils.nullSafeToString(this.locations))
				.append("inheritLocations", this.inheritLocations)
				.append("initializers", ObjectUtils.nullSafeToString(this.initializers))
				.append("inheritInitializers", this.inheritInitializers)
				.append("name", this.name)
				.append("contextLoaderClass", this.contextLoaderClass.getName())
				.toString();
	}

}
