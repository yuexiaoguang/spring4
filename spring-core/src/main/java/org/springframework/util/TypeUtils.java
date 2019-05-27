package org.springframework.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * 使用Java 5泛型类型参数的实用程序.
 * 主要供框架内部使用.
 */
public abstract class TypeUtils {

	/**
	 * 检查是否可以按照Java泛型规则, 将右侧类型分配给左侧类型.
	 * 
	 * @param lhsType 目标类型
	 * @param rhsType 应分配给目标类型的值类型
	 * 
	 * @return true 如果rhs可分配给lhs
	 */
	public static boolean isAssignable(Type lhsType, Type rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");

		// 所有类型都可以分配给自己和类Object
		if (lhsType.equals(rhsType) || Object.class == lhsType) {
			return true;
		}

		if (lhsType instanceof Class) {
			Class<?> lhsClass = (Class<?>) lhsType;

			// 只比较两个类
			if (rhsType instanceof Class) {
				return ClassUtils.isAssignable(lhsClass, (Class<?>) rhsType);
			}

			if (rhsType instanceof ParameterizedType) {
				Type rhsRaw = ((ParameterizedType) rhsType).getRawType();

				// 参数化类型始终可分配给其原始类类型
				if (rhsRaw instanceof Class) {
					return ClassUtils.isAssignable(lhsClass, (Class<?>) rhsRaw);
				}
			}
			else if (lhsClass.isArray() && rhsType instanceof GenericArrayType) {
				Type rhsComponent = ((GenericArrayType) rhsType).getGenericComponentType();

				return isAssignable(lhsClass.getComponentType(), rhsComponent);
			}
		}

		// 参数化类型只能分配给其他参数化类型和类类型
		if (lhsType instanceof ParameterizedType) {
			if (rhsType instanceof Class) {
				Type lhsRaw = ((ParameterizedType) lhsType).getRawType();

				if (lhsRaw instanceof Class) {
					return ClassUtils.isAssignable((Class<?>) lhsRaw, (Class<?>) rhsType);
				}
			}
			else if (rhsType instanceof ParameterizedType) {
				return isAssignable((ParameterizedType) lhsType, (ParameterizedType) rhsType);
			}
		}

		if (lhsType instanceof GenericArrayType) {
			Type lhsComponent = ((GenericArrayType) lhsType).getGenericComponentType();

			if (rhsType instanceof Class) {
				Class<?> rhsClass = (Class<?>) rhsType;

				if (rhsClass.isArray()) {
					return isAssignable(lhsComponent, rhsClass.getComponentType());
				}
			}
			else if (rhsType instanceof GenericArrayType) {
				Type rhsComponent = ((GenericArrayType) rhsType).getGenericComponentType();

				return isAssignable(lhsComponent, rhsComponent);
			}
		}

		if (lhsType instanceof WildcardType) {
			return isAssignable((WildcardType) lhsType, rhsType);
		}

		return false;
	}

	private static boolean isAssignable(ParameterizedType lhsType, ParameterizedType rhsType) {
		if (lhsType.equals(rhsType)) {
			return true;
		}

		Type[] lhsTypeArguments = lhsType.getActualTypeArguments();
		Type[] rhsTypeArguments = rhsType.getActualTypeArguments();

		if (lhsTypeArguments.length != rhsTypeArguments.length) {
			return false;
		}

		for (int size = lhsTypeArguments.length, i = 0; i < size; ++i) {
			Type lhsArg = lhsTypeArguments[i];
			Type rhsArg = rhsTypeArguments[i];

			if (!lhsArg.equals(rhsArg) &&
					!(lhsArg instanceof WildcardType && isAssignable((WildcardType) lhsArg, rhsArg))) {
				return false;
			}
		}

		return true;
	}

	private static boolean isAssignable(WildcardType lhsType, Type rhsType) {
		Type[] lUpperBounds = lhsType.getUpperBounds();

		// 如果没有指定, 则提供隐式上限
		if (lUpperBounds.length == 0) {
			lUpperBounds = new Type[] { Object.class };
		}

		Type[] lLowerBounds = lhsType.getLowerBounds();

		// 如果未指定, 则提供隐式下限
		if (lLowerBounds.length == 0) {
			lLowerBounds = new Type[] { null };
		}

		if (rhsType instanceof WildcardType) {
			// 右手边的上下界必须完全包围在左手边的上下界.
			WildcardType rhsWcType = (WildcardType) rhsType;
			Type[] rUpperBounds = rhsWcType.getUpperBounds();

			if (rUpperBounds.length == 0) {
				rUpperBounds = new Type[] { Object.class };
			}

			Type[] rLowerBounds = rhsWcType.getLowerBounds();

			if (rLowerBounds.length == 0) {
				rLowerBounds = new Type[] { null };
			}

			for (Type lBound : lUpperBounds) {
				for (Type rBound : rUpperBounds) {
					if (!isAssignableBound(lBound, rBound)) {
						return false;
					}
				}

				for (Type rBound : rLowerBounds) {
					if (!isAssignableBound(lBound, rBound)) {
						return false;
					}
				}
			}

			for (Type lBound : lLowerBounds) {
				for (Type rBound : rUpperBounds) {
					if (!isAssignableBound(rBound, lBound)) {
						return false;
					}
				}

				for (Type rBound : rLowerBounds) {
					if (!isAssignableBound(rBound, lBound)) {
						return false;
					}
				}
			}
		}
		else {
			for (Type lBound : lUpperBounds) {
				if (!isAssignableBound(lBound, rhsType)) {
					return false;
				}
			}

			for (Type lBound : lLowerBounds) {
				if (!isAssignableBound(rhsType, lBound)) {
					return false;
				}
			}
		}

		return true;
	}

	public static boolean isAssignableBound(Type lhsType, Type rhsType) {
		if (rhsType == null) {
			return true;
		}
		if (lhsType == null) {
			return false;
		}
		return isAssignable(lhsType, rhsType);
	}
}
