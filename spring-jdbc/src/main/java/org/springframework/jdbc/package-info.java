/**
 * 此包中的类使JDBC更易于使用, 并降低了常见错误的可能性.
 * 特别是他们:
 * <ul>
 * <li>简化错误处理, 避免在应用程序代码中使用 try/catch/finally块.
 * <li>在非受检的异常的通用层次结构中呈现应用程序代码的异常, 使应用程序能够捕获数据访问异常而不依赖于JDBC,
 * 并忽略致命异常, 这对于捕获没有任何价值.
 * <li>允许修改错误处理的实现以针对不同的RDBMS, 而不将专有依赖项引入应用程序代码.
 * </ul>
 *
 * <p>This package and related packages are discussed in Chapter 9 of
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 */
package org.springframework.jdbc;
