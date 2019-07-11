package org.springframework.transaction.support;

import java.util.Date;

import org.springframework.transaction.TransactionTimedOutException;

/**
 * 资源保存器的便捷基类.
 *
 * <p>支持仅回滚事务.
 * 可以在一定的秒数或毫秒后过期, 以确定事务超时.
 */
public abstract class ResourceHolderSupport implements ResourceHolder {

	private boolean synchronizedWithTransaction = false;

	private boolean rollbackOnly = false;

	private Date deadline;

	private int referenceCount = 0;

	private boolean isVoid = false;


	/**
	 * 将资源标记为与事务同步.
	 */
	public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
		this.synchronizedWithTransaction = synchronizedWithTransaction;
	}

	/**
	 * 返回资源是否与事务同步.
	 */
	public boolean isSynchronizedWithTransaction() {
		return this.synchronizedWithTransaction;
	}

	/**
	 * 将资源事务标记为仅回滚.
	 */
	public void setRollbackOnly() {
		this.rollbackOnly = true;
	}

	/**
	 * 返回资源事务是否标记为仅回滚.
	 */
	public boolean isRollbackOnly() {
		return this.rollbackOnly;
	}

	/**
	 * 设置此对象的超时, 以秒为单位.
	 * 
	 * @param seconds 到期前的秒数
	 */
	public void setTimeoutInSeconds(int seconds) {
		setTimeoutInMillis(seconds * 1000L);
	}

	/**
	 * 设置此对象的超时, 以毫秒为单位.
	 * 
	 * @param millis 到期前的毫秒数
	 */
	public void setTimeoutInMillis(long millis) {
		this.deadline = new Date(System.currentTimeMillis() + millis);
	}

	/**
	 * 返回此对象是否具有关联的超时.
	 */
	public boolean hasTimeout() {
		return (this.deadline != null);
	}

	/**
	 * 返回此对象的到期截止日期.
	 * 
	 * @return 截止日期
	 */
	public Date getDeadline() {
		return this.deadline;
	}

	/**
	 * 返回此对象的生存时间, 以秒为单位.
	 * 取整加一, e.g. 9.00001 转换为 10.
	 * 
	 * @return 到期前的秒数
	 * @throws TransactionTimedOutException 如果截止日期已经达到
	 */
	public int getTimeToLiveInSeconds() {
		double diff = ((double) getTimeToLiveInMillis()) / 1000;
		int secs = (int) Math.ceil(diff);
		checkTransactionTimeout(secs <= 0);
		return secs;
	}

	/**
	 * 返回此对象的生存时间, 以毫秒为单位.
	 * 
	 * @return 到期前的毫秒数
	 * @throws TransactionTimedOutException 如果截止日期已经达到
	 */
	public long getTimeToLiveInMillis() throws TransactionTimedOutException{
		if (this.deadline == null) {
			throw new IllegalStateException("No timeout specified for this resource holder");
		}
		long timeToLive = this.deadline.getTime() - System.currentTimeMillis();
		checkTransactionTimeout(timeToLive <= 0);
		return timeToLive;
	}

	/**
	 * 如果已达到截止日期, 则设置事务回滚 - 并抛出TransactionTimedOutException.
	 */
	private void checkTransactionTimeout(boolean deadlineReached) throws TransactionTimedOutException {
		if (deadlineReached) {
			setRollbackOnly();
			throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.deadline);
		}
	}

	/**
	 * 将引用计数增加一, 因为已经请求了保存器 (i.e. 有人请求其持有的资源).
	 */
	public void requested() {
		this.referenceCount++;
	}

	/**
	 * 将引用计数减1, 因为保存器已被释放 (i.e. 有人释放了它所持有的资源).
	 */
	public void released() {
		this.referenceCount--;
	}

	/**
	 * 返回是否仍有对该保存器的公开引用.
	 */
	public boolean isOpen() {
		return (this.referenceCount > 0);
	}

	/**
	 * 清除此资源保存器的事务状态.
	 */
	public void clear() {
		this.synchronizedWithTransaction = false;
		this.rollbackOnly = false;
		this.deadline = null;
	}

	/**
	 * 重置此资源保存器 - 事务状态以及引用计数.
	 */
	@Override
	public void reset() {
		clear();
		this.referenceCount = 0;
	}

	@Override
	public void unbound() {
		this.isVoid = true;
	}

	@Override
	public boolean isVoid() {
		return this.isVoid;
	}
}
