package org.springframework.beans.factory.config;

import java.io.Serializable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean}实现,
 * 它返回一个{@link org.springframework.beans.factory.ObjectFactory}的值,
 * 该值又返回一个来自 {@link org.springframework.beans.factory.BeanFactory}的bean.
 *
 * <p>因此, 这可以用于避免让客户端对象直接调用 {@link org.springframework.beans.factory.BeanFactory#getBean(String)},
 *  来从{@link org.springframework.beans.factory.BeanFactory}获取(通常是原型)bean,
 * 这将违反控制反转的原则.
 * 相反, 通过使用此类, 客户端对象可以作为一个属性提供给{@link org.springframework.beans.factory.ObjectFactory}实例,
 * 该属性直接只返回一个目标bean(同样, 它通常是一个原型bean) .
 *
 * <p>基于XML的{@link org.springframework.beans.factory.BeanFactory}中的示例配置可能如下所示:
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype bean since we have state --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" scope="prototype"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *       class="org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean"&gt;
 *     &lt;property name="targetBeanName"&gt;&lt;idref local="myService"/&gt;&lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>{@code MyClientBean}类实现可能看起来像这样:
 *
 * <pre class="code">package a.b.c;
 *
 * import org.springframework.beans.factory.ObjectFactory;
 *
 * public class MyClientBean {
 *
 *   private ObjectFactory&lt;MyService&gt; myServiceFactory;
 *
 *   public void setMyServiceFactory(ObjectFactory&lt;MyService&gt; myServiceFactory) {
 *     this.myServiceFactory = myServiceFactory;
 *   }
 *
 *   public void someBusinessMethod() {
 *     // get a 'fresh', brand new MyService instance
 *     MyService service = this.myServiceFactory.getObject();
 *     // use the service object to effect the business logic...
 *   }
 * }</pre>
 *
 * <p>对象创建模式的这种应用的另一种方法是使用{@link ServiceLocatorFactoryBean}来提供(原型)bean.
 * {@link ServiceLocatorFactoryBean}方法的优点是, 不需要依赖任何特定于Spring的接口, 例如{@link org.springframework.beans.factory.ObjectFactory},
 * 但是缺点是需要运行时类生成.
 * 请查阅{@link ServiceLocatorFactoryBean ServiceLocatorFactoryBean JavaDoc}以更全面地讨论此问题.
 */
public class ObjectFactoryCreatingFactoryBean extends AbstractFactoryBean<ObjectFactory<Object>> {

	private String targetBeanName;


	/**
	 * 设置目标bean的名称.
	 * <p>目标不必是非单例bean, 但实际上总是如此
	 * (因为如果目标bean是单例, 那么所说的单例bean可以简单地直接注入到依赖对象中,
	 * 从而避免了对这种工厂方法提供的额外间接级别的需求).
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
		super.afterPropertiesSet();
	}


	@Override
	public Class<?> getObjectType() {
		return ObjectFactory.class;
	}

	@Override
	protected ObjectFactory<Object> createInstance() {
		return new TargetBeanObjectFactory(getBeanFactory(), this.targetBeanName);
	}


	/**
	 * 独立的内部类 - 用于序列化.
	 */
	@SuppressWarnings("serial")
	private static class TargetBeanObjectFactory implements ObjectFactory<Object>, Serializable {

		private final BeanFactory beanFactory;

		private final String targetBeanName;

		public TargetBeanObjectFactory(BeanFactory beanFactory, String targetBeanName) {
			this.beanFactory = beanFactory;
			this.targetBeanName = targetBeanName;
		}

		@Override
		public Object getObject() throws BeansException {
			return this.beanFactory.getBean(this.targetBeanName);
		}
	}

}
