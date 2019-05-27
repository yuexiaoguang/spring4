package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link ParameterNameDiscoverer}实现, 连续尝试几个发现者委托.
 * 那些在{@code addDiscoverer}方法中首先添加的方法具有最高优先级.
 * 如果返回{@code null}, 则会尝试下一个.
 *
 * <p>如果没有发现者匹配, 则默认行为是返回{@code null}.
 */
public class PrioritizedParameterNameDiscoverer implements ParameterNameDiscoverer {

	private final List<ParameterNameDiscoverer> parameterNameDiscoverers =
			new LinkedList<ParameterNameDiscoverer>();


	/**
	 * 将{@link ParameterNameDiscoverer}委托添加到此{@code PrioritizedParameterNameDiscoverer}检查的发现者列表中.
	 */
	public void addDiscoverer(ParameterNameDiscoverer pnd) {
		this.parameterNameDiscoverers.add(pnd);
	}


	@Override
	public String[] getParameterNames(Method method) {
		for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
			String[] result = pnd.getParameterNames(method);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public String[] getParameterNames(Constructor<?> ctor) {
		for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
			String[] result = pnd.getParameterNames(ctor);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

}
