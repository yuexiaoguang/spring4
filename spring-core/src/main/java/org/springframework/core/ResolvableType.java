package org.springframework.core;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.springframework.core.SerializableTypeWrapper.FieldTypeProvider;
import org.springframework.core.SerializableTypeWrapper.MethodParameterTypeProvider;
import org.springframework.core.SerializableTypeWrapper.TypeProvider;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 封装Java {@link java.lang.reflect.Type}, 提供对
 * {@link #getSuperType() 超类型}, {@link #getInterfaces() 接口},
 * {@link #getGeneric(int...) 泛型参数}的访问,
 * 以及最终{@link #resolve() resolve}为{@link java.lang.Class}的能力.
 *
 * <p>{@code ResolvableTypes}可能从{@link #forField(Field) 字段},
 * {@link #forMethodParameter(Method, int) 方法参数},
 * {@link #forMethodReturnType(Method) 方法返回值}或{@link #forClass(Class) classes}获取.
 * 此类中的大多数方法本身都会返回{@link ResolvableType}s, 从而可以轻松导航.
 * 例如:
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 */
@SuppressWarnings("serial")
public class ResolvableType implements Serializable {

	/**
	 * 没有值可用时返回的{@code ResolvableType}.
	 * {@code NONE}优先于{@code null}使用, 以便可以安全地链接多个方法调用.
	 */
	public static final ResolvableType NONE = new ResolvableType(null, null, null, 0);

	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

	private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
			new ConcurrentReferenceHashMap<ResolvableType, ResolvableType>(256);


	/**
	 * 托管的底层Java类型 (只有{@link #NONE}时才是{@code null}).
	 */
	private final Type type;

	/**
	 * 该类型的可选提供者.
	 */
	private final TypeProvider typeProvider;

	/**
	 * 如果没有可用的解析器, 则使用{@code VariableResolver}或{@code null}.
	 */
	private final VariableResolver variableResolver;

	/**
	 * 如果要推断类型, 则为数组的组件类型或{@code null}.
	 */
	private final ResolvableType componentType;

	/**
	 * 已解析的值的副本.
	 */
	private final Class<?> resolved;

	private final Integer hash;

	private ResolvableType superType;

	private ResolvableType[] interfaces;

	private ResolvableType[] generics;


	/**
	 * 创建新的{@link ResolvableType}以用于缓存Key, 没有前期解析.
	 */
	private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = null;
		this.resolved = null;
		this.hash = calculateHashCode();
	}

	/**
	 * 创建新的{@link ResolvableType}以用于缓存值, 具有前期解析和预先计算的哈希.
	 */
	private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver, Integer hash) {
		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = null;
		this.resolved = resolveClass();
		this.hash = hash;
	}

	/**
	 * 为未缓存创建一个新的{@link ResolvableType}, 具有前期解析但延迟计算的哈希值.
	 */
	private ResolvableType(
			Type type, TypeProvider typeProvider, VariableResolver variableResolver, ResolvableType componentType) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = componentType;
		this.resolved = resolveClass();
		this.hash = null;
	}

	/**
	 * 在{@link Class}的基础上创建一个新的{@link ResolvableType}.
	 * 避免所有{@code instanceof}检查, 以便创建一个直接的{@link Class}包装器.
	 */
	private ResolvableType(Class<?> clazz) {
		this.resolved = (clazz != null ? clazz : Object.class);
		this.type = this.resolved;
		this.typeProvider = null;
		this.variableResolver = null;
		this.componentType = null;
		this.hash = null;
	}


	/**
	 * 返回正在管理的底层Java {@link Type}.
	 * 除{@link #NONE}常量外, 此方法永远不会返回{@code null}.
	 */
	public Type getType() {
		return SerializableTypeWrapper.unwrap(this.type);
	}

	/**
	 * 返回正在管理的底层Java {@link Class}; 或者{@code null}.
	 */
	public Class<?> getRawClass() {
		if (this.type == this.resolved) {
			return this.resolved;
		}
		Type rawType = this.type;
		if (rawType instanceof ParameterizedType) {
			rawType = ((ParameterizedType) rawType).getRawType();
		}
		return (rawType instanceof Class ? (Class<?>) rawType : null);
	}

	/**
	 * 返回可解析类型的底层源.
	 * 将返回{@link Field}, {@link MethodParameter}或 {@link Type}, 具体取决于{@link ResolvableType}的构造方式.
	 * 除{@link #NONE}常量外, 此方法永远不会返回{@code null}.
	 * 此方法主要用于提供对备用JVM语言可能提供的其他类型信息或元数据的访问.
	 */
	public Object getSource() {
		Object source = (this.typeProvider != null ? this.typeProvider.getSource() : null);
		return (source != null ? source : this.type);
	}

	/**
	 * 确定给定对象是否是此{@code ResolvableType}的实例.
	 * 
	 * @param obj 要检查的对象
	 */
	public boolean isInstance(Object obj) {
		return (obj != null && isAssignableFrom(obj.getClass()));
	}

	/**
	 * 确定此{@code ResolvableType}是否可从指定的其他类型派生.
	 * 
	 * @param other 要检查的类型(as a {@code Class})
	 */
	public boolean isAssignableFrom(Class<?> other) {
		return isAssignableFrom(forClass(other), null);
	}

	/**
	 * 确定此{@code ResolvableType}是否可从指定的其他类型派生.
	 * <p>尝试遵循与Java编译器相同的规则, 考虑{@link #resolve() resolved} {@code Class}
	 * 是否{@link Class#isAssignableFrom(Class) 派生自}给定类型, 以及是否所有{@link #getGenerics() generics}都可分配.
	 * 
	 * @param other 要检查的类型 (as a {@code ResolvableType})
	 * 
	 * @return {@code true} 如果指定的其他类型可以派生给此{@code ResolvableType}; 否则{@code false}
	 */
	public boolean isAssignableFrom(ResolvableType other) {
		return isAssignableFrom(other, null);
	}

	private boolean isAssignableFrom(ResolvableType other, Map<Type, Type> matchedBefore) {
		Assert.notNull(other, "ResolvableType must not be null");

		// 如果无法解析类型, 我们就不能分配
		if (this == NONE || other == NONE) {
			return false;
		}

		// 通过委托给组件类型处理数组
		if (isArray()) {
			return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
		}

		if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
			return true;
		}

		// 处理通配符边界
		WildcardBounds ourBounds = WildcardBounds.get(this);
		WildcardBounds typeBounds = WildcardBounds.get(other);

		// X格式可派生到 <? extends Number>
		if (typeBounds != null) {
			return (ourBounds != null && ourBounds.isSameKind(typeBounds) &&
					ourBounds.isAssignableFrom(typeBounds.getBounds()));
		}

		// <? extends Number>格式派生到 X...
		if (ourBounds != null) {
			return ourBounds.isAssignableFrom(other);
		}

		// Main assignability check about to follow
		boolean exactMatch = (matchedBefore != null);  // 检查嵌套的泛型变量...
		boolean checkGenerics = true;
		Class<?> ourResolved = null;
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			// 尝试默认变量解析
			if (this.variableResolver != null) {
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				if (resolved != null) {
					ourResolved = resolved.resolve();
				}
			}
			if (ourResolved == null) {
				// 尝试针对目标类型的变量解析
				if (other.variableResolver != null) {
					ResolvableType resolved = other.variableResolver.resolveVariable(variable);
					if (resolved != null) {
						ourResolved = resolved.resolve();
						checkGenerics = false;
					}
				}
			}
			if (ourResolved == null) {
				// 未解析的类型变量, 可能是嵌套的 -> 从不坚持完全匹配
				exactMatch = false;
			}
		}
		if (ourResolved == null) {
			ourResolved = resolve(Object.class);
		}
		Class<?> otherResolved = other.resolve(Object.class);

		// 需要一个精确的类型匹配, 泛型的List<CharSequence>不能从 List<String> 派生
		if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved)) {
			return false;
		}

		if (checkGenerics) {
			// 递归检查每个泛型
			ResolvableType[] ourGenerics = getGenerics();
			ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
			if (ourGenerics.length != typeGenerics.length) {
				return false;
			}
			if (matchedBefore == null) {
				matchedBefore = new IdentityHashMap<Type, Type>(1);
			}
			matchedBefore.put(this.type, other.type);
			for (int i = 0; i < ourGenerics.length; i++) {
				if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 如果此类型解析为表示数组的Class, 则返回{@code true}.
	 */
	public boolean isArray() {
		if (this == NONE) {
			return false;
		}
		return ((this.type instanceof Class && ((Class<?>) this.type).isArray()) ||
				this.type instanceof GenericArrayType || resolveType().isArray());
	}

	/**
	 * 如果此类型不表示数组, 则返回表示数组的组件类型的ResolvableType或{@link #NONE}.
	 */
	public ResolvableType getComponentType() {
		if (this == NONE) {
			return NONE;
		}
		if (this.componentType != null) {
			return this.componentType;
		}
		if (this.type instanceof Class) {
			Class<?> componentType = ((Class<?>) this.type).getComponentType();
			return forType(componentType, this.variableResolver);
		}
		if (this.type instanceof GenericArrayType) {
			return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
		}
		return resolveType().getComponentType();
	}

	/**
	 * 将此类型作为可解析的{@link Collection}类型返回的便捷方法.
	 * 如果此类型未实现或扩展{@link Collection}, 则返回{@link #NONE}.
	 */
	public ResolvableType asCollection() {
		return as(Collection.class);
	}

	/**
	 * 将此类型作为可解析的{@link Map}类型返回的便捷方法.
	 * 如果此类型未实现或扩展{@link Map}, 则返回{@link #NONE}.
	 */
	public ResolvableType asMap() {
		return as(Map.class);
	}

	/**
	 * 将此类型作为指定类的{@link ResolvableType}返回.
	 * 搜索{@link #getSuperType() supertype} 和 {@link #getInterfaces() interface}层次结构以查找匹配项,
	 * 如果此类型未实现或扩展指定的类, 则返回{@link #NONE}.
	 * 
	 * @param type 所需类型 (通常缩小)
	 * 
	 * @return 将此对象表示为指定类型的{@link ResolvableType}; 如果不能解析为该类型, 则为{@link #NONE}
	 */
	public ResolvableType as(Class<?> type) {
		if (this == NONE) {
			return NONE;
		}
		if (ObjectUtils.nullSafeEquals(resolve(), type)) {
			return this;
		}
		for (ResolvableType interfaceType : getInterfaces()) {
			ResolvableType interfaceAsType = interfaceType.as(type);
			if (interfaceAsType != NONE) {
				return interfaceAsType;
			}
		}
		return getSuperType().as(type);
	}

	/**
	 * 返回表示此类型的直接超类型的{@link ResolvableType}.
	 * 如果没有超类型, 则此方法返回{@link #NONE}.
	 */
	public ResolvableType getSuperType() {
		Class<?> resolved = resolve();
		if (resolved == null || resolved.getGenericSuperclass() == null) {
			return NONE;
		}
		if (this.superType == null) {
			this.superType = forType(SerializableTypeWrapper.forGenericSuperclass(resolved), asVariableResolver());
		}
		return this.superType;
	}

	/**
	 * 返回表示此类型实现的直接接口的{@link ResolvableType}数组.
	 * 如果此类型未实现任何接口, 则返回空数组.
	 */
	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null || ObjectUtils.isEmpty(resolved.getGenericInterfaces())) {
			return EMPTY_TYPES_ARRAY;
		}
		if (this.interfaces == null) {
			this.interfaces = forTypes(SerializableTypeWrapper.forGenericInterfaces(resolved), asVariableResolver());
		}
		return this.interfaces;
	}

	/**
	 * 如果此类型包含泛型参数, 则返回{@code true}.
	 */
	public boolean hasGenerics() {
		return (getGenerics().length > 0);
	}

	/**
	 * 如果此类型仅包含不可解析的泛型, 则返回{@code true}, 即无法替换其任何声明的类型变量.
	 */
	boolean isEntirelyUnresolvable() {
		if (this == NONE) {
			return false;
		}
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 确定底层类型是否具有任何无法解析的泛型:
	 * 或者通过类型本身上的不可解析的类型变量, 或者通过以原始方式实现的泛型接口,
	 * i.e. 不替换该接口的类型变量.
	 * 只有在这两种情况下, 结果才会是{@code true}.
	 */
	public boolean hasUnresolvableGenerics() {
		if (this == NONE) {
			return false;
		}
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds()) {
				return true;
			}
		}
		Class<?> resolved = resolve();
		if (resolved != null) {
			for (Type genericInterface : resolved.getGenericInterfaces()) {
				if (genericInterface instanceof Class) {
					if (forClass((Class<?>) genericInterface).hasGenerics()) {
						return true;
					}
				}
			}
			return getSuperType().hasUnresolvableGenerics();
		}
		return false;
	}

	/**
	 * 确定底层类型是否是无法通过关联的变量解析器解析的类型变量.
	 */
	private boolean isUnresolvableTypeVariable() {
		if (this.type instanceof TypeVariable) {
			if (this.variableResolver == null) {
				return true;
			}
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			ResolvableType resolved = this.variableResolver.resolveVariable(variable);
			if (resolved == null || resolved.isUnresolvableTypeVariable()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定底层类型是否表示没有特定边界的通配符 (i.e., 等于{@code ? extends Object}).
	 */
	private boolean isWildcardWithoutBounds() {
		if (this.type instanceof WildcardType) {
			WildcardType wt = (WildcardType) this.type;
			if (wt.getLowerBounds().length == 0) {
				Type[] upperBounds = wt.getUpperBounds();
				if (upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 返回指定的嵌套级别的{@link ResolvableType}.
	 * See {@link #getNested(int, Map)} for details.
	 * 
	 * @param nestingLevel 嵌套级别
	 * 
	 * @return {@link ResolvableType}类型, 或{@code #NONE}
	 */
	public ResolvableType getNested(int nestingLevel) {
		return getNested(nestingLevel, null);
	}

	/**
	 * 返回指定的嵌套级别的{@link ResolvableType}.
	 * 嵌套级别是指应返回的特定泛型参数.
	 * 嵌套级别为1表示此类型; 2表示第一个嵌套的泛型; 3 表示第二个; 以此类推.
	 * 例如, 给定{@code List<Set<Integer>>}级别1引用{@code List}, 级别2引用{@code Set}, 级别3引用{@code Integer}.
	 * <p>{@code typeIndexesPerLevel}映射可用于引用给定级别的特定泛型.
	 * 例如, 索引0表示{@code Map}键; 然而, 1 表示值.
	 * 如果Map不包含特定级别的值, 则将使用最后一个泛型(e.g. {@code Map}值).
	 * <p>嵌套级别也可能适用于数组类型; 例如, 给定{@code String[]}, 嵌套级别2指的是{@code String}.
	 * <p>如果某个类型没有{@link #hasGenerics() 包含}泛型, 则会考虑{@link #getSuperType() 超类型}层次结构.
	 * 
	 * @param nestingLevel 所需的嵌套级别, 从当前类型的索引为1, 第一个嵌套泛型为2, 第二个为3, 依此类推
	 * @param typeIndexesPerLevel 包含给定嵌套级别的泛型索引的映射 (may be {@code null})
	 * 
	 * @return 嵌套级别的{@link ResolvableType}或{@link #NONE}
	 */
	public ResolvableType getNested(int nestingLevel, Map<Integer, Integer> typeIndexesPerLevel) {
		ResolvableType result = this;
		for (int i = 2; i <= nestingLevel; i++) {
			if (result.isArray()) {
				result = result.getComponentType();
			}
			else {
				// Handle derived types
				while (result != ResolvableType.NONE && !result.hasGenerics()) {
					result = result.getSuperType();
				}
				Integer index = (typeIndexesPerLevel != null ? typeIndexesPerLevel.get(i) : null);
				index = (index == null ? result.getGenerics().length - 1 : index);
				result = result.getGeneric(index);
			}
		}
		return result;
	}

	/**
	 * 返回表示给定索引的泛型参数的{@link ResolvableType}.
	 * 从零开始的索引; 例如, 给定的类型 {@code Map<Integer, List<String>>}, {@code getGeneric(0)}将访问{@code Integer}.
	 * 可以通过指定多个索引来访问嵌套泛型;
	 * 例如{@code getGeneric(1, 0)}将从嵌套的{@code List}访问{@code String}.
	 * 为方便起见, 如果未指定索引, 则返回第一个泛型.
	 * <p>如果指定的索引没有可用的泛型, 则返回{@link #NONE}.
	 * 
	 * @param indexes 引用泛型参数的索引 (可以省略返回第一个泛型)
	 * 
	 * @return 指定泛型的{@link ResolvableType}或{@link #NONE}
	 */
	public ResolvableType getGeneric(int... indexes) {
		ResolvableType[] generics = getGenerics();
		if (indexes == null || indexes.length == 0) {
			return (generics.length == 0 ? NONE : generics[0]);
		}
		ResolvableType generic = this;
		for (int index : indexes) {
			generics = generic.getGenerics();
			if (index < 0 || index >= generics.length) {
				return NONE;
			}
			generic = generics[index];
		}
		return generic;
	}

	/**
	 * 返回表示此类型的泛型参数的{@link ResolvableType}数组.
	 * 如果没有可用的泛型, 则返回空数组.
	 * 如果您需要访问特定的泛型, 请考虑使用{@link #getGeneric(int...)}方法,
	 * 因为它允许访问嵌套泛型并防止{@code IndexOutOfBoundsExceptions}.
	 * 
	 * @return 表示泛型参数的{@link ResolvableType}数组 (never {@code null})
	 */
	public ResolvableType[] getGenerics() {
		if (this == NONE) {
			return EMPTY_TYPES_ARRAY;
		}
		if (this.generics == null) {
			if (this.type instanceof Class) {
				Class<?> typeClass = (Class<?>) this.type;
				this.generics = forTypes(SerializableTypeWrapper.forTypeParameters(typeClass), this.variableResolver);
			}
			else if (this.type instanceof ParameterizedType) {
				Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
				ResolvableType[] generics = new ResolvableType[actualTypeArguments.length];
				for (int i = 0; i < actualTypeArguments.length; i++) {
					generics[i] = forType(actualTypeArguments[i], this.variableResolver);
				}
				this.generics = generics;
			}
			else {
				this.generics = resolveType().getGenerics();
			}
		}
		return this.generics;
	}

	/**
	 * 将{@link #getGenerics() get} 和 {@link #resolve() resolve}泛型参数的便捷方法.
	 * 
	 * @return 已解析的泛型参数 (结果数组永远不会是 {@code null}, 但它可能包含{@code null}元素})
	 */
	public Class<?>[] resolveGenerics() {
		return resolveGenerics(null);
	}

	/**
	 * 将{@link #getGenerics() get}和{@link #resolve() resolve}泛型参数的便捷方法;
	 * 如果任何类型无法解析, 则使用指定的{@code fallback}.
	 * 
	 * @param fallback 如果解析失败, 则使用的回退类 (may be {@code null})
	 * 
	 * @return 已解析的泛型参数 (结果数组永远不会是 {@code null}, 但它可能包含{@code null}元素})
	 */
	public Class<?>[] resolveGenerics(Class<?> fallback) {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve(fallback);
		}
		return resolvedGenerics;
	}

	/**
	 * 将 {@link #getGeneric(int...) get}和{@link #resolve() resolve}特定泛型参数的便捷方法.
	 * 
	 * @param indexes 引用泛型参数的索引 (可以省略, 返回第一个泛型)
	 * 
	 * @return 已解析的{@link Class}或{@code null}
	 */
	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	/**
	 * 将此类型解析为{@link java.lang.Class}; 如果无法解析类型, 则返回{@code null}.
	 * 如果直接解析失败, 此方法将考虑{@link TypeVariable}和{@link WildcardType}的边界;
	 * 但是, 将忽略{@code Object.class}的边界.
	 * 
	 * @return 已解析的{@link Class}或{@code null}
	 */
	public Class<?> resolve() {
		return resolve(null);
	}

	/**
	 * 将此类型解析为{@link java.lang.Class}; 如果无法解析类型, 则返回指定的{@code fallback}.
	 * 如果直接解析失败, 此方法将考虑{@link TypeVariable}和{@link WildcardType}的边界;
	 * 但是, 将忽略{@code Object.class}的边界.
	 * 
	 * @param fallback 如果解析失败, 则使用回退类 (may be {@code null})
	 * 
	 * @return 已解析的{@link Class}或{@code fallback}
	 */
	public Class<?> resolve(Class<?> fallback) {
		return (this.resolved != null ? this.resolved : fallback);
	}

	private Class<?> resolveClass() {
		if (this.type instanceof Class || this.type == null) {
			return (Class<?>) this.type;
		}
		if (this.type instanceof GenericArrayType) {
			Class<?> resolvedComponent = getComponentType().resolve();
			return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
		}
		return resolveType().resolve();
	}

	/**
	 * 通过单个级别解析此类型, 返回已解析的值或{@link #NONE}.
	 * <p>Note: 返回的{@link ResolvableType}应仅用作中介, 因为它无法序列化.
	 */
	ResolvableType resolveType() {
		if (this.type instanceof ParameterizedType) {
			return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
		}
		if (this.type instanceof WildcardType) {
			Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
			if (resolved == null) {
				resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
			}
			return forType(resolved, this.variableResolver);
		}
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			// 尝试默认变量解析
			if (this.variableResolver != null) {
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				if (resolved != null) {
					return resolved;
				}
			}
			// Fallback to bounds
			return forType(resolveBounds(variable.getBounds()), this.variableResolver);
		}
		return NONE;
	}

	private Type resolveBounds(Type[] bounds) {
		if (ObjectUtils.isEmpty(bounds) || Object.class == bounds[0]) {
			return null;
		}
		return bounds[0];
	}

	private ResolvableType resolveVariable(TypeVariable<?> variable) {
		if (this.type instanceof TypeVariable) {
			return resolveType().resolveVariable(variable);
		}
		if (this.type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) this.type;
			TypeVariable<?>[] variables = resolve().getTypeParameters();
			for (int i = 0; i < variables.length; i++) {
				if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
					Type actualType = parameterizedType.getActualTypeArguments()[i];
					return forType(actualType, this.variableResolver);
				}
			}
			if (parameterizedType.getOwnerType() != null) {
				return forType(parameterizedType.getOwnerType(), this.variableResolver).resolveVariable(variable);
			}
		}
		if (this.variableResolver != null) {
			return this.variableResolver.resolveVariable(variable);
		}
		return null;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResolvableType)) {
			return false;
		}

		ResolvableType otherType = (ResolvableType) other;
		if (!ObjectUtils.nullSafeEquals(this.type, otherType.type)) {
			return false;
		}
		if (this.typeProvider != otherType.typeProvider &&
				(this.typeProvider == null || otherType.typeProvider == null ||
				!ObjectUtils.nullSafeEquals(this.typeProvider.getType(), otherType.typeProvider.getType()))) {
			return false;
		}
		if (this.variableResolver != otherType.variableResolver &&
				(this.variableResolver == null || otherType.variableResolver == null ||
				!ObjectUtils.nullSafeEquals(this.variableResolver.getSource(), otherType.variableResolver.getSource()))) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(this.componentType, otherType.componentType)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return (this.hash != null ? this.hash : calculateHashCode());
	}

	private int calculateHashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.type);
		if (this.typeProvider != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.typeProvider.getType());
		}
		if (this.variableResolver != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
		}
		if (this.componentType != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
		}
		return hashCode;
	}

	/**
	 * 将此{@link ResolvableType}适配为{@link VariableResolver}.
	 */
	VariableResolver asVariableResolver() {
		if (this == NONE) {
			return null;
		}
		return new DefaultVariableResolver();
	}

	/**
	 * {@link #NONE}的自定义序列化支持.
	 */
	private Object readResolve() {
		return (this.type == null ? NONE : this);
	}

	/**
	 * 以完全解析的形式返回此类型的String表示形式 (包括任何泛型参数).
	 */
	@Override
	public String toString() {
		if (isArray()) {
			return getComponentType() + "[]";
		}
		if (this.resolved == null) {
			return "?";
		}
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
				// 不要为toString()的变量边界而烦恼...
				// 在自引用的情况下可以导致无限递归
				return "?";
			}
		}
		StringBuilder result = new StringBuilder(this.resolved.getName());
		if (hasGenerics()) {
			result.append('<');
			result.append(StringUtils.arrayToDelimitedString(getGenerics(), ", "));
			result.append('>');
		}
		return result.toString();
	}


	// Factory methods

	/**
	 * 为指定的{@link Class}返回{@link ResolvableType}, 使用完整的泛型类型信息进行可指派性检查.
	 * 例如: {@code ResolvableType.forClass(MyArrayList.class)}.
	 * 
	 * @param clazz 要内省的类 ({@code null}在语义上等同于用于典型用例的{@code Object.class})
	 * 
	 * @return 指定的类的{@link ResolvableType}
	 */
	public static ResolvableType forClass(Class<?> clazz) {
		return new ResolvableType(clazz);
	}

	/**
	 * 为指定的{@link Class}返回{@link ResolvableType}, 仅针对原始类进行可派生性检查
	 * (类似于{@link Class#isAssignableFrom}, 这可用作包装器).
	 * 例如: {@code ResolvableType.forRawClass(List.class)}.
	 * 
	 * @param clazz 要内省的类 ({@code null}在语义上等同于用于典型用例的{@code Object.class})
	 * 
	 * @return 指定的类的{@link ResolvableType}
	 */
	public static ResolvableType forRawClass(Class<?> clazz) {
		return new ResolvableType(clazz) {
			@Override
			public ResolvableType[] getGenerics() {
				return EMPTY_TYPES_ARRAY;
			}
			@Override
			public boolean isAssignableFrom(Class<?> other) {
				return ClassUtils.isAssignable(getRawClass(), other);
			}
			@Override
			public boolean isAssignableFrom(ResolvableType other) {
				Class<?> otherClass = other.getRawClass();
				return (otherClass != null && ClassUtils.isAssignable(getRawClass(), otherClass));
			}
		};
	}

	/**
	 * 返回给定的实现类的指定基类型 (接口或基类)的{@link ResolvableType}.
	 * 例如: {@code ResolvableType.forClass(List.class, MyArrayList.class)}.
	 * 
	 * @param baseType 基类型 (must not be {@code null})
	 * @param implementationClass 实现类
	 * 
	 * @return 对于由给定实现类支持的指定基类型的{@link ResolvableType}
	 */
	public static ResolvableType forClass(Class<?> baseType, Class<?> implementationClass) {
		Assert.notNull(baseType, "Base type must not be null");
		ResolvableType asType = forType(implementationClass).as(baseType);
		return (asType == NONE ? forType(baseType) : asType);
	}

	/**
	 * 使用预先声明的泛型, 返回指定{@link Class}的{@link ResolvableType}.
	 * 
	 * @param clazz 要内省的类 (或接口)
	 * @param generics 类的泛型
	 * 
	 * @return 指定的类和泛型的{@link ResolvableType}
	 */
	public static ResolvableType forClassWithGenerics(Class<?> clazz, Class<?>... generics) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(generics, "Generics array must not be null");
		ResolvableType[] resolvableGenerics = new ResolvableType[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvableGenerics[i] = forClass(generics[i]);
		}
		return forClassWithGenerics(clazz, resolvableGenerics);
	}

	/**
	 * 使用预先声明的泛型, 返回指定{@link Class}的{@link ResolvableType}.
	 * 
	 * @param clazz 要内省的类 (或接口)
	 * @param generics 类的泛型
	 * 
	 * @return 指定类和泛型的{@link ResolvableType}
	 */
	public static ResolvableType forClassWithGenerics(Class<?> clazz, ResolvableType... generics) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(generics, "Generics array must not be null");
		TypeVariable<?>[] variables = clazz.getTypeParameters();
		Assert.isTrue(variables.length == generics.length, "Mismatched number of generics specified");

		Type[] arguments = new Type[generics.length];
		for (int i = 0; i < generics.length; i++) {
			ResolvableType generic = generics[i];
			Type argument = (generic != null ? generic.getType() : null);
			arguments[i] = (argument != null ? argument : variables[i]);
		}

		ParameterizedType syntheticType = new SyntheticParameterizedType(clazz, arguments);
		return forType(syntheticType, new TypeVariablesVariableResolver(variables, generics));
	}

	/**
	 * 返回指定的实例的{@link ResolvableType}.
	 * 该实例不传达泛型信息, 但如果它实现了{@link ResolvableTypeProvider},
	 * 则可以使用比基于{@link #forClass(Class) Class实例}的简单{@link ResolvableType}更精确的{@link ResolvableType}.
	 * 
	 * @param instance 实例
	 * 
	 * @return 指定的实例的{@link ResolvableType}
	 */
	public static ResolvableType forInstance(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		if (instance instanceof ResolvableTypeProvider) {
			ResolvableType type = ((ResolvableTypeProvider) instance).getResolvableType();
			if (type != null) {
				return type;
			}
		}
		return ResolvableType.forClass(instance.getClass());
	}

	/**
	 * 返回指定的{@link Field}的{@link ResolvableType}.
	 * 
	 * @param field 源字段
	 * 
	 * @return 指定字段的{@link ResolvableType}
	 */
	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return forType(null, new FieldTypeProvider(field), null);
	}

	/**
	 * 使用给定的实现, 返回指定{@link Field}的{@link ResolvableType}.
	 * <p>当声明字段的类包含实现类满足的泛型参数变量时, 请使用此变体.
	 * 
	 * @param field 源字段
	 * @param implementationClass 实现类
	 * 
	 * @return 指定字段的{@link ResolvableType}
	 */
	public static ResolvableType forField(Field field, Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
	}

	/**
	 * 使用给定的实现, 返回指定{@link Field}的{@link ResolvableType}.
	 * <p>当声明字段的类包含实现类型满足的泛型参数变量时, 请使用此变体.
	 * 
	 * @param field 源字段
	 * @param implementationType 实现类型
	 * 
	 * @return 指定字段的{@link ResolvableType}
	 */
	public static ResolvableType forField(Field field, ResolvableType implementationType) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = (implementationType != null ? implementationType : NONE);
		owner = owner.as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
	}

	/**
	 * 使用给定的嵌套级别, 返回指定{@link Field}的{@link ResolvableType}.
	 * 
	 * @param field 源字段
	 * @param nestingLevel 嵌套级别 (1 表示外层; 2 表示嵌套的泛型类型; 等)
	 */
	public static ResolvableType forField(Field field, int nestingLevel) {
		Assert.notNull(field, "Field must not be null");
		return forType(null, new FieldTypeProvider(field), null).getNested(nestingLevel);
	}

	/**
	 * 使用给定的实现和给定的嵌套级别, 返回指定的{@link Field}的{@link ResolvableType}.
	 * <p>当声明字段的类包含实现类满足的泛型参数变量时, 请使用此变体.
	 * 
	 * @param field 源字段
	 * @param nestingLevel 嵌套级别 (1 表示外层; 2 表示嵌套的泛型类型; 等)
	 * @param implementationClass 实现类
	 * 
	 * @return 指定字段的{@link ResolvableType}
	 */
	public static ResolvableType forField(Field field, int nestingLevel, Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver()).getNested(nestingLevel);
	}

	/**
	 * 返回指定的{@link Constructor}参数的{@link ResolvableType}.
	 * 
	 * @param constructor 源构造函数 (must not be {@code null})
	 * @param parameterIndex 参数索引
	 * 
	 * @return 指定构造函数参数的{@link ResolvableType}
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex) {
		Assert.notNull(constructor, "Constructor must not be null");
		return forMethodParameter(new MethodParameter(constructor, parameterIndex));
	}

	/**
	 * 使用给定的实现, 返回指定的{@link Constructor}参数{@link ResolvableType}.
	 * 当声明构造函数的类包含实现类满足的泛型参数变量时, 请使用此变体.
	 * 
	 * @param constructor 源构造函数 (must not be {@code null})
	 * @param parameterIndex 参数索引
	 * @param implementationClass 实现类
	 * 
	 * @return 指定构造函数参数的{@link ResolvableType}
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex,
			Class<?> implementationClass) {

		Assert.notNull(constructor, "Constructor must not be null");
		MethodParameter methodParameter = new MethodParameter(constructor, parameterIndex);
		methodParameter.setContainingClass(implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 返回指定的{@link Method}返回类型的{@link ResolvableType}.
	 * 
	 * @param method 方法返回类型的源
	 * 
	 * @return 指定方法返回类型的{@link ResolvableType}
	 */
	public static ResolvableType forMethodReturnType(Method method) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(new MethodParameter(method, -1));
	}

	/**
	 * 返回指定的{@link Method}返回类型的{@link ResolvableType}.
	 * 当声明方法的类包含实现类满足的泛型参数变量时, 请使用此变体.
	 * 
	 * @param method 方法返回类型的源
	 * @param implementationClass 实现类
	 * 
	 * @return 指定方法返回类型的{@link ResolvableType}
	 */
	public static ResolvableType forMethodReturnType(Method method, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		MethodParameter methodParameter = new MethodParameter(method, -1);
		methodParameter.setContainingClass(implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 返回指定的{@link Method}参数的{@link ResolvableType}.
	 * 
	 * @param method 源方法 (must not be {@code null})
	 * @param parameterIndex 参数索引
	 * 
	 * @return 指定方法返回类型的{@link ResolvableType}
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(new MethodParameter(method, parameterIndex));
	}

	/**
	 * 使用给定的实现, 返回指定的{@link Method}参数的{@link ResolvableType}.
	 * 当声明方法的类包含实现类满足的泛型参数变量时, 请使用此变体.
	 * 
	 * @param method 源方法 (must not be {@code null})
	 * @param parameterIndex 参数索引
	 * @param implementationClass 实现类
	 * 
	 * @return 指定方法参数的{@link ResolvableType}
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		MethodParameter methodParameter = new MethodParameter(method, parameterIndex);
		methodParameter.setContainingClass(implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 返回指定的{@link MethodParameter}的{@link ResolvableType}.
	 * 
	 * @param methodParameter 源方法参数 (must not be {@code null})
	 * 
	 * @return 指定方法参数的{@link ResolvableType}
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
		return forMethodParameter(methodParameter, (Type) null);
	}

	/**
	 * 返回具有给定实现类型的指定{@link MethodParameter}的{@link ResolvableType}.
	 * 当声明方法的类包含实现类型满足的泛型参数变量时, 请使用此变体.
	 * 
	 * @param methodParameter 源方法参数 (must not be {@code null})
	 * @param implementationType 实现类型
	 * 
	 * @return 指定方法参数的{@link ResolvableType}
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter, ResolvableType implementationType) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		implementationType = (implementationType != null ? implementationType :
				forType(methodParameter.getContainingClass()));
		ResolvableType owner = implementationType.as(methodParameter.getDeclaringClass());
		return forType(null, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
				getNested(methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
	}

	/**
	 * 返回指定的{@link MethodParameter}的{@link ResolvableType}, 覆盖目标类型以使用特定的给定类型解析.
	 * 
	 * @param methodParameter 源方法参数 (must not be {@code null})
	 * @param targetType 要解析的类型 (方法参数类型的一部分)
	 * 
	 * @return 指定方法参数的{@link ResolvableType}
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter, Type targetType) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
		return forType(targetType, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
				getNested(methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
	}

	/**
	 * 解析给定{@code MethodParameter}的顶级参数类型.
	 * 
	 * @param methodParameter 要解析的方法参数
	 */
	static void resolveMethodParameter(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
		methodParameter.setParameterType(
				forType(null, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).resolve());
	}

	/**
	 * 将{@link ResolvableType}作为指定{@code componentType}的数组返回.
	 * 
	 * @param componentType 组件类型
	 * 
	 * @return 作为指定组件类型的数组的{@link ResolvableType}
	 */
	public static ResolvableType forArrayComponent(ResolvableType componentType) {
		Assert.notNull(componentType, "Component type must not be null");
		Class<?> arrayClass = Array.newInstance(componentType.resolve(), 0).getClass();
		return new ResolvableType(arrayClass, null, null, componentType);
	}

	private static ResolvableType[] forTypes(Type[] types, VariableResolver owner) {
		ResolvableType[] result = new ResolvableType[types.length];
		for (int i = 0; i < types.length; i++) {
			result[i] = forType(types[i], owner);
		}
		return result;
	}

	/**
	 * 返回指定的{@link Type}的{@link ResolvableType}.
	 * Note: 生成的{@link ResolvableType}可能不是{@link Serializable}.
	 * 
	 * @param type 源类型或{@code null}
	 * 
	 * @return 指定{@link Type}的{@link ResolvableType}
	 */
	public static ResolvableType forType(Type type) {
		return forType(type, null, null);
	}

	/**
	 * 返回给定所有者类型支持的指定{@link Type}的{@link ResolvableType}.
	 * Note: 生成的{@link ResolvableType}可能不是{@link Serializable}.
	 * 
	 * @param type 源类型或{@code null}
	 * @param owner 用于解析变量的所有者类型
	 * 
	 * @return 指定{@link Type}和所有者的{@link ResolvableType}
	 */
	public static ResolvableType forType(Type type, ResolvableType owner) {
		VariableResolver variableResolver = null;
		if (owner != null) {
			variableResolver = owner.asVariableResolver();
		}
		return forType(type, variableResolver);
	}


	/**
	 * 返回指定的{@link ParameterizedTypeReference}的{@link ResolvableType}.
	 * Note: 生成的{@link ResolvableType}可能不是{@link Serializable}.
	 * 
	 * @param typeReference 从中获取源类型的引用
	 * 
	 * @return 指定{@link ParameterizedTypeReference}的{@link ResolvableType}
	 */
	public static ResolvableType forType(ParameterizedTypeReference<?> typeReference) {
		return forType(typeReference.getType(), null, null);
	}

	/**
	 * 返回给定{@link VariableResolver}支持的指定{@link Type}的{@link ResolvableType}.
	 * 
	 * @param type 源类型或{@code null}
	 * @param variableResolver 变量解析器或{@code null}
	 * 
	 * @return 指定{@link Type}和{@link VariableResolver}的{@link ResolvableType}
	 */
	static ResolvableType forType(Type type, VariableResolver variableResolver) {
		return forType(type, null, variableResolver);
	}

	/**
	 * 返回给定{@link VariableResolver}支持的指定{@link Type}的{@link ResolvableType}.
	 * 
	 * @param type 源类型或{@code null}
	 * @param typeProvider 类型提供者或{@code null}
	 * @param variableResolver 变量解析器或{@code null}
	 * 
	 * @return 指定{@link Type}和{@link VariableResolver}的{@link ResolvableType}
	 */
	static ResolvableType forType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
		if (type == null && typeProvider != null) {
			type = SerializableTypeWrapper.forTypeProvider(typeProvider);
		}
		if (type == null) {
			return NONE;
		}

		// 对于简单的Class引用, 立即构建包装器 - 不需要昂贵的解析, 因此不值得缓存...
		if (type instanceof Class) {
			return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
		}

		// 由于没有清理线程等, 因此清除访问时的空条目.
		cache.purgeUnreferencedEntries();

		// 检查缓存 - 可能有一个之前已经解析的ResolvableType...
		ResolvableType key = new ResolvableType(type, typeProvider, variableResolver);
		ResolvableType resolvableType = cache.get(key);
		if (resolvableType == null) {
			resolvableType = new ResolvableType(type, typeProvider, variableResolver, key.hash);
			cache.put(resolvableType, resolvableType);
		}
		return resolvableType;
	}

	/**
	 * 清空内部{@code ResolvableType}/{@code SerializableTypeWrapper}缓存.
	 */
	public static void clearCache() {
		cache.clear();
		SerializableTypeWrapper.cache.clear();
	}


	/**
	 * 用于解析{@link TypeVariable}的策略接口.
	 */
	interface VariableResolver extends Serializable {

		/**
		 * 返回解析器的源 (用于 hashCode 和 equals).
		 */
		Object getSource();

		/**
		 * 解析指定的变量.
		 * 
		 * @param variable 要解析的变量
		 * 
		 * @return 已解析的变量, 或{@code null}
		 */
		ResolvableType resolveVariable(TypeVariable<?> variable);
	}


	@SuppressWarnings("serial")
	private class DefaultVariableResolver implements VariableResolver {

		@Override
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			return ResolvableType.this.resolveVariable(variable);
		}

		@Override
		public Object getSource() {
			return ResolvableType.this;
		}
	}


	@SuppressWarnings("serial")
	private static class TypeVariablesVariableResolver implements VariableResolver {

		private final TypeVariable<?>[] variables;

		private final ResolvableType[] generics;

		public TypeVariablesVariableResolver(TypeVariable<?>[] variables, ResolvableType[] generics) {
			this.variables = variables;
			this.generics = generics;
		}

		@Override
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			for (int i = 0; i < this.variables.length; i++) {
				TypeVariable<?> v1 = SerializableTypeWrapper.unwrap(this.variables[i]);
				TypeVariable<?> v2 = SerializableTypeWrapper.unwrap(variable);
				if (ObjectUtils.nullSafeEquals(v1, v2)) {
					return this.generics[i];
				}
			}
			return null;
		}

		@Override
		public Object getSource() {
			return this.generics;
		}
	}


	private static final class SyntheticParameterizedType implements ParameterizedType, Serializable {

		private final Type rawType;

		private final Type[] typeArguments;

		public SyntheticParameterizedType(Type rawType, Type[] typeArguments) {
			this.rawType = rawType;
			this.typeArguments = typeArguments;
		}

		@Override  // on Java 8
		@UsesJava8
		public String getTypeName() {
			StringBuilder result = new StringBuilder(this.rawType.getTypeName());
			if (this.typeArguments.length > 0) {
				result.append('<');
				for (int i = 0; i < this.typeArguments.length; i++) {
					if (i > 0) {
						result.append(", ");
					}
					result.append(this.typeArguments[i].getTypeName());
				}
				result.append('>');
			}
			return result.toString();
		}

		@Override
		public Type getOwnerType() {
			return null;
		}

		@Override
		public Type getRawType() {
			return this.rawType;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return this.typeArguments;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ParameterizedType)) {
				return false;
			}
			ParameterizedType otherType = (ParameterizedType) other;
			return (otherType.getOwnerType() == null && this.rawType.equals(otherType.getRawType()) &&
					Arrays.equals(this.typeArguments, otherType.getActualTypeArguments()));
		}

		@Override
		public int hashCode() {
			return (this.rawType.hashCode() * 31 + Arrays.hashCode(this.typeArguments));
		}
	}


	/**
	 * 内部帮助器, 来处理来自{@link WildcardType}的边界.
	 */
	private static class WildcardBounds {

		private final Kind kind;

		private final ResolvableType[] bounds;

		/**
		 * @param kind 边界的种类
		 * @param bounds 边界
		 */
		public WildcardBounds(Kind kind, ResolvableType[] bounds) {
			this.kind = kind;
			this.bounds = bounds;
		}

		/**
		 * 如果此边界与指定边界种类相同, 则返回{@code true}.
		 */
		public boolean isSameKind(WildcardBounds bounds) {
			return this.kind == bounds.kind;
		}

		/**
		 * 如果此边界可分配给所有指定类型, 则返回{@code true}.
		 * 
		 * @param types 要测试的类型
		 * 
		 * @return {@code true} 如果此边界可分配给所有类型
		 */
		public boolean isAssignableFrom(ResolvableType... types) {
			for (ResolvableType bound : this.bounds) {
				for (ResolvableType type : types) {
					if (!isAssignable(bound, type)) {
						return false;
					}
				}
			}
			return true;
		}

		private boolean isAssignable(ResolvableType source, ResolvableType from) {
			return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
		}

		/**
		 * 返回底层边界.
		 */
		public ResolvableType[] getBounds() {
			return this.bounds;
		}

		/**
		 * 获取指定类型的{@link WildcardBounds}实例; 如果指定的类型无法解析为{@link WildcardType}, 则返回{@code null}.
		 * 
		 * @param type 源类型
		 * 
		 * @return {@link WildcardBounds}实例或{@code null}
		 */
		public static WildcardBounds get(ResolvableType type) {
			ResolvableType resolveToWildcard = type;
			while (!(resolveToWildcard.getType() instanceof WildcardType)) {
				if (resolveToWildcard == NONE) {
					return null;
				}
				resolveToWildcard = resolveToWildcard.resolveType();
			}
			WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
			Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
			Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
			ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
			for (int i = 0; i < bounds.length; i++) {
				resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
			}
			return new WildcardBounds(boundsType, resolvableBounds);
		}

		/**
		 * 边界的种类.
		 */
		enum Kind {UPPER, LOWER}
	}
}
