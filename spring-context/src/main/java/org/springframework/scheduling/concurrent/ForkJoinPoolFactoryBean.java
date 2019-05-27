package org.springframework.scheduling.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.UsesJava7;

/**
 * Spring {@link FactoryBean}, 用于构建和公开预配置的{@link ForkJoinPool}.
 * 可以在Java 7和8以及Java 6上的类路径上的{@code jsr166.jar}使用 (理想情况下在VM引导程序类路径上).
 *
 * <p>有关ForkJoinPool API及其与RecursiveActions一起使用的详细信息, see the
 * <a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ForkJoinPool.html">JDK 7 javadoc</a>.
 *
 * <p>{@code jsr166.jar}, 包含Java 6的{@code java.util.concurrent}更新, 可以从
 * <a href="http://gee.cs.oswego.edu/dl/concurrency-interest/">concurrency interest website</a>获取.
 */
@UsesJava7
public class ForkJoinPoolFactoryBean implements FactoryBean<ForkJoinPool>, InitializingBean, DisposableBean {

	private boolean commonPool = false;

	private int parallelism = Runtime.getRuntime().availableProcessors();

	private ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	private boolean asyncMode = false;

	private int awaitTerminationSeconds = 0;

	private ForkJoinPool forkJoinPool;


	/**
	 * 设置是否公开JDK 8的'common' {@link ForkJoinPool}.
	 * <p>默认"false", 创建一个本地{@link ForkJoinPool}实例, 基于此FactoryBean上的
	 * {@link #setParallelism "parallelism"}, {@link #setThreadFactory "threadFactory"},
	 * {@link #setUncaughtExceptionHandler "uncaughtExceptionHandler"},
	 * {@link #setAsyncMode "asyncMode"}属性.
	 * <p><b>NOTE:</b>将此标志设置为"true", 会有效地忽略此FactoryBean上的所有其他属性, 而是重用共享的公共JDK {@link ForkJoinPool}.
	 * 这是JDK 8上的一个很好的选择, 但确实删除了应用程序自定义ForkJoinPool行为的能力, 特别是使用自定义线程.
	 */
	public void setCommonPool(boolean commonPool) {
		this.commonPool = commonPool;
	}

	/**
	 * 指定并行度级别. 默认{@link Runtime#availableProcessors()}.
	 */
	public void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}

	/**
	 * 设置创建新的ForkJoinWorkerThreads的工厂.
	 * 默认{@link ForkJoinPool#defaultForkJoinWorkerThreadFactory}.
	 */
	public void setThreadFactory(ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * 设置处理器, 用于执行任务时遇到的不可恢复错误而终止的内部工作者线程.
	 * 默认无.
	 */
	public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
	}

	/**
	 * 指定是否为从未合并的分叉任务建立本地先进先出调度模式.
	 * 此模式 (asyncMode = {@code true})可能比, 应用程序中的默认本地的基于堆栈的模式(工作线程仅处理事件样式的异步任务)更合适.
	 * 默认{@code false}.
	 */
	public void setAsyncMode(boolean asyncMode) {
		this.asyncMode = asyncMode;
	}

	/**
	 * 设置此ForkJoinPool在关闭时应阻塞的最大秒数, 以便在容器的其余部分继续关闭之前等待剩余任务完成执行.
	 * 如果剩余任务可能需要访问也由容器管理的其他资源, 则此功能尤其有用.
	 * <p>默认情况下, 此ForkJoinPool不会等待任务终止.
	 * 它将继续完全执行所有正在进行的任务以及队列中的所有剩余任务, 与容器的其余部分并行关闭.
	 * 相反, 如果使用此属性指定等待终止周期, 则此执行器将等待给定时间(最大值)以终止任务.
	 * <p>请注意, 此功能也适用于{@link #setCommonPool "commonPool"}模式.
	 * 底层的ForkJoinPool在这种情况下实际上不会终止, 但会等待所有任务终止.
	 */
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationSeconds = awaitTerminationSeconds;
	}

	@Override
	public void afterPropertiesSet() {
		this.forkJoinPool = (this.commonPool ? ForkJoinPool.commonPool() :
				new ForkJoinPool(this.parallelism, this.threadFactory, this.uncaughtExceptionHandler, this.asyncMode));
	}


	@Override
	public ForkJoinPool getObject() {
		return this.forkJoinPool;
	}

	@Override
	public Class<?> getObjectType() {
		return ForkJoinPool.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		// 忽略公共池.
		this.forkJoinPool.shutdown();

		// 等待所有任务终止 - 也适用于公共池.
		if (this.awaitTerminationSeconds > 0) {
			try {
				this.forkJoinPool.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
