package org.springframework.jdbc.core;

/**
 * 扩展{@link BatchPreparedStatementSetter}接口, 添加批量耗尽检查.
 *
 * <p>此接口允许发出批处理结束的信号, 而不必预先确定确切的批处理大小.
 * 批量大小仍然受到尊重, 但它现在是批量的最大大小.
 *
 * <p>每次调用{@link #setValues}后调用{@link #isBatchExhausted}方法,
 * 以确定是否添加了某些值, 或者确定批量是否已完成, 并且在上次调用{@code setValues}时没有提供其他值.
 *
 * <p>考虑扩展
 * {@link org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter}基类,
 * 而不是直接实现此接口,
 * 使用单个{@code setValuesIfAvailable}回调方法检查可用值并设置它们, 返回是否实际提供了值.
 */
public interface InterruptibleBatchPreparedStatementSetter extends BatchPreparedStatementSetter {

	/**
	 * 返回批处理是否完成, 即在上一次{@code setValues}调用期间是否没有添加其他值.
	 * <p><b>NOTE:</b> 如果此方法返回{@code true}, 则将忽略在上一次{@code setValues}调用期间可能已设置的任何参数!
	 * 如果在{@code setValues}实现的<i>开头</i>检测到耗尽, 请确保设置相应的内部标志, 让此方法根据标志返回{@code true}.
	 * 
	 * @param i 在批处理中发出的语句的索引, 从0开始
	 * 
	 * @return 批处理是否已经耗尽
	 */
	boolean isBatchExhausted(int i);

}
