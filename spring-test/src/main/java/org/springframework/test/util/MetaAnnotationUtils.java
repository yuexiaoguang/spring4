package org.springframework.test.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@code MetaAnnotationUtils}是一组工具方法, 它补充了{@link AnnotationUtils}中已有的标准支持.
 *
 * <p>虽然{@code AnnotationUtils}为<em>获取</em>或<em>查找</em>注解提供了实用工具,
 * 但{@code MetaAnnotationUtils}更进了一步, 支持确定声明注解的<em>根类</em>, 直接声明或间接通过<em>组合注解</em>.
 * 此附加信息封装在{@link AnnotationDescriptor}中.
 *
 * <p><em>Spring TestContext Framework</em>需要{@code AnnotationDescriptor}提供的附加信息,
 * 以便能够支持注解的类层次结构遍历, 例如
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration},
 * {@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners},
 * 和{@link org.springframework.test.context.ActiveProfiles @ActiveProfiles},
 * 它支持合并和覆盖各种<em>继承</em>注解属性
 * (e.g. {@link org.springframework.test.context.ContextConfiguration#inheritLocations}).
 */
public abstract class MetaAnnotationUtils {

	/**
	 * 如果在给定的类本身上找不到注解, 则在提供的{@link Class}上找到
	 * 所提供的{@code annotationType}的{@link AnnotationDescriptor}, 遍历其注解, 接口和超类.
	 * <p>此方法显式处理类级别注解, 这些注解未声明为{@linkplain java.lang.annotation.Inherited inherited},
	 * <em>以及元注解</em>.
	 * <p>该算法如下操作:
	 * <ol>
	 * <li>搜索给定类的注解, 如果找到则返回相应的{@code AnnotationDescriptor}.
	 * <li>递归搜索给定类声明的所有注解.
	 * <li>递归搜索给定类实现的所有接口.
	 * <li>递归搜索给定类的超类层次结构.
	 * </ol>
	 * <p>在此上下文中, 术语<em>递归</em>意味着搜索过程继续返回步骤 #1 当前注解, 接口或超类作为类来查找注解.
	 * 
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 如果找到注释, 则为对应的注解描述符; 否则{@code null}
	 */
	public static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(
			Class<?> clazz, Class<T> annotationType) {

		return findAnnotationDescriptor(clazz, new HashSet<Annotation>(), annotationType);
	}

	/**
	 * 执行{@link #findAnnotationDescriptor(Class, Class)}的搜索算法,
	 * 通过跟踪哪些注解已经<em>访问</em>来避免无限递归.
	 * 
	 * @param clazz 要查找注解的类
	 * @param visited 已经访问过的注解集合
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 如果找到注解, 则为对应的注解描述符; 否则{@code null}
	 */
	private static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(
			Class<?> clazz, Set<Annotation> visited, Class<T> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null");
		if (clazz == null || Object.class == clazz) {
			return null;
		}

		// Declared locally?
		if (AnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)) {
			return new AnnotationDescriptor<T>(clazz, clazz.getAnnotation(annotationType));
		}

		// 在组合注解上声明 (i.e., 作为元注解)?
		for (Annotation composedAnn : clazz.getDeclaredAnnotations()) {
			Class<? extends Annotation> composedType = composedAnn.annotationType();
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(composedType.getName()) && visited.add(composedAnn)) {
				AnnotationDescriptor<T> descriptor = findAnnotationDescriptor(composedType, visited, annotationType);
				if (descriptor != null) {
					return new AnnotationDescriptor<T>(
							clazz, descriptor.getDeclaringClass(), composedAnn, descriptor.getAnnotation());
				}
			}
		}

		// Declared on interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			AnnotationDescriptor<T> descriptor = findAnnotationDescriptor(ifc, visited, annotationType);
			if (descriptor != null) {
				return new AnnotationDescriptor<T>(clazz, descriptor.getDeclaringClass(),
						descriptor.getComposedAnnotation(), descriptor.getAnnotation());
			}
		}

		// Declared on a superclass?
		return findAnnotationDescriptor(clazz.getSuperclass(), visited, annotationType);
	}

	/**
	 * 找到指定{@code clazz}的继承层次结构中的第一个{@link Class}的{@link UntypedAnnotationDescriptor}
	 * (包括指定的{@code clazz}本身), 它声明了至少一个指定的{@code annotationTypes}.
	 * <p>如果在给定的类本身上找不到注解, 则此方法遍历指定的{@code clazz}的注解, 接口和超类.
	 * <p>此方法显式处理类级别注解, 这些注解未声明为{@linkplain java.lang.annotation.Inherited inherited} <em>以及元注解</em>.
	 * <p>该算法如下操作:
	 * <ol>
	 * <li>在给定类中搜索其中一个注解类型的本地声明, 如果找到则返回相应的{@code UntypedAnnotationDescriptor}.
	 * <li>递归搜索给定类声明的所有注解.
	 * <li>递归搜索给定类实现的所有接口.
	 * <li>递归搜索给定类的超类层次结构.
	 * </ol>
	 * <p>在此上下文中, 术语<em>递归</em>意味着搜索过程继续返回步骤 #1 当前注解, 接口或超类作为类来查找注释.
	 * 
	 * @param clazz 要查找注解的类
	 * @param annotationTypes 要查找的注解类型
	 * 
	 * @return 如果找到其中一个注解, 则返回相应的注解描述符; 否则为{@code null}
	 */
	@SuppressWarnings("unchecked")
	public static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(
			Class<?> clazz, Class<? extends Annotation>... annotationTypes) {

		return findAnnotationDescriptorForTypes(clazz, new HashSet<Annotation>(), annotationTypes);
	}

	/**
	 * 执行{@link #findAnnotationDescriptorForTypes(Class, Class...)}的搜索算法,
	 * 通过跟踪哪些注解已经<em>访问</em>来避免无限递归.
	 * 
	 * @param clazz 要查找注解的类
	 * @param visited 已经访问过的注解集合
	 * @param annotationTypes 要查找的注解类型
	 * 
	 * @return 如果找到其中一个注解, 则返回相应的注解描述符; 否则为{@code null}
	 */
	@SuppressWarnings("unchecked")
	private static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(Class<?> clazz,
			Set<Annotation> visited, Class<? extends Annotation>... annotationTypes) {

		assertNonEmptyAnnotationTypeArray(annotationTypes, "The list of annotation types must not be empty");
		if (clazz == null || Object.class == clazz) {
			return null;
		}

		// Declared locally?
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (AnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)) {
				return new UntypedAnnotationDescriptor(clazz, clazz.getAnnotation(annotationType));
			}
		}

		// 在组合注解上声明 (i.e., 作为元注解)?
		for (Annotation composedAnnotation : clazz.getDeclaredAnnotations()) {
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(composedAnnotation) && visited.add(composedAnnotation)) {
				UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
						composedAnnotation.annotationType(), visited, annotationTypes);
				if (descriptor != null) {
					return new UntypedAnnotationDescriptor(clazz, descriptor.getDeclaringClass(),
							composedAnnotation, descriptor.getAnnotation());
				}
			}
		}

		// Declared on interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(ifc, visited, annotationTypes);
			if (descriptor != null) {
				return new UntypedAnnotationDescriptor(clazz, descriptor.getDeclaringClass(),
						descriptor.getComposedAnnotation(), descriptor.getAnnotation());
			}
		}

		// Declared on a superclass?
		return findAnnotationDescriptorForTypes(clazz.getSuperclass(), visited, annotationTypes);
	}

	private static void assertNonEmptyAnnotationTypeArray(Class<?>[] annotationTypes, String message) {
		if (ObjectUtils.isEmpty(annotationTypes)) {
			throw new IllegalArgumentException(message);
		}
		for (Class<?> clazz : annotationTypes) {
			if (!Annotation.class.isAssignableFrom(clazz)) {
				throw new IllegalArgumentException("Array elements must be of type Annotation");
			}
		}
	}


	/**
	 * {@link Annotation}的描述符, 包括<em>声明</em>注解的{@linkplain #getDeclaringClass() class},
	 * 以及实际的{@linkplain #getAnnotation() 注解}实例.
	 * <p>如果注释用作元注解, 则描述符还包括注解所在的{@linkplain #getComposedAnnotation() 组合注解}.
	 * 在这种情况下, <em>根声明类</em>不是直接使用注解, 而是通过组合注解间接注解.
	 * <p>给出以下示例, 如果在{@code TransactionalTests}类上搜索{@code @Transactional}注解,
	 * 那么{@code AnnotationDescriptor}的属性将如下所示.
	 * <ul>
	 * <li>rootDeclaringClass: {@code TransactionalTests} 类对象</li>
	 * <li>declaringClass: {@code TransactionalTests} 类对象</li>
	 * <li>composedAnnotation: {@code null}</li>
	 * <li>annotation: {@code Transactional} 注解的实例</li>
	 * </ul>
	 * <pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * public class TransactionalTests { }
	 * </pre>
	 * <p>给出以下示例, 如果在{@code UserRepositoryTests}类上搜索{@code @Transactional}注解,
	 * 那么{@code AnnotationDescriptor}的属性将如下所示.
	 * <ul>
	 * <li>rootDeclaringClass: {@code UserRepositoryTests} 类对象</li>
	 * <li>declaringClass: {@code RepositoryTests} 类对象</li>
	 * <li>composedAnnotation: {@code RepositoryTests}注解的实例</li>
	 * <li>annotation: {@code Transactional}注解的实例</li>
	 * </ul>
	 * <pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * &#064;Retention(RetentionPolicy.RUNTIME)
	 * public &#064;interface RepositoryTests { }
	 *
	 * &#064;RepositoryTests
	 * public class UserRepositoryTests { }
	 * </pre>
	 */
	public static class AnnotationDescriptor<T extends Annotation> {

		private final Class<?> rootDeclaringClass;

		private final Class<?> declaringClass;

		private final Annotation composedAnnotation;

		private final T annotation;

		private final AnnotationAttributes annotationAttributes;

		public AnnotationDescriptor(Class<?> rootDeclaringClass, T annotation) {
			this(rootDeclaringClass, rootDeclaringClass, null, annotation);
		}

		public AnnotationDescriptor(Class<?> rootDeclaringClass, Class<?> declaringClass,
				Annotation composedAnnotation, T annotation) {

			Assert.notNull(rootDeclaringClass, "'rootDeclaringClass' must not be null");
			Assert.notNull(annotation, "Annotation must not be null");
			this.rootDeclaringClass = rootDeclaringClass;
			this.declaringClass = declaringClass;
			this.composedAnnotation = composedAnnotation;
			this.annotation = annotation;
			this.annotationAttributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
					rootDeclaringClass, annotation.annotationType().getName(), false, false);
		}

		public Class<?> getRootDeclaringClass() {
			return this.rootDeclaringClass;
		}

		public Class<?> getDeclaringClass() {
			return this.declaringClass;
		}

		public T getAnnotation() {
			return this.annotation;
		}

		/**
		 * 将此描述符中合并的{@link #getAnnotationAttributes AnnotationAttributes}
		 * 合成回目标{@linkplain #getAnnotationType 注解类型}的注解.
		 */
		@SuppressWarnings("unchecked")
		public T synthesizeAnnotation() {
			return AnnotationUtils.synthesizeAnnotation(
					getAnnotationAttributes(), (Class<T>) getAnnotationType(), getRootDeclaringClass());
		}

		public Class<? extends Annotation> getAnnotationType() {
			return this.annotation.annotationType();
		}

		public AnnotationAttributes getAnnotationAttributes() {
			return this.annotationAttributes;
		}

		public Annotation getComposedAnnotation() {
			return this.composedAnnotation;
		}

		public Class<? extends Annotation> getComposedAnnotationType() {
			return (this.composedAnnotation != null ? this.composedAnnotation.annotationType() : null);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("rootDeclaringClass", this.rootDeclaringClass)
					.append("declaringClass", this.declaringClass)
					.append("composedAnnotation", this.composedAnnotation)
					.append("annotation", this.annotation)
					.toString();
		}
	}


	/**
	 * {@code AnnotationDescriptor}的无类型扩展, 用于描述几种候选注解类型之一的声明, 其中无法预先确定实际注解类型.
	 */
	public static class UntypedAnnotationDescriptor extends AnnotationDescriptor<Annotation> {

		public UntypedAnnotationDescriptor(Class<?> rootDeclaringClass, Annotation annotation) {
			this(rootDeclaringClass, rootDeclaringClass, null, annotation);
		}

		public UntypedAnnotationDescriptor(Class<?> rootDeclaringClass, Class<?> declaringClass,
				Annotation composedAnnotation, Annotation annotation) {

			super(rootDeclaringClass, declaringClass, composedAnnotation, annotation);
		}

		/**
		 * 抛出{@link UnsupportedOperationException}, 因为{@code UntypedAnnotationDescriptor}中
		 * {@link #getAnnotationAttributes AnnotationAttributes}表示的注解类型未知.
		 */
		@Override
		public Annotation synthesizeAnnotation() {
			throw new UnsupportedOperationException(
					"getMergedAnnotation() is unsupported in UntypedAnnotationDescriptor");
		}
	}

}
