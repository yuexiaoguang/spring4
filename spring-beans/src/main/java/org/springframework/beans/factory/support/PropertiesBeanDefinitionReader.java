package org.springframework.beans.factory.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

/**
 * 用于简单的属性格式的Bean定义读取器.
 *
 * <p>为Map/Properties和ResourceBundle提供bean定义注册方法. 通常应用于 DefaultListableBeanFactory.
 *
 * <p><b>例子:</b>
 *
 * <pre class="code">
 * employee.(class)=MyClass       // bean是MyClass类
 * employee.(abstract)=true       // 这个bean无法直接实例化
 * employee.group=Insurance       // 真实的属性
 * employee.usesDialUp=false      // 真实的属性 (可能被覆盖)
 *
 * salesrep.(parent)=employee     // 派生自 “employee” bean定义
 * salesrep.(lazy-init)=true      // 延迟初始化这个单例bean
 * salesrep.manager(ref)=tony     // 引用另一个bean
 * salesrep.department=Sales      // 真实的属性
 *
 * techie.(parent)=employee       // 派生自 “employee” bean定义
 * techie.(scope)=prototype       // bean是原型(不是共享实例)
 * techie.manager(ref)=jeff       // 引用另一个bean
 * techie.department=Engineering  // 真实的属性
 * techie.usesDialUp=true         // 真实的属性 (覆盖父级的值)
 *
 * ceo.$0(ref)=secretary          // 注入 'secretary' bean作为第0个构造函数参数
 * ceo.$1=1000000                 // 注入 值 '1000000' 作为第1个构造函数参数
 * </pre>
 */
public class PropertiesBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * 表示true的T/F属性的值.
	 * 其他任何东西都是 false. Case seNsItive.
	 */
	public static final String TRUE_VALUE = "true";

	/**
	 * bean名称和属性名称之间的分隔符.
	 * 遵循常规的Java约定.
	 */
	public static final String SEPARATOR = ".";

	/**
	 * 特殊的key来区分 {@code owner.(class)=com.myapp.MyClass}-
	 */
	public static final String CLASS_KEY = "(class)";

	/**
	 * 特殊的key来区分 {@code owner.(parent)=parentBeanName}.
	 */
	public static final String PARENT_KEY = "(parent)";

	/**
	 * 特殊的key来区分 {@code owner.(scope)=prototype}.
	 * 默认 "true".
	 */
	public static final String SCOPE_KEY = "(scope)";

	/**
	 * 特殊的key来区分 {@code owner.(singleton)=false}.
	 * 默认 "true".
	 */
	public static final String SINGLETON_KEY = "(singleton)";

	/**
	 * 特殊的key来区分 {@code owner.(abstract)=true}
	 * 默认 "false".
	 */
	public static final String ABSTRACT_KEY = "(abstract)";

	/**
	 * 特殊的key来区分 {@code owner.(lazy-init)=true}
	 * 默认 "false".
	 */
	public static final String LAZY_INIT_KEY = "(lazy-init)";

	/**
	 * 当前BeanFactory中对其他bean的引用的属性后缀: e.g. {@code owner.dog(ref)=fido}.
	 * 这是对单例还是原型的引用, 将取决于目标bean的定义.
	 */
	public static final String REF_SUFFIX = "(ref)";

	/**
	 * 在引用其他bean的值之前的前缀.
	 */
	public static final String REF_PREFIX = "*";

	/**
	 * 用于表示构造函数参数定义的前缀.
	 */
	public static final String CONSTRUCTOR_ARG_PREFIX = "$";


	private String defaultParentBean;

	private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();


	/**
	 * @param registry 以BeanDefinitionRegistry的形式加载bean定义的BeanFactory
	 */
	public PropertiesBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}


	/**
	 * 设置此Bean工厂的默认父级bean.
	 * 如果此工厂处理的子级bean定义, 既未提供父属性, 也未提供class属性, 则使用此默认值.
	 * <p>可以用于视图定义文件, 定义具有默认视图类的父级和所有视图的公共属性.
	 * 定义自己的父级或携带自己的类的视图定义仍然可以覆盖它.
	 * <p>严格地说, 默认父级设置不适用于带有类的bean定义的规则, 是出于向后兼容性的原因.
	 * 它仍然符合典型的用例.
	 */
	public void setDefaultParentBean(String defaultParentBean) {
		this.defaultParentBean = defaultParentBean;
	}

	/**
	 * 返回此bean工厂的默认父级bean.
	 */
	public String getDefaultParentBean() {
		return this.defaultParentBean;
	}

	/**
	 * 设置用于解析属性文件的PropertiesPersister.
	 * 默认 DefaultPropertiesPersister.
	 */
	public void setPropertiesPersister(PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : new DefaultPropertiesPersister());
	}

	/**
	 * 返回用于解析属性文件的PropertiesPersister.
	 */
	public PropertiesPersister getPropertiesPersister() {
		return this.propertiesPersister;
	}


	/**
	 * 使用所有属性Key, 从指定的属性文件加载bean定义 (i.e. 不按前缀过滤).
	 * 
	 * @param resource 属性文件的资源描述符
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource), null);
	}

	/**
	 * 从指定的属性文件加载bean定义.
	 * 
	 * @param resource 属性文件的资源描述符
	 * @param prefix Map中Key的过滤器: e.g. 'beans.' (can be empty or {@code null})
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(Resource resource, String prefix) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource), prefix);
	}

	/**
	 * 从指定的属性文件加载bean定义.
	 * 
	 * @param encodedResource 属性文件的资源描述符, 允许指定用于解析文件的编码
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(encodedResource, null);
	}

	/**
	 * 从指定的属性文件加载bean定义.
	 * 
	 * @param encodedResource 属性文件的资源描述符, 允许指定用于解析文件的编码
	 * @param prefix Map中Key的过滤器: e.g. 'beans.' (can be empty or {@code null})
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource, String prefix)
			throws BeanDefinitionStoreException {

		Properties props = new Properties();
		try {
			InputStream is = encodedResource.getResource().getInputStream();
			try {
				if (encodedResource.getEncoding() != null) {
					getPropertiesPersister().load(props, new InputStreamReader(is, encodedResource.getEncoding()));
				}
				else {
					getPropertiesPersister().load(props, is);
				}
			}
			finally {
				is.close();
			}
			return registerBeanDefinitions(props, prefix, encodedResource.getResource().getDescription());
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Could not parse properties from " + encodedResource.getResource(), ex);
		}
	}

	/**
	 * 使用所有属性Key, 注册资源包中包含的bean定义 (i.e. 不按前缀过滤).
	 * 
	 * @param rb 要从中加载的ResourceBundle
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int registerBeanDefinitions(ResourceBundle rb) throws BeanDefinitionStoreException {
		return registerBeanDefinitions(rb, null);
	}

	/**
	 * 注册ResourceBundle中包含的bean定义.
	 * <p>与Map类似的语法. 此方法对于启用标准Java国际化支持很有用.
	 * 
	 * @param rb 要从中加载的ResourceBundle
	 * @param prefix Map中Key的过滤器: e.g. 'beans.' (can be empty or {@code null})
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int registerBeanDefinitions(ResourceBundle rb, String prefix) throws BeanDefinitionStoreException {
		// 只需创建一个Map, 并调用重载方法.
		Map<String, Object> map = new HashMap<String, Object>();
		Enumeration<String> keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			map.put(key, rb.getObject(key));
		}
		return registerBeanDefinitions(map, prefix);
	}


	/**
	 * 使用所有属性Key, 注册Map中包含的bean定义 (i.e. 不按前缀过滤).
	 * 
	 * @param map Map: name -> property (String or Object). 如果来自Properties文件等, 属性值将是字符串.
	 * 属性名称(键)必须是字符串. 类键必须是字符串.
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeansException 在加载或解析错误的情况下
	 */
	public int registerBeanDefinitions(Map<?, ?> map) throws BeansException {
		return registerBeanDefinitions(map, null);
	}

	/**
	 * 注册Map中包含的bean定义. 忽略不合格的属性.
	 * 
	 * @param map Map name -> property (String or Object). 如果来自Properties文件等, 属性值将是字符串.
	 * 属性名称(键)必须是字符串. 类键必须是字符串.
	 * @param prefix Map中Key的过滤器: e.g. 'beans.' (can be empty or {@code null})
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeansException 在加载或解析错误的情况下
	 */
	public int registerBeanDefinitions(Map<?, ?> map, String prefix) throws BeansException {
		return registerBeanDefinitions(map, prefix, "Map " + map);
	}

	/**
	 * 注册Map中包含的bean定义. 忽略不合格的属性.
	 * 
	 * @param map Map name -> property (String or Object). 如果来自Properties文件等, 属性值将是字符串.
	 * 属性名称(键)必须是字符串. 类键必须是字符串.
	 * @param prefix Map中Key的过滤器: e.g. 'beans.' (can be empty or {@code null})
	 * @param resourceDescription Map来自的资源的描述 (用于日志)
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeansException 在加载或解析错误的情况下
	 */
	public int registerBeanDefinitions(Map<?, ?> map, String prefix, String resourceDescription)
			throws BeansException {

		if (prefix == null) {
			prefix = "";
		}
		int beanCount = 0;

		for (Object key : map.keySet()) {
			if (!(key instanceof String)) {
				throw new IllegalArgumentException("Illegal key [" + key + "]: only Strings allowed");
			}
			String keyString = (String) key;
			if (keyString.startsWith(prefix)) {
				// Key is of form: prefix<name>.property
				String nameAndProperty = keyString.substring(prefix.length());
				// 在属性名称之前查找点, 忽略属性键中的点.
				int sepIdx = -1;
				int propKeyIdx = nameAndProperty.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX);
				if (propKeyIdx != -1) {
					sepIdx = nameAndProperty.lastIndexOf(SEPARATOR, propKeyIdx);
				}
				else {
					sepIdx = nameAndProperty.lastIndexOf(SEPARATOR);
				}
				if (sepIdx != -1) {
					String beanName = nameAndProperty.substring(0, sepIdx);
					if (logger.isDebugEnabled()) {
						logger.debug("Found bean name '" + beanName + "'");
					}
					if (!getRegistry().containsBeanDefinition(beanName)) {
						// If we haven't already registered it...
						registerBeanDefinition(beanName, map, prefix + beanName, resourceDescription);
						++beanCount;
					}
				}
				else {
					// Ignore it: 它不是一个有效的bean名称和属性, 尽管它确实以所需的前缀开头.
					if (logger.isDebugEnabled()) {
						logger.debug("Invalid bean name and property [" + nameAndProperty + "]");
					}
				}
			}
		}

		return beanCount;
	}

	/**
	 * 获取所有属性值, 给定前缀(将被剥离), 并将他们定义的bean添加到具有给定名称的工厂
	 * 
	 * @param beanName 要定义的bean的名称
	 * @param map 包含字符串对的Map
	 * @param prefix 将被剥离的每个条目的前缀
	 * @param resourceDescription Map来自的资源的描述 (用于日志)
	 * 
	 * @throws BeansException 如果无法解析或注册bean定义
	 */
	protected void registerBeanDefinition(String beanName, Map<?, ?> map, String prefix, String resourceDescription)
			throws BeansException {

		String className = null;
		String parent = null;
		String scope = GenericBeanDefinition.SCOPE_SINGLETON;
		boolean isAbstract = false;
		boolean lazyInit = false;

		ConstructorArgumentValues cas = new ConstructorArgumentValues();
		MutablePropertyValues pvs = new MutablePropertyValues();

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = StringUtils.trimWhitespace((String) entry.getKey());
			if (key.startsWith(prefix + SEPARATOR)) {
				String property = key.substring(prefix.length() + SEPARATOR.length());
				if (CLASS_KEY.equals(property)) {
					className = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (PARENT_KEY.equals(property)) {
					parent = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (ABSTRACT_KEY.equals(property)) {
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					isAbstract = TRUE_VALUE.equals(val);
				}
				else if (SCOPE_KEY.equals(property)) {
					// Spring 2.0 style
					scope = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (SINGLETON_KEY.equals(property)) {
					// Spring 1.2 style
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					scope = ((val == null || TRUE_VALUE.equals(val) ? GenericBeanDefinition.SCOPE_SINGLETON :
							GenericBeanDefinition.SCOPE_PROTOTYPE));
				}
				else if (LAZY_INIT_KEY.equals(property)) {
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					lazyInit = TRUE_VALUE.equals(val);
				}
				else if (property.startsWith(CONSTRUCTOR_ARG_PREFIX)) {
					if (property.endsWith(REF_SUFFIX)) {
						int index = Integer.parseInt(property.substring(1, property.length() - REF_SUFFIX.length()));
						cas.addIndexedArgumentValue(index, new RuntimeBeanReference(entry.getValue().toString()));
					}
					else {
						int index = Integer.parseInt(property.substring(1));
						cas.addIndexedArgumentValue(index, readValue(entry));
					}
				}
				else if (property.endsWith(REF_SUFFIX)) {
					// 这不是一个真正的属性, 而是对另一个原型的引用
					// 提取属性名称: property is of form dog(ref)
					property = property.substring(0, property.length() - REF_SUFFIX.length());
					String ref = StringUtils.trimWhitespace((String) entry.getValue());

					// 引用的bean是否尚未注册无关紧要:
					// 这将确保在运行时解析引用.
					Object val = new RuntimeBeanReference(ref);
					pvs.add(property, val);
				}
				else {
					// 这是一个普通的bean属性.
					pvs.add(property, readValue(entry));
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Registering bean definition for bean name '" + beanName + "' with " + pvs);
		}

		// 如果没有处理父级本身, 并且没有指定类名, 使用默认父级.
		// 后者必须出于向后兼容性原因而发生.
		if (parent == null && className == null && !beanName.equals(this.defaultParentBean)) {
			parent = this.defaultParentBean;
		}

		try {
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(
					parent, className, getBeanClassLoader());
			bd.setScope(scope);
			bd.setAbstract(isAbstract);
			bd.setLazyInit(lazyInit);
			bd.setConstructorArgumentValues(cas);
			bd.setPropertyValues(pvs);
			getRegistry().registerBeanDefinition(beanName, bd);
		}
		catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(resourceDescription, beanName, className, ex);
		}
		catch (LinkageError err) {
			throw new CannotLoadBeanClassException(resourceDescription, beanName, className, err);
		}
	}

	/**
	 * 读取条目的值. 正确解释bean引用的前缀为星号的值.
	 */
	private Object readValue(Map.Entry<? ,?> entry) {
		Object val = entry.getValue();
		if (val instanceof String) {
			String strVal = (String) val;
			// 如果它以引用前缀开头...
			if (strVal.startsWith(REF_PREFIX)) {
				// 扩展引用.
				String targetName = strVal.substring(1);
				if (targetName.startsWith(REF_PREFIX)) {
					// 转移前缀 -> 使用普通值.
					val = targetName;
				}
				else {
					val = new RuntimeBeanReference(targetName);
				}
			}
		}
		return val;
	}
}
