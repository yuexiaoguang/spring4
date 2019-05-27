package org.springframework.beans;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ReflectionUtils;

/**
 * 直接访问实例字段的{@link ConfigurablePropertyAccessor}实现.
 * 允许直接绑定到字段而不是通过JavaBean setter.
 *
 * <p>截至Spring 4.2, 绝大多数{@link BeanWrapper}功能已合并到{@link AbstractPropertyAccessor},
 * 意味着现在也支持属性遍历以及集合和Map访问.
 *
 * <p>DirectFieldAccessor的“extractOldValueForEditor”设置的默认值为“true”, 因为可以始终读取字段而没有副作用.
 */
public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {

	private final Map<String, FieldPropertyHandler> fieldMap = new HashMap<String, FieldPropertyHandler>();


	/**
	 * @param object 由此DirectFieldAccessor包装的对象
	 */
	public DirectFieldAccessor(Object object) {
		super(object);
	}

	/**
	 * @param object 由此DirectFieldAccessor包装的对象
	 * @param nestedPath 对象的嵌套路径
	 * @param parent 包含对象的DirectFieldAccessor (must not be {@code null})
	 */
	protected DirectFieldAccessor(Object object, String nestedPath, DirectFieldAccessor parent) {
		super(object, nestedPath, parent);
	}


	@Override
	protected FieldPropertyHandler getLocalPropertyHandler(String propertyName) {
		FieldPropertyHandler propertyHandler = this.fieldMap.get(propertyName);
		if (propertyHandler == null) {
			Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
			if (field != null) {
				propertyHandler = new FieldPropertyHandler(field);
				this.fieldMap.put(propertyName, propertyHandler);
			}
		}
		return propertyHandler;
	}

	@Override
	protected DirectFieldAccessor newNestedPropertyAccessor(Object object, String nestedPath) {
		return new DirectFieldAccessor(object, nestedPath, this);
	}

	@Override
	protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
		PropertyMatches matches = PropertyMatches.forField(propertyName, getRootClass());
		throw new NotWritablePropertyException(
				getRootClass(), getNestedPath() + propertyName,
				matches.buildErrorMessage(), matches.getPossibleMatches());
	}


	private class FieldPropertyHandler extends PropertyHandler {

		private final Field field;

		public FieldPropertyHandler(Field field) {
			super(field.getType(), true, true);
			this.field = field;
		}

		@Override
		public TypeDescriptor toTypeDescriptor() {
			return new TypeDescriptor(this.field);
		}

		@Override
		public ResolvableType getResolvableType() {
			return ResolvableType.forField(this.field);
		}

		@Override
		public TypeDescriptor nested(int level) {
			return TypeDescriptor.nested(this.field, level);
		}

		@Override
		public Object getValue() throws Exception {
			try {
				ReflectionUtils.makeAccessible(this.field);
				return this.field.get(getWrappedInstance());
			}

			catch (IllegalAccessException ex) {
				throw new InvalidPropertyException(getWrappedClass(),
						this.field.getName(), "Field is not accessible", ex);
			}
		}

		@Override
		public void setValue(Object object, Object value) throws Exception {
			try {
				ReflectionUtils.makeAccessible(this.field);
				this.field.set(object, value);
			}
			catch (IllegalAccessException ex) {
				throw new InvalidPropertyException(getWrappedClass(), this.field.getName(),
						"Field is not accessible", ex);
			}
		}
	}

}
