package org.springframework.beans.factory.support;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * 根bean定义表示合并的bean定义, 该定义在运行时支持Spring BeanFactory中的特定bean.
 * 它可能是从多个原始bean定义创建的, 这些定义相互继承, 通常注册为 {@link GenericBeanDefinition GenericBeanDefinitions}.
 * 根bean定义本质上是运行时的“统一”bean定义视图.
 *
 * <p>根bean定义也可用于在配置阶段注册单个bean定义.
 * 但是, 从Spring 2.5开始, 以编程方式注册bean定义的首选方法是{@link GenericBeanDefinition}类.
 * GenericBeanDefinition的优点是它允许动态定义父依赖关系, 而不是将角色“硬编码”为根bean定义.
 */
@SuppressWarnings("serial")
public class RootBeanDefinition extends AbstractBeanDefinition {

	private BeanDefinitionHolder decoratedDefinition;

	private AnnotatedElement qualifiedElement;

	boolean allowCaching = true;

	boolean isFactoryMethodUnique = false;

	volatile ResolvableType targetType;

	/** 包可见字段, 用于缓存给定bean定义的确定的Class */
	volatile Class<?> resolvedTargetType;

	/** 包可见字段, 用于缓存泛型工厂方法的返回类型 */
	volatile ResolvableType factoryMethodReturnType;

	/** 下面四个构造函数字段的常用锁 */
	final Object constructorArgumentLock = new Object();

	/** 包可见字段, 用于缓存已解析的构造函数或工厂方法 */
	Object resolvedConstructorOrFactoryMethod;

	/** 包可见字段, 用于将构造函数参数标记为已解析 */
	boolean constructorArgumentsResolved = false;

	/** 包可见字段, 用于缓存完全解析的构造函数参数 */
	Object[] resolvedConstructorArguments;

	/** 包可见字段, 用于缓存部分准备的构造函数参数 */
	Object[] preparedConstructorArguments;

	/** 下面两个后处理字段的常用锁 */
	final Object postProcessingLock = new Object();

	/** 包可见字段, 指示已应用MergedBeanDefinitionPostProcessor */
	boolean postProcessed = false;

	/** 包可见字段, 表示实例化之前的后处理器已经生效 */
	volatile Boolean beforeInstantiationResolved;

	private Set<Member> externallyManagedConfigMembers;

	private Set<String> externallyManagedInitMethods;

	private Set<String> externallyManagedDestroyMethods;


	/**
	 * 通过其bean属性和配置方法进行配置.
	 */
	public RootBeanDefinition() {
		super();
	}

	/**
	 * @param beanClass 要实例化的bean的类
	 */
	public RootBeanDefinition(Class<?> beanClass) {
		super();
		setBeanClass(beanClass);
	}

	/**
	 * @param beanClass 要实例化的bean的类
	 * @param autowireMode 按名称或类型, 使用此接口中的常量
	 * @param dependencyCheck 是否对对象执行依赖性检查 (不适用于自动装配构造函数, 因此忽略它)
	 */
	public RootBeanDefinition(Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
		super();
		setBeanClass(beanClass);
		setAutowireMode(autowireMode);
		if (dependencyCheck && getResolvedAutowireMode() != AUTOWIRE_CONSTRUCTOR) {
			setDependencyCheck(DEPENDENCY_CHECK_OBJECTS);
		}
	}

	/**
	 * @param beanClass 要实例化的bean的类
	 * @param cargs 要应用的构造函数参数值
	 * @param pvs 要应用的属性值
	 */
	public RootBeanDefinition(Class<?> beanClass, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		super(cargs, pvs);
		setBeanClass(beanClass);
	}

	/**
	 * <p>采用bean类名称以避免实时加载bean类.
	 * 
	 * @param beanClassName 要实例化的类的名称
	 */
	public RootBeanDefinition(String beanClassName) {
		setBeanClassName(beanClassName);
	}

	/**
	 * <p>采用bean类名称以避免实时加载bean类.
	 * 
	 * @param beanClassName 要实例化的类的名称
	 * @param cargs 要应用的构造函数参数值
	 * @param pvs 要应用的属性值
	 */
	public RootBeanDefinition(String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		super(cargs, pvs);
		setBeanClassName(beanClassName);
	}

	/**
	 * 深度克隆.
	 * 
	 * @param original 要从中复制的原始bean定义
	 */
	public RootBeanDefinition(RootBeanDefinition original) {
		super(original);
		this.decoratedDefinition = original.decoratedDefinition;
		this.qualifiedElement = original.qualifiedElement;
		this.allowCaching = original.allowCaching;
		this.isFactoryMethodUnique = original.isFactoryMethodUnique;
		this.targetType = original.targetType;
	}

	/**
	 * 深度克隆.
	 * 
	 * @param original 要从中复制的原始bean定义
	 */
	RootBeanDefinition(BeanDefinition original) {
		super(original);
	}


	@Override
	public String getParentName() {
		return null;
	}

	@Override
	public void setParentName(String parentName) {
		if (parentName != null) {
			throw new IllegalArgumentException("Root bean cannot be changed into a child bean with parent reference");
		}
	}

	/**
	 * 注册由此bean定义修饰的目标定义.
	 */
	public void setDecoratedDefinition(BeanDefinitionHolder decoratedDefinition) {
		this.decoratedDefinition = decoratedDefinition;
	}

	/**
	 * 返回由此bean定义修饰的目标定义.
	 */
	public BeanDefinitionHolder getDecoratedDefinition() {
		return this.decoratedDefinition;
	}

	/**
	 * 指定要定义限定符的{@link AnnotatedElement}, 而不是目标类或工厂方法.
	 */
	public void setQualifiedElement(AnnotatedElement qualifiedElement) {
		this.qualifiedElement = qualifiedElement;
	}

	/**
	 * 返回要定义限定符的{@link AnnotatedElement}.
	 * 否则, 将检查工厂方法和目标类.
	 */
	public AnnotatedElement getQualifiedElement() {
		return this.qualifiedElement;
	}

	/**
	 * 如果事先知道, 指定此bean定义的包含泛型的目标类型.
	 */
	public void setTargetType(ResolvableType targetType) {
		this.targetType = targetType;
	}

	/**
	 * 如果事先知道, 指定此bean定义的目标类型.
	 */
	public void setTargetType(Class<?> targetType) {
		this.targetType = (targetType != null ? ResolvableType.forClass(targetType) : null);
	}

	/**
	 * 返回此bean定义的目标类型 (要么提前指定, 要么在第一次实例化时解析).
	 */
	public Class<?> getTargetType() {
		if (this.resolvedTargetType != null) {
			return this.resolvedTargetType;
		}
		return (this.targetType != null ? this.targetType.resolve() : null);
	}

	/**
	 * 指定引用非重载的方法的工厂方法名称.
	 */
	public void setUniqueFactoryMethodName(String name) {
		Assert.hasText(name, "Factory method name must not be empty");
		setFactoryMethodName(name);
		this.isFactoryMethodUnique = true;
	}

	/**
	 * 检查给定的候选者是否有资格作为工厂方法.
	 */
	public boolean isFactoryMethod(Method candidate) {
		return (candidate != null && candidate.getName().equals(getFactoryMethodName()));
	}

	/**
	 * 将已解析的工厂方法作为Java Method对象返回.
	 * 
	 * @return 工厂方法, 或{@code null}如果未找到或尚未解析
	 */
	public Method getResolvedFactoryMethod() {
		synchronized (this.constructorArgumentLock) {
			Object candidate = this.resolvedConstructorOrFactoryMethod;
			return (candidate instanceof Method ? (Method) candidate : null);
		}
	}

	public void registerExternallyManagedConfigMember(Member configMember) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedConfigMembers == null) {
				this.externallyManagedConfigMembers = new HashSet<Member>(1);
			}
			this.externallyManagedConfigMembers.add(configMember);
		}
	}

	public boolean isExternallyManagedConfigMember(Member configMember) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedConfigMembers != null &&
					this.externallyManagedConfigMembers.contains(configMember));
		}
	}

	public void registerExternallyManagedInitMethod(String initMethod) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedInitMethods == null) {
				this.externallyManagedInitMethods = new HashSet<String>(1);
			}
			this.externallyManagedInitMethods.add(initMethod);
		}
	}

	public boolean isExternallyManagedInitMethod(String initMethod) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedInitMethods != null &&
					this.externallyManagedInitMethods.contains(initMethod));
		}
	}

	public void registerExternallyManagedDestroyMethod(String destroyMethod) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedDestroyMethods == null) {
				this.externallyManagedDestroyMethods = new HashSet<String>(1);
			}
			this.externallyManagedDestroyMethods.add(destroyMethod);
		}
	}

	public boolean isExternallyManagedDestroyMethod(String destroyMethod) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedDestroyMethods != null &&
					this.externallyManagedDestroyMethods.contains(destroyMethod));
		}
	}


	@Override
	public RootBeanDefinition cloneBeanDefinition() {
		return new RootBeanDefinition(this);
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof RootBeanDefinition && super.equals(other)));
	}

	@Override
	public String toString() {
		return "Root bean: " + super.toString();
	}

}
