package org.springframework.core.convert;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 关于要转换的类型的上下文.
 */
@SuppressWarnings("serial")
public class TypeDescriptor implements Serializable {

	static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	private static final boolean streamAvailable = ClassUtils.isPresent(
			"java.util.stream.Stream", TypeDescriptor.class.getClassLoader());

	private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<Class<?>, TypeDescriptor>(32);

	private static final Class<?>[] CACHED_COMMON_TYPES = {
			boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
			double.class, Double.class, float.class, Float.class, int.class, Integer.class,
			long.class, Long.class, short.class, Short.class, String.class, Object.class};

	static {
		for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
			commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
		}
	}


	private final Class<?> type;

	private final ResolvableType resolvableType;

	private final AnnotatedElementAdapter annotatedElement;


	/**
	 * 从{@link MethodParameter}创建一个新的类型描述符.
	 * <p>当源或目标转换点是构造函数参数, 方法参数或方法返回值时, 请使用此构造函数.
	 * 
	 * @param methodParameter 方法参数
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
		this.type = this.resolvableType.resolve(methodParameter.getParameterType());
		this.annotatedElement = new AnnotatedElementAdapter(methodParameter.getParameterIndex() == -1 ?
				methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
	}

	/**
	 * 从{@link Field}创建一个新的类型描述符.
	 * <p>当源或目标转换点是字段时, 请使用此构造函数.
	 * 
	 * @param field 字段
	 */
	public TypeDescriptor(Field field) {
		this.resolvableType = ResolvableType.forField(field);
		this.type = this.resolvableType.resolve(field.getType());
		this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
	}

	/**
	 * 从{@link Property}创建一个新的类型描述符.
	 * <p>当源或目标转换点是Java类的属性时, 请使用此构造函数.
	 * 
	 * @param property 属性
	 */
	public TypeDescriptor(Property property) {
		Assert.notNull(property, "Property must not be null");
		this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
		this.type = this.resolvableType.resolve(property.getType());
		this.annotatedElement = new AnnotatedElementAdapter(property.getAnnotations());
	}

	/**
	 * 从{@link ResolvableType}创建一个新的类型描述符.
	 * 此受保护的构造函数在内部使用, 也可由支持扩展类型系统使用非Java语言的子类使用.
	 * 
	 * @param resolvableType 可解析的类型
	 * @param type 支持类型 (或{@code null}, 如果它应该被解析)
	 * @param annotations 类型注解
	 */
	protected TypeDescriptor(ResolvableType resolvableType, Class<?> type, Annotation[] annotations) {
		this.resolvableType = resolvableType;
		this.type = (type != null ? type : resolvableType.resolve(Object.class));
		this.annotatedElement = new AnnotatedElementAdapter(annotations);
	}


	/**
	 * 通过返回其对象包装类型来解释基本类型的{@link #getType()}的变体.
	 * <p>这对于希望规范化为基于对象的类型而不是直接使用基本类型的转换服务实现很有用.
	 */
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * 此TypeDescriptor描述的支持类, 方法参数, 字段或属性的类型.
	 * <p>按原样返回原始类型. 有关此操作的变体, 请参阅{@link #getObjectType()}, 可将原始类型解析为其对应的Object类型.
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * 返回底层{@link ResolvableType}.
	 */
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * 返回描述符的底层源.
	 * 将返回{@link Field}, {@link MethodParameter}或 {@link Type}, 具体取决于{@link TypeDescriptor}的构造方式.
	 * 此方法主要用于提供对备用JVM语言可能提供的其他类型信息或元数据的访问.
	 */
	public Object getSource() {
		return (this.resolvableType != null ? this.resolvableType.getSource() : null);
	}

	/**
	 * 通过将其类型设置为提供值的类, 来缩小此{@link TypeDescriptor}.
	 * <p>如果值为{@code null}, 则不执行缩小, 并且不更改此TypeDescriptor.
	 * <p>设计为在读取属性, 字段或方法返回值时由绑定框架调用.
	 * 允许此类框架缩小从声明的属性, 字段或方法返回值类型构建的TypeDescriptor.
	 * 例如, 声明为{@code java.lang.Object}的字段将缩小为{@code java.util.HashMap}, 如果它被设置为{@code java.util.HashMap}值.
	 * 然后可以使用缩小的TypeDescriptor将HashMap转换为其他类型.
	 * 缩小的副本保留注解和嵌套的类型上下文.
	 * 
	 * @param value 用于缩小此类型描述符的值
	 * 
	 * @return 缩小的TypeDescriptor (返回其类型更新为提供值的类的副本)
	 */
	public TypeDescriptor narrow(Object value) {
		if (value == null) {
			return this;
		}
		ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
		return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
	}

	/**
	 * 将此{@link TypeDescriptor}转换为超类或实现的接口, 保留注解和嵌套的类型上下文.
	 * 
	 * @param superType 要转换为的超级类型 (can be {@code null})
	 * 
	 * @return 向上转型的新TypeDescriptor
	 * @throws IllegalArgumentException 如果此类型不能分配给超类型
	 */
	public TypeDescriptor upcast(Class<?> superType) {
		if (superType == null) {
			return null;
		}
		Assert.isAssignable(superType, getType());
		return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
	}

	/**
	 * 返回此类型的名称: 完全限定的类名.
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * 这种类型是原始类型?
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * 返回与此类型描述符关联的注解.
	 * 
	 * @return 注解, 或空数组
	 */
	public Annotation[] getAnnotations() {
		return this.annotatedElement.getAnnotations();
	}

	/**
	 * 确定此类型描述符是否具有指定的注解.
	 * <p>从Spring Framework 4.2开始, 此方法支持任意级别的元注解.
	 * 
	 * @param annotationType 注解类型
	 * 
	 * @return <tt>true</tt> 如果注解存在
	 */
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils必须期望 AnnotatedElement.getAnnotations() 返回数组的副本, 而我们可以在这里更有效地执行它.
			return false;
		}
		return AnnotatedElementUtils.isAnnotated(this.annotatedElement, annotationType);
	}

	/**
	 * 获取此类型描述符上指定的{@code annotationType}的注解.
	 * <p>从Spring Framework 4.2开始, 此方法支持任意级别的元注解.
	 * 
	 * @param annotationType 注解类型
	 * 
	 * @return 注解; 如果此类型描述符上不存在此类注解, 则为{@code null}
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils 必须期望AnnotatedElement.getAnnotations() 返回数组的副本, 而我们可以在这里更有效地执行它.
			return null;
		}
		return AnnotatedElementUtils.getMergedAnnotation(this.annotatedElement, annotationType);
	}

	/**
	 * 如果可以将此类型描述符的对象分配给给定类型描述符描述的位置, 则返回true.
	 * <p>例如, {@code valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))}返回 {@code true},
	 * 因为可以将String值分配给CharSequence变量.
	 * 另一方面, {@code valueOf(Number.class).isAssignableTo(valueOf(Integer.class))}返回{@code false},
	 * 因为虽然所有Integer 都是 Number, 但并非所有Number都是Integer.
	 * <p>对于数组, 集合和映射, 如果声明, 则检查元素和键/值类型.
	 * 例如, List&lt;String&gt; 字段值可分配给 Collection&lt;CharSequence&gt; 字段,
	 * 但 List&lt;Number&gt; 不能分配给 List&lt;Integer&gt;.
	 * 
	 * @return {@code true} 如果此类型可分配给由提供的类型描述符表示的类型
	 */
	public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
		boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			return false;
		}
		if (isArray() && typeDescriptor.isArray()) {
			return getElementTypeDescriptor().isAssignableTo(typeDescriptor.getElementTypeDescriptor());
		}
		else if (isCollection() && typeDescriptor.isCollection()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		}
		else if (isMap() && typeDescriptor.isMap()) {
			return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor()) &&
				isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
		}
		else {
			return true;
		}
	}

	private boolean isNestedAssignable(TypeDescriptor nestedTypeDescriptor, TypeDescriptor otherNestedTypeDescriptor) {
		if (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null) {
			return true;
		}
		return nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor);
	}

	/**
	 * 此类型是{@link Collection}类型?
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * 此类型是数组类型?
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * 如果此类型是数组, 则返回数组的组件类型.
	 * 如果此类型是{@code Stream}, 则返回流的组件类型.
	 * 如果此类型是{@link Collection}并且参数化, 则返回Collection的元素类型.
	 * 如果Collection未参数化, 则返回{@code null}表示未声明元素类型.
	 * 
	 * @return 数组组件类型或Collection元素类型, 或{@code null}如果此类型是Collection但其元素类型未参数化
	 * @throws IllegalStateException 如果此类型不是{@code java.util.Collection}或数组类型
	 */
	public TypeDescriptor getElementTypeDescriptor() {
		if (getResolvableType().isArray()) {
			return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
		}
		if (streamAvailable && StreamDelegate.isStream(getType())) {
			return StreamDelegate.getStreamElementType(this);
		}
		return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
	}

	/**
	 * 如果此类型是{@link Collection}或数组, 则从提供的集合或数组元素创建元素TypeDescriptor.
	 * <p>将{@link #getElementTypeDescriptor() elementType}属性缩小为提供的集合或数组元素的类.
	 * 例如, 如果这描述了{@code java.util.List&lt;java.lang.Number&lt;}并且元素参数是{@code java.lang.Integer},
	 * 则返回的TypeDescriptor将是{@code java.lang.Integer}.
	 * 如果这描述了{@code java.util.List&lt;?&gt;}并且元素参数是{@code java.lang.Integer},
	 * 则返回的TypeDescriptor也将是{@code java.lang.Integer}.
	 * <p>注解和嵌套的类型上下文将保留在返回的缩小的TypeDescriptor中.
	 * 
	 * @param element 集合或数组元素
	 * 
	 * @return 元素类型描述符, 缩小为提供的元素的类型
	 * @throws IllegalStateException 如果此类型不是{@code java.util.Collection}或数组类型
	 */
	public TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
	}

	/**
	 * 此类型是{@link Map}类型?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * 如果此类型是{@link Map}并且其键类型已参数化, 则返回Map的键类型.
	 * 如果Map的键类型未参数化, 则返回{@code null}表示未声明键类型.
	 * 
	 * @return Map键类型, 或{@code null}如果此类型是Map但其键类型未参数化
	 * @throws IllegalStateException 如果此类型不是{@code java.util.Map}
	 */
	public TypeDescriptor getMapKeyTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
	}

	/**
	 * 如果此类型是{@link Map}, 则从提供的Map键创建mapKey {@link TypeDescriptor}.
	 * <p>将{@link #getMapKeyTypeDescriptor() mapKeyType}属性缩小为提供的Map键的类.
	 * 例如, 如果这描述了{@code java.util.Map&lt;java.lang.Number, java.lang.String&lt;}并且key参数是{@code java.lang.Integer},
	 * 则返回的TypeDescriptor将是{@code java.lang.Integer}.
	 * 如果这描述了{@code java.util.Map&lt;?, ?&gt;}并且key参数是{@code java.lang.Integer},
	 * 则返回的TypeDescriptor也将是{@code java.lang.Integer}.
	 * <p>注解和嵌套的类型上下文将保留在返回的缩小的TypeDescriptor中.
	 * 
	 * @param mapKey Map的键
	 * 
	 * @return Map的键的类型描述符
	 * @throws IllegalStateException 如果此类型不是{@code java.util.Map}
	 */
	public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * 如果此类型是{@link Map}并且其值类型已参数化, 则返回Map的值类型.
	 * <p>如果Map的值类型未参数化, 则返回{@code null}指示未声明值类型.
	 * 
	 * @return Map值类型, 或{@code null}如果此类型是Map但其值类型未参数化
	 * @throws IllegalStateException 如果此类型不是{@code java.util.Map}
	 */
	public TypeDescriptor getMapValueTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
	}

	/**
	 * 如果此类型是{@link Map}, 从提供的Map值创建mapValue {@link TypeDescriptor}.
	 * <p>将{@link #getMapValueTypeDescriptor() mapValueType}属性缩小为提供的Map值的类.
	 * 例如, 如果这描述了{@code java.util.Map&lt;java.lang.String, java.lang.Number&lt;}并且值参数是{@code java.lang.Integer},
	 * 则返回的TypeDescriptor将是{@code java.lang.Integer}.
	 * 如果这描述了{@code java.util.Map&lt;?, ?&gt;}并且值参数是{@code java.lang.Integer},
	 * 则返回的TypeDescriptor也将是{@code java.lang.Integer}.
	 * <p>注解和嵌套的类型上下文将保留在返回的缩小的TypeDescriptor中.
	 * 
	 * @param mapValue Map的值
	 * 
	 * @return Map的值的类型描述符
	 * @throws IllegalStateException 如果此类型不是{@code java.util.Map}
	 */
	public TypeDescriptor getMapValueTypeDescriptor(Object mapValue) {
		return narrow(mapValue, getMapValueTypeDescriptor());
	}

	private TypeDescriptor narrow(Object value, TypeDescriptor typeDescriptor) {
		if (typeDescriptor != null) {
			return typeDescriptor.narrow(value);
		}
		if (value != null) {
			return narrow(value);
		}
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypeDescriptor)) {
			return false;
		}
		TypeDescriptor otherDesc = (TypeDescriptor) other;
		if (getType() != otherDesc.getType()) {
			return false;
		}
		if (!annotationsMatch(otherDesc)) {
			return false;
		}
		if (isCollection() || isArray()) {
			return ObjectUtils.nullSafeEquals(getElementTypeDescriptor(), otherDesc.getElementTypeDescriptor());
		}
		else if (isMap()) {
			return (ObjectUtils.nullSafeEquals(getMapKeyTypeDescriptor(), otherDesc.getMapKeyTypeDescriptor()) &&
					ObjectUtils.nullSafeEquals(getMapValueTypeDescriptor(), otherDesc.getMapValueTypeDescriptor()));
		}
		else {
			return true;
		}
	}

	private boolean annotationsMatch(TypeDescriptor otherDesc) {
		Annotation[] anns = getAnnotations();
		Annotation[] otherAnns = otherDesc.getAnnotations();
		if (anns == otherAnns) {
			return true;
		}
		if (anns.length != otherAnns.length) {
			return false;
		}
		if (anns.length > 0) {
			for (int i = 0; i < anns.length; i++) {
				if (!annotationEquals(anns[i], otherAnns[i])) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
		// Annotation.equals 是反射性的, 非常慢, 所以让我们首先检查身份和代理类型.
		return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
	}

	@Override
	public int hashCode() {
		return getType().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Annotation ann : getAnnotations()) {
			builder.append("@").append(ann.annotationType().getName()).append(' ');
		}
		builder.append(getResolvableType().toString());
		return builder.toString();
	}


	/**
	 * 为对象创建新的类型描述符.
	 * <p>在请求转换系统将其转换为其他类型之前, 请使用此工厂方法来内省源对象.
	 * <p>如果提供的对象是{@code null}, 则返回{@code null}, 否则调用{@link #valueOf(Class)}从对象的类构建TypeDescriptor.
	 * 
	 * @param source the source object
	 * 
	 * @return the type descriptor
	 */
	public static TypeDescriptor forObject(Object source) {
		return (source != null ? valueOf(source.getClass()) : null);
	}

	/**
	 * 从给定的类型创建新的类型描述符.
	 * <p>当没有类型位置(如方法参数或字段)可用于提供其他转换上下文时, 使用这个来指示转换系统将对象转换为特定目标类型.
	 * <p>通常更喜欢使用{@link #forObject(Object)}从源对象构造类型描述符, 因为它处理{@code null}对象的情况.
	 * 
	 * @param type 类(可能是{@code null}来指示{@code Object.class})
	 * 
	 * @return 相应的类型描述符
	 */
	public static TypeDescriptor valueOf(Class<?> type) {
		if (type == null) {
			type = Object.class;
		}
		TypeDescriptor desc = commonTypesCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
	}

	/**
	 * 从{@link java.util.Collection}类型创建新的类型描述符.
	 * <p>用于转换为类型Collection.
	 * <p>例如, 通过转换为使用此方法构建的targetType, 可以将{@code List<String>}转换为{@code List<EmailAddress>}.
	 * 构造这样的{@code TypeDescriptor}的方法调用看起来像: {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	 * 
	 * @param collectionType 必须实现{@link Collection}的集合类型.
	 * @param elementTypeDescriptor 集合的元素类型的描述符, 用于转换集合元素
	 * 
	 * @return 集合类型描述符
	 */
	public static TypeDescriptor collection(Class<?> collectionType, TypeDescriptor elementTypeDescriptor) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
		}
		ResolvableType element = (elementTypeDescriptor != null ? elementTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, null);
	}

	/**
	 * 从{@link java.util.Map}类型创建新的类型描述符.
	 * <p>用于转换为类型 Map.
	 * <p>例如, Map&lt;String, String&gt; 可以转换为 Map&lt;Id, EmailAddress&gt;, 通过转换为使用此方法构建的targetType:
	 * 构造这样的TypeDescriptor的方法调用看起来像:
	 * <pre class="code">
	 * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * </pre>
	 * 
	 * @param mapType Map类型, 必须实现{@link Map}
	 * @param keyTypeDescriptor Map的键类型的描述符, 用于转换Map键
	 * @param valueTypeDescriptor Map的值类型, 用于转换Map值
	 * 
	 * @return Map类型描述符
	 */
	public static TypeDescriptor map(Class<?> mapType, TypeDescriptor keyTypeDescriptor, TypeDescriptor valueTypeDescriptor) {
		Assert.notNull(mapType, "Map type must not be null");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("Map type must be a [java.util.Map]");
		}
		ResolvableType key = (keyTypeDescriptor != null ? keyTypeDescriptor.resolvableType : null);
		ResolvableType value = (valueTypeDescriptor != null ? valueTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, null);
	}

	/**
	 * 创建一个指定类型的数组的新类型描述符.
	 * <p>例如创建{@code Map<String,String>[]}使用:
	 * <pre class="code">
	 * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	 * </pre>
	 * 
	 * @param elementTypeDescriptor 数组元素的{@link TypeDescriptor}或{@code null}
	 * 
	 * @return 数组{@link TypeDescriptor}, 或{@code null} 如果{@code elementTypeDescriptor}是 {@code null}
	 */
	public static TypeDescriptor array(TypeDescriptor elementTypeDescriptor) {
		if (elementTypeDescriptor == null) {
			return null;
		}
		return new TypeDescriptor(ResolvableType.forArrayComponent(elementTypeDescriptor.resolvableType),
				null, elementTypeDescriptor.getAnnotations());
	}

	/**
	 * 为method参数中声明的嵌套类型创建类型描述符.
	 * <p>例如, 如果 methodParameter 是{@code List<String>}, 并且嵌套级别为1, 则嵌套的类型描述符将为 String.class.
	 * <p>如果methodParameter是{@code List<List<String>>}, 并且嵌套级别为 2, 则嵌套的类型描述符也将为 String.class.
	 * <p>如果methodParameter是{@code Map<Integer, String>}, 并且嵌套级别为 1, 则嵌套的类型描述符将为 String, 从Map值派生.
	 * <p>如果methodParameter是{@code List<Map<Integer, String>>}, 并且嵌套级别为 2, 则嵌套的类型描述符将为 String, 从Map值派生.
	 * <p>如果由于未声明嵌套类型而无法获取嵌套类型, 则返回{@code null}.
	 * 例如, 如果方法参数是 {@code List<?>}, 则返回的嵌套类型描述符将为 {@code null}.
	 * 
	 * @param methodParameter nestingLevel为1的method参数
	 * @param nestingLevel 方法参数中的集合/数组元素, 或Map键/值声明的嵌套级别
	 * 
	 * @return 指定的嵌套级别的嵌套类型描述符, 或{@code null}如果无法获取
	 * @throws IllegalArgumentException 如果输入{@link MethodParameter}参数的嵌套级别不是 1,
	 * 或者指定嵌套级别的类型不是集合, 数组, 或Map类型
	 */
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		if (methodParameter.getNestingLevel() != 1) {
			throw new IllegalArgumentException("MethodParameter nesting level must be 1: " +
					"use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal");
		}
		return nested(new TypeDescriptor(methodParameter), nestingLevel);
	}

	/**
	 * 为字段中声明的嵌套类型创建类型描述符.
	 * <p>例如, 如果字段是{@code List<String>}, 并且嵌套级别为 1, 则嵌套的类型描述符将为 {@code String.class}.
	 * <p>如果字段是{@code List<List<String>>}, 并且嵌套级别为 2, 则嵌套的类型描述符将为 {@code String.class}.
	 * <p>如果字段是{@code Map<Integer, String>}, 并且嵌套级别为 1, 则嵌套的类型描述符将为 String, 从Map值派生.
	 * <p>如果字段是{@code List<Map<Integer, String>>}, 并且嵌套级别为 2, 则嵌套的类型描述符将为 String, 从Map值派生.
	 * <p>如果由于未声明嵌套类型而无法获取嵌套类型, 则返回{@code null}.
	 * 例如, 如果字段是{@code List<?>}, 则返回的嵌套类型描述符将为 {@code null}.
	 * 
	 * @param field 字段
	 * @param nestingLevel 字段中的集合/数组元素, 或Map键/值声明的嵌套级别
	 * 
	 * @return 指定的嵌套级别的嵌套类型描述符, 或{@code null}如果无法获取
	 * @throws IllegalArgumentException 如果指定的嵌套级别的类型不是集合, 数组, 或Map类型
	 */
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new TypeDescriptor(field), nestingLevel);
	}

	/**
	 * 为属性中声明的嵌套类型创建类型描述符.
	 * <p>例如, 如果属性是{@code List<String>}, 并且嵌套级别为 1, 则嵌套的类型描述符将为 {@code String.class}.
	 * <p>如果属性是{@code List<List<String>>}, 并且嵌套级别为 2, 则嵌套的类型描述符将为 {@code String.class}.
	 * <p>如果属性是{@code Map<Integer, String>}, 并且嵌套级别为 1, 则嵌套的类型描述符将为 String, 从Map值派生.
	 * <p>如果属性是{@code List<Map<Integer, String>>}, 并且嵌套级别为 2, 则嵌套的类型描述符将为 String, 从Map值派生.
	 * <p>如果由于未声明嵌套类型而无法获取嵌套类型, 则返回{@code null}.
	 * 例如, 如果属性是 {@code List<?>}, 则返回的嵌套类型描述符将为 {@code null}.
	 * 
	 * @param property 属性
	 * @param nestingLevel 属性中的集合/数组元素, 或Map键/值声明的嵌套级别
	 * 
	 * @return 指定的嵌套级别的嵌套类型描述符, 或{@code null}如果无法获取
	 * @throws IllegalArgumentException 如果指定的嵌套级别的类型不是集合, 数组, 或Map类型
	 */
	public static TypeDescriptor nested(Property property, int nestingLevel) {
		return nested(new TypeDescriptor(property), nestingLevel);
	}

	private static TypeDescriptor nested(TypeDescriptor typeDescriptor, int nestingLevel) {
		ResolvableType nested = typeDescriptor.resolvableType;
		for (int i = 0; i < nestingLevel; i++) {
			if (Object.class == nested.getType()) {
				// 可以是集合类型, 但我们不知道它的元素类型, 所以假设有一个Object类型的元素类型...
			}
			else {
				nested = nested.getNested(2);
			}
		}
		if (nested == ResolvableType.NONE) {
			return null;
		}
		return getRelatedIfResolvable(typeDescriptor, nested);
	}

	private static TypeDescriptor getRelatedIfResolvable(TypeDescriptor source, ResolvableType type) {
		if (type.resolve() == null) {
			return null;
		}
		return new TypeDescriptor(type, null, source.getAnnotations());
	}


	/**
	 * 用于将{@code TypeDescriptor}的注解公开为{@link AnnotatedElement}的适配器类, 特别是{@link AnnotatedElementUtils}.
	 */
	private class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

		private final Annotation[] annotations;

		public AnnotatedElementAdapter(Annotation[] annotations) {
			this.annotations = annotations;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return true;
				}
			}
			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return (this.annotations != null ? this.annotations : EMPTY_ANNOTATION_ARRAY);
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return getAnnotations();
		}

		public boolean isEmpty() {
			return ObjectUtils.isEmpty(this.annotations);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof AnnotatedElementAdapter &&
					Arrays.equals(this.annotations, ((AnnotatedElementAdapter) other).annotations)));
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.annotations);
		}

		@Override
		public String toString() {
			return TypeDescriptor.this.toString();
		}
	}


	/**
	 * 内部类, 以避免对Java 8的硬依赖.
	 */
	@UsesJava8
	private static class StreamDelegate {

		public static boolean isStream(Class<?> type) {
			return Stream.class.isAssignableFrom(type);
		}

		public static TypeDescriptor getStreamElementType(TypeDescriptor source) {
			return getRelatedIfResolvable(source, source.getResolvableType().as(Stream.class).getGeneric(0));
		}
	}

}
