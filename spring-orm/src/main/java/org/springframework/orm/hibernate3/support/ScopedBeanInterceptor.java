package org.springframework.orm.hibernate3.support;

import org.hibernate.EmptyInterceptor;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.support.AopUtils;

/**
 * Hibernate3 interceptor used for getting the proper entity name for scoped
 * beans. As scoped bean classes are proxies generated at runtime, they are
 * unrecognized by the persisting framework. Using this interceptor, the
 * original scoped bean class is retrieved end exposed to Hibernate for
 * persisting.
 *
 * <p>Usage example:
 *
 * <pre class="code">
 * &lt;bean id=&quot;sessionFactory&quot; class=&quot;org.springframework.orm.hibernate3.LocalSessionFactoryBean&quot;&gt;
 *   ...
 *   &lt;property name=&quot;entityInterceptor&quot;&gt;
 *     &lt;bean class=&quot;org.springframework.orm.hibernate3.support.ScopedBeanInterceptor&quot;/&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
@SuppressWarnings("serial")
public class ScopedBeanInterceptor extends EmptyInterceptor {

	@Override
	public String getEntityName(Object entity) {
		if (entity instanceof ScopedObject) {
			// Determine underlying object's type.
			Object targetObject = ((ScopedObject) entity).getTargetObject();
			return AopUtils.getTargetClass(targetObject).getName();
		}

		// Any other object: delegate to the default implementation.
		return super.getEntityName(entity);
	}

}
