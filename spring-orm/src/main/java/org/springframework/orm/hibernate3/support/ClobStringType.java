package org.springframework.orm.hibernate3.support;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.transaction.TransactionManager;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Hibernate UserType implementation for Strings that get mapped to CLOBs.
 * Retrieves the LobHandler to use from LocalSessionFactoryBean at config time.
 *
 * <p>Particularly useful for storing Strings with more than 4000 characters in an
 * Oracle database (only possible via CLOBs), in combination with OracleLobHandler.
 *
 * <p>Can also be defined in generic Hibernate mappings, as DefaultLobCreator will
 * work with most JDBC-compliant database drivers. In this case, the field type
 * does not have to be CLOB: For databases like MySQL and MS SQL Server, any
 * large enough character type will work.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class ClobStringType extends AbstractLobType {

	/**
	 * Constructor used by Hibernate: fetches config-time LobHandler and
	 * config-time JTA TransactionManager from LocalSessionFactoryBean.
	 */
	public ClobStringType() {
		super();
	}

	/**
	 * Constructor used for testing: takes an explicit LobHandler
	 * and an explicit JTA TransactionManager (can be {@code null}).
	 */
	protected ClobStringType(LobHandler lobHandler, TransactionManager jtaTransactionManager) {
		super(lobHandler, jtaTransactionManager);
	}

	@Override
	public int[] sqlTypes() {
		return new int[] {Types.CLOB};
	}

	@Override
	public Class<?> returnedClass() {
		return String.class;
	}

	@Override
	protected Object nullSafeGetInternal(
			ResultSet rs, String[] names, Object owner, LobHandler lobHandler)
			throws SQLException {

		return lobHandler.getClobAsString(rs, names[0]);
	}

	@Override
	protected void nullSafeSetInternal(
			PreparedStatement ps, int index, Object value, LobCreator lobCreator)
			throws SQLException {

		lobCreator.setClobAsString(ps, index, (String) value);
	}

}
