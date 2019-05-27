/**
 * 该包包含允许轻松访问EJB的类.
 * 基础是在EJB调用之前和之后运行的AOP拦截器.
 * 特别是, 此包中的类允许使用本地接口透明地访问无状态会话Bean (SLSBs),
 * 避免应用程序代码需要使用它们, 来使用特定于EJB的API和JNDI查找, 并可以和在不使用EJB时实现的业务接口一起运行.
 * 这为客户端 (例如Web组件) 和业务对象 (可能是EJB, 也可能不是EJB)提供了有价值的解耦.
 * 这使我们可以选择将EJB引入应用程序 (或从应用程序中删除EJB), 而不会影响使用业务对象的代码.
 *
 * <p>这个包中类的动机将在
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>的第11章讨论,
 * by Rod Johnson (Wrox, 2002).
 *
 * <p>但是, 此包中的类的实现和命名已更改.
 * 它现在使用FactoryBeans和AOP, 而不是<i>Expert One-on-One J2EE</i>中描述的自定义bean定义.
 */
package org.springframework.ejb.access;
