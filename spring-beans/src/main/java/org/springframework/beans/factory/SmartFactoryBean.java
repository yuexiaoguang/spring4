package org.springframework.beans.factory;

/**
 * {@link FactoryBean}接口的扩展.
 * 实现可以指示它们是否总是返回独立的实例, 用于返回{@code false}的{@link #isSingleton()}实现没有明确指出独立的实例的情况.
 *
 * <p>如果{@link #isSingleton()}实现返回{@code false}, 那么简单地假定不实现此扩展接口的普通{@link FactoryBean}实现总是返回独立的实例;
 * 只能按需访问公开的对象.
 *
 * <p><b>NOTE:</b> 该接口是一个专用接口, 主要用于框架内和协作框架内的内部使用.
 * 一般来说, 应用程序提供的FactoryBeans应该只实现普通的{@link FactoryBean}接口.
 * 即使在点发布中, 也可以向此扩展接口添加新方法.
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * 该工厂管理的对象是原型吗?
	 * 即, {@link #getObject()}是否总是返回一个独立的实例?
	 * <p>FactoryBean本身的原型状态通常由{@link BeanFactory}提供; 通常, 它必须被定义为单例.
	 * <p>该方法应该严格检查独立的实例;
	 * 对于作用域对象或其他类型的非单例非独立对象, 它不应返回{@code true}.
	 * 出于这个原因, 这不仅仅是{@link #isSingleton()}的倒置形式.
	 * 
	 * @return 暴露的对象是否是原型
	 */
	boolean isPrototype();

	/**
	 * 这个FactoryBean是否期望实时初始化, 即, 实时地初始化自己以及期待其单例对象实时初始化?
	 * <p>标准的FactoryBean不会实时地初始化:
	 * 只会调用它的{@link #getObject()}以进行实际访问, 即使是单例对象.
	 * 从此方法返回{@code true}表明应该实时地调用{@link #getObject()}, 也应该实时地应用后处理器.
	 * 对于{@link #isSingleton() singleton}对象, 这可能有意义, 特别是如果后处理器期望在启动时应用.
	 * 
	 * @return 是否实时初始化
	 */
	boolean isEagerInit();

}
