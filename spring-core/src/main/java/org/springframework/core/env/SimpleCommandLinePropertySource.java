package org.springframework.core.env;

import java.util.List;

import org.springframework.util.StringUtils;

/**
 * {@link CommandLinePropertySource}实现, 由简单的String数组支持.
 *
 * <h3>目的</h3>
 * 此{@code CommandLinePropertySource}实现旨在提供解析命令行参数的最简单方法.
 * 与所有{@code CommandLinePropertySource}实现一样, 命令行参数分为两个不同的组:
 * <em>选项参数</em>和<em>非选项参数</em>, 如下所述 <em>(从{@link SimpleCommandLineArgsParser的Javadoc复制的某些部分)</em>:
 *
 * <h3>使用选项参数</h3>
 * 选项参数必须遵循确切的语法:
 * <pre class="code">--optName[=optValue]</pre>
 * 也就是说, 选项必须以 "{@code --}"为前缀, 并且可能指定或不指定值.
 * 如果指定了值, 则名称和值必须用等号("=")分隔, 不能有<em>空格</em>.
 *
 * <h4>选项参数的有效示例</h4>
 * <pre class="code">
 * --foo
 * --foo=bar
 * --foo="bar then baz"
 * --foo=bar,baz,biz</pre>
 *
 * <h4>选项参数的无效示例</h4>
 * <pre class="code">
 * -foo
 * --foo bar
 * --foo = bar
 * --foo=bar --foo=baz --foo=biz</pre>
 *
 * <h3>使用非选项参数</h3>
 * 在没有"{@code --}"选项前缀的命令行中指定的所有参数将被视为"非选项参数",
 * 并通过{@link CommandLineArgs#getNonOptionArgs()}方法提供.
 *
 * <h2>典型用法</h2>
 * <pre class="code">
 * public static void main(String[] args) {
 *     PropertySource<?> ps = new SimpleCommandLinePropertySource(args);
 *     // ...
 * }</pre>
 *
 * 有关完整的一般用法示例, 请参阅{@link CommandLinePropertySource}.
 *
 * <h3>超越基础</h3>
 *
 * <p>当需要更全面的命令行解析时，请考虑使用提供的{@link JOptCommandLinePropertySource},
 * 或针对您选择的命令行解析库实现您自己的{@code CommandLinePropertySource}!
 */
public class SimpleCommandLinePropertySource extends CommandLinePropertySource<CommandLineArgs> {

	/**
	 * 创建一个具有默认名称并由命令行参数的给定{@code String []}支持的新{@code SimpleCommandLinePropertySource}.
	 */
	public SimpleCommandLinePropertySource(String... args) {
		super(new SimpleCommandLineArgsParser().parse(args));
	}

	/**
	 * 创建一个具有给定名称并由命令行参数的给定{@code String []}支持的新{@code SimpleCommandLinePropertySource}.
	 */
	public SimpleCommandLinePropertySource(String name, String[] args) {
		super(name, new SimpleCommandLineArgsParser().parse(args));
	}

	/**
	 * 获取选项参数的属性名称.
	 */
	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.getOptionNames());
	}

	@Override
	protected boolean containsOption(String name) {
		return this.source.containsOption(name);
	}

	@Override
	protected List<String> getOptionValues(String name) {
		return this.source.getOptionValues(name);
	}

	@Override
	protected List<String> getNonOptionArgs() {
		return this.source.getNonOptionArgs();
	}

}
