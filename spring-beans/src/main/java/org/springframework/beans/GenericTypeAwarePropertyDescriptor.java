package org.springframework.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 扩展标准JavaBeans {@link PropertyDescriptor}类, 重写{@code getPropertyType()}, 以便根据包含的bean类解析一般声明的类型变量.
 */
final class GenericTypeAwarePropertyDescriptor extends PropertyDescriptor {

	private final Class<?> beanClass;

	private final Method readMethod;

	private final Method writeMethod;

	private volatile Set<Method> ambiguousWriteMethods;

	private MethodParameter writeMethodParameter;

	private Class<?> propertyType;

	private final Class<?> propertyEditorClass;


	public GenericTypeAwarePropertyDescriptor(Class<?> beanClass, String propertyName,
			Method readMethod, Method writeMethod, Class<?> propertyEditorClass)
			throws IntrospectionException {

		super(propertyName, null, null);

		if (beanClass == null)  {
			throw new IntrospectionException("Bean class must not be null");
		}
		this.beanClass = beanClass;

		Method readMethodToUse = BridgeMethodResolver.findBridgedMethod(readMethod);
		Method writeMethodToUse = BridgeMethodResolver.findBridgedMethod(writeMethod);
		if (writeMethodToUse == null && readMethodToUse != null) {
			// Fallback: 由于缺少桥接方法解析, 原始JavaBeans反射可能找不到匹配的setter方法,
			// 在getter使用协变(子类)返回类型的情况下, 而setter是为具体的属性类型定义的.
			Method candidate = ClassUtils.getMethodIfAvailable(
					this.beanClass, "set" + StringUtils.capitalize(getName()), (Class<?>[]) null);
			if (candidate != null && candidate.getParameterTypes().length == 1) {
				writeMethodToUse = candidate;
			}
		}
		this.readMethod = readMethodToUse;
		this.writeMethod = writeMethodToUse;

		if (this.writeMethod != null) {
			if (this.readMethod == null) {
				// 写入方法与读取方法不匹配: 通过几个重载变量可能含糊不清, 在这种情况下, JDK的JavaBeans Introspector选择了任意获胜者...
				Set<Method> ambiguousCandidates = new HashSet<Method>();
				for (Method method : beanClass.getMethods()) {
					if (method.getName().equals(writeMethodToUse.getName()) &&
							!method.equals(writeMethodToUse) && !method.isBridge() &&
							method.getParameterTypes().length == writeMethodToUse.getParameterTypes().length) {
						ambiguousCandidates.add(method);
					}
				}
				if (!ambiguousCandidates.isEmpty()) {
					this.ambiguousWriteMethods = ambiguousCandidates;
				}
			}
			this.writeMethodParameter = new MethodParameter(this.writeMethod, 0);
			GenericTypeResolver.resolveParameterType(this.writeMethodParameter, this.beanClass);
		}

		if (this.readMethod != null) {
			this.propertyType = GenericTypeResolver.resolveReturnType(this.readMethod, this.beanClass);
		}
		else if (this.writeMethodParameter != null) {
			this.propertyType = this.writeMethodParameter.getParameterType();
		}

		this.propertyEditorClass = propertyEditorClass;
	}


	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	@Override
	public Method getReadMethod() {
		return this.readMethod;
	}

	@Override
	public Method getWriteMethod() {
		return this.writeMethod;
	}

	public Method getWriteMethodForActualAccess() {
		Set<Method> ambiguousCandidates = this.ambiguousWriteMethods;
		if (ambiguousCandidates != null) {
			this.ambiguousWriteMethods = null;
			LogFactory.getLog(GenericTypeAwarePropertyDescriptor.class).warn("Invalid JavaBean property '" +
					getName() + "' being accessed! Ambiguous write methods found next to actually used [" +
					this.writeMethod + "]: " + ambiguousCandidates);
		}
		return this.writeMethod;
	}

	public MethodParameter getWriteMethodParameter() {
		return this.writeMethodParameter;
	}

	@Override
	public Class<?> getPropertyType() {
		return this.propertyType;
	}

	@Override
	public Class<?> getPropertyEditorClass() {
		return this.propertyEditorClass;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof GenericTypeAwarePropertyDescriptor)) {
			return false;
		}
		GenericTypeAwarePropertyDescriptor otherPd = (GenericTypeAwarePropertyDescriptor) other;
		return (getBeanClass().equals(otherPd.getBeanClass()) && PropertyDescriptorUtils.equals(this, otherPd));
	}

	@Override
	public int hashCode() {
		int hashCode = getBeanClass().hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getReadMethod());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getWriteMethod());
		return hashCode;
	}

}
