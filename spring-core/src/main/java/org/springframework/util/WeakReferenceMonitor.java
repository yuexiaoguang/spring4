package org.springframework.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 使用代理和弱引用跟踪对任意对象的引用.
 * 要监视句柄, 应该调用{@link #monitor(Object, ReleaseListener)}, 给定的句柄对象通常是使用下面的目标对象的持有者,
 * 并且一旦不再强引用句柄, 释放监听器就会执行目标对象的清理.
 *
 * <p>当给定句柄变得弱可达时, 后台线程将调用指定的监听器.
 * 此线程只会延迟启动, 并且一旦没有注册要进行监视的句柄就会停止, 如果添加了更多句柄则重新启动.
 *
 * <p>Thanks to Tomasz Wysocki for the suggestion and the original implementation of this class!
 *
 * @deprecated as of Spring Framework 4.3.6
 */
@Deprecated
public class WeakReferenceMonitor {

	private static final Log logger = LogFactory.getLog(WeakReferenceMonitor.class);

	// 接收可达性事件的队列
	private static final ReferenceQueue<Object> handleQueue = new ReferenceQueue<Object>();

	// 所有跟踪的条目 (WeakReference => ReleaseListener)
	private static final Map<Reference<?>, ReleaseListener> trackedEntries = new HashMap<Reference<?>, ReleaseListener>();

	// 线程轮询handleQueue, 延迟初始化
	private static Thread monitoringThread = null;


	/**
	 * 开始监视给定的句柄对象是否变得弱可达.
	 * 当不再使用句柄时, 将调用给定的监听器.
	 * 
	 * @param handle 将被监控的对象
	 * @param listener 释放句柄时将调用的监听器
	 */
	public static void monitor(Object handle, ReleaseListener listener) {
		if (logger.isDebugEnabled()) {
			logger.debug("Monitoring handle [" + handle + "] with release listener [" + listener + "]");
		}

		// 对此句柄进行弱引用, 因此可以通过轮询handleQueue来说明何时不再使用句柄.
		WeakReference<Object> weakRef = new WeakReference<Object>(handle, handleQueue);

		// 将受监视的条目添加到所有受监视条目的内部Map.
		addEntry(weakRef, listener);
	}

	/**
	 * 将条目添加到跟踪条目的内部Map.
	 * 如果尚未运行, 则启动内部监视线程.
	 * 
	 * @param ref 对跟踪的句柄的引用
	 * @param entry 关联的条目
	 */
	private static void addEntry(Reference<?> ref, ReleaseListener entry) {
		synchronized (WeakReferenceMonitor.class) {
			// Add entry, the key is given reference.
			trackedEntries.put(ref, entry);

			// Start monitoring thread lazily.
			if (monitoringThread == null) {
				monitoringThread = new Thread(new MonitoringProcess(), WeakReferenceMonitor.class.getName());
				monitoringThread.setDaemon(true);
				monitoringThread.start();
			}
		}
	}

	/**
	 * 从已跟踪条目的内部Map中删除条目.
	 * 
	 * @param reference 应该删除的引用
	 * 
	 * @return 与给定引用相关联的条目对象
	 */
	private static ReleaseListener removeEntry(Reference<?> reference) {
		synchronized (WeakReferenceMonitor.class) {
			return trackedEntries.remove(reference);
		}
	}

	/**
	 * 检查是否保持监视线程处于活动状态, i.e. 是否仍有被跟踪的条目.
	 */
	private static boolean keepMonitoringThreadAlive() {
		synchronized (WeakReferenceMonitor.class) {
			if (!trackedEntries.isEmpty()) {
				return true;
			}
			else {
				logger.debug("No entries left to track - stopping reference monitor thread");
				monitoringThread = null;
				return false;
			}
		}
	}


	/**
	 * 执行实际监视的线程实现.
	 */
	private static class MonitoringProcess implements Runnable {

		@Override
		public void run() {
			logger.debug("Starting reference monitor thread");
			// Check if there are any tracked entries left.
			while (keepMonitoringThreadAlive()) {
				try {
					Reference<?> reference = handleQueue.remove();
					// Stop tracking this reference.
					ReleaseListener entry = removeEntry(reference);
					if (entry != null) {
						// Invoke listener callback.
						try {
							entry.released();
						}
						catch (Throwable ex) {
							logger.warn("Reference release listener threw exception", ex);
						}
					}
				}
				catch (InterruptedException ex) {
					synchronized (WeakReferenceMonitor.class) {
						monitoringThread = null;
					}
					logger.debug("Reference monitor thread interrupted", ex);
					break;
				}
			}
		}
	}


	/**
	 * 释放句柄时通知的监听器.
	 * 由此引用监视器的用户实现.
	 */
	public static interface ReleaseListener {

		/**
		 * 一旦释放了相关的句柄, 就会调用此回调方法, i.e. 一旦没有受监控的强引用句柄了.
		 */
		void released();
	}

}
