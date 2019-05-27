package org.springframework.scripting;

import java.io.IOException;

/**
 * 脚本定义接口, 封装特定脚本的配置, 以及用于创建实际脚本的Java {@code Object}的工厂方法.
 */
public interface ScriptFactory {

	/**
	 * 返回指向脚本源的定位器.
	 * 由实际创建脚本的后处理器解释.
	 * <p>典型的支持的定位器是Spring资源位置
	 * (例如 "file:C:/myScript.bsh" or "classpath:myPackage/myScript.bsh")
	 * 和内联脚本 ("inline:myScriptText...").
	 * 
	 * @return 脚本源的定位器
	 */
	String getScriptSourceLocator();

	/**
	 * 返回脚本应该实现的业务接口.
	 * <p>如果脚本本身确定其Java接口 (例如在Groovy的情况下), .
	 * 
	 * @return 脚本的接口
	 */
	Class<?>[] getScriptInterfaces();

	/**
	 * 返回脚本是否需要为其生成配置接口.
	 * 对于不自行确定Java签名的脚本, 通常就是这种情况, {@code getScriptInterfaces()}中没有指定适当的配置接口.
	 * 
	 * @return 脚本是否需要生成的配置接口
	 */
	boolean requiresConfigInterface();

	/**
	 * 用于创建脚本的Java对象的工厂方法.
	 * <p>鼓励实现缓存脚本元数据, 例如生成的脚本类.
	 * 请注意, 此方法可以并发调用, 并且必须以线程安全的方式实现.
	 * 
	 * @param scriptSource 实际的ScriptSource, 从中检索脚本源文本 (never {@code null})
	 * @param actualInterfaces 要公开的实际接口, 包括脚本接口以及生成的配置接口 (如果适用; 可能是{@code null})
	 * 
	 * @return 脚本的Java对象
	 * @throws IOException 如果脚本检索失败
	 * @throws ScriptCompilationException 如果脚本编译失败
	 */
	Object getScriptedObject(ScriptSource scriptSource, Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException;

	/**
	 * 确定脚本的Java对象的类型.
	 * <p>鼓励实现缓存脚本元数据, 例如生成的脚本类.
	 * 请注意, 此方法可以并发调用, 并且必须以线程安全的方式实现.
	 * 
	 * @param scriptSource 实际的ScriptSource, 从中检索脚本源文本 (never {@code null})
	 * 
	 * @return 脚本的Java对象的类型, 或{@code null}如果不能确定
	 * @throws IOException 如果脚本检索失败
	 * @throws ScriptCompilationException 如果脚本编译失败
	 */
	Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException;

	/**
	 * 确定是否需要刷新 (e.g. 通过ScriptSource的 {@code isModified()}方法).
	 * 
	 * @param scriptSource 实际的ScriptSource, 从中检索脚本源文本 (never {@code null})
	 * 
	 * @return 是否需要新的{@link #getScriptedObject}调用
	 */
	boolean requiresScriptedObjectRefresh(ScriptSource scriptSource);

}
