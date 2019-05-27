package org.springframework.orm.hibernate3;

import java.util.Properties;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean that encapsulates a Hibernate type definition.
 *
 * <p>Typically defined as inner bean within a LocalSessionFactoryBean
 * definition, as list element for the "typeDefinitions" bean property.
 * For example:
 *
 * <pre class="code">
 * &lt;bean id="sessionFactory" class="org.springframework.orm.hibernate3.LocalSessionFactoryBean"&gt;
 *   ...
 *   &lt;property name="typeDefinitions"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="org.springframework.orm.hibernate3.TypeDefinitionBean"&gt;
 *         &lt;property name="typeName" value="myType"/&gt;
 *         &lt;property name="typeClass" value="mypackage.MyTypeClass"/&gt;
 *       &lt;/bean&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 *   ...
 * &lt;/bean&gt;</pre>
 *
 * Alternatively, specify a bean id (or name) attribute for the inner bean,
 * instead of the "typeName" property.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class TypeDefinitionBean implements BeanNameAware, InitializingBean {

	private String typeName;

	private String typeClass;

	private Properties parameters = new Properties();


	/**
	 * Set the name of the type.
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	/**
	 * Return the name of the type.
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * Set the type implementation class.
	 * @see org.hibernate.cfg.Mappings#addTypeDef(String, String, java.util.Properties)
	 */
	public void setTypeClass(String typeClass) {
		this.typeClass = typeClass;
	}

	/**
	 * Return the type implementation class.
	 */
	public String getTypeClass() {
		return typeClass;
	}

	/**
	 * Specify default parameters for the type.
	 * This only applies to parameterized types.
	 * @see org.hibernate.cfg.Mappings#addTypeDef(String, String, java.util.Properties)
	 * @see org.hibernate.usertype.ParameterizedType
	 */
	public void setParameters(Properties parameters) {
		this.parameters = parameters;
	}

	/**
	 * Return the default parameters for the type.
	 */
	public Properties getParameters() {
		return parameters;
	}


	/**
	 * If no explicit type name has been specified, the bean name of
	 * the TypeDefinitionBean will be used.
	 * @see #setTypeName
	 */
	@Override
	public void setBeanName(String name) {
		if (this.typeName == null) {
			this.typeName = name;
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (this.typeName == null) {
			throw new IllegalArgumentException("typeName is required");
		}
		if (this.typeClass == null) {
			throw new IllegalArgumentException("typeClass is required");
		}
	}

}
