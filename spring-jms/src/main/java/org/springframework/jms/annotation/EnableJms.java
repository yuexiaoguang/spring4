package org.springframework.jms.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 通过{@link org.springframework.jms.config.JmsListenerContainerFactory JmsListenerContainerFactory}
 * 启用创建的JMS监听器带注解的端点.
 * 要在{@link org.springframework.context.annotation.Configuration Configuration}类上使用, 如下所示:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public DefaultJmsListenerContainerFactory myJmsListenerContainerFactory() {
 *       DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
 *       factory.setConnectionFactory(connectionFactory());
 *       factory.setDestinationResolver(destinationResolver());
 *       factory.setSessionTransacted(true);
 *       factory.setConcurrency("5");
 *       return factory;
 *     }
 *
 *     // other &#064;Bean definitions
 * }</pre>
 *
 * {@code JmsListenerContainerFactory}创建负责特定端点的监听器容器.
 * 典型实现, 如上面示例中使用的
 * {@link org.springframework.jms.config.DefaultJmsListenerContainerFactory DefaultJmsListenerContainerFactory},
 * 提供了底层{@link org.springframework.jms.listener.MessageListenerContainer MessageListenerContainer}
 * 支持的必要配置选项.
 *
 * <p>{@code @EnableJms}允许在容器中的任何Spring管理的bean上检测{@link JmsListener}注解.
 * 例如, 给定一个类{@code MyService}:
 *
 * <pre class="code">
 * package com.acme.foo;
 *
 * public class MyService {
 *
 *     &#064;JmsListener(containerFactory = "myJmsListenerContainerFactory", destination="myQueue")
 *     public void process(String msg) {
 *         // process incoming message
 *     }
 * }</pre>
 *
 * 要使用的容器工厂由{@link JmsListener#containerFactory() containerFactory}属性标识,
 * 该属性定义要使用的{@code JmsListenerContainerFactory} bean的名称.
 * 当没有设置时, 假定存在名为{@code jmsListenerContainerFactory}的{@code JmsListenerContainerFactory} bean.
 *
 * <p>以下配置将确保每次在名为"myQueue"的{@link javax.jms.Destination}上收到{@link javax.jms.Message}时,
 * 将使用消息的内容调用{@code MyService.process()}:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 *     // JMS infrastructure setup
 * }</pre>
 *
 * 或者, 如果{@code MyService}使用{@code @Component}注解,
 * 则以下配置将确保使用匹配的传入消息调用其带{@code @JmsListener}注解的方法:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * &#064;ComponentScan(basePackages="com.acme.foo")
 * public class AppConfig {
 * }</pre>
 *
 * 请注意, 创建的容器未针对应用程序上下文进行注册, 但可以使用
 * {@link org.springframework.jms.config.JmsListenerEndpointRegistry JmsListenerEndpointRegistry}轻松定位以进行管理.
 *
 * <p>带注解的方法可以使用灵活的签名; 特别是, 可以使用{@link org.springframework.messaging.Message Message}抽象和相关注解,
 * 有关详细信息, 请参阅{@link JmsListener} Javadoc.
 * 例如, 以下内容将注入消息的内容和自定义的"myCounter" JMS header:
 *
 * <pre class="code">
 * &#064;JmsListener(containerFactory = "myJmsListenerContainerFactory", destination="myQueue")
 * public void process(String msg, @Header("myCounter") int counter) {
 *     // process incoming message
 * }</pre>
 *
 * 这些功能由{@link org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory}抽象,
 * 负责构建必要的调用器以处理带注解的方法.
 * 默认使用{@link org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory}.
 *
 * <p>当需要更多控制时, {@code @Configuration}类可以实现{@link JmsListenerConfigurer}.
 * 这允许访问底层{@link org.springframework.jms.config.JmsListenerEndpointRegistrar JmsListenerEndpointRegistrar}实例.
 * 以下示例演示如何指定显式默认{@code JmsListenerContainerFactory}
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         registrar.setContainerFactory(myJmsListenerContainerFactory());
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerContainerFactory<?> myJmsListenerContainerFactory() {
 *         // factory settings
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 * }</pre>
 *
 * 作为参考, 可以将上面的示例与以下Spring XML配置进行比较:
 *
 * <pre class="code">
 * {@code <beans>
 *
 *     <jms:annotation-driven container-factory="myJmsListenerContainerFactory"/>
 *
 *     <bean id="myJmsListenerContainerFactory" class="org.springframework.jms.config.DefaultJmsListenerContainerFactory">
 *           // factory settings
 *     </bean>
 *
 *     <bean id="myService" class="com.acme.foo.MyService"/>
 *
 * </beans>
 * }</pre>
 *
 * 如果需要更多地控制容器的创建和管理,
 * 也可以指定自定义{@link org.springframework.jms.config.JmsListenerEndpointRegistry JmsListenerEndpointRegistry}.
 * 下面的示例还演示了如何自定义{@code JmsHandlerMethodFactory},
 * 以与自定义{@link org.springframework.validation.Validator Validator}一起使用,
 * 以便使用{@link org.springframework.validation.annotation.Validated Validated}注解的有效负载首先针对自定义{@code Validator}进行验证.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         registrar.setEndpointRegistry(myJmsListenerEndpointRegistry());
 *         registrar.setMessageHandlerMethodFactory(myJmsHandlerMethodFactory);
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerEndpointRegistry<?> myJmsListenerEndpointRegistry() {
 *         // registry configuration
 *     }
 *
 *     &#064;Bean
 *     public JmsHandlerMethodFactory myJmsHandlerMethodFactory() {
 *        DefaultJmsHandlerMethodFactory factory = new DefaultJmsHandlerMethodFactory();
 *        factory.setValidator(new MyValidator());
 *        return factory;
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 * }</pre>
 *
 * 作为参考, 可以将上面的示例与以下Spring XML配置进行比较:
 * <pre class="code">
 * {@code <beans>
 *
 *     <jms:annotation-driven registry="myJmsListenerEndpointRegistry"
 *         handler-method-factory="myJmsHandlerMethodFactory"/&gt;
 *
 *     <bean id="myJmsListenerEndpointRegistry"
 *           class="org.springframework.jms.config.JmsListenerEndpointRegistry">
 *           // registry configuration
 *     </bean>
 *
 *     <bean id="myJmsHandlerMethodFactory"
 *           class="org.springframework.messaging.handler.support.DefaultJmsHandlerMethodFactory">
 *         <property name="validator" ref="myValidator"/>
 *     </bean>
 *
 *     <bean id="myService" class="com.acme.foo.MyService"/>
 *
 * </beans>
 * }</pre>
 *
 * 实现{@code JmsListenerConfigurer}还允许通过{@code JmsListenerEndpointRegistrar}对端点注册进行细粒度控制.
 * 例如, 以下配置额外端点:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         SimpleJmsListenerEndpoint myEndpoint = new SimpleJmsListenerEndpoint();
 *         // ... configure the endpoint
 *         registrar.registerEndpoint(endpoint, anotherJmsListenerContainerFactory());
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerContainerFactory<?> anotherJmsListenerContainerFactory() {
 *         // ...
 *     }
 *
 *     // JMS infrastructure setup
 * }</pre>
 *
 * 请注意, 将以类似的方式检测和调用实现了{@code JmsListenerConfigurer}的所有bean.
 * 如果使用XML配置, 上面的示例可以在上下文中注册的常规bean定义中进行转换.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(JmsBootstrapConfiguration.class)
public @interface EnableJms {
}
