/**
 * 用于表插入和存储过程调用的简化层.
 *
 * <p>{@code SimpleJdbcInsert}和{@code SimpleJdbcCall}利用JDBC驱动程序提供的数据库元数据来简化应用程序代码.
 * 大部分参数规范变得不必要, 因为它可以在元数据中查找.
 */
package org.springframework.jdbc.core.simple;
