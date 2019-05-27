/**
 * 此包中的类将RDBMS查询, 更新和存储过程表示为线程安全的可重用对象.
 * 这种方法由JDO建模, 但当然查询返回的对象与数据库"断开连接".
 *
 * <p>这种更高级别的JDBC抽象取决于{@code org.springframework.jdbc.core}包中的低级抽象.
 * 抛出的异常与{@code org.springframework.dao}包一样, 这意味着使用此包的代码不需要实现JDBC或RDBMS特定的错误处理.
 *
 * <p>This package and related packages are discussed in Chapter 9 of
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
package org.springframework.jdbc.object;
