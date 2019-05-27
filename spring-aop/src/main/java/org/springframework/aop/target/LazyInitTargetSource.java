package org.springframework.aop.target;

import org.springframework.beans.BeansException;

/**
 * {@link org.springframework.aop.TargetSource}从{@link org.springframework.beans.factory.BeanFactory}延迟访问单例bean.
 *
 * <p>在初始化时需要代理引用但在第一次使用之前不应初始化实际目标对象时很有用.
 * 在{@link org.springframework.context.ApplicationContext}中定义目标bean时 (或者是实时预实例化单例bean的{@code BeanFactory})
 * 它也必须标记为“lazy-init”, 否则它将在启动时由{@code ApplicationContext}实例化 (or {@code BeanFactory}).
 * <p>For example:
 *
 * <pre class="code">
 * &lt;bean id="serviceTarget" class="example.MyService" lazy-init="true"&gt;
 *   ...
 * &lt;/bean&gt;
 *
 * &lt;bean id="service" class="org.springframework.aop.framework.ProxyFactoryBean"&gt;
 *   &lt;property name="targetSource"&gt;
 *     &lt;bean class="org.springframework.aop.target.LazyInitTargetSource"&gt;
 *       &lt;property name="targetBeanName"&gt;&lt;idref local="serviceTarget"/&gt;&lt;/property&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * "serviceTarget" bean 在调用"service"代理上的方法之前, 不会初始化.
 *
 * <p>子类可以扩展此类并覆盖{@link #postProcessTargetObject(Object)}以在首次加载时对目标对象执行一些额外的处理.
 */
@SuppressWarnings("serial")
public class LazyInitTargetSource extends AbstractBeanFactoryBasedTargetSource {

	private Object target;


	@Override
	public synchronized Object getTarget() throws BeansException {
		if (this.target == null) {
			this.target = getBeanFactory().getBean(getTargetBeanName());
			postProcessTargetObject(this.target);
		}
		return this.target;
	}

	/**
	 * 子类可以重写此方法，以在首次加载时对目标对象执行其他处理.
	 * 
	 * @param targetObject 刚刚实例化 (并配置)的目标对象
	 */
	protected void postProcessTargetObject(Object targetObject) {
	}

}
