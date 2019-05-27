package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 用于处理注解, 处理元注解, 桥接方法 (编译器为泛型声明生成), 以及超级方法 (用于可选的<em>注解继承</em>)的常规实用方法.
 *
 * <p>请注意, JDK的内省工具本身不提供此类的大多数功能.
 *
 * <p>作为运行时保留注释的一般规则 (e.g. 用于事务控制, 授权, 或服务公开),
 * 始终使用此类上的查找方法
 * (e.g., {@link #findAnnotation(Method, Class)}, {@link #getAnnotation(Method, Class)}, {@link #getAnnotations(Method)})
 * 而不是JDK中的纯注解查找方法.
 * 仍然可以在给定的类级别上明确选择<em>get</em>查找 ({@link #getAnnotation(Method, Class)}),
 * 和在给定方法的整个继承层次结构中<em>find</em>查找 ({@link #findAnnotation(Method, Class)}).
 *
 * <h3>术语</h3>
 * 术语<em>直接存在</em>, <em>间接存在</em>, 和<em>存在</em>与{@link AnnotatedElement}的类级别javadoc中定义的含义相同(in Java 8).
 *
 * <p>如果注解被声明为元素上<em>存在</em>的其他注解上的元注解, 则注解在元素上是<em>元存在</em>.
 * 如果{@code A}是其它注解上的 <em>直接存在</em>或<em>元存在</em>, 则注解{@code A}在另一个注解上为<em>元存在</em>.
 *
 * <h3>元注解支持</h3>
 * <p>此类中的大多数{@code find*()}方法和一些{@code get*()}方法都支持查找用作元注解的注解.
 * 有关详细信息, 请参阅此类中每个方法的javadoc.
 * 要在<em>组合注解</em>中使用<em>属性覆盖</em>进行元注解的细粒度支持, 请考虑使用{@link AnnotatedElementUtils}更具体的方法.
 *
 * <h3>属性别名</h3>
 * <p>此类中返回注解, 注解数组或{@link AnnotationAttributes}的所有公共方法都透明地支持通过{@link AliasFor @AliasFor}配置的属性别名.
 * 有关详细信息, 请参阅各种{@code synthesizeAnnotation*(..)}方法.
 *
 * <h3>搜索范围</h3>
 * <p>一旦找到指定类型的第一个注解, 此类中的方法使用的搜索算法将停止搜索注解.
 * 因此, 将默默忽略指定类型的其他注解.
 */
public abstract class AnnotationUtils {

	/**
	 * 带有单个元素的注解的属性名称.
	 */
	public static final String VALUE = "value";

	private static final String REPEATABLE_CLASS_NAME = "java.lang.annotation.Repeatable";

	private static final Map<AnnotationCacheKey, Annotation> findAnnotationCache =
			new ConcurrentReferenceHashMap<AnnotationCacheKey, Annotation>(256);

	private static final Map<AnnotationCacheKey, Boolean> metaPresentCache =
			new ConcurrentReferenceHashMap<AnnotationCacheKey, Boolean>(256);

	private static final Map<Class<?>, Boolean> annotatedInterfaceCache =
			new ConcurrentReferenceHashMap<Class<?>, Boolean>(256);

	private static final Map<Class<? extends Annotation>, Boolean> synthesizableCache =
			new ConcurrentReferenceHashMap<Class<? extends Annotation>, Boolean>(256);

	private static final Map<Class<? extends Annotation>, Map<String, List<String>>> attributeAliasesCache =
			new ConcurrentReferenceHashMap<Class<? extends Annotation>, Map<String, List<String>>>(256);

	private static final Map<Class<? extends Annotation>, List<Method>> attributeMethodsCache =
			new ConcurrentReferenceHashMap<Class<? extends Annotation>, List<Method>>(256);

	private static final Map<Method, AliasDescriptor> aliasDescriptorCache =
			new ConcurrentReferenceHashMap<Method, AliasDescriptor>(256);

	private static transient Log logger;


	/**
	 * 从提供的注解中获取{@code annotationType}的{@link Annotation}:
	 * 给定注解本身或其直接元注解.
	 * <p>请注意, 此方法仅支持单级元注解.
	 * 要支持任意级别的元注解, 请使用{@code find*()}方法之一.
	 * 
	 * @param annotation 要检查的注解
	 * @param annotationType 要在本地和作为元注解查找的注解类型
	 * 
	 * @return 第一个匹配的注解, 或{@code null}如果没有找到
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A getAnnotation(Annotation annotation, Class<A> annotationType) {
		if (annotationType.isInstance(annotation)) {
			return synthesizeAnnotation((A) annotation);
		}
		Class<? extends Annotation> annotatedElement = annotation.annotationType();
		try {
			return synthesizeAnnotation(annotatedElement.getAnnotation(annotationType), annotatedElement);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotatedElement, ex);
			return null;
		}
	}

	/**
	 * 从提供的{@link AnnotatedElement}中获取{@code annotationType}的单个{@link Annotation},
	 * 其中注解在{@code AnnotatedElement}上<em>存在</em>或<em>元存在</em>.
	 * <p>请注意, 此方法仅支持单级元注解.
	 * 要支持任意级别的元注解, 请使用{@link #findAnnotation(AnnotatedElement, Class)}代替.
	 * 
	 * @param annotatedElement 从中获取注解的{@code AnnotatedElement}
	 * @param annotationType 要在本地和作为元注解查找的注解类型
	 * 
	 * @return 第一个匹配的注解, 或{@code null}如果没有找到
	 */
	public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		try {
			A annotation = annotatedElement.getAnnotation(annotationType);
			if (annotation == null) {
				for (Annotation metaAnn : annotatedElement.getAnnotations()) {
					annotation = metaAnn.annotationType().getAnnotation(annotationType);
					if (annotation != null) {
						break;
					}
				}
			}
			return synthesizeAnnotation(annotation, annotatedElement);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotatedElement, ex);
			return null;
		}
	}

	/**
	 * 从提供的{@link Method}中获取{@code annotationType}的单个{@link Annotation},
	 * 其中注解在方法上<em>存在</em>或<em>元存在</em>.
	 * <p>正确处理编译器生成的桥接{@link Method Methods}.
	 * <p>请注意, 此方法仅支持单级元注解.
	 * 要支持任意级别的元注解, 请使用{@link #findAnnotation(Method, Class)}代替.
	 * 
	 * @param method 要寻找其注解的方法
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 第一个匹配的注解, 或{@code null}如果没有找到
	 */
	public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
		Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
		return getAnnotation((AnnotatedElement) resolvedMethod, annotationType);
	}

	/**
	 * 获取提供的{@link AnnotatedElement}上<em>存在</em>的所有{@link Annotation Annotations}.
	 * <p><em>不会</em>搜索元注解.
	 * 
	 * @param annotatedElement 从中检索注解的Method, Constructor, Field
	 * 
	 * @return 找到的注解, 空数组, 或{@code null}如果不可解析
	 * (e.g. 因为注解属性中的嵌套的Class值无法在运行时解析)
	 */
	public static Annotation[] getAnnotations(AnnotatedElement annotatedElement) {
		try {
			return synthesizeAnnotationArray(annotatedElement.getAnnotations(), annotatedElement);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotatedElement, ex);
			return null;
		}
	}

	/**
	 * 获取提供的{@link Method}上<em>存在</em>的所有{@link Annotation Annotations}.
	 * <p>正确处理编译器生成的桥接{@link Method Methods}.
	 * <p><em>不会</em>搜索元注解.
	 * 
	 * @param method 从中检索注解的Method
	 * 
	 * @return 找到的注解, 空数组, 或{@code null}如果不可解析
	 * (e.g. 因为注解属性中的嵌套的Class值无法在运行时解析)
	 */
	public static Annotation[] getAnnotations(Method method) {
		try {
			return synthesizeAnnotationArray(BridgeMethodResolver.findBridgedMethod(method).getAnnotations(), method);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(method, ex);
			return null;
		}
	}

	/**
	 * 委托给{@link #getRepeatableAnnotations(AnnotatedElement, Class, Class)}.
	 * 
	 * @deprecated As of Spring Framework 4.2, use {@code getRepeatableAnnotations()}
	 * or {@code getDeclaredRepeatableAnnotations()} instead.
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getRepeatableAnnotation(Method method,
			Class<? extends Annotation> containerAnnotationType, Class<A> annotationType) {

		return getRepeatableAnnotations(method, annotationType, containerAnnotationType);
	}

	/**
	 * 委托给{@link #getRepeatableAnnotations(AnnotatedElement, Class, Class)}.
	 * 
	 * @deprecated As of Spring Framework 4.2, use {@code getRepeatableAnnotations()}
	 * or {@code getDeclaredRepeatableAnnotations()} instead.
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getRepeatableAnnotation(AnnotatedElement annotatedElement,
			Class<? extends Annotation> containerAnnotationType, Class<A> annotationType) {

		return getRepeatableAnnotations(annotatedElement, annotationType, containerAnnotationType);
	}

	/**
	 * 从提供的{@link AnnotatedElement}获取{@code annotationType}的<em>可重复</em> {@linkplain Annotation annotations},
	 * 这些注解在元素上<em>存在</em>, <em>间接存在</em>, 或<em>元存在</em>.
	 * <p>此方法模仿Java 8的{@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)}的功能,
	 * 支持自动检测通过@{@link java.lang.annotation.Repeatable}(在Java 8或更高版本上运行时)声明的<em>容器注解</em>, 并支持元注解.
	 * <p>处理单个注解和嵌套在<em>容器注解</em>中的注解.
	 * <p>如果提供的元素是{@link Method}, 则正确处理编译器生成的<em>桥接方法</em>.
	 * <p>如果注解在提供的元素上不<em>存在</em>, 则将搜索元注解.
	 * 
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 找到的注解或空集合 (never {@code null})
	 */
	public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType) {

		return getRepeatableAnnotations(annotatedElement, annotationType, null);
	}

	/**
	 * 从提供的{@link AnnotatedElement}获取{@code annotationType}的<em>可重复</em> {@linkplain Annotation annotations},
	 * 这些注解在元素上<em>存在</em>, <em>间接存在</em>, 或<em>元存在</em>.
	 * <p>此方法模仿Java 8的{@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)}的功能,
	 * 并支持元注解.
	 * <p>处理单个注解和嵌套在<em>容器注解</em>中的注解.
	 * <p>如果提供的元素是{@link Method}, 则正确处理编译器生成的<em>桥接方法</em>.
	 * <p>如果注解在提供的元素上不<em>存在</em>, 则将搜索元注解.
	 * 
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param containerAnnotationType 包含注解的容器的类型;
	 * 如果不支持容器, 或者在Java 8或更高版本上运行时应通过@{@link java.lang.annotation.Repeatable}查找, 则可能是{@code null}
	 * 
	 * @return 找到的注解或空集合 (never {@code null})
	 */
	public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, Class<? extends Annotation> containerAnnotationType) {

		Set<A> annotations = getDeclaredRepeatableAnnotations(annotatedElement, annotationType, containerAnnotationType);
		if (!annotations.isEmpty()) {
			return annotations;
		}

		if (annotatedElement instanceof Class) {
			Class<?> superclass = ((Class<?>) annotatedElement).getSuperclass();
			if (superclass != null && Object.class != superclass) {
				return getRepeatableAnnotations(superclass, annotationType, containerAnnotationType);
			}
		}

		return getRepeatableAnnotations(annotatedElement, annotationType, containerAnnotationType, false);
	}

	/**
	 * 从提供的{@link AnnotatedElement}获取{@code annotationType}的声明的<em>可重复</em>的{@linkplain Annotation annotations},
	 * 这些注解在元素上<em>直接存在</em>, <em>间接存在</em>, 或<em>元存在</em>.
	 * <p>此方法模仿Java 8的{@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)}的功能,
	 * 支持自动检测通过@{@link java.lang.annotation.Repeatable}(在Java 8或更高版本上运行时)声明的<em>容器注解</em>, 并支持元注解.
	 * <p>处理单个注解和嵌套在<em>容器注解</em>中的注解.
	 * <p>如果提供的元素是{@link Method}, 则正确处理编译器生成的<em>桥接方法</em>.
	 * <p>如果注解在提供的元素上不<em>存在</em>, 则将搜索元注解.
	 * 
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 找到的注解或空集合 (never {@code null})
	 */
	public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType) {

		return getDeclaredRepeatableAnnotations(annotatedElement, annotationType, null);
	}

	/**
	 * 从提供的{@link AnnotatedElement}获取{@code annotationType}的声明的<em>可重复</em>的{@linkplain Annotation annotations},
	 * 这些注解在元素上<em>直接存在</em>, <em>间接存在</em>, 或<em>元存在</em>.
	 * <p>此方法模仿Java 8的{@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)}的功能,
	 * 并支持元注解.
	 * <p>处理单个注解和嵌套在<em>容器注解</em>中的注解.
	 * <p>如果提供的元素是{@link Method}, 则正确处理编译器生成的<em>桥接方法</em>.
	 * <p>如果注解在提供的元素上不<em>存在</em>, 则将搜索元注解.
	 * 
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param containerAnnotationType 包含注解的容器的类型;
	 * 如果不支持容器, 或者在Java 8或更高版本上运行时应通过@{@link java.lang.annotation.Repeatable}查找, 则可能是{@code null}
	 * 
	 * @return 找到的注解或空集合 (never {@code null})
	 */
	public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, Class<? extends Annotation> containerAnnotationType) {

		return getRepeatableAnnotations(annotatedElement, annotationType, containerAnnotationType, true);
	}

	/**
	 * 执行{@link #getRepeatableAnnotations(AnnotatedElement, Class, Class)}
	 * 和{@link #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)}的实际工作.
	 * <p>如果提供的元素是{@link Method}, 则正确处理编译器生成的<em>桥接方法</em>.
	 * <p>如果注解在提供的元素上不<em>存在</em>, 则将搜索元注解.
	 * 
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param containerAnnotationType 包含注解的容器的类型;
	 * 如果不支持容器, 或者在Java 8或更高版本上运行时应通过@{@link java.lang.annotation.Repeatable}查找, 则可能是{@code null}
	 * @param declaredMode {@code true} 如果只考虑声明的注解 (i.e., 直接或间接存在)
	 * 
	 * @return 找到的注解或空集合 (never {@code null})
	 */
	private static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, Class<? extends Annotation> containerAnnotationType, boolean declaredMode) {

		Assert.notNull(annotatedElement, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "Annotation type must not be null");

		try {
			if (annotatedElement instanceof Method) {
				annotatedElement = BridgeMethodResolver.findBridgedMethod((Method) annotatedElement);
			}
			return new AnnotationCollector<A>(annotationType, containerAnnotationType, declaredMode).getResult(annotatedElement);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotatedElement, ex);
			return Collections.emptySet();
		}
	}

	/**
	 * 在提供的{@link AnnotatedElement}上查找{@code annotationType}的单个{@link Annotation}.
	 * <p>如果注解在提供的元素上不<em>直接存在</em>, 则将搜索元注解.
	 * <p><strong>Warning</strong>: 此方法通常在带注解的元素上运行.
	 * 换句话说, 此方法不会为类或方法执行专门的搜索算法.
	 * 如果需要{@link #findAnnotation(Class, Class)}或{@link #findAnnotation(Method, Class)}的更具体的语义, 请调用其中一种方法.
	 * 
	 * @param annotatedElement 要在其上查找注解的{@code AnnotatedElement}
	 * @param annotationType 要在本地和作为元注解查找的注解类型
	 * 
	 * @return 第一个匹配的注解, 或{@code null}
	 */
	public static <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		Assert.notNull(annotatedElement, "AnnotatedElement must not be null");
		if (annotationType == null) {
			return null;
		}

		// 不要将结果存储在findAnnotationCache中, 因为这样做会破坏 findAnnotation(Class, Class) 和 findAnnotation(Method, Class).
		A ann = findAnnotation(annotatedElement, annotationType, new HashSet<Annotation>());
		return synthesizeAnnotation(ann, annotatedElement);
	}

	/**
	 * 执行{@link #findAnnotation(AnnotatedElement, Class)}的搜索算法, 通过跟踪哪些注解已经<em>访问</em>来避免无限递归.
	 * 
	 * @param annotatedElement 要在其上查找注解的{@code AnnotatedElement}
	 * @param annotationType 要在本地和作为元注解查找的注解类型
	 * @param visited 已经访问过的注解
	 * 
	 * @return 第一个匹配的注解, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A findAnnotation(
			AnnotatedElement annotatedElement, Class<A> annotationType, Set<Annotation> visited) {
		try {
			Annotation[] anns = annotatedElement.getDeclaredAnnotations();
			for (Annotation ann : anns) {
				if (ann.annotationType() == annotationType) {
					return (A) ann;
				}
			}
			for (Annotation ann : anns) {
				if (!isInJavaLangAnnotationPackage(ann) && visited.add(ann)) {
					A annotation = findAnnotation((AnnotatedElement) ann.annotationType(), annotationType, visited);
					if (annotation != null) {
						return annotation;
					}
				}
			}
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotatedElement, ex);
		}
		return null;
	}

	/**
	 * 在提供的{@link Method}上查找{@code annotationType}的单个{@link Annotation},
	 * 如果注解不是在给定的方法上<em>直接存在</em>, 则遍历其超类方法(i.e., 从超类和接口).
	 * <p>正确处理编译器生成的桥接{@link Method Methods}.
	 * <p>如果注解不是在方法上<em>直接存在</em>, 则将搜索元注解.
	 * <p>默认情况下, 方法的注解不会继承, 因此需要显式处理.
	 * 
	 * @param method 要查找注解的方法
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 第一个匹配的注解, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
		Assert.notNull(method, "Method must not be null");
		if (annotationType == null) {
			return null;
		}

		AnnotationCacheKey cacheKey = new AnnotationCacheKey(method, annotationType);
		A result = (A) findAnnotationCache.get(cacheKey);

		if (result == null) {
			Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
			result = findAnnotation((AnnotatedElement) resolvedMethod, annotationType);
			if (result == null) {
				result = searchOnInterfaces(method, annotationType, method.getDeclaringClass().getInterfaces());
			}

			Class<?> clazz = method.getDeclaringClass();
			while (result == null) {
				clazz = clazz.getSuperclass();
				if (clazz == null || Object.class == clazz) {
					break;
				}
				try {
					Method equivalentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
					Method resolvedEquivalentMethod = BridgeMethodResolver.findBridgedMethod(equivalentMethod);
					result = findAnnotation((AnnotatedElement) resolvedEquivalentMethod, annotationType);
				}
				catch (NoSuchMethodException ex) {
					// No equivalent method found
				}
				if (result == null) {
					result = searchOnInterfaces(method, annotationType, clazz.getInterfaces());
				}
			}

			if (result != null) {
				result = synthesizeAnnotation(result, method);
				findAnnotationCache.put(cacheKey, result);
			}
		}

		return result;
	}

	private static <A extends Annotation> A searchOnInterfaces(Method method, Class<A> annotationType, Class<?>... ifcs) {
		A annotation = null;
		for (Class<?> ifc : ifcs) {
			if (isInterfaceWithAnnotatedMethods(ifc)) {
				try {
					Method equivalentMethod = ifc.getMethod(method.getName(), method.getParameterTypes());
					annotation = getAnnotation(equivalentMethod, annotationType);
				}
				catch (NoSuchMethodException ex) {
					// Skip this interface - it doesn't have the method...
				}
				if (annotation != null) {
					break;
				}
			}
		}
		return annotation;
	}

	static boolean isInterfaceWithAnnotatedMethods(Class<?> ifc) {
		Boolean found = annotatedInterfaceCache.get(ifc);
		if (found != null) {
			return found;
		}
		found = Boolean.FALSE;
		for (Method ifcMethod : ifc.getMethods()) {
			try {
				if (ifcMethod.getAnnotations().length > 0) {
					found = Boolean.TRUE;
					break;
				}
			}
			catch (Throwable ex) {
				handleIntrospectionFailure(ifcMethod, ex);
			}
		}
		annotatedInterfaceCache.put(ifc, found);
		return found;
	}

	/**
	 * 在提供的{@link Class}上查找{@code annotationType}的单个{@link Annotation},
	 * 如果注解在给定的类上不是<em>直接存在</em>, 则遍历其接口, 注释和超类.
	 * <p>此方法显式处理类级别注解, 这些注解未声明为{@link java.lang.annotation.Inherited inherited},
	 * <em>以及接口上的元注解和注解</em>.
	 * <p>该算法如下操作:
	 * <ol>
	 * <li>搜索给定类的注解, 如果找到则返回它.
	 * <li>递归搜索给定类声明的所有注解.
	 * <li>递归搜索给定类声明的所有接口.
	 * <li>递归搜索给定类的超类层次结构.
	 * </ol>
	 * <p>Note: 在此上下文中, 术语<em>递归</em>意味着搜索过程继续返回步骤 #1,
	 * 当前接口, 注释或超类作为类来查找注解.
	 * 
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 *
	 * @return 第一个匹配的注解, 或{@code null}
	 */
	public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
		return findAnnotation(clazz, annotationType, true);
	}

	/**
	 * 执行{@link #findAnnotation(AnnotatedElement, Class)}的实际工作, 尊重{@code synthesize}标志.
	 * 
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 * @param synthesize {@code true}如果结果应该是{@linkplain #synthesizeAnnotation(Annotation) synthesized}
	 * 
	 * @return 第一个匹配的注解, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType, boolean synthesize) {
		Assert.notNull(clazz, "Class must not be null");
		if (annotationType == null) {
			return null;
		}

		AnnotationCacheKey cacheKey = new AnnotationCacheKey(clazz, annotationType);
		A result = (A) findAnnotationCache.get(cacheKey);
		if (result == null) {
			result = findAnnotation(clazz, annotationType, new HashSet<Annotation>());
			if (result != null && synthesize) {
				result = synthesizeAnnotation(result, clazz);
				findAnnotationCache.put(cacheKey, result);
			}
		}
		return result;
	}

	/**
	 * 执行{@link #findAnnotation(Class, Class)}的搜索算法, 通过跟踪哪些注解已经<em>访问</em>来避免无限递归.
	 * 
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 * @param visited 已经访问过的注解
	 * 
	 * @return 第一个匹配的注解, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType, Set<Annotation> visited) {
		try {
			Annotation[] anns = clazz.getDeclaredAnnotations();
			for (Annotation ann : anns) {
				if (ann.annotationType() == annotationType) {
					return (A) ann;
				}
			}
			for (Annotation ann : anns) {
				if (!isInJavaLangAnnotationPackage(ann) && visited.add(ann)) {
					A annotation = findAnnotation(ann.annotationType(), annotationType, visited);
					if (annotation != null) {
						return annotation;
					}
				}
			}
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(clazz, ex);
			return null;
		}

		for (Class<?> ifc : clazz.getInterfaces()) {
			A annotation = findAnnotation(ifc, annotationType, visited);
			if (annotation != null) {
				return annotation;
			}
		}

		Class<?> superclass = clazz.getSuperclass();
		if (superclass == null || Object.class == superclass) {
			return null;
		}
		return findAnnotation(superclass, annotationType, visited);
	}

	/**
	 * 在指定的{@code clazz}的继承层次结构中查找第一个{@link Class} (包括指定的{@code clazz}本身),
	 * 其中指定的{@code annotationType}的注解是<em>直接存在</em>.
	 * <p>如果提供的{@code clazz}是一个接口, 则只会检查接口本身;
	 * 不会遍历接口的继承层次结构.
	 * <p><em>不会</em>搜索元注解.
	 * <p>标准的{@link Class} API没有提供一种机制来确定继承层次结构中哪个类实际声明了{@link Annotation}, 所以我们需要明确地处理它.
	 * 
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要检查注解的类 (may be {@code null})
	 * 
	 * @return 继承层次结构中的第一个{@link Class}, 用于声明指定的{@code annotationType}的注解; 如果未找到, 则为{@code null}
	 */
	public static Class<?> findAnnotationDeclaringClass(Class<? extends Annotation> annotationType, Class<?> clazz) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		if (clazz == null || Object.class == clazz) {
			return null;
		}
		if (isAnnotationDeclaredLocally(annotationType, clazz)) {
			return clazz;
		}
		return findAnnotationDeclaringClass(annotationType, clazz.getSuperclass());
	}

	/**
	 * 在指定的{@code clazz}的继承层次结构中查找第一个{@link Class} (包括指定的{@code clazz}本身),
	 * 其中至少有一个指定的{@code annotationTypes}是 <em>直接存在</em>.
	 * <p>如果提供的{@code clazz}是一个接口, 则只会检查接口本身;
	 * 不会遍历接口的继承层次结构.
	 * <p><em>不会</em>搜索元注解.
	 * <p>标准的{@link Class} API没有提供一种机制来确定继承层次结构中哪个类实际声明了几个候选{@linkplain Annotation annotations}中的一个,
	 * 所以我们需要明确地处理它.
	 * 
	 * @param annotationTypes 要查找的注解类型
	 * @param clazz 要检查注解的类, 或{@code null}
	 * 
	 * @return 继承层次结构中的第一个{@link Class}, 它声明了至少一个指定的{@code annotationTypes}的注解; 如果没有找到, 则为{@code null}
	 */
	public static Class<?> findAnnotationDeclaringClassForTypes(List<Class<? extends Annotation>> annotationTypes, Class<?> clazz) {
		Assert.notEmpty(annotationTypes, "List of annotation types must not be empty");
		if (clazz == null || Object.class == clazz) {
			return null;
		}
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (isAnnotationDeclaredLocally(annotationType, clazz)) {
				return clazz;
			}
		}
		return findAnnotationDeclaringClassForTypes(annotationTypes, clazz.getSuperclass());
	}

	/**
	 * 确定在提供的{@code clazz}上是否在本地声明了指定的{@code annotationType}的注解 (i.e., <em>直接存在</em>).
	 * <p>提供的{@link Class}可以代表任何类型.
	 * <p><em>不会</em>搜索元注解.
	 * <p>Note: 此方法<strong>不确定</strong>注解是否为{@linkplain java.lang.annotation.Inherited inherited}.
	 * 为了更清楚地了解继承注解, 请考虑使用{@link #isAnnotationInherited(Class, Class)}来代替.
	 * 
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要检查注解的类
	 * 
	 * @return {@code true} 如果指定的{@code annotationType}的注解是<em>直接存在</em>
	 */
	public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		Assert.notNull(clazz, "Class must not be null");
		try {
			for (Annotation ann : clazz.getDeclaredAnnotations()) {
				if (ann.annotationType() == annotationType) {
					return true;
				}
			}
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(clazz, ex);
		}
		return false;
	}

	/**
	 * 确定所提供的{@code clazz}上指定的{@code annotationType}的注解是否<em>存在</em>,
	 * 并且是{@linkplain java.lang.annotation.Inherited inherited} (i.e., 不是<em>直接存在</em>).
	 * <p><em>不会</em>搜索元注解.
	 * <p>如果提供的{@code clazz}是一个接口, 则只会检查接口本身.
	 * 根据Java中的标准元注解语义, 不会遍历接口的继承层次结构.
	 * 有关注释继承的更多详细信息, 请参阅{@linkplain java.lang.annotation.Inherited javadoc}以获取{@code @Inherited}元注解.
	 * 
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要检查注解的类
	 * 
	 * @return {@code true} 如果指定的{@code annotationType}的注解<em>存在</em>且<em>继承</em>
	 */
	public static boolean isAnnotationInherited(Class<? extends Annotation> annotationType, Class<?> clazz) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isAnnotationPresent(annotationType) && !isAnnotationDeclaredLocally(annotationType, clazz));
	}

	/**
	 * 在提供的{@code annotationType}上确定类型{@code metaAnnotationType}的注解是否为<em>元存在</em>.
	 * 
	 * @param annotationType 要查找的注解类型
	 * @param metaAnnotationType 要搜索的元注解的类型
	 * 
	 * @return {@code true} 如果这样的注解是元存在的
	 */
	public static boolean isAnnotationMetaPresent(Class<? extends Annotation> annotationType,
			Class<? extends Annotation> metaAnnotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null");
		if (metaAnnotationType == null) {
			return false;
		}

		AnnotationCacheKey cacheKey = new AnnotationCacheKey(annotationType, metaAnnotationType);
		Boolean metaPresent = metaPresentCache.get(cacheKey);
		if (metaPresent != null) {
			return metaPresent;
		}
		metaPresent = Boolean.FALSE;
		if (findAnnotation(annotationType, metaAnnotationType, false) != null) {
			metaPresent = Boolean.TRUE;
		}
		metaPresentCache.put(cacheKey, metaPresent);
		return metaPresent;
	}

	/**
	 * 确定在核心JDK {@code java.lang.annotation}包中是否定义了提供的{@link Annotation}.
	 * 
	 * @param annotation 要检查的注解
	 * 
	 * @return {@code true} 如果注解位于{@code java.lang.annotation}包中
	 */
	public static boolean isInJavaLangAnnotationPackage(Annotation annotation) {
		return (annotation != null && isInJavaLangAnnotationPackage(annotation.annotationType()));
	}

	/**
	 * 确定核心JDK {@code java.lang.annotation}包中是否定义了带有指定名称的{@link Annotation}.
	 * 
	 * @param annotationType 要检查的注解类型
	 * 
	 * @return {@code true} 如果注解位于{@code java.lang.annotation}包中
	 */
	static boolean isInJavaLangAnnotationPackage(Class<? extends Annotation> annotationType) {
		return (annotationType != null && isInJavaLangAnnotationPackage(annotationType.getName()));
	}

	/**
	 * 确定核心JDK {@code java.lang.annotation}包中是否定义了带有指定名称的{@link Annotation}.
	 * 
	 * @param annotationType 要检查的注解类型的名称
	 * 
	 * @return {@code true} 如果注解位于{@code java.lang.annotation}包中
	 */
	public static boolean isInJavaLangAnnotationPackage(String annotationType) {
		return (annotationType != null && annotationType.startsWith("java.lang.annotation"));
	}

	/**
	 * 检查给定注解声明的属性, 特别是覆盖Google App Engine迟到的{@code Class}值的
	 * {@code TypeNotPresentExceptionProxy}(而不是早期{@code Class.getAnnotations() 失败}.
	 * <p>此方法未失败表示{@link #getAnnotationAttributes(Annotation)}也不会失败 (稍后尝试).
	 * 
	 * @param annotation 要验证的注解
	 * 
	 * @throws IllegalStateException 如果无法读取声明的{@code Class}属性
	 */
	public static void validateAnnotation(Annotation annotation) {
		for (Method method : getAttributeMethods(annotation.annotationType())) {
			Class<?> returnType = method.getReturnType();
			if (returnType == Class.class || returnType == Class[].class) {
				try {
					method.invoke(annotation);
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Could not obtain annotation attribute value for " + method, ex);
				}
			}
		}
	}

	/**
	 * 检索给定注解的属性为{@link Map}, 保留所有属性类型.
	 * <p>等效于调用{@link #getAnnotationAttributes(Annotation, boolean, boolean)},
	 * 其中{@code classValuesAsString}和{@code nestedAnnotationsAsMap}参数为{@code false}.
	 * <p>Note: 此方法实际返回{@link AnnotationAttributes}实例.
	 * 但是, 保留了{@code Map}签名以实现二进制兼容性.
	 * 
	 * @param annotation 要检索属性的注解
	 * 
	 * @return 注解属性的Map, 属性名称为键, 相应的属性值为值 (never {@code null})
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
		return getAnnotationAttributes(null, annotation);
	}

	/**
	 * 检索给定注解的属性为{@link Map}.
	 * <p>等效于调用{@link #getAnnotationAttributes(Annotation, boolean, boolean)},
	 * 其中{@code nestedAnnotationsAsMap}参数为{@code false}.
	 * <p>Note: 此方法实际返回{@link AnnotationAttributes}实例.
	 * 但是, 保留了{@code Map}签名以实现二进制兼容性.
	 * 
	 * @param annotation 要检索属性的注解
	 * @param classValuesAsString 是否将Class引用转换为字符串
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为Class引用
	 * 
	 * @return 注解属性的Map, 属性名称为键, 相应的属性值为值 (never {@code null})
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation, boolean classValuesAsString) {
		return getAnnotationAttributes(annotation, classValuesAsString, false);
	}

	/**
	 * 检索给定注解的属性为{@link AnnotationAttributes}映射.
	 * <p>此方法提供完全递归的注解读取功能, 与基于反射的{@link org.springframework.core.type.StandardAnnotationMetadata}相同.
	 * 
	 * @param annotation 要检索属性的注解
	 * @param classValuesAsString 是否将Class引用转换为字符串
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为{@link AnnotationAttributes}映射
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为{@code Annotation}实例
	 * 
	 * @return 注解属性 (一个特殊的Map), 属性名称为键, 相应的属性值为值  (never {@code null})
	 */
	public static AnnotationAttributes getAnnotationAttributes(Annotation annotation, boolean classValuesAsString,
			boolean nestedAnnotationsAsMap) {

		return getAnnotationAttributes(null, annotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 检索给定注解的属性为{@link AnnotationAttributes}映射.
	 * <p>等效于调用{@link #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)},
	 * 其中{@code classValuesAsString}和{@code nestedAnnotationsAsMap}参数为{@code false}.
	 * 
	 * @param annotatedElement 使用提供的注解进行注解的元素; may be {@code null} if unknown
	 * @param annotation 要检索属性的注解
	 * 
	 * @return 注解属性 (一个特殊的Map), 属性名称为键, 相应的属性值为值 (never {@code null})
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement, Annotation annotation) {
		return getAnnotationAttributes(annotatedElement, annotation, false, false);
	}

	/**
	 * 检索给定注解的属性为{@link AnnotationAttributes}映射.
	 * <p>此方法提供完全递归的注解读取功能, 与基于反射的{@link org.springframework.core.type.StandardAnnotationMetadata}相同.
	 * 
	 * @param annotatedElement 使用提供的注解进行注解的元素; may be {@code null} if unknown
	 * @param annotation 要检索属性的注解
	 * @param classValuesAsString 是否将Class引用转换为字符串
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为{@link AnnotationAttributes}映射
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为{@code Annotation}实例
	 * 
	 * @return 注解属性 (一个特殊的Map), 属性名称为键, 相应的属性值为值 (never {@code null})
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement,
			Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		return getAnnotationAttributes(
				(Object) annotatedElement, annotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	private static AnnotationAttributes getAnnotationAttributes(Object annotatedElement,
			Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		AnnotationAttributes attributes =
				retrieveAnnotationAttributes(annotatedElement, annotation, classValuesAsString, nestedAnnotationsAsMap);
		postProcessAnnotationAttributes(annotatedElement, attributes, classValuesAsString, nestedAnnotationsAsMap);
		return attributes;
	}

	/**
	 * 检索给定注解的属性为{@link AnnotationAttributes}映射.
	 * <p>此方法提供完全递归的注解读取功能, 与基于反射的{@link org.springframework.core.type.StandardAnnotationMetadata}相同.
	 * <p><strong>NOTE</strong>: {@code getAnnotationAttributes()}的这个变体仅供在框架内使用.
	 * 以下特殊规则适用:
	 * <ol>
	 * <li>默认值将替换为默认值占位符.</li>
	 * <li>生成的, 合并的注解属性最终应该是{@linkplain #postProcessAnnotationAttributes 后处理},
	 * 为了确保占位符已被实际默认值替换, 并强制执行{@code @AliasFor}语义.</li>
	 * </ol>
	 * 
	 * @param annotatedElement 使用提供的注解进行注解的元素; may be {@code null} if unknown
	 * @param annotation 要检索属性的注解
	 * @param classValuesAsString 是否将Class引用转换为字符串
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为{@link AnnotationAttributes}映射
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为{@code Annotation}实例
	 * 
	 * @return 注解属性 (一个特殊的Map), 属性名称为键, 相应的属性值为值 (never {@code null})
	 */
	static AnnotationAttributes retrieveAnnotationAttributes(Object annotatedElement, Annotation annotation,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		Class<? extends Annotation> annotationType = annotation.annotationType();
		AnnotationAttributes attributes = new AnnotationAttributes(annotationType);

		for (Method method : getAttributeMethods(annotationType)) {
			try {
				Object attributeValue = method.invoke(annotation);
				Object defaultValue = method.getDefaultValue();
				if (defaultValue != null && ObjectUtils.nullSafeEquals(attributeValue, defaultValue)) {
					attributeValue = new DefaultValueHolder(defaultValue);
				}
				attributes.put(method.getName(),
						adaptValue(annotatedElement, attributeValue, classValuesAsString, nestedAnnotationsAsMap));
			}
			catch (Throwable ex) {
				if (ex instanceof InvocationTargetException) {
					Throwable targetException = ((InvocationTargetException) ex).getTargetException();
					rethrowAnnotationConfigurationException(targetException);
				}
				throw new IllegalStateException("Could not obtain annotation attribute value for " + method, ex);
			}
		}

		return attributes;
	}

	/**
	 * 根据给定的类和嵌套的注解设置适配给定值.
	 * <p>嵌套的注解{@linkplain #synthesizeAnnotation(Annotation, AnnotatedElement) 合成}.
	 * 
	 * @param annotatedElement 带注解的元素, 用于上下文日志记录; may be {@code null} if unknown
	 * @param value 注解属性值
	 * @param classValuesAsString 是否将Class引用转换为字符串
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为{@link AnnotationAttributes}映射
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为{@code Annotation}实例
	 * 
	 * @return 适配后的值, 或原始值
	 */
	static Object adaptValue(Object annotatedElement, Object value, boolean classValuesAsString,
			boolean nestedAnnotationsAsMap) {

		if (classValuesAsString) {
			if (value instanceof Class) {
				return ((Class<?>) value).getName();
			}
			else if (value instanceof Class[]) {
				Class<?>[] clazzArray = (Class<?>[]) value;
				String[] classNames = new String[clazzArray.length];
				for (int i = 0; i < clazzArray.length; i++) {
					classNames[i] = clazzArray[i].getName();
				}
				return classNames;
			}
		}

		if (value instanceof Annotation) {
			Annotation annotation = (Annotation) value;
			if (nestedAnnotationsAsMap) {
				return getAnnotationAttributes(annotatedElement, annotation, classValuesAsString, true);
			}
			else {
				return synthesizeAnnotation(annotation, annotatedElement);
			}
		}

		if (value instanceof Annotation[]) {
			Annotation[] annotations = (Annotation[]) value;
			if (nestedAnnotationsAsMap) {
				AnnotationAttributes[] mappedAnnotations = new AnnotationAttributes[annotations.length];
				for (int i = 0; i < annotations.length; i++) {
					mappedAnnotations[i] =
							getAnnotationAttributes(annotatedElement, annotations[i], classValuesAsString, true);
				}
				return mappedAnnotations;
			}
			else {
				return synthesizeAnnotationArray(annotations, annotatedElement);
			}
		}

		// Fallback
		return value;
	}

	/**
	 * 为给定属性注册注解声明的默认值.
	 * 
	 * @param attributes 要处理的注解属性
	 */
	public static void registerDefaultValues(AnnotationAttributes attributes) {
		// 仅对公共注解进行默认扫描;
		// 否则我们会遇到IllegalAccessExceptions, 我们不想在SecurityManager环境中搞乱可访问性.
		Class<? extends Annotation> annotationType = attributes.annotationType();
		if (annotationType != null && Modifier.isPublic(annotationType.getModifiers())) {
			// 检查注解类型中声明的属性的默认值.
			for (Method annotationAttribute : getAttributeMethods(annotationType)) {
				String attributeName = annotationAttribute.getName();
				Object defaultValue = annotationAttribute.getDefaultValue();
				if (defaultValue != null && !attributes.containsKey(attributeName)) {
					if (defaultValue instanceof Annotation) {
						defaultValue = getAnnotationAttributes((Annotation) defaultValue, false, true);
					}
					else if (defaultValue instanceof Annotation[]) {
						Annotation[] realAnnotations = (Annotation[]) defaultValue;
						AnnotationAttributes[] mappedAnnotations = new AnnotationAttributes[realAnnotations.length];
						for (int i = 0; i < realAnnotations.length; i++) {
							mappedAnnotations[i] = getAnnotationAttributes(realAnnotations[i], false, true);
						}
						defaultValue = mappedAnnotations;
					}
					attributes.put(attributeName, new DefaultValueHolder(defaultValue));
				}
			}
		}
	}

	/**
	 * 对提供的{@link AnnotationAttributes}进行后处理, 保留嵌套的注解为{@code Annotation}实例.
	 * <p>具体来说, 此方法对使用{@link AliasFor @AliasFor}注解的注解属性强制执行<em>属性别名</em>语义, 并将默认值占位符替换为其原始默认值.
	 * 
	 * @param annotatedElement 使用注解或注解层次结构进行注解的元素, 从中创建了提供的属性; may be {@code null} if unknown
	 * @param attributes 要后处理的注解属性
	 * @param classValuesAsString 是否将Class引用转换为字符串
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为Class引用
	 */
	public static void postProcessAnnotationAttributes(Object annotatedElement,
			AnnotationAttributes attributes, boolean classValuesAsString) {

		postProcessAnnotationAttributes(annotatedElement, attributes, classValuesAsString, false);
	}

	/**
	 * 后处理提供的{@link AnnotationAttributes}.
	 * <p>具体来说, 此方法对使用{@link AliasFor @AliasFor}注解的注解属性强制执行<em>属性别名</em>语义, 并将默认值占位符替换为其原始默认值.
	 * 
	 * @param annotatedElement 使用注解或注解层次结构进行注解的元素, 从中创建了提供的属性; may be {@code null} if unknown
	 * @param attributes 要后处理的注解属性
	 * @param classValuesAsString 是否将Class引用转换为字符串
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为{@link AnnotationAttributes}映射
	 * (为了与{@link org.springframework.core.type.AnnotationMetadata}兼容), 或将它们保存为{@code Annotation}实例
	 */
	static void postProcessAnnotationAttributes(Object annotatedElement,
			AnnotationAttributes attributes, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		// Abort?
		if (attributes == null) {
			return;
		}

		Class<? extends Annotation> annotationType = attributes.annotationType();

		// 跟踪哪些属性值已被替换, 以便我们可以短路搜索算法.
		Set<String> valuesAlreadyReplaced = new HashSet<String>();

		if (!attributes.validated) {
			// 验证@AliasFor配置
			Map<String, List<String>> aliasMap = getAttributeAliasMap(annotationType);
			for (String attributeName : aliasMap.keySet()) {
				if (valuesAlreadyReplaced.contains(attributeName)) {
					continue;
				}
				Object value = attributes.get(attributeName);
				boolean valuePresent = (value != null && !(value instanceof DefaultValueHolder));
				for (String aliasedAttributeName : aliasMap.get(attributeName)) {
					if (valuesAlreadyReplaced.contains(aliasedAttributeName)) {
						continue;
					}
					Object aliasedValue = attributes.get(aliasedAttributeName);
					boolean aliasPresent = (aliasedValue != null && !(aliasedValue instanceof DefaultValueHolder));
					// 用别名验证或替换的东西?
					if (valuePresent || aliasPresent) {
						if (valuePresent && aliasPresent) {
							// 由于注解属性可以是数组, 必须使用 ObjectUtils.nullSafeEquals().
							if (!ObjectUtils.nullSafeEquals(value, aliasedValue)) {
								String elementAsString =
										(annotatedElement != null ? annotatedElement.toString() : "unknown element");
								throw new AnnotationConfigurationException(String.format(
										"In AnnotationAttributes for annotation [%s] declared on %s, " +
										"attribute '%s' and its alias '%s' are declared with values of [%s] and [%s], " +
										"but only one is permitted.", annotationType.getName(), elementAsString,
										attributeName, aliasedAttributeName, ObjectUtils.nullSafeToString(value),
										ObjectUtils.nullSafeToString(aliasedValue)));
							}
						}
						else if (aliasPresent) {
							// Replace value with aliasedValue
							attributes.put(attributeName,
									adaptValue(annotatedElement, aliasedValue, classValuesAsString, nestedAnnotationsAsMap));
							valuesAlreadyReplaced.add(attributeName);
						}
						else {
							// Replace aliasedValue with value
							attributes.put(aliasedAttributeName,
									adaptValue(annotatedElement, value, classValuesAsString, nestedAnnotationsAsMap));
							valuesAlreadyReplaced.add(aliasedAttributeName);
						}
					}
				}
			}
			attributes.validated = true;
		}

		// 用实际默认值替换任何剩余占位符
		for (String attributeName : attributes.keySet()) {
			if (valuesAlreadyReplaced.contains(attributeName)) {
				continue;
			}
			Object value = attributes.get(attributeName);
			if (value instanceof DefaultValueHolder) {
				value = ((DefaultValueHolder) value).defaultValue;
				attributes.put(attributeName,
						adaptValue(annotatedElement, value, classValuesAsString, nestedAnnotationsAsMap));
			}
		}
	}

	/**
	 * 在给定注解实例的情况下, 检索单元素注解的{@code value}属性的<em>值</em>.
	 * 
	 * @param annotation 从中检索值的注解实例
	 * 
	 * @return 属性值; 如果找不到, 则为{@code null},
	 * 除非由于{@link AnnotationConfigurationException}而无法检索属性值, 在这种情况下将重新抛出此类异常
	 */
	public static Object getValue(Annotation annotation) {
		return getValue(annotation, VALUE);
	}

	/**
	 * 在给定注解实例的情况下, 检索命名属性的<em>值</em>.
	 * 
	 * @param annotation 从中检索值的注解实例
	 * @param attributeName 要检索属性值的名称
	 * 
	 * @return 属性值; 如果找不到, 则为{@code null},
	 * 除非由于{@link AnnotationConfigurationException}而无法检索属性值, 在这种情况下将重新抛出此类异常
	 */
	public static Object getValue(Annotation annotation, String attributeName) {
		if (annotation == null || !StringUtils.hasText(attributeName)) {
			return null;
		}
		try {
			Method method = annotation.annotationType().getDeclaredMethod(attributeName);
			ReflectionUtils.makeAccessible(method);
			return method.invoke(annotation);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
		catch (InvocationTargetException ex) {
			rethrowAnnotationConfigurationException(ex.getTargetException());
			throw new IllegalStateException(
					"Could not obtain value for annotation attribute '" + attributeName + "' in " + annotation, ex);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotation.getClass(), ex);
			return null;
		}
	}

	/**
	 * 在给定注解实例的情况下, 检索单元素注解的{@code value}属性的<em>默认值</em>.
	 * 
	 * @param annotation 从中检索默认值的注解实例
	 * 
	 * @return 默认值, 或{@code null}
	 */
	public static Object getDefaultValue(Annotation annotation) {
		return getDefaultValue(annotation, VALUE);
	}

	/**
	 * 在给定注解实例的情况下, 检索命名属性的<em>默认值</em>.
	 * 
	 * @param annotation 从中检索默认值的注解实例
	 * @param attributeName 要检索属性值的名称
	 * 
	 * @return 命名属性的默认值, 或{@code null}
	 */
	public static Object getDefaultValue(Annotation annotation, String attributeName) {
		if (annotation == null) {
			return null;
		}
		return getDefaultValue(annotation.annotationType(), attributeName);
	}

	/**
	 * 在给定{@link Class 注解类型}的情况下, 检索单元素注解的{@code value}属性的<em>默认值</em>.
	 * 
	 * @param annotationType 要检索默认值的<em>注解类型</em>
	 * 
	 * @return 默认值, 或{@code null}
	 */
	public static Object getDefaultValue(Class<? extends Annotation> annotationType) {
		return getDefaultValue(annotationType, VALUE);
	}

	/**
	 * 在给定{@link Class 注解类型}的情况下, 检索命名属性的<em>默认值</em>.
	 * 
	 * @param annotationType 要检索默认值的<em>注解类型</em>
	 * @param attributeName 要检索属性值的名称.
	 * 
	 * @return 命名属性的默认值, 或{@code null}
	 */
	public static Object getDefaultValue(Class<? extends Annotation> annotationType, String attributeName) {
		if (annotationType == null || !StringUtils.hasText(attributeName)) {
			return null;
		}
		try {
			return annotationType.getDeclaredMethod(attributeName).getDefaultValue();
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotationType, ex);
			return null;
		}
	}

	/**
	 * 从提供的{@code annotation}<em>合成</em>注解, 将其包装在动态代理中,
	 * 该代理透明地为使用{@link AliasFor @AliasFor}注解的注解属性强制执行<em>属性别名</em>语义.
	 * 
	 * @param annotation 要合成的注解
	 * 
	 * @return 如果提供的注解是<em>可合成的</em>, 则为合成注解; {@code null} 如果提供的注解是{@code null};
	 * 否则, 提供的注解不会被修改
	 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
	 */
	static <A extends Annotation> A synthesizeAnnotation(A annotation) {
		return synthesizeAnnotation(annotation, null);
	}

	/**
	 * 从提供的{@code annotation}<em>合成</em>注解, 将其包装在动态代理中,
	 * 该代理透明地为使用{@link AliasFor @AliasFor}注解的注解属性强制执行<em>属性别名</em>语义.
	 * 
	 * @param annotation 要合成的注解
	 * @param annotatedElement 使用提供的注解进行注解的元素; may be {@code null} if unknown
	 * 
	 * @return 如果提供的注解是<em>可合成的</em>, 则为合成注解; {@code null} 如果提供的注解是{@code null};
	 * 否则, 提供的注解不会被修改
	 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
	 */
	public static <A extends Annotation> A synthesizeAnnotation(A annotation, AnnotatedElement annotatedElement) {
		return synthesizeAnnotation(annotation, (Object) annotatedElement);
	}

	@SuppressWarnings("unchecked")
	static <A extends Annotation> A synthesizeAnnotation(A annotation, Object annotatedElement) {
		if (annotation == null) {
			return null;
		}
		if (annotation instanceof SynthesizedAnnotation) {
			return annotation;
		}

		Class<? extends Annotation> annotationType = annotation.annotationType();
		if (!isSynthesizable(annotationType)) {
			return annotation;
		}

		DefaultAnnotationAttributeExtractor attributeExtractor =
				new DefaultAnnotationAttributeExtractor(annotation, annotatedElement);
		InvocationHandler handler = new SynthesizedAnnotationInvocationHandler(attributeExtractor);

		// 总是可以公开Spring的SynthesizedAnnotation标记, 因为我们之前显式检查了可合成的注解 (需要从同一个包中声明@AliasFor)
		Class<?>[] exposedInterfaces = new Class<?>[] {annotationType, SynthesizedAnnotation.class};
		return (A) Proxy.newProxyInstance(annotation.getClass().getClassLoader(), exposedInterfaces, handler);
	}

	/**
	 * 从提供的注解属性的Map中<em>合成</em>注解, 将Map包装在动态代理中, 该动态代理实现指定{@code annotationType}的注解,
	 * 透明地为使用{@link AliasFor @AliasFor}注解的注解属性强制执行<em>属性别名</em>语义.
	 * <p>提供的Map必须包含所提供的{@code annotationType}中定义的每个属性的键值对, 该属性没有别名或没有默认值.
	 * Map中嵌套的Map和嵌套的数组将分别递归合成为嵌套的注解或嵌套的注解数组.
	 * <p>请注意, {@link AnnotationAttributes}是{@link Map}的特殊类型, 是此方法的{@code attributes}参数的理想候选者.
	 * 
	 * @param attributes 要合成的注解属性
	 * @param annotationType 要合成的注解类型
	 * @param annotatedElement 使用与提供的属性对应的注解进行注解的元素; may be {@code null} if unknown
	 * 
	 * @return 合成后的注解; 或{@code null} 如果提供的属性Map为 {@code null}
	 * @throws IllegalArgumentException 如果缺少必需的属性或属性的类型不正确
	 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A synthesizeAnnotation(Map<String, Object> attributes,
			Class<A> annotationType, AnnotatedElement annotatedElement) {

		Assert.notNull(annotationType, "'annotationType' must not be null");
		if (attributes == null) {
			return null;
		}

		MapAnnotationAttributeExtractor attributeExtractor =
				new MapAnnotationAttributeExtractor(attributes, annotationType, annotatedElement);
		InvocationHandler handler = new SynthesizedAnnotationInvocationHandler(attributeExtractor);
		Class<?>[] exposedInterfaces = (canExposeSynthesizedMarker(annotationType) ?
				new Class<?>[] {annotationType, SynthesizedAnnotation.class} : new Class<?>[] {annotationType});
		return (A) Proxy.newProxyInstance(annotationType.getClassLoader(), exposedInterfaces, handler);
	}

	/**
	 * 从默认属性值<em>合成</em>注解.
	 * <p>此方法委托给{@link #synthesizeAnnotation(Map, Class, AnnotatedElement)},
	 * 其中源属性值为空Map, 且{@link AnnotatedElement}为{@code null}.
	 * 
	 * @param annotationType 要合成的注解类型
	 * 
	 * @return 合成后的注解
	 * @throws IllegalArgumentException 如果缺少必需的属性
	 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
	 */
	public static <A extends Annotation> A synthesizeAnnotation(Class<A> annotationType) {
		return synthesizeAnnotation(Collections.<String, Object> emptyMap(), annotationType, null);
	}

	/**
	 * 从所提供的{@code annotations}数组<em>合成</em>注解数组, 创建一个大小和类型相同的新数组,
	 * 并使用{@linkplain #synthesizeAnnotation(Annotation) 合成的}注解版本从输入数组填充.
	 * 
	 * @param annotations 要合成的注解数组
	 * @param annotatedElement 使用提供的注解数组进行注解的元素; may be {@code null} if unknown
	 * 
	 * @return 一个新的合成注解的数组, 或{@code null} 如果提供的数组为{@code null}
	 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
	 */
	static Annotation[] synthesizeAnnotationArray(Annotation[] annotations, Object annotatedElement) {
		if (annotations == null) {
			return null;
		}

		Annotation[] synthesized = (Annotation[]) Array.newInstance(
				annotations.getClass().getComponentType(), annotations.length);
		for (int i = 0; i < annotations.length; i++) {
			synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
		}
		return synthesized;
	}

	/**
	 * 从提供的注解属性的{@code maps}数组<em>合成</em>注解数组, 创建一个具有相同大小的{@code annotationType}的新数组,
	 * 并使用{@linkplain #synthesizeAnnotation(Map, Class, AnnotatedElement) 合成的} maps版本从输入数组填充.
	 * 
	 * @param maps 要合成的注解属性
	 * @param annotationType 要合成的注解类型 (never {@code null})
	 * 
	 * @return 一个新的合成注解的数组, 或{@code null} 如果提供的数组为{@code null}
	 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
	 */
	@SuppressWarnings("unchecked")
	static <A extends Annotation> A[] synthesizeAnnotationArray(Map<String, Object>[] maps, Class<A> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		if (maps == null) {
			return null;
		}

		A[] synthesized = (A[]) Array.newInstance(annotationType, maps.length);
		for (int i = 0; i < maps.length; i++) {
			synthesized[i] = synthesizeAnnotation(maps[i], annotationType, null);
		}
		return synthesized;
	}

	/**
	 * 获取所提供的注解类型中通过{@code @AliasFor}声明的所有属性别名.
	 * <p>属性名称作为Key, 每个值表示别名属性的名称列表.
	 * <p>对于<em>显式</em>别名对, 例如x和y (i.e., 其中x是{@code @AliasFor("y")}, y是{@code @AliasFor("x")},
	 * Map中会有两个条目: {@code x -> (y)} 和 {@code y -> (x)}.
	 * <p>对于<em>隐式</em>别名 (i.e., 在同一元注解中声明为同一属性的属性覆盖的属性), Map中将有n个条目.
	 * 例如, 如果 x, y, 和 z 是隐式别名, 则Map将包含以下条目:
	 * {@code x -> (y, z)}, {@code y -> (x, z)}, {@code z -> (x, y)}.
	 * <p>空返回值意味着注解不声明任何属性别名.
	 * 
	 * @param annotationType 要查找属性别名的注解类型
	 * 
	 * @return 包含属性别名的Map (never {@code null})
	 */
	static Map<String, List<String>> getAttributeAliasMap(Class<? extends Annotation> annotationType) {
		if (annotationType == null) {
			return Collections.emptyMap();
		}

		Map<String, List<String>> map = attributeAliasesCache.get(annotationType);
		if (map != null) {
			return map;
		}

		map = new LinkedHashMap<String, List<String>>();
		for (Method attribute : getAttributeMethods(annotationType)) {
			List<String> aliasNames = getAttributeAliasNames(attribute);
			if (!aliasNames.isEmpty()) {
				map.put(attribute.getName(), aliasNames);
			}
		}

		attributeAliasesCache.put(annotationType, map);
		return map;
	}

	/**
	 * 检查是否可以为给定的注解类型公开{@link SynthesizedAnnotation}标记.
	 * 
	 * @param annotationType 即将为其创建合成代理的注解类型
	 */
	private static boolean canExposeSynthesizedMarker(Class<? extends Annotation> annotationType) {
		try {
			return (Class.forName(SynthesizedAnnotation.class.getName(), false, annotationType.getClassLoader()) ==
					SynthesizedAnnotation.class);
		}
		catch (ClassNotFoundException ex) {
			return false;
		}
	}

	/**
	 * 确定提供的{@code annotationType}的注解是否为<em>可合成</em>的
	 * (i.e., 需要包装在动态代理中, 该代理提供的功能高于标准JDK注解的功能).
	 * <p>具体来说, 注解是<em>可合成的</em>, 如果它通过{@link AliasFor @AliasFor}声明任何配置为<em>别名对</em>的属性,
	 * 或者注解使用的任何嵌套注解声明此<em>别名对</em>.
	 */
	@SuppressWarnings("unchecked")
	private static boolean isSynthesizable(Class<? extends Annotation> annotationType) {
		Boolean synthesizable = synthesizableCache.get(annotationType);
		if (synthesizable != null) {
			return synthesizable;
		}

		synthesizable = Boolean.FALSE;
		for (Method attribute : getAttributeMethods(annotationType)) {
			if (!getAttributeAliasNames(attribute).isEmpty()) {
				synthesizable = Boolean.TRUE;
				break;
			}
			Class<?> returnType = attribute.getReturnType();
			if (Annotation[].class.isAssignableFrom(returnType)) {
				Class<? extends Annotation> nestedAnnotationType =
						(Class<? extends Annotation>) returnType.getComponentType();
				if (isSynthesizable(nestedAnnotationType)) {
					synthesizable = Boolean.TRUE;
					break;
				}
			}
			else if (Annotation.class.isAssignableFrom(returnType)) {
				Class<? extends Annotation> nestedAnnotationType = (Class<? extends Annotation>) returnType;
				if (isSynthesizable(nestedAnnotationType)) {
					synthesizable = Boolean.TRUE;
					break;
				}
			}
		}

		synthesizableCache.put(annotationType, synthesizable);
		return synthesizable;
	}

	/**
	 * 获取通过{@link AliasFor @AliasFor}配置的别名属性的名称, 用于提供的注解{@code attribute}.
	 * 
	 * @param attribute 要查找别名的属性
	 * 
	 * @return 别名属性的名称 (从不为 {@code null}, 但可能为<em>空</em>)
	 * @throws IllegalArgumentException 如果提供的属性方法是{@code null}, 或不是来自一个注解
	 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
	 */
	static List<String> getAttributeAliasNames(Method attribute) {
		Assert.notNull(attribute, "attribute must not be null");
		AliasDescriptor descriptor = AliasDescriptor.from(attribute);
		return (descriptor != null ? descriptor.getAttributeAliasNames() : Collections.<String> emptyList());
	}

	/**
	 * 获取通过{@link AliasFor @AliasFor}为所提供的注解{@code attribute}配置的覆盖属性的名称.
	 * 
	 * @param attribute 从中检索覆盖的属性 (never {@code null})
	 * @param metaAnnotationType 元注解的类型, 允许在其中声明覆盖属性
	 * 
	 * @return 已覆盖属性的名称; 或{@code null}如果未找到, 或不适用于指定的元注解类型
	 * @throws IllegalArgumentException 如果提供的属性方法是{@code null}, 或不是来自一个注解,
	 * 或者提供的元注解类型是{@code null}或{@link Annotation}
	 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
	 */
	static String getAttributeOverrideName(Method attribute, Class<? extends Annotation> metaAnnotationType) {
		Assert.notNull(attribute, "attribute must not be null");
		Assert.notNull(metaAnnotationType, "metaAnnotationType must not be null");
		Assert.isTrue(Annotation.class != metaAnnotationType,
				"metaAnnotationType must not be [java.lang.annotation.Annotation]");

		AliasDescriptor descriptor = AliasDescriptor.from(attribute);
		return (descriptor != null ? descriptor.getAttributeOverrideName(metaAnnotationType) : null);
	}

	/**
	 * 获取所提供的{@code annotationType}中声明的所有方法, 这些方法符合Java对注解<em>属性</em>的要求.
	 * <p>返回的列表中的所有方法都是{@linkplain ReflectionUtils#makeAccessible(Method) 可访问}.
	 * 
	 * @param annotationType 要搜索属性方法的类型 (never {@code null})
	 * 
	 * @return 指定注解类型中的所有注解属性方法 (从不为{@code null}, 但可能为<em>空</em>)
	 */
	static List<Method> getAttributeMethods(Class<? extends Annotation> annotationType) {
		List<Method> methods = attributeMethodsCache.get(annotationType);
		if (methods != null) {
			return methods;
		}

		methods = new ArrayList<Method>();
		for (Method method : annotationType.getDeclaredMethods()) {
			if (isAttributeMethod(method)) {
				ReflectionUtils.makeAccessible(method);
				methods.add(method);
			}
		}

		attributeMethodsCache.put(annotationType, methods);
		return methods;
	}

	/**
	 * 获取提供的{@code element}上使用提供的{@code annotationName}的注解.
	 * 
	 * @param element 要搜索的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 找到的注解; 或{@code null}
	 */
	static Annotation getAnnotation(AnnotatedElement element, String annotationName) {
		for (Annotation annotation : element.getAnnotations()) {
			if (annotation.annotationType().getName().equals(annotationName)) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * 确定提供的{@code method}是否为注解属性方法.
	 * 
	 * @param method 要检查的方法
	 * 
	 * @return {@code true} 如果方法是属性方法
	 */
	static boolean isAttributeMethod(Method method) {
		return (method != null && method.getParameterTypes().length == 0 && method.getReturnType() != void.class);
	}

	/**
	 * 确定提供的方法是否为"annotationType"方法.
	 * 
	 * @return {@code true}如果该方法是"annotationType"方法
	 */
	static boolean isAnnotationTypeMethod(Method method) {
		return (method != null && method.getName().equals("annotationType") && method.getParameterTypes().length == 0);
	}

	/**
	 * 解析所提供的可重复{@code annotationType}的容器类型.
	 * <p>自动检测通过{@link java.lang.annotation.Repeatable}声明的<em>容器注解</em>.
	 * 如果提供的注解类型未使用{@code @Repeatable}注解, 则此方法只返回{@code null}.
	 */
	@SuppressWarnings("unchecked")
	static Class<? extends Annotation> resolveContainerAnnotationType(Class<? extends Annotation> annotationType) {
		try {
			Annotation repeatable = getAnnotation(annotationType, REPEATABLE_CLASS_NAME);
			if (repeatable != null) {
				Object value = getValue(repeatable);
				return (Class<? extends Annotation>) value;
			}
		}
		catch (Exception ex) {
			handleIntrospectionFailure(annotationType, ex);
		}
		return null;
	}

	/**
	 * 如果提供的throwable是{@link AnnotationConfigurationException},
	 * 它将被强制转换为{@code AnnotationConfigurationException}并抛出, 允许它传播给调用者.
	 * <p>否则, 此方法不执行任何操作.
	 * 
	 * @param ex the throwable to inspect
	 */
	static void rethrowAnnotationConfigurationException(Throwable ex) {
		if (ex instanceof AnnotationConfigurationException) {
			throw (AnnotationConfigurationException) ex;
		}
	}

	/**
	 * 处理提供的注解内省异常.
	 * <p>如果提供的异常是{@link AnnotationConfigurationException}, 它将被简单地抛出, 允许它传播给调用者, 并且不会记录任何内容.
	 * <p>否则, 此方法在继续之前记录内省失败 (特别是{@code TypeNotPresentExceptions}),
	 * 假设嵌套的Class值在注解属性中无法解析, 从而有效地假装指定元素上没有注解.
	 * 
	 * @param element 试图在其上内省注解的元素
	 * @param ex 遇到的异常
	 */
	static void handleIntrospectionFailure(AnnotatedElement element, Throwable ex) {
		rethrowAnnotationConfigurationException(ex);

		Log loggerToUse = logger;
		if (loggerToUse == null) {
			loggerToUse = LogFactory.getLog(AnnotationUtils.class);
			logger = loggerToUse;
		}
		if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
			// 注解类型上的元注解或 (默认) 值查找
			if (loggerToUse.isDebugEnabled()) {
				loggerToUse.debug("Failed to meta-introspect annotation " + element + ": " + ex);
			}
		}
		else {
			// 在常规Class, Method, Field上直接注解查找
			if (loggerToUse.isInfoEnabled()) {
				loggerToUse.info("Failed to introspect annotations on " + element + ": " + ex);
			}
		}
	}

	/**
	 * 清除内部注解元数据缓存.
	 */
	public static void clearCache() {
		findAnnotationCache.clear();
		metaPresentCache.clear();
		annotatedInterfaceCache.clear();
		synthesizableCache.clear();
		attributeAliasesCache.clear();
		attributeMethodsCache.clear();
		aliasDescriptorCache.clear();
	}


	/**
	 * AnnotatedElement缓存的缓存键.
	 */
	private static final class AnnotationCacheKey implements Comparable<AnnotationCacheKey> {

		private final AnnotatedElement element;

		private final Class<? extends Annotation> annotationType;

		public AnnotationCacheKey(AnnotatedElement element, Class<? extends Annotation> annotationType) {
			this.element = element;
			this.annotationType = annotationType;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof AnnotationCacheKey)) {
				return false;
			}
			AnnotationCacheKey otherKey = (AnnotationCacheKey) other;
			return (this.element.equals(otherKey.element) && this.annotationType.equals(otherKey.annotationType));
		}

		@Override
		public int hashCode() {
			return (this.element.hashCode() * 29 + this.annotationType.hashCode());
		}

		@Override
		public String toString() {
			return "@" + this.annotationType + " on " + this.element;
		}

		@Override
		public int compareTo(AnnotationCacheKey other) {
			int result = this.element.toString().compareTo(other.element.toString());
			if (result == 0) {
				result = this.annotationType.getName().compareTo(other.annotationType.getName());
			}
			return result;
		}
	}


	private static class AnnotationCollector<A extends Annotation> {

		private final Class<A> annotationType;

		private final Class<? extends Annotation> containerAnnotationType;

		private final boolean declaredMode;

		private final Set<AnnotatedElement> visited = new HashSet<AnnotatedElement>();

		private final Set<A> result = new LinkedHashSet<A>();

		AnnotationCollector(Class<A> annotationType, Class<? extends Annotation> containerAnnotationType, boolean declaredMode) {
			this.annotationType = annotationType;
			this.containerAnnotationType = (containerAnnotationType != null ? containerAnnotationType :
					resolveContainerAnnotationType(annotationType));
			this.declaredMode = declaredMode;
		}

		Set<A> getResult(AnnotatedElement element) {
			process(element);
			return Collections.unmodifiableSet(this.result);
		}

		@SuppressWarnings("unchecked")
		private void process(AnnotatedElement element) {
			if (this.visited.add(element)) {
				try {
					Annotation[] annotations = (this.declaredMode ? element.getDeclaredAnnotations() : element.getAnnotations());
					for (Annotation ann : annotations) {
						Class<? extends Annotation> currentAnnotationType = ann.annotationType();
						if (ObjectUtils.nullSafeEquals(this.annotationType, currentAnnotationType)) {
							this.result.add(synthesizeAnnotation((A) ann, element));
						}
						else if (ObjectUtils.nullSafeEquals(this.containerAnnotationType, currentAnnotationType)) {
							this.result.addAll(getValue(element, ann));
						}
						else if (!isInJavaLangAnnotationPackage(currentAnnotationType)) {
							process(currentAnnotationType);
						}
					}
				}
				catch (Throwable ex) {
					handleIntrospectionFailure(element, ex);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private List<A> getValue(AnnotatedElement element, Annotation annotation) {
			try {
				List<A> synthesizedAnnotations = new ArrayList<A>();
				for (A anno : (A[]) AnnotationUtils.getValue(annotation)) {
					synthesizedAnnotations.add(synthesizeAnnotation(anno, element));
				}
				return synthesizedAnnotations;
			}
			catch (Throwable ex) {
				handleIntrospectionFailure(element, ex);
			}
			// 无法从重复注解容器中读取值 -> 忽略它.
			return Collections.emptyList();
		}
	}


	/**
	 * {@code AliasDescriptor}封装了{@code @AliasFor}对给定注解属性的声明,
	 * 并包括对验证别名配置 (显式和隐式)的支持.
	 */
	private static class AliasDescriptor {

		private final Method sourceAttribute;

		private final Class<? extends Annotation> sourceAnnotationType;

		private final String sourceAttributeName;

		private final Method aliasedAttribute;

		private final Class<? extends Annotation> aliasedAnnotationType;

		private final String aliasedAttributeName;

		private final boolean isAliasPair;

		/**
		 * <em>从</em>提供的注解属性上{@code @AliasFor}的声明创建一个{@code AliasDescriptor},
		 * 并验证{@code @AliasFor}的配置.
		 * 
		 * @param attribute 使用{@code @AliasFor}注解的注解属性
		 * 
		 * @return 别名描述符, 或{@code null} 如果该属性未使用{@code @AliasFor}注解
		 */
		public static AliasDescriptor from(Method attribute) {
			AliasDescriptor descriptor = aliasDescriptorCache.get(attribute);
			if (descriptor != null) {
				return descriptor;
			}

			AliasFor aliasFor = attribute.getAnnotation(AliasFor.class);
			if (aliasFor == null) {
				return null;
			}

			descriptor = new AliasDescriptor(attribute, aliasFor);
			descriptor.validate();
			aliasDescriptorCache.put(attribute, descriptor);
			return descriptor;
		}

		@SuppressWarnings("unchecked")
		private AliasDescriptor(Method sourceAttribute, AliasFor aliasFor) {
			Class<?> declaringClass = sourceAttribute.getDeclaringClass();
			Assert.isTrue(declaringClass.isAnnotation(), "sourceAttribute must be from an annotation");

			this.sourceAttribute = sourceAttribute;
			this.sourceAnnotationType = (Class<? extends Annotation>) declaringClass;
			this.sourceAttributeName = sourceAttribute.getName();

			this.aliasedAnnotationType = (Annotation.class == aliasFor.annotation() ?
					this.sourceAnnotationType : aliasFor.annotation());
			this.aliasedAttributeName = getAliasedAttributeName(aliasFor, sourceAttribute);
			if (this.aliasedAnnotationType == this.sourceAnnotationType &&
					this.aliasedAttributeName.equals(this.sourceAttributeName)) {
				String msg = String.format("@AliasFor declaration on attribute '%s' in annotation [%s] points to " +
						"itself. Specify 'annotation' to point to a same-named attribute on a meta-annotation.",
						sourceAttribute.getName(), declaringClass.getName());
				throw new AnnotationConfigurationException(msg);
			}
			try {
				this.aliasedAttribute = this.aliasedAnnotationType.getDeclaredMethod(this.aliasedAttributeName);
			}
			catch (NoSuchMethodException ex) {
				String msg = String.format(
						"Attribute '%s' in annotation [%s] is declared as an @AliasFor nonexistent attribute '%s' in annotation [%s].",
						this.sourceAttributeName, this.sourceAnnotationType.getName(), this.aliasedAttributeName,
						this.aliasedAnnotationType.getName());
				throw new AnnotationConfigurationException(msg, ex);
			}

			this.isAliasPair = (this.sourceAnnotationType == this.aliasedAnnotationType);
		}

		private void validate() {
			// 目标注解不是元存在的?
			if (!this.isAliasPair && !isAnnotationMetaPresent(this.sourceAnnotationType, this.aliasedAnnotationType)) {
				String msg = String.format("@AliasFor declaration on attribute '%s' in annotation [%s] declares " +
						"an alias for attribute '%s' in meta-annotation [%s] which is not meta-present.",
						this.sourceAttributeName, this.sourceAnnotationType.getName(), this.aliasedAttributeName,
						this.aliasedAnnotationType.getName());
				throw new AnnotationConfigurationException(msg);
			}

			if (this.isAliasPair) {
				AliasFor mirrorAliasFor = this.aliasedAttribute.getAnnotation(AliasFor.class);
				if (mirrorAliasFor == null) {
					String msg = String.format("Attribute '%s' in annotation [%s] must be declared as an @AliasFor [%s].",
							this.aliasedAttributeName, this.sourceAnnotationType.getName(), this.sourceAttributeName);
					throw new AnnotationConfigurationException(msg);
				}

				String mirrorAliasedAttributeName = getAliasedAttributeName(mirrorAliasFor, this.aliasedAttribute);
				if (!this.sourceAttributeName.equals(mirrorAliasedAttributeName)) {
					String msg = String.format("Attribute '%s' in annotation [%s] must be declared as an @AliasFor [%s], not [%s].",
							this.aliasedAttributeName, this.sourceAnnotationType.getName(), this.sourceAttributeName,
							mirrorAliasedAttributeName);
					throw new AnnotationConfigurationException(msg);
				}
			}

			Class<?> returnType = this.sourceAttribute.getReturnType();
			Class<?> aliasedReturnType = this.aliasedAttribute.getReturnType();
			if (returnType != aliasedReturnType &&
					(!aliasedReturnType.isArray() || returnType != aliasedReturnType.getComponentType())) {
				String msg = String.format("Misconfigured aliases: attribute '%s' in annotation [%s] " +
						"and attribute '%s' in annotation [%s] must declare the same return type.",
						this.sourceAttributeName, this.sourceAnnotationType.getName(), this.aliasedAttributeName,
						this.aliasedAnnotationType.getName());
				throw new AnnotationConfigurationException(msg);
			}

			if (this.isAliasPair) {
				validateDefaultValueConfiguration(this.aliasedAttribute);
			}
		}

		private void validateDefaultValueConfiguration(Method aliasedAttribute) {
			Assert.notNull(aliasedAttribute, "aliasedAttribute must not be null");
			Object defaultValue = this.sourceAttribute.getDefaultValue();
			Object aliasedDefaultValue = aliasedAttribute.getDefaultValue();

			if (defaultValue == null || aliasedDefaultValue == null) {
				String msg = String.format("Misconfigured aliases: attribute '%s' in annotation [%s] " +
						"and attribute '%s' in annotation [%s] must declare default values.",
						this.sourceAttributeName, this.sourceAnnotationType.getName(), aliasedAttribute.getName(),
						aliasedAttribute.getDeclaringClass().getName());
				throw new AnnotationConfigurationException(msg);
			}

			if (!ObjectUtils.nullSafeEquals(defaultValue, aliasedDefaultValue)) {
				String msg = String.format("Misconfigured aliases: attribute '%s' in annotation [%s] " +
						"and attribute '%s' in annotation [%s] must declare the same default value.",
						this.sourceAttributeName, this.sourceAnnotationType.getName(), aliasedAttribute.getName(),
						aliasedAttribute.getDeclaringClass().getName());
				throw new AnnotationConfigurationException(msg);
			}
		}

		/**
		 * 根据提供的描述符验证此描述符.
		 * <p>此方法仅验证两个描述符的默认值的配置, 因为描述符的其他方面在创建时已经过验证.
		 */
		private void validateAgainst(AliasDescriptor otherDescriptor) {
			validateDefaultValueConfiguration(otherDescriptor.sourceAttribute);
		}

		/**
		 * 确定此描述符是否表示所提供的{@code metaAnnotationType}中属性的显式覆盖.
		 */
		private boolean isOverrideFor(Class<? extends Annotation> metaAnnotationType) {
			return (this.aliasedAnnotationType == metaAnnotationType);
		}

		/**
		 * 确定此描述符和提供的描述符是否有效地表示同一目标注解中相同属性的别名, 不论显式或隐式.
		 * <p>此方法从此描述符开始搜索属性覆盖层次结构, 以便检测隐式和传递隐式别名.
		 * 
		 * @return {@code true} 如果此描述符和提供的描述符有效地对同一个注解属性进行别名
		 */
		private boolean isAliasFor(AliasDescriptor otherDescriptor) {
			for (AliasDescriptor lhs = this; lhs != null; lhs = lhs.getAttributeOverrideDescriptor()) {
				for (AliasDescriptor rhs = otherDescriptor; rhs != null; rhs = rhs.getAttributeOverrideDescriptor()) {
					if (lhs.aliasedAttribute.equals(rhs.aliasedAttribute)) {
						return true;
					}
				}
			}
			return false;
		}

		public List<String> getAttributeAliasNames() {
			// 显式别名对?
			if (this.isAliasPair) {
				return Collections.singletonList(this.aliasedAttributeName);
			}

			// Else: 搜索隐式别名
			List<String> aliases = new ArrayList<String>();
			for (AliasDescriptor otherDescriptor : getOtherDescriptors()) {
				if (this.isAliasFor(otherDescriptor)) {
					this.validateAgainst(otherDescriptor);
					aliases.add(otherDescriptor.sourceAttributeName);
				}
			}
			return aliases;
		}

		private List<AliasDescriptor> getOtherDescriptors() {
			List<AliasDescriptor> otherDescriptors = new ArrayList<AliasDescriptor>();
			for (Method currentAttribute : getAttributeMethods(this.sourceAnnotationType)) {
				if (!this.sourceAttribute.equals(currentAttribute)) {
					AliasDescriptor otherDescriptor = AliasDescriptor.from(currentAttribute);
					if (otherDescriptor != null) {
						otherDescriptors.add(otherDescriptor);
					}
				}
			}
			return otherDescriptors;
		}

		public String getAttributeOverrideName(Class<? extends Annotation> metaAnnotationType) {
			Assert.notNull(metaAnnotationType, "metaAnnotationType must not be null");
			Assert.isTrue(Annotation.class != metaAnnotationType,
					"metaAnnotationType must not be [java.lang.annotation.Annotation]");

			// 从当前属性开始搜索属性覆盖层次结构
			for (AliasDescriptor desc = this; desc != null; desc = desc.getAttributeOverrideDescriptor()) {
				if (desc.isOverrideFor(metaAnnotationType)) {
					return desc.aliasedAttributeName;
				}
			}

			// Else: 不同元注解的显式属性覆盖
			return null;
		}

		private AliasDescriptor getAttributeOverrideDescriptor() {
			if (this.isAliasPair) {
				return null;
			}
			return AliasDescriptor.from(this.aliasedAttribute);
		}

		/**
		 * 获取通过提供的{@code attribute}上提供的{@link AliasFor @AliasFor}注解配置的别名属性的名称,
		 * 如果没有指定别名, 则获取原始属性 (表示引用转到元注解的同名属性).
		 * <p>此方法返回{@code @AliasFor}的{@code attribute}或{@code value}属性的值,
		 * 确保仅声明其中一个属性, 同时确保至少有一个属性已被声明.
		 * 
		 * @param aliasFor 要从中检索别名属性名称的{@code @AliasFor}注解
		 * @param attribute 使用{@code @AliasFor}注解的属性
		 * 
		 * @return 别名属性的名称 (never {@code null} or empty)
		 * @throws AnnotationConfigurationException 如果检测到{@code @AliasFor}的无效配置
		 */
		private String getAliasedAttributeName(AliasFor aliasFor, Method attribute) {
			String attributeName = aliasFor.attribute();
			String value = aliasFor.value();
			boolean attributeDeclared = StringUtils.hasText(attributeName);
			boolean valueDeclared = StringUtils.hasText(value);

			// 确保用户未在@AliasFor中声明'value' 和 'attribute'
			if (attributeDeclared && valueDeclared) {
				String msg = String.format("In @AliasFor declared on attribute '%s' in annotation [%s], attribute 'attribute' " +
						"and its alias 'value' are present with values of [%s] and [%s], but only one is permitted.",
						attribute.getName(), attribute.getDeclaringClass().getName(), attributeName, value);
				throw new AnnotationConfigurationException(msg);
			}

			// 默认情况下显式属性名称或指向同名属性
			attributeName = (attributeDeclared ? attributeName : value);
			return (StringUtils.hasText(attributeName) ? attributeName.trim() : attribute.getName());
		}

		@Override
		public String toString() {
			return String.format("%s: @%s(%s) is an alias for @%s(%s)", getClass().getSimpleName(),
					this.sourceAnnotationType.getSimpleName(), this.sourceAttributeName,
					this.aliasedAnnotationType.getSimpleName(), this.aliasedAttributeName);
		}
	}


	private static class DefaultValueHolder {

		final Object defaultValue;

		public DefaultValueHolder(Object defaultValue) {
			this.defaultValue = defaultValue;
		}
	}
}
