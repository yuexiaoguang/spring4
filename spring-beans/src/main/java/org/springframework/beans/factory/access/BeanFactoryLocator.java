package org.springframework.beans.factory.access;

import org.springframework.beans.BeansException;

/**
 * 定义查找, 使用和发布{@link org.springframework.beans.factory.BeanFactory},
 * 或{@code BeanFactory}的子类, 例如{@link org.springframework.context.ApplicationContext}的规范.
 *
 * <p>将此接口实现为单例类, 例如{@link SingletonBeanFactoryLocator}, Spring团队强烈建议谨慎使用它.
 * 到目前为止, 应用程序中的绝大多数代码最好以依赖注入样式编写,
 * 该代码由{@code BeanFactory}/{@ code ApplicationContext}容器提供, 并且在创建容器时由容器提供自己的依赖项.
 * 但是, 甚至这样的单例实现有时也会在小规模的粘合层代码中使用, 有时需要将其他代码绑定在一起.
 * 例如, 第三方代码可能会尝试直接构造新对象, 没有能力迫使它从{@code BeanFactory}中获取这些对象.
 * 如果由第三方代码构造的对象只是一个小stub或代理, 然后使用此类的实现从它获取委派给它的真实对象的地方来获取{@code BeanFactory},
 * 然后实现了适当的依赖注入.
 *
 * <p>另一个例子, 在具有多个层的复杂J2EE应用程序中, 每个层都有自己的{@code ApplicationContext}定义（在层次结构中）,
 * 像{@code SingletonBeanFactoryLocator}这样的类可以用来要求加载这些上下文.
 */
public interface BeanFactoryLocator {

	/**
	 * 使用{@code factoryKey}参数指定的{@link org.springframework.beans.factory.BeanFactory}
	 * (或派生的接口, 例如{@link org.springframework.context.ApplicationContext}).
	 * <p>可以根据需要加载/创建定义.
	 * 
	 * @param factoryKey 一个资源名称, 指定{@code BeanFactory} {@code BeanFactoryLocator}必须返回以供使用.
	 * 资源名称的实际含义特定于{@code BeanFactoryLocator}的实现.
	 * 
	 * @return {@code BeanFactory}实例, 包装为{@link BeanFactoryReference}对象
	 * @throws BeansException 如果加载或访问{@code BeanFactory}时出错
	 */
	BeanFactoryReference useBeanFactory(String factoryKey) throws BeansException;

}
