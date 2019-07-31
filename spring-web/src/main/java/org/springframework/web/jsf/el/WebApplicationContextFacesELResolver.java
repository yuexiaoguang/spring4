package org.springframework.web.jsf.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * 特殊的JSF {@code ELResolver}, 在名为"webApplicationContext"的变量下公开Spring {@code WebApplicationContext}实例.
 *
 * <p>与{@link SpringBeanFacesELResolver}相比, 此ELResolver变体<i>不</i>将JSF变量名称解析为Spring bean名称.
 * 它以一个特殊的名称公开Spring的根WebApplicationContext <i>本身</i>,
 * 并且能够解析该应用程序上下文中Spring定义的bean的"webApplicationContext.mySpringManagedBusinessObject"引用.
 *
 * <p>在{@code faces-config.xml}文件中配置此解析器, 如下所示:
 *
 * <pre class="code">
 * &lt;application>
 *   ...
 *   &lt;el-resolver>org.springframework.web.jsf.el.WebApplicationContextFacesELResolver&lt;/el-resolver>
 * &lt;/application></pre>
 */
public class WebApplicationContextFacesELResolver extends ELResolver {

	/**
	 * 公开的WebApplicationContext变量的名称: "webApplicationContext".
	 */
	public static final String WEB_APPLICATION_CONTEXT_VARIABLE_NAME = "webApplicationContext";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public Object getValue(ELContext elContext, Object base, Object property) throws ELException {
		if (base != null) {
			if (base instanceof WebApplicationContext) {
				WebApplicationContext wac = (WebApplicationContext) base;
				String beanName = property.toString();
				if (logger.isTraceEnabled()) {
					logger.trace("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
				}
				if (wac.containsBean(beanName)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
					}
					elContext.setPropertyResolved(true);
					try {
						return wac.getBean(beanName);
					}
					catch (BeansException ex) {
						throw new ELException(ex);
					}
				}
				else {
					// 当base是Map时, 通过返回null, 模仿标准JSF/JSP行为.
					return null;
				}
			}
		}
		else {
			if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
				elContext.setPropertyResolved(true);
				return getWebApplicationContext(elContext);
			}
		}

		return null;
	}

	@Override
	public Class<?> getType(ELContext elContext, Object base, Object property) throws ELException {
		if (base != null) {
			if (base instanceof WebApplicationContext) {
				WebApplicationContext wac = (WebApplicationContext) base;
				String beanName = property.toString();
				if (logger.isDebugEnabled()) {
					logger.debug("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
				}
				if (wac.containsBean(beanName)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
					}
					elContext.setPropertyResolved(true);
					try {
						return wac.getType(beanName);
					}
					catch (BeansException ex) {
						throw new ELException(ex);
					}
				}
				else {
					// 当base是Map时, 通过返回null, 模仿标准JSF/JSP行为.
					return null;
				}
			}
		}
		else {
			if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
				elContext.setPropertyResolved(true);
				return WebApplicationContext.class;
			}
		}

		return null;
	}

	@Override
	public void setValue(ELContext elContext, Object base, Object property, Object value) throws ELException {
	}

	@Override
	public boolean isReadOnly(ELContext elContext, Object base, Object property) throws ELException {
		if (base instanceof WebApplicationContext) {
			elContext.setPropertyResolved(true);
			return true;
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
	 * 检索要公开的{@link WebApplicationContext}引用.
	 * <p>默认实现委托给{@link FacesContextUtils}, 如果没有{@code WebApplicationContext}, 则返回{@code null}.
	 * 
	 * @param elContext 当前的JSF ELContext
	 * 
	 * @return Spring Web应用程序上下文
	 */
	protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
