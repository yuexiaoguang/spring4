package org.springframework.context;

import java.io.Closeable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ProtocolResolver;

/**
 * 由大多数应用程序上下文实现的SPI接口.
 * 除了{@link org.springframework.context.ApplicationContext}接口中的应用程序上下文客户端方法之外, 还提供配置应用程序上下文的工具.
 *
 * <p>这里封装了配置和生命周期方法, 以避免使它们对ApplicationContext客户端代码变得明显.
 * 本方法只能由启动和关闭代码使用.
 */
public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {

	/**
	 * 任何数量的这些字符都被视为单个String值中多个上下文配置路径之间的分隔符.
	 */
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

	/**
	 * 工厂中的ConversionService bean的名称.
	 * 如果未提供, 则应用默认转换规则.
	 */
	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	/**
	 * 工厂中LoadTimeWeaver bean的名称.
	 * 如果提供了这样的bean, 则上下文将使用临时ClassLoader进行类型匹配, 以便允许LoadTimeWeaver处理所有实际的bean类.
	 */
	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

	/**
	 * 工厂中{@link Environment} bean的名称.
	 */
	String ENVIRONMENT_BEAN_NAME = "environment";

	/**
	 * 工厂中System属性bean的名称.
	 */
	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

	/**
	 * 工厂中的系统环境bean的名称.
	 */
	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";


	/**
	 * 设置此应用程序上下文的唯一ID.
	 */
	void setId(String id);

	/**
	 * 设置此应用程序上下文的父级.
	 * <p>请注意, 不应更改父级:
	 * 如果在创建此类的对象时它不可用, 则只应在构造函数外部设置它, 例如在WebApplicationContext设置的情况下.
	 * 
	 * @param parent 父级上下文
	 */
	void setParent(ApplicationContext parent);

	/**
	 * 为此应用程序上下文设置{@code Environment}.
	 * 
	 * @param environment 新的环境
	 */
	void setEnvironment(ConfigurableEnvironment environment);

	/**
	 * 以可配置的形式返回此应用程序上下文的{@code Environment}, 以便进一步自定义.
	 */
	@Override
	ConfigurableEnvironment getEnvironment();

	/**
	 * 添加一个新的BeanFactoryPostProcessor, 它将在刷新时应用于此应用程序上下文的内部bean工厂,
	 * 在评估任何bean定义之前. 在上下文配置期间调用.
	 * 
	 * @param postProcessor 要注册的工厂处理器
	 */
	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

	/**
	 * 添加一个新的ApplicationListener, 它将在上下文事件(例如上下文刷新和上下文关闭)上得到通知.
	 * <p>请注意, 如果上下文尚未处于活动状态, 则此处注册的任何ApplicationListener都将在刷新时应用,
	 * 或者在已经激活的上下文的情况下, 使用当前事件多播器.
	 * 
	 * @param listener 要注册的ApplicationListener
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 使用此应用程序上下文注册给定的协议解析程序, 允许处理其他资源协议.
	 * <p>任何此类解析器都将在此上下文的标准解析规则之前调用. 因此它也可以覆盖任何默认规则.
	 */
	void addProtocolResolver(ProtocolResolver resolver);

	/**
	 * 加载或刷新配置的持久表示, 可能是XML文件、属性文件或关系数据库模式.
	 * <p>由于这是一个启动方法, 它应该销毁已创建的单例, 如果它失败, 以避免悬空资源.
	 * 换句话说, 在调用该方法之后, 应该实例化所有实例, 不论是不是单例.
	 * 
	 * @throws BeansException 如果bean工厂无法初始化
	 * @throws IllegalStateException 如果已初始化并且不支持多次刷新尝试
	 */
	void refresh() throws BeansException, IllegalStateException;

	/**
	 * 向JVM运行时注册关闭挂钩, 在JVM关闭时关闭此上下文.
	 * <p>可以多次调用此方法. 每个上下文实例最多只会注册一个关闭挂钩.
	 */
	void registerShutdownHook();

	/**
	 * 关闭此应用程序上下文, 释放实现可能包含的所有资源和锁.
	 * 这包括销毁所有缓存的单例bean.
	 * <p>Note: 不在父级上下文上调用{@code close}; 父级上下文有自己独立的生命周期.
	 * <p>可以多次调用此方法, 而不会产生副作用: 对已经关闭的上下文的后续{@code close}调用将被忽略.
	 */
	@Override
	void close();

	/**
	 * 确定此应用程序上下文是否处于活动状态, 即是否已至少刷新一次并且尚未关闭.
	 * 
	 * @return 上下文是否仍然有效
	 */
	boolean isActive();

	/**
	 * 返回此应用程序上下文的内部bean工厂.
	 * 可用于访问底层工厂的特定功能.
	 * <p>Note: 不要使用它来后处理bean工厂; 单例之前已经被实例化了.
	 * 在触摸bean之前, 使用BeanFactoryPostProcessor拦截BeanFactory设置过程.
	 * <p>通常, 只有在上下文处于活动状态时, 即{@link #refresh()}和{@link #close()}之间, 才能访问此内部工厂.
	 * {@link #isActive()}可用于检查上下文是否处于适当的状态.
	 * 
	 * @return 底层bean工厂
	 * @throws IllegalStateException 如果上下文不包含内部bean工厂 (通常如果尚未调用{@link #refresh()}; 或者已经调用 {@link #close()})
	 */
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
