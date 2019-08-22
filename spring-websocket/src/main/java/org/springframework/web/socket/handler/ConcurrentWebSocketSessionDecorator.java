package org.springframework.web.socket.handler;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 包装{@link org.springframework.web.socket.WebSocketSession WebSocketSession},
 * 以确保一次只有一个线程可以发送消息.
 *
 * <p>如果发送速度很慢, 后续其他线程尝试发送更多消息将无法获取刷新锁定, 转而将缓冲消息.
 * 此时, 将检查指定的缓冲区大小限制和发送时间限制, 如果超出限制, 将关闭会话.
 */
public class ConcurrentWebSocketSessionDecorator extends WebSocketSessionDecorator {

	private static final Log logger = LogFactory.getLog(ConcurrentWebSocketSessionDecorator.class);


	private final int sendTimeLimit;

	private final int bufferSizeLimit;

	private final Queue<WebSocketMessage<?>> buffer = new LinkedBlockingQueue<WebSocketMessage<?>>();

	private final AtomicInteger bufferSize = new AtomicInteger();

	private volatile long sendStartTime;

	private volatile boolean limitExceeded;

	private volatile boolean closeInProgress;

	private final Lock flushLock = new ReentrantLock();

	private final Lock closeLock = new ReentrantLock();


	/**
	 * @param delegate 要委托给的{@code WebSocketSession}
	 * @param sendTimeLimit 发送时间限制 (毫秒)
	 * @param bufferSizeLimit 缓冲区大小限制 (字节数)
	 */
	public ConcurrentWebSocketSessionDecorator(WebSocketSession delegate, int sendTimeLimit, int bufferSizeLimit) {
		super(delegate);
		this.sendTimeLimit = sendTimeLimit;
		this.bufferSizeLimit = bufferSizeLimit;
	}


	/**
	 * 返回配置的发送时间限制 (毫秒).
	 */
	public int getSendTimeLimit() {
		return this.sendTimeLimit;
	}

	/**
	 * 返回配置的缓冲区大小限制 (字节数).
	 */
	public int getBufferSizeLimit() {
		return this.bufferSizeLimit;
	}

	/**
	 * 返回当前缓冲区大小 (字节数).
	 */
	public int getBufferSize() {
		return this.bufferSize.get();
	}

	/**
	 * 返回自当前发送开始以来的时间 (毫秒), 如果当前没有发送, 则返回0.
	 */
	public long getTimeSinceSendStarted() {
		long start = this.sendStartTime;
		return (start > 0 ? (System.currentTimeMillis() - start) : 0);
	}


	@Override
	public void sendMessage(WebSocketMessage<?> message) throws IOException {
		if (shouldNotSend()) {
			return;
		}

		this.buffer.add(message);
		this.bufferSize.addAndGet(message.getPayloadLength());

		do {
			if (!tryFlushMessageBuffer()) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Another send already in progress: " +
							"session id '%s':, \"in-progress\" send time %d (ms), buffer size %d bytes",
							getId(), getTimeSinceSendStarted(), getBufferSize()));
				}
				checkSessionLimits();
				break;
			}
		}
		while (!this.buffer.isEmpty() && !shouldNotSend());
	}

	private boolean shouldNotSend() {
		return (this.limitExceeded || this.closeInProgress);
	}

	private boolean tryFlushMessageBuffer() throws IOException {
		if (this.flushLock.tryLock()) {
			try {
				while (true) {
					WebSocketMessage<?> message = this.buffer.poll();
					if (message == null || shouldNotSend()) {
						break;
					}
					this.bufferSize.addAndGet(message.getPayloadLength() * -1);
					this.sendStartTime = System.currentTimeMillis();
					getDelegate().sendMessage(message);
					this.sendStartTime = 0;
				}
			}
			finally {
				this.sendStartTime = 0;
				this.flushLock.unlock();
			}
			return true;
		}
		return false;
	}

	private void checkSessionLimits() {
		if (!shouldNotSend() && this.closeLock.tryLock()) {
			try {
				if (getTimeSinceSendStarted() > getSendTimeLimit()) {
					String format = "Message send time %d (ms) for session '%s' exceeded the allowed limit %d";
					String reason = String.format(format, getTimeSinceSendStarted(), getId(), getSendTimeLimit());
					limitExceeded(reason);
				}
				else if (getBufferSize() > getBufferSizeLimit()) {
					String format = "The send buffer size %d bytes for session '%s' exceeded the allowed limit %d";
					String reason = String.format(format, getBufferSize(), getId(), getBufferSizeLimit());
					limitExceeded(reason);
				}
			}
			finally {
				this.closeLock.unlock();
			}
		}
	}

	private void limitExceeded(String reason) {
		this.limitExceeded = true;
		throw new SessionLimitExceededException(reason, CloseStatus.SESSION_NOT_RELIABLE);
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		this.closeLock.lock();
		try {
			if (this.closeInProgress) {
				return;
			}
			if (!CloseStatus.SESSION_NOT_RELIABLE.equals(status)) {
				try {
					checkSessionLimits();
				}
				catch (SessionLimitExceededException ex) {
					// Ignore
				}
				if (this.limitExceeded) {
					if (logger.isDebugEnabled()) {
						logger.debug("Changing close status " + status + " to SESSION_NOT_RELIABLE.");
					}
					status = CloseStatus.SESSION_NOT_RELIABLE;
				}
			}
			this.closeInProgress = true;
			super.close(status);
		}
		finally {
			this.closeLock.unlock();
		}
	}


	@Override
	public String toString() {
		return getDelegate().toString();
	}

}
