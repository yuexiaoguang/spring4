package org.springframework.beans.factory.parsing;

/**
 * SPI接口, 允许工具和其他外部进程处理bean定义解析期间报告的错误和警告.
 */
public interface ProblemReporter {

	/**
	 * 在解析过程中遇到致命错误时调用.
	 * <p>实现必须将给定的问题视为致命的, i.e. 最终必须抛出异常.
	 * 
	 * @param problem 错误源 (never {@code null})
	 */
	void fatal(Problem problem);

	/**
	 * 在解析过程中遇到错误时调用.
	 * <p>实现可能会有选择地将错误视为致命错误.
	 * 
	 * @param problem 错误源 (never {@code null})
	 */
	void error(Problem problem);

	/**
	 * 在解析过程中发出警告时调用.
	 * <p>警告从未被视为致命的.
	 * 
	 * @param problem 警告源 (never {@code null})
	 */
	void warning(Problem problem);

}
