package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;

/**
 * 即将注入的特定依赖项的描述符.
 * 包装构造函数参数, 方法参数或字段, 允许统一访问其元数据.
 */
@SuppressWarnings("serial")
public class DependencyDescriptor extends InjectionPoint implements Serializable {

	private final Class<?> declaringClass;

	private String methodName;

	private Class<?>[] parameterTypes;

	private int parameterIndex;

	private String fieldName;

	private final boolean required;

	private final boolean eager;

	private int nestingLevel = 1;

	private Class<?> containingClass;

	private volatile ResolvableType resolvableType;


	/**
	 * 为方法或构造函数参数创建新描述符.
	 * 将依赖视为 '实时'的.
	 * 
	 * @param methodParameter 要包装的MethodParameter
	 * @param required 依赖是否必需
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
		this(methodParameter, required, true);
	}

	/**
	 * 为方法或构造函数参数创建新描述符.
	 * 
	 * @param methodParameter 要包装的MethodParameter
	 * @param required 依赖是否必需
	 * @param eager 在实时解析潜在目标bean用于类型匹配时, 这种依赖性是否“实时”
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
		super(methodParameter);
		this.declaringClass = methodParameter.getDeclaringClass();
		if (this.methodParameter.getMethod() != null) {
			this.methodName = methodParameter.getMethod().getName();
			this.parameterTypes = methodParameter.getMethod().getParameterTypes();
		}
		else {
			this.parameterTypes = methodParameter.getConstructor().getParameterTypes();
		}
		this.parameterIndex = methodParameter.getParameterIndex();
		this.containingClass = methodParameter.getContainingClass();
		this.required = required;
		this.eager = eager;
	}

	/**
	 * 为字段创建新描述符.
	 * 将依赖视为 '实时'的.
	 * 
	 * @param field 要包装的字段
	 * @param required 依赖是否必需
	 */
	public DependencyDescriptor(Field field, boolean required) {
		this(field, required, true);
	}

	/**
	 * 为字段创建新描述符.
	 * 
	 * @param field 要包装的字段
	 * @param required 依赖是否必需
	 * @param eager 在实时解析潜在目标bean用于类型匹配时, 这种依赖性是否“实时”
	 */
	public DependencyDescriptor(Field field, boolean required, boolean eager) {
		super(field);
		this.declaringClass = field.getDeclaringClass();
		this.fieldName = field.getName();
		this.required = required;
		this.eager = eager;
	}

	/**
	 * 克隆构造函数.
	 * 
	 * @param original 从中创建副本的原始描述符
	 */
	public DependencyDescriptor(DependencyDescriptor original) {
		super(original);
		this.declaringClass = original.declaringClass;
		this.methodName = original.methodName;
		this.parameterTypes = original.parameterTypes;
		this.parameterIndex = original.parameterIndex;
		this.fieldName = original.fieldName;
		this.containingClass = original.containingClass;
		this.required = original.required;
		this.eager = original.eager;
		this.nestingLevel = original.nestingLevel;
	}


	/**
	 * 依赖是否必需.
	 */
	public boolean isRequired() {
		return this.required;
	}

	/**
	 * 在实时解析潜在目标bean用于类型匹配时, 这种依赖性是否“实时”.
	 */
	public boolean isEager() {
		return this.eager;
	}

	/**
	 * 解析指定的非唯一场景: 默认抛出 {@link NoUniqueBeanDefinitionException}.
	 * <p>子类可以重写此选项以选择其中一个实例, 或者通过返回{@code null}来选择退出.
	 * 
	 * @param type 请求的bean类型
	 * @param matchingBeans 已为给定类型预先选择的bean名称和相应bean实例 (限定符等已经应用)
	 * 
	 * @return 要继续的bean实例, 或{@code null}
	 * @throws BeansException 如果不是唯一的场景是致命的
	 * @since 4.3
	 */
	public Object resolveNotUnique(Class<?> type, Map<String, Object> matchingBeans) throws BeansException {
		throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
	}

	/**
	 * 解决给定工厂的依赖关系的快捷方式, 例如, 考虑一些预先解决的信息.
	 * <p>在进入所有bean的常规类型匹配算法之前, 解析算法将首先尝试通过此方法解析快捷方式.
	 * 子类可以覆盖此方法, 以在仍然接收{@link InjectionPoint}等的同时, 提高基于预缓存的信息解析性能.
	 * 
	 * @param beanFactory 关联的工厂
	 * 
	 * @return 快捷方式的结果, 或{@code null}
	 * @throws BeansException 如果无法获得快捷方式
	 * @since 4.3.1
	 */
	public Object resolveShortcut(BeanFactory beanFactory) throws BeansException {
		return null;
	}

	/**
	 * 将指定的bean名称解析为给定工厂的bean实例, 作为此依赖项的匹配算法的候选结果.
	 * <p>默认实现调用 {@link BeanFactory#getBean(String)}. 子类可以提供其他参数或其他自定义.
	 * 
	 * @param beanName bean名称, 作为此依赖项的候选结果
	 * @param requiredType bean的预期类型 (作为一个断言)
	 * @param beanFactory 关联的工厂
	 * 
	 * @return bean实例 (never {@code null})
	 * @throws BeansException 如果无法获得bean
	 * @since 4.3.2
	 */
	public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory)
			throws BeansException {

		return beanFactory.getBean(beanName, requiredType);
	}


	/**
	 * 增加此描述符的嵌套级别.
	 */
	public void increaseNestingLevel() {
		this.nestingLevel++;
		this.resolvableType = null;
		if (this.methodParameter != null) {
			this.methodParameter.increaseNestingLevel();
		}
	}

	/**
	 * 设置包含此依赖项的具体类(可选).
	 * 这可能与声明参数/字段的类不同, 因为它可能是其子类, 可能会替换类型变量.
	 */
	public void setContainingClass(Class<?> containingClass) {
		this.containingClass = containingClass;
		this.resolvableType = null;
		if (this.methodParameter != null) {
			GenericTypeResolver.resolveParameterType(this.methodParameter, containingClass);
		}
	}

	/**
	 * 为包装的参数/字段构建ResolvableType对象.
	 * @since 4.0
	 */
	public ResolvableType getResolvableType() {
		ResolvableType resolvableType = this.resolvableType;
		if (resolvableType == null) {
			resolvableType = (this.field != null ?
					ResolvableType.forField(this.field, this.nestingLevel, this.containingClass) :
					ResolvableType.forMethodParameter(this.methodParameter));
			this.resolvableType = resolvableType;
		}
		return resolvableType;
	}

	/**
	 * 返回是否允许回退匹配.
	 * <p>默认是{@code false}, 但可能会被覆盖以返回{@code true},
	 * 以便建议 {@link org.springframework.beans.factory.support.AutowireCandidateResolver}, 它也接受回退匹配.
	 * @since 4.0
	 */
	public boolean fallbackMatchAllowed() {
		return false;
	}

	/**
	 * 返回此描述符的变体, 用于回退匹配.
	 * @since 4.0
	 */
	public DependencyDescriptor forFallbackMatch() {
		return new DependencyDescriptor(this) {
			@Override
			public boolean fallbackMatchAllowed() {
				return true;
			}
		};
	}

	/**
	 * 初始化基础方法参数的参数名称发现.
	 * <p>此方法实际上并不尝试在此时检索参数名称; 它只允许在应用程序调用{@link #getDependencyName()}时发生.
	 */
	public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
		if (this.methodParameter != null) {
			this.methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
		}
	}

	/**
	 * 确定包装参数/字段的名称.
	 * 
	 * @return 声明的名称 (never {@code null})
	 */
	public String getDependencyName() {
		return (this.field != null ? this.field.getName() : this.methodParameter.getParameterName());
	}

	/**
	 * 确定包装参数/字段的声明（非泛型）类型.
	 * 
	 * @return 声明的类型 (never {@code null})
	 */
	public Class<?> getDependencyType() {
		if (this.field != null) {
			if (this.nestingLevel > 1) {
				Type type = this.field.getGenericType();
				for (int i = 2; i <= this.nestingLevel; i++) {
					if (type instanceof ParameterizedType) {
						Type[] args = ((ParameterizedType) type).getActualTypeArguments();
						type = args[args.length - 1];
					}
					// TODO: Object.class if unresolvable
				}
				if (type instanceof Class) {
					return (Class<?>) type;
				}
				else if (type instanceof ParameterizedType) {
					Type arg = ((ParameterizedType) type).getRawType();
					if (arg instanceof Class) {
						return (Class<?>) arg;
					}
				}
				return Object.class;
			}
			else {
				return this.field.getType();
			}
		}
		else {
			return this.methodParameter.getNestedParameterType();
		}
	}

	/**
	 * 确定包装的Collection参数/字段的泛型元素类型.
	 * 
	 * @return 泛型类型, 或{@code null}
	 * @deprecated as of 4.3.6, in favor of direct {@link ResolvableType} usage
	 */
	@Deprecated
	public Class<?> getCollectionType() {
		return (this.field != null ?
				org.springframework.core.GenericCollectionTypeResolver.getCollectionFieldType(this.field, this.nestingLevel) :
				org.springframework.core.GenericCollectionTypeResolver.getCollectionParameterType(this.methodParameter));
	}

	/**
	 * 确定包装的Map参数/字段的泛型键类型.
	 * 
	 * @return 泛型类型, 或{@code null}
	 * @deprecated as of 4.3.6, in favor of direct {@link ResolvableType} usage
	 */
	@Deprecated
	public Class<?> getMapKeyType() {
		return (this.field != null ?
				org.springframework.core.GenericCollectionTypeResolver.getMapKeyFieldType(this.field, this.nestingLevel) :
				org.springframework.core.GenericCollectionTypeResolver.getMapKeyParameterType(this.methodParameter));
	}

	/**
	 * 确定包装的Map参数/字段的泛型值类型.
	 * 
	 * @return 泛型类型, 或{@code null}
	 * @deprecated as of 4.3.6, in favor of direct {@link ResolvableType} usage
	 */
	@Deprecated
	public Class<?> getMapValueType() {
		return (this.field != null ?
				org.springframework.core.GenericCollectionTypeResolver.getMapValueFieldType(this.field, this.nestingLevel) :
				org.springframework.core.GenericCollectionTypeResolver.getMapValueParameterType(this.methodParameter));
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		DependencyDescriptor otherDesc = (DependencyDescriptor) other;
		return (this.required == otherDesc.required && this.eager == otherDesc.eager &&
				this.nestingLevel == otherDesc.nestingLevel && this.containingClass == otherDesc.containingClass);
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Restore reflective handles (which are unfortunately not serializable)
		try {
			if (this.fieldName != null) {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			}
			else {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
				}
				else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
				for (int i = 1; i < this.nestingLevel; i++) {
					this.methodParameter.increaseNestingLevel();
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not find original class structure", ex);
		}
	}
}
