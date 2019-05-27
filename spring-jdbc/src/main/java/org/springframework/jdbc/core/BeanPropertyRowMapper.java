package org.springframework.jdbc.core;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link RowMapper}实现, 它将行转换为指定映射目标类的新实例.
 * 映射的目标类必须是顶级类, 并且必须具有default或no-arg构造函数.
 *
 * <p>基于将从结果集元数据获得的列名, 与相应属性的公共setter匹配来映射列值.
 * 这些名称可以直接匹配, 也可以使用"驼峰"将名称与下划线分隔成相同的名称.
 *
 * <p>为许多常见类型提供了目标类中字段的映射, e.g.:
 * String, boolean, Boolean, byte, Byte, short, Short, int, Integer, long, Long,
 * float, Float, double, Double, BigDecimal, {@code java.util.Date}, etc.
 *
 * <p>为了便于列和没有匹配名称的字段之间的映射, 尝试在SQL语句中使用列别名,
 * 例如"select fname as first_name from customer".
 *
 * <p>对于从数据库读取的'null'值, 将尝试调用setter, 但在Java基础类型的情况下, 这会导致TypeMismatchException.
 * 可以配置此类 (使用 primitivesDefaultedForNullValue 属性) 来捕获此异常并使用基础类型默认值.
 * 请注意, 如果使用生成的bean中的值来更新数据库, 则原始值将设置为基础类型的默认值而不是null.
 *
 * <p>请注意, 此类旨在提供方便而非高性能.
 * 为获得最佳性能, 请考虑使用自定义{@link RowMapper}实现.
 */
public class BeanPropertyRowMapper<T> implements RowMapper<T> {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 要映射到的类 */
	private Class<T> mappedClass;

	/** 是否严格验证 */
	private boolean checkFullyPopulated = false;

	/** 是否在映射null值时默认基础类型 */
	private boolean primitivesDefaultedForNullValue = false;

	/** 用于将JDBC值绑定到bean属性的ConversionService */
	private ConversionService conversionService = DefaultConversionService.getSharedInstance();

	/** 提供映射的字段 */
	private Map<String, PropertyDescriptor> mappedFields;

	/** 提供映射的bean属性 */
	private Set<String> mappedProperties;


	/**
	 * 用于bean样式配置.
	 */
	public BeanPropertyRowMapper() {
	}

	/**
	 * 接受目标bean中未填充的属性.
	 * <p>请考虑使用{@link #newInstance}工厂方法, 它允许仅指定一次映射类型.
	 * 
	 * @param mappedClass 每行应映射到的类
	 */
	public BeanPropertyRowMapper(Class<T> mappedClass) {
		initialize(mappedClass);
	}

	/**
	 * @param mappedClass 每行应映射到的类
	 * @param checkFullyPopulated 是否严格验证所有bean属性是否已从相应的数据库字段映射
	 */
	public BeanPropertyRowMapper(Class<T> mappedClass, boolean checkFullyPopulated) {
		initialize(mappedClass);
		this.checkFullyPopulated = checkFullyPopulated;
	}


	/**
	 * 设置每行应映射到的类.
	 */
	public void setMappedClass(Class<T> mappedClass) {
		if (this.mappedClass == null) {
			initialize(mappedClass);
		}
		else {
			if (this.mappedClass != mappedClass) {
				throw new InvalidDataAccessApiUsageException("The mapped class can not be reassigned to map to " +
						mappedClass + " since it is already providing mapping for " + this.mappedClass);
			}
		}
	}

	/**
	 * 获取要映射到的类.
	 */
	public final Class<T> getMappedClass() {
		return this.mappedClass;
	}

	/**
	 * 设置是否严格验证已从相应的数据库字段映射到所有bean属性.
	 * <p>默认{@code false}, 接受目标bean中未填充的属性.
	 */
	public void setCheckFullyPopulated(boolean checkFullyPopulated) {
		this.checkFullyPopulated = checkFullyPopulated;
	}

	/**
	 * 是否严格验证已从相应的数据库字段映射到所有bean属性.
	 */
	public boolean isCheckFullyPopulated() {
		return this.checkFullyPopulated;
	}

	/**
	 * 设置在从相应的数据库字段映射null值的情况下, 是否默认Java基础类型.
	 * <p>默认 {@code false}, 当null映射到Java基础类型时抛出异常.
	 */
	public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
		this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
	}

	/**
	 * 返回在从相应的数据库字段映射null值的情况下, 是否默认Java基础类型.
	 */
	public boolean isPrimitivesDefaultedForNullValue() {
		return this.primitivesDefaultedForNullValue;
	}

	/**
	 * 设置{@link ConversionService}以将JDBC值绑定到bean属性, 或{@code null}为none.
	 * <p>从Spring 4.3开始, 默认为{@link DefaultConversionService}.
	 * 这为{@code java.time}转换和其他特殊类型提供了支持.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回{@link ConversionService}以将JDBC值绑定到bean属性, 如果没有, 则返回{@code null}.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}


	/**
	 * 初始化给定类的映射元数据.
	 * 
	 * @param mappedClass 映射的类
	 */
	protected void initialize(Class<T> mappedClass) {
		this.mappedClass = mappedClass;
		this.mappedFields = new HashMap<String, PropertyDescriptor>();
		this.mappedProperties = new HashSet<String>();
		PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(mappedClass);
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null) {
				this.mappedFields.put(lowerCaseName(pd.getName()), pd);
				String underscoredName = underscoreName(pd.getName());
				if (!lowerCaseName(pd.getName()).equals(underscoredName)) {
					this.mappedFields.put(underscoredName, pd);
				}
				this.mappedProperties.add(pd.getName());
			}
		}
	}

	/**
	 * 将camelCase中的名称转换为小写的下划线名称.
	 * 任何大写字母都会转换为带有前面下划线的小写字母.
	 * 
	 * @param name 原始名称
	 * 
	 * @return 已转换的名称
	 */
	protected String underscoreName(String name) {
		if (!StringUtils.hasLength(name)) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		result.append(lowerCaseName(name.substring(0, 1)));
		for (int i = 1; i < name.length(); i++) {
			String s = name.substring(i, i + 1);
			String slc = lowerCaseName(s);
			if (!s.equals(slc)) {
				result.append("_").append(slc);
			}
			else {
				result.append(s);
			}
		}
		return result.toString();
	}

	/**
	 * 将给定名称转换为小写.
	 * 默认情况下, 转化将在US区域设置中进行.
	 * 
	 * @param name 原始名称
	 * 
	 * @return 已转换的名称
	 */
	protected String lowerCaseName(String name) {
		return name.toLowerCase(Locale.US);
	}


	/**
	 * 提取当前行中所有列的值.
	 * <p>利用public setter和结果集元数据.
	 */
	@Override
	public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
		Assert.state(this.mappedClass != null, "Mapped class was not specified");
		T mappedObject = BeanUtils.instantiateClass(this.mappedClass);
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);
		initBeanWrapper(bw);

		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Set<String> populatedProperties = (isCheckFullyPopulated() ? new HashSet<String>() : null);

		for (int index = 1; index <= columnCount; index++) {
			String column = JdbcUtils.lookupColumnName(rsmd, index);
			String field = lowerCaseName(column.replaceAll(" ", ""));
			PropertyDescriptor pd = this.mappedFields.get(field);
			if (pd != null) {
				try {
					Object value = getColumnValue(rs, index, pd);
					if (rowNumber == 0 && logger.isDebugEnabled()) {
						logger.debug("Mapping column '" + column + "' to property '" + pd.getName() +
								"' of type '" + ClassUtils.getQualifiedName(pd.getPropertyType()) + "'");
					}
					try {
						bw.setPropertyValue(pd.getName(), value);
					}
					catch (TypeMismatchException ex) {
						if (value == null && this.primitivesDefaultedForNullValue) {
							if (logger.isDebugEnabled()) {
								logger.debug("Intercepted TypeMismatchException for row " + rowNumber +
										" and column '" + column + "' with null value when setting property '" +
										pd.getName() + "' of type '" +
										ClassUtils.getQualifiedName(pd.getPropertyType()) +
										"' on object: " + mappedObject, ex);
							}
						}
						else {
							throw ex;
						}
					}
					if (populatedProperties != null) {
						populatedProperties.add(pd.getName());
					}
				}
				catch (NotWritablePropertyException ex) {
					throw new DataRetrievalFailureException(
							"Unable to map column '" + column + "' to property '" + pd.getName() + "'", ex);
				}
			}
			else {
				// No PropertyDescriptor found
				if (rowNumber == 0 && logger.isDebugEnabled()) {
					logger.debug("No property found for column '" + column + "' mapped to field '" + field + "'");
				}
			}
		}

		if (populatedProperties != null && !populatedProperties.equals(this.mappedProperties)) {
			throw new InvalidDataAccessApiUsageException("Given ResultSet does not contain all fields " +
					"necessary to populate object of class [" + this.mappedClass.getName() + "]: " +
					this.mappedProperties);
		}

		return mappedObject;
	}

	/**
	 * 初始化给定的BeanWrapper以用于行映射.
	 * 要为每一行调用.
	 * <p>默认实现应用已配置的{@link ConversionService}. 可以在子类中重写.
	 * 
	 * @param bw 要初始化的BeanWrapper
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		ConversionService cs = getConversionService();
		if (cs != null) {
			bw.setConversionService(cs);
		}
	}

	/**
	 * 检索指定列的JDBC对象值.
	 * <p>默认实现调用
	 * {@link JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)}.
	 * 子类可以覆盖它以预先检查特定值类型, 或者从{@code getResultSetValue}返回后处理值.
	 * 
	 * @param rs 保存数据的ResultSet
	 * @param index 列索引
	 * @param pd 每个结果对象应该匹配的bean属性
	 * 
	 * @return Object值
	 * @throws SQLException 在提取失败的情况下
	 */
	protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
	}


	/**
	 * 静态工厂方法创建一个新的{@code BeanPropertyRowMapper} (只指定一次映射类).
	 * 
	 * @param mappedClass 每行应映射到的类
	 */
	public static <T> BeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
		return new BeanPropertyRowMapper<T>(mappedClass);
	}
}
