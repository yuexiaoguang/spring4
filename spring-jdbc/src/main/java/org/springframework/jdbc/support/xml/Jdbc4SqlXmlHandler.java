package org.springframework.jdbc.support.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * {@link SqlXmlHandler}接口的默认实现.
 * 提供数据库特定的实现, 用于存储和从数据库中的字段检索XML文档, 依赖于JDBC 4.0 {@code java.sql.SQLXML}工具.
 */
public class Jdbc4SqlXmlHandler implements SqlXmlHandler {

	//-------------------------------------------------------------------------
	// Convenience methods for accessing XML content
	//-------------------------------------------------------------------------

	@Override
	public String getXmlAsString(ResultSet rs, String columnName) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnName);
		return (xmlObject != null ? xmlObject.getString() : null);
	}

	@Override
	public String getXmlAsString(ResultSet rs, int columnIndex) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnIndex);
		return (xmlObject != null ? xmlObject.getString() : null);
	}

	@Override
	public InputStream getXmlAsBinaryStream(ResultSet rs, String columnName) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnName);
		return (xmlObject != null ? xmlObject.getBinaryStream() : null);
	}

	@Override
	public InputStream getXmlAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnIndex);
		return (xmlObject != null ? xmlObject.getBinaryStream() : null);
	}

	@Override
	public Reader getXmlAsCharacterStream(ResultSet rs, String columnName) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnName);
		return (xmlObject != null ? xmlObject.getCharacterStream() : null);
	}

	@Override
	public Reader getXmlAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnIndex);
		return (xmlObject != null ? xmlObject.getCharacterStream() : null);
	}

	@Override
	public Source getXmlAsSource(ResultSet rs, String columnName, Class<? extends Source> sourceClass) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnName);
		if (xmlObject == null) {
			return null;
		}
		return (sourceClass != null ? xmlObject.getSource(sourceClass) : xmlObject.getSource(DOMSource.class));
	}

	@Override
	public Source getXmlAsSource(ResultSet rs, int columnIndex, Class<? extends Source> sourceClass) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnIndex);
		if (xmlObject == null) {
			return null;
		}
		return (sourceClass != null ? xmlObject.getSource(sourceClass) : xmlObject.getSource(DOMSource.class));
	}


	//-------------------------------------------------------------------------
	// Convenience methods for building XML content
	//-------------------------------------------------------------------------

	@Override
	public SqlXmlValue newSqlXmlValue(final String value) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				xmlObject.setString(value);
			}
		};
	}

	@Override
	public SqlXmlValue newSqlXmlValue(final XmlBinaryStreamProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setBinaryStream());
			}
		};
	}

	@Override
	public SqlXmlValue newSqlXmlValue(final XmlCharacterStreamProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setCharacterStream());
			}
		};
	}

	@Override
	public SqlXmlValue newSqlXmlValue(final Class<? extends Result> resultClass, final XmlResultProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setResult(resultClass));
			}
		};
	}

	@Override
	public SqlXmlValue newSqlXmlValue(final Document document) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				xmlObject.setResult(DOMResult.class).setNode(document);
			}
		};
	}


	/**
	 * {@link SqlXmlValue}实现的内部基类.
	 */
	private static abstract class AbstractJdbc4SqlXmlValue implements SqlXmlValue {

		private SQLXML xmlObject;

		@Override
		public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
			this.xmlObject = ps.getConnection().createSQLXML();
			try {
				provideXml(this.xmlObject);
			}
			catch (IOException ex) {
				throw new DataAccessResourceFailureException("Failure encountered while providing XML", ex);
			}
			ps.setSQLXML(paramIndex, this.xmlObject);
		}

		@Override
		public void cleanup() {
			try {
				this.xmlObject.free();
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not free SQLXML object", ex);
			}
		}

		protected abstract void provideXml(SQLXML xmlObject) throws SQLException, IOException;
	}
}
