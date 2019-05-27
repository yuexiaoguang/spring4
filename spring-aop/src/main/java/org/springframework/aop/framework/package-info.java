/**
 * 包含Spring的基本AOP基础架构的包, 符合<a href="http://aopalliance.sourceforge.net">AOP Alliance</a>接口.
 *
 * <p>Spring AOP支持代理接口或类，引入，并提供静态和动态切点.
 *
 * <p>可以将任何Spring AOP代理强制转换为此程序包中的ProxyConfig AOP配置接口，以添加或删除拦截器.
 *
 * <p>ProxyFactoryBean是在BeanFactory或ApplicationContext中创建AOP代理的便捷方式.
 * 但是, 可以使用ProxyFactory类以编程方式创建代理.
 */
package org.springframework.aop.framework;
