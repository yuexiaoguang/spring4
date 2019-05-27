package org.springframework.core.env;

import java.util.Collection;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * 由命令行参数支持的{@link PropertySource}实现的抽象基类.
 * 参数化类型{@code T}表示命令行选项的底层源.
 * 对于{@link SimpleCommandLinePropertySource}, 这可能就像String数组一样简单,
 * 或者在{@link JOptCommandLinePropertySource}的情况下特定于特定API, 例如JOpt的{@code OptionSet}.
 *
 * <h3>目的和一般用法</h3>
 *
 * 用于独立的基于Spring的应用程序, i.e. 通过传统的{@code main}方法引导的应用程序, 从命令行接受{@code String []}参数.
 * 在许多情况下, 直接在{@code main}方法中处理命令行参数可能就足够了, 但在其他情况下, 可能需要将参数作为值注入Spring bean.
 * 正是后一组案例中{@code CommandLinePropertySource}变得有用.
 * {@code CommandLinePropertySource}通常会添加到Spring {@code ApplicationContext}的{@link Environment},
 * 此时所有命令行参数都可通过{@link Environment#getProperty(String)}方法系列获得.
 * 例如:
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * 使用上面的引导逻辑, {@code AppConfig}类可以{@code @Inject} Spring {@code Environment}, 并直接查询属性:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public void DataSource dataSource() {
 *         MyVendorDataSource dataSource = new MyVendorDataSource();
 *         dataSource.setHostname(env.getProperty("db.hostname", "localhost"));
 *         dataSource.setUsername(env.getRequiredProperty("db.username"));
 *         dataSource.setPassword(env.getRequiredProperty("db.password"));
 *         // ...
 *         return dataSource;
 *     }
 * }</pre>
 *
 * 因为使用{@code #addFirst}方法将{@code CommandLinePropertySource}添加到{@code Environment}的{@link MutablePropertySources}集合中,
 * 它具有最高的搜索优先级, 这意味着虽然"db.hostname"和其他属性可能存在于其他属性源中, 如系统环境变量,
 * 但它将首先从命令行属性源中选择.
 * 这是一种合理的方法, 因为命令行中指定的参数自然比指定为环境变量的参数更具体.
 *
 * <p>作为注入{@code Environment}的替代方法, 可以使用Spring的{@code @Value}注解来注入这些属性,
 * 因为已经注册了{@link PropertySourcesPropertyResolver} bean, 直接或通过使用{@code <context:property-placeholder>}元素.
 * 例如:
 *
 * <pre class="code">
 * &#064;Component
 * public class MyComponent {
 *
 *     &#064;Value("my.property:defaultVal")
 *     private String myProperty;
 *
 *     public void getMyProperty() {
 *         return this.myProperty;
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>使用选项参数</h3>
 *
 * <p>单个命令行参数通过通常的{@link PropertySource#getProperty(String)}
 * 和{@link PropertySource#containsProperty(String)}方法表示为属性.
 * 例如, 给定以下命令行:
 *
 * <pre class="code">--o1=v1 --o2</pre>
 *
 * 'o1'和'o2'被视为"选项参数", 以下断言将评估为true:
 *
 * <pre class="code">
 * CommandLinePropertySource<?> ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("o3") == false;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("");
 * assert ps.getProperty("o3") == null;
 * </pre>
 *
 * 请注意, 'o2'选项没有参数, 但{@code getProperty("o2")}解析为空字符串({@code ""}), 而不是{@code null},
 * 而{@code getProperty("o3")}解析为 {@code null}, 因为它没有指定.
 * 此行为与所有{@code PropertySource}实现遵循的一般约定一致.
 *
 * <p>另请注意, 虽然上面的示例中使用了"--"来表示选项参数, 但此语法可能因各个命令行参数库而异.
 * 例如, 基于JOpt或Commons CLI的实现可能允许单个破折号 ("-") "short"选项参数, etc.
 *
 * <h3>使用非选项参数</h3>
 *
 * <p>通过这种抽象也支持非选项参数.
 * 任何没有选项样式前缀("-" 或 "--")的参数都被视为"非选项参数",
 * 可通过特殊的{@linkplain #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME "nonOptionArgs"}属性获得.
 * 如果指定了多个非选项参数, 则此属性的值将是包含所有参数的逗号分隔的字符串.
 * 这种方法确保了来自{@code CommandLinePropertySource}的所有属性的简单且一致的返回类型 (String),
 * 在同时与Spring {@link Environment}及其内置 {@code ConversionService}一起使用时, 有助于转换.
 * 请考虑以下示例:
 *
 * <pre class="code">--o1=v1 --o2=v2 /path/to/file1 /path/to/file2</pre>
 *
 * 在此示例中, "o1" 和 "o2"将被视为"选项参数", 而两个文件系统路径则被视为"非选项参数".
 * 因此, 以下断言将评估为真:
 *
 * <pre class="code">
 * CommandLinePropertySource<?> ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("nonOptionArgs") == true;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("v2");
 * assert ps.getProperty("nonOptionArgs").equals("/path/to/file1,/path/to/file2");
 * </pre>
 *
 * <p>如上所述, 当与Spring {@code Environment}抽象结合使用时, 这个以逗号分隔的字符串可以很容易地转换为String数组或列表:
 *
 * <pre class="code">
 * Environment env = applicationContext.getEnvironment();
 * String[] nonOptionArgs = env.getProperty("nonOptionArgs", String[].class);
 * assert nonOptionArgs[0].equals("/path/to/file1");
 * assert nonOptionArgs[1].equals("/path/to/file2");
 * </pre>
 *
 * <p>可以通过{@link #setNonOptionArgsPropertyName(String)}方法自定义特殊"非选项参数"属性的名称.
 * 建议这样做, 因为它为非选项参数提供了适当的语义值.
 * 例如, 如果将文件系统路径指定为非选项参数, 则最好将它们称为"file.locations", 而不是默认的"nonOptionArgs":
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     clps.setNonOptionArgsPropertyName("file.locations");
 *
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * <h3>限制</h3>
 *
 * 此抽象并非旨在公开底层命令行解析API的全部功能, 如 JOpt 或 Commons CLI.
 * 它的意图恰恰相反: 提供最简单的抽象, 以便在它们被解析<em>之后</em>访问命令行参数.
 * 因此, 典型案例将涉及完全配置底层命令行解析API, 解析进入main方法的参数的{@code String[]},
 * 然后将解析结果提供给{@code CommandLinePropertySource}的实现.
 * 此时, 所有参数都可以被视为'选项'或'非选项'参数, 并且如上所述可以通过普通的{@code PropertySource}和{@code Environment} API访问.
 */
public abstract class CommandLinePropertySource<T> extends EnumerablePropertySource<T> {

	/** {@link CommandLinePropertySource}实例的默认名称: {@value} */
	public static final String COMMAND_LINE_PROPERTY_SOURCE_NAME = "commandLineArgs";

	/** 表示非选项参数的属性的默认名称: {@value} */
	public static final String DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME = "nonOptionArgs";


	private String nonOptionArgsPropertyName = DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME;


	/**
	 * 创建一个新的{@code CommandLinePropertySource},
	 * 其默认名称为{@value #COMMAND_LINE_PROPERTY_SOURCE_NAME}, 并由给定的源对象支持.
	 */
	public CommandLinePropertySource(T source) {
		super(COMMAND_LINE_PROPERTY_SOURCE_NAME, source);
	}

	/**
	 * 创建一个具有给定名称, 并由给定源对象支持的新{@link CommandLinePropertySource}.
	 */
	public CommandLinePropertySource(String name, T source) {
		super(name, source);
	}


	/**
	 * 指定特殊"非选项参数"属性的名称.
	 * 默认{@value #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME}.
	 */
	public void setNonOptionArgsPropertyName(String nonOptionArgsPropertyName) {
		this.nonOptionArgsPropertyName = nonOptionArgsPropertyName;
	}

	/**
	 * 此实现首先检查指定的名称是否为特殊
	 * {@linkplain #setNonOptionArgsPropertyName(String) "非选项参数"属性},
	 * 如果是, 则委托给抽象{@link #getNonOptionArgs()}方法检查它是否返回一个空集合.
	 * 否则委托并返回抽象{@link #containsOption(String)}方法的值.
	 */
	@Override
	public final boolean containsProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			return !this.getNonOptionArgs().isEmpty();
		}
		return this.containsOption(name);
	}

	/**
	 * 此实现首先检查指定的名称是否是特殊的
	 * {@linkplain #setNonOptionArgsPropertyName(String) "非选项参数"属性},
	 * 如果是, 则委托给抽象{@link #getNonOptionArgs()}方法.
	 * 如果是这样且非选项参数的集合为空, 则此方法返回{@code null}.
	 * 如果不为空, 则返回所有非选项参数的逗号分隔的String.
	 * 否则委托并返回抽象{@link #getOptionValues(String)}方法的结果.
	 */
	@Override
	public final String getProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			Collection<String> nonOptionArguments = this.getNonOptionArgs();
			if (nonOptionArguments.isEmpty()) {
				return null;
			}
			else {
				return StringUtils.collectionToCommaDelimitedString(nonOptionArguments);
			}
		}
		Collection<String> optionValues = this.getOptionValues(name);
		if (optionValues == null) {
			return null;
		}
		else {
			return StringUtils.collectionToCommaDelimitedString(optionValues);
		}
	}


	/**
	 * 从命令行解析的选项参数集, 是否包含具有给定名称的选项.
	 */
	protected abstract boolean containsOption(String name);

	/**
	 * 返回与具有给定名称的命令行选项关联的值集合.
	 * <ul>
	 * <li>如果该选项存在且没有参数 (e.g.: "--foo"), 则返回一个空集合 ({@code []})</li>
	 * <li>如果该选项存在且具有单个值 (e.g. "--foo=bar"), 则返回具有一个元素的集合 ({@code ["bar"]})</li>
	 * <li>如果该选项存在且底层命令行解析库支持多个参数 (e.g. "--foo=bar --foo=baz"), 则返回一个包含每个值元素的集合 ({@code ["bar", "baz"]})</li>
	 * <li>如果该选项不存在, 则返回{@code null}</li>
	 * </ul>
	 */
	protected abstract List<String> getOptionValues(String name);

	/**
	 * 返回从命令行解析的非选项参数的集合.
	 * Never {@code null}.
	 */
	protected abstract List<String> getNonOptionArgs();

}
