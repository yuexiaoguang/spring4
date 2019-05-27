package org.springframework.jdbc.support.xml;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.w3c.dom.Document;

/**
 * 用于处理特定数据库中的XML字段的抽象.
 * 其主要目的是隔离数据库中存储的XML的特定于数据库的处理.
 *
 * <p>JDBC 4.0引入了新的数据类型{@code java.sql.SQLXML}, 但大多数数据库及其驱动程序目前依赖于特定于数据库的数据类型和功能.
 *
 * <p>提供XML字段的访问器方法, 并充当{@link SqlXmlValue}实例的工厂.
 */
public interface SqlXmlHandler {

	//-------------------------------------------------------------------------
	// Convenience methods for accessing XML content
	//-------------------------------------------------------------------------

	/**
	 * 从给定的ResultSet中检索给定列.
	 * <p>可能只是调用{@code ResultSet.getString}或使用{@code SQLXML}或特定于数据库的类, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	String getXmlAsString(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * <p>可能只是调用{@code ResultSet.getString}或使用{@code SQLXML}或特定于数据库的类, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	String getXmlAsString(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * <p>可能只是调用{@code ResultSet.getAsciiStream}或使用{@code SQLXML}或特定于数据库的类, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	InputStream getXmlAsBinaryStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * <p>可能只是调用{@code ResultSet.getAsciiStream}或使用{@code SQLXML}或特定于数据库的类, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	InputStream getXmlAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * <p>可能只是调用{@code ResultSet.getCharacterStream}或使用{@code SQLXML}或特定于数据库的类, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Reader getXmlAsCharacterStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * <p>可能只是调用{@code ResultSet.getCharacterStream}或使用{@code SQLXML}或特定于数据库的类, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Reader getXmlAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 使用指定的源类从给定的ResultSet中检索给定列.
	 * <p>可能与{@code SQLXML}或特定于数据库的类一起使用, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名
	 * @param sourceClass 要使用的实现类
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Source getXmlAsSource(ResultSet rs, String columnName, Class<? extends Source> sourceClass) throws SQLException;

	/**
	 * 使用指定的源类从给定的ResultSet中检索给定列.
	 * <p>可能与{@code SQLXML}或特定于数据库的类一起使用, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * @param sourceClass 要使用的实现类
	 * 
	 * @return 内容, 或{@code null}用于SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Source getXmlAsSource(ResultSet rs, int columnIndex, Class<? extends Source> sourceClass) throws SQLException;


	//-------------------------------------------------------------------------
	// Convenience methods for building XML content
	//-------------------------------------------------------------------------

	/**
	 * 根据底层JDBC驱动程序的支持, 为给定的XML数据创建一个{@code SqlXmlValue}实例.
	 * 
	 * @param value 提供XML数据的XML String值
	 * 
	 * @return 实现特定实例
	 */
	SqlXmlValue newSqlXmlValue(String value);

	/**
	 * 根据底层JDBC驱动程序的支持, 为给定的XML数据创建一个{@code SqlXmlValue}实例.
	 * 
	 * @param provider 提供XML数据的{@code XmlBinaryStreamProvider}
	 * 
	 * @return 实现特定实例
	 */
	SqlXmlValue newSqlXmlValue(XmlBinaryStreamProvider provider);

	/**
	 * 根据底层JDBC驱动程序的支持, 为给定的XML数据创建一个{@code SqlXmlValue}实例.
	 * 
	 * @param provider 提供XML数据的{@code XmlCharacterStreamProvider}
	 * 
	 * @return 实现特定实例
	 */
	SqlXmlValue newSqlXmlValue(XmlCharacterStreamProvider provider);

	/**
	 * 根据底层JDBC驱动程序的支持, 为给定的XML数据创建一个{@code SqlXmlValue}实例.
	 * 
	 * @param resultClass 要使用的Result实现类
	 * @param provider 提供XML数据的{@code XmlResultProvider}
	 * 
	 * @return 实现特定实例
	 */
	SqlXmlValue newSqlXmlValue(Class<? extends Result> resultClass, XmlResultProvider provider);

	/**
	 * 根据底层JDBC驱动程序的支持, 为给定的XML数据创建一个{@code SqlXmlValue}实例.
	 * 
	 * @param doc 要使用的XML Document
	 * 
	 * @return 实现特定实例
	 */
	SqlXmlValue newSqlXmlValue(Document doc);

}
