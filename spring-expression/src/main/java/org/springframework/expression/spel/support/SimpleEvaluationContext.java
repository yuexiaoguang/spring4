package org.springframework.expression.spel.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * {@link EvaluationContext}的基本实现, 侧重于基本SpEL功能和自定义选项的子集, 针对简单的条件评估, 特别是数据绑定场景.
 *
 * <p>在许多情况下, SpEL语言的完整范围不是必需的, 应该有意义地加以限制.
 * 示例包括但不限于数据绑定表达式, 基于属性的过滤器等.
 * 为此, {@code SimpleEvaluationContext}被定制为仅支持SpEL语言语法的子集, e.g. 不包括对Java类型, 构造函数, 和bean引用的引用.
 *
 * <p>创建{@code SimpleEvaluationContext}时, 需要选择SpEL表达式中属性访问所需的支持级别:
 * <ul>
 * <li>自定义{@code PropertyAccessor} (通常不基于反射), 可能与{@link DataBindingPropertyAccessor}结合使用</li>
 * <li>只读访问的数据绑定属性</li>
 * <li>读写的数据绑定属性</li>
 * </ul>
 *
 * <p>方便地, {@link SimpleEvaluationContext#forReadOnlyDataBinding()}通过{@link DataBindingPropertyAccessor}启用对属性的读访问;
 * {@link SimpleEvaluationContext#forReadWriteDataBinding()}需要写访问权限时同样如此.
 * 或者, 通过{@link SimpleEvaluationContext#forPropertyAccessors}配置自定义访问器, 并可能通过构建器激活方法解析和/或类型转换器.
 *
 * <p>请注意, {@code SimpleEvaluationContext}通常未配置默认根对象.
 * 相反, 它意味着创建一次, 并通过{@code getValue}调用
 * 在预编译的{@link org.springframework.expression.Expression}上重复使用{@code EvaluationContext}和根对象作为参数:
 * {@link org.springframework.expression.Expression#getValue(EvaluationContext, Object)}.
 *
 * <p>为了获得更多功能和灵活性, 特别是对于内部配置场景, 请考虑使用{@link StandardEvaluationContext}.
 */
public class SimpleEvaluationContext implements EvaluationContext {

	private static final TypeLocator typeNotFoundTypeLocator = new TypeLocator() {
		@Override
		public Class<?> findType(String typeName) throws EvaluationException {
			throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
		}
	};


	private final TypedValue rootObject;

	private final List<PropertyAccessor> propertyAccessors;

	private final List<MethodResolver> methodResolvers;

	private final TypeConverter typeConverter;

	private final TypeComparator typeComparator = new StandardTypeComparator();

	private final OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

	private final Map<String, Object> variables = new HashMap<String, Object>();


	private SimpleEvaluationContext(List<PropertyAccessor> accessors, List<MethodResolver> resolvers,
			TypeConverter converter, TypedValue rootObject) {

		this.propertyAccessors = accessors;
		this.methodResolvers = resolvers;
		this.typeConverter = (converter != null ? converter : new StandardTypeConverter());
		this.rootObject = (rootObject != null ? rootObject : TypedValue.NULL);
	}


	/**
	 * 返回指定的根对象.
	 */
	@Override
	public TypedValue getRootObject() {
		return this.rootObject;
	}

	/**
	 * 返回指定的{@link PropertyAccessor}代理.
	 */
	@Override
	public List<PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	/**
	 * 始终返回一个空列表, 因为此上下文不支持使用类型引用.
	 */
	@Override
	public List<ConstructorResolver> getConstructorResolvers() {
		return Collections.emptyList();
	}

	/**
	 * 返回指定的{@link MethodResolver}委托.
	 */
	@Override
	public List<MethodResolver> getMethodResolvers() {
		return this.methodResolvers;
	}

	/**
	 * {@code SimpleEvaluationContext}不支持使用bean引用.
	 * 
	 * @return 始终返回{@code null}
	 */
	@Override
	public BeanResolver getBeanResolver() {
		return null;
	}

	/**
	 * {@code SimpleEvaluationContext}不支持使用类型引用.
	 * 
	 * @return 使用{@link SpelMessage#TYPE_NOT_FOUND}引发{@link SpelEvaluationException}的{@code TypeLocator}实现.
	 */
	@Override
	public TypeLocator getTypeLocator() {
		return typeNotFoundTypeLocator;
	}

	/**
	 * 配置的{@link TypeConverter}.
	 * <p>默认是{@link StandardTypeConverter}.
	 */
	@Override
	public TypeConverter getTypeConverter() {
		return this.typeConverter;
	}

	/**
	 * 返回{@link StandardTypeComparator}的实例.
	 */
	@Override
	public TypeComparator getTypeComparator() {
		return this.typeComparator;
	}

	/**
	 * 返回{@link StandardOperatorOverloader}的实例.
	 */
	@Override
	public OperatorOverloader getOperatorOverloader() {
		return this.operatorOverloader;
	}

	@Override
	public void setVariable(String name, Object value) {
		this.variables.put(name, value);
	}

	@Override
	public Object lookupVariable(String name) {
		return this.variables.get(name);
	}


	/**
	 * 为指定的{@link PropertyAccessor}代理创建{@code SimpleEvaluationContext}:
	 * 通常是特定于用例的自定义{@code PropertyAccessor} (e.g. 自定义数据结构中的属性解析),
	 * 如果需要属性解引用, 则可能与{@link DataBindingPropertyAccessor}结合使用.
	 * 
	 * @param accessors 要使用的访问器代理
	 */
	public static Builder forPropertyAccessors(PropertyAccessor... accessors) {
		for (PropertyAccessor accessor : accessors) {
			if (accessor.getClass() == ReflectivePropertyAccessor.class) {
				throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
						"ReflectivePropertyAccessor. Consider using DataBindingPropertyAccessor or a custom subclass.");
			}
		}
		return new Builder(accessors);
	}

	/**
	 * 通过{@link DataBindingPropertyAccessor}创建一个{@code SimpleEvaluationContext}, 以便对public属性进行只读访问.
	 */
	public static Builder forReadOnlyDataBinding() {
		return new Builder(DataBindingPropertyAccessor.forReadOnlyAccess());
	}

	/**
	 * 创建{@code SimpleEvaluationContext}, 以便通过{@link DataBindingPropertyAccessor}对公共属性进行读写访问.
	 */
	public static Builder forReadWriteDataBinding() {
		return new Builder(DataBindingPropertyAccessor.forReadWriteAccess());
	}


	/**
	 * {@code SimpleEvaluationContext}的构建器.
	 */
	public static class Builder {

		private final List<PropertyAccessor> accessors;

		private List<MethodResolver> resolvers = Collections.emptyList();

		private TypeConverter typeConverter;

		private TypedValue rootObject;

		public Builder(PropertyAccessor... accessors) {
			this.accessors = Arrays.asList(accessors);
		}

		/**
		 * 注册指定的{@link MethodResolver}委托, 以获取属性访问和方法解析的组合.
		 * 
		 * @param resolvers 要使用的解析器委托
		 */
		public Builder withMethodResolvers(MethodResolver... resolvers) {
			for (MethodResolver resolver : resolvers) {
				if (resolver.getClass() == ReflectiveMethodResolver.class) {
					throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
							"ReflectiveMethodResolver. Consider using DataBindingMethodResolver or a custom subclass.");
				}
			}
			this.resolvers = Arrays.asList(resolvers);
			return this;
		}

		/**
		 * 除了指定的属性访问器之外, 还注册{@link DataBindingMethodResolver}以进行实例方法调用 (i.e. 不支持静态方法),
		 * 通常与{@link DataBindingPropertyAccessor}结合使用.
		 */
		public Builder withInstanceMethods() {
			this.resolvers = Collections.singletonList(
					(MethodResolver) DataBindingMethodResolver.forInstanceMethodInvocation());
			return this;
		}


		/**
		 * 注册自定义{@link ConversionService}.
		 * <p>默认情况下, 使用{@link org.springframework.core.convert.support.DefaultConversionService}支持的{@link StandardTypeConverter}.
		 */
		public Builder withConversionService(ConversionService conversionService) {
			this.typeConverter = new StandardTypeConverter(conversionService);
			return this;
		}
		/**
		 * 注册自定义{@link TypeConverter}.
		 * <p>默认情况下, 使用{@link org.springframework.core.convert.support.DefaultConversionService}支持的{@link StandardTypeConverter}.
		 */
		public Builder withTypeConverter(TypeConverter converter) {
			this.typeConverter = converter;
			return this;
		}

		/**
		 * 指定要解析的默认根对象.
		 * <p>默认值为none, 在评估时期望对象参数.
		 */
		public Builder withRootObject(Object rootObject) {
			this.rootObject = new TypedValue(rootObject);
			return this;
		}

		/**
		 * 指定要解析的类型化根对象.
		 * <p>默认值为none, 在评估时期望对象参数.
		 */
		public Builder withTypedRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
			this.rootObject = new TypedValue(rootObject, typeDescriptor);
			return this;
		}

		public SimpleEvaluationContext build() {
			return new SimpleEvaluationContext(this.accessors, this.resolvers, this.typeConverter, this.rootObject);
		}
	}
}
