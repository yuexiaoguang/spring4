package org.springframework.web.jsf.el;

import javax.el.ELContext;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.el.SpringBeanELResolver;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * 委托给Spring root {@code WebApplicationContext}的JSF {@code ELResolver}, 解析对Spring定义的bean的名称引用.
 *
 * <p>在{@code faces-config.xml}文件中配置此解析器, 如下所示:
 *
 * <pre class="code">
 * &lt;application>
 *   ...
 *   &lt;el-resolver>org.springframework.web.jsf.el.SpringBeanFacesELResolver&lt;/el-resolver>
 * &lt;/application></pre>
 *
 * 然后, 所有JSF表达式都可以隐式引用Spring管理的服务层bean的名称, 例如在JSF管理的bean的属性值中:
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
 * 将"mySpringManagedBusinessObject"定义为applicationContext.xml中的Spring bean:
 *
 * <pre class="code">
 * &lt;bean id="mySpringManagedBusinessObject" class="example.MySpringManagedBusinessObject">
 *   ...
 * &lt;/bean></pre>
 */
public class SpringBeanFacesELResolver extends SpringBeanELResolver {

	/**
	 * 此实现委托给{@link #getWebApplicationContext}.
	 * 可以重写以提供任意BeanFactory引用来解析; 通常, 这将是一个完整的Spring ApplicationContext.
	 * 
	 * @param elContext 当前的JSF ELContext
	 * 
	 * @return the Spring BeanFactory (never {@code null})
	 */
	@Override
	protected BeanFactory getBeanFactory(ELContext elContext) {
		return getWebApplicationContext(elContext);
	}

	/**
	 * 检索将bean名称解析委托给的Web应用程序上下文.
	 * <p>默认实现委托给FacesContextUtils.
	 * 
	 * @param elContext 当前JSF ELContext
	 * 
	 * @return Spring Web应用程序上下文 (never {@code null})
	 */
	protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
