/**
 * 在ApplicationContexts中使用的Bean后处理器，通过自动创建AOP代理来简化AOP使用，而无需使用ProxyFactoryBean.
 *
 * <p>此包中的各种后处理器只需添加到ApplicationContext（通常在XML bean定义文档中）即可自动代理选定的bean.
 *
 * <p><b>NB</b>: BeanFactory实现不支持自动自动代理, 因为后处理器bean只能在应用程序上下文中自动检测到.
 * 后处理器可以在ConfigurableBeanFactory上显式注册.
 */
package org.springframework.aop.framework.autoproxy;
