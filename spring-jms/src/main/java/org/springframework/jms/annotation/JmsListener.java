package org.springframework.jms.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.handler.annotation.MessageMapping;

/**
 * 标记在指定的{@link #destination}上成为JMS消息监听器目标的方法的注解.
 * {@link #containerFactory}标识用于构建JMS监听器容器的{@link org.springframework.jms.config.JmsListenerContainerFactory}.
 * 如果未设置, 则假定<em>默认</em>容器工厂的bean名称为{@code jmsListenerContainerFactory},
 * 除非通过配置提供了显式默认值.
 *
 * <p><b>考虑设置一个自定义的{@link org.springframework.jms.config.DefaultJmsListenerContainerFactory} bean.</b>
 * 出于生产目的, 通常会微调超时和恢复设置.
 * 最重要的是, 默认的'AUTO_ACKNOWLEDGE'模式不提供可靠性保证, 因此请确保在可靠性需求的情况下使用事务处理会话.
 *
 * <p>通过注册{@link JmsListenerAnnotationBeanPostProcessor}来执行{@code @JmsListener}注解的处理.
 * 这可以手动完成, 或者更方便地通过{@code <jms:annotation-driven/>}元素或{@link EnableJms @EnableJms}注解完成.
 *
 * <p>允许带注解的JMS监听器方法具有类似于{@link MessageMapping}提供的灵活签名:
 * <ul>
 * <li>{@link javax.jms.Session}可以访问JMS会话</li>
 * <li>{@link javax.jms.Message}或其子类之一, 以获取对原始JMS消息的访问权限</li>
 * <li>{@link org.springframework.messaging.Message}使用Spring的消息抽象对应</li>
 * <li>带{@link org.springframework.messaging.handler.annotation.Payload @Payload}注解的方法参数, 包括对验证的支持</li>
 * <li>带{@link org.springframework.messaging.handler.annotation.Header @Header}注解的方法参数, 用于提取特定header值,
 * 包括{@link org.springframework.jms.support.JmsHeaders}定义的标准JMS header</li>
 * <li>带{@link org.springframework.messaging.handler.annotation.Headers @Headers}注解的方法参数,
 * 也必须可分配给{@link java.util.Map}以获取对所有header的访问权限</li>
 * <li>{@link org.springframework.messaging.MessageHeaders}参数，用于获取对所有header的访问权限</li>
 * <li>{@link org.springframework.messaging.support.MessageHeaderAccessor}
 * 或{@link org.springframework.jms.support.JmsMessageHeaderAccessor}, 用于访问所有方法参数</li>
 * </ul>
 *
 * <p>带注解的方法可能具有非{@code void}返回类型.
 * 当它们这样做时, 方法调用的结果作为JMS回复发送到由传入消息的{@code JMSReplyTO} header定义的目标.
 * 如果未设置此header, 则可以通过将{@link org.springframework.messaging.handler.annotation.SendTo @SendTo}添加到方法声明来提供默认目标.
 *
 * <p>此注解可用作<em>元注解</em>以使用属性覆盖创建自定义<em>组合注解</em>.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(JmsListeners.class)
@MessageMapping
public @interface JmsListener {

	/**
	 * 管理此端点的容器的唯一标识符.
	 * <p>如果未指定, 则自动生成的一个.
	 */
	String id() default "";

	/**
	 * {@link org.springframework.jms.config.JmsListenerContainerFactory}的bean名称,
	 * 用于创建负责为此端点提供服务的消息监听器容器.
	 * <p>如果未指定, 则使用默认容器工厂.
	 */
	String containerFactory() default "";

	/**
	 * 此监听器的目标名称, 通过容器范围的{@link org.springframework.jms.support.destination.DestinationResolver}策略解析.
	 */
	String destination();

	/**
	 * 持久订阅的名称.
	 */
	String subscription() default "";

	/**
	 * JMS消息选择器表达式.
	 * <p>有关选择器表达式的详细定义, 请参阅JMS规范.
	 */
	String selector() default "";

	/**
	 * 监听器的并发限制. 覆盖用于创建监听器容器的容器工厂定义的值.
	 * <p>并发限制可以是"下限-上限" String &mdash; 例如, "5-10" &mdash;
	 * 或简单的上限 String &mdash; 例如, "10", 在这种情况下, 下限将为1.
	 * <p>请注意, 底层容器可能支持也可能不支持所有功能.
	 * 例, 它可能无法缩放, 在这种情况下仅使用上限.
	 */
	String concurrency() default "";

}
