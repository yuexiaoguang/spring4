package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 具体的, 成熟的 {@link BeanDefinition}类的基类, 分解 {@link GenericBeanDefinition},
 * {@link RootBeanDefinition}, {@link ChildBeanDefinition}的常见属性.
 *
 * <p>autowire常量与 {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}接口中定义的常量匹配.
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor
		implements BeanDefinition, Cloneable {

	/**
	 * 默认作用域名称: {@code ""}, 等效于单例状态, 除非从父bean定义中重写.
	 */
	public static final String SCOPE_DEFAULT = "";

	/**
	 * 表示根本没有自动装配.
	 */
	public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

	/**
	 * 表示按名称自动装配bean属性.
	 */
	public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
	 * 表示按类型自动装配bean属性.
	 */
	public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

	/**
	 * 指示自动装配构造函数.
	 */
	public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

	/**
	 * 表示通过反射bean类确定适当的自动装配策略.
	 * 
	 * @deprecated 截止Spring 3.0: 如果使用混合自动装配策略, 使用基于注解的自动装配来更清晰地划分自动装配需求.
	 */
	@Deprecated
	public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

	/**
	 * 表示根本没有依赖性检查.
	 */
	public static final int DEPENDENCY_CHECK_NONE = 0;

	/**
	 * 指示对象引用的依赖性检查.
	 */
	public static final int DEPENDENCY_CHECK_OBJECTS = 1;

	/**
	 * 表示对“简单”属性的依赖性检查.
	 */
	public static final int DEPENDENCY_CHECK_SIMPLE = 2;

	/**
	 * 表示所有属性的依赖性检查 (对象引用以及 "simple"属性).
	 */
	public static final int DEPENDENCY_CHECK_ALL = 3;

	/**
	 * 指示容器应该尝试推断bean的{@link #setDestroyMethodName销毁方法名称}, 而不是显式指定方法名称.
	 * 值{@value}专门设计为包含方法名称中非法的字符, 确保不会与具有相同名称的合法命名方法发生冲突.
	 * <p>目前, 在destroy方法推断期间检测到的方法名称是“close”和“shutdown”, 如果存在于特定的bean类中.
	 */
	public static final String INFER_METHOD = "(inferred)";


	private volatile Object beanClass;

	private String scope = SCOPE_DEFAULT;

	private boolean abstractFlag = false;

	private boolean lazyInit = false;

	private int autowireMode = AUTOWIRE_NO;

	private int dependencyCheck = DEPENDENCY_CHECK_NONE;

	private String[] dependsOn;

	private boolean autowireCandidate = true;

	private boolean primary = false;

	private final Map<String, AutowireCandidateQualifier> qualifiers =
			new LinkedHashMap<String, AutowireCandidateQualifier>(0);

	private boolean nonPublicAccessAllowed = true;

	private boolean lenientConstructorResolution = true;

	private String factoryBeanName;

	private String factoryMethodName;

	private ConstructorArgumentValues constructorArgumentValues;

	private MutablePropertyValues propertyValues;

	private MethodOverrides methodOverrides = new MethodOverrides();

	private String initMethodName;

	private String destroyMethodName;

	private boolean enforceInitMethod = true;

	private boolean enforceDestroyMethod = true;

	private boolean synthetic = false;

	private int role = BeanDefinition.ROLE_APPLICATION;

	private String description;

	private Resource resource;


	protected AbstractBeanDefinition() {
		this(null, null);
	}

	/**
	 * 使用给定的构造函数参数值和属性值.
	 */
	protected AbstractBeanDefinition(ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		setConstructorArgumentValues(cargs);
		setPropertyValues(pvs);
	}

	/**
	 * 深度复制给定的bean定义.
	 * 
	 * @param original 要从中复制的原始bean定义
	 */
	protected AbstractBeanDefinition(BeanDefinition original) {
		setParentName(original.getParentName());
		setBeanClassName(original.getBeanClassName());
		setScope(original.getScope());
		setAbstract(original.isAbstract());
		setLazyInit(original.isLazyInit());
		setFactoryBeanName(original.getFactoryBeanName());
		setFactoryMethodName(original.getFactoryMethodName());
		setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
		setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
		setRole(original.getRole());
		setSource(original.getSource());
		copyAttributesFrom(original);

		if (original instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition originalAbd = (AbstractBeanDefinition) original;
			if (originalAbd.hasBeanClass()) {
				setBeanClass(originalAbd.getBeanClass());
			}
			setAutowireMode(originalAbd.getAutowireMode());
			setDependencyCheck(originalAbd.getDependencyCheck());
			setDependsOn(originalAbd.getDependsOn());
			setAutowireCandidate(originalAbd.isAutowireCandidate());
			setPrimary(originalAbd.isPrimary());
			copyQualifiersFrom(originalAbd);
			setNonPublicAccessAllowed(originalAbd.isNonPublicAccessAllowed());
			setLenientConstructorResolution(originalAbd.isLenientConstructorResolution());
			setMethodOverrides(new MethodOverrides(originalAbd.getMethodOverrides()));
			setInitMethodName(originalAbd.getInitMethodName());
			setEnforceInitMethod(originalAbd.isEnforceInitMethod());
			setDestroyMethodName(originalAbd.getDestroyMethodName());
			setEnforceDestroyMethod(originalAbd.isEnforceDestroyMethod());
			setSynthetic(originalAbd.isSynthetic());
			setResource(originalAbd.getResource());
		}
		else {
			setResourceDescription(original.getResourceDescription());
		}
	}


	/**
	 * 从给定的bean定义(可能是子节点)覆盖此bean定义中的设置(可能是父子继承关系中复制的父节点).
	 * <ul>
	 * <li>如果在给定的bean定义中指定, 将覆盖beanClass.
	 * <li>将始终从给定的bean定义中获取 {@code abstract}, {@code scope}, {@code lazyInit}, {@code autowireMode},
	 * {@code dependencyCheck}, {@code dependsOn}.
	 * <li>将从给定的bean定义中添加{@code constructorArgumentValues}, {@code propertyValues}, {@code methodOverrides}到现有的定义.
	 * <li>如果在给定的bean定义中指定, 将覆盖{@code factoryBeanName}, {@code factoryMethodName},
	 * {@code initMethodName}, {@code destroyMethodName}.
	 * </ul>
	 */
	public void overrideFrom(BeanDefinition other) {
		if (StringUtils.hasLength(other.getBeanClassName())) {
			setBeanClassName(other.getBeanClassName());
		}
		if (StringUtils.hasLength(other.getScope())) {
			setScope(other.getScope());
		}
		setAbstract(other.isAbstract());
		setLazyInit(other.isLazyInit());
		if (StringUtils.hasLength(other.getFactoryBeanName())) {
			setFactoryBeanName(other.getFactoryBeanName());
		}
		if (StringUtils.hasLength(other.getFactoryMethodName())) {
			setFactoryMethodName(other.getFactoryMethodName());
		}
		getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
		getPropertyValues().addPropertyValues(other.getPropertyValues());
		setRole(other.getRole());
		setSource(other.getSource());
		copyAttributesFrom(other);

		if (other instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition otherAbd = (AbstractBeanDefinition) other;
			if (otherAbd.hasBeanClass()) {
				setBeanClass(otherAbd.getBeanClass());
			}
			setAutowireMode(otherAbd.getAutowireMode());
			setDependencyCheck(otherAbd.getDependencyCheck());
			setDependsOn(otherAbd.getDependsOn());
			setAutowireCandidate(otherAbd.isAutowireCandidate());
			setPrimary(otherAbd.isPrimary());
			copyQualifiersFrom(otherAbd);
			setNonPublicAccessAllowed(otherAbd.isNonPublicAccessAllowed());
			setLenientConstructorResolution(otherAbd.isLenientConstructorResolution());
			getMethodOverrides().addOverrides(otherAbd.getMethodOverrides());
			if (StringUtils.hasLength(otherAbd.getInitMethodName())) {
				setInitMethodName(otherAbd.getInitMethodName());
				setEnforceInitMethod(otherAbd.isEnforceInitMethod());
			}
			if (otherAbd.getDestroyMethodName() != null) {
				setDestroyMethodName(otherAbd.getDestroyMethodName());
				setEnforceDestroyMethod(otherAbd.isEnforceDestroyMethod());
			}
			setSynthetic(otherAbd.isSynthetic());
			setResource(otherAbd.getResource());
		}
		else {
			setResourceDescription(other.getResourceDescription());
		}
	}

	/**
	 * 将提供的默认值应用于此bean.
	 * 
	 * @param defaults 要应用的默认值
	 */
	public void applyDefaults(BeanDefinitionDefaults defaults) {
		setLazyInit(defaults.isLazyInit());
		setAutowireMode(defaults.getAutowireMode());
		setDependencyCheck(defaults.getDependencyCheck());
		setInitMethodName(defaults.getInitMethodName());
		setEnforceInitMethod(false);
		setDestroyMethodName(defaults.getDestroyMethodName());
		setEnforceDestroyMethod(false);
	}


	/**
	 * 指定此bean定义的bean类名称.
	 */
	@Override
	public void setBeanClassName(String beanClassName) {
		this.beanClass = beanClassName;
	}

	/**
	 * 返回此bean定义的当前bean类名.
	 */
	@Override
	public String getBeanClassName() {
		Object beanClassObject = this.beanClass;
		if (beanClassObject instanceof Class) {
			return ((Class<?>) beanClassObject).getName();
		}
		else {
			return (String) beanClassObject;
		}
	}

	/**
	 * 指定此bean的类.
	 */
	public void setBeanClass(Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	/**
	 * 如果已经解析, 则返回包装的bean的类.
	 * 
	 * @return bean类, 或{@code null}
	 * @throws IllegalStateException 如果bean定义没有定义bean类, 或者指定的bean类名尚未解析为实际的Class
	 */
	public Class<?> getBeanClass() throws IllegalStateException {
		Object beanClassObject = this.beanClass;
		if (beanClassObject == null) {
			throw new IllegalStateException("No bean class specified on bean definition");
		}
		if (!(beanClassObject instanceof Class)) {
			throw new IllegalStateException(
					"Bean class name [" + beanClassObject + "] has not been resolved into an actual Class");
		}
		return (Class<?>) beanClassObject;
	}

	/**
	 * 返回此定义是否指定bean类.
	 */
	public boolean hasBeanClass() {
		return (this.beanClass instanceof Class);
	}

	/**
	 * 确定包装的bean的类, 必要时从指定的类名解析它.
	 * 在使用已经解析的bean类调用时, 还将从其名称重新加载指定的Class.
	 * 
	 * @param classLoader 用于解析(潜在)类名的ClassLoader
	 * 
	 * @return 解析的bean类
	 * @throws ClassNotFoundException 如果类名可以解析
	 */
	public Class<?> resolveBeanClass(ClassLoader classLoader) throws ClassNotFoundException {
		String className = getBeanClassName();
		if (className == null) {
			return null;
		}
		Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
		this.beanClass = resolvedClass;
		return resolvedClass;
	}

	/**
	 * 设置bean的目标作用域的名称.
	 * <p>默认值为单例状态, 但仅在bean定义在工厂中变为活动状态时才应用.
	 * bean定义最终可能会从父bean定义继承其作用域.
	 * 因此, 默认作用域名称为空字符串 (i.e., {@code ""}), 假定为单例状态, 直到设置已解析的作用域.
	 */
	@Override
	public void setScope(String scope) {
		this.scope = scope;
	}

	/**
	 * 返回bean的目标作用域的名称.
	 */
	@Override
	public String getScope() {
		return this.scope;
	}

	/**
	 * 返回是否为 <b>Singleton</b>, 并从所有调用返回单个共享实例.
	 */
	@Override
	public boolean isSingleton() {
		return SCOPE_SINGLETON.equals(scope) || SCOPE_DEFAULT.equals(scope);
	}

	/**
	 * 返回是否为<b>Prototype</b>, 并从每个调用返回独立的实例.
	 */
	@Override
	public boolean isPrototype() {
		return SCOPE_PROTOTYPE.equals(scope);
	}

	/**
	 * 设置这个bean是否是 "abstract", i.e. 并不意味着自己实例化, 而只是作为具体的子级bean定义的父级.
	 * <p>默认 "false". 指定true, 以告诉bean工厂在任何情况下都不尝试实例化该特定bean.
	 */
	public void setAbstract(boolean abstractFlag) {
		this.abstractFlag = abstractFlag;
	}

	/**
	 * 返回这个bean是否是 "abstract", i.e. 并不意味着自己实例化, 而只是作为具体的子级bean定义的父级.
	 */
	@Override
	public boolean isAbstract() {
		return this.abstractFlag;
	}

	/**
	 * 设置是否应该延迟地初始化此bean.
	 * <p>如果是{@code false}, bean将在启动时由bean工厂实例化, 这些工厂执行单例的实时初始化.
	 */
	@Override
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回是否应该延迟地初始化此bean, i.e. 在启动时不实时地实例化. 仅适用于单例bean.
	 */
	@Override
	public boolean isLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 设置自动装配模式. 这决定了是否会发生bean引用的自动检测和设置.
	 * 默认是 AUTOWIRE_NO, 不会自动装配.
	 * 
	 * @param autowireMode 要设置的自动装配模式. 必须是此类中定义的常量之一.
	 */
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * 返回bean定义中指定的autowire模式.
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * 返回已解析的autowire代码, (resolving AUTOWIRE_AUTODETECT to AUTOWIRE_CONSTRUCTOR or AUTOWIRE_BY_TYPE).
	 */
	public int getResolvedAutowireMode() {
		if (this.autowireMode == AUTOWIRE_AUTODETECT) {
			// 确定是否应用setter自动装配或构造函数自动装配.
			// 如果它有一个no-arg的构造函数, 它被认为是setter自动装配, 否则将尝试构造函数自动装配.
			Constructor<?>[] constructors = getBeanClass().getConstructors();
			for (Constructor<?> constructor : constructors) {
				if (constructor.getParameterTypes().length == 0) {
					return AUTOWIRE_BY_TYPE;
				}
			}
			return AUTOWIRE_CONSTRUCTOR;
		}
		else {
			return this.autowireMode;
		}
	}

	/**
	 * 设置依赖性检查代码.
	 * 
	 * @param dependencyCheck 要设置的值. 必须是此类中定义的四个常量之一.
	 */
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * 返回依赖性检查代码.
	 */
	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * 设置此bean依赖其初始化的bean的名称.
	 * bean工厂将保证首先初始化这些bean.
	 * <p>请注意, 依赖关系通常通过bean属性或构造函数参数表示.
	 * 对于其他类型的依赖项, 例如静态 (*ugh*)或启动时的数据库准备, 此属性应该是必需的.
	 */
	@Override
	public void setDependsOn(String... dependsOn) {
		this.dependsOn = dependsOn;
	}

	/**
	 * 返回此bean依赖的bean名称.
	 */
	@Override
	public String[] getDependsOn() {
		return this.dependsOn;
	}

	/**
	 * 设置此bean是否可以自动装配到其他bean中.
	 * <p>请注意, 此标志旨在仅影响基于类型的自动装配.
	 * 它不会影响按名称的显式引用, 即使指定的bean未标记为autowire候选, 也会解析它.
	 * 因此, 如果名称匹配, 按名称自动装配将注入bean.
	 */
	@Override
	public void setAutowireCandidate(boolean autowireCandidate) {
		this.autowireCandidate = autowireCandidate;
	}

	/**
	 * 返回此bean是否可以自动装配到其他bean中.
	 */
	@Override
	public boolean isAutowireCandidate() {
		return this.autowireCandidate;
	}

	/**
	 * 设置此bean是否为主要的autowire候选者.
	 * <p>如果这个值对于多个匹配的候选者中的一个bean来说是{@code true}, 那么它将成为首选的.
	 */
	@Override
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	/**
	 * 返回此bean是否为主要的autowire候选者.
	 */
	@Override
	public boolean isPrimary() {
		return this.primary;
	}

	/**
	 * 注册用于autowire候选解析的限定符, 限定符的类型名称作为Key.
	 */
	public void addQualifier(AutowireCandidateQualifier qualifier) {
		this.qualifiers.put(qualifier.getTypeName(), qualifier);
	}

	/**
	 * 返回此bean是否具有指定的限定符.
	 */
	public boolean hasQualifier(String typeName) {
		return this.qualifiers.keySet().contains(typeName);
	}

	/**
	 * 返回映射到提供的类型名称的限定符.
	 */
	public AutowireCandidateQualifier getQualifier(String typeName) {
		return this.qualifiers.get(typeName);
	}

	/**
	 * 返回所有注册的限定符.
	 * 
	 * @return {@link AutowireCandidateQualifier}对象.
	 */
	public Set<AutowireCandidateQualifier> getQualifiers() {
		return new LinkedHashSet<AutowireCandidateQualifier>(this.qualifiers.values());
	}

	/**
	 * 将限定符从提供的AbstractBeanDefinition复制到此bean定义.
	 * 
	 * @param source 要从中复制的AbstractBeanDefinition
	 */
	public void copyQualifiersFrom(AbstractBeanDefinition source) {
		Assert.notNull(source, "Source must not be null");
		this.qualifiers.putAll(source.qualifiers);
	}

	/**
	 * 是否允许访问non-public构造函数和方法.
	 * 默认 {@code true}; 将其切换为{@code false}仅限public访问.
	 * <p>这适用于构造函数解析, 工厂方法解析, 以及init/destroy方法.
	 * Bean属性访问器在任何情况下都必须是public, 并且不受此设置的影响.
	 * <p>请注意, 注解驱动的配置仍将访问non-public成员, 只要它们已被注解.
	 * 此设置仅适用于此Bean定义中的外部元数据.
	 */
	public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
	}

	/**
	 * 返回是否允许访问non-public构造函数和方法.
	 */
	public boolean isNonPublicAccessAllowed() {
		return this.nonPublicAccessAllowed;
	}

	/**
	 * 指定是否以宽松模式解析构造函数(默认{@code true}), 或切换到严格解析
	 * (在转换参数时多个匹配的模糊构造函数的情况下, 严格模式抛出异常, 而宽松模式将使用具有“最接近”类型匹配的构造函数).
	 */
	public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
		this.lenientConstructorResolution = lenientConstructorResolution;
	}

	/**
	 * 返回是以宽松模式, 还是以严格模式解析构造函数.
	 */
	public boolean isLenientConstructorResolution() {
		return this.lenientConstructorResolution;
	}

	/**
	 * 指定要使用的工厂bean.
	 * 这是调用指定工厂方法的bean的名称.
	 */
	@Override
	public void setFactoryBeanName(String factoryBeanName) {
		this.factoryBeanName = factoryBeanName;
	}

	/**
	 * 返回工厂bean名称.
	 */
	@Override
	public String getFactoryBeanName() {
		return this.factoryBeanName;
	}

	/**
	 * 指定工厂方法.
	 * 将使用构造函数参数调用此方法; 如果未指定任何参数, 则不使用参数调用此方法.
	 * 该方法将在指定的工厂bean上调用, 或者作为本地bean类的静态方法调用.
	 */
	@Override
	public void setFactoryMethodName(String factoryMethodName) {
		this.factoryMethodName = factoryMethodName;
	}

	/**
	 * 返回工厂方法.
	 */
	@Override
	public String getFactoryMethodName() {
		return this.factoryMethodName;
	}

	/**
	 * 为此bean指定构造函数参数值.
	 */
	public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
		this.constructorArgumentValues =
				(constructorArgumentValues != null ? constructorArgumentValues : new ConstructorArgumentValues());
	}

	/**
	 * 返回此bean的构造函数参数值 (never {@code null}).
	 */
	@Override
	public ConstructorArgumentValues getConstructorArgumentValues() {
		return this.constructorArgumentValues;
	}

	/**
	 * 是否为此bean定义了构造函数参数值.
	 */
	public boolean hasConstructorArgumentValues() {
		return !this.constructorArgumentValues.isEmpty();
	}

	/**
	 * 指定此bean的属性值.
	 */
	public void setPropertyValues(MutablePropertyValues propertyValues) {
		this.propertyValues = (propertyValues != null ? propertyValues : new MutablePropertyValues());
	}

	/**
	 * 返回此bean的属性值 (never {@code null}).
	 */
	@Override
	public MutablePropertyValues getPropertyValues() {
		return this.propertyValues;
	}

	/**
	 * 指定bean的方法覆盖.
	 */
	public void setMethodOverrides(MethodOverrides methodOverrides) {
		this.methodOverrides = (methodOverrides != null ? methodOverrides : new MethodOverrides());
	}

	/**
	 * 返回有关IoC容器要覆盖的方法的信息. 如果没有方法覆盖, 则为空.
	 * Never returns {@code null}.
	 */
	public MethodOverrides getMethodOverrides() {
		return this.methodOverrides;
	}

	/**
	 * 设置初始化方法的名称. 默认值为{@code null}, 在这种情况下, 没有初始化方法.
	 */
	public void setInitMethodName(String initMethodName) {
		this.initMethodName = initMethodName;
	}

	/**
	 * 返回初始化方法的名称.
	 */
	public String getInitMethodName() {
		return this.initMethodName;
	}

	/**
	 * 指定配置的init方法是否为默认方法.
	 * 默认是 {@code false}.
	 */
	public void setEnforceInitMethod(boolean enforceInitMethod) {
		this.enforceInitMethod = enforceInitMethod;
	}

	/**
	 * 指示配置的init方法是否为默认方法.
	 */
	public boolean isEnforceInitMethod() {
		return this.enforceInitMethod;
	}

	/**
	 * 设置destroy方法的名称. 默认值为{@code null}, 在这种情况下没有destroy方法.
	 */
	public void setDestroyMethodName(String destroyMethodName) {
		this.destroyMethodName = destroyMethodName;
	}

	/**
	 * 返回destroy方法的名称.
	 */
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

	/**
	 * 指定配置的destroy方法是否为默认方法.
	 * 默认是 {@code false}.
	 */
	public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
		this.enforceDestroyMethod = enforceDestroyMethod;
	}

	/**
	 * 配置的destroy方法是否为默认方法.
	 */
	public boolean isEnforceDestroyMethod() {
		return this.enforceDestroyMethod;
	}

	/**
	 * 设置此bean定义是否为 'synthetic', 即不是由应用程序本身定义
	 * (例如, 通过{@code <aop:config>}创建的基础结构bean, 用于自动代理的帮助程序).
	 */
	public void setSynthetic(boolean synthetic) {
		this.synthetic = synthetic;
	}

	/**
	 * 返回此bean定义是否为 'synthetic', 即不是由应用程序本身定义.
	 */
	public boolean isSynthetic() {
		return this.synthetic;
	}

	/**
	 * 设置此{@code BeanDefinition}的角色提示.
	 */
	public void setRole(int role) {
		this.role = role;
	}

	/**
	 * 返回此{@code BeanDefinition}的角色提示.
	 */
	@Override
	public int getRole() {
		return this.role;
	}

	/**
	 * 设置此bean定义的可读描述.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 返回此bean定义的可读描述.
	 */
	@Override
	public String getDescription() {
		return this.description;
	}

	/**
	 * 设置此bean定义来自的资源 (为了在出现错误时显示上下文).
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * 返回此bean定义来自的资源.
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * 设置此bean定义来自的资源的描述 (为了在出现错误时显示上下文).
	 */
	public void setResourceDescription(String resourceDescription) {
		this.resource = new DescriptiveResource(resourceDescription);
	}

	/**
	 * 返回此bean定义来自的资源的描述 (为了在出现错误时显示上下文).
	 */
	@Override
	public String getResourceDescription() {
		return (this.resource != null ? this.resource.getDescription() : null);
	}

	/**
	 * 设置原始(例如装饰的)BeanDefinition.
	 */
	public void setOriginatingBeanDefinition(BeanDefinition originatingBd) {
		this.resource = new BeanDefinitionResource(originatingBd);
	}

	/**
	 * 返回原始的BeanDefinition, 或{@code null}.
	 * 允许检索装饰的bean定义.
	 * <p>请注意, 此方法返回直接的原始者. 遍历原始者链以查找用户定义的原始BeanDefinition.
	 */
	@Override
	public BeanDefinition getOriginatingBeanDefinition() {
		return (this.resource instanceof BeanDefinitionResource ?
				((BeanDefinitionResource) this.resource).getBeanDefinition() : null);
	}

	/**
	 * 验证此bean定义.
	 * 
	 * @throws BeanDefinitionValidationException 验证失败
	 */
	public void validate() throws BeanDefinitionValidationException {
		if (!getMethodOverrides().isEmpty() && getFactoryMethodName() != null) {
			throw new BeanDefinitionValidationException(
					"Cannot combine static factory method with method overrides: " +
					"the static factory method must create the instance");
		}

		if (hasBeanClass()) {
			prepareMethodOverrides();
		}
	}

	/**
	 * 验证并准备为此bean定义的方法覆盖.
	 * 检查是否存在具有指定名称的方法.
	 * 
	 * @throws BeanDefinitionValidationException 验证失败
	 */
	public void prepareMethodOverrides() throws BeanDefinitionValidationException {
		// 检查查找方法是否存在.
		MethodOverrides methodOverrides = getMethodOverrides();
		if (!methodOverrides.isEmpty()) {
			Set<MethodOverride> overrides = methodOverrides.getOverrides();
			synchronized (overrides) {
				for (MethodOverride mo : overrides) {
					prepareMethodOverride(mo);
				}
			}
		}
	}

	/**
	 * 验证并准备给定的方法覆盖.
	 * 检查是否存在具有指定名称的方法, 如果没有找到, 则将其标记为未重载.
	 * 
	 * @param mo 要验证的MethodOverride对象
	 * 
	 * @throws BeanDefinitionValidationException 验证失败
	 */
	protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
		int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
		if (count == 0) {
			throw new BeanDefinitionValidationException(
					"Invalid method override: no method with name '" + mo.getMethodName() +
					"' on class [" + getBeanClassName() + "]");
		}
		else if (count == 1) {
			// 将覆盖标记为未重载, 以避免arg类型检查的开销.
			mo.setOverloaded(false);
		}
	}


	/**
	 * Object的 {@code clone()}方法.
	 * 委托给 {@link #cloneBeanDefinition()}.
	 */
	@Override
	public Object clone() {
		return cloneBeanDefinition();
	}

	/**
	 * 克隆此bean定义.
	 * 由具体的子类实现.
	 * 
	 * @return 克隆的bean定义对象
	 */
	public abstract AbstractBeanDefinition cloneBeanDefinition();

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractBeanDefinition)) {
			return false;
		}

		AbstractBeanDefinition that = (AbstractBeanDefinition) other;

		if (!ObjectUtils.nullSafeEquals(getBeanClassName(), that.getBeanClassName())) return false;
		if (!ObjectUtils.nullSafeEquals(this.scope, that.scope)) return false;
		if (this.abstractFlag != that.abstractFlag) return false;
		if (this.lazyInit != that.lazyInit) return false;

		if (this.autowireMode != that.autowireMode) return false;
		if (this.dependencyCheck != that.dependencyCheck) return false;
		if (!Arrays.equals(this.dependsOn, that.dependsOn)) return false;
		if (this.autowireCandidate != that.autowireCandidate) return false;
		if (!ObjectUtils.nullSafeEquals(this.qualifiers, that.qualifiers)) return false;
		if (this.primary != that.primary) return false;

		if (this.nonPublicAccessAllowed != that.nonPublicAccessAllowed) return false;
		if (this.lenientConstructorResolution != that.lenientConstructorResolution) return false;
		if (!ObjectUtils.nullSafeEquals(this.constructorArgumentValues, that.constructorArgumentValues)) return false;
		if (!ObjectUtils.nullSafeEquals(this.propertyValues, that.propertyValues)) return false;
		if (!ObjectUtils.nullSafeEquals(this.methodOverrides, that.methodOverrides)) return false;

		if (!ObjectUtils.nullSafeEquals(this.factoryBeanName, that.factoryBeanName)) return false;
		if (!ObjectUtils.nullSafeEquals(this.factoryMethodName, that.factoryMethodName)) return false;
		if (!ObjectUtils.nullSafeEquals(this.initMethodName, that.initMethodName)) return false;
		if (this.enforceInitMethod != that.enforceInitMethod) return false;
		if (!ObjectUtils.nullSafeEquals(this.destroyMethodName, that.destroyMethodName)) return false;
		if (this.enforceDestroyMethod != that.enforceDestroyMethod) return false;

		if (this.synthetic != that.synthetic) return false;
		if (this.role != that.role) return false;

		return super.equals(other);
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(getBeanClassName());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.scope);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.constructorArgumentValues);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.propertyValues);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryBeanName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryMethodName);
		hashCode = 29 * hashCode + super.hashCode();
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("class [");
		sb.append(getBeanClassName()).append("]");
		sb.append("; scope=").append(this.scope);
		sb.append("; abstract=").append(this.abstractFlag);
		sb.append("; lazyInit=").append(this.lazyInit);
		sb.append("; autowireMode=").append(this.autowireMode);
		sb.append("; dependencyCheck=").append(this.dependencyCheck);
		sb.append("; autowireCandidate=").append(this.autowireCandidate);
		sb.append("; primary=").append(this.primary);
		sb.append("; factoryBeanName=").append(this.factoryBeanName);
		sb.append("; factoryMethodName=").append(this.factoryMethodName);
		sb.append("; initMethodName=").append(this.initMethodName);
		sb.append("; destroyMethodName=").append(this.destroyMethodName);
		if (this.resource != null) {
			sb.append("; defined in ").append(this.resource.getDescription());
		}
		return sb.toString();
	}

}
