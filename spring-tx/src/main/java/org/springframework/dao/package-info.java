/**
 * 异常层次结构支持复杂的错误处理, 与使用中的数据访问方法无关.
 * 例如, 当DAO和数据访问框架使用此包中的异常 (和自定义子类)时, 调用代码可以检测和处理常见问题(如死锁),
 * 而不必依赖于特定的数据访问策略, 例如JDBC.
 *
 * <p>所有这些异常都是未受检的, 这意味着调用代码可以使它们保持未被捕获状态, 并将所有数据访问异常视为致命的.
 *
 * <p>Rod Johnson (Wrox, 2002)的
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * 第9章讨论了这个包中的类.
 */
package org.springframework.dao;
