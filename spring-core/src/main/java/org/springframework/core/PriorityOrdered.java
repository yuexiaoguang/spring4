package org.springframework.core;

/**
 * {@link Ordered}接口的扩展, 表示<em>优先级</em>排序:
 * {@code PriorityOrdered}对象表示的排序值始终在<em>普通的</em> {@link Ordered}对象表示的相同排序值之前应用.
 *
 * <p>这主要是一个专用接口, 用于首先识别<em>优先级</em>对象最为重要的对象, 甚至无需获取剩余对象.
 * 一个典型的例子:
 * Spring {@link org.springframework.context.ApplicationContext}中的优先级后处理器.
 *
 * <p>Note: {@code PriorityOrdered}后处理器bean在特殊阶段初始化, 领先于其他后处理器bean.
 * 这巧妙地影响了他们的自动装配行为: 它们只会针对不需要实时初始化的类型匹配的bean进行自动装配.
 */
public interface PriorityOrdered extends Ordered {

}
