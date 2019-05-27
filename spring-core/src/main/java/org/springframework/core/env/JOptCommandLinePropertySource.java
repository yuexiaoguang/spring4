package org.springframework.core.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.util.StringUtils;

/**
 * {@link CommandLinePropertySource}实现, 由JOpt {@link OptionSet}支持.
 *
 * <h2>典型用法</h2>
 *
 * 针对提供给{@code main}方法的参数的{@code String []}, 配置并执行{@code OptionParser},
 * 并使用生成的{@code OptionSet}对象创建{@link JOptCommandLinePropertySource}:
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     OptionParser parser = new OptionParser();
 *     parser.accepts("option1");
 *     parser.accepts("option2").withRequiredArg();
 *     OptionSet options = parser.parse(args);
 *     PropertySource<?> ps = new JOptCommandLinePropertySource(options);
 *     // ...
 * }</pre>
 *
 * 有关完整的一般用法示例, 请参阅{@link CommandLinePropertySource}.
 *
 * <p>需要JOpt Simple 4.3或更高版本. 测试JOpt直到5.0.
 */
public class JOptCommandLinePropertySource extends CommandLinePropertySource<OptionSet> {

	/**
	 * 创建一个具有默认名称并由给定{@code OptionSet}支持的新{@code JOptCommandLinePropertySource}.
	 */
	public JOptCommandLinePropertySource(OptionSet options) {
		super(options);
	}

	/**
	 * 创建一个具有给定名称并由给定{@code OptionSet}支持的新{@code JOptCommandLinePropertySource}.
	 */
	public JOptCommandLinePropertySource(String name, OptionSet options) {
		super(name, options);
	}


	@Override
	protected boolean containsOption(String name) {
		return this.source.has(name);
	}

	@Override
	public String[] getPropertyNames() {
		List<String> names = new ArrayList<String>();
		for (OptionSpec<?> spec : this.source.specs()) {
			List<String> aliases = spec.options();
			if (!aliases.isEmpty()) {
				// 只有最长的名称用于枚举
				names.add(aliases.get(aliases.size() - 1));
			}
		}
		return StringUtils.toStringArray(names);
	}

	@Override
	public List<String> getOptionValues(String name) {
		List<?> argValues = this.source.valuesOf(name);
		List<String> stringArgValues = new ArrayList<String>();
		for (Object argValue : argValues) {
			stringArgValues.add(argValue.toString());
		}
		if (stringArgValues.isEmpty()) {
			return (this.source.has(name) ? Collections.<String>emptyList() : null);
		}
		return Collections.unmodifiableList(stringArgValues);
	}

	@Override
	protected List<String> getNonOptionArgs() {
		List<?> argValues = this.source.nonOptionArguments();
		List<String> stringArgValues = new ArrayList<String>();
		for (Object argValue : argValues) {
			stringArgValues.add(argValue.toString());
		}
		return (stringArgValues.isEmpty() ? Collections.<String>emptyList() :
				Collections.unmodifiableList(stringArgValues));
	}

}
