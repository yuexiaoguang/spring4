package org.springframework.orm.hibernate3.support;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import javax.transaction.TransactionManager;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Hibernate UserType implementation for byte arrays that get mapped to BLOBs.
 * Retrieves the LobHandler to use from LocalSessionFactoryBean at config time.
 *
 * <p>Can also be defined in generic Hibernate mappings, as DefaultLobCreator will
 * work with most JDBC-compliant database drivers. In this case, the field type
 * does not have to be BLOB: For databases like MySQL and MS SQL Server, any
 * large enough binary type will work.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class BlobByteArrayType extends AbstractLobType  {

	/**
	 * Constructor used by Hibernate: fetches config-time LobHandler and
	 * config-time JTA TransactionManager from LocalSessionFactoryBean.
	 */
	public BlobByteArrayType() {
		super();
	}

	/**
	 * Constructor used for testing: takes an explicit LobHandler
	 * and an explicit JTA TransactionManager (can be {@code null}).
	 */
	protected BlobByteArrayType(LobHandler lobHandler, TransactionManager jtaTransactionManager) {
		super(lobHandler, jtaTransactionManager);
	}

	@Override
	public int[] sqlTypes() {
		return new int[] {Types.BLOB};
	}

	@Override
	public Class<?> returnedClass() {
		return byte[].class;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public boolean equals(Object x, Object y) {
		return (x == y) ||
				(x instanceof byte[] && y instanceof byte[] && Arrays.equals((byte[]) x, (byte[]) y));
	}

	@Override
	public Object deepCopy(Object value) {
		if (value == null) {
			return null;
		}
		byte[] original = (byte[]) value;
		byte[] copy = new byte[original.length];
		System.arraycopy(original, 0, copy, 0, original.length);
		return copy;
	}

	@Override
	protected Object nullSafeGetInternal(
			ResultSet rs, String[] names, Object owner, LobHandler lobHandler)
			throws SQLException {

		return lobHandler.getBlobAsBytes(rs, names[0]);
	}

	@Override
	protected void nullSafeSetInternal(
			PreparedStatement ps, int index, Object value, LobCreator lobCreator)
			throws SQLException {

		lobCreator.setBlobAsBytes(ps, index, (byte[]) value);
	}

}
