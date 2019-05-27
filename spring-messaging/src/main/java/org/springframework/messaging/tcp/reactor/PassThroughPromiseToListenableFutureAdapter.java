package org.springframework.messaging.tcp.reactor;

import reactor.rx.Promise;

/**
 * 其中来自Promise和ListenableFuture的源和目标分别属于同一类型.
 */
class PassThroughPromiseToListenableFutureAdapter<T> extends AbstractPromiseToListenableFutureAdapter<T, T> {


	public PassThroughPromiseToListenableFutureAdapter(Promise<T> promise) {
		super(promise);
	}


	@Override
	protected T adapt(T result) {
		return result;
	}

}
