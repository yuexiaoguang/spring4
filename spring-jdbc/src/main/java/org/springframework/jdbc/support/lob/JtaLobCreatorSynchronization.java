package org.springframework.jdbc.support.lob;

import javax.transaction.Synchronization;

import org.springframework.util.Assert;

/**
 * 在JTA事务结束时资源清理的回调.
 * 调用{@code LobCreator.close()}来清理可能已创建的临时LOB.
 */
public class JtaLobCreatorSynchronization implements Synchronization {

	private final LobCreator lobCreator;

	private boolean beforeCompletionCalled = false;


	/**
	 * @param lobCreator 在事务完成后关闭的LobCreator
	 */
	public JtaLobCreatorSynchronization(LobCreator lobCreator) {
		Assert.notNull(lobCreator, "LobCreator must not be null");
		this.lobCreator = lobCreator;
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
			// beforeCompletion之前没有调用过 (可能是因为JTA回滚).
			// 在这里关闭LobCreator.
			this.lobCreator.close();
		}
	}
}
