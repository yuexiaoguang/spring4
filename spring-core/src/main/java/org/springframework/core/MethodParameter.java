package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper类, 它封装了方法参数的规范,
 * i.e. {@link Method}或{@link Constructor}加上参数索引和声明的泛型类型的嵌套类型索引.
 * 用于传递的规范对象.
 *
 * <p>从4.2开始, 有一个{@link org.springframework.core.annotation.SynthesizingMethodParameter}子类,
 * 可用于合成带有属性别名的注解.
 * 该子类特别用于Web和消息端点处理.
 */
public class MethodParameter {

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	private static final Class<?> javaUtilOptionalClass;

	static {
		Class<?> clazz;
		try {
			clazz = ClassUtils.forName("java.util.Optional", MethodParameter.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// Java 8 not available - Optional references simply not supported then.
			clazz = null;
		}
		javaUtilOptionalClass = clazz;
	}


	private final Method method;

	private final Constructor<?> constructor;

	private final int parameterIndex;

	private int nestingLevel = 1;

	/** Map from Integer level to Integer type index */
	Map<Integer, Integer> typeIndexesPerLevel;

	private volatile Class<?> containingClass;

	private volatile Class<?> parameterType;

	private volatile Type genericParameterType;

	private volatile Annotation[] parameterAnnotations;

	private volatile ParameterNameDiscoverer parameterNameDiscoverer;

	private volatile String parameterName;

	private volatile MethodParameter nestedMethodParameter;


	/**
	 * 为给定方法创建一个新的{@code MethodParameter}, 嵌套级别为1.
	 * 
	 * @param method 要指定参数的方法
	 * @param parameterIndex 参数的索引:
	 * -1 对应方法返回类型;
	 * 0 对应第一个方法参数;
	 * 1 对应第二个方法参数, etc.
	 */
	public MethodParameter(Method method, int parameterIndex) {
		this(method, parameterIndex, 1);
	}

	/**
	 * 为给定方法创建一个新的{@code MethodParameter}.
	 * 
	 * @param method 要指定参数的方法
	 * @param parameterIndex 参数的索引:
	 * -1 对应方法返回类型;
	 * 0 对应第一个方法参数;
	 * 1 对应第二个方法参数, etc.
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List 中嵌套List的情况下, 1表示嵌套的List, 而2表示嵌套List的元素)
	 */
	public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
		Assert.notNull(method, "Method must not be null");
		this.method = method;
		this.parameterIndex = parameterIndex;
		this.nestingLevel = nestingLevel;
		this.constructor = null;
	}

	/**
	 * 为给定的构造函数创建一个新的MethodParameter, 嵌套级别为1.
	 * 
	 * @param constructor 要指定参数的构造函数
	 * @param parameterIndex 参数的索引
	 */
	public MethodParameter(Constructor<?> constructor, int parameterIndex) {
		this(constructor, parameterIndex, 1);
	}

	/**
	 * 为给定的构造函数创建一个新的MethodParameter.
	 * 
	 * @param constructor 要指定参数的构造函数
	 * @param parameterIndex 参数的索引
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List 中嵌套List的情况下, 1表示嵌套的List, 而2表示嵌套List的元素)
	 */
	public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
		Assert.notNull(constructor, "Constructor must not be null");
		this.constructor = constructor;
		this.parameterIndex = parameterIndex;
		this.nestingLevel = nestingLevel;
		this.method = null;
	}

	/**
	 * 复制构造函数, 根据原始对象所在的相同元数据和缓存状态生成独立的MethodParameter对象.
	 * 
	 * @param original 要从中复制的原始MethodParameter对象
	 */
	public MethodParameter(MethodParameter original) {
		Assert.notNull(original, "Original must not be null");
		this.method = original.method;
		this.constructor = original.constructor;
		this.parameterIndex = original.parameterIndex;
		this.nestingLevel = original.nestingLevel;
		this.typeIndexesPerLevel = original.typeIndexesPerLevel;
		this.containingClass = original.containingClass;
		this.parameterType = original.parameterType;
		this.genericParameterType = original.genericParameterType;
		this.parameterAnnotations = original.parameterAnnotations;
		this.parameterNameDiscoverer = original.parameterNameDiscoverer;
		this.parameterName = original.parameterName;
	}


	/**
	 * 返回包装的方法.
	 * <p>Note: 方法或构造函数都可用.
	 * 
	 * @return Method, 或{@code null}
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * 返回包装的Constructor.
	 * <p>Note: 方法或构造函数都可用.
	 * 
	 * @return Constructor, 或{@code null}
	 */
	public Constructor<?> getConstructor() {
		return this.constructor;
	}

	/**
	 * 返回声明底层Method或Constructor的类.
	 */
	public Class<?> getDeclaringClass() {
		return getMember().getDeclaringClass();
	}

	/**
	 * 返回包装的成员.
	 * 
	 * @return Method或Constructor 作为 Member
	 */
	public Member getMember() {
		// NOTE: 即使使用JDK 8编译器, 也没有三元表达式来保持JDK < 8的兼容性 (可能选择java.lang.reflect.Executable作为通用类型, 旧的JDK上没有新的基类)
		if (this.method != null) {
			return this.method;
		}
		else {
			return this.constructor;
		}
	}

	/**
	 * 返回包装的带注解的元素.
	 * <p>Note: 此方法公开在方法/构造函数本身上声明的注解 (i.e. 在方法/构造函数级别, 而不是在参数级别).
	 * 
	 * @return Method或Constructor 作为AnnotatedElement
	 */
	public AnnotatedElement getAnnotatedElement() {
		// NOTE: 即使使用JDK 8编译器, 也没有三元表达式来保持JDK < 8的兼容性 (可能选择java.lang.reflect.Executable作为通用类型, 旧的JDK上没有新的基类)
		if (this.method != null) {
			return this.method;
		}
		else {
			return this.constructor;
		}
	}

	/**
	 * 返回方法/构造函数参数的索引.
	 * 
	 * @return 参数的索引(-1表示返回类型)
	 */
	public int getParameterIndex() {
		return this.parameterIndex;
	}

	/**
	 * 增加此参数的嵌套级别.
	 */
	public void increaseNestingLevel() {
		this.nestingLevel++;
	}

	/**
	 * 降低此参数的嵌套级别.
	 */
	public void decreaseNestingLevel() {
		getTypeIndexesPerLevel().remove(this.nestingLevel);
		this.nestingLevel--;
	}

	/**
	 * 返回目标类型的嵌套级别
	 * (通常为 1; e.g. 在List 中嵌套List的情况下, 1表示嵌套的List, 而2表示嵌套List的元素).
	 */
	public int getNestingLevel() {
		return this.nestingLevel;
	}

	/**
	 * 设置当前嵌套级别的类型索引.
	 * 
	 * @param typeIndex 相应的类型索引 (或{@code null}为默认类型索引)
	 */
	public void setTypeIndexForCurrentLevel(int typeIndex) {
		getTypeIndexesPerLevel().put(this.nestingLevel, typeIndex);
	}

	/**
	 * 返回当前嵌套级别的类型索引.
	 * 
	 * @return 相应的类型索引, 或{@code null}如果没有指定 (表示默认类型索引)
	 */
	public Integer getTypeIndexForCurrentLevel() {
		return getTypeIndexForLevel(this.nestingLevel);
	}

	/**
	 * 返回指定嵌套级别的类型索引.
	 * 
	 * @param nestingLevel 要检查的嵌套级别
	 * 
	 * @return 相应的类型索引, 或{@code null}如果没有指定 (表示默认类型索引)
	 */
	public Integer getTypeIndexForLevel(int nestingLevel) {
		return getTypeIndexesPerLevel().get(nestingLevel);
	}

	/**
	 * 获取(延迟构造的) type-indexes-per-level Map.
	 */
	private Map<Integer, Integer> getTypeIndexesPerLevel() {
		if (this.typeIndexesPerLevel == null) {
			this.typeIndexesPerLevel = new HashMap<Integer, Integer>(4);
		}
		return this.typeIndexesPerLevel;
	}

	/**
	 * 返回此{@code MethodParameter}的变体, 它指向相同的参数, 但更深一个嵌套级别.
	 * 这实际上与{@link #increaseNestingLevel()}相同, 只是使用独立的{@code MethodParameter}对象 (e.g. 在原始文件被缓存的情况下).
	 */
	public MethodParameter nested() {
		if (this.nestedMethodParameter != null) {
			return this.nestedMethodParameter;
		}
		MethodParameter nestedParam = clone();
		nestedParam.nestingLevel = this.nestingLevel + 1;
		this.nestedMethodParameter = nestedParam;
		return nestedParam;
	}

	/**
	 * 返回此方法参数是否以Java 8的{@link java.util.Optional}形式声明为可选.
	 */
	public boolean isOptional() {
		return (getParameterType() == javaUtilOptionalClass);
	}

	/**
	 * 返回此{@code MethodParameter}的变体, 它指向相同的参数, 但在{@link java.util.Optional}声明的情况下更深一个嵌套级别.
	 */
	public MethodParameter nestedIfOptional() {
		return (isOptional() ? nested() : this);
	}

	/**
	 * 设置包含类以解析参数类型.
	 */
	void setContainingClass(Class<?> containingClass) {
		this.containingClass = containingClass;
	}

	public Class<?> getContainingClass() {
		return (this.containingClass != null ? this.containingClass : getDeclaringClass());
	}

	/**
	 * 设置已解析的(泛型)参数类型.
	 */
	void setParameterType(Class<?> parameterType) {
		this.parameterType = parameterType;
	}

	/**
	 * 返回方法/构造函数参数的类型.
	 * 
	 * @return 参数的类型 (never {@code null})
	 */
	public Class<?> getParameterType() {
		Class<?> paramType = this.parameterType;
		if (paramType == null) {
			if (this.parameterIndex < 0) {
				Method method = getMethod();
				paramType = (method != null ? method.getReturnType() : void.class);
			}
			else {
				paramType = (this.method != null ?
						this.method.getParameterTypes()[this.parameterIndex] :
						this.constructor.getParameterTypes()[this.parameterIndex]);
			}
			this.parameterType = paramType;
		}
		return paramType;
	}

	/**
	 * 返回方法/构造函数参数的泛型类型.
	 * 
	 * @return 参数的类型(never {@code null})
	 */
	public Type getGenericParameterType() {
		Type paramType = this.genericParameterType;
		if (paramType == null) {
			if (this.parameterIndex < 0) {
				Method method = getMethod();
				paramType = (method != null ? method.getGenericReturnType() : void.class);
			}
			else {
				Type[] genericParameterTypes = (this.method != null ?
						this.method.getGenericParameterTypes() : this.constructor.getGenericParameterTypes());
				int index = this.parameterIndex;
				if (this.constructor != null && this.constructor.getDeclaringClass().isMemberClass() &&
						!Modifier.isStatic(this.constructor.getDeclaringClass().getModifiers()) &&
						genericParameterTypes.length == this.constructor.getParameterTypes().length - 1) {
					// Bug in javac: type数组排除包含至少一个泛型构造函数参数的内部类的实例参数,
					// 所以访问它, 实际参数索引减1
					index = this.parameterIndex - 1;
				}
				paramType = (index >= 0 && index < genericParameterTypes.length ?
						genericParameterTypes[index] : getParameterType());
			}
			this.genericParameterType = paramType;
		}
		return paramType;
	}

	/**
	 * 返回方法/构造函数参数的嵌套类型.
	 * 
	 * @return 参数的类型(never {@code null})
	 */
	public Class<?> getNestedParameterType() {
		if (this.nestingLevel > 1) {
			Type type = getGenericParameterType();
			for (int i = 2; i <= this.nestingLevel; i++) {
				if (type instanceof ParameterizedType) {
					Type[] args = ((ParameterizedType) type).getActualTypeArguments();
					Integer index = getTypeIndexForLevel(i);
					type = args[index != null ? index : args.length - 1];
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
			return getParameterType();
		}
	}

	/**
	 * 返回方法/构造函数参数的嵌套泛型类型.
	 * 
	 * @return 参数的类型 (never {@code null})
	 */
	public Type getNestedGenericParameterType() {
		if (this.nestingLevel > 1) {
			Type type = getGenericParameterType();
			for (int i = 2; i <= this.nestingLevel; i++) {
				if (type instanceof ParameterizedType) {
					Type[] args = ((ParameterizedType) type).getActualTypeArguments();
					Integer index = getTypeIndexForLevel(i);
					type = args[index != null ? index : args.length - 1];
				}
			}
			return type;
		}
		else {
			return getGenericParameterType();
		}
	}

	/**
	 * 返回与目标方法/构造函数本身关联的注解.
	 */
	public Annotation[] getMethodAnnotations() {
		return adaptAnnotationArray(getAnnotatedElement().getAnnotations());
	}

	/**
	 * 返回给定类型的方法/构造函数注解.
	 * 
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 注解对象, 或{@code null}
	 */
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return adaptAnnotation(getAnnotatedElement().getAnnotation(annotationType));
	}

	/**
	 * 返回方法/构造函数是否带有给定类型的注解.
	 * 
	 * @param annotationType 要查找的注解类型
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return getAnnotatedElement().isAnnotationPresent(annotationType);
	}

	/**
	 * 返回与特定方法/构造函数参数关联的注解.
	 */
	public Annotation[] getParameterAnnotations() {
		Annotation[] paramAnns = this.parameterAnnotations;
		if (paramAnns == null) {
			Annotation[][] annotationArray = (this.method != null ?
					this.method.getParameterAnnotations() : this.constructor.getParameterAnnotations());
			int index = this.parameterIndex;
			if (this.constructor != null && this.constructor.getDeclaringClass().isMemberClass() &&
					!Modifier.isStatic(this.constructor.getDeclaringClass().getModifiers()) &&
					annotationArray.length == this.constructor.getParameterTypes().length - 1) {
				// Bug in javac in JDK <9:
				// 注解数组排除了内部类的封闭实例参数, 因此使用减1的实际参数索引访问它
				index = this.parameterIndex - 1;
			}
			paramAnns = (index >= 0 && index < annotationArray.length ?
					adaptAnnotationArray(annotationArray[index]) : EMPTY_ANNOTATION_ARRAY);
			this.parameterAnnotations = paramAnns;
		}
		return paramAnns;
	}

	/**
	 * 如果参数至少有一个注解, 则返回{@code true}; 如果没有, 则返回{@code false}.
	 */
	public boolean hasParameterAnnotations() {
		return (getParameterAnnotations().length != 0);
	}

	/**
	 * 返回给定类型的参数注解.
	 * 
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 注解对象, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
		Annotation[] anns = getParameterAnnotations();
		for (Annotation ann : anns) {
			if (annotationType.isInstance(ann)) {
				return (A) ann;
			}
		}
		return null;
	}

	/**
	 * 返回是否使用给定的注解类型声明参数.
	 * 
	 * @param annotationType 要查找的注解类型
	 */
	public <A extends Annotation> boolean hasParameterAnnotation(Class<A> annotationType) {
		return (getParameterAnnotation(annotationType) != null);
	}

	/**
	 * 初始化此方法参数的参数名称发现.
	 * <p>此方法实际上并不尝试在此时检索参数名称; 它只允许在应用程序调用{@link #getParameterName()}时发现 (如果有的话).
	 */
	public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 返回方法/构造函数参数的名称.
	 * 
	 * @return 参数的名称
	 * (可能是{@code null}, 如果类文件中不包含参数名称元数据, 或者没有{@link #initParameterNameDiscovery ParameterNameDiscoverer}被设置为开头)
	 */
	public String getParameterName() {
		ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
		if (discoverer != null) {
			String[] parameterNames = (this.method != null ?
					discoverer.getParameterNames(this.method) : discoverer.getParameterNames(this.constructor));
			if (parameterNames != null) {
				this.parameterName = parameterNames[this.parameterIndex];
			}
			this.parameterNameDiscoverer = null;
		}
		return this.parameterName;
	}


	/**
	 * 模板方法, 用于在将给定的注解实例返回给调用者之前对其进行后处理.
	 * <p>默认实现只是按原样返回给定的注解.
	 * 
	 * @param annotation 要返回的注解
	 * 
	 * @return 后处理的注解 (或原始的注解)
	 */
	protected <A extends Annotation> A adaptAnnotation(A annotation) {
		return annotation;
	}

	/**
	 * 模板方法, 用于在将给定的注解实例返回给调用者之前对其进行后处理.
	 * <p>默认实现只是按原样返回给定的注解.
	 * 
	 * @param annotations 要返回的注解
	 * 
	 * @return 后处理的注解 (或原始的注解)
	 */
	protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
		return annotations;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodParameter)) {
			return false;
		}
		MethodParameter otherParam = (MethodParameter) other;
		return (this.parameterIndex == otherParam.parameterIndex && getMember().equals(otherParam.getMember()));
	}

	@Override
	public int hashCode() {
		return (getMember().hashCode() * 31 + this.parameterIndex);
	}

	@Override
	public String toString() {
		return (this.method != null ? "method '" + this.method.getName() + "'" : "constructor") +
				" parameter " + this.parameterIndex;
	}

	@Override
	public MethodParameter clone() {
		return new MethodParameter(this);
	}


	/**
	 * 为给定的方法或构造函数创建一个新的MethodParameter.
	 * <p>对于以泛型方式处理Method或Constructor引用的场景, 这是一个便利的构造函数.
	 * 
	 * @param methodOrConstructor 要指定参数的Method或Constructor
	 * @param parameterIndex 参数的索引
	 * 
	 * @return 相应的MethodParameter实例
	 */
	public static MethodParameter forMethodOrConstructor(Object methodOrConstructor, int parameterIndex) {
		if (methodOrConstructor instanceof Method) {
			return new MethodParameter((Method) methodOrConstructor, parameterIndex);
		}
		else if (methodOrConstructor instanceof Constructor) {
			return new MethodParameter((Constructor<?>) methodOrConstructor, parameterIndex);
		}
		else {
			throw new IllegalArgumentException(
					"Given object [" + methodOrConstructor + "] is neither a Method nor a Constructor");
		}
	}

}
