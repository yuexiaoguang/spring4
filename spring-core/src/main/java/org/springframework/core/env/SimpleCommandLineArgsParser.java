package org.springframework.core.env;

/**
 * 解析命令行参数的{@code String[]}, 以填充{@link CommandLineArgs}对象.
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
 */
class SimpleCommandLineArgsParser {

	/**
	 * 根据上面描述的{@linkplain SimpleCommandLineArgsParser}规则解析给定的{@code String}数组,
	 * 返回一个完全填充的{@link CommandLineArgs}对象.
	 * 
	 * @param args 命令行参数, 通常来自{@code main()}方法
	 */
	public CommandLineArgs parse(String... args) {
		CommandLineArgs commandLineArgs = new CommandLineArgs();
		for (String arg : args) {
			if (arg.startsWith("--")) {
				String optionText = arg.substring(2, arg.length());
				String optionName;
				String optionValue = null;
				if (optionText.contains("=")) {
					optionName = optionText.substring(0, optionText.indexOf('='));
					optionValue = optionText.substring(optionText.indexOf('=')+1, optionText.length());
				}
				else {
					optionName = optionText;
				}
				if (optionName.isEmpty() || (optionValue != null && optionValue.isEmpty())) {
					throw new IllegalArgumentException("Invalid argument syntax: " + arg);
				}
				commandLineArgs.addOptionArg(optionName, optionValue);
			}
			else {
				commandLineArgs.addNonOptionArg(arg);
			}
		}
		return commandLineArgs;
	}
}
