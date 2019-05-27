package org.springframework.web.jsf.el;

import javax.el.ELContext;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.el.SpringBeanELResolver;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * JSF {@code ELResolver} that delegates to the Spring root {@code WebApplicationContext},
 * resolving name references to Spring-defined beans.
 *
 * <p>Configure this resolver in your {@code faces-config.xml} file as follows:
 *
 * <pre class="code">
 * &lt;application>
 *   ...
 *   &lt;el-resolver>org.springframework.web.jsf.el.SpringBeanFacesELResolver&lt;/el-resolver>
 * &lt;/application></pre>
 *
 * All your JSF expressions can then implicitly refer to the names of
 * Spring-managed service layer beans, for example in property values of
 * JSF-managed beans:
 *
 * <pre class="code">
 * &lt;managed-bean>
 *   &lt;managed-bean-name>myJsfManagedBean&lt;/managed-bean-name>
 *   &lt;managed-bean-class>example.MyJsfManagedBean&lt;/managed-bean-class>
 *   &lt;managed-bean-scope>session&lt;/managed-bean-scope>
 *   &lt;managed-property>
 *     &lt;property-name>mySpringManagedBusinessObject&lt;/property-name>
 *     &lt;value>#{mySpringManagedBusinessObject}&lt;/value>
 *   &lt;/managed-property>
 * &lt;/managed-bean></pre>
 *
 * with "mySpringManagedBusinessObject" defined as Spring bean in
 * applicationContext.xml:
 *
 * <pre class="code">
 * &lt;bean id="mySpringManagedBusinessObject" class="example.MySpringManagedBusinessObject">
 *   ...
 * &lt;/bean></pre>
 */
public class SpringBeanFacesELResolver extends SpringBeanELResolver {

	/**
	 * This implementation delegates to {@link #getWebApplicationContext}.
	 * Can be overridden to provide an arbitrary BeanFactory reference to resolve
	 * against; usually, this will be a full Spring ApplicationContext.
	 * @param elContext the current JSF ELContext
	 * @return the Spring BeanFactory (never {@code null})
	 */
	@Override
	protected BeanFactory getBeanFactory(ELContext elContext) {
		return getWebApplicationContext(elContext);
	}

	/**
	 * Retrieve the web application context to delegate bean name resolution to.
	 * <p>The default implementation delegates to FacesContextUtils.
	 * @param elContext the current JSF ELContext
	 * @return the Spring web application context (never {@code null})
	 * @see org.springframework.web.jsf.FacesContextUtils#getRequiredWebApplicationContext
	 */
	protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
