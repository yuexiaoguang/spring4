package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;

/**
 * 在bean定义读取过程中传递的上下文, 封装了所有相关配置以及状态.
 */
public class ReaderContext {

	private final Resource resource;

	private final ProblemReporter problemReporter;

	private final ReaderEventListener eventListener;

	private final SourceExtractor sourceExtractor;


	/**
	 * @param resource XML bean定义源
	 * @param problemReporter 问题报告器
	 * @param eventListener 事件监听器
	 * @param sourceExtractor 源提取器
	 */
	public ReaderContext(Resource resource, ProblemReporter problemReporter,
			ReaderEventListener eventListener, SourceExtractor sourceExtractor) {

		this.resource = resource;
		this.problemReporter = problemReporter;
		this.eventListener = eventListener;
		this.sourceExtractor = sourceExtractor;
	}

	public final Resource getResource() {
		return this.resource;
	}


	// Errors and warnings

	/**
	 * 致命错误.
	 */
	public void fatal(String message, Object source) {
		fatal(message, source, null, null);
	}

	/**
	 * 致命错误.
	 */
	public void fatal(String message, Object source, Throwable ex) {
		fatal(message, source, null, ex);
	}

	/**
	 * 致命错误.
	 */
	public void fatal(String message, Object source, ParseState parseState) {
		fatal(message, source, parseState, null);
	}

	/**
	 * 致命错误.
	 */
	public void fatal(String message, Object source, ParseState parseState, Throwable cause) {
		Location location = new Location(getResource(), source);
		this.problemReporter.fatal(new Problem(message, location, parseState, cause));
	}

	/**
	 * 常规错误.
	 */
	public void error(String message, Object source) {
		error(message, source, null, null);
	}

	/**
	 * 常规错误.
	 */
	public void error(String message, Object source, Throwable ex) {
		error(message, source, null, ex);
	}

	/**
	 * 常规错误.
	 */
	public void error(String message, Object source, ParseState parseState) {
		error(message, source, parseState, null);
	}

	/**
	 * 常规错误.
	 */
	public void error(String message, Object source, ParseState parseState, Throwable cause) {
		Location location = new Location(getResource(), source);
		this.problemReporter.error(new Problem(message, location, parseState, cause));
	}

	/**
	 * 非严重警告.
	 */
	public void warning(String message, Object source) {
		warning(message, source, null, null);
	}

	/**
	 * 非严重警告.
	 */
	public void warning(String message, Object source, Throwable ex) {
		warning(message, source, null, ex);
	}

	/**
	 * 非严重警告.
	 */
	public void warning(String message, Object source, ParseState parseState) {
		warning(message, source, parseState, null);
	}

	/**
	 * 非严重警告.
	 */
	public void warning(String message, Object source, ParseState parseState, Throwable cause) {
		Location location = new Location(getResource(), source);
		this.problemReporter.warning(new Problem(message, location, parseState, cause));
	}


	// Explicit parse events

	/**
	 * 触发默认注册事件.
	 */
	public void fireDefaultsRegistered(DefaultsDefinition defaultsDefinition) {
		this.eventListener.defaultsRegistered(defaultsDefinition);
	}

	/**
	 * 触发组件注册事件.
	 */
	public void fireComponentRegistered(ComponentDefinition componentDefinition) {
		this.eventListener.componentRegistered(componentDefinition);
	}

	/**
	 * 触发别名注册事件.
	 */
	public void fireAliasRegistered(String beanName, String alias, Object source) {
		this.eventListener.aliasRegistered(new AliasDefinition(beanName, alias, source));
	}

	/**
	 * 触发导入处理的事件.
	 */
	public void fireImportProcessed(String importedResource, Object source) {
		this.eventListener.importProcessed(new ImportDefinition(importedResource, source));
	}

	/**
	 * 触发导入处理的事件.
	 */
	public void fireImportProcessed(String importedResource, Resource[] actualResources, Object source) {
		this.eventListener.importProcessed(new ImportDefinition(importedResource, actualResources, source));
	}


	// Source extraction

	/**
	 * 返回正在使用的源提取器.
	 */
	public SourceExtractor getSourceExtractor() {
		return this.sourceExtractor;
	}

	/**
	 * 为给定的源对象调用源提取器.
	 * 
	 * @param sourceCandidate 原始源对象
	 * 
	 * @return 要存储的源对象, 或{@code null}.
	 */
	public Object extractSource(Object sourceCandidate) {
		return this.sourceExtractor.extractSource(sourceCandidate, this.resource);
	}

}
