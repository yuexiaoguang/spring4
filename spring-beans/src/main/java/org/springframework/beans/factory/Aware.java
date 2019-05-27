package org.springframework.beans.factory;

/**
 * 标记超级接口, 指示bean能够通过回调样式方法, 由Spring容器通知特定框架对象.
 * 实际方法签名由各个子接口确定, 但通常应该只包含一个接受单个参数的void返回方法.
 *
 * <p>请注意, 仅实现{@link Aware}不提供默认功能.
 * 相反, 必须明确地进行处理, 例如在 {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}中.
 * 参考 {@link org.springframework.context.support.ApplicationContextAwareProcessor}
 * 和 {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory}
 * 的处理 {@code *Aware}接口回调.
 */
public interface Aware {

}
