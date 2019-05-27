package org.springframework.expression.spel.support;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 一个功能强大的{@link PropertyAccessor}, 它使用反射来访问属性以进行读取, 也可能用于写入.
 *
 * <p>可以通过 public getter方法 (在读取时)或公共 public setter方法 (在写入时), 以及作为public字段引用属性.
 */
public class ReflectivePropertyAccessor implements PropertyAccessor {

	private static final Set<Class<?>> ANY_TYPES = Collections.emptySet();

	private static final Set<Class<?>> BOOLEAN_TYPES;

	static {
		Set<Class<?>> booleanTypes = new HashSet<Class<?>>(4);
		booleanTypes.add(Boolean.class);
		booleanTypes.add(Boolean.TYPE);
		BOOLEAN_TYPES = Collections.unmodifiableSet(booleanTypes);
	}


	private final boolean allowWrite;

	private final Map<PropertyCacheKey, InvokerPair> readerCache =
			new ConcurrentHashMap<PropertyCacheKey, InvokerPair>(64);

	private final Map<PropertyCacheKey, Member> writerCache =
			new ConcurrentHashMap<PropertyCacheKey, Member>(64);

	private final Map<PropertyCacheKey, TypeDescriptor> typeDescriptorCache =
			new ConcurrentHashMap<PropertyCacheKey, TypeDescriptor>(64);

	private final Map<Class<?>, Method[]> sortedMethodsCache =
			new ConcurrentHashMap<Class<?>, Method[]>(64);

	private volatile InvokerPair lastReadInvokerPair;


	/**
	 * 创建一个新的属性访问器, 用于读取和写入.
	 */
	public ReflectivePropertyAccessor() {
		this.allowWrite = true;
	}

	/**
	 * 创建一个新的属性访问器来读取和写入.
	 * 
	 * @param allowWrite 是否允许写操作
	 */
	public ReflectivePropertyAccessor(boolean allowWrite) {
		this.allowWrite = allowWrite;
	}


	/**
	 * 返回{@code null}, 这意味着这是一个通用访问器.
	 */
	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return null;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		if (target == null) {
			return false;
		}

		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (type.isArray() && name.equals("length")) {
			return true;
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		if (this.readerCache.containsKey(cacheKey)) {
			return true;
		}

		Method method = findGetterForProperty(name, type, target);
		if (method != null) {
			// 将其视为属性...
			// readerCache只包含gettable属性 (现在不要担心setter).
			Property property = new Property(type, method, null);
			TypeDescriptor typeDescriptor = new TypeDescriptor(property);
			this.readerCache.put(cacheKey, new InvokerPair(method, typeDescriptor));
			this.typeDescriptorCache.put(cacheKey, typeDescriptor);
			return true;
		}
		else {
			Field field = findField(name, type, target);
			if (field != null) {
				TypeDescriptor typeDescriptor = new TypeDescriptor(field);
				this.readerCache.put(cacheKey, new InvokerPair(field, typeDescriptor));
				this.typeDescriptorCache.put(cacheKey, typeDescriptor);
				return true;
			}
		}

		return false;
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		if (target == null) {
			throw new AccessException("Cannot read property of null target");
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		if (type.isArray() && name.equals("length")) {
			if (target instanceof Class) {
				throw new AccessException("Cannot access length on array class itself");
			}
			return new TypedValue(Array.getLength(target));
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		InvokerPair invoker = this.readerCache.get(cacheKey);
		this.lastReadInvokerPair = invoker;

		if (invoker == null || invoker.member instanceof Method) {
			Method method = (Method) (invoker != null ? invoker.member : null);
			if (method == null) {
				method = findGetterForProperty(name, type, target);
				if (method != null) {
					// 将其视为属性...
					// readerCache只包含gettable属性 (现在不要担心setter).
					Property property = new Property(type, method, null);
					TypeDescriptor typeDescriptor = new TypeDescriptor(property);
					invoker = new InvokerPair(method, typeDescriptor);
					this.lastReadInvokerPair = invoker;
					this.readerCache.put(cacheKey, invoker);
				}
			}
			if (method != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					Object value = method.invoke(target);
					return new TypedValue(value, invoker.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter method", ex);
				}
			}
		}

		if (invoker == null || invoker.member instanceof Field) {
			Field field = (Field) (invoker == null ? null : invoker.member);
			if (field == null) {
				field = findField(name, type, target);
				if (field != null) {
					invoker = new InvokerPair(field, new TypeDescriptor(field));
					this.lastReadInvokerPair = invoker;
					this.readerCache.put(cacheKey, invoker);
				}
			}
			if (field != null) {
				try {
					ReflectionUtils.makeAccessible(field);
					Object value = field.get(target);
					return new TypedValue(value, invoker.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		throw new AccessException("Neither getter method nor field found for property '" + name + "'");
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		if (!this.allowWrite || target == null) {
			return false;
		}

		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		if (this.writerCache.containsKey(cacheKey)) {
			return true;
		}

		Method method = findSetterForProperty(name, type, target);
		if (method != null) {
			// 将其视为属性
			Property property = new Property(type, null, method);
			TypeDescriptor typeDescriptor = new TypeDescriptor(property);
			this.writerCache.put(cacheKey, method);
			this.typeDescriptorCache.put(cacheKey, typeDescriptor);
			return true;
		}
		else {
			Field field = findField(name, type, target);
			if (field != null) {
				this.writerCache.put(cacheKey, field);
				this.typeDescriptorCache.put(cacheKey, new TypeDescriptor(field));
				return true;
			}
		}

		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		if (!this.allowWrite) {
			throw new AccessException("PropertyAccessor for property '" + name +
					"' on target [" + target + "] does not allow write operations");
		}

		if (target == null) {
			throw new AccessException("Cannot write property on null target");
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		Object possiblyConvertedNewValue = newValue;
		TypeDescriptor typeDescriptor = getTypeDescriptor(context, target, name);
		if (typeDescriptor != null) {
			try {
				possiblyConvertedNewValue = context.getTypeConverter().convertValue(
						newValue, TypeDescriptor.forObject(newValue), typeDescriptor);
			}
			catch (EvaluationException evaluationException) {
				throw new AccessException("Type conversion failure", evaluationException);
			}
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		Member cachedMember = this.writerCache.get(cacheKey);

		if (cachedMember == null || cachedMember instanceof Method) {
			Method method = (Method) cachedMember;
			if (method == null) {
				method = findSetterForProperty(name, type, target);
				if (method != null) {
					cachedMember = method;
					this.writerCache.put(cacheKey, cachedMember);
				}
			}
			if (method != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					method.invoke(target, possiblyConvertedNewValue);
					return;
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through setter method", ex);
				}
			}
		}

		if (cachedMember == null || cachedMember instanceof Field) {
			Field field = (Field) cachedMember;
			if (field == null) {
				field = findField(name, type, target);
				if (field != null) {
					cachedMember = field;
					this.writerCache.put(cacheKey, cachedMember);
				}
			}
			if (field != null) {
				try {
					ReflectionUtils.makeAccessible(field);
					field.set(target, possiblyConvertedNewValue);
					return;
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		throw new AccessException("Neither setter method nor field found for property '" + name + "'");
	}

	/**
	 * @deprecated as of 4.3.15 since it is not used within the framework anymore
	 */
	@Deprecated
	public Member getLastReadInvokerPair() {
		InvokerPair lastReadInvoker = this.lastReadInvokerPair;
		return (lastReadInvoker != null ? lastReadInvoker.member : null);
	}


	private TypeDescriptor getTypeDescriptor(EvaluationContext context, Object target, String name) {
		if (target == null) {
			return null;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		if (type.isArray() && name.equals("length")) {
			return TypeDescriptor.valueOf(Integer.TYPE);
		}
		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		TypeDescriptor typeDescriptor = this.typeDescriptorCache.get(cacheKey);
		if (typeDescriptor == null) {
			// 尝试填充缓存条目
			try {
				if (canRead(context, target, name) || canWrite(context, target, name)) {
					typeDescriptor = this.typeDescriptorCache.get(cacheKey);
				}
			}
			catch (AccessException ex) {
				// 继续使用null类型描述符
			}
		}
		return typeDescriptor;
	}

	private Method findGetterForProperty(String propertyName, Class<?> clazz, Object target) {
		Method method = findGetterForProperty(propertyName, clazz, target instanceof Class);
		if (method == null && target instanceof Class) {
			method = findGetterForProperty(propertyName, target.getClass(), false);
		}
		return method;
	}

	private Method findSetterForProperty(String propertyName, Class<?> clazz, Object target) {
		Method method = findSetterForProperty(propertyName, clazz, target instanceof Class);
		if (method == null && target instanceof Class) {
			method = findSetterForProperty(propertyName, target.getClass(), false);
		}
		return method;
	}

	/**
	 * 找到指定属性的getter方法.
	 */
	protected Method findGetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		Method method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
				 "get", clazz, mustBeStatic, 0, ANY_TYPES);
		if (method == null) {
			method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
					 "is", clazz, mustBeStatic, 0, BOOLEAN_TYPES);
		}
		return method;
	}

	/**
	 * 查找指定属性的setter方法.
	 */
	protected Method findSetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		return findMethodForProperty(getPropertyMethodSuffixes(propertyName),
				"set", clazz, mustBeStatic, 1, ANY_TYPES);
	}

	private Method findMethodForProperty(String[] methodSuffixes, String prefix, Class<?> clazz,
			boolean mustBeStatic, int numberOfParams, Set<Class<?>> requiredReturnTypes) {

		Method[] methods = getSortedMethods(clazz);
		for (String methodSuffix : methodSuffixes) {
			for (Method method : methods) {
				if (isCandidateForProperty(method, clazz) && method.getName().equals(prefix + methodSuffix) &&
						method.getParameterTypes().length == numberOfParams &&
						(!mustBeStatic || Modifier.isStatic(method.getModifiers())) &&
						(requiredReturnTypes.isEmpty() || requiredReturnTypes.contains(method.getReturnType()))) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * 确定给定的{@code Method}是否是给定目标类的实例上的属性访问的候选者.
	 * <p>默认实现将任何方法视为候选方法, 即使对于{@link Object}基类上的非用户声明的属性也是如此.
	 * 
	 * @param method 要评估的方法
	 * @param targetClass 正在被内省的具体目标类
	 */
	protected boolean isCandidateForProperty(Method method, Class<?> targetClass) {
		return true;
	}

	/**
	 * 返回类方法, 以非桥接方法优先级更高排序.
	 */
	private Method[] getSortedMethods(Class<?> clazz) {
		Method[] methods = this.sortedMethodsCache.get(clazz);
		if (methods == null) {
			methods = clazz.getMethods();
			Arrays.sort(methods, new Comparator<Method>() {
				@Override
				public int compare(Method o1, Method o2) {
					return (o1.isBridge() == o2.isBridge()) ? 0 : (o1.isBridge() ? 1 : -1);
				}
			});
			this.sortedMethodsCache.put(clazz, methods);
		}
		return methods;
	}

	/**
	 * 返回给定属性名称的方法后缀.
	 * 默认实现使用JavaBean约定, 并对'xY'形式的属性提供额外支持, 其中方法'getXY()'优先于'getxY()'的JavaBean约定使用.
	 */
	protected String[] getPropertyMethodSuffixes(String propertyName) {
		String suffix = getPropertyMethodSuffix(propertyName);
		if (suffix.length() > 0 && Character.isUpperCase(suffix.charAt(0))) {
			return new String[] {suffix};
		}
		return new String[] {suffix, StringUtils.capitalize(suffix)};
	}

	/**
	 * 返回给定属性名称的方法后缀. 默认实现使用JavaBean约定.
	 */
	protected String getPropertyMethodSuffix(String propertyName) {
		if (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(1))) {
			return propertyName;
		}
		return StringUtils.capitalize(propertyName);
	}

	private Field findField(String name, Class<?> clazz, Object target) {
		Field field = findField(name, clazz, target instanceof Class);
		if (field == null && target instanceof Class) {
			field = findField(name, target.getClass(), false);
		}
		return field;
	}

	/**
	 * 在指定的类上查找特定名称的字段.
	 */
	protected Field findField(String name, Class<?> clazz, boolean mustBeStatic) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers()))) {
				return field;
			}
		}
		// 将显式搜索超类并实现接口, 尽管它不是必需的 - 但是, 参见SPR-10125.
		if (clazz.getSuperclass() != null) {
			Field field = findField(name, clazz.getSuperclass(), mustBeStatic);
			if (field != null) {
				return field;
			}
		}
		for (Class<?> implementedInterface : clazz.getInterfaces()) {
			Field field = findField(name, implementedInterface, mustBeStatic);
			if (field != null) {
				return field;
			}
		}
		return null;
	}

	/**
	 * 尝试创建针对特定类的特定名称的属性定制的优化属性访问器.
	 * 一般的ReflectivePropertyAccessor将始终工作但不是最佳的, 因为每次调用read()时需要查找使用哪个反射成员 (方法/字段).
	 * 如果无法构建更优化的访问器, 此方法将返回ReflectivePropertyAccessor实例.
	 * <p>Note: 最佳访问器当前仅可用于读取尝试.
	 * 如果需要读写访问器, 请不要调用此方法.
	 */
	public PropertyAccessor createOptimalAccessor(EvaluationContext context, Object target, String name) {
		// Don't be clever for arrays or a null target...
		if (target == null) {
			return this;
		}
		Class<?> clazz = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (clazz.isArray()) {
			return this;
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(clazz, name, target instanceof Class);
		InvokerPair invocationTarget = this.readerCache.get(cacheKey);

		if (invocationTarget == null || invocationTarget.member instanceof Method) {
			Method method = (Method) (invocationTarget != null ? invocationTarget.member : null);
			if (method == null) {
				method = findGetterForProperty(name, clazz, target);
				if (method != null) {
					invocationTarget = new InvokerPair(method, new TypeDescriptor(new MethodParameter(method, -1)));
					ReflectionUtils.makeAccessible(method);
					this.readerCache.put(cacheKey, invocationTarget);
				}
			}
			if (method != null) {
				return new OptimalPropertyAccessor(invocationTarget);
			}
		}

		if (invocationTarget == null || invocationTarget.member instanceof Field) {
			Field field = (invocationTarget != null ? (Field) invocationTarget.member : null);
			if (field == null) {
				field = findField(name, clazz, target instanceof Class);
				if (field != null) {
					invocationTarget = new InvokerPair(field, new TypeDescriptor(field));
					ReflectionUtils.makeAccessible(field);
					this.readerCache.put(cacheKey, invocationTarget);
				}
			}
			if (field != null) {
				return new OptimalPropertyAccessor(invocationTarget);
			}
		}

		return this;
	}


	/**
	 * 捕获成员 (方法/字段) 以反射调用以访问属性值, 以及反射调用返回的值的类型描述符.
	 */
	private static class InvokerPair {

		final Member member;

		final TypeDescriptor typeDescriptor;

		public InvokerPair(Member member, TypeDescriptor typeDescriptor) {
			this.member = member;
			this.typeDescriptor = typeDescriptor;
		}
	}


	private static final class PropertyCacheKey implements Comparable<PropertyCacheKey> {

		private final Class<?> clazz;

		private final String property;

		private boolean targetIsClass;

		public PropertyCacheKey(Class<?> clazz, String name, boolean targetIsClass) {
			this.clazz = clazz;
			this.property = name;
			this.targetIsClass = targetIsClass;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof PropertyCacheKey)) {
				return false;
			}
			PropertyCacheKey otherKey = (PropertyCacheKey) other;
			return (this.clazz == otherKey.clazz && this.property.equals(otherKey.property) &&
					this.targetIsClass == otherKey.targetIsClass);
		}

		@Override
		public int hashCode() {
			return (this.clazz.hashCode() * 29 + this.property.hashCode());
		}

		@Override
		public String toString() {
			return "CacheKey [clazz=" + this.clazz.getName() + ", property=" + this.property + ", " +
					this.property + ", targetIsClass=" + this.targetIsClass + "]";
		}

		@Override
		public int compareTo(PropertyCacheKey other) {
			int result = this.clazz.getName().compareTo(other.clazz.getName());
			if (result == 0) {
				result = this.property.compareTo(other.property);
			}
			return result;
		}
	}


	/**
	 * PropertyAccessor的优化形式, 它将使用反射, 但只知道如何访问特定类的特定属性.
	 * 这与一般的ReflectivePropertyResolver不同, 后者管理可以调用​​以访问不同类的不同属性的方法/字段的缓存.
	 * 这种最佳存取器的存在, 是因为在每次读取时, 按类/名称查找适当的反射对象并不便宜.
	 */
	public static class OptimalPropertyAccessor implements CompilablePropertyAccessor {

		public final Member member;

		private final TypeDescriptor typeDescriptor;

		private final boolean needsToBeMadeAccessible;

		OptimalPropertyAccessor(InvokerPair target) {
			this.member = target.member;
			this.typeDescriptor = target.typeDescriptor;
			this.needsToBeMadeAccessible = (!Modifier.isPublic(this.member.getModifiers()) ||
					!Modifier.isPublic(this.member.getDeclaringClass().getModifiers()));
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			if (target == null) {
				return false;
			}
			Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
			if (type.isArray()) {
				return false;
			}

			if (this.member instanceof Method) {
				Method method = (Method) this.member;
				String getterName = "get" + StringUtils.capitalize(name);
				if (getterName.equals(method.getName())) {
					return true;
				}
				getterName = "is" + StringUtils.capitalize(name);
				return getterName.equals(method.getName());
			}
			else {
				Field field = (Field) this.member;
				return field.getName().equals(name);
			}
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			if (this.member instanceof Method) {
				Method method = (Method) this.member;
				try {
					if (this.needsToBeMadeAccessible && !method.isAccessible()) {
						method.setAccessible(true);
					}
					Object value = method.invoke(target);
					return new TypedValue(value, this.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter method", ex);
				}
			}
			else {
				Field field = (Field) this.member;
				try {
					if (this.needsToBeMadeAccessible && !field.isAccessible()) {
						field.setAccessible(true);
					}
					Object value = field.get(target);
					return new TypedValue(value, this.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public boolean isCompilable() {
			return (Modifier.isPublic(this.member.getModifiers()) &&
					Modifier.isPublic(this.member.getDeclaringClass().getModifiers()));
		}

		@Override
		public Class<?> getPropertyType() {
			if (this.member instanceof Method) {
				return ((Method) this.member).getReturnType();
			}
			else {
				return ((Field) this.member).getType();
			}
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
			boolean isStatic = Modifier.isStatic(this.member.getModifiers());
			String descriptor = cf.lastDescriptor();
			String classDesc = this.member.getDeclaringClass().getName().replace('.', '/');

			if (!isStatic) {
				if (descriptor == null) {
					cf.loadTarget(mv);
				}
				if (descriptor == null || !classDesc.equals(descriptor.substring(1))) {
					mv.visitTypeInsn(CHECKCAST, classDesc);
				}
			}
			else {
				if (descriptor != null) {
					// 静态字段/方法调用不会消耗堆栈上的内容, 需要弹出它.
					mv.visitInsn(POP);
				}
			}

			if (this.member instanceof Method) {
				mv.visitMethodInsn((isStatic ? INVOKESTATIC : INVOKEVIRTUAL), classDesc, this.member.getName(),
						CodeFlow.createSignatureDescriptor((Method) this.member), false);
			}
			else {
				mv.visitFieldInsn((isStatic ? GETSTATIC : GETFIELD), classDesc, this.member.getName(),
						CodeFlow.toJvmDescriptor(((Field) this.member).getType()));
			}
		}
	}
}
