package org.springframework.util.comparator;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Boolean对象的比较器, 可以先对true或false进行排序.
 */
@SuppressWarnings("serial")
public final class BooleanComparator implements Comparator<Boolean>, Serializable {

	/**
	 * 此比较器的共享默认实例, 处理true低于false.
	 */
	public static final BooleanComparator TRUE_LOW = new BooleanComparator(true);

	/**
	 * 此比较器的共享默认实例, 处理true高于false.
	 */
	public static final BooleanComparator TRUE_HIGH = new BooleanComparator(false);


	private final boolean trueLow;


	/**
	 * 创建一个BooleanComparator, 根据提供的标志对布尔值进行排序.
	 * <p>或者, 可以使用默认的共享实例:
	 * {@code BooleanComparator.TRUE_LOW} 和 {@code BooleanComparator.TRUE_HIGH}.
	 * 
	 * @param trueLow 是否将true视为低于或高于false
	 */
	public BooleanComparator(boolean trueLow) {
		this.trueLow = trueLow;
	}


	@Override
	public int compare(Boolean v1, Boolean v2) {
		return (v1 ^ v2) ? ((v1 ^ this.trueLow) ? 1 : -1) : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BooleanComparator)) {
			return false;
		}
		return (this.trueLow == ((BooleanComparator) obj).trueLow);
	}

	@Override
	public int hashCode() {
		return (this.trueLow ? -1 : 1) * getClass().hashCode();
	}

	@Override
	public String toString() {
		return "BooleanComparator: " + (this.trueLow ? "true low" : "true high");
	}

}
