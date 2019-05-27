package org.springframework.core.env;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.SpringProperties;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Environment}实现的抽象基类.
 * 支持保留的默认配置文件名称的概念,
 * 并允许通过{@link #ACTIVE_PROFILES_PROPERTY_NAME}和{@link #DEFAULT_PROFILES_PROPERTY_NAME}属性指定活动的和默认的配置文件.
 *
 * <p>具体的子类主要区别在于它们默认添加的{@link PropertySource}对象.
 * {@code AbstractEnvironment}不添加.
 * 子类应通过受保护的{@link #customizePropertySources(MutablePropertySources)}钩子提供属性源,
 * 而客户端应使用{@link ConfigurableEnvironment#getPropertySources()}进行自定义, 并使用{@link MutablePropertySources} API.
 * 有关用法示例, 请参阅{@link ConfigurableEnvironment} javadoc.
 */
public abstract class AbstractEnvironment implements ConfigurableEnvironment {

	/**
	 * 指示Spring忽略系统环境变量的系统属性,
	 * i.e. 永远不要尝试通过{@link System#getenv()}检索这样的变量.
	 * <p>默认 "false", 回退到系统环境变量检查, 如果Spring环境属性 (e.g. 配置字符串中的占位符)无法解析.
	 * 考虑将此标志切换为"true", 如果遇到来自Spring的{@code getenv}调用的日志警告,
	 * e.g. 在WebSphere上使用严格的SecurityManager设置和AccessControlExceptions警告.
	 */
	public static final String IGNORE_GETENV_PROPERTY_NAME = "spring.getenv.ignore";

	/**
	 * 指定活动的配置文件的属性名称: {@value}. 值可以用逗号分隔.
	 * <p>请注意, 某些shell环境(如Bash)不允许在变量名中使用句点字符.
	 * 假设正在使用Spring的{@link SystemEnvironmentPropertySource}, 可以将此属性指定为{@code SPRING_PROFILES_ACTIVE}环境变量.
	 */
	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profiles.active";

	/**
	 * 指定默认的配置文件的属性名称: {@value}. 值可以用逗号分隔.
	 * <p>请注意, 某些shell环境(如Bash)不允许在变量名中使用句点字符.
	 * 假设正在使用Spring的 {@link SystemEnvironmentPropertySource}, 可以将此属性指定为{@code SPRING_PROFILES_DEFAULT}环境变量.
	 */
	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profiles.default";

	/**
	 * 保留的默认配置文件名称: {@value}.
	 * 如果未明确设置默认的配置文件名称, 且未显式设置活动的配置文件名称, 则默认情况下将自动激活此配置文件.
	 */
	protected static final String RESERVED_DEFAULT_PROFILE_NAME = "default";


	protected final Log logger = LogFactory.getLog(getClass());

	private final Set<String> activeProfiles = new LinkedHashSet<String>();

	private final Set<String> defaultProfiles = new LinkedHashSet<String>(getReservedDefaultProfiles());

	private final MutablePropertySources propertySources = new MutablePropertySources(this.logger);

	private final ConfigurablePropertyResolver propertyResolver =
			new PropertySourcesPropertyResolver(this.propertySources);


	/**
	 * 创建一个新的{@code Environment}实例, 在构造期间回调{@link #customizePropertySources(MutablePropertySources)},
	 * 以允许子类在适当的时候贡献或操作{@link PropertySource}实例.
	 */
	public AbstractEnvironment() {
		customizePropertySources(this.propertySources);
		if (logger.isDebugEnabled()) {
			logger.debug("Initialized " + getClass().getSimpleName() + " with PropertySources " + this.propertySources);
		}
	}


	/**
	 * 在调用{@link #getProperty(String)}和相关方法期间, 自定义要由{@code Environment}搜索的{@link PropertySource}对象.
	 *
	 * <p>鼓励覆盖此方法的子类使用{@link MutablePropertySources#addLast(PropertySource)}添加属性源,
	 * 以便进一步的子类可以调用{@code super.customizePropertySources()}, 并获得可预测的结果.
	 * 例如:
	 * <pre class="code">
	 * public class Level1Environment extends AbstractEnvironment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // no-op from base class
	 *         propertySources.addLast(new PropertySourceA(...));
	 *         propertySources.addLast(new PropertySourceB(...));
	 *     }
	 * }
	 *
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // add all from superclass
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *     }
	 * }
	 * </pre>
	 * 在这种安排中, 将按顺序解析源 A, B, C, D的属性.
	 * 也就是说, 属性源 "A"优先于属性源 "D".
	 * 如果{@code Level2Environment}子类希望属性源 C 和 D 优先于A和B, 可以在之后调用{@code super.customizePropertySources},
	 * 而不是在添加自己的属性源之前:
	 * <pre class="code">
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *         super.customizePropertySources(propertySources); // add all from superclass
	 *     }
	 * }
	 * </pre>
	 * 搜索顺序现在是 C, D, A, B.
	 *
	 * <p>除了这些建议之外, 子类可以使用{@link MutablePropertySources}公开的任何{@code add&#42;}, {@code remove}, 或{@code replace}方法,
	 * 以创建所需属性源的精确排列.
	 *
	 * <p>基础实现不会注册属性源.
	 *
	 * <p>请注意, 任何{@link ConfigurableEnvironment}的客户端都可以通过{@link #getPropertySources()}访问器进一步自定义属性源,
	 * 通常位于{@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}中.
	 * 例如:
	 * <pre class="code">
	 * ConfigurableEnvironment env = new StandardEnvironment();
	 * env.getPropertySources().addLast(new PropertySourceX(...));
	 * </pre>
	 *
	 * <h2>关于实例变量访问的警告</h2>
	 * 在子类中声明并具有默认初始值的实例变量<em>不</em>应该在此方法中访问.
	 * 由于Java对象创建生命周期约束, 当{@link #AbstractEnvironment()}构造函数调用此回调时, 尚未分配任何初始值,
	 * 这可能导致{@code NullPointerException}或其他问题.
	 * 如果需要访问实例变量的默认值, 请将此方法保留为无操作, 并直接在子类构造函数中执行属性源操作和实例变量访问.
	 * 请注意, 为实例变量<em>分配</em>值不是问题; 它只是试图读取必须避免的默认值.
	 */
	protected void customizePropertySources(MutablePropertySources propertySources) {
	}

	/**
	 * 返回保留的默认配置文件名称.
	 * 此实现返回{@value #RESERVED_DEFAULT_PROFILE_NAME}.
	 * 子类可以覆盖以自定义保留的名称.
	 */
	protected Set<String> getReservedDefaultProfiles() {
		return Collections.singleton(RESERVED_DEFAULT_PROFILE_NAME);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableEnvironment interface
	//---------------------------------------------------------------------

	@Override
	public String[] getActiveProfiles() {
		return StringUtils.toStringArray(doGetActiveProfiles());
	}

	/**
	 * 返回通过{@link #setActiveProfiles}显式设置的活动的配置文件,
	 * 或者如果当前活动的配置文件集合为空, 检查是否存在{@value #ACTIVE_PROFILES_PROPERTY_NAME}属性, 并将其值分配给活动的配置文件集合.
	 */
	protected Set<String> doGetActiveProfiles() {
		synchronized (this.activeProfiles) {
			if (this.activeProfiles.isEmpty()) {
				String profiles = getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
				if (StringUtils.hasText(profiles)) {
					setActiveProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.activeProfiles;
		}
	}

	@Override
	public void setActiveProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profiles " + Arrays.asList(profiles));
		}
		synchronized (this.activeProfiles) {
			this.activeProfiles.clear();
			for (String profile : profiles) {
				validateProfile(profile);
				this.activeProfiles.add(profile);
			}
		}
	}

	@Override
	public void addActiveProfile(String profile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profile '" + profile + "'");
		}
		validateProfile(profile);
		doGetActiveProfiles();
		synchronized (this.activeProfiles) {
			this.activeProfiles.add(profile);
		}
	}


	@Override
	public String[] getDefaultProfiles() {
		return StringUtils.toStringArray(doGetDefaultProfiles());
	}

	/**
	 * 返回通过{@link #setDefaultProfiles(String...)}显式设置的默认的配置文件集合,
	 * 或者当前默认的配置文件集仅包含{@linkplain #getReservedDefaultProfiles() 保留的默认配置文件},
	 * 然后检查是否存在{@value #DEFAULT_PROFILES_PROPERTY_NAME}属性, 并将其值分配给默认配置文件集合.
	 */
	protected Set<String> doGetDefaultProfiles() {
		synchronized (this.defaultProfiles) {
			if (this.defaultProfiles.equals(getReservedDefaultProfiles())) {
				String profiles = getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
				if (StringUtils.hasText(profiles)) {
					setDefaultProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.defaultProfiles;
		}
	}

	/**
	 * 如果通过{@link #setActiveProfiles}没有显式激活其他配置文件, 则指定默认情况下要激活的配置文件集合.
	 * <p>调用此方法将删除覆盖在构建环境期间可能已添加的任何保留的默认配置文件.
	 */
	@Override
	public void setDefaultProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		synchronized (this.defaultProfiles) {
			this.defaultProfiles.clear();
			for (String profile : profiles) {
				validateProfile(profile);
				this.defaultProfiles.add(profile);
			}
		}
	}

	@Override
	public boolean acceptsProfiles(String... profiles) {
		Assert.notEmpty(profiles, "Must specify at least one profile");
		for (String profile : profiles) {
			if (StringUtils.hasLength(profile) && profile.charAt(0) == '!') {
				if (!isProfileActive(profile.substring(1))) {
					return true;
				}
			}
			else if (isProfileActive(profile)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 返回给定的配置文件是否处于活动状态, 或者如果活动的配置文件是否为空, 默认情况下配置文件是否应激活.
	 * 
	 * @throws IllegalArgumentException per {@link #validateProfile(String)}
	 */
	protected boolean isProfileActive(String profile) {
		validateProfile(profile);
		Set<String> currentActiveProfiles = doGetActiveProfiles();
		return (currentActiveProfiles.contains(profile) ||
				(currentActiveProfiles.isEmpty() && doGetDefaultProfiles().contains(profile)));
	}

	/**
	 * 在添加到活动或默认的配置文件集合之前, 验证在内部调用的给定配置文件.
	 * <p>子类可以重写以对配置文件语法施加进一步的限制.
	 * 
	 * @throws IllegalArgumentException 如果配置文件为 null, 空, 仅为空格, 或以配置文件NOT运算符 (!)开头.
	 */
	protected void validateProfile(String profile) {
		if (!StringUtils.hasText(profile)) {
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must contain text");
		}
		if (profile.charAt(0) == '!') {
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must not begin with ! operator");
		}
	}

	@Override
	public MutablePropertySources getPropertySources() {
		return this.propertySources;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Map<String, Object> getSystemEnvironment() {
		if (suppressGetenvAccess()) {
			return Collections.emptyMap();
		}
		try {
			return (Map) System.getenv();
		}
		catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getenv(attributeName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system environment variable '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	/**
	 * 为了{@link #getSystemEnvironment()}的目的, 确定是否禁止{@link System#getenv()}/{@link System#getenv(String)}访问.
	 * <p>如果此方法返回{@code true}, 将使用空的虚拟Map, 而不是常规系统环境Map, 甚至不会尝试调用{@code getenv},
	 * 从而避免安全管理器警告.
	 * <p>默认实现检查"spring.getenv.ignore"系统属性, 如果其值在任何情况下都等于"true", 则返回{@code true}.
	 */
	protected boolean suppressGetenvAccess() {
		return SpringProperties.getFlag(IGNORE_GETENV_PROPERTY_NAME);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Map<String, Object> getSystemProperties() {
		try {
			return (Map) System.getProperties();
		}
		catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getProperty(attributeName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system property '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	@Override
	public void merge(ConfigurableEnvironment parent) {
		for (PropertySource<?> ps : parent.getPropertySources()) {
			if (!this.propertySources.contains(ps.getName())) {
				this.propertySources.addLast(ps);
			}
		}
		String[] parentActiveProfiles = parent.getActiveProfiles();
		if (!ObjectUtils.isEmpty(parentActiveProfiles)) {
			synchronized (this.activeProfiles) {
				for (String profile : parentActiveProfiles) {
					this.activeProfiles.add(profile);
				}
			}
		}
		String[] parentDefaultProfiles = parent.getDefaultProfiles();
		if (!ObjectUtils.isEmpty(parentDefaultProfiles)) {
			synchronized (this.defaultProfiles) {
				this.defaultProfiles.remove(RESERVED_DEFAULT_PROFILE_NAME);
				for (String profile : parentDefaultProfiles) {
					this.defaultProfiles.add(profile);
				}
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurablePropertyResolver interface
	//---------------------------------------------------------------------

	@Override
	public ConfigurableConversionService getConversionService() {
		return this.propertyResolver.getConversionService();
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		this.propertyResolver.setConversionService(conversionService);
	}

	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
	}

	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
	}

	@Override
	public void setValueSeparator(String valueSeparator) {
		this.propertyResolver.setValueSeparator(valueSeparator);
	}

	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.propertyResolver.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
	}

	@Override
	public void setRequiredProperties(String... requiredProperties) {
		this.propertyResolver.setRequiredProperties(requiredProperties);
	}

	@Override
	public void validateRequiredProperties() throws MissingRequiredPropertiesException {
		this.propertyResolver.validateRequiredProperties();
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyResolver interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsProperty(String key) {
		return this.propertyResolver.containsProperty(key);
	}

	@Override
	public String getProperty(String key) {
		return this.propertyResolver.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return this.propertyResolver.getProperty(key, defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType) {
		return this.propertyResolver.getProperty(key, targetType);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return this.propertyResolver.getProperty(key, targetType, defaultValue);
	}

	@Override
	@Deprecated
	public <T> Class<T> getPropertyAsClass(String key, Class<T> targetType) {
		return this.propertyResolver.getPropertyAsClass(key, targetType);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key);
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key, targetType);
	}

	@Override
	public String resolvePlaceholders(String text) {
		return this.propertyResolver.resolvePlaceholders(text);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		return this.propertyResolver.resolveRequiredPlaceholders(text);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " {activeProfiles=" + this.activeProfiles +
				", defaultProfiles=" + this.defaultProfiles + ", propertySources=" + this.propertySources + "}";
	}

}
