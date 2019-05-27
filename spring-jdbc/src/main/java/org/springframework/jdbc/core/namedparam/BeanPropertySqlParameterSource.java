package org.springframework.jdbc.core.namedparam;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.util.StringUtils;

/**
 * {@link SqlParameterSource}实现, 它从给定JavaBean对象的bean属性中获取参数值.
 * bean属性的名称必须与参数名称匹配.
 *
 * <p>使用Spring BeanWrapper进行下面的bean属性访问.
 */
public class BeanPropertySqlParameterSource extends AbstractSqlParameterSource {

	private final BeanWrapper beanWrapper;

	private String[] propertyNames;


	/**
	 * @param object 要包装的bean实例
	 */
	public BeanPropertySqlParameterSource(Object object) {
		this.beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
	}


	@Override
	public boolean hasValue(String paramName) {
		return this.beanWrapper.isReadableProperty(paramName);
	}

	@Override
	public Object getValue(String paramName) throws IllegalArgumentException {
		try {
			return this.beanWrapper.getPropertyValue(paramName);
		}
		catch (NotReadablePropertyException ex) {
			throw new IllegalArgumentException(ex.getMessage());
		}
	}

	/**
	 * 从相应的属性类型派生默认SQL类型.
	 */
	@Override
	public int getSqlType(String paramName) {
		int sqlType = super.getSqlType(paramName);
		if (sqlType != TYPE_UNKNOWN) {
			return sqlType;
		}
		Class<?> propType = this.beanWrapper.getPropertyType(paramName);
		return StatementCreatorUtils.javaTypeToSqlParameterType(propType);
	}

	/**
	 * 提供对包装bean的属性名称的访问.
	 * 使用{@link PropertyAccessor}接口中提供的支持.
	 * 
	 * @return 包含所有已知属性名称的数组
	 */
	public String[] getReadablePropertyNames() {
		if (this.propertyNames == null) {
			List<String> names = new ArrayList<String>();
			PropertyDescriptor[] props = this.beanWrapper.getPropertyDescriptors();
			for (PropertyDescriptor pd : props) {
				if (this.beanWrapper.isReadableProperty(pd.getName())) {
					names.add(pd.getName());
				}
			}
			this.propertyNames = StringUtils.toStringArray(names);
		}
		return this.propertyNames;
	}

}
