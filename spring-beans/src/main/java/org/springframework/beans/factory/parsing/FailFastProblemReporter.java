package org.springframework.beans.factory.parsing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 简单的{@link ProblemReporter}实现, 在遇到错误时表现出快速失败的行为.
 *
 * <p>遇到的第一个错误导致抛出{@link BeanDefinitionParsingException}.
 *
 * <p>警告写入此类的 {@link #setLogger(org.apache.commons.logging.Log) 日志}.
 */
public class FailFastProblemReporter implements ProblemReporter {

	private Log logger = LogFactory.getLog(getClass());


	/**
	 * 设置用于报告警告的{@link Log logger}.
	 * <p>如果设置为{@code null}, 则将使用设置为实例类名称的默认{@link Log logger}.
	 * 
	 * @param logger 用于报告警告的 {@link Log logger}
	 */
	public void setLogger(Log logger) {
		this.logger = (logger != null ? logger : LogFactory.getLog(getClass()));
	}


	/**
	 * 抛出详细说明发生的错误的{@link BeanDefinitionParsingException}.
	 * 
	 * @param problem 错误源
	 */
	@Override
	public void fatal(Problem problem) {
		throw new BeanDefinitionParsingException(problem);
	}

	/**
	 * 抛出详细说明发生的错误的{@link BeanDefinitionParsingException}.
	 * 
	 * @param problem 错误源
	 */
	@Override
	public void error(Problem problem) {
		throw new BeanDefinitionParsingException(problem);
	}

	/**
	 * 将提供的{@link Problem}写入{@code WARN}级别的{@link Log}.
	 * 
	 * @param problem 警告源
	 */
	@Override
	public void warning(Problem problem) {
		this.logger.warn(problem, problem.getRootCause());
	}

}
