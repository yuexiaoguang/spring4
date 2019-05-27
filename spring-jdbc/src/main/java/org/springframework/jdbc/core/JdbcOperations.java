package org.springframework.jdbc.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * 指定一组基本的JDBC操作的接口.
 * 由{@link JdbcTemplate}实现. 不经常直接使用, 但是增强可测试性的有用选项, 因为它很容易被模拟或存根.
 *
 * <p>或者, 可以模拟标准JDBC基础结构. 但是, 模拟这个接口所构成的工作要少得多.
 * 作为测试数据访问代码的模拟对象方法的替代方法, 请考虑{@code org.springframework.test}包中提供的强大的集成测试支持,
 * 该代码包在{@code spring-test.jar}中提供.
 */
public interface JdbcOperations {

	//-------------------------------------------------------------------------
	// Methods dealing with a plain java.sql.Connection
	//-------------------------------------------------------------------------

	/**
	 * 执行JDBC数据访问操作, 实现为在JDBC连接上进行的回调操作.
	 * 这允许在Spring的托管JDBC环境中实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JDBC SQLExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T execute(ConnectionCallback<T> action) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with static SQL (java.sql.Statement)
	//-------------------------------------------------------------------------

	/**
	 * 执行JDBC数据访问操作, 实现为处理JDBC语句的回调操作.
	 * 这允许在Spring的托管JDBC环境中对单个Statement实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JDBC SQLExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T execute(StatementCallback<T> action) throws DataAccessException;

	/**
	 * 发出单个SQL执行, 通常是DDL语句.
	 * 
	 * @param sql 要执行的静态SQL
	 * 
	 * @throws DataAccessException 如果有任何问题
	 */
	void execute(String sql) throws DataAccessException;

	/**
	 * 执行查询给定静态SQL, 使用ResultSetExtractor读取ResultSet.
	 * <p>使用JDBC语句, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询, 使用将{@code null}作为参数数组的重载{@code query}方法.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rse 将提取所有结果行的对象
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	<T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 执行给定静态SQL的查询, 使用RowCallbackHandler逐行读取ResultSet.
	 * <p>使用JDBC Statement, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询, 使用将{@code null}作为参数数组的重载{@code query}方法.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rch 将提取结果的对象, 一次一行
	 * 
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	void query(String sql, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 执行给定静态SQL的查询, 通过RowMapper将每一行映射到Java对象.
	 * <p>使用JDBC Statement, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询, 使用将{@code null}作为参数数组的重载{@code query}方法.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 结果List, 包含映射的对象
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 执行给定静态SQL的查询, 通过RowMapper将单个结果行映射到Java对象.
	 * <p>使用JDBC Statement, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询,
	 * 使用将{@code null}作为参数数组的重载{@link #queryForObject(String, RowMapper, Object...)}方法.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 单个映射的对象 (如果给定的{@link RowMapper}返回{@code null}, 则可能是{@code null})
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	<T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 给定静态SQL, 执行查询.
	 * <p>使用JDBC Statement, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询,
	 * 使用将{@code null}作为参数数组的重载{@link #queryForObject(String, Class, Object...)}方法.
	 * <p>此方法对于运行具有已知结果的静态SQL非常有用.
	 * 该查询应该是单行/单列查询; 返回的结果将直接映射到相应的对象类型.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param requiredType 结果对象期望匹配的类型
	 * 
	 * @return 所需类型的结果对象, 或者在SQL NULL的情况下为{@code null}
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行, 或者没有返回该行中的一列
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	<T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException;

	/**
	 * 给定静态SQL, 执行查询.
	 * <p>使用JDBC Statement, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询,
	 * 使用将{@code null}作为参数数组的重载{@link #queryForMap(String, Object...)}方法.
	 * <p>该查询应该是单行查询; 结果行将映射到Map (每列的一个条目, 使用列名作为键).
	 * 
	 * @param sql 要执行的SQL查询
	 * 
	 * @return 结果Map (每列一个条目, 使用列名作为键)
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	Map<String, Object> queryForMap(String sql) throws DataAccessException;

	/**
	 * 给定静态SQL, 执行查询.
	 * <p>使用JDBC Statement, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询, 使用将{@code null}作为参数数组的重载{@code queryForList}方法.
	 * <p>结果将映射到结果对象的List (每行一个条目), 每个结果对象与指定的元素类型匹配.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param elementType 结果列表中所需的元素类型 (例如, {@code Integer.class})
	 * 
	 * @return 与指定元素类型匹配的对象列表
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	<T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException;

	/**
	 * 给定静态SQL, 执行查询.
	 * <p>使用JDBC Statement, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询, 使用将{@code null}作为参数数组的重载{@code queryForList}方法.
	 * <p>结果将映射到Map (每列使用列名作为键的一个条目)的List (每行一个条目).
	 * 列表中的每个元素都是此接口的 queryForMap()方法返回的形式.
	 * 
	 * @param sql 要执行的SQL查询
	 * 
	 * @return 包含每行Map的List
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	List<Map<String, Object>> queryForList(String sql) throws DataAccessException;

	/**
	 * 给定静态SQL, 执行查询.
	 * <p>使用JDBC Statement, 而不是PreparedStatement.
	 * 如果要使用PreparedStatement执行静态查询, 使用将{@code null}作为参数数组的重载{@code queryForRowSet}方法.
	 * <p>结果将映射到SqlRowSet, 它以断开连接的方式保存数据. 这个包装器将转换抛出的任何SQLExceptions.
	 * <p>请注意, 对于默认实现, JDBC RowSet支持需要在运行时可用:
	 * 默认情况下, 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类, 它是JDK 1.5+的一部分,
	 * 也可作为Sun的JDBC RowSet实现下载 (rowset.jar)的一部分单独提供.
	 * 
	 * @param sql 要执行的SQL查询
	 * 
	 * @return SqlRowSet表示 (可能是{@code javax.sql.rowset.CachedRowSet}的包装器)
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	SqlRowSet queryForRowSet(String sql) throws DataAccessException;

	/**
	 * 发出单个SQL更新操作 (例如插入, 更新或删除语句).
	 * 
	 * @param sql 要执行的静态SQL
	 * 
	 * @return 受影响的行数
	 * @throws DataAccessException 如果有任何问题
	 */
	int update(String sql) throws DataAccessException;

	/**
	 * 使用批处理在单个JDBC语句上发出多个SQL更新.
	 * <p>如果JDBC驱动程序不支持批量更新, 将回退到单个Statement的单独更新.
	 * 
	 * @param sql 定义将要执行的SQL语句数组.
	 * 
	 * @return 每个语句影响的行数的数组
	 * @throws DataAccessException 如果执行批处理有任何问题
	 */
	int[] batchUpdate(String... sql) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with prepared statements
	//-------------------------------------------------------------------------

	/**
	 * 执行JDBC数据访问操作, 实现为在JDBC PreparedStatement上进行的回调操作.
	 * 这允许在Spring的托管JDBC环境中对单个Statement实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JDBC SQLExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param psc 给定Connection的可以创建PreparedStatement的对象
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException;

	/**
	 * 执行JDBC数据访问操作, 实现为在JDBC PreparedStatement上进行的回调操作.
	 * 这允许在Spring的托管JDBC环境中对单个Statement实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JDBC SQLExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param sql 要执行的SQL
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;

	/**
	 * 使用预准备语句进行查询, 使用ResultSetExtractor读取ResultSet.
	 * <p>PreparedStatementCreator既可以直接实现, 也可以通过PreparedStatementCreatorFactory配置.
	 * 
	 * @param psc 给定Connection, 可以创建PreparedStatement的对象
	 * @param rse 将提取结果的对象
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 使用预准备语句进行查询, 使用ResultSetExtractor读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param pss 知道如何在预准备语句上设置值的对象.
	 * 如果这是{@code null}, 则假定SQL不包含绑定参数.
	 * 即使没有绑定参数, 此对象也可用于设置提取大小和其他性能选项.
	 * @param rse 将提取结果的对象
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T query(String sql, PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用ResultSetExtractor读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * @param rse 将提取结果的对象
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用ResultSetExtractor读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * @param rse 将提取结果的对象
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T query(String sql, Object[] args, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用ResultSetExtractor读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rse 将提取结果的对象
	 * @param args 绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T query(String sql, ResultSetExtractor<T> rse, Object... args) throws DataAccessException;

	/**
	 * 使用预准备语句进行查询, 使用RowCallbackHandler逐行读取ResultSet.
	 * <p>PreparedStatementCreator既可以直接实现, 也可以通过PreparedStatementCreatorFactory配置.
	 * 
	 * @param psc 给定Connection, 可以创建PreparedStatement的对象
	 * @param rch 将提取结果的对象, 一次一行
	 * 
	 * @throws DataAccessException 如果有任何问题
	 */
	void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句和PreparedStatementSetter实现,
	 * 该实现知道如何将值绑定到查询, 使用RowCallbackHandler逐行读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param pss 知道如何在预准备语句上设置值的对象.
	 * 如果这是{@code null}, 则假定SQL不包含绑定参数.
	 * 即使没有绑定参数, 此对象也可用于设置提取大小和其他性能选项.
	 * @param rch 将提取结果的对象, 一次一行
	 * 
	 * @throws DataAccessException 如果查询失败
	 */
	void query(String sql, PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用RowCallbackHandler逐行读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * @param rch 将提取结果的对象, 一次一行
	 * 
	 * @throws DataAccessException 如果查询失败
	 */
	void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用RowCallbackHandler逐行读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * @param rch 将提取结果的对象, 一次一行
	 * 
	 * @throws DataAccessException 如果查询失败
	 */
	void query(String sql, Object[] args, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用RowCallbackHandler逐行读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rch 将提取结果的对象, 一次一行
	 * @param args 绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @throws DataAccessException 如果查询失败
	 */
	void query(String sql, RowCallbackHandler rch, Object... args) throws DataAccessException;

	/**
	 * 使用预准备语句进行查询, 通过RowMapper将每一行映射到Java对象.
	 * <p>PreparedStatementCreator既可以直接实现, 也可以通过PreparedStatementCreatorFactory配置.
	 * 
	 * @param psc 给定Connection, 可以创建PreparedStatement的对象
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 结果List, 包含映射的对象
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句和PreparedStatementSetter实现,
	 * 该实现知道如何将值绑定到查询, 通过RowMapper将每一行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param pss 知道如何在预准备语句上设置值的对象.
	 * 如果这是{@code null}, 则假定SQL不包含绑定参数.
	 * 即使没有绑定参数, 此对象也可用于设置提取大小和其他性能选项.
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 结果List, 包含映射的对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将每一行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * @param rowMapper 将每行映射到一个对象的对象
	 * 
	 * @return 结果List, 包含映射的对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将每一行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * @param rowMapper 将每行映射到一个对象的对象
	 * 
	 * @return 结果List, 包含映射的对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将每一行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rowMapper 将每行映射到一个对象的对象
	 * @param args 绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return 结果List, 包含映射的对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将单个结果行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数 (将其留给PreparedStatement来猜测相应的SQL类型)
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 单个映射对象 (如果给定的{@link RowMapper}返回{@code null}, 则可能是{@code null})
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将单个结果行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 单个映射对象 (如果给定的{@link RowMapper}返回{@code null}, 则可能是{@code null})
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将单个结果行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rowMapper 将每行映射成一个对象的对象
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return 单个映射对象 (如果给定的{@link RowMapper}返回{@code null}, 则可能是{@code null})
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果对象.
	 * <p>该查询应该是单行/单列查询; 返回的结果将直接映射到相应的对象类型.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * @param requiredType 结果对象期望匹配的类型
	 * 
	 * @return 所需类型的结果对象, 或者在SQL NULL的情况下为{@code null}
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行, 或者没有返回该行中的一列
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果对象.
	 * <p>该查询应该是单行/单列查询; 返回的结果将直接映射到相应的对象类型.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * @param requiredType 结果对象期望匹配的类型
	 * 
	 * @return 所需类型的结果对象, 或者在SQL NULL的情况下为{@code null}
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行, 或者没有返回该行中的一列
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, Object[] args, Class<T> requiredType) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果对象.
	 * <p>该查询应该是单行/单列查询; 返回的结果将直接映射到相应的对象类型.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param requiredType 结果对象期望匹配的类型
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return 所需类型的结果对象, 或者在SQL NULL的情况下为{@code null}
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行, 或者没有返回该行中的一列
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果Map.
	 * <p>该查询应该是单行查询; 结果行将映射到Map (每列的一个条目, 使用列名作为键).
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * 
	 * @return 结果Map (每列一个条目, 使用列名作为键)
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws DataAccessException 如果查询失败
	 */
	Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果Map.
	 * 当没有域模型时, 此接口定义的queryForMap()方法是合适的.
	 * 否则, 请考虑使用queryForObject()方法之一.
	 * <p>该查询应该是单行查询; 结果行将映射到Map (每列的一个条目, 使用列名作为键).
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return 结果Map (每列一个条目, 使用列名作为键)
	 * @throws IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws DataAccessException 如果查询失败
	 */
	Map<String, Object> queryForMap(String sql, Object... args) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到结果对象的List (每行一个条目), 每个结果对象与指定的元素类型匹配.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * @param elementType 结果列表中所需的元素类型 (例如, {@code Integer.class})
	 * 
	 * @return 与指定元素类型匹配的对象列表
	 * @throws DataAccessException 如果查询失败
	 */
	<T>List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType)
			throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到结果对象的List (每行一个条目), 每个结果对象与指定的元素类型匹配.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * @param elementType 结果列表中所需的元素类型 (例如, {@code Integer.class})
	 * 
	 * @return 与指定元素类型匹配的对象List
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> queryForList(String sql, Object[] args, Class<T> elementType) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到结果对象的List (每行一个条目), 每个结果对象与指定的元素类型匹配.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param elementType 结果列表中所需的元素类型 (例如, {@code Integer.class})
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return 与指定元素类型匹配的对象List
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> queryForList(String sql, Class<T> elementType, Object... args) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到Map (每列一个条目, 使用列名作为键)的List (每行一个条目).
	 * 因此, List中的每个元素都将是此接口的 queryForMap()方法返回的形式.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * 
	 * @return 包含每行Map的List
	 * @throws DataAccessException 如果查询失败
	 */
	List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到Map (每列一个条目, 使用列名作为键)的List (每行一个条目).
	 * 因此, List中的每个元素都将是此接口的 queryForMap()方法返回的形式.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return 包含每行Map的List
	 * @throws DataAccessException 如果查询失败
	 */
	List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望 SqlRowSet.
	 * <p>结果将映射到SqlRowSet, 它以断开连接的方式保存数据. 这个包装器将转换抛出的任何SQLExceptions.
	 * <p>请注意, 对于默认实现, JDBC RowSet支持需要在运行时可用:
	 * 默认情况下, 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类, 它是JDK 1.5+的一部分,
	 * 也可作为Sun的JDBC RowSet实现下载 (rowset.jar)的一部分单独提供.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * 
	 * @return 一个SqlRowSet (可能是{@code javax.sql.rowset.CachedRowSet}的包装器)
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException;

	/**
	 * 查询给定SQL以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望 SqlRowSet.
	 * <p>结果将映射到SqlRowSet, 它以断开连接的方式保存数据. 这个包装器将转换抛出的任何SQLExceptions.
	 * <p>请注意, 对于默认实现, JDBC RowSet支持需要在运行时可用:
	 * 默认情况下, 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类, 它是JDK 1.5+的一部分,
	 * 也可作为Sun的JDBC RowSet实现下载 (rowset.jar)的一部分单独提供.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return 一个SqlRowSet (可能是{@code javax.sql.rowset.CachedRowSet}的包装器)
	 * @throws DataAccessException 如果执行查询有任何问题
	 */
	SqlRowSet queryForRowSet(String sql, Object... args) throws DataAccessException;

	/**
	 * 使用PreparedStatementCreator发出单个SQL更新操作 (例如 insert, update或 delete语句)以提供SQL和任何必需的参数.
	 * <p>PreparedStatementCreator既可以直接实现, 也可以通过PreparedStatementCreatorFactory配置.
	 * 
	 * @param psc 提供SQL和任何必要参数的对象
	 * 
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新有任何问题
	 */
	int update(PreparedStatementCreator psc) throws DataAccessException;

	/**
	 * 使用PreparedStatementCreator发出更新语句, 以提供SQL和任何必需的参数.
	 * 生成的键将被放入给定的KeyHolder中.
	 * <p>请注意, 给定的PreparedStatementCreator必须创建一个语句, 激活提取生成的键 (JDBC 3.0功能).
	 * 这可以直接完成, 也可以使用PreparedStatementCreatorFactory完成.
	 * 
	 * @param psc 提供SQL和任何必要参数的对象
	 * @param generatedKeyHolder 将保存生成的键的KeyHolder
	 * 
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新有任何问题
	 */
	int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException;

	/**
	 * 使用PreparedStatementSetter发出更新语句, 以使用给定的SQL设置绑定参数.
	 * 比使用PreparedStatementCreator更简单, 因为此方法将创建PreparedStatement:
	 * PreparedStatementSetter只需要设置参数.
	 * 
	 * @param sql 包含绑定参数的SQL
	 * @param pss 设置绑定参数的对象. 如果这是{@code null}, 使用静态SQL运行更新.
	 * 
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新有任何问题
	 */
	int update(String sql, PreparedStatementSetter pss) throws DataAccessException;

	/**
	 * 通过预准备的语句发出单个SQL更新操作 (例如插入, 更新或删除语句), 绑定给定的参数.
	 * 
	 * @param sql 包含绑定参数的SQL
	 * @param args 要绑定到查询的参数
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * 
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新有任何问题
	 */
	int update(String sql, Object[] args, int[] argTypes) throws DataAccessException;

	/**
	 * 通过预准备的语句发出单个SQL更新操作 (例如插入, 更新或删除语句), 绑定给定的参数.
	 * 
	 * @param sql 包含绑定参数的SQL
	 * @param args 要绑定到查询的参数
	 * (将它留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的小数位数
	 * 
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新有任何问题
	 */
	int update(String sql, Object... args) throws DataAccessException;

	/**
	 * 在单个PreparedStatement上发出多个更新语句, 使用批处理更新和BatchPreparedStatementSetter来设置值.
	 * <p>如果JDBC驱动程序不支持批量更新, 将回退到单个PreparedStatement上的单独更新.
	 * 
	 * @param sql 定义将被重用的PreparedStatement.
	 * 批处理中的所有语句都将使用相同的SQL.
	 * @param pss 用于在此方法创建的PreparedStatement上设置参数的对象
	 * 
	 * @return 每个语句影响的行数的数组
	 * @throws DataAccessException 如果发出更新有任何问题
	 */
	int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException;

	/**
	 * 使用提供的SQL语句和一批提供的参数执行批处理.
	 * 
	 * @param sql 要执行的SQL语句
	 * @param batchArgs 包含查询批处理参数的Object数组列表
	 * 
	 * @return 一个数组, 包含批处理中每次更新所影响的行数
	 */
	int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException;

	/**
	 * 使用提供的SQL语句和一批提供的参数执行批处理.
	 * 
	 * @param sql 要执行的SQL语句.
	 * @param batchArgs 包含查询批处理参数的Object数组列表
	 * @param argTypes 参数的SQL类型 (来自{@code java.sql.Types}的常量)
	 * 
	 * @return 一个数组, 包含批处理中每次更新所影响的行数
	 */
	int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) throws DataAccessException;

	/**
	 * 使用提供的SQL语句和提供的参数执行多个批处理.
	 * 参数的值将使用ParameterizedPreparedStatementSetter设置.
	 * 每个批处理的大小应在'batchSize'中指明.
	 * 
	 * @param sql 要执行的SQL语句.
	 * @param batchArgs 包含查询批处理参数的Object数组列表
	 * @param batchSize 批处理的大小
	 * @param pss 要使用的ParameterizedPreparedStatementSetter
	 * 
	 * @return 一个数组, 其中包含每个批处理的另一个数组, 该数组包含批处理中每个更新所影响的行数
	 */
	<T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
			ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with callable statements
	//-------------------------------------------------------------------------

	/**
	 * 执行JDBC数据访问操作, 实现为处理JDBC CallableStatement的回调操作.
	 * 这允许在Spring的托管JDBC环境中对单个Statement实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JDBC SQLExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param csc 给定Connection, 可以创建CallableStatement的对象
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action) throws DataAccessException;

	/**
	 * 执行JDBC数据访问操作, 实现为处理JDBC CallableStatement的回调操作.
	 * 这允许在Spring的托管JDBC环境中对单个Statement实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JDBC SQLExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param callString 要执行的SQL调用字符串
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException;

	/**
	 * 使用CallableStatementCreator执行SQL调用, 以提供SQL和任何必需的参数.
	 * 
	 * @param csc 提供SQL和任何必要参数的对象
	 * @param declaredParameters 声明的SqlParameter对象的列表
	 * 
	 * @return 提取出的参数的Map
	 * @throws DataAccessException 如果发出更新有任何问题
	 */
	Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
			throws DataAccessException;

}
