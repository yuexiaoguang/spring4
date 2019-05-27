package org.springframework.beans.factory.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;

/**
 * {@link FactoryBean}返回一个值, 该值是静态或实例方法调用的结果.
 * 对于大多数用例, 最好只使用容器的内置工厂方法支持来实现相同的目的, 因为转换参数更聪明.
 * 当需要调用不返回任何值的方法时, 此工厂bean仍然有用 (例如, 一种静态类方法, 可以强制进行某种初始化).
 * 工厂方法不支持此用例, 因为获取bean实例需要返回值.
 *
 * <p>请注意, 由于预计主要用于访问工厂方法, 因此默认情况下, 此工厂以<b>单例</b>方式运行.
 * bean工厂对{@link #getObject}的第一个请求将导致方法调用, 其返回值将被缓存以用于后续请求.
 * 内部{@link #setSingleton singleton}属性可能设置为 "false", 以使此工厂在每次请求对象时调用目标方法.
 *
 * <p><b>NOTE: 如果目标方法没有产生公开的结果, 请考虑使用{@link MethodInvokingBean},
 * 这避免了此{@link MethodInvokingFactoryBean}附带的类型确定和生命周期限制.</b>
 *
 * <p>这个调用者支持任何类型的目标方法.
 * 可以通过将{@link #setTargetMethod targetMethod}属性设置为表示静态方法名称的String来指定静态方法,
 * 使用{@link #setTargetClass targetClass}指定定义静态方法的Class.
 * 或者, 可以通过将{@link #setTargetObject targetObject}属性设置为目标对象,
 * 并将{@link #setTargetMethod targetMethod}属性指定为要在该目标对象上调用的方法的名称来指定目标实例方法.
 * 可以通过设置{@link #setArguments arguments}属性来指定方法调用的参数.
 *
 * <p>根据InitializingBean契约, 一旦设置了所有属性, 此类依赖于{@link #afterPropertiesSet()}被调用.
 *
 * <p>bean定义的一个示例(在基于XML的bean工厂定义中), 它使用此类来调用静态工厂方法:
 *
 * <pre class="code">
 * &lt;bean id="myObject" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
 *   &lt;property name="staticMethod" value="com.whatever.MyClassFactory.getInstance"/>
 * &lt;/bean></pre>
 *
 * <p>调用静态方法, 然后使用实例方法获取Java系统属性的示例. 有点冗长, 但它可行.
 *
 * <pre class="code">
 * &lt;bean id="sysProps" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
 *   &lt;property name="targetClass" value="java.lang.System"/>
 *   &lt;property name="targetMethod" value="getProperties"/>
 * &lt;/bean>
 *
 * &lt;bean id="javaVersion" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
 *   &lt;property name="targetObject" ref="sysProps"/>
 *   &lt;property name="targetMethod" value="getProperty"/>
 *   &lt;property name="arguments" value="java.version"/>
 * &lt;/bean></pre>
 */
public class MethodInvokingFactoryBean extends MethodInvokingBean implements FactoryBean<Object> {

	private boolean singleton = true;

	private boolean initialized = false;

	/** 单例情况下的方法调用结果 */
	private Object singletonObject;


	/**
	 * 设置是否应创建单例, 或者在每个 {@link #getObject()}请求上获取新对象. 默认是"true".
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		prepare();
		if (this.singleton) {
			this.initialized = true;
			this.singletonObject = invokeWithTargetException();
		}
	}


	/**
	 * 如果singleton属性设置为“true”, 则每次返回相同的值; 否则返回从动态调用指定方法返回的值.
	 */
	@Override
	public Object getObject() throws Exception {
		if (this.singleton) {
			if (!this.initialized) {
				throw new FactoryBeanNotInitializedException();
			}
			// Singleton: return shared object.
			return this.singletonObject;
		}
		else {
			// Prototype: new object on each call.
			return invokeWithTargetException();
		}
	}

	/**
	 * 返回此FactoryBean创建的对象类型, 或{@code null}如果事先不知道的话.
	 */
	@Override
	public Class<?> getObjectType() {
		if (!isPrepared()) {
			// Not fully initialized yet -> return null to indicate "not known yet".
			return null;
		}
		return getPreparedMethod().getReturnType();
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

}
