package org.springframework.beans;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ObjectUtils;

/**
 * 装饰器用于标准{@link BeanInfo}对象, e.g. {@link Introspector#getBeanInfo(Class)}创建的对象,
 * 旨在发现和注册静态和/或非空返回的setter方法. 例如:
 * <pre class="code">
 * public class Bean {
 *     private Foo foo;
 *
 *     public Foo getFoo() {
 *         return this.foo;
 *     }
 *
 *     public Bean setFoo(Foo foo) {
 *         this.foo = foo;
 *         return this;
 *     }
 * }</pre>
 * 标准JavaBeans {@code Introspector}将发现{@code getFoo}读取方法, 但会绕过 {@code #setFoo(Foo)}写入方法,
 * 因为它的非void返回签名不符合JavaBeans规范.
 * {@code ExtendedBeanInfo}, 另一方面, 将识别并包括它. 为了允许在Spring {@code <beans>} XML中使用具有“构建器”或方法链样式的setter签名的API.
 * {@link #getPropertyDescriptors()}返回包装的{@code BeanInfo}中的所有现有属性描述符, 以及为非void返回的setter添加的属性描述符.
 * Both standard ("non-indexed") and <a href="http://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html"> indexed properties</a> are fully supported.
 */
class ExtendedBeanInfo implements BeanInfo {

	private static final Log logger = LogFactory.getLog(ExtendedBeanInfo.class);

	private final BeanInfo delegate;

	private final Set<PropertyDescriptor> propertyDescriptors =
			new TreeSet<PropertyDescriptor>(new PropertyDescriptorComparator());


	/**
	 * 包装给定的{@link BeanInfo}实例;
	 * 在本地复制其所有现有属性描述符, 将每个{@link BeanInfo}实例包装在自定义的 {@link SimpleIndexedPropertyDescriptor indexed}
	 * 或 {@link SimplePropertyDescriptor non-indexed} {@code PropertyDescriptor}变量中, 绕过默认的JDK弱/软引用管理;
	 * 然后搜索其方法描述符, 以查找任何非void返回的写入方法, 并为每个找到的更新或创建相应的{@link PropertyDescriptor}.
	 * 
	 * @param delegate 包装的{@code BeanInfo}, 永远不会被修改
	 * 
	 * @throws IntrospectionException 如果在创建和添加新属性描述符时出现问题
	 */
	public ExtendedBeanInfo(BeanInfo delegate) throws IntrospectionException {
		this.delegate = delegate;
		for (PropertyDescriptor pd : delegate.getPropertyDescriptors()) {
			try {
				this.propertyDescriptors.add(pd instanceof IndexedPropertyDescriptor ?
						new SimpleIndexedPropertyDescriptor((IndexedPropertyDescriptor) pd) :
						new SimplePropertyDescriptor(pd));
			}
			catch (IntrospectionException ex) {
				// 可能只是一种不打算遵循JavaBeans模式的方法...
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring invalid bean property '" + pd.getName() + "': " + ex.getMessage());
				}
			}
		}
		MethodDescriptor[] methodDescriptors = delegate.getMethodDescriptors();
		if (methodDescriptors != null) {
			for (Method method : findCandidateWriteMethods(methodDescriptors)) {
				try {
					handleCandidateWriteMethod(method);
				}
				catch (IntrospectionException ex) {
					// 只是想找到候选人, 这里很容易忽略额外的...
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring candidate write method [" + method + "]: " + ex.getMessage());
					}
				}
			}
		}
	}


	private List<Method> findCandidateWriteMethods(MethodDescriptor[] methodDescriptors) {
		List<Method> matches = new ArrayList<Method>();
		for (MethodDescriptor methodDescriptor : methodDescriptors) {
			Method method = methodDescriptor.getMethod();
			if (isCandidateWriteMethod(method)) {
				matches.add(method);
			}
		}
		// 对非void返回的写入方法进行排序, 以防止在JDK 7下从Class#getDeclaredMethods返回的方法的非确定性排序的不良影响.
		// See http://bugs.sun.com/view_bug.do?bug_id=7023180
		Collections.sort(matches, new Comparator<Method>() {
			@Override
			public int compare(Method m1, Method m2) {
				return m2.toString().compareTo(m1.toString());
			}
		});
		return matches;
	}

	public static boolean isCandidateWriteMethod(Method method) {
		String methodName = method.getName();
		Class<?>[] parameterTypes = method.getParameterTypes();
		int nParams = parameterTypes.length;
		return (methodName.length() > 3 && methodName.startsWith("set") && Modifier.isPublic(method.getModifiers()) &&
				(!void.class.isAssignableFrom(method.getReturnType()) || Modifier.isStatic(method.getModifiers())) &&
				(nParams == 1 || (nParams == 2 && int.class == parameterTypes[0])));
	}

	private void handleCandidateWriteMethod(Method method) throws IntrospectionException {
		int nParams = method.getParameterTypes().length;
		String propertyName = propertyNameFor(method);
		Class<?> propertyType = method.getParameterTypes()[nParams - 1];
		PropertyDescriptor existingPd = findExistingPropertyDescriptor(propertyName, propertyType);
		if (nParams == 1) {
			if (existingPd == null) {
				this.propertyDescriptors.add(new SimplePropertyDescriptor(propertyName, null, method));
			}
			else {
				existingPd.setWriteMethod(method);
			}
		}
		else if (nParams == 2) {
			if (existingPd == null) {
				this.propertyDescriptors.add(
						new SimpleIndexedPropertyDescriptor(propertyName, null, null, null, method));
			}
			else if (existingPd instanceof IndexedPropertyDescriptor) {
				((IndexedPropertyDescriptor) existingPd).setIndexedWriteMethod(method);
			}
			else {
				this.propertyDescriptors.remove(existingPd);
				this.propertyDescriptors.add(new SimpleIndexedPropertyDescriptor(
						propertyName, existingPd.getReadMethod(), existingPd.getWriteMethod(), null, method));
			}
		}
		else {
			throw new IllegalArgumentException("Write method must have exactly 1 or 2 parameters: " + method);
		}
	}

	private PropertyDescriptor findExistingPropertyDescriptor(String propertyName, Class<?> propertyType) {
		for (PropertyDescriptor pd : this.propertyDescriptors) {
			final Class<?> candidateType;
			final String candidateName = pd.getName();
			if (pd instanceof IndexedPropertyDescriptor) {
				IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
				candidateType = ipd.getIndexedPropertyType();
				if (candidateName.equals(propertyName) &&
						(candidateType.equals(propertyType) || candidateType.equals(propertyType.getComponentType()))) {
					return pd;
				}
			}
			else {
				candidateType = pd.getPropertyType();
				if (candidateName.equals(propertyName) &&
						(candidateType.equals(propertyType) || propertyType.equals(candidateType.getComponentType()))) {
					return pd;
				}
			}
		}
		return null;
	}

	private String propertyNameFor(Method method) {
		return Introspector.decapitalize(method.getName().substring(3, method.getName().length()));
	}


	/**
	 * 从包装的{@link BeanInfo}对象返回{@link PropertyDescriptor}的集合, 
	 * 以及在构造期间找到的每个非void返回的setter方法的{@code PropertyDescriptor}.
	 */
	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		return this.propertyDescriptors.toArray(new PropertyDescriptor[this.propertyDescriptors.size()]);
	}

	@Override
	public BeanInfo[] getAdditionalBeanInfo() {
		return this.delegate.getAdditionalBeanInfo();
	}

	@Override
	public BeanDescriptor getBeanDescriptor() {
		return this.delegate.getBeanDescriptor();
	}

	@Override
	public int getDefaultEventIndex() {
		return this.delegate.getDefaultEventIndex();
	}

	@Override
	public int getDefaultPropertyIndex() {
		return this.delegate.getDefaultPropertyIndex();
	}

	@Override
	public EventSetDescriptor[] getEventSetDescriptors() {
		return this.delegate.getEventSetDescriptors();
	}

	@Override
	public Image getIcon(int iconKind) {
		return this.delegate.getIcon(iconKind);
	}

	@Override
	public MethodDescriptor[] getMethodDescriptors() {
		return this.delegate.getMethodDescriptors();
	}


	static class SimplePropertyDescriptor extends PropertyDescriptor {

		private Method readMethod;

		private Method writeMethod;

		private Class<?> propertyType;

		private Class<?> propertyEditorClass;

		public SimplePropertyDescriptor(PropertyDescriptor original) throws IntrospectionException {
			this(original.getName(), original.getReadMethod(), original.getWriteMethod());
			PropertyDescriptorUtils.copyNonMethodProperties(original, this);
		}

		public SimplePropertyDescriptor(String propertyName, Method readMethod, Method writeMethod) throws IntrospectionException {
			super(propertyName, null, null);
			this.readMethod = readMethod;
			this.writeMethod = writeMethod;
			this.propertyType = PropertyDescriptorUtils.findPropertyType(readMethod, writeMethod);
		}

		@Override
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setReadMethod(Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		public Method getWriteMethod() {
			return this.writeMethod;
		}

		@Override
		public void setWriteMethod(Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		@Override
		public Class<?> getPropertyType() {
			if (this.propertyType == null) {
				try {
					this.propertyType = PropertyDescriptorUtils.findPropertyType(this.readMethod, this.writeMethod);
				}
				catch (IntrospectionException ex) {
					// Ignore, as does PropertyDescriptor#getPropertyType
				}
			}
			return this.propertyType;
		}

		@Override
		public Class<?> getPropertyEditorClass() {
			return this.propertyEditorClass;
		}

		@Override
		public void setPropertyEditorClass(Class<?> propertyEditorClass) {
			this.propertyEditorClass = propertyEditorClass;
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof PropertyDescriptor &&
					PropertyDescriptorUtils.equals(this, (PropertyDescriptor) other)));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(getReadMethod()) * 29 + ObjectUtils.nullSafeHashCode(getWriteMethod()));
		}

		@Override
		public String toString() {
			return String.format("%s[name=%s, propertyType=%s, readMethod=%s, writeMethod=%s]",
					getClass().getSimpleName(), getName(), getPropertyType(), this.readMethod, this.writeMethod);
		}
	}


	static class SimpleIndexedPropertyDescriptor extends IndexedPropertyDescriptor {

		private Method readMethod;

		private Method writeMethod;

		private Class<?> propertyType;

		private Method indexedReadMethod;

		private Method indexedWriteMethod;

		private Class<?> indexedPropertyType;

		private Class<?> propertyEditorClass;

		public SimpleIndexedPropertyDescriptor(IndexedPropertyDescriptor original) throws IntrospectionException {
			this(original.getName(), original.getReadMethod(), original.getWriteMethod(),
					original.getIndexedReadMethod(), original.getIndexedWriteMethod());
			PropertyDescriptorUtils.copyNonMethodProperties(original, this);
		}

		public SimpleIndexedPropertyDescriptor(String propertyName, Method readMethod, Method writeMethod,
				Method indexedReadMethod, Method indexedWriteMethod) throws IntrospectionException {

			super(propertyName, null, null, null, null);
			this.readMethod = readMethod;
			this.writeMethod = writeMethod;
			this.propertyType = PropertyDescriptorUtils.findPropertyType(readMethod, writeMethod);
			this.indexedReadMethod = indexedReadMethod;
			this.indexedWriteMethod = indexedWriteMethod;
			this.indexedPropertyType = PropertyDescriptorUtils.findIndexedPropertyType(
					propertyName, this.propertyType, indexedReadMethod, indexedWriteMethod);
		}

		@Override
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setReadMethod(Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		public Method getWriteMethod() {
			return this.writeMethod;
		}

		@Override
		public void setWriteMethod(Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		@Override
		public Class<?> getPropertyType() {
			if (this.propertyType == null) {
				try {
					this.propertyType = PropertyDescriptorUtils.findPropertyType(this.readMethod, this.writeMethod);
				}
				catch (IntrospectionException ex) {
					// Ignore, as does IndexedPropertyDescriptor#getPropertyType
				}
			}
			return this.propertyType;
		}

		@Override
		public Method getIndexedReadMethod() {
			return this.indexedReadMethod;
		}

		@Override
		public void setIndexedReadMethod(Method indexedReadMethod) throws IntrospectionException {
			this.indexedReadMethod = indexedReadMethod;
		}

		@Override
		public Method getIndexedWriteMethod() {
			return this.indexedWriteMethod;
		}

		@Override
		public void setIndexedWriteMethod(Method indexedWriteMethod) throws IntrospectionException {
			this.indexedWriteMethod = indexedWriteMethod;
		}

		@Override
		public Class<?> getIndexedPropertyType() {
			if (this.indexedPropertyType == null) {
				try {
					this.indexedPropertyType = PropertyDescriptorUtils.findIndexedPropertyType(
							getName(), getPropertyType(), this.indexedReadMethod, this.indexedWriteMethod);
				}
				catch (IntrospectionException ex) {
					// Ignore, as does IndexedPropertyDescriptor#getIndexedPropertyType
				}
			}
			return this.indexedPropertyType;
		}

		@Override
		public Class<?> getPropertyEditorClass() {
			return this.propertyEditorClass;
		}

		@Override
		public void setPropertyEditorClass(Class<?> propertyEditorClass) {
			this.propertyEditorClass = propertyEditorClass;
		}

		/*
		 * See java.beans.IndexedPropertyDescriptor#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof IndexedPropertyDescriptor)) {
				return false;
			}
			IndexedPropertyDescriptor otherPd = (IndexedPropertyDescriptor) other;
			return (ObjectUtils.nullSafeEquals(getIndexedReadMethod(), otherPd.getIndexedReadMethod()) &&
					ObjectUtils.nullSafeEquals(getIndexedWriteMethod(), otherPd.getIndexedWriteMethod()) &&
					ObjectUtils.nullSafeEquals(getIndexedPropertyType(), otherPd.getIndexedPropertyType()) &&
					PropertyDescriptorUtils.equals(this, otherPd));
		}

		@Override
		public int hashCode() {
			int hashCode = ObjectUtils.nullSafeHashCode(getReadMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getWriteMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getIndexedReadMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getIndexedWriteMethod());
			return hashCode;
		}

		@Override
		public String toString() {
			return String.format("%s[name=%s, propertyType=%s, indexedPropertyType=%s, " +
							"readMethod=%s, writeMethod=%s, indexedReadMethod=%s, indexedWriteMethod=%s]",
					getClass().getSimpleName(), getName(), getPropertyType(), getIndexedPropertyType(),
					this.readMethod, this.writeMethod, this.indexedReadMethod, this.indexedWriteMethod);
		}
	}


	/**
	 * 以字母数字方式对PropertyDescriptor实例进行排序, 以模拟{@link java.beans.BeanInfo#getPropertyDescriptors()}的行为.
	 */
	static class PropertyDescriptorComparator implements Comparator<PropertyDescriptor> {

		@Override
		public int compare(PropertyDescriptor desc1, PropertyDescriptor desc2) {
			String left = desc1.getName();
			String right = desc2.getName();
			for (int i = 0; i < left.length(); i++) {
				if (right.length() == i) {
					return 1;
				}
				int result = left.getBytes()[i] - right.getBytes()[i];
				if (result != 0) {
					return result;
				}
			}
			return left.length() - right.length();
		}
	}
}
