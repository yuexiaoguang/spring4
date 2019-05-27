package org.springframework.util.backoff;

/**
 * 提供{@link BackOffExecution}, 指示应重试操作的速率.
 *
 * <p>此接口的用户应该像这样使用它:
 *
 * <pre class="code">
 * BackOffExecution exec = backOff.start();
 *
 * // In the operation recovery/retry loop:
 * long waitInterval = exec.nextBackOff();
 * if (waitInterval == BackOffExecution.STOP) {
 *     // do not retry operation
 * }
 * else {
 *     // sleep, e.g. Thread.sleep(waitInterval)
 *     // retry operation
 * }
 * }</pre>
 *
 * 一旦底层操作成功完成, 就可以简单地丢弃执行实例.
 */
public interface BackOff {

	/**
	 * 开始新的回退执行.
	 * 
	 * @return 准备好使用的新的{@link BackOffExecution}
	 */
	BackOffExecution start();

}
