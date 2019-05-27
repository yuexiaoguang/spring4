package org.springframework.aop.target;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import org.springframework.beans.BeansException;
import org.springframework.core.Constants;

/**
 * {@link org.springframework.aop.TargetSource}实现，它将对象保存在可配置的Apache Commons Pool中.
 *
 * <p>默认情况下, 创建了一个{@code GenericObjectPool}实例.
 * 子类可能会通过覆盖{@code createObjectPool()}方法更改所使用的{@code ObjectPool}的类型.
 *
 * <p>提供许多配置属性，反映Commons Pool {@code GenericObjectPool}类; 这些属性在构造期间传递给{@code GenericObjectPool}. 
 * 如果创建此类的子类要更改{@code ObjectPool}实现类型, 传递与您选择的实现相关的配置属性的值.
 *
 * <p>{@code testOnBorrow}, {@code testOnReturn}, {@code testWhileIdle} 属性显式未反映, 因为此类使用的{@code PoolableObjectFactory}的实现没有实现有意义的验证. 
 * 所有公开的Commons Pool属性都使用相应的Commons Pool默认值.
 *
 * <p>Compatible with Apache Commons Pool 1.5.x and 1.6.
 * 请注意，此类不会声明Commons Pool 1.6的泛型类型，以便在运行时与Commons Pool 1.5.x保持兼容.
 * 
 * @deprecated as of Spring 4.2, in favor of {@link CommonsPool2TargetSource}
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
@Deprecated
public class CommonsPoolTargetSource extends AbstractPoolingTargetSource implements PoolableObjectFactory {

	private static final Constants constants = new Constants(GenericObjectPool.class);


	private int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;

	private int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;

	private long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;

	private long timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

	private long minEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

	private byte whenExhaustedAction = GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;

	/**
	 * The Apache Commons {@code ObjectPool} used to pool target objects
	 */
	private ObjectPool pool;


	/**
	 * Create a CommonsPoolTargetSource with default settings.
	 * Default maximum size of the pool is 8.
	 */
	public CommonsPoolTargetSource() {
		setMaxSize(GenericObjectPool.DEFAULT_MAX_ACTIVE);
	}

	/**
	 * Set the maximum number of idle objects in the pool.
	 * Default is 8.
	 */
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	/**
	 * Return the maximum number of idle objects in the pool.
	 */
	public int getMaxIdle() {
		return this.maxIdle;
	}

	/**
	 * Set the minimum number of idle objects in the pool.
	 * Default is 0.
	 */
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	/**
	 * Return the minimum number of idle objects in the pool.
	 */
	public int getMinIdle() {
		return this.minIdle;
	}

	/**
	 * Set the maximum waiting time for fetching an object from the pool.
	 * Default is -1, waiting forever.
	 */
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	/**
	 * Return the maximum waiting time for fetching an object from the pool.
	 */
	public long getMaxWait() {
		return this.maxWait;
	}

	/**
	 * Set the time between eviction runs that check idle objects whether
	 * they have been idle for too long or have become invalid.
	 * Default is -1, not performing any eviction.
	 */
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	/**
	 * Return the time between eviction runs that check idle objects.
	 */
	public long getTimeBetweenEvictionRunsMillis() {
		return this.timeBetweenEvictionRunsMillis;
	}

	/**
	 * Set the minimum time that an idle object can sit in the pool before
	 * it becomes subject to eviction. Default is 1800000 (30 minutes).
	 * <p>Note that eviction runs need to be performed to take this
	 * setting into effect.
	 */
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	/**
	 * Return the minimum time that an idle object can sit in the pool.
	 */
	public long getMinEvictableIdleTimeMillis() {
		return this.minEvictableIdleTimeMillis;
	}

	/**
	 * Set the action to take when the pool is exhausted. Uses the
	 * constant names defined in Commons Pool's GenericObjectPool class:
	 * "WHEN_EXHAUSTED_BLOCK", "WHEN_EXHAUSTED_FAIL", "WHEN_EXHAUSTED_GROW".
	 */
	public void setWhenExhaustedActionName(String whenExhaustedActionName) {
		setWhenExhaustedAction(constants.asNumber(whenExhaustedActionName).byteValue());
	}

	/**
	 * Set the action to take when the pool is exhausted. Uses the
	 * constant values defined in Commons Pool's GenericObjectPool class.
	 */
	public void setWhenExhaustedAction(byte whenExhaustedAction) {
		this.whenExhaustedAction = whenExhaustedAction;
	}

	/**
	 * Return the action to take when the pool is exhausted.
	 */
	public byte getWhenExhaustedAction() {
		return whenExhaustedAction;
	}


	/**
	 * Creates and holds an ObjectPool instance.
	 */
	@Override
	protected final void createPool() {
		logger.debug("Creating Commons object pool");
		this.pool = createObjectPool();
	}

	/**
	 * Subclasses can override this if they want to return a specific Commons pool.
	 * They should apply any configuration properties to the pool here.
	 * <p>Default is a GenericObjectPool instance with the given pool size.
	 * 
	 * @return an empty Commons {@code ObjectPool}.
	 */
	protected ObjectPool createObjectPool() {
		GenericObjectPool gop = new GenericObjectPool(this);
		gop.setMaxActive(getMaxSize());
		gop.setMaxIdle(getMaxIdle());
		gop.setMinIdle(getMinIdle());
		gop.setMaxWait(getMaxWait());
		gop.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		gop.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		gop.setWhenExhaustedAction(getWhenExhaustedAction());
		return gop;
	}


	/**
	 * Borrow an object from the {@code ObjectPool}.
	 */
	@Override
	public Object getTarget() throws Exception {
		return this.pool.borrowObject();
	}

	/**
	 * Returns the specified object to the underlying {@code ObjectPool}.
	 */
	@Override
	public void releaseTarget(Object target) throws Exception {
		this.pool.returnObject(target);
	}

	@Override
	public int getActiveCount() throws UnsupportedOperationException {
		return this.pool.getNumActive();
	}

	@Override
	public int getIdleCount() throws UnsupportedOperationException {
		return this.pool.getNumIdle();
	}


	/**
	 * Closes the underlying {@code ObjectPool} when destroying this object.
	 */
	@Override
	public void destroy() throws Exception {
		logger.debug("Closing Commons ObjectPool");
		this.pool.close();
	}


	//----------------------------------------------------------------------------
	// Implementation of org.apache.commons.pool.PoolableObjectFactory interface
	//----------------------------------------------------------------------------

	@Override
	public Object makeObject() throws BeansException {
		return newPrototypeInstance();
	}

	@Override
	public void destroyObject(Object obj) throws Exception {
		destroyPrototypeInstance(obj);
	}

	@Override
	public boolean validateObject(Object obj) {
		return true;
	}

	@Override
	public void activateObject(Object obj) {
	}

	@Override
	public void passivateObject(Object obj) {
	}

}
