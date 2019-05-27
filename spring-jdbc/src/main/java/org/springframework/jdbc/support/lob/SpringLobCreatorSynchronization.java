package org.springframework.jdbc.support.lob;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.util.Assert;

/**
 * 在Spring事务结束时资源清理的回调.
 * 调用{@code LobCreator.close()}来清理可能已创建的临时LOB.
 */
public class SpringLobCreatorSynchronization extends TransactionSynchronizationAdapter {

	/**
	 * 清理LobCreator的TransactionSynchronization对象的顺序值.
	 * 返回 CONNECTION_SYNCHRONIZATION_ORDER - 200 以在Hibernate会话 (- 100)和JDBC 连接清理之前执行LobCreator清理.
	 */
	public static final int LOB_CREATOR_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 200;


	private final LobCreator lobCreator;

	private boolean beforeCompletionCalled = false;


	/**
	 * @param lobCreator 在事务完成后关闭的LobCreator
	 */
	public SpringLobCreatorSynchronization(LobCreator lobCreator) {
		Assert.notNull(lobCreator, "LobCreator must not be null");
		this.lobCreator = lobCreator;
	}

	@Override
	public int getOrder() {
		return LOB_CREATOR_SYNCHRONIZATION_ORDER;
	}


	@Override
	public void beforeCompletion() {
		// 尽可能早地关闭LobCreator, 以避免在事务完成之后执行JDBC操作时, 发出警告的严格JTA实现的问题.
		this.beforeCompletionCalled = true;
		this.lobCreator.close();
	}

	@Override
	public void afterCompletion(int status) {
		if (!this.beforeCompletionCalled) {
			// 之前没有调用beforeCompletion (可能是因为在事务管理器中提交时刷新, 在beforeCompletion调用链之后).
			// 关闭 LobCreator.
			this.lobCreator.close();
		}
	}
}
