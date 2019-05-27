package org.springframework.scripting;

import java.io.IOException;

/**
 * 定义脚本源的接口.
 * 跟踪底层脚本是否已被修改.
 */
public interface ScriptSource {

	/**
	 * 检索当前脚本源文本.
	 * 
	 * @return 脚本
	 * @throws IOException 如果脚本检索失败
	 */
	String getScriptAsString() throws IOException;

	/**
	 * 指示自上次调用{@link #getScriptAsString()}以来是否已修改基础脚本数据.
	 * 如果尚未读取脚本, 则返回{@code true}.
	 * 
	 * @return 脚本数据是否已被修改
	 */
	boolean isModified();

	/**
	 * 确定底层脚本的类名.
	 * 
	 * @return 建议的类名, 或{@code null}
	 */
	String suggestedClassName();

}
