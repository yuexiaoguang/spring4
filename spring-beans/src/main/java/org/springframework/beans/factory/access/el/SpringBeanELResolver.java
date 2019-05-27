package org.springframework.beans.factory.access.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;

/**
 * 委托给Spring BeanFactory的Unified EL {@code ELResolver}, 解析对Spring定义的bean的名称引用.
 */
public abstract class SpringBeanELResolver extends ELResolver {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public Object getValue(ELContext elContext, Object base, Object property) throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Successfully resolved variable '" + beanName + "' in Spring BeanFactory");
				}
				elContext.setPropertyResolved(true);
				return bf.getBean(beanName);
			}
		}
		return null;
	}

	@Override
	public Class<?> getType(ELContext elContext, Object base, Object property) throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				elContext.setPropertyResolved(true);
				return bf.getType(beanName);
			}
		}
		return null;
	}

	@Override
	public void setValue(ELContext elContext, Object base, Object property, Object value) throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				if (value == bf.getBean(beanName)) {
					// 将bean引用设置为相同的值是正常的 - can simply be ignored...
					elContext.setPropertyResolved(true);
				}
				else {
					throw new PropertyNotWritableException(
							"Variable '" + beanName + "' refers to a Spring bean which by definition is not writable");
				}
			}
		}
	}

	@Override
	public boolean isReadOnly(ELContext elContext, Object base, Object property) throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
		return null;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
		return Object.class;
	}


	/**
	 * 将解析bean名称的Spring BeanFactory.
	 * 
	 * @param elContext 当前ELContext
	 * 
	 * @return Spring BeanFactory (never {@code null})
	 */
	protected abstract BeanFactory getBeanFactory(ELContext elContext);

}
