package org.springframework.aop.target;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * {@link org.springframework.aop.TargetSource}实现, 在可配置的Apache Commons2池中保存对象.
 *
 * <p>默认情况下, 创建了一个{@code GenericObjectPool}实例.
 * 子类可能会通过覆盖{@code createObjectPool()}方法更改所使用的{@code ObjectPool}的类型.
 *
 * <p>提供许多配置属性，反映Commons Pool {@code GenericObjectPool}类;
 * 这些属性在构造期间传递给{@code GenericObjectPool}.
 * 如果创建此类的子类以更改{@code ObjectPool}实现类型, 传递与您选择的实现相关的配置属性的值.
 *
 * <p>{@code testOnBorrow}, {@ code testOnReturn}和{@code testWhileIdle}属性未显式反映,
 * 因为此类使用的{@code PoolableObjectFactory}的实现未实现有意义的验证.
 * 所有公开的Commons Pool属性都使用相应的Commons Pool默认值.
 *
 * <p>Compatible with Apache Commons Pool 2.4, as of Spring 4.2.
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class CommonsPool2TargetSource extends AbstractPoolingTargetSource implements PooledObjectFactory<Object> {

	private int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

	private int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;

	private long maxWait = GenericObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS;

	private long timeBetweenEvictionRunsMillis = GenericObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

	private long minEvictableIdleTimeMillis = GenericObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

	private boolean blockWhenExhausted = GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

	/**
	 * 用于池化目标对象的Apache Commons {@code ObjectPool}
	 */
	private ObjectPool pool;


	/**
	 * 池的默认最大大小为8.
	 */
	public CommonsPool2TargetSource() {
		setMaxSize(GenericObjectPoolConfig.DEFAULT_MAX_TOTAL);
	}


	/**
	 * 设置池中的最大空闲对象数.
	 * 默认 8.
	 */
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	/**
	 * 返回池中的最大空闲对象数.
	 */
	public int getMaxIdle() {
		return this.maxIdle;
	}

	/**
	 * 设置池中的最小空闲对象数.
	 * 默认 0.
	 */
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	/**
	 * 返回池中的最小空闲对象数.
	 */
	public int getMinIdle() {
		return this.minIdle;
	}

	/**
	 * 设置从池中获取对象的最长等待时间.
	 * 默认 -1, 永远等待.
	 */
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	/**
	 * 返回从池中获取对象的最长等待时间.
	 */
	public long getMaxWait() {
		return this.maxWait;
	}

	/**
	 * 设置驱逐运行之间的时间，检查空闲对象是否已空闲太久或已变为无效.
	 * 默认 -1, 不执行驱逐.
	 */
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	/**
	 * 返回检查空闲对象的驱逐运行之间的时间.
	 */
	public long getTimeBetweenEvictionRunsMillis() {
		return this.timeBetweenEvictionRunsMillis;
	}

	/**
	 * 设置空闲对象在被驱逐之前可以在池中放置的最短时间. 默认 1800000 (30 minutes).
	 * <p>请注意，需要执行驱逐运行才能使此设置生效.
	 */
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	/**
	 * 返回空闲对象可以在池中放置的最短时间.
	 */
	public long getMinEvictableIdleTimeMillis() {
		return this.minEvictableIdleTimeMillis;
	}

	/**
	 * 设置当池耗尽时是否应该阻止调用.
	 */
	public void setBlockWhenExhausted(boolean blockWhenExhausted) {
		this.blockWhenExhausted = blockWhenExhausted;
	}

	/**
	 * 指定在池耗尽时是否应阻止调用.
	 */
	public boolean isBlockWhenExhausted() {
		return this.blockWhenExhausted;
	}


	/**
	 * 创建并保存ObjectPool实例.
	 */
	@Override
	protected final void createPool() {
		logger.debug("Creating Commons object pool");
		this.pool = createObjectPool();
	}

	/**
	 * 如果子类想要返回特定的Commons池，则可以覆盖它.
	 * 他们应该在这里将任何配置属性应用于池.
	 * <p>默认是具有给定池大小的GenericObjectPool实例.
	 * 
	 * @return an empty Commons {@code ObjectPool}.
	 */
	protected ObjectPool createObjectPool() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(getMaxSize());
		config.setMaxIdle(getMaxIdle());
		config.setMinIdle(getMinIdle());
		config.setMaxWaitMillis(getMaxWait());
		config.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		config.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		config.setBlockWhenExhausted(isBlockWhenExhausted());
		return new GenericObjectPool(this, config);
	}


	/**
	 * 借用{@code ObjectPool}中的对象.
	 */
	@Override
	public Object getTarget() throws Exception {
		return this.pool.borrowObject();
	}

	/**
	 * 将指定对象返回给底层{@code ObjectPool}.
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
	 * 在销毁此对象时关闭底层{@code ObjectPool}.
	 */
	@Override
	public void destroy() throws Exception {
		logger.debug("Closing Commons ObjectPool");
		this.pool.close();
	}


	//----------------------------------------------------------------------------
	// Implementation of org.apache.commons.pool2.PooledObjectFactory interface
	//----------------------------------------------------------------------------

	@Override
	public PooledObject<Object> makeObject() throws Exception {
		return new DefaultPooledObject<Object>(newPrototypeInstance());
	}

	@Override
	public void destroyObject(PooledObject<Object> p) throws Exception {
		destroyPrototypeInstance(p.getObject());
	}

	@Override
	public boolean validateObject(PooledObject<Object> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<Object> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<Object> p) throws Exception {
	}

}
