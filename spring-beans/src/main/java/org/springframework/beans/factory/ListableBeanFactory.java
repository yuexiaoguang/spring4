package org.springframework.beans.factory;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;

/**
 * {@link BeanFactory}接口的扩展, 由bean工厂实现, 可以枚举其所有bean实例,
 * 而不是按客户要求逐个尝试按名称查找bean.
 * 预加载所有bean定义（例如基于XML的工厂）的BeanFactory实现可以实现此接口.
 *
 * <p>如果这是一个 {@link HierarchicalBeanFactory}, 返回值不会考虑BeanFactory的层次结构,
 * 仅涉及当前工厂中定义的bean.
 * 使用{@link BeanFactoryUtils}帮助类来考虑祖先工厂中的bean.
 *
 * <p>此接口中的方法将仅考虑此工厂中的bean定义.
 * 他们将忽略任何通过其他方式注册的单例bean,
 * 例如{@link org.springframework.beans.factory.config.ConfigurableBeanFactory}的 {@code registerSingleton}方法,
 * 除了检查这些手动注册的单例的{@code getBeanNamesOfType}和{@code getBeansOfType}之外.
 * 当然, BeanFactory的{@code getBean}确实允许透明访问这些特殊的bean.
 * 但是, 通常情况下, 所有bean都将由外部bean定义来定义, 所以大多数应用程序不需要担心这种差异.
 *
 * <p><b>NOTE:</b> 除了{@code getBeanDefinitionCount}和{@code containsBeanDefinition}之外,
 * 此接口中的方法不是为频繁调用而设计的. 实现可能很慢.
 */
public interface ListableBeanFactory extends BeanFactory {

	/**
	 * 检查此bean工厂是否包含具有给定名称的bean定义.
	 * <p>不考虑该工厂可能参与的任何层级, 并忽略任何通过bean定义以外的方式注册的单例bean.
	 * 
	 * @param beanName 要查找的bean的名称
	 * 
	 * @return 如果此bean工厂包含具有给定名称的bean定义
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回工厂中定义的bean数量.
	 * <p>不考虑该工厂可能参与的任何层级, 并忽略任何通过bean定义以外的方式注册的单例bean.
	 * 
	 * @return 工厂中定义的bean数量
	 */
	int getBeanDefinitionCount();

	/**
	 * 返回此工厂中定义的所有bean的名称.
	 * <p>不考虑该工厂可能参与的任何层级, 并忽略任何通过bean定义以外的方式注册的单例bean.
	 * 
	 * @return 此工厂中定义的所有bean的名称, 或空数组
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回与给定类型匹配的bean的名称 (包括子类),
	 * 在FactoryBeans的情况下, 从bean定义或{@code getObjectType}的值来判断.
	 * <p><b>NOTE: 此方法仅对顶级bean进行反射.</b> 它不会检查可能与指定类型匹配的嵌套bean.
	 * <p>考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * <p>不考虑该工厂可能参与的任何层级.
	 * 使用BeanFactoryUtils的 {@code beanNamesForTypeIncludingAncestors}将包括在祖先工厂中的bean.
	 * <p>Note: 不要忽略通过bean定义以外的其他方式注册的单例bean.
	 * <p>此版本的{@code getBeanNamesForType}匹配所有类型的bean, 无论是单例, 原型, 还是FactoryBeans.
	 * 在大多数实现中, 结果和 {@code getBeanNamesForType(type, true, true)}相同.
	 * <p>此方法返回的Bean名称应始终尽可能在后端配置中以定义 的顺序返回bean名称 .
	 * 
	 * @param type 要匹配的类或接口, 或 {@code null}匹配所有bean名称
	 * 
	 * @return 与给定对象类型匹配的bean（或FactoryBeans创建的对象）的名称（包括子类）, 或空数组
	 */
	String[] getBeanNamesForType(ResolvableType type);

	/**
	 * 返回与给定类型匹配的bean的名称 (包括子类),
	 * 在FactoryBeans的情况下, 从bean定义或{@code getObjectType}的值来判断.
	 * <p><b>NOTE: 此方法仅对顶级bean进行反射.</b> 它不会检查可能与指定类型匹配的嵌套bean.
	 * <p>考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * <p>不考虑该工厂可能参与的任何层级.
	 * 使用BeanFactoryUtils的 {@code beanNamesForTypeIncludingAncestors}将包括在祖先工厂中的bean.
	 * <p>Note: 不要忽略通过bean定义以外的其他方式注册的单例bean.
	 * <p>此版本的{@code getBeanNamesForType}匹配所有类型的bean, 无论是单例, 原型, 还是FactoryBeans.
	 * 在大多数实现中, 结果和 {@code getBeanNamesForType(type, true, true)}相同.
	 * <p>此方法返回的Bean名称应始终尽可能在后端配置中以定义 的顺序返回bean名称 .
	 * 
	 * @param type 要匹配的类或接口, 或 {@code null}匹配所有bean名称
	 * 
	 * @return 与给定对象类型匹配的bean（或FactoryBeans创建的对象）的名称（包括子类）, 或空数组
	 */
	String[] getBeanNamesForType(Class<?> type);

	/**
	 * 返回与给定类型匹配的bean的名称 (包括子类),
	 * 在FactoryBeans的情况下, 从bean定义或{@code getObjectType}的值来判断.
	 * <p><b>NOTE: 此方法仅对顶级bean进行反射.</b> 它不会检查可能与指定类型匹配的嵌套bean.
	 * <p>如果设置了“allowEagerInit”标志, 是否考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * 如果未设置“allowEagerInit”, 只检查原始FactoryBeans (这不需要每个FactoryBean都初始化).
	 * <p>不考虑该工厂可能参与的任何层级.
	 * 使用BeanFactoryUtils的 {@code beanNamesForTypeIncludingAncestors}将包括在祖先工厂中的bean.
	 * <p>Note: 不要忽略通过bean定义以外的其他方式注册的单例bean.
	 * <p>此方法返回的Bean名称应始终尽可能在后端配置中以定义 的顺序返回bean名称 .
	 * 
	 * @param type 要匹配的类或接口, 或 {@code null}匹配所有bean名称
	 * @param includeNonSingletons 是否包括原型或范围Bean或只是单例 (也适用于FactoryBeans)
	 * @param allowEagerInit 是否初始化<i>lazy-init单例</i>和<i>由FactoryBeans创建的对象</i>（或通过带有“factory-bean”引用的工厂方法）进行类型检查.
	 * 请注意, 需要实时地初始化FactoryBeans以确定其类型:
	 * 因此请注意, 为此标志传入“true”将初始化FactoryBeans和“factory-bean”引用.
	 * 
	 * @return 与给定对象类型匹配的bean（或FactoryBeans创建的对象）的名称（包括子类）, 或空数组
	 */
	String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与给定类型匹配的bean的名称 (包括子类),
	 * 在FactoryBeans的情况下, 从bean定义或{@code getObjectType}的值来判断.
	 * <p><b>NOTE: 此方法仅对顶级bean进行反射.</b> 它不会检查可能与指定类型匹配的嵌套bean.
	 * <p>考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * <p>不考虑该工厂可能参与的任何层级.
	 * 使用BeanFactoryUtils的 {@code beansOfTypeIncludingAncestors}将包括在祖先工厂中的bean.
	 * <p>Note: 不要忽略通过bean定义以外的其他方式注册的单例bean.
	 * <p>此版本的{@code getBeansOfType}匹配所有类型的bean, 无论是单例, 原型, 还是FactoryBeans.
	 * 在大多数实现中, 结果和 {@code getBeansOfType(type, true, true)}相同.
	 * <p>此方法返回的Map应始终尽可能在后端配置中按定义顺序返回bean名称和相应的bean实例.
	 * 
	 * @param type 要匹配的类或接口, 或 {@code null}匹配所有bean名称
	 * 
	 * @return 包含匹配的bean的Map, bean名称作为键, 相应的bean实例作为值
	 * @throws BeansException 如果无法创建bean
	 */
	<T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException;

	/**
	 * 返回与给定类型匹配的bean的名称 (包括子类),
	 * 在FactoryBeans的情况下, 从bean定义或{@code getObjectType}的值来判断.
	 * <p><b>NOTE: 此方法仅对顶级bean进行反射.</b> 它不会检查可能与指定类型匹配的嵌套bean.
	 * <p>如果设置了“allowEagerInit”标志, 是否考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * 如果未设置“allowEagerInit”, 只检查原始FactoryBeans (这不需要每个FactoryBean都初始化).
	 * <p>不考虑该工厂可能参与的任何层级.
	 * 使用BeanFactoryUtils的 {@code beansOfTypeIncludingAncestors}将包括在祖先工厂中的bean.
	 * <p>Note: 不要忽略通过bean定义以外的其他方式注册的单例bean.
	 * <p>此方法返回的Map应始终尽可能在后端配置中按定义顺序返回bean名称和相应的bean实例.
	 * 
	 * @param type 要匹配的类或接口, 或 {@code null}匹配所有bean名称
	 * @param includeNonSingletons 是否包括原型或范围Bean或只是单例 (也适用于FactoryBeans)
	 * @param allowEagerInit 是否初始化<i>lazy-init单例</i>和<i>由FactoryBeans创建的对象</i>（或通过带有“factory-bean”引用的工厂方法）进行类型检查.
	 * 请注意, 需要实时地初始化FactoryBeans以确定其类型:
	 * 因此请注意, 为此标志传入“true”将初始化FactoryBeans和“factory-bean”引用.
	 * 
	 * @return 包含匹配的bean的Map, bean名称作为键, 相应的bean实例作为值
	 * @throws BeansException 如果无法创建bean
	 */
	<T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;

	/**
	 * 查找{@code Class}具有提供的{@link Annotation}类型的所有bean的名称, 没有创建任何bean实例.
	 * 
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 所有匹配的bean的名称
	 */
	String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

	/**
	 * 查找{@code Class}具有提供的{@link Annotation}类型的所有bean.
	 * 
	 * @param annotationType 要查找的注解类型
	 * 
	 * @return 包含匹配的bean的Map, bean名称作为键, 相应的bean实例作为值
	 * @throws BeansException 如果无法创建bean
	 */
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

	/**
	 * 在指定的bean上找到{@code annotationType}的{@link Annotation}, 如果在给定的类本身上找不到注解, 则遍历其接口和超类.
	 * 
	 * @param beanName 要查找注解的bean的名称
	 * @param annotationType 要查找的注解类
	 * 
	 * @return 给定类型的注解, 或 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 */
	<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException;

}
