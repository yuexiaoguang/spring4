package org.springframework.util;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.net.ServerSocketFactory;

/**
 * 使用网络套接字的简单实用方法 &mdash; 例如, 在{@code localhost}上查找可用端口.
 *
 * <p>在此类中, TCP端口引用{@link ServerSocket}的端口; 而UDP端口是指{{@link DatagramSocket}的端口.
 */
public class SocketUtils {

	/**
	 * 查找可用套接字端口时, 使用的端口范围的默认最小值.
	 */
	public static final int PORT_RANGE_MIN = 1024;

	/**
	 * 查找可用套接字端口时, 使用的端口范围的默认最大值.
	 */
	public static final int PORT_RANGE_MAX = 65535;


	private static final Random random = new Random(System.currentTimeMillis());


	/**
	 * 虽然{@code SocketUtils}仅由静态方法组成, 但这个构造函数是故意{@code public}.
	 * <h4>理由</h4>
	 * <p>可以使用Spring Expression Language (SpEL) 和以下语法从XML配置文件中调用此类中的静态方法.
	 * <pre><code>&lt;bean id="bean1" ... p:port="#{T(org.springframework.util.SocketUtils).findAvailableTcpPort(12000)}" /&gt;</code></pre>
	 * 如果此构造函数是{@code private}, 则需要为SpEL的{@code T()}函数提供完全限定的类名以用于每种用法.
	 * 因此, 此构造函数为{@code public}的事实允许您使用SpEL减少样板配置, 如以下示例所示.
	 * <pre><code>&lt;bean id="socketUtils" class="org.springframework.util.SocketUtils" /&gt;
	 * &lt;bean id="bean1" ... p:port="#{socketUtils.findAvailableTcpPort(12000)}" /&gt;
	 * &lt;bean id="bean2" ... p:port="#{socketUtils.findAvailableTcpPort(30000)}" /&gt;</code></pre>
	 */
	public SocketUtils() {
		/* no-op */
	}


	/**
	 * 查找从[{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}]范围中随机选择的可用TCP端口.
	 * 
	 * @return 可用的TCP端口号
	 * @throws IllegalStateException 如果找不到可用的端口
	 */
	public static int findAvailableTcpPort() {
		return findAvailableTcpPort(PORT_RANGE_MIN);
	}

	/**
	 * 查找从[{@code minPort}, {@value #PORT_RANGE_MAX}]范围中随机选择的可用TCP端口.
	 * 
	 * @param minPort 最小端口号
	 * 
	 * @return 可用的TCP端口号
	 * @throws IllegalStateException 如果找不到可用的端口
	 */
	public static int findAvailableTcpPort(int minPort) {
		return findAvailableTcpPort(minPort, PORT_RANGE_MAX);
	}

	/**
	 * 查找从[{@code minPort}, {@code maxPort}]范围中随机选择的可用TCP端口.
	 * 
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * 
	 * @return 可用的TCP端口号
	 * @throws IllegalStateException 如果找不到可用的端口
	 */
	public static int findAvailableTcpPort(int minPort, int maxPort) {
		return SocketType.TCP.findAvailablePort(minPort, maxPort);
	}

	/**
	 * 查找所请求的可用TCP端口数, 每个端口从[{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}]范围中随机选择.
	 * 
	 * @param numRequested 要查找的可用端口数
	 * 
	 * @return 一组有序的可用TCP端口号
	 * @throws IllegalStateException 如果找不到请求的可用端口数
	 */
	public static SortedSet<Integer> findAvailableTcpPorts(int numRequested) {
		return findAvailableTcpPorts(numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	/**
	 * 找到所请求的可用TCP端口数, 每个端口随机选自[{@code minPort}, {@code maxPort}]范围.
	 * 
	 * @param numRequested 要查找的可用端口数
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * 
	 * @return 一组有序的可用TCP端口号
	 * @throws IllegalStateException 如果找不到请求的可用端口数
	 */
	public static SortedSet<Integer> findAvailableTcpPorts(int numRequested, int minPort, int maxPort) {
		return SocketType.TCP.findAvailablePorts(numRequested, minPort, maxPort);
	}

	/**
	 * 查找从[{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}]范围中随机选择的可用UDP端口.
	 * 
	 * @return 可用的UDP端口号
	 * @throws IllegalStateException 如果找不到可用的端口
	 */
	public static int findAvailableUdpPort() {
		return findAvailableUdpPort(PORT_RANGE_MIN);
	}

	/**
	 * 查找从 [{@code minPort}, {@value #PORT_RANGE_MAX}]范围中随机选择的可用UDP端口.
	 * 
	 * @param minPort 最小端口号
	 * 
	 * @return 可用的UDP端口号
	 * @throws IllegalStateException 如果找不到可用的端口
	 */
	public static int findAvailableUdpPort(int minPort) {
		return findAvailableUdpPort(minPort, PORT_RANGE_MAX);
	}

	/**
	 * 查找从[{@code minPort}, {@code maxPort}]范围中随机选择的可用UDP端口.
	 * 
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * 
	 * @return 可用的UDP端口号
	 * @throws IllegalStateException 如果找不到可用的端口
	 */
	public static int findAvailableUdpPort(int minPort, int maxPort) {
		return SocketType.UDP.findAvailablePort(minPort, maxPort);
	}

	/**
	 * 查找所请求的可用UDP端口数, 每个端口从[{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}]范围中随机选择.
	 * 
	 * @param numRequested 要查找的可用端口数
	 * 
	 * @return 一组有序的可用UDP端口号
	 * @throws IllegalStateException 如果找不到请求的可用端口数
	 */
	public static SortedSet<Integer> findAvailableUdpPorts(int numRequested) {
		return findAvailableUdpPorts(numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	/**
	 * 查找所请求的可用UDP端口数, 每个端口从[{@code minPort}, {@code maxPort}]范围中随机选择.
	 * 
	 * @param numRequested 要查找的可用端口数
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * 
	 * @return 一组有序的可用UDP端口号
	 * @throws IllegalStateException 如果找不到请求的可用端口数
	 */
	public static SortedSet<Integer> findAvailableUdpPorts(int numRequested, int minPort, int maxPort) {
		return SocketType.UDP.findAvailablePorts(numRequested, minPort, maxPort);
	}


	private enum SocketType {

		TCP {
			@Override
			protected boolean isPortAvailable(int port) {
				try {
					ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
							port, 1, InetAddress.getByName("localhost"));
					serverSocket.close();
					return true;
				}
				catch (Exception ex) {
					return false;
				}
			}
		},

		UDP {
			@Override
			protected boolean isPortAvailable(int port) {
				try {
					DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
					socket.close();
					return true;
				}
				catch (Exception ex) {
					return false;
				}
			}
		};

		/**
		 * 确定此{@code SocketType}的指定端口当前是否可用于{@code localhost}.
		 */
		protected abstract boolean isPortAvailable(int port);

		/**
		 * 查找[{@code minPort}, {@code maxPort}]范围内的伪随机端口号.
		 * 
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * 
		 * @return 指定范围内的随机端口号
		 */
		private int findRandomPort(int minPort, int maxPort) {
			int portRange = maxPort - minPort;
			return minPort + random.nextInt(portRange + 1);
		}

		/**
		 * 查找{@code SocketType}的可用端口, 从[{@code minPort}, {@code maxPort}]范围中随机选择.
		 * 
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * 
		 * @return 此套接字类型的可用端口号
		 * @throws IllegalStateException 如果找不到可用的端口
		 */
		int findAvailablePort(int minPort, int maxPort) {
			Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
			Assert.isTrue(maxPort >= minPort, "'maxPort' must be greater than or equals 'minPort'");
			Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);

			int portRange = maxPort - minPort;
			int candidatePort;
			int searchCounter = 0;
			do {
				if (++searchCounter > portRange) {
					throw new IllegalStateException(String.format(
							"Could not find an available %s port in the range [%d, %d] after %d attempts",
							name(), minPort, maxPort, searchCounter));
				}
				candidatePort = findRandomPort(minPort, maxPort);
			}
			while (!isPortAvailable(candidatePort));

			return candidatePort;
		}

		/**
		 * 查找{@code SocketType}所请求的可用端口数, 每个端口都是从 [{@code minPort}, {@code maxPort}]范围内随机选择的.
		 * 
		 * @param numRequested 要查找的可用端口数
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * 
		 * @return 此套接字类型的一组已排序的可用端口号
		 * @throws IllegalStateException 如果找不到请求的可用端口数
		 */
		SortedSet<Integer> findAvailablePorts(int numRequested, int minPort, int maxPort) {
			Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
			Assert.isTrue(maxPort > minPort, "'maxPort' must be greater than 'minPort'");
			Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);
			Assert.isTrue(numRequested > 0, "'numRequested' must be greater than 0");
			Assert.isTrue((maxPort - minPort) >= numRequested,
					"'numRequested' must not be greater than 'maxPort' - 'minPort'");

			SortedSet<Integer> availablePorts = new TreeSet<Integer>();
			int attemptCount = 0;
			while ((++attemptCount <= numRequested + 100) && availablePorts.size() < numRequested) {
				availablePorts.add(findAvailablePort(minPort, maxPort));
			}

			if (availablePorts.size() != numRequested) {
				throw new IllegalStateException(String.format(
						"Could not find %d available %s ports in the range [%d, %d]",
						numRequested, name(), minPort, maxPort));
			}

			return availablePorts;
		}
	}
}
