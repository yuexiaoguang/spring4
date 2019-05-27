package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;

/**
 * 功能强大且可高度配置的{@link EvaluationContext}实现.
 * 此上下文使用所有适用策略的标准实现, 基于反射来解析属性, 方法和字段.
 *
 * <p>对于用于数据绑定目的的更简单的构建器样式上下文变体, 考虑使用{@link SimpleEvaluationContext},
 * 它允许根据特定评估方案的需要选择多个SpEL功能.
 */
public class StandardEvaluationContext implements EvaluationContext {

	private TypedValue rootObject;

	private List<ConstructorResolver> constructorResolvers;

	private List<MethodResolver> methodResolvers;

	private BeanResolver beanResolver;

	private ReflectiveMethodResolver reflectiveMethodResolver;

	private List<PropertyAccessor> propertyAccessors;

	private TypeLocator typeLocator;

	private TypeConverter typeConverter;

	private TypeComparator typeComparator = new StandardTypeComparator();

	private OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

	private final Map<String, Object> variables = new HashMap<String, Object>();


	/**
	 * 使用 null根对象.
	 */
	public StandardEvaluationContext() {
		setRootObject(null);
	}

	/**
	 * @param rootObject 要使用的根对象
	 */
	public StandardEvaluationContext(Object rootObject) {
		setRootObject(rootObject);
	}


	public void setRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
		this.rootObject = new TypedValue(rootObject, typeDescriptor);
	}

	public void setRootObject(Object rootObject) {
		this.rootObject = (rootObject != null ? new TypedValue(rootObject) : TypedValue.NULL);
	}

	@Override
	public TypedValue getRootObject() {
		return this.rootObject;
	}

	public void setPropertyAccessors(List<PropertyAccessor> propertyAccessors) {
		this.propertyAccessors = propertyAccessors;
	}

	@Override
	public List<PropertyAccessor> getPropertyAccessors() {
		ensurePropertyAccessorsInitialized();
		return this.propertyAccessors;
	}

	public void addPropertyAccessor(PropertyAccessor accessor) {
		ensurePropertyAccessorsInitialized();
		this.propertyAccessors.add(this.propertyAccessors.size() - 1, accessor);
	}

	public boolean removePropertyAccessor(PropertyAccessor accessor) {
		return this.propertyAccessors.remove(accessor);
	}

	public void setConstructorResolvers(List<ConstructorResolver> constructorResolvers) {
		this.constructorResolvers = constructorResolvers;
	}

	@Override
	public List<ConstructorResolver> getConstructorResolvers() {
		ensureConstructorResolversInitialized();
		return this.constructorResolvers;
	}

	public void addConstructorResolver(ConstructorResolver resolver) {
		ensureConstructorResolversInitialized();
		this.constructorResolvers.add(this.constructorResolvers.size() - 1, resolver);
	}

	public boolean removeConstructorResolver(ConstructorResolver resolver) {
		ensureConstructorResolversInitialized();
		return this.constructorResolvers.remove(resolver);
	}

	public void setMethodResolvers(List<MethodResolver> methodResolvers) {
		this.methodResolvers = methodResolvers;
	}

	@Override
	public List<MethodResolver> getMethodResolvers() {
		ensureMethodResolversInitialized();
		return this.methodResolvers;
	}

	public void addMethodResolver(MethodResolver resolver) {
		ensureMethodResolversInitialized();
		this.methodResolvers.add(this.methodResolvers.size() - 1, resolver);
	}

	public boolean removeMethodResolver(MethodResolver methodResolver) {
		ensureMethodResolversInitialized();
		return this.methodResolvers.remove(methodResolver);
	}

	public void setBeanResolver(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	@Override
	public BeanResolver getBeanResolver() {
		return this.beanResolver;
	}

	public void setTypeLocator(TypeLocator typeLocator) {
		Assert.notNull(typeLocator, "TypeLocator must not be null");
		this.typeLocator = typeLocator;
	}

	@Override
	public TypeLocator getTypeLocator() {
		if (this.typeLocator == null) {
			this.typeLocator = new StandardTypeLocator();
		}
		return this.typeLocator;
	}

	public void setTypeConverter(TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "TypeConverter must not be null");
		this.typeConverter = typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		if (this.typeConverter == null) {
			 this.typeConverter = new StandardTypeConverter();
		}
		return this.typeConverter;
	}

	public void setTypeComparator(TypeComparator typeComparator) {
		Assert.notNull(typeComparator, "TypeComparator must not be null");
		this.typeComparator = typeComparator;
	}

	@Override
	public TypeComparator getTypeComparator() {
		return this.typeComparator;
	}

	public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
		Assert.notNull(operatorOverloader, "OperatorOverloader must not be null");
		this.operatorOverloader = operatorOverloader;
	}

	@Override
	public OperatorOverloader getOperatorOverloader() {
		return this.operatorOverloader;
	}

	@Override
	public void setVariable(String name, Object value) {
		this.variables.put(name, value);
	}

	public void setVariables(Map<String,Object> variables) {
		this.variables.putAll(variables);
	}

	public void registerFunction(String name, Method method) {
		this.variables.put(name, method);
	}

	@Override
	public Object lookupVariable(String name) {
		return this.variables.get(name);
	}

	/**
	 * 注册{@code MethodFilter}, 它将在方法解析期间为指定类型调用.
	 * <p>{@code MethodFilter}可以删除方法和/或对方法进行排序, 然后SpEL将使用这些方法作为候选者来查看匹配项.
	 * 
	 * @param type 应该调用过滤器的类型
	 * @param filter {@code MethodFilter}, 或{@code null}取消注册该类型的过滤器
	 * 
	 * @throws IllegalStateException 如果{@link ReflectiveMethodResolver}未使用
	 */
	public void registerMethodFilter(Class<?> type, MethodFilter filter) throws IllegalStateException {
		ensureMethodResolversInitialized();
		if (this.reflectiveMethodResolver != null) {
			this.reflectiveMethodResolver.registerMethodFilter(type, filter);
		}
		else {
			throw new IllegalStateException("Method filter cannot be set as the reflective method resolver is not in use");
		}
	}


	private void ensurePropertyAccessorsInitialized() {
		if (this.propertyAccessors == null) {
			initializePropertyAccessors();
		}
	}

	private synchronized void initializePropertyAccessors() {
		if (this.propertyAccessors == null) {
			List<PropertyAccessor> defaultAccessors = new ArrayList<PropertyAccessor>();
			defaultAccessors.add(new ReflectivePropertyAccessor());
			this.propertyAccessors = defaultAccessors;
		}
	}

	private void ensureConstructorResolversInitialized() {
		if (this.constructorResolvers == null) {
			initializeConstructorResolvers();
		}
	}

	private synchronized void initializeConstructorResolvers() {
		if (this.constructorResolvers == null) {
			List<ConstructorResolver> defaultResolvers = new ArrayList<ConstructorResolver>();
			defaultResolvers.add(new ReflectiveConstructorResolver());
			this.constructorResolvers = defaultResolvers;
		}
	}

	private void ensureMethodResolversInitialized() {
		if (this.methodResolvers == null) {
			initializeMethodResolvers();
		}
	}

	private synchronized void initializeMethodResolvers() {
		if (this.methodResolvers == null) {
			List<MethodResolver> defaultResolvers = new ArrayList<MethodResolver>();
			this.reflectiveMethodResolver = new ReflectiveMethodResolver();
			defaultResolvers.add(this.reflectiveMethodResolver);
			this.methodResolvers = defaultResolvers;
		}
	}

}
