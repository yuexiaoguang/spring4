/**
 * 最简单的JNDI SPI实现.
 *
 * <p>用于为测试套件或独立应用程序设置简单的JNDI环境.
 * 例如, 如果JDBC DataSources绑定到与Java EE容器中相同的JNDI名称, 则可以重用应用程序代码和配置而无需更改.
 */
package org.springframework.mock.jndi;
