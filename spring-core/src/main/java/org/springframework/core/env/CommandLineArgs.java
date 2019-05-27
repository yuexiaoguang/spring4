package org.springframework.core.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 命令行参数的简单表示, 分为"选项参数" 和 "非选项参数".
 */
class CommandLineArgs {

	private final Map<String, List<String>> optionArgs = new HashMap<String, List<String>>();
	private final List<String> nonOptionArgs = new ArrayList<String>();

	/**
	 * 为给定的选项名称添加选项参数, 并将给定值添加到与此选项关联的值列表中 (其中可能有零个或更多).
	 * 给定的值可能是{@code null}, 表示该选项是在没有关联值的情况下指定的(e.g. "--foo" vs. "--foo=bar").
	 */
	public void addOptionArg(String optionName, String optionValue) {
		if (!this.optionArgs.containsKey(optionName)) {
			this.optionArgs.put(optionName, new ArrayList<String>());
		}
		if (optionValue != null) {
			this.optionArgs.get(optionName).add(optionValue);
		}
	}

	/**
	 * 返回命令行中存在的所有选项参数的集合.
	 */
	public Set<String> getOptionNames() {
		return Collections.unmodifiableSet(this.optionArgs.keySet());
	}

	/**
	 * 返回命令行中是否存在具有给定名称的选项.
	 */
	public boolean containsOption(String optionName) {
		return this.optionArgs.containsKey(optionName);
	}

	/**
	 * 返回与给定选项关联的值列表.
	 * {@code null}表示该选项不存在; 空列表表示没有与此选项关联的值.
	 */
	public List<String> getOptionValues(String optionName) {
		return this.optionArgs.get(optionName);
	}

	/**
	 * 将给定值添加到非选项参数列表中.
	 */
	public void addNonOptionArg(String value) {
		this.nonOptionArgs.add(value);
	}

	/**
	 * 返回命令行中指定的非选项参数列表.
	 */
	public List<String> getNonOptionArgs() {
		return Collections.unmodifiableList(this.nonOptionArgs);
	}

}
