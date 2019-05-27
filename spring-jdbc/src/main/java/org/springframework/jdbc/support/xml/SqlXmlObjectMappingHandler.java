package org.springframework.jdbc.support.xml;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 用于处理XML对象映射到数据库中的字段的抽象.
 *
 * <p>为解组为Object的XML字段提供访问器方法, 并作为编组{@link SqlXmlValue}实例的工厂.
 */
public interface SqlXmlObjectMappingHandler extends SqlXmlHandler {

	/**
	 * 从给定ResultSet检索的XML数据检索给定的列.
	 * <p>使用内部Object 到 XML Mapping实现.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws java.sql.SQLException 如果被JDBC方法抛出
	 */
	Object getXmlAsObject(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定ResultSet检索的XML数据检索给定的列.
	 * <p>使用内部Object 到 XML Mapping实现.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws java.sql.SQLException 如果被JDBC方法抛出
	 */
	Object getXmlAsObject(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 获取{@code SqlXmlValue}实现的实例, 以与此{@code SqlXmlObjectMappingHandler}的数据库特定实现一起使用.
	 * 
	 * @param value 要编组为XML的对象
	 * 
	 * @return 实现的特定实例
	 */
	SqlXmlValue newMarshallingSqlXmlValue(Object value);

}
