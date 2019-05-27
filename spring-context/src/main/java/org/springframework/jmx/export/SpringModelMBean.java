package org.springframework.jmx.export;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

/**
 * {@link RequiredModelMBean}类的扩展, 确保在发生任何调用之前,
 * 为托管资源的{@link ClassLoader}切换{@link Thread#getContextClassLoader() 线程上下文ClassLoader}.
 */
public class SpringModelMBean extends RequiredModelMBean {

	/**
	 * 存储用于调用的{@link ClassLoader}.
	 * 默认为当前线程 {@link ClassLoader}.
	 */
	private ClassLoader managedResourceClassLoader = Thread.currentThread().getContextClassLoader();


	/**
	 * 使用空{@link ModelMBeanInfo}.
	 */
	public SpringModelMBean() throws MBeanException, RuntimeOperationsException {
		super();
	}

	/**
	 * 使用给定的{@link ModelMBeanInfo}.
	 */
	public SpringModelMBean(ModelMBeanInfo mbi) throws MBeanException, RuntimeOperationsException {
		super(mbi);
	}


	/**
	 * 设置托管资源以公开和存储其{@link ClassLoader}.
	 */
	@Override
	public void setManagedResource(Object managedResource, String managedResourceType)
			throws MBeanException, InstanceNotFoundException, InvalidTargetObjectTypeException {

		this.managedResourceClassLoader = managedResource.getClass().getClassLoader();
		super.setManagedResource(managedResource, managedResourceType);
	}


	/**
	 * 在允许调用发生之前, 为托管资源{@link ClassLoader}切换{@link Thread#getContextClassLoader() 上下文ClassLoader}.
	 */
	@Override
	public Object invoke(String opName, Object[] opArgs, String[] sig)
			throws MBeanException, ReflectionException {

		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			return super.invoke(opName, opArgs, sig);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	/**
	 * 在允许调用发生之前, 为托管资源{@link ClassLoader}切换{@link Thread#getContextClassLoader() 上下文ClassLoader}.
	 */
	@Override
	public Object getAttribute(String attrName)
			throws AttributeNotFoundException, MBeanException, ReflectionException {

		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			return super.getAttribute(attrName);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	/**
	 * 在允许调用发生之前, 为托管资源{@link ClassLoader}切换{@link Thread#getContextClassLoader() 上下文ClassLoader}.
	 */
	@Override
	public AttributeList getAttributes(String[] attrNames) {
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			return super.getAttributes(attrNames);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	/**
	 * 在允许调用发生之前, 为托管资源{@link ClassLoader}切换{@link Thread#getContextClassLoader() 上下文ClassLoader}.
	 */
	@Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {

		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			super.setAttribute(attribute);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	/**
	 * 在允许调用发生之前, 为托管资源{@link ClassLoader}切换{@link Thread#getContextClassLoader() 上下文ClassLoader}.
	 */
	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			return super.setAttributes(attributes);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

}
