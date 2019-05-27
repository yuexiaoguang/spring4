package org.springframework.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.util.Assert;

/**
 * 抽象类, 它将通过S参数化的{@link Future}适配为通过T参数化的{@code Future}.
 * 所有方法都委托给适配器, 其中{@link #get()}和{@link #get(long, TimeUnit)}在适配器的结果上调用{@link #adapt(Object)}.
 *
 * @param <T> 这个{@code Future}的类型
 * @param <S> 适配器的{@code Future}的类型
 */
public abstract class FutureAdapter<T, S> implements Future<T> {

	private final Future<S> adaptee;

	private Object result = null;

	private State state = State.NEW;

	private final Object mutex = new Object();


	/**
	 * @param adaptee 要委托给的Future
	 */
	protected FutureAdapter(Future<S> adaptee) {
		Assert.notNull(adaptee, "Delegate must not be null");
		this.adaptee = adaptee;
	}


	/**
	 * 返回适配器.
	 */
	protected Future<S> getAdaptee() {
		return this.adaptee;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.adaptee.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return this.adaptee.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.adaptee.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return adaptInternal(this.adaptee.get());
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return adaptInternal(this.adaptee.get(timeout, unit));
	}

	@SuppressWarnings("unchecked")
	final T adaptInternal(S adapteeResult) throws ExecutionException {
		synchronized (this.mutex) {
			switch (this.state) {
				case SUCCESS:
					return (T) this.result;
				case FAILURE:
					throw (ExecutionException) this.result;
				case NEW:
					try {
						T adapted = adapt(adapteeResult);
						this.result = adapted;
						this.state = State.SUCCESS;
						return adapted;
					}
					catch (ExecutionException ex) {
						this.result = ex;
						this.state = State.FAILURE;
						throw ex;
					}
					catch (Throwable ex) {
						ExecutionException execEx = new ExecutionException(ex);
						this.result = execEx;
						this.state = State.FAILURE;
						throw execEx;
					}
				default:
					throw new IllegalStateException();
			}
		}
	}

	/**
	 * 使给定的适配器的结果适配为T.
	 * 
	 * @return 适配后的结果
	 */
	protected abstract T adapt(S adapteeResult) throws ExecutionException;


	private enum State {NEW, SUCCESS, FAILURE}

}
