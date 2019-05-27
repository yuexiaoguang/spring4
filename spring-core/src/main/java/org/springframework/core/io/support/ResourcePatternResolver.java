package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 用于将位置模式 (例如, Ant样式路径模式)解析为Resource对象的策略接口.
 *
 * <p>这是{@link org.springframework.core.io.ResourceLoader}接口的扩展.
 * 可以检查传入的ResourceLoader
 * (例如, 在上下文中运行时, 通过{@link org.springframework.context.ResourceLoaderAware}
 * 传入{@link org.springframework.context.ApplicationContext})
 * 是否也实现了这个扩展接口.
 *
 * <p>{@link PathMatchingResourcePatternResolver}是一个独立的实现, 可以在ApplicationContext之外使用,
 * 也可以由{@link ResourceArrayPropertyEditor}用于填充Resource数组bean属性.
 *
 * <p>可以与任何类型的位置模式一起使用 (e.g. "/WEB-INF/*-context.xml"):
 * 输入模式必须与策略实现相匹配. 此接口仅指定转换方法, 而不是特定的模式格式.
 *
 * <p>此接口还为类路径中的所有匹配资源建议新的资源前缀 "classpath*:".
 * 请注意, 在这种情况下, 资源位置应该是没有占位符的路径 (e.g. "/beans.xml");
 * JAR文件或类目录可以包含多个同名文件.
 */
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * 来自类路径的所有匹配资源的伪URL前缀: "classpath*:"
	 * 这与ResourceLoader的类路径URL前缀的不同之处在于, 它检索给定名称的所有匹配资源 (e.g. "/beans.xml"),
	 * 例如在所有已部署的JAR文件的根目录中.
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * 将给定的位置模式解析为Resource对象.
	 * <p>应尽可能避免重叠指向相同物理资源的资源条目.
	 * 结果应该设置语义.
	 * 
	 * @param locationPattern 要解析的位置模式
	 * 
	 * @return 相应的Resource对象
	 * @throws IOException 发生I/O错误
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
