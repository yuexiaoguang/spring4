package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;

/**
 * 用于访问Spring bean容器的根接口.
 * 这是bean容器的基本客户端视图;
 * 其他接口, 例如{@link ListableBeanFactory}和{@link org.springframework.beans.factory.config.ConfigurableBeanFactory}可用于特定目的.
 *
 * <p>此接口由包含许多bean定义的对象实现, 每个都由String名称唯一标识.
 * 取决于bean的定义, 工厂将返回包含对象的独立实例 (原型设计模式), 或单个共享实例 (Singleton设计模式的优越替代品, 其实例是工厂范围内的单例).
 * 将返回哪种类型的实例取决于bean工厂配置: API 是一样的.
 * 从Spring 2.0开始, 根据具体的应用环境, 可以使用更多的范围 (e.g. web环境中的"request"和"session"范围).
 *
 * <p>这种方法的重点是, BeanFactory是应用程序组件的中央注册表, 并集中应用程序组件的配置 (例如, 单个对象不再需要读取属性文件).
 * 有关此方法的优点的讨论, 请参阅“Expert One-on-One J2EE设计和开发”的第4章和第11章.
 *
 * <p>请注意，通常最好依靠依赖注入("push" 配置)来通过setter或构造函数配置应用程序对象, 而不是使用任何形式的"pull"配置, 例如BeanFactory查找.
 * Spring的依赖注入功能是使用这个BeanFactory接口及其子接口实现的.
 *
 * <p>通常, BeanFactory将加载存储在配置源中的bean定义 (例如XML文档), 并使用{@code org.springframework.beans}包来配置bean.
 * 但是, 实现可以在Java代码中直接返回它创建的Java对象. 对如何存储定义没有限制: LDAP, RDBMS, XML, properties 文件, etc.
 * 鼓励实现支持bean之间的引用 (依赖注入).
 *
 * <p>与{@link ListableBeanFactory}中的方法形成对比, 如果这是{@link HierarchicalBeanFactory}, 此接口中的所有操作也将检查父工厂.
 * 如果在此工厂实例中找不到bean, 将询问最直接的父级工厂. 此工厂实例中的Bean应该覆盖在父级工厂中同名的Bean.
 *
 * <p>Bean工厂实现应尽可能支持标准bean生命周期接口. 完整的初始化方法及其标准顺序是:
 * <ol>
 * <li>BeanNameAware's {@code setBeanName}
 * <li>BeanClassLoaderAware's {@code setBeanClassLoader}
 * <li>BeanFactoryAware's {@code setBeanFactory}
 * <li>EnvironmentAware's {@code setEnvironment}
 * <li>EmbeddedValueResolverAware's {@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware's {@code setResourceLoader} (仅适用于在应用程序上下文中运行时)
 * <li>ApplicationEventPublisherAware's {@code setApplicationEventPublisher} (仅适用于在应用程序上下文中运行时)
 * <li>MessageSourceAware's {@code setMessageSource} (仅适用于在应用程序上下文中运行时)
 * <li>ApplicationContextAware's {@code setApplicationContext} (仅适用于在应用程序上下文中运行时)
 * <li>ServletContextAware's {@code setServletContext} (仅适用于在应用程序上下文中运行时)
 * <li>BeanPostProcessors 的 {@code postProcessBeforeInitialization}方法
 * <li>InitializingBean's {@code afterPropertiesSet}
 * <li>自定义的init方法定义
 * <li>BeanPostProcessors 的 {@code postProcessAfterInitialization}方法
 * </ol>
 *
 * <p>bean 工厂关闭时, 以下生命周期方法适用:
 * <ol>
 * <li>DestructionAwareBeanPostProcessors 的 {@code postProcessBeforeDestruction}方法
 * <li>DisposableBean's {@code destroy}
 * <li>自定义销毁方法定义
 * </ol>
 */
public interface BeanFactory {

	/**
	 * 用于取消引用{@link FactoryBean}实例, 并将其与FactoryBean创建的bean区分开来.
	 * 例如, 如果名为{@code myJndiObject}的bean是FactoryBean, 获取{@code &myJndiObject}将返回工厂, 不是工厂返回的实例.
	 */
	String FACTORY_BEAN_PREFIX = "&";


	/**
	 * 返回指定bean的实例, 该实例可以是共享的或独立的.
	 * <p>此方法允许Spring BeanFactory用作Singleton或Prototype设计模式的替代.
	 * 在Singleton bean的情况下, 调用者可以保留对返回对象的引用.
	 * <p>将别名转换回相应的规范bean名称. 将询问父级工厂是否在此工厂实例中找不到bean.
	 * 
	 * @param name 要检索的bean的名称
	 * 
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException 如果没有具有指定名称的bean定义
	 * @throws BeansException 如果无法获得bean
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * 返回指定bean的实例, 该实例可以是共享的或独立的.
	 * <p>行为与{@link #getBean(String)}相同, 但如果bean不是所需类型, 则通过抛出BeanNotOfRequiredTypeException来提供类型安全性的衡量.
	 * 这意味着在正确转换结果时不能抛出ClassCastException, 但{@link #getBean(String)}会发生这种情况.
	 * <p>将别名转换回相应的规范bean名称. 将询问父级工厂是否在此工厂实例中找不到bean.
	 * 
	 * @param name 要检索的bean的名称
	 * @param requiredType bean必须匹配的类型. 可以是实际类的接口或超类, 或{@code null}都匹配.
	 * 例如, 如果值是{@code Object.class}, 无论返回的实例的类是什么, 此方法都将成功.
	 * 
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 * @throws BeanNotOfRequiredTypeException 如果bean不是所需的类型
	 * @throws BeansException 如果无法创建bean
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定bean的实例, 该实例可以是共享的或独立的.
	 * <p>允许指定显式构造函数参数/工厂方法参数, 覆盖bean定义中指定的默认参数.
	 * 
	 * @param name 要检索的bean的名称
	 * @param args 使用显式参数创建bean实例时使用的参数 (仅在创建新实例时应用, 而不是在检索现有实例时应用)
	 * 
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 * @throws BeanDefinitionStoreException 如果已经给出了参数, 但受影响的bean不是原型
	 * @throws BeansException 如果无法创建bean
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * 返回唯一匹配给定对象类型的bean实例.
	 * <p>此方法进入{@link ListableBeanFactory}按类型查找区域, 但也可以根据给定类型的名称转换为常规的按名称查找.
	 * 对于跨Bean集的更广泛的检索操作, 使用{@link ListableBeanFactory} 或 {@link BeanFactoryUtils}.
	 * 
	 * @param requiredType bean必须匹配的类型; 可以是接口或超类. 不允许{@code null}.
	 * 
	 * @return 匹配所需类型的单个bean的实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定bean的实例, 该实例可以是共享的或独立的.
	 * <p>允许指定显式构造函数参数/工厂方法参数, 覆盖bean定义中指定的默认参数.
	 * <p>此方法进入{@link ListableBeanFactory}按类型查找区域, 但也可以根据给定类型的名称转换为常规的按名称查找.
	 * 对于跨Bean集的更广泛的检索操作, 使用{@link ListableBeanFactory} 或 {@link BeanFactoryUtils}.
	 * 
	 * @param requiredType bean必须匹配的类型; 可以是接口或超类. 不允许{@code null}.
	 * @param args 使用显式参数创建bean实例时使用的参数 (仅在创建新实例时应用, 而不是在检索现有实例时应用)
	 * 
	 * @return bean的实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 * @throws BeanDefinitionStoreException 如果已经给出了参数, 但受影响的bean不是原型
	 * @throws BeansException 如果无法创建bean
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;


	/**
	 * 此bean工厂是否包含具有给定名称的bean定义或外部注册的单例实例?
	 * <p>如果给定名称是别名, 它将被翻译回相应的规范bean名称.
	 * <p>如果这个工厂是分层的, 将询问父级工厂是否在此工厂实例中找不到bean.
	 * <p>如果找到与给定名称匹配的bean定义或单例实例,
	 * 无论命名的bean定义是具体的还是抽象的, 延迟的还是实时的, 这个方法都会返回{@code true}.
	 * 因此, 请注意, 此方法返回{@code true}不一定表示{@link #getBean}能够获取同名的实例.
	 * 
	 * @param name 要查询的bean的名称
	 * 
	 * @return 是否存在具有给定名称的bean
	 */
	boolean containsBean(String name);

	/**
	 * 这个bean是一个共享单例吗? 即, {@link #getBean}始终返回相同的实例?
	 * <p>Note: 此方法返回{@code false}不能清楚地表明独立的实例.
	 * 它表示非单例实例, 也可以对应于作用域bean. 使用{@link #isPrototype}操作显式检查独立的实例.
	 * <p>将别名转换回相应的规范bean名称. 将询问父级工厂是否在此工厂实例中找不到bean.
	 * 
	 * @param name 要查询的bean的名称
	 * 
	 * @return 此bean是否对应于单例实例
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 这个bean是原型吗? 即, {@link #getBean}始终返回独立的实例?
	 * <p>Note: 此方法返回{@code false}不能清楚地指示单例对象.
	 * 它表示非独立的实例, 也可以对应于范围bean. 使用{@link #isSingleton}操作显式检查共享单例实例.
	 * <p>将别名转换回相应的规范bean名称. 将询问父级工厂是否在此工厂实例中找不到bean.
	 * 
	 * @param name 要查询的bean的名称
	 * 
	 * @return 这个bean是否总是提供独立的实例
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 检查具有给定名称的bean是否与指定的类型匹配.
	 * 进一步来说, 检查对给定名称的{@link #getBean}调用是否会返回可分配给指定目标类型的对象.
	 * <p>将别名转换回相应的规范bean名称. 将询问父级工厂是否在此工厂实例中找不到bean.
	 * 
	 * @param name 要查询的bean的名称
	 * @param typeToMatch 要匹配的类型 (as a {@code ResolvableType})
	 * 
	 * @return {@code true}如果bean类型匹配, {@code false} 如果它不匹配或无法确定
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 */
	boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 检查具有给定名称的bean是否与指定的类型匹配.
	 * 进一步来说, 检查对给定名称的{@link #getBean}调用是否会返回可分配给指定目标类型的对象.
	 * <p>将别名转换回相应的规范bean名称. 将询问父级工厂是否在此工厂实例中找不到bean.
	 * 
	 * @param name 要查询的bean的名称
	 * @param typeToMatch 要匹配的类型 (as a {@code Class})
	 * 
	 * @return {@code true} 如果bean类型匹配, {@code false} 如果它不匹配或无法确定
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 */
	boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的bean的类型.
	 * 进一步来说, 确定{@link #getBean}将为给定名称返回的对象类型.
	 * <p>对于{@link FactoryBean}, 返回FactoryBean创建的对象类型, 由{@link FactoryBean#getObjectType()}公开.
	 * <p>将别名转换回相应的规范bean名称. 将询问父级工厂是否在此工厂实例中找不到bean.
	 * 
	 * @param name 要查询的bean的名称
	 * 
	 * @return bean的类型, 或{@code null}不确定
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 */
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定bean名称的别名.
	 * 当在{@link #getBean}调用中使用时, 所有这些别名都指向同一个bean.
	 * <p>如果给定名称是别名, 将返回相应的原始bean名称和其他别名, 原始bean名称是数组中的第一个元素.
	 * <p>将询问父级工厂是否在此工厂实例中找不到bean.
	 * 
	 * @param name 用于检查别名的bean名称
	 * 
	 * @return 别名, 或空数组
	 */
	String[] getAliases(String name);

}
