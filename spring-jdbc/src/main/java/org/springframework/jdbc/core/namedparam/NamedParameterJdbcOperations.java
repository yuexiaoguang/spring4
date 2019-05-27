package org.springframework.jdbc.core.namedparam;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * 指定一组基本的JDBC操作的接口, 允许使用命名参数而不是传统的 '?' 占位符.
 *
 * <p>这是{@link org.springframework.jdbc.core.JdbcOperations}接口的替代方法, 由{@link NamedParameterJdbcTemplate}实现.
 * 此接口通常不直接使用, 但提供了一个有用的选项来增强可测试性, 因为它很容易被模拟或存根.
 */
public interface NamedParameterJdbcOperations {

	/**
	 * 公开经典的Spring JdbcTemplate以允许调用经典的JDBC操作.
	 */
	JdbcOperations getJdbcOperations();


	/**
	 * 执行JDBC数据访问操作, 实现为在JDBC PreparedStatement上进行的回调操作.
	 * 这允许在Spring的托管JDBC环境中对单个Statement实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JDBC SQLExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param sql 要执行的SQL
	 * @param paramSource 绑定到查询的参数的容器
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
			throws DataAccessException;

	/**
	 * 执行JDBC数据访问操作, 实现为在JDBC PreparedStatement上进行的回调操作.
	 * 这允许在Spring的托管JDBC环境中对单个Statement实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JDBC SQLExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param sql 要执行的SQL
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果有任何问题
	 */
	<T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
			throws DataAccessException;

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
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用ResultSetExtractor读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * @param rse 将提取结果的对象
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用ResultSetExtractor读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * @param rse 将提取结果的对象
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 使用ResultSetExtractor读取ResultSet.
	 * <p>Note: 与具有相同签名的JdbcOperations方法相比, 此查询变体始终使用PreparedStatement.
	 * 它实际上等效于带有空参数Map的查询调用.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rse 将提取结果的对象
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用RowCallbackHandler逐行读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * @param rch 将提取结果的对象, 一次一行
	 * 
	 * @throws DataAccessException 如果查询失败
	 */
	void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 使用RowCallbackHandler逐行读取ResultSet.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * @param rch 将提取结果的对象, 一次一行
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 使用RowCallbackHandler逐行读取ResultSet.
	 * <p>Note: 与具有相同签名的JdbcOperations方法相比, 此查询变体始终使用PreparedStatement.
	 * 它实际上等效于带有空参数Map的查询调用.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rch 将提取结果的对象, 一次一行
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	void query(String sql, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将每一行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 结果List, 包含映射对象
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将每一行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 结果List, 包含映射对象
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 通过RowMapper将每一行映射到Java对象.
	 * <p>Note: 与具有相同签名的JdbcOperations方法相比, 此查询变体始终使用PreparedStatement.
	 * 它实际上等效于带有空参数Map的查询调用.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 结果List, 包含映射对象
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将单个结果行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 单个映射对象 (如果给定的{@link RowMapper}返回{@code null}, 则可能是{@code null})
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * 如果查询没有返回一行, 或者没有返回该行中的一列
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 通过RowMapper将单个结果行映射到Java对象.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * @param rowMapper 将每行映射成一个对象的对象
	 * 
	 * @return 单个映射对象 (如果给定的{@link RowMapper}返回{@code null}, 则可能是{@code null})
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * 如果查询没有返回一行, 或者没有返回该行中的一列
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果对象.
	 * <p>该查询应该是单行/单列查询; 返回的结果将直接映射到相应的对象类型.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * @param requiredType 结果对象期望匹配的类型
	 * 
	 * @return 所需类型的结果对象, 或者在SQL NULL的情况下为{@code null}
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * 如果查询没有返回一行, 或者没有返回该行中的一列
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果对象.
	 * <p>该查询应该是单行/单列查询; 返回的结果将直接映射到相应的对象类型.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * @param requiredType 结果对象期望匹配的类型
	 * 
	 * @return 所需类型的结果对象, 或者在SQL NULL的情况下为{@code null}
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * 如果查询没有返回一行, 或者没有返回该行中的一列
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果 Map.
	 * <p>该查询应该是单行查询; 结果行将映射到Map (每列的一个条目, 使用列名作为键).
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * 
	 * @return 结果Map (每列的一个条目, 使用列名作为键)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果 Map.
	 * 当没有域模型时, 此接口定义的queryForMap()方法是合适的.
	 * 否则, 请考虑使用 queryForObject()方法之一.
	 * <p>该查询应该是单行查询; 结果行将映射到Map (每列的一个条目, 使用列名作为键).
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * 
	 * @return 结果Map (每列的一个条目, 使用列名作为键)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 如果查询没有返回一行
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到结果对象的List (每行一个条目), 每个结果对象都匹配指定的元素类型.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * @param elementType 结果列表中所需的元素类型 (例如, {@code Integer.class})
	 * 
	 * @return 与指定元素类型匹配的对象列表
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到结果对象的List (每行一个条目), 每个结果对象都匹配指定的元素类型.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * @param elementType 结果列表中所需的元素类型 (例如, {@code Integer.class})
	 * 
	 * @return 与指定元素类型匹配的对象列表
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	<T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
			throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到Map(每列一个条目, 使用列名作为键)的List (每行一个条目).
	 * 列表中的每个元素都是此接口的{@code queryForMap}方法返回的形式.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * 
	 * @return 包含每行Map的列表
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望结果列表.
	 * <p>结果将映射到Map(每列一个条目, 使用列名作为键)的List (每行一个条目).
	 * 列表中的每个元素都是此接口的{@code queryForMap}方法返回的形式.
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * 
	 * @return 包含每行Map的列表
	 * @throws org.springframework.dao.DataAccessException 如果查询失败
	 */
	List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望 SqlRowSet.
	 * <p>结果将映射到SqlRowSet, 它以断开连接的方式保存数据.
	 * 这个包装器将转换抛出的任何SQLExceptions.
	 * <p>请注意, 对于默认实现, JDBC RowSet支持需要在运行时可用:
	 * 默认情况下, 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类, 它是JDK 1.5+的一部分,
	 * 也可作为Sun的JDBC RowSet实现的一部分单独提供下载 (rowset.jar).
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramSource 绑定到查询的参数的容器
	 * 
	 * @return 一个SqlRowSet表示 (可能是{@code javax.sql.rowset.CachedRowSet}的包装器)
	 * @throws org.springframework.dao.DataAccessException 如果执行查询时有任何问题
	 */
	SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 查询给定SQL, 以从SQL创建预准备语句, 并将参数列表绑定到查询, 期望 SqlRowSet.
	 * <p>结果将映射到SqlRowSet, 它以断开连接的方式保存数据.
	 * 这个包装器将转换抛出的任何SQLExceptions.
	 * <p>请注意, 对于默认实现, JDBC RowSet支持需要在运行时可用:
	 * 默认情况下, 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类, 它是JDK 1.5+的一部分,
	 * 也可作为Sun的JDBC RowSet实现的一部分单独提供下载 (rowset.jar).
	 * 
	 * @param sql 要执行的SQL查询
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * 
	 * @return 一个SqlRowSet表示 (可能是{@code javax.sql.rowset.CachedRowSet}的包装器)
	 * @throws org.springframework.dao.DataAccessException 如果执行查询时有任何问题
	 */
	SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 通过预准备语句发出更新, 绑定给定的参数.
	 * 
	 * @param sql 包含命名参数的SQL
	 * @param paramSource 要绑定到查询的参数和SQL类型的容器
	 * 
	 * @return 受影响的行数
	 * @throws org.springframework.dao.DataAccessException 如果发出更新时有任何问题
	 */
	int update(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 通过预准备语句发出更新, 绑定给定的参数.
	 * 
	 * @param sql 包含命名参数的SQL
	 * @param paramMap 要绑定到查询的参数Map (将其留给PreparedStatement来猜测相应的SQL类型)
	 * 
	 * @return 受影响的行数
	 * @throws org.springframework.dao.DataAccessException 如果发出更新时有任何问题
	 */
	int update(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 通过预准备语句发出更新, 绑定给定参数, 返回生成的键.
	 * 
	 * @param sql 包含命名参数的SQL
	 * @param paramSource 要绑定到查询的参数和SQL类型的容器
	 * @param generatedKeyHolder 将保存生成的键的KeyHolder
	 * 
	 * @return 受影响的行数
	 * @throws org.springframework.dao.DataAccessException 如果发出更新时有任何问题
	 */
	int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
			throws DataAccessException;

	/**
	 * 通过预准备语句发出更新, 绑定给定参数, 返回生成的键.
	 * 
	 * @param sql 包含命名参数的SQL
	 * @param paramSource 要绑定到查询的参数和SQL类型的容器
	 * @param generatedKeyHolder 将保存生成的键的KeyHolder
	 * @param keyColumnNames 将为其生成键的列的名称
	 * 
	 * @return 受影响的行数
	 * @throws org.springframework.dao.DataAccessException 如果发出更新时有任何问题
	 */
	int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames)
			throws DataAccessException;

	/**
	 * 使用提供的SQL语句和一批提供的参数执行批处理.
	 * 
	 * @param sql 要执行的SQL语句
	 * @param batchValues 包含查询批处理参数的Map数组
	 * 
	 * @return 一个数组, 包含批处理中每次更新所影响的行数
	 */
	int[] batchUpdate(String sql, Map<String, ?>[] batchValues);

	/**
	 * 使用提供的SQL语句和一批提供的参数执行批处理.
	 * 
	 * @param sql 要执行的SQL语句
	 * @param batchArgs 包含查询批处理参数的{@link SqlParameterSource}数组
	 * 
	 * @return 一个数组, 包含批处理中每次更新所影响的行数
	 */
	int[] batchUpdate(String sql, SqlParameterSource[] batchArgs);

}
