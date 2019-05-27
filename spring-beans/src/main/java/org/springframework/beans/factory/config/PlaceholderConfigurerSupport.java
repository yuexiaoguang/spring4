package org.springframework.beans.factory.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.StringValueResolver;

/**
 * 属性资源配置器的抽象基类, 用于解析bean定义属性值中的占位符.
 * 实现将值从属性文件或其他{@linkplain org.springframework.core.env.PropertySource属性源}拉入bean定义.
 *
 * <p>默认占位符语法遵循 Ant / Log4J / JSP EL 样式:
 *
 * <pre class="code">${...}</pre>
 *
 * 示例XML bean定义:
 *
 * <pre class="code">
 * <bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource"/>
 *   <property name="driverClassName" value="${driver}"/>
 *   <property name="url" value="jdbc:${dbname}"/>
 * </bean>
 * </pre>
 *
 * 示例属性文件:
 *
 * <pre class="code">driver=com.mysql.jdbc.Driver
 * dbname=mysql:mydb</pre>
 *
 * 被注解的bean定义可以使用 {@link org.springframework.beans.factory.annotation.Value @Value}注解来利用属性替换:
 *
 * <pre class="code">@Value("${person.age}")</pre>
 *
 * 实现检查bean引用中的简单属性值, lists, maps, props, 和bean名称.
 * 此外, 占位符值还可以交叉引用其他占位符, 例如:
 *
 * <pre class="code">rootPath=myrootdir
 * subPath=${rootPath}/subdir</pre>
 *
 * 与{@link PropertyOverrideConfigurer}相反, 此类型的子类允许在bean定义中填充显式占位符.
 *
 * <p>如果配置程序无法解析占位符, 将抛出{@link BeanDefinitionStoreException}.
 * 如果要检查多个属性文件, 请通过 {@link #setLocations locations}属性指定多个资源.
 * 还可以定义多个配置器, 每个配置器都有自己的占位符语法.
 * 如果占位符无法解析, 请使用{@link #ignoreUnresolvablePlaceholders}故意抑制抛出异常.
 *
 * <p>可以通过{@link #setProperties properties}属性为每个配置器实例全局定义默认属性值,
 * 或者默认情况下, 在逐个属性的基础上使用默认值分隔符 {@code ":"}, 并通过{@link #setValueSeparator(String)}自定义.
 *
 * <p>具有默认值的XML属性示例:
 *
 * <pre class="code">
 *   <property name="url" value="jdbc:${dbname:defaultdb}"/>
 * </pre>
 */
public abstract class PlaceholderConfigurerSupport extends PropertyResourceConfigurer
		implements BeanNameAware, BeanFactoryAware {

	/** 默认占位符前缀: {@value} */
	public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

	/** 默认占位符后缀: {@value} */
	public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

	/** 默认值分隔符: {@value} */
	public static final String DEFAULT_VALUE_SEPARATOR = ":";


	/** Defaults to {@value #DEFAULT_PLACEHOLDER_PREFIX} */
	protected String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

	/** Defaults to {@value #DEFAULT_PLACEHOLDER_SUFFIX} */
	protected String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

	/** Defaults to {@value #DEFAULT_VALUE_SEPARATOR} */
	protected String valueSeparator = DEFAULT_VALUE_SEPARATOR;

	protected boolean trimValues = false;

	protected String nullValue;

	protected boolean ignoreUnresolvablePlaceholders = false;

	private String beanName;

	private BeanFactory beanFactory;


	/**
	 * 设置占位符字符串开头的前缀.
	 * 默认是 {@value #DEFAULT_PLACEHOLDER_PREFIX}.
	 */
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * 设置占位符字符串结尾的后缀.
	 * 默认是 {@value #DEFAULT_PLACEHOLDER_SUFFIX}.
	 */
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * 指定占位符变量和关联的默认值之间的分隔字符; 如果不应将此特殊字符作为值分隔符处理, 则指定{@code null}.
	 * 默认是 {@value #DEFAULT_VALUE_SEPARATOR}.
	 */
	public void setValueSeparator(String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * 在应用之前是否修剪已解析的值, 从开头和结尾删除多余的空格.
	 * <p>默认是{@code false}.
	 * @since 4.3
	 */
	public void setTrimValues(boolean trimValues) {
		this.trimValues = trimValues;
	}

	/**
	 * 设置一个值, 当作为占位符值解析时, 该值应被视为{@code null}: e.g. "" (empty String) or "null".
	 * <p>请注意, 这仅适用于完整属性值, 而不适用于连接值的部分.
	 * <p>默认情况下, 不定义此类空值. 这意味着除非在此处明确映射相应的值, 否则无法将{@code null}表示为属性值.
	 */
	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	/**
	 * 设置是否忽略不可解析的占位符.
	 * <p>默认 "false": 如果占位符无法解析, 则会抛出异常.
	 * 将此标志切换为 "true", 以便在这种情况下保留占位符字符串, 将其留给其他占位符配置器来解析它.
	 */
	public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}

	/**
	 * 只需检查是否正在解析我们自己的bean定义, 以避免在属性文件位置中无法解析的占位符的失败.
	 * 后一种情况可能发生在资源位置中的系统属性的占位符中.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 只需检查是否正在解析我们自己的bean定义, 以避免在属性文件位置中无法解析的占位符的失败.
	 * 后一种情况可能发生在资源位置中的系统属性的占位符中.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
			StringValueResolver valueResolver) {

		BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);

		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		for (String curName : beanNames) {
			// 检查是否正在解析自己的bean定义, 以避免在属性文件位置中无法解析的占位符失败.
			if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
				BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
				try {
					visitor.visitBeanDefinition(bd);
				}
				catch (Exception ex) {
					throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage(), ex);
				}
			}
		}

		// New in Spring 2.5: 解析别名目标名称和别名中的占位符.
		beanFactoryToProcess.resolveAliases(valueResolver);

		// New in Spring 3.0: 解析嵌入值中的占位符, 例如注解属性.
		beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
	}

}
