package org.springframework.beans.factory.config;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.StringValueResolver;

/**
 * 由大多数bean工厂实现的配置接口.
 * 提供配置Bean工厂的工具, 除了{@link org.springframework.beans.factory.BeanFactory}接口中的bean工厂客户端方法.
 *
 * <p>此bean工厂接口不应在常规应用程序代码中使用:
 * 使用{@link org.springframework.beans.factory.BeanFactory} 
 * 或{@link org.springframework.beans.factory.ListableBeanFactory}以满足一般需求.
 * 这个扩展接口只是为了允许框架内部的即插即用和对bean工厂配置方法的特殊访问.
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

	/**
	 * 标准单例范围的范围标识符: "singleton".
	 * 可以通过{@code registerScope}添加自定义范围.
	 */
	String SCOPE_SINGLETON = "singleton";

	/**
	 * 标准原型范围的范围标识符: "prototype".
	 * 可以通过{@code registerScope}添加自定义范围.
	 */
	String SCOPE_PROTOTYPE = "prototype";


	/**
	 * 设置此bean工厂的父级.
	 * <p>请注意, 父级无法更改: 如果它在工厂实例化时不可用, 则只应在构造函数外部设置.
	 * 
	 * @param parentBeanFactory 父级BeanFactory
	 * 
	 * @throws IllegalStateException 如果此工厂已与父级BeanFactory关联
	 */
	void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

	/**
	 * 设置类加载器以用于加载bean类. 默认是线程上下文类加载器.
	 * <p>请注意, 此类加载器仅适用于尚未带有已解析的bean类的bean定义.
	 * 默认情况下, 就像Spring 2.0一样: Bean定义只带有bean类名, 一旦工厂处理bean定义就解析了.
	 * 
	 * @param beanClassLoader 要使用的类加载器, 或{@code null}默认的类加载器
	 */
	void setBeanClassLoader(ClassLoader beanClassLoader);

	/**
	 * 返回此工厂的类加载器以加载bean类.
	 */
	ClassLoader getBeanClassLoader();

	/**
	 * 指定用于类型匹配目的的临时ClassLoader.
	 * 默认为none, 只使用标准bean ClassLoader.
	 * <p>如果涉及<i>load-time weaving</i>, 通常只指定临时ClassLoader, 确保尽可能延迟地加载实际的bean类.
	 * 一旦BeanFactory完成其引导阶段, 就会删除临时加载器.
	 */
	void setTempClassLoader(ClassLoader tempClassLoader);

	/**
	 * 返回临时ClassLoader以用于类型匹配目的.
	 */
	ClassLoader getTempClassLoader();

	/**
	 * 设置是否缓存bean元数据, 例如给定的bean定义(以合并方式)和已解析的bean类.
	 * 默认true.
	 * <p>关闭此标志以启用bean定义对象的热刷新, 特别是bean类.
	 * 如果此标志关闭, 则任何bean实例的创建都将为了新解析的类重新查询bean类加载器.
	 */
	void setCacheBeanMetadata(boolean cacheBeanMetadata);

	/**
	 * 返回是否缓存bean元数据, 例如给定的bean定义(以合并方式)和已解析的bean类.
	 */
	boolean isCacheBeanMetadata();

	/**
	 * 为bean定义值中的表达式指定解析策略.
	 * <p>默认情况下, BeanFactory中没有活动表达式支持.
	 * ApplicationContext通常会在此处设置标准表达式策略, 支持Unified EL兼容样式中的 "#{...}"表达式.
	 */
	void setBeanExpressionResolver(BeanExpressionResolver resolver);

	/**
	 * 返回bean定义值中表达式的解析策略.
	 */
	BeanExpressionResolver getBeanExpressionResolver();

	/**
	 * 指定用于转换属性值的Spring 3.0 ConversionService, 作为JavaBeans PropertyEditors的替代方法.
	 */
	void setConversionService(ConversionService conversionService);

	/**
	 * 返回关联的ConversionService.
	 */
	ConversionService getConversionService();

	/**
	 * 添加PropertyEditorRegistrar以应用于所有bean创建过程.
	 * <p>这样的注册器创建新的PropertyEditor实例, 并在给定的注册表上注册它们, 每个bean创建尝试都是新的.
	 * 避免了在自定义编辑器上进行同步的需要; 因此, 通常最好使用此方法代替{@link #registerCustomEditor}.
	 * 
	 * @param registrar 要注册的PropertyEditorRegistrar
	 */
	void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

	/**
	 * 为给定类型的所有属性注册给定的自定义属性编辑器. 在工厂配置期间调用.
	 * <p>请注意, 此方法将注册共享的自定义编辑器实例;
	 * 为了线程安全, 将同步对该实例的访问.
	 * 通常最好使用{@link #addPropertyEditorRegistrar} 代替此方法, 避免在自定义编辑器上进行同步.
	 * 
	 * @param requiredType 属性的类型
	 * @param propertyEditorClass 要注册的{@link PropertyEditor}类
	 */
	void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

	/**
	 * 使用已在此BeanFactory中注册的自定义编辑器, 初始化给定的PropertyEditorRegistry.
	 * 
	 * @param registry 要初始化的PropertyEditorRegistry
	 */
	void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

	/**
	 * 设置一个自定义类型转换器, 该BeanFactory使用它转换bean属性值, 构造函数参数值等.
	 * <p>这将覆盖默认的PropertyEditor机制, 因此使任何自定义编辑器或自定义编辑器注册表都无关紧要.
	 */
	void setTypeConverter(TypeConverter typeConverter);

	/**
	 * 获取此BeanFactory使用的类型转换器. 这可能每次调用都返回新实例, 因为TypeConverters通常不是线程安全的.
	 * <p>如果默认的PropertyEditor机制处于活动状态, 返回的TypeConverter将知道已注册的所有自定义编辑器.
	 */
	TypeConverter getTypeConverter();

	/**
	 * 为嵌入值（例如注解属性）添加String解析器.
	 * 
	 * @param valueResolver 要应用于嵌入值的String解析器
	 */
	void addEmbeddedValueResolver(StringValueResolver valueResolver);

	/**
	 * 确定是否已向此Bean工厂注册了嵌入值解析器, 通过{@link #resolveEmbeddedValue(String)}应用.
	 */
	boolean hasEmbeddedValueResolver();

	/**
	 * 解析给定的嵌入值, e.g. 一个注解属性.
	 * 
	 * @param value 要解析的值
	 * 
	 * @return 解析后的值 (可能是原始值)
	 */
	String resolveEmbeddedValue(String value);

	/**
	 * 添加一个新的BeanPostProcessor, 它将应用于此工厂创建的bean. 在工厂配置期间调用.
	 * <p>Note: 此处提交的后处理程序将按注册顺序应用;
	 * 过实现{@link org.springframework.core.Ordered}接口表达的排序将被忽略.
	 * 请注意, 自动检测的后处理器(例如, 作为ApplicationContext中的bean)将始终在以编程方式注册后应用.
	 * 
	 * @param beanPostProcessor 要注册的后处理器
	 */
	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	/**
	 * 返回已注册的BeanPostProcessors的当前数量.
	 */
	int getBeanPostProcessorCount();

	/**
	 * 注册给定的范围, 由给定的Scope实现支持.
	 * 
	 * @param scopeName 范围标识符
	 * @param scope Scope的实现
	 */
	void registerScope(String scopeName, Scope scope);

	/**
	 * 返回所有当前注册的范围的名称.
	 * <p>这只会返回显式注册的范围的名称. “单例”和“原型”等内置范围不会公开.
	 * 
	 * @return 范围名称数组, 或空数组
	 */
	String[] getRegisteredScopeNames();

	/**
	 * 返回给定范围名称的Scope实现.
	 * <p>这只会返回显式注册的范围的名称. “单例”和“原型”等内置范围不会公开.
	 * 
	 * @param scopeName 范围的名称
	 * 
	 * @return 注册的Scope实现, 或{@code null}
	 */
	Scope getRegisteredScope(String scopeName);

	/**
	 * 提供与此工厂相关的安全访问控制上下文.
	 * 
	 * @return 适用的AccessControlContext(never {@code null})
	 */
	AccessControlContext getAccessControlContext();

	/**
	 * 复制给定的其他工厂中的所有相关配置.
	 * <p>包括所有标准配置设置, 以及BeanPostProcessors, Scopes和工厂特定的内部设置.
	 * 不应包含实际bean定义的任何元数据, 例如BeanDefinition对象和bean名称别名.
	 * 
	 * @param otherFactory 要从中复制的另一个BeanFactory
	 */
	void copyConfigurationFrom(ConfigurableBeanFactory otherFactory);

	/**
	 * 给定bean名称, 创建别名. 通常使用此方法来支持XML ID中非法的名称 (用于bean名称).
	 * <p>通常在工厂配置期间调用, 但也可用于别名的运行时注册.
	 * 因此, 工厂实现应该同步别名访问.
	 * 
	 * @param beanName 目标bean的规范名称
	 * @param alias 要为bean注册的别名
	 * 
	 * @throws BeanDefinitionStoreException 如果别名已被使用
	 */
	void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException;

	/**
	 * 解析在此工厂中注册的所有别名目标名称和别名, 将给定的StringValueResolver应用于它们.
	 * <p>例如, 值解析器可以解析目标bean名称中的占位符, 甚至可以解析别名中的占位符.
	 * 
	 * @param valueResolver 要应用的StringValueResolver
	 */
	void resolveAliases(StringValueResolver valueResolver);

	/**
	 * 返回给定bean名称的合并BeanDefinition, 如有必要, 将子bean定义与其父项合并.
	 * 也考虑祖先工厂中的bean定义.
	 * 
	 * @param beanName 为了合并定义要检索的bean的名称
	 * 
	 * @return 给定bean的(可能合并的)BeanDefinition
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean定义
	 */
	BeanDefinition getMergedBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的bean是否为FactoryBean.
	 * 
	 * @param name 要检查的bean的名称
	 * 
	 * @return bean是否是FactoryBean ({@code false} 表示bean存在, 但不是FactoryBean)
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 */
	boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 显式控制指定bean的当前创建状态.
	 * 仅限容器内部使用.
	 * 
	 * @param beanName bean的名称
	 * @param inCreation bean是否正在创建中
	 */
	void setCurrentlyInCreation(String beanName, boolean inCreation);

	/**
	 * 确定指定的bean当前是否正在创建.
	 * 
	 * @param beanName bean的名称
	 * 
	 * @return bean是否正在创建中
	 */
	boolean isCurrentlyInCreation(String beanName);

	/**
	 * 为给定的bean注册一个依赖bean, 在给定的bean被销毁之前被销毁.
	 * 
	 * @param beanName bean的名称
	 * @param dependentBeanName 依赖的bean的名称
	 */
	void registerDependentBean(String beanName, String dependentBeanName);

	/**
	 * 返回依赖于指定bean的所有bean的名称.
	 * 
	 * @param beanName bean的名称
	 * 
	 * @return 依赖的bean名称的数组, 或空数组
	 */
	String[] getDependentBeans(String beanName);

	/**
	 * 返回指定bean所依赖的所有bean的名称.
	 * 
	 * @param beanName bean的名称
	 * 
	 * @return bean所依赖的bean名称数组, 或空数组
	 */
	String[] getDependenciesForBean(String beanName);

	/**
	 * 根据bean的定义, 销毁给定的bean实例（通常是从这个工厂获得的原型实例）.
	 * <p>应该捕获并记录在销毁期间出现的任何异常, 而不是传播给此方法的调用方.
	 * 
	 * @param beanName bean定义的名称
	 * @param beanInstance 要销毁的bean实例
	 */
	void destroyBean(String beanName, Object beanInstance);

	/**
	 * 销毁当前目标作用域中指定的作用域bean.
	 * <p>应该捕获并记录在销毁期间出现的任何异常, 而不是传播给此方法的调用方.
	 * 
	 * @param beanName 作用域bean的名称
	 */
	void destroyScopedBean(String beanName);

	/**
	 * 销毁这个工厂里的所有单例bean, 包括已注册为已处理的内部bean.
	 * 在关闭工厂时被调用.
	 * <p>应该捕获并记录在销毁期间出现的任何异常, 而不是传播给此方法的调用方.
	 */
	void destroySingletons();

}
