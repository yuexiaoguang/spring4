package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 用于在{@link AnnotatedElement AnnotatedElements}上查找注解, 元注解和可重复注解的常规方法.
 *
 * <p>{@code AnnotatedElementUtils}定义了Spring的元注解编程模型的公共API, 支持<em>注解属性覆盖</em>.
 * 如果不需要支持注解属性覆盖, 请考虑使用{@link AnnotationUtils}.
 *
 * <p>请注意, JDK的内省工具本身不提供此类的功能.
 *
 * <h3>注解属性覆盖</h3>
 * <p>{@code getMergedAnnotationAttributes()}, {@code getMergedAnnotation()},
 * {@code getAllMergedAnnotations()}, {@code getMergedRepeatableAnnotations()},
 * {@code findMergedAnnotationAttributes()}, {@code findMergedAnnotation()},
 * {@code findAllMergedAnnotations()}, 和{@code findMergedRepeatableAnnotations()}
 * 方法的所有变体都提供了对<em>组合注解</em>中<em>属性覆盖</em>的元注解的支持.
 *
 * <h3>Find vs. Get语义</h3>
 * <p>此类中的方法使用的搜索算法遵循<em>find</em>或<em>get</em>语义.
 * 有关使用哪种搜索算法的详细信息, 请参阅每个方法的javadoc.
 *
 * <p><strong>Get 语义</strong>仅限于在{@code AnnotatedElement}上搜索<em>存在</em>的注解
 * (i.e. 在本地声明, 或{@linkplain java.lang.annotation.Inherited inherited}),
 * 或在{@code AnnotatedElement}<em>上面</em>的注解层次结构声明的注解.
 *
 * <p><strong>Find 语义</strong>更详尽, 提供<em>get 语义</em>以及对以下内容的支持:
 *
 * <ul>
 * <li>如果带注解的元素是类, 则在接口上搜索
 * <li>如果带注解的元素是类, 则在超类上搜索
 * <li>如果带注解的元素是方法, 则解析桥接方法
 * <li>如果带注解的元素是方法, 则在接口中搜索方法
 * <li>如果带注解的元素是方法, 则在超类中搜索方法
 * </ul>
 *
 * <h3>对{@code @Inherited}的支持</h3>
 * <p><em>get 语义</em>之后的方法将遵守Java的 {@link java.lang.annotation.Inherited @Inherited}注解的约定,
 * 除了本地声明的注解(包括自定义组合注解) 优先于继承的注解.
 * 相比之下, <em>find 语义</em>之后的方法将完全忽略{@code @Inherited}的存在, 因为<em>find</em>搜索算法手动遍历类型和方法层次结构,
 * 从而隐式支持注解继承而无需{@code @Inherited}.
 */
public class AnnotatedElementUtils {

	/**
	 * {@code null}常量用于表示搜索算法应该继续.
	 */
	private static final Boolean CONTINUE = null;

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	private static final Processor<Boolean> alwaysTrueAnnotationProcessor = new AlwaysTrueBooleanAnnotationProcessor();


	/**
	 * 为给定的注解构建一个适配的{@link AnnotatedElement}, 通常用于{@link AnnotatedElementUtils}上的其他方法.
	 * 
	 * @param annotations 要通过{@code AnnotatedElement}公开的注解
	 */
	public static AnnotatedElement forAnnotations(final Annotation... annotations) {
		return new AnnotatedElement() {
			@Override
			@SuppressWarnings("unchecked")
			public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
				for (Annotation ann : annotations) {
					if (ann.annotationType() == annotationClass) {
						return (T) ann;
					}
				}
				return null;
			}
			@Override
			public Annotation[] getAnnotations() {
				return annotations;
			}
			@Override
			public Annotation[] getDeclaredAnnotations() {
				return annotations;
			}
		};
	}

	/**
	 * 在提供的{@link AnnotatedElement}上的注解(指定的{@code annotationType})上获取所有<em>存在的</em>元注解类型的完全限定类名.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要在其上查找元注解的注解类型
	 * 
	 * @return 注解中存在的所有元注解的名称, 或{@code null}
	 */
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "'annotationType' must not be null");

		return getMetaAnnotationTypes(element, element.getAnnotation(annotationType));
	}

	/**
	 * 在提供的{@link AnnotatedElement}上的注解(指定的{@code annotationName})上获取所有<em>存在的</em>元注解类型的完全限定类名.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要在其上查找元注解的注解类型的完全限定类名
	 * 
	 * @return 注解中存在的所有元注解的名称, 或{@code null}
	 */
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.hasLength(annotationName, "'annotationName' must not be null or empty");

		return getMetaAnnotationTypes(element, AnnotationUtils.getAnnotation(element, annotationName));
	}

	private static Set<String> getMetaAnnotationTypes(AnnotatedElement element, Annotation composed) {
		if (composed == null) {
			return null;
		}

		try {
			final Set<String> types = new LinkedHashSet<String>();
			searchWithGetSemantics(composed.annotationType(), null, null, null, new SimpleAnnotationProcessor<Object>(true) {
					@Override
					public Object process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
						types.add(annotation.annotationType().getName());
						return CONTINUE;
					}
				}, new HashSet<AnnotatedElement>(), 1);
			return (!types.isEmpty() ? types : null);
		}
		catch (Throwable ex) {
			AnnotationUtils.rethrowAnnotationConfigurationException(ex);
			throw new IllegalStateException("Failed to introspect annotations on " + element, ex);
		}
	}

	/**
	 * 确定提供的{@link AnnotatedElement}是否使用<em>组合注解</em>进行注解,
	 * 该注解使用指定的{@code annotationType}的注解进行元注解.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的元注解类型
	 * 
	 * @return {@code true}如果存在匹配的元注解
	 */
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "'annotationType' must not be null");

		return hasMetaAnnotationTypes(element, annotationType, null);
	}

	/**
	 * 确定提供的{@link AnnotatedElement}是否使用<em>组合注解</em>进行注解,
	 * 该注解使用指定的{@code annotationName}的注解进行元注解.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要查找的元注解类型的完全限定类名
	 * 
	 * @return {@code true}如果存在匹配的元注解
	 */
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.hasLength(annotationName, "'annotationName' must not be null or empty");

		return hasMetaAnnotationTypes(element, null, annotationName);
	}

	private static boolean hasMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType,
			String annotationName) {

		return Boolean.TRUE.equals(
			searchWithGetSemantics(element, annotationType, annotationName, new SimpleAnnotationProcessor<Boolean>() {
				@Override
				public Boolean process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
					return (metaDepth > 0 ? Boolean.TRUE : CONTINUE);
				}
			}));
	}

	/**
	 * 确定指定的{@code annotationType}的注解是否<em>存在</em>在提供的{@link AnnotatedElement}上,
	 * 或在指定元素<em>上方</em>的注解层次结构中.
	 * <p>如果此方法返回{@code true}, 那么{@link #getMergedAnnotationAttributes}将返回非null值.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return {@code true} 如果存在匹配的注解
	 */
	public static boolean isAnnotated(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "'annotationType' must not be null");

		// Shortcut: directly present on the element, with no processing needed?
		if (element.isAnnotationPresent(annotationType)) {
			return true;
		}
		return Boolean.TRUE.equals(searchWithGetSemantics(element, annotationType, null, alwaysTrueAnnotationProcessor));
	}

	/**
	 * 确定指定的{@code annotationName}的注解是否<em>存在</em>在提供的{@link AnnotatedElement}上,
	 * 或在指定元素<em>上方</em>的注解层次结构中.
	 * <p>如果此方法返回{@code true}, 那么{@link #getMergedAnnotationAttributes}将返回非null值.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return {@code true} 如果存在匹配的注解
	 */
	public static boolean isAnnotated(AnnotatedElement element, String annotationName) {
		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.hasLength(annotationName, "'annotationName' must not be null or empty");

		return Boolean.TRUE.equals(searchWithGetSemantics(element, null, annotationName, alwaysTrueAnnotationProcessor));
	}

	/**
	 * @deprecated As of Spring Framework 4.2, use {@link #getMergedAnnotationAttributes(AnnotatedElement, String)} instead.
	 */
	@Deprecated
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationName) {
		return getMergedAnnotationAttributes(element, annotationName);
	}

	/**
	 * @deprecated As of Spring Framework 4.2, use {@link #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)} instead.
	 */
	@Deprecated
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationName,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		return getMergedAnnotationAttributes(element, annotationName, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationType}的第一个注解,
	 * 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法委托给{@link #getMergedAnnotationAttributes(AnnotatedElement, String)}.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 合并后的{@code AnnotationAttributes}, 或{@code null}
	 */
	public static AnnotationAttributes getMergedAnnotationAttributes(
			AnnotatedElement element, Class<? extends Annotation> annotationType) {

		Assert.notNull(annotationType, "'annotationType' must not be null");
		AnnotationAttributes attributes = searchWithGetSemantics(element, annotationType, null,
				new MergedAnnotationAttributesProcessor());
		AnnotationUtils.postProcessAnnotationAttributes(element, attributes, false, false);
		return attributes;
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationName}的第一个注解,
	 * 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法委托给{@link #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)},
	 * 其中{@code classValuesAsString}和{@code nestedAnnotationsAsMap}属性的值都为{@code false}.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 合并后的{@code AnnotationAttributes}, 或{@code null}
	 */
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element, String annotationName) {
		return getMergedAnnotationAttributes(element, annotationName, false, false);
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationName}的第一个注解,
	 * 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并.
	 * <p>注解层次结构中较低级别的属性覆盖较高级别的同名属性,
	 * 并且完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>与{@link #getAllAnnotationAttributes}相反, 一旦找到了指定的{@code annotationName}的第一个注解,
	 * 此方法使用的搜索算法将停止搜索注解层次结构.
	 * 因此, 将忽略指定的{@code annotationName}的附加注解.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是否将Class引用转换为字符串, 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套的Annotation实例转换为{@code AnnotationAttributes} Map, 或将它们保存为Annotation实例
	 * 
	 * @return 合并后的{@code AnnotationAttributes}, 或{@code null}
	 */
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		Assert.hasLength(annotationName, "'annotationName' must not be null or empty");
		AnnotationAttributes attributes = searchWithGetSemantics(element, null, annotationName,
				new MergedAnnotationAttributesProcessor(classValuesAsString, nestedAnnotationsAsMap));
		AnnotationUtils.postProcessAnnotationAttributes(element, attributes, classValuesAsString, nestedAnnotationsAsMap);
		return attributes;
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationType}的第一个注解,
	 * 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并, 并将结果合成回指定的{@code annotationType}的注解.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法委托给{@link #getMergedAnnotationAttributes(AnnotatedElement, Class)}
	 * 和{@link AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)}.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 合并的, 合成的 {@code Annotation}, 或{@code null}
	 */
	public static <A extends Annotation> A getMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");

		// Shortcut: 直接出现在元素上, 不需要合并?
		if (!(element instanceof Class)) {
			// 不要对Class使用此快捷方式: 继承注解将优先于本地声明的组合注解.
			A annotation = element.getAnnotation(annotationType);
			if (annotation != null) {
				return AnnotationUtils.synthesizeAnnotation(annotation, element);
			}
		}

		// 彻底检索合并的注解属性...
		AnnotationAttributes attributes = getMergedAnnotationAttributes(element, annotationType);
		return AnnotationUtils.synthesizeAnnotation(attributes, annotationType, element);
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationType}的<strong>所有</strong>注解;
	 * 并且对于找到的每个注解, 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并,
	 * 并将结果合成回指定的{@code annotationType}的注解.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素 (never {@code null})
	 * @param annotationType 要查找的注解类型 (never {@code null})
	 * 
	 * @return 所有找到的合并的, 合成的 {@code Annotations}, 或空集合
	 */
	public static <A extends Annotation> Set<A> getAllMergedAnnotations(AnnotatedElement element,
			Class<A> annotationType) {

		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "'annotationType' must not be null");

		MergedAnnotationAttributesProcessor processor = new MergedAnnotationAttributesProcessor(false, false, true);
		searchWithGetSemantics(element, annotationType, null, processor);
		return postProcessAndSynthesizeAggregatedResults(element, annotationType, processor.getAggregatedResults());
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationType}的所有<em>可重复的注解</em>;
	 * 并且对于找到的每个注解, 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并,
	 * 并将结果合成回指定的{@code annotationType}的注解.
	 * <p>保存可重复注解的容器类型将通过{@link java.lang.annotation.Repeatable}查找.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素 (never {@code null})
	 * @param annotationType 要查找的注解类型 (never {@code null})
	 * 
	 * @return 找到的所有合并的可重复{@code Annotations}, 或空集合, 如果没有找到
	 * @throws IllegalArgumentException 如果{@code element}或{@code annotationType}是{@code null}, 或者如果无法解析容器类型
	 */
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType) {

		return getMergedRepeatableAnnotations(element, annotationType, null);
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationType}的所有<em>可重复的注解</em>;
	 * 并且对于找到的每个注解, 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并,
	 * 并将结果合成回指定的{@code annotationType}的注解.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素 (never {@code null})
	 * @param annotationType 要查找的注解类型 (never {@code null})
	 * @param containerType 保存注解的容器的类型;
	 * 可能是{@code null}, 如果应通过{@link java.lang.annotation.Repeatable}查找容器类型
	 * 
	 * @return 找到的所有合并的可重复{@code Annotations}, 或空集合, 如果没有找到
	 * @throws IllegalArgumentException 如果{@code element}或{@code annotationType}是{@code null}, 或者如果无法解析容器类型
	 * @throws AnnotationConfigurationException 如果提供的{@code containerType}不是指定{@code annotationType}的有效容器注解
	 */
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType, Class<? extends Annotation> containerType) {

		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "'annotationType' must not be null");

		if (containerType == null) {
			containerType = resolveContainerType(annotationType);
		}
		else {
			validateContainerType(annotationType, containerType);
		}

		MergedAnnotationAttributesProcessor processor = new MergedAnnotationAttributesProcessor(false, false, true);
		searchWithGetSemantics(element, annotationType, null, containerType, processor);
		return postProcessAndSynthesizeAggregatedResults(element, annotationType, processor.getAggregatedResults());
	}

	/**
	 * 获取所提供的{@link AnnotatedElement}上注解层次结构中指定的{@code annotationName}的<strong>所有</strong>注解的注解属性,
	 * 并将结果存储在{@link MultiValueMap}中.
	 * <p>Note: 与{@link #getMergedAnnotationAttributes(AnnotatedElement, String)}相反, 此方法<em>不</em>支持属性覆盖.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 属性名称作为Key的{@link MultiValueMap}, 包含找到的所有注解的注解属性, 或{@code null}
	 */
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element, String annotationName) {
		return getAllAnnotationAttributes(element, annotationName, false, false);
	}

	/**
	 * 获取所提供的{@link AnnotatedElement}上注解层次结构中指定的{@code annotationName}的<strong>所有</strong>注解的注解属性,
	 * 并将结果存储在{@link MultiValueMap}中.
	 * <p>Note: 与{@link #getMergedAnnotationAttributes(AnnotatedElement, String)}相反, 此方法<em>不</em>支持属性覆盖.
	 * <p>此方法遵循<em>get 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是否将Class引用转换为字符串, 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套的Annotation实例转换为{@code AnnotationAttributes} Map, 或将它们保存为Annotation实例
	 * 
	 * @return 属性名称作为Key的{@link MultiValueMap}, 包含找到的所有注解的注解属性, 或{@code null}
	 */
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
			String annotationName, final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		final MultiValueMap<String, Object> attributesMap = new LinkedMultiValueMap<String, Object>();

		searchWithGetSemantics(element, null, annotationName, new SimpleAnnotationProcessor<Object>() {
			@Override
			public Object process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
				AnnotationAttributes annotationAttributes = AnnotationUtils.getAnnotationAttributes(
						annotation, classValuesAsString, nestedAnnotationsAsMap);
				for (Map.Entry<String, Object> entry : annotationAttributes.entrySet()) {
					attributesMap.add(entry.getKey(), entry.getValue());
				}
				return CONTINUE;
			}
		});

		return (!attributesMap.isEmpty() ? attributesMap : null);
	}

	/**
	 * 确定指定的{@code annotationType}的注解是否<em>可用</em>, 在提供的{@link AnnotatedElement}上,
	 * 或在指定元素<em>上</em>的注解层次结构中.
	 * <p>如果此方法返回{@code true}, 那么{@link #findMergedAnnotationAttributes}将返回非null值.
	 * <p>此方法遵循<em>find 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return {@code true} 如果存在匹配的注解
	 */
	public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "'annotationType' must not be null");

		// Shortcut: 直接出现在元素上, 无需处理?
		if (element.isAnnotationPresent(annotationType)) {
			return true;
		}
		return Boolean.TRUE.equals(searchWithFindSemantics(element, annotationType, null, alwaysTrueAnnotationProcessor));
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中查找指定{@code annotationType}的第一个注解,
	 * 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并.
	 * <p>注解层次结构中较低级别的属性会覆盖较高级别的同名属性,
	 * 并且完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>与{@link #getAllAnnotationAttributes}相反, 一旦找到了指定的{@code annotationType}的第一个注解,
	 * 此方法使用的搜索算法将停止搜索注解层次结构.
	 * 因此, 将忽略指定的{@code annotationType}的附加注解.
	 * <p>此方法遵循<em>find 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param classValuesAsString 是否将Class引用转换为字符串, 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套的Annotation实例转换为{@code AnnotationAttributes} Map, 或将它们保存为Annotation实例
	 * 
	 * @return 合并后的{@code AnnotationAttributes}, 或{@code null}如果未找到
	 */
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			Class<? extends Annotation> annotationType, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		AnnotationAttributes attributes = searchWithFindSemantics(element, annotationType, null,
				new MergedAnnotationAttributesProcessor(classValuesAsString, nestedAnnotationsAsMap));
		AnnotationUtils.postProcessAnnotationAttributes(element, attributes, classValuesAsString, nestedAnnotationsAsMap);
		return attributes;
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中查找指定{@code annotationName}的第一个注解,
	 * 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并.
	 * <p>注解层次结构中较低级别的属性会覆盖较高级别的同名属性,
	 * 并且完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>与{@link #getAllAnnotationAttributes}相反, 一旦找到了指定的{@code annotationName}的第一个注解,
	 * 此方法使用的搜索算法将停止搜索注解层次结构.
	 * 因此, 将忽略指定的{@code annotationName}的附加注解.
	 * <p>此方法遵循<em>find 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是否将Class引用转换为字符串, 或将它们保存为Class引用
	 * @param nestedAnnotationsAsMap 是否将嵌套的Annotation实例转换为{@code AnnotationAttributes} Map, 或将它们保存为Annotation实例
	 * 
	 * @return 合并后的{@code AnnotationAttributes}, 或{@code null}如果未找到
	 */
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		AnnotationAttributes attributes = searchWithFindSemantics(element, null, annotationName,
				new MergedAnnotationAttributesProcessor(classValuesAsString, nestedAnnotationsAsMap));
		AnnotationUtils.postProcessAnnotationAttributes(element, attributes, classValuesAsString, nestedAnnotationsAsMap);
		return attributes;
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中查找指定{@code annotationType}的第一个注解,
	 * 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并, 并将结果合成回指定的{@code annotationType}的注解.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法遵循<em>find 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 合并的, 合成的 {@code Annotation}, 或{@code null}如果未找到
	 */
	public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");

		// Shortcut: 直接出现在元素上, 不需要合并?
		if (!(element instanceof Class)) {
			// 不要对Class使用此快捷方式: 继承注解将优先于本地声明的组合注解.
			A annotation = element.getAnnotation(annotationType);
			if (annotation != null) {
				return AnnotationUtils.synthesizeAnnotation(annotation, element);
			}
		}

		// 彻底检索合并的注解属性...
		AnnotationAttributes attributes = findMergedAnnotationAttributes(element, annotationType, false, false);
		return AnnotationUtils.synthesizeAnnotation(attributes, annotationType, element);
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中查找指定{@code annotationName}的第一个注解,
	 * 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并, 并将结果合成回指定的{@code annotationName}的注解.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法委托给{@link #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)}
	 * (将{@code classValuesAsString}和{@code nestedAnnotationsAsMap}设置为{@code false})
	 * 和{@link AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)}.
	 * <p>此方法遵循<em>find 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 合并的, 合成的 {@code Annotation}, 或{@code null}如果未找到
	 * @deprecated As of Spring Framework 4.2.3, use {@link #findMergedAnnotation(AnnotatedElement, Class)} instead.
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, String annotationName) {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(element, annotationName, false, false);
		return AnnotationUtils.synthesizeAnnotation(attributes, (Class<A>) attributes.annotationType(), element);
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中查找指定{@code annotationType}的<strong>所有</strong>注解;
	 * 并且对于找到的每个注解, 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并,
	 * 并将结果合成回指定的{@code annotationType}的注解.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法遵循<em>find 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素 (never {@code null})
	 * @param annotationType 要查找的注解类型  (never {@code null})
	 * 
	 * @return 所有找到的合并的, 合成的 {@code Annotations}, 或空集合
	 */
	public static <A extends Annotation> Set<A> findAllMergedAnnotations(AnnotatedElement element,
			Class<A> annotationType) {

		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "'annotationType' must not be null");

		MergedAnnotationAttributesProcessor processor = new MergedAnnotationAttributesProcessor(false, false, true);
		searchWithFindSemantics(element, annotationType, null, processor);
		return postProcessAndSynthesizeAggregatedResults(element, annotationType, processor.getAggregatedResults());
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationType}的所有<em>可重复的注解</em>;
	 * 并且对于找到的每个注解, 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并,
	 * 并将结果合成回指定的{@code annotationType}的注解.
	 * <p>保存可重复注解的容器类型将通过{@link java.lang.annotation.Repeatable}查找.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法遵循<em>find 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素 (never {@code null})
	 * @param annotationType 要查找的注解类型 (never {@code null})
	 * 
	 * @return 找到的所有合并的可重复{@code Annotations}, 或空集合, 如果没有找到
	 * @throws IllegalArgumentException 如果{@code element}或{@code annotationType}是{@code null}, 或者如果无法解析容器类型
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType) {

		return findMergedRepeatableAnnotations(element, annotationType, null);
	}

	/**
	 * 在提供的{@code element}<em>上</em>的注解层次结构中获取指定{@code annotationType}的所有<em>可重复的注解</em>;
	 * 并且对于找到的每个注解, 并将该注解的属性与来自注解层次结构中较低级别的注解的<em>匹配</em>属性合并,
	 * 并将结果合成回指定的{@code annotationType}的注解.
	 * <p>完全支持{@link AliasFor @AliasFor}语义, 包括单个注解和注解层次结构中的注解.
	 * <p>此方法遵循<em>find 语义</em>, 如{@linkplain AnnotatedElementUtils class-level javadoc}中所述.
	 * 
	 * @param element 带注解的元素 (never {@code null})
	 * @param annotationType 要查找的注解类型 (never {@code null})
	 * @param containerType 保存注解的容器的类型;
	 * 可能是{@code null}, 如果应通过{@link java.lang.annotation.Repeatable}查找容器类型
	 * 
	 * @return 找到的所有合并的可重复{@code Annotations}, 或空集合, 如果没有找到
	 * @throws IllegalArgumentException 如果{@code element}或{@code annotationType}是{@code null}, 或者如果无法解析容器类型
	 * @throws AnnotationConfigurationException 如果提供的{@code containerType}不是指定{@code annotationType}的有效容器注解
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType, Class<? extends Annotation> containerType) {

		Assert.notNull(element, "AnnotatedElement must not be null");
		Assert.notNull(annotationType, "'annotationType' must not be null");

		if (containerType == null) {
			containerType = resolveContainerType(annotationType);
		}
		else {
			validateContainerType(annotationType, containerType);
		}

		MergedAnnotationAttributesProcessor processor = new MergedAnnotationAttributesProcessor(false, false, true);
		searchWithFindSemantics(element, annotationType, null, containerType, processor);
		return postProcessAndSynthesizeAggregatedResults(element, annotationType, processor.getAggregatedResults());
	}

	/**
	 * 遵循<em>get 语义</em>, 在指定的{@code element}上搜索指定的{@code annotationName}或{@code annotationType}的注解.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param annotationName 要查找的注释类型的完全限定类名 (作为{@code annotationType}的替代)
	 * @param processor 委托给的处理器
	 * 
	 * @return 处理器的结果 (可能 {@code null})
	 */
	private static <T> T searchWithGetSemantics(AnnotatedElement element,
			Class<? extends Annotation> annotationType, String annotationName, Processor<T> processor) {

		return searchWithGetSemantics(element, annotationType, annotationName, null, processor);
	}

	/**
	 * 遵循<em>get 语义</em>, 在指定的{@code element}上搜索指定的{@code annotationName}或{@code annotationType}的注解.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param annotationName 要查找的注释类型的完全限定类名 (作为{@code annotationType}的替代)
	 * @param containerType 保存可重复注解的容器的类型, 或{@code null} 如果注解不可重复
	 * @param processor 委托给的处理器
	 * 
	 * @return 处理器的结果 (可能 {@code null})
	 */
	private static <T> T searchWithGetSemantics(AnnotatedElement element,
			Class<? extends Annotation> annotationType, String annotationName,
			Class<? extends Annotation> containerType, Processor<T> processor) {

		try {
			return searchWithGetSemantics(element, annotationType, annotationName,
					containerType, processor, new HashSet<AnnotatedElement>(), 0);
		}
		catch (Throwable ex) {
			AnnotationUtils.rethrowAnnotationConfigurationException(ex);
			throw new IllegalStateException("Failed to introspect annotations on " + element, ex);
		}
	}

	/**
	 * 执行{@link #searchWithGetSemantics}方法的搜索算法, 通过跟踪那些已经<em>访问</em>的带注解的元素来避免无限递归.
	 * <p>{@code metaDepth}参数在{@link Processor} API的{@link Processor#process process()}方法中进行了解释.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param annotationName 要查找的注释类型的完全限定类名 (作为{@code annotationType}的替代)
	 * @param containerType 保存可重复注解的容器的类型, 或{@code null} 如果注解不可重复
	 * @param processor 委托给的处理器
	 * @param visited 已经访问过的带注解的元素
	 * @param metaDepth 注解的元深度
	 * 
	 * @return 处理器的结果 (可能 {@code null})
	 */
	private static <T> T searchWithGetSemantics(AnnotatedElement element,
			Class<? extends Annotation> annotationType, String annotationName,
			Class<? extends Annotation> containerType, Processor<T> processor,
			Set<AnnotatedElement> visited, int metaDepth) {

		Assert.notNull(element, "AnnotatedElement must not be null");

		if (visited.add(element)) {
			try {
				// 开始在本地声明的注解中搜索
				List<Annotation> declaredAnnotations = Arrays.asList(element.getDeclaredAnnotations());
				T result = searchWithGetSemanticsInAnnotations(element, declaredAnnotations,
						annotationType, annotationName, containerType, processor, visited, metaDepth);
				if (result != null) {
					return result;
				}

				if (element instanceof Class) {  // 否则getAnnotations不会返回任何新内容
					List<Annotation> inheritedAnnotations = new ArrayList<Annotation>();
					for (Annotation annotation : element.getAnnotations()) {
						if (!declaredAnnotations.contains(annotation)) {
							inheritedAnnotations.add(annotation);
						}
					}

					// 继续在继承的注解中进行搜索
					result = searchWithGetSemanticsInAnnotations(element, inheritedAnnotations,
							annotationType, annotationName, containerType, processor, visited, metaDepth);
					if (result != null) {
						return result;
					}
				}
			}
			catch (Throwable ex) {
				AnnotationUtils.handleIntrospectionFailure(element, ex);
			}
		}

		return null;
	}

	/**
	 * {@link #searchWithGetSemantics}调用此方法以在提供的注解列表中执行实际搜索.
	 * <p>应首先使用本地声明的注解调用此方法, 然后使用继承的注解调用此方法, 从而允许本地注解优先于继承的注解.
	 * <p>{@code metaDepth}参数在{@link Processor} API的{@link Processor#process process()}方法中进行了解释.
	 * 
	 * @param element 使用提供的注解进行注解的元素, 用于上下文日志记录; may be {@code null} if unknown
	 * @param annotations 要搜索的注解
	 * @param annotationType 要查找的注解类型
	 * @param annotationName 要查找的注释类型的完全限定类名 (作为{@code annotationType}的替代)
	 * @param containerType 保存可重复注解的容器的类型, 或{@code null} 如果注解不可重复
	 * @param processor 委托给的处理器
	 * @param visited 已经访问过的带注解的元素
	 * @param metaDepth 注解的元深度
	 * 
	 * @return 处理器的结果 (可能 {@code null})
	 */
	private static <T> T searchWithGetSemanticsInAnnotations(AnnotatedElement element,
			List<Annotation> annotations, Class<? extends Annotation> annotationType,
			String annotationName, Class<? extends Annotation> containerType,
			Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth) {

		// Search in annotations
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> currentAnnotationType = annotation.annotationType();
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(currentAnnotationType)) {
				if (currentAnnotationType == annotationType ||
						currentAnnotationType.getName().equals(annotationName) ||
						processor.alwaysProcesses()) {
					T result = processor.process(element, annotation, metaDepth);
					if (result != null) {
						if (processor.aggregates() && metaDepth == 0) {
							processor.getAggregatedResults().add(result);
						}
						else {
							return result;
						}
					}
				}
				// 容器中可重复的注解?
				else if (currentAnnotationType == containerType) {
					for (Annotation contained : getRawAnnotationsFromContainer(element, annotation)) {
						T result = processor.process(element, contained, metaDepth);
						if (result != null) {
							// 无需后处理, 因为容器内的可重复注解无法组成注解.
							processor.getAggregatedResults().add(result);
						}
					}
				}
			}
		}

		// 递归搜索元注解
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> currentAnnotationType = annotation.annotationType();
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(currentAnnotationType)) {
				T result = searchWithGetSemantics(currentAnnotationType, annotationType,
						annotationName, containerType, processor, visited, metaDepth + 1);
				if (result != null) {
					processor.postProcess(element, annotation, result);
					if (processor.aggregates() && metaDepth == 0) {
						processor.getAggregatedResults().add(result);
					}
					else {
						return result;
					}
				}
			}
		}

		return null;
	}

	/**
	 * 遵循<em>find 语义</em>, 在指定的{@code element}上搜索指定的{@code annotationName}或{@code annotationType}的注解.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param annotationName 要查找的注释类型的完全限定类名 (作为{@code annotationType}的替代)
	 * @param processor 委托给的处理器
	 * 
	 * @return 处理器的结果 (可能 {@code null})
	 */
	private static <T> T searchWithFindSemantics(AnnotatedElement element,
			Class<? extends Annotation> annotationType,
			String annotationName, Processor<T> processor) {

		return searchWithFindSemantics(element, annotationType, annotationName, null, processor);
	}

	/**
	 * 遵循<em>find 语义</em>, 在指定的{@code element}上搜索指定的{@code annotationName}或{@code annotationType}的注解.
	 * 
	 * @param element 带注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param annotationName 要查找的注释类型的完全限定类名 (作为{@code annotationType}的替代)
	 * @param containerType 保存可重复注解的容器的类型, 或{@code null} 如果注解不可重复
	 * @param processor 委托给的处理器
	 * 
	 * @return 处理器的结果 (可能 {@code null})
	 */
	private static <T> T searchWithFindSemantics(AnnotatedElement element,
			Class<? extends Annotation> annotationType, String annotationName,
			Class<? extends Annotation> containerType, Processor<T> processor) {

		if (containerType != null && !processor.aggregates()) {
			throw new IllegalArgumentException(
					"Searches for repeatable annotations must supply an aggregating Processor");
		}

		try {
			return searchWithFindSemantics(element, annotationType, annotationName,
					containerType, processor, new HashSet<AnnotatedElement>(), 0);
		}
		catch (Throwable ex) {
			AnnotationUtils.rethrowAnnotationConfigurationException(ex);
			throw new IllegalStateException("Failed to introspect annotations on " + element, ex);
		}
	}

	/**
	 * 执行{@link #searchWithFindSemantics}方法的搜索算法, 通过跟踪那些已经<em>访问</em>的带注解的元素来避免无限递归.
	 * <p>{@code metaDepth}参数在{@link Processor} API的{@link Processor#process process()}方法中进行了解释.
	 * 
	 * @param element 带注解的元素 (never {@code null})
	 * @param annotationType 要查找的注解类型
	 * @param annotationName 要查找的注释类型的完全限定类名 (作为{@code annotationType}的替代)
	 * @param containerType 保存可重复注解的容器的类型, 或{@code null} 如果注解不可重复
	 * @param processor 委托给的处理器
	 * @param visited 已经访问过的带注解的元素
	 * @param metaDepth 注解的元深度
	 * 
	 * @return 处理器的结果 (可能 {@code null})
	 */
	private static <T> T searchWithFindSemantics(AnnotatedElement element, Class<? extends Annotation> annotationType,
			String annotationName, Class<? extends Annotation> containerType, Processor<T> processor,
			Set<AnnotatedElement> visited, int metaDepth) {

		Assert.notNull(element, "AnnotatedElement must not be null");

		if (visited.add(element)) {
			try {
				// 本地声明的注解 (ignoring @Inherited)
				Annotation[] annotations = element.getDeclaredAnnotations();
				if (annotations.length > 0) {
					List<T> aggregatedResults = (processor.aggregates() ? new ArrayList<T>() : null);

					// 在本地注解中搜索
					for (Annotation annotation : annotations) {
						Class<? extends Annotation> currentAnnotationType = annotation.annotationType();
						if (!AnnotationUtils.isInJavaLangAnnotationPackage(currentAnnotationType)) {
							if (currentAnnotationType == annotationType ||
									currentAnnotationType.getName().equals(annotationName) ||
									processor.alwaysProcesses()) {
								T result = processor.process(element, annotation, metaDepth);
								if (result != null) {
									if (aggregatedResults != null && metaDepth == 0) {
										aggregatedResults.add(result);
									}
									else {
										return result;
									}
								}
							}
							// 容器中可重复的注解?
							else if (currentAnnotationType == containerType) {
								for (Annotation contained : getRawAnnotationsFromContainer(element, annotation)) {
									T result = processor.process(element, contained, metaDepth);
									if (aggregatedResults != null && result != null) {
										// 无需后处理, 因为容器内的可重复注解无法组成注解.
										aggregatedResults.add(result);
									}
								}
							}
						}
					}

					// 递归搜索元注解
					for (Annotation annotation : annotations) {
						Class<? extends Annotation> currentAnnotationType = annotation.annotationType();
						if (!AnnotationUtils.isInJavaLangAnnotationPackage(currentAnnotationType)) {
							T result = searchWithFindSemantics(currentAnnotationType, annotationType, annotationName,
									containerType, processor, visited, metaDepth + 1);
							if (result != null) {
								processor.postProcess(currentAnnotationType, annotation, result);
								if (aggregatedResults != null && metaDepth == 0) {
									aggregatedResults.add(result);
								}
								else {
									return result;
								}
							}
						}
					}

					if (!CollectionUtils.isEmpty(aggregatedResults)) {
						// 前置以支持类层次结构中的自上而下排序
						processor.getAggregatedResults().addAll(0, aggregatedResults);
					}
				}

				if (element instanceof Method) {
					Method method = (Method) element;
					T result;

					// 搜索可能的桥接方法
					Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
					if (resolvedMethod != method) {
						result = searchWithFindSemantics(resolvedMethod, annotationType, annotationName,
								containerType, processor, visited, metaDepth);
						if (result != null) {
							return result;
						}
					}

					// 搜索本地声明的接口中的方法
					Class<?>[] ifcs = method.getDeclaringClass().getInterfaces();
					if (ifcs.length > 0) {
						result = searchOnInterfaces(method, annotationType, annotationName,
								containerType, processor, visited, metaDepth, ifcs);
						if (result != null) {
							return result;
						}
					}

					// 搜索类层次结构和接口层次结构中的方法
					Class<?> clazz = method.getDeclaringClass();
					while (true) {
						clazz = clazz.getSuperclass();
						if (clazz == null || Object.class == clazz) {
							break;
						}
						try {
							Method equivalentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
							Method resolvedEquivalentMethod = BridgeMethodResolver.findBridgedMethod(equivalentMethod);
							result = searchWithFindSemantics(resolvedEquivalentMethod, annotationType, annotationName,
									containerType, processor, visited, metaDepth);
							if (result != null) {
								return result;
							}
						}
						catch (NoSuchMethodException ex) {
							// No equivalent method found
						}
						// 搜索超类上声明的接口
						result = searchOnInterfaces(method, annotationType, annotationName,
								containerType, processor, visited, metaDepth, clazz.getInterfaces());
						if (result != null) {
							return result;
						}
					}
				}
				else if (element instanceof Class) {
					Class<?> clazz = (Class<?>) element;

					// Search on interfaces
					for (Class<?> ifc : clazz.getInterfaces()) {
						T result = searchWithFindSemantics(ifc, annotationType, annotationName,
								containerType, processor, visited, metaDepth);
						if (result != null) {
							return result;
						}
					}

					// Search on superclass
					Class<?> superclass = clazz.getSuperclass();
					if (superclass != null && Object.class != superclass) {
						T result = searchWithFindSemantics(superclass, annotationType, annotationName,
								containerType, processor, visited, metaDepth);
						if (result != null) {
							return result;
						}
					}
				}
			}
			catch (Throwable ex) {
				AnnotationUtils.handleIntrospectionFailure(element, ex);
			}
		}
		return null;
	}

	private static <T> T searchOnInterfaces(Method method, Class<? extends Annotation> annotationType,
			String annotationName, Class<? extends Annotation> containerType, Processor<T> processor,
			Set<AnnotatedElement> visited, int metaDepth, Class<?>[] ifcs) {

		for (Class<?> iface : ifcs) {
			if (AnnotationUtils.isInterfaceWithAnnotatedMethods(iface)) {
				try {
					Method equivalentMethod = iface.getMethod(method.getName(), method.getParameterTypes());
					T result = searchWithFindSemantics(equivalentMethod, annotationType, annotationName, containerType,
							processor, visited, metaDepth);
					if (result != null) {
						return result;
					}
				}
				catch (NoSuchMethodException ex) {
					// Skip this interface - it doesn't have the method...
				}
			}
		}

		return null;
	}

	/**
	 * 从提供的可重复注解{@code container}的{@code value}属性中获取原始 (未合成)注解.
	 */
	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A[] getRawAnnotationsFromContainer(AnnotatedElement element,
			Annotation container) {

		try {
			return (A[]) AnnotationUtils.getValue(container);
		}
		catch (Throwable ex) {
			AnnotationUtils.handleIntrospectionFailure(element, ex);
		}
		// 无法从重复注解容器中读取值 -> 忽略它.
		return (A[]) EMPTY_ANNOTATION_ARRAY;
	}

	/**
	 * 解析所提供的可重复{@code annotationType}的容器类型.
	 * <p>委托给{@link AnnotationUtils#resolveContainerAnnotationType(Class)}.
	 * 
	 * @param annotationType 要解析其容器的注解类型
	 * 
	 * @return 容器类型 (never {@code null})
	 * @throws IllegalArgumentException 如果容器类型无法解析
	 */
	private static Class<? extends Annotation> resolveContainerType(Class<? extends Annotation> annotationType) {
		Class<? extends Annotation> containerType = AnnotationUtils.resolveContainerAnnotationType(annotationType);
		if (containerType == null) {
			throw new IllegalArgumentException(
					"Annotation type must be a repeatable annotation: failed to resolve container type for " +
					annotationType.getName());
		}
		return containerType;
	}

	/**
	 * 验证提供的{@code containerType}是否为提供的可重复{@code annotationType}的正确容器注解
	 * (i.e., 它声明了一个保存{@code annotationType}数组的{@code value}属性).
	 * 
	 * @throws AnnotationConfigurationException 如果提供的{@code containerType}不是所提供的{@code annotationType}的有效容器注解
	 */
	private static void validateContainerType(Class<? extends Annotation> annotationType,
			Class<? extends Annotation> containerType) {

		try {
			Method method = containerType.getDeclaredMethod(AnnotationUtils.VALUE);
			Class<?> returnType = method.getReturnType();
			if (!returnType.isArray() || returnType.getComponentType() != annotationType) {
				String msg = String.format(
						"Container type [%s] must declare a 'value' attribute for an array of type [%s]",
						containerType.getName(), annotationType.getName());
				throw new AnnotationConfigurationException(msg);
			}
		}
		catch (Throwable ex) {
			AnnotationUtils.rethrowAnnotationConfigurationException(ex);
			String msg = String.format("Invalid declaration of container type [%s] for repeatable annotation [%s]",
					containerType.getName(), annotationType.getName());
			throw new AnnotationConfigurationException(msg, ex);
		}
	}

	private static <A extends Annotation> Set<A> postProcessAndSynthesizeAggregatedResults(AnnotatedElement element,
			Class<A> annotationType, List<AnnotationAttributes> aggregatedResults) {

		Set<A> annotations = new LinkedHashSet<A>();
		for (AnnotationAttributes attributes : aggregatedResults) {
			AnnotationUtils.postProcessAnnotationAttributes(element, attributes, false, false);
			annotations.add(AnnotationUtils.synthesizeAnnotation(attributes, annotationType, element));
		}
		return annotations;
	}


	/**
	 * 回调接口, 用于在搜索过程中处理注解.
	 * <p>根据用例, 处理器可以选择{@linkplain #process}单个目标注解, 多个目标注解, 或当前正在执行的搜索发现的所有注解.
	 * 此上下文中的术语"target" 指的是匹配的注解 (i.e., 在搜索期间找到的特定注解类型).
	 * <p>从{@link #process}方法返回非null值指示搜索算法停止进一步搜索;
	 * 然而, 从{@link #process}方法返回{@code null}指示搜索算法继续搜索其他注解.
	 * 此规则的一个例外适用于{@linkplain #aggregates aggregate}生成的处理器.
	 * 如果聚合处理器返回非null值, 则该值将添加到{@linkplain #getAggregatedResults 聚合结果}列表中, 并且搜索算法将继续.
	 * <p>处理器可以选择性的{@linkplain #postProcess 后处理} {@link #process}方法的结果,
	 * 因为搜索算法从返回非null值的{@link #process}调用返回到注解层次结构, 直到作为搜索算法的起点提供的{@link AnnotatedElement}.
	 * 
	 * @param <T> 处理器返回的结果类型
	 */
	private interface Processor<T> {

		/**
		 * 处理提供的注解.
		 * <p>提供的注解将是搜索算法找到的实际目标注解, 除非此处理器配置为{@linkplain #alwaysProcesses 总是处理}注解,
		 * 在这种情况下, 它可能是注解层次结构中的其他注解.
		 * 在后一种情况下, {@code metaDepth}的值大于{@code 0}.
		 * 在任何情况下, 由此方法的具体实现决定如何处理提供的注解.
		 * <p>{@code metaDepth}参数表示注解相对于注解层次结构中第一个带注解的元素的深度.
		 * 例如, 非注释元素上<em>存在</em>的注解的深度为0;
		 * 元注解的深度为1;
		 * 元元注解的深度为2; etc.
		 * 
		 * @param annotatedElement 使用提供的注解进行注解的元素, 用于上下文日志记录; 如果不知道, 可能是{@code null}
		 * @param annotation 要处理的注解
		 * @param metaDepth 注解的元深度
		 * 
		 * @return 处理的结果, 或{@code null}继续搜索其他注解
		 */
		T process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth);

		/**
		 * 后处理{@link #process}方法返回的结果.
		 * <p>提供给此方法的{@code annotation}是一个注解, 它存在于注解层次结构中,
		 * 位于初始{@link AnnotatedElement}和返回非null值的{@link #process}调用之间.
		 * 
		 * @param annotatedElement 使用提供的注解进行注解的元素, 用于上下文日志记录; 如果不知道, 可能是{@code null}
		 * @param annotation 要后处理的注解
		 * @param result 要后处理的结果
		 */
		void postProcess(AnnotatedElement annotatedElement, Annotation annotation, T result);

		/**
		 * 无论是否找到目标注解, 确定此处理器是否始终处理注解.
		 * 
		 * @return {@code true} 如果此处理器始终处理注解
		 */
		boolean alwaysProcesses();

		/**
		 * 确定此处理器是否聚合{@link #process}返回的结果.
		 * <p>如果此方法返回{@code true}, 则{@link #getAggregatedResults()}必须返回非null值.
		 * 
		 * @return {@code true} 如果此处理器支持聚合的结果
		 */
		boolean aggregates();

		/**
		 * 获取此处理器聚合的结果列表.
		 * <p>NOTE: 处理器<strong>不</strong>聚合结果本身.
		 * 相反, 使用此处理器的搜索算法负责询问此处理器{@link #aggregates}结果, 然后将后处理结果添加到此方法返回的列表中.
		 * 
		 * @return 此处理器聚合的结果列表 (never {@code null})
		 */
		List<T> getAggregatedResults();
	}


	/**
	 * {@link Processor}, {@linkplain #process(AnnotatedElement, Annotation, int) 处理}注解,
	 * 但没有{@linkplain #postProcess 后处理} 或 {@linkplain #aggregates 聚合}结果.
	 */
	private abstract static class SimpleAnnotationProcessor<T> implements Processor<T> {

		private final boolean alwaysProcesses;

		public SimpleAnnotationProcessor() {
			this(false);
		}

		public SimpleAnnotationProcessor(boolean alwaysProcesses) {
			this.alwaysProcesses = alwaysProcesses;
		}

		@Override
		public final boolean alwaysProcesses() {
			return this.alwaysProcesses;
		}

		@Override
		public final void postProcess(AnnotatedElement annotatedElement, Annotation annotation, T result) {
			// no-op
		}

		@Override
		public final boolean aggregates() {
			return false;
		}

		@Override
		public final List<T> getAggregatedResults() {
			throw new UnsupportedOperationException("SimpleAnnotationProcessor does not support aggregated results");
		}
	}


	/**
	 * {@link SimpleAnnotationProcessor}始终返回 {@link Boolean#TRUE},
	 * 在被要求{@linkplain #process(AnnotatedElement, Annotation, int) 处理}注解时.
	 */
	static class AlwaysTrueBooleanAnnotationProcessor extends SimpleAnnotationProcessor<Boolean> {

		@Override
		public final Boolean process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
			return Boolean.TRUE;
		}
	}


	/**
	 * {@link Processor}, 在{@link #process}阶段获取目标注解的{@code AnnotationAttributes},
	 * 然后在{@link #postProcess}阶段合并注解层次结构中较低级别的注解属性.
	 * <p>{@code MergedAnnotationAttributesProcessor}可以选择性地配置为{@linkplain #aggregates 聚合}结果.
	 */
	private static class MergedAnnotationAttributesProcessor implements Processor<AnnotationAttributes> {

		private final boolean classValuesAsString;

		private final boolean nestedAnnotationsAsMap;

		private final boolean aggregates;

		private final List<AnnotationAttributes> aggregatedResults;

		MergedAnnotationAttributesProcessor() {
			this(false, false, false);
		}

		MergedAnnotationAttributesProcessor(boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
			this(classValuesAsString, nestedAnnotationsAsMap, false);
		}

		MergedAnnotationAttributesProcessor(boolean classValuesAsString, boolean nestedAnnotationsAsMap,
				boolean aggregates) {

			this.classValuesAsString = classValuesAsString;
			this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
			this.aggregates = aggregates;
			this.aggregatedResults = (aggregates ? new ArrayList<AnnotationAttributes>() : null);
		}

		@Override
		public boolean alwaysProcesses() {
			return false;
		}

		@Override
		public boolean aggregates() {
			return this.aggregates;
		}

		@Override
		public List<AnnotationAttributes> getAggregatedResults() {
			return this.aggregatedResults;
		}

		@Override
		public AnnotationAttributes process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
			return AnnotationUtils.retrieveAnnotationAttributes(annotatedElement, annotation,
					this.classValuesAsString, this.nestedAnnotationsAsMap);
		}

		@Override
		public void postProcess(AnnotatedElement element, Annotation annotation, AnnotationAttributes attributes) {
			annotation = AnnotationUtils.synthesizeAnnotation(annotation, element);
			Class<? extends Annotation> targetAnnotationType = attributes.annotationType();

			// 跟踪哪些属性值已被替换, 以便我们可以短路搜索算法.
			Set<String> valuesAlreadyReplaced = new HashSet<String>();

			for (Method attributeMethod : AnnotationUtils.getAttributeMethods(annotation.annotationType())) {
				String attributeName = attributeMethod.getName();
				String attributeOverrideName = AnnotationUtils.getAttributeOverrideName(attributeMethod, targetAnnotationType);

				// 通过@AliasFor声明的显式注释属性覆盖
				if (attributeOverrideName != null) {
					if (valuesAlreadyReplaced.contains(attributeOverrideName)) {
						continue;
					}

					List<String> targetAttributeNames = new ArrayList<String>();
					targetAttributeNames.add(attributeOverrideName);
					valuesAlreadyReplaced.add(attributeOverrideName);

					// 确保覆盖目标注解中的所有别名属性. (SPR-14069)
					List<String> aliases = AnnotationUtils.getAttributeAliasMap(targetAnnotationType).get(attributeOverrideName);
					if (aliases != null) {
						for (String alias : aliases) {
							if (!valuesAlreadyReplaced.contains(alias)) {
								targetAttributeNames.add(alias);
								valuesAlreadyReplaced.add(alias);
							}
						}
					}

					overrideAttributes(element, annotation, attributes, attributeName, targetAttributeNames);
				}
				// 基于约定的隐式注解属性覆盖
				else if (!AnnotationUtils.VALUE.equals(attributeName) && attributes.containsKey(attributeName)) {
					overrideAttribute(element, annotation, attributes, attributeName, attributeName);
				}
			}
		}

		private void overrideAttributes(AnnotatedElement element, Annotation annotation,
				AnnotationAttributes attributes, String sourceAttributeName, List<String> targetAttributeNames) {

			Object adaptedValue = getAdaptedValue(element, annotation, sourceAttributeName);

			for (String targetAttributeName : targetAttributeNames) {
				attributes.put(targetAttributeName, adaptedValue);
			}
		}

		private void overrideAttribute(AnnotatedElement element, Annotation annotation, AnnotationAttributes attributes,
				String sourceAttributeName, String targetAttributeName) {

			attributes.put(targetAttributeName, getAdaptedValue(element, annotation, sourceAttributeName));
		}

		private Object getAdaptedValue(AnnotatedElement element, Annotation annotation, String sourceAttributeName) {
			Object value = AnnotationUtils.getValue(annotation, sourceAttributeName);
			return AnnotationUtils.adaptValue(element, value, this.classValuesAsString, this.nestedAnnotationsAsMap);
		}
	}
}
