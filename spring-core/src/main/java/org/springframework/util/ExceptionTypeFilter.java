package org.springframework.util;

import java.util.Collection;

/**
 * 用于处理异常类型的{@link InstanceFilter}实现.
 * 如果类型可分配给该候选者, 则该类型将与给定候选者匹配.
 */
public class ExceptionTypeFilter extends InstanceFilter<Class<? extends Throwable>> {

	public ExceptionTypeFilter(Collection<? extends Class<? extends Throwable>> includes,
			Collection<? extends Class<? extends Throwable>> excludes, boolean matchIfEmpty) {

		super(includes, excludes, matchIfEmpty);
	}

	@Override
	protected boolean match(Class<? extends Throwable> instance, Class<? extends Throwable> candidate) {
		return candidate.isAssignableFrom(instance);
	}

}
