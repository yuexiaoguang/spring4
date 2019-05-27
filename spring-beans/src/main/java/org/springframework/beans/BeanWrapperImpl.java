package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

/**
 * 默认的{@link BeanWrapper}实现应该足以满足所有典型用例. 为了效率, 缓存反射结果.
 *
 * <p>Note: 自动注册{@code org.springframework.beans.propertyeditors}包中的默认属性编辑器, 除了JDK的标准PropertyEditors之外.
 * 应用程序可以调用 {@link #registerCustomEditor(Class, java.beans.PropertyEditor)}方法为特定实例注册编辑器 (即它们不在整个应用中共享).
 * 有关详细信息, 请参阅基类{@link PropertyEditorRegistrySupport}.
 *
 * <p><b>NOTE: 截至Spring 2.5, 这是 - 几乎所有目的 - 内部类.</b>
 * 它只是public, 以允许从其他框架包访问. 用于标准应用程序访问, 请改用{@link PropertyAccessorFactory＃forBeanPropertyAccess}工厂方法.
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

	/**
	 * 缓存此对象的反射结果, 防止每次遇到JavaBeans反射的消耗.
	 */
	private CachedIntrospectionResults cachedIntrospectionResults;

	/**
	 * 用于调用属性方法的安全上下文
	 */
	private AccessControlContext acc;


	/**
	 * 之后需要设置包装的实例. 注册默认的编辑器.
	 */
	public BeanWrapperImpl() {
		this(true);
	}

	/**
	 * 之后需要设置包装的实例.
	 * 
	 * @param registerDefaultEditors 是否注册默认的编辑器 (如果BeanWrapper不需要任何类型转换，则可以禁止它)
	 */
	public BeanWrapperImpl(boolean registerDefaultEditors) {
		super(registerDefaultEditors);
	}

	/**
	 * @param object 这个BeanWrapper包装的对象
	 */
	public BeanWrapperImpl(Object object) {
		super(object);
	}

	/**
	 * 包装指定类的新实例.
	 * 
	 * @param clazz 要实例化和包装的类
	 */
	public BeanWrapperImpl(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * 为给定对象创建一个新的BeanWrapperImpl, 并注册对象所在的嵌套路径.
	 * 
	 * @param object 这个BeanWrapper包装的对象
	 * @param nestedPath 对象的嵌套路径
	 * @param rootObject 路径顶部的根对象
	 */
	public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
		super(object, nestedPath, rootObject);
	}

	/**
	 * 为给定对象创建一个新的BeanWrapperImpl, 并注册对象所在的嵌套路径.
	 * 
	 * @param object 这个BeanWrapper包装的对象
	 * @param nestedPath 对象的嵌套路径
	 * @param parent 包含对象的BeanWrapper (不能是 {@code null})
	 */
	private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
		super(object, nestedPath, parent);
		setSecurityContext(parent.acc);
	}


	/**
	 * 设置要保留的bean实例, 不包括 {@link java.util.Optional}.
	 * 
	 * @param object 实际的目标对象
	 * @since 4.3
	 */
	public void setBeanInstance(Object object) {
		this.wrappedObject = object;
		this.rootObject = object;
		this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
		setIntrospectionClass(object.getClass());
	}

	@Override
	public void setWrappedInstance(Object object, String nestedPath, Object rootObject) {
		super.setWrappedInstance(object, nestedPath, rootObject);
		setIntrospectionClass(getWrappedClass());
	}

	/**
	 * 设置要反射的类. 需要在目标对象更改时调用.
	 * 
	 * @param clazz 要反射的类
	 */
	protected void setIntrospectionClass(Class<?> clazz) {
		if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
			this.cachedIntrospectionResults = null;
		}
	}

	/**
	 * 获取包装的对象的延迟初始化的CachedIntrospectionResults实例.
	 */
	private CachedIntrospectionResults getCachedIntrospectionResults() {
		Assert.state(getWrappedInstance() != null, "BeanWrapper does not hold a bean instance");
		if (this.cachedIntrospectionResults == null) {
			this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
		}
		return this.cachedIntrospectionResults;
	}

	/**
	 * 设置在调用包装实例方法期间使用的安全上下文.
	 * Can be null.
	 */
	public void setSecurityContext(AccessControlContext acc) {
		this.acc = acc;
	}

	/**
	 * 返回在调用包装实例方法期间使用的安全上下文.
	 * Can be null.
	 */
	public AccessControlContext getSecurityContext() {
		return this.acc;
	}


	/**
	 * 将指定属性的给定值转换为后者的类型.
	 * <p>此方法仅用于BeanFactory中的优化. 使用{@code convertIfNecessary}方法进行编程转换.
	 * 
	 * @param value 要转换的值
	 * @param propertyName 目标属性 (请注意, 此处不支持嵌套或索引属性)
	 * 
	 * @return 新值, 可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 */
	public Object convertForProperty(Object value, String propertyName) throws TypeMismatchException {
		CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
		PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
		if (pd == null) {
			throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
					"No property '" + propertyName + "' found");
		}
		TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
		if (td == null) {
			td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
		}
		return convertForProperty(propertyName, null, value, td);
	}

	private Property property(PropertyDescriptor pd) {
		GenericTypeAwarePropertyDescriptor gpd = (GenericTypeAwarePropertyDescriptor) pd;
		return new Property(gpd.getBeanClass(), gpd.getReadMethod(), gpd.getWriteMethod(), gpd.getName());
	}

	@Override
	protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
		PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
		return (pd != null ? new BeanPropertyHandler(pd) : null);
	}

	@Override
	protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
		return new BeanWrapperImpl(object, nestedPath, this);
	}

	@Override
	protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
		PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
		throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName,
				matches.buildErrorMessage(), matches.getPossibleMatches());
	}

	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		return getCachedIntrospectionResults().getPropertyDescriptors();
	}

	@Override
	public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
		BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
		String finalPath = getFinalPath(nestedBw, propertyName);
		PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
		if (pd == null) {
			throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
					"No property '" + propertyName + "' found");
		}
		return pd;
	}


	private class BeanPropertyHandler extends PropertyHandler {

		private final PropertyDescriptor pd;

		public BeanPropertyHandler(PropertyDescriptor pd) {
			super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
			this.pd = pd;
		}

		@Override
		public ResolvableType getResolvableType() {
			return ResolvableType.forMethodReturnType(this.pd.getReadMethod());
		}

		@Override
		public TypeDescriptor toTypeDescriptor() {
			return new TypeDescriptor(property(this.pd));
		}

		@Override
		public TypeDescriptor nested(int level) {
			return TypeDescriptor.nested(property(pd), level);
		}

		@Override
		public Object getValue() throws Exception {
			final Method readMethod = this.pd.getReadMethod();
			if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers()) && !readMethod.isAccessible()) {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						@Override
						public Object run() {
							readMethod.setAccessible(true);
							return null;
						}
					});
				}
				else {
					readMethod.setAccessible(true);
				}
			}
			if (System.getSecurityManager() != null) {
				try {
					return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
							return readMethod.invoke(getWrappedInstance(), (Object[]) null);
						}
					}, acc);
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
				return readMethod.invoke(getWrappedInstance(), (Object[]) null);
			}
		}

		@Override
		public void setValue(final Object object, Object valueToApply) throws Exception {
			final Method writeMethod = (this.pd instanceof GenericTypeAwarePropertyDescriptor ?
					((GenericTypeAwarePropertyDescriptor) this.pd).getWriteMethodForActualAccess() :
					this.pd.getWriteMethod());
			if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers()) && !writeMethod.isAccessible()) {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						@Override
						public Object run() {
							writeMethod.setAccessible(true);
							return null;
						}
					});
				}
				else {
					writeMethod.setAccessible(true);
				}
			}
			final Object value = valueToApply;
			if (System.getSecurityManager() != null) {
				try {
					AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
							writeMethod.invoke(object, value);
							return null;
						}
					}, acc);
				}
				catch (PrivilegedActionException ex) {
					throw ex.getException();
				}
			}
			else {
				writeMethod.invoke(getWrappedInstance(), value);
			}
		}
	}
}
