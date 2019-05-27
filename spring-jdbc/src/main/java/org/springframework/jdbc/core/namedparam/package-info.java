/**
 * 具有命名参数支持的JdbcTemplate变体.
 *
 * <p>NamedParameterJdbcTemplate是JdbcTemplate的包装器, 它添加了对命名参数解析的支持.
 * 它没有实现JdbcOperations接口或扩展JdbcTemplate, 而是实现了专用的NamedParameterJdbcOperations接口.
 *
 * <P>如果需要Spring JDBC的全部功能来进行不太常见的操作,
 * 请使用NamedParameterJdbcTemplate的{@code getJdbcOperations()}方法并使用返回的经典模板, 或直接使用JdbcTemplate实例.
 */
package org.springframework.jdbc.core.namedparam;
