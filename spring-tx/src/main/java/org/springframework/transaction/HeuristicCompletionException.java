package org.springframework.transaction;

/**
 * 表示由事务协调器的启发式决策引起的事务失败的异常.
 */
@SuppressWarnings("serial")
public class HeuristicCompletionException extends TransactionException {

	/**
	 * 启发式完成事务的结果状态的值.
	 */
	public static final int STATE_UNKNOWN = 0;
	public static final int STATE_COMMITTED = 1;
	public static final int STATE_ROLLED_BACK = 2;
	public static final int STATE_MIXED = 3;


	public static String getStateString(int state) {
		switch (state) {
			case STATE_COMMITTED:
				return "committed";
			case STATE_ROLLED_BACK:
				return "rolled back";
			case STATE_MIXED:
				return "mixed";
			default:
				return "unknown";
		}
	}


	/**
	 * 事务的结果状态: 已提交部分或全部资源?
	 */
	private int outcomeState = STATE_UNKNOWN;


	/**
	 * @param outcomeState 事务的结果状态
	 * @param cause 正在使用的事务API的根本原因
	 */
	public HeuristicCompletionException(int outcomeState, Throwable cause) {
		super("Heuristic completion: outcome state is " + getStateString(outcomeState), cause);
		this.outcomeState = outcomeState;
	}

	/**
	 * 返回事务状态的结果状态, 此类中的常量之一.
	 */
	public int getOutcomeState() {
		return outcomeState;
	}
}
