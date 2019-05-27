package org.springframework.context.support;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * {@link org.springframework.context.ApplicationContext}实现,
 * 扩展了{@link GenericApplicationContext}, 并且实现了 {@link GroovyObject},
 * 这样可以使用dot de-reference语法检索bean, 而不是使用{@link #getBean}.
 *
 * <p>等效于Groovy bean定义的{@link GenericXmlApplicationContext},
 * 甚至是其升级版, 因为它也无缝地理解XML bean定义文件.
 * 主要区别在于, 在Groovy脚本中, 上下文可以与内联bean定义闭包一起使用, 如下所示:
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * def context = new GenericGroovyApplicationContext()
 * context.reader.beans {
 *     dataSource(BasicDataSource) {                  // <--- invokeMethod
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"                            // <-- setProperty
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource                    // <-- getProperty for retrieving references
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean ->         // <-- setProperty with closure for nested bean
 *             dataSource = dataSource
 *         }
 *     }
 * }
 * context.refresh()
 * </pre>
 *
 * <p>或者, 从外部资源加载如下所示的Groovy bean定义脚本 (e.g. an "applicationContext.groovy"文件):
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * beans {
 *     dataSource(BasicDataSource) {
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean ->
 *             dataSource = dataSource
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>使用以下Java代码创建 {@code GenericGroovyApplicationContext} (可能使用Ant风格的 '*'/'**' 位置模式):
 *
 * <pre class="code">
 * GenericGroovyApplicationContext context = new GenericGroovyApplicationContext();
 * context.load("org/myapp/applicationContext.groovy");
 * context.refresh();
 * </pre>
 *
 * <p>或者更简洁, 只要不需要额外的配置:
 *
 * <pre class="code">
 * ApplicationContext context = new GenericGroovyApplicationContext("org/myapp/applicationContext.groovy");
 * </pre>
 *
 * <p><b>此应用程序上下文还理解XML bean定义文件, 允许与Groovy bean定义文件无缝混合和匹配.</b>
 * ".xml" 文件将被解析为XML内容; 所有其他类型的资源将被解析为Groovy脚本.
 */
public class GenericGroovyApplicationContext extends GenericApplicationContext implements GroovyObject {

	private final GroovyBeanDefinitionReader reader = new GroovyBeanDefinitionReader(this);

	private final BeanWrapper contextWrapper = new BeanWrapperImpl(this);

    private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());


	/**
	 * 创建一个需要{@link #load loaded}, 然后手动{@link #refresh刷新}的新的GenericGroovyApplicationContext.
	 */
	public GenericGroovyApplicationContext() {
	}

	/**
	 * 从给定的资源加载bean定义并自动刷新上下文.
	 * 
	 * @param resources 要加载的资源
	 */
	public GenericGroovyApplicationContext(Resource... resources) {
		load(resources);
		refresh();
	}

	/**
	 * 从给定的资源位置加载bean定义并自动刷新上下文.
	 * 
	 * @param resourceLocations 要加载的资源
	 */
	public GenericGroovyApplicationContext(String... resourceLocations) {
		load(resourceLocations);
		refresh();
	}

	/**
	 * 从给定的资源位置加载bean定义并自动刷新上下文.
	 * 
	 * @param relativeClass 加载每个指定资源名称时, 其包将用作前缀的类
	 * @param resourceNames 要加载的资源的相对合格的名称
	 */
	public GenericGroovyApplicationContext(Class<?> relativeClass, String... resourceNames) {
		load(relativeClass, resourceNames);
		refresh();
	}


	/**
	 * 暴露底层的 {@link GroovyBeanDefinitionReader}, 以便访问它上面的 {@code loadBeanDefinition}方法
	 * 和指定的内联Groovy bean定义闭包.
	 */
	public final GroovyBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * 将给定环境委托给底层 {@link GroovyBeanDefinitionReader}.
	 * 应该在调用 {@code #load}之前调用.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(getEnvironment());
	}

	/**
	 * 从给定的Groovy脚本或XML文件加载bean定义.
	 * <p>请注意, ".xml" 文件将被解析为XML内容; 所有其他类型的资源将被解析为Groovy脚本.
	 * 
	 * @param resources 要加载的一个或多个资源
	 */
	public void load(Resource... resources) {
		this.reader.loadBeanDefinitions(resources);
	}

	/**
	 * 从给定的Groovy脚本或XML文件加载bean定义.
	 * <p>请注意, ".xml" 文件将被解析为XML内容; 所有其他类型的资源将被解析为Groovy脚本.
	 * 
	 * @param resourceLocations 要加载的一个或多个资源位置
	 */
	public void load(String... resourceLocations) {
		this.reader.loadBeanDefinitions(resourceLocations);
	}

	/**
	 * 从给定的Groovy脚本或XML文件加载bean定义.
	 * <p>请注意, ".xml" 文件将被解析为XML内容; 所有其他类型的资源将被解析为Groovy脚本.
	 * 
	 * @param relativeClass 加载每个指定资源名称时, 其包将用作前缀的类
	 * @param resourceNames 要加载的资源的相对合格的名称
	 */
	public void load(Class<?> relativeClass, String... resourceNames) {
		Resource[] resources = new Resource[resourceNames.length];
		for (int i = 0; i < resourceNames.length; i++) {
			resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
		}
		load(resources);
	}


	// Implementation of the GroovyObject interface

	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

    public MetaClass getMetaClass() {
		return this.metaClass;
	}

	public Object invokeMethod(String name, Object args) {
		return this.metaClass.invokeMethod(this, name, args);
	}

	public void setProperty(String property, Object newValue) {
		if (newValue instanceof BeanDefinition) {
			registerBeanDefinition(property, (BeanDefinition) newValue);
		}
		else {
			this.metaClass.setProperty(this, property, newValue);
		}
	}

    public Object getProperty(String property) {
		if (containsBean(property)) {
			return getBean(property);
		}
		else if (this.contextWrapper.isReadableProperty(property)) {
			return this.contextWrapper.getPropertyValue(property);
		}
		throw new NoSuchBeanDefinitionException(property);
	}

}
