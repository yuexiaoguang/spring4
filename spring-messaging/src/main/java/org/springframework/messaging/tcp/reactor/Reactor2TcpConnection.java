package org.springframework.messaging.tcp.reactor;

import reactor.io.net.ChannelStream;
import reactor.rx.Promise;
import reactor.rx.Promises;
import reactor.rx.Streams;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 基于Reactor项目的TCP客户端支持的{@link org.springframework.messaging.tcp.TcpConnection TcpConnection}的实现.
 *
 * @param <P> 读取或写入TCP流的消息的有效负载类型
 */
public class Reactor2TcpConnection<P> implements TcpConnection<P> {

	private final ChannelStream<Message<P>, Message<P>> channelStream;

	private final Promise<Void> closePromise;


	public Reactor2TcpConnection(ChannelStream<Message<P>, Message<P>> channelStream, Promise<Void> closePromise) {
		this.channelStream = channelStream;
		this.closePromise = closePromise;
	}


	@Override
	public ListenableFuture<Void> send(Message<P> message) {
		Promise<Void> afterWrite = Promises.prepare();
		this.channelStream.writeWith(Streams.just(message)).subscribe(afterWrite);
		return new PassThroughPromiseToListenableFutureAdapter<Void>(afterWrite);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onReadInactivity(Runnable runnable, long inactivityDuration) {
		this.channelStream.on().readIdle(inactivityDuration, reactor.fn.Functions.<Void>consumer(runnable));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onWriteInactivity(Runnable runnable, long inactivityDuration) {
		this.channelStream.on().writeIdle(inactivityDuration, reactor.fn.Functions.<Void>consumer(runnable));
	}

	@Override
	public void close() {
		this.closePromise.onComplete();
	}

}
