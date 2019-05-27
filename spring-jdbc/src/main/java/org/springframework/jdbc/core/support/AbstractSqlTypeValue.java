package org.springframework.jdbc.core.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.SqlTypeValue;

/**
 * SqlTypeValue接口的抽象实现, 用于方便地创建应该传递到{@code PreparedStatement.setObject}方法的类型值.
 * {@code createTypeValue}回调方法可以访问底层连接, 如果需要创建任何特定于数据库的对象.
 *
 * <p>StoredProcedure的一个用法示例 (将其与超类javadoc中的普通SqlTypeValue版本进行比较):
 *
 * <pre class="code">proc.declareParameter(new SqlParameter("myarray", Types.ARRAY, "NUMBERS"));
 * ...
 *
 * Map&lt;String, Object&gt; in = new HashMap&lt;String, Object&gt;();
 * in.put("myarray", new AbstractSqlTypeValue() {
 *   public Object createTypeValue(Connection con, int sqlType, String typeName) throws SQLException {
 *	   oracle.sql.ArrayDescriptor desc = new oracle.sql.ArrayDescriptor(typeName, con);
 *	   return new oracle.sql.ARRAY(desc, con, seats);
 *   }
 * });
 * Map out = execute(in);
 * </pre>
 */
public abstract class AbstractSqlTypeValue implements SqlTypeValue {

	@Override
	public final void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName)
			throws SQLException {

		Object value = createTypeValue(ps.getConnection(), sqlType, typeName);
		if (sqlType == TYPE_UNKNOWN) {
			ps.setObject(paramIndex, value);
		}
		else {
			ps.setObject(paramIndex, value, sqlType);
		}
	}

	/**
	 * 创建要传递到{@code PreparedStatement.setObject}的类型值.
	 * 
	 * @param con JDBC连接, 如果需要创建任何特定于数据库的对象
	 * @param sqlType 正在设置的参数的SQL类型
	 * @param typeName 参数的类型名称
	 * 
	 * @return 类型值
	 * @throws SQLException 如果设置参数值时遇到SQLException (也就是说, 不需要捕获SQLException)
	 */
	protected abstract Object createTypeValue(Connection con, int sqlType, String typeName) throws SQLException;

}
