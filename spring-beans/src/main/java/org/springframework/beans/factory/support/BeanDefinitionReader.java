package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * bean定义读取器的简单接口.
 * 使用Resource和String位置参数指定加载方法.
 *
 * <p>具体的bean定义读取器可以为bean定义添加额外的load和register方法, 特定于bean定义格式.
 *
 * <p>请注意, bean定义读取器不必实现此接口.
 * 它仅作为想要遵循标准命名约定的bean定义读取器的建议.
 */
public interface BeanDefinitionReader {

	/**
	 * 返回bean工厂以注册bean定义.
	 * <p>工厂通过BeanDefinitionRegistry接口暴露出去, 封装与bean定义处理相关的方法.
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * 返回资源加载器以用于资源定位.
	 * 可以检查ResourcePatternResolver接口并进行相应的转换, 以便为给定的资源模式加载多个资源.
	 * <p>Null表明绝对资源加载不适用于此bean定义读取器.
	 * <p>这主要用于从bean定义资源中导入更多资源, 例如通过XML bean定义中的“import”标签.
	 * 但是, 建议相对于定义资源应用此类导入; 只有显式的完整资源位置才会触发绝对资源加载.
	 * <p>还有一个 {@code loadBeanDefinitions(String)}方法, 用于从资源位置(或位置模式)加载bean定义.
	 * 这是避免显式ResourceLoader处理的便利方法.
	 */
	ResourceLoader getResourceLoader();

	/**
	 * 返回用于bean类的类加载器.
	 * <p>{@code null} 建议不要实时地加载bean类, 而只是用类名注册bean定义, 相应的Classes稍后要解析(或者永远不会).
	 */
	ClassLoader getBeanClassLoader();

	/**
	 * 返回用于匿名bean的BeanNameGenerator (未指定显式bean名称).
	 */
	BeanNameGenerator getBeanNameGenerator();


	/**
	 * 从指定的资源加载bean定义.
	 * 
	 * @param resource 资源描述符
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

	/**
	 * 从指定的资源加载bean定义.
	 * 
	 * @param resources 资源描述符
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

	/**
	 * 从指定的资源位置加载bean定义.
	 * <p>该位置也可以是位置模式, 前提是此bean定义读取器的ResourceLoader是ResourcePatternResolver.
	 * 
	 * @param location 要使用此bean定义读取器的ResourceLoader(或ResourcePatternResolver)加载的资源位置
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

	/**
	 * 从指定的资源位置加载bean定义.
	 * 
	 * @param locations 要使用此bean定义读取器的ResourceLoader(或ResourcePatternResolver)加载的资源位置
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
