/**
 * 提供<a href="http://www.hibernate.org">Hibernate 5.x</a>与Spring概念的集成的包.
 *
 * <p>包含Spring的事务SPI实现, 用于本地Hibernate事务.
 * 为了尽可能地遵循Hibernate建议, 该包有意地相当简单, 没有模板类等.
 * 建议使用Hibernate的原生<code>sessionFactory.getCurrentSession()</code>样式.
 *
 * <p><b>该包仅支持Hibernate 5.x.</b>
 * 有关Hibernate 4.x支持, 请参阅{@code org.springframework.orm.hibernate4}包.
 * <b>Note:</b> 不要将HibernateTemplate或hibernate4包中的其他类与Hibernate 5一起使用; 这将导致运行时的类定义异常.
 */
package org.springframework.orm.hibernate5;
