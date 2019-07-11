package org.springframework.transaction.interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Properties;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * AOP Alliance MethodInterceptor, 用于使用通用Spring事务基础结构进行声明式事务管理
 * ({@link org.springframework.transaction.PlatformTransactionManager}).
 *
 * <p>派生自{@link TransactionAspectSupport}类, 其中包含与Spring的底层事务API的集成.
 * TransactionInterceptor只是以正确的顺序调用相关的超类方法，如{@link #invokeWithinTransaction}.
 *
 * <p>TransactionInterceptors是线程安全的.
 */
@SuppressWarnings("serial")
public class TransactionInterceptor extends TransactionAspectSupport implements MethodInterceptor, Serializable {

	/**
	 * <p>仍然需要设置事务管理器和事务属性.
	 */
	public TransactionInterceptor() {
	}

	/**
	 * @param ptm 默认事务管理器, 用于执行实际的事务管理
	 * @param attributes 属性格式的事务属性
	 */
	public TransactionInterceptor(PlatformTransactionManager ptm, Properties attributes) {
		setTransactionManager(ptm);
		setTransactionAttributes(attributes);
	}

	/**
	 * @param ptm 默认事务管理器, 用于执行实际的事务管理
	 * @param tas 用于查找事务属性的属性源
	 */
	public TransactionInterceptor(PlatformTransactionManager ptm, TransactionAttributeSource tas) {
		setTransactionManager(ptm);
		setTransactionAttributeSource(tas);
	}


	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		// 计算出目标类: may be {@code null}.
		// TransactionAttributeSource应该传递目标类以及可能来自接口的方法.
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

		// 适配TransactionAspectSupport的invokeWithinTransaction...
		return invokeWithinTransaction(invocation.getMethod(), targetClass, new InvocationCallback() {
			@Override
			public Object proceedWithInvocation() throws Throwable {
				return invocation.proceed();
			}
		});
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void writeObject(ObjectOutputStream oos) throws IOException {
		// 依赖于默认序列化, 尽管这个类本身无论如何都不带状态...
		oos.defaultWriteObject();

		// 反序列化超类字段.
		oos.writeObject(getTransactionManagerBeanName());
		oos.writeObject(getTransactionManager());
		oos.writeObject(getTransactionAttributeSource());
		oos.writeObject(getBeanFactory());
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖于默认序列化, 尽管这个类本身无论如何都不带状态...
		ois.defaultReadObject();

		// 序列化所有相关的超类字段.
		// 超类不能实现Serializable, 因为它也可以作为AspectJ切面的基类 (不允许实现Serializable)!
		setTransactionManagerBeanName((String) ois.readObject());
		setTransactionManager((PlatformTransactionManager) ois.readObject());
		setTransactionAttributeSource((TransactionAttributeSource) ois.readObject());
		setBeanFactory((BeanFactory) ois.readObject());
	}

}
