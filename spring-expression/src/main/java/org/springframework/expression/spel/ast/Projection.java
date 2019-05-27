package org.springframework.expression.spel.ast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 表示投影, 其中对某些输入序列中的所有元素执行给定操作, 返回相同大小的新序列.
 * 例如:
 * "{1,2,3,4,5,6,7,8,9,10}.!{#isEven(#this)}" returns "[n, y, n, y, n, y, n, y, n, y]"
 */
public class Projection extends SpelNodeImpl {

	private final boolean nullSafe;


	public Projection(boolean nullSafe, int pos, SpelNodeImpl expression) {
		super(pos, expression);
		this.nullSafe = nullSafe;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		return getValueRef(state).getValue();
	}

	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		TypedValue op = state.getActiveContextObject();

		Object operand = op.getValue();
		boolean operandIsArray = ObjectUtils.isArray(operand);
		// TypeDescriptor operandTypeDescriptor = op.getTypeDescriptor();

		// 当输入是 map时, 在调用指定的操作之前在堆栈上推送特殊的上下文对象.
		// 这个特殊的上下文对象有两个字段'key'和'value', 它们引用了map条目键和值, 它们可以在操作中引用
		// eg. {'a':'y','b':'n'}.![value=='y'?key:null]" == ['a', null]
		if (operand instanceof Map) {
			Map<?, ?> mapData = (Map<?, ?>) operand;
			List<Object> result = new ArrayList<Object>();
			for (Map.Entry<?, ?> entry : mapData.entrySet()) {
				try {
					state.pushActiveContextObject(new TypedValue(entry));
					state.enterScope();
					result.add(this.children[0].getValueInternal(state).getValue());
				}
				finally {
					state.popActiveContextObject();
					state.exitScope();
				}
			}
			return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);  // TODO unable to build correct type descriptor
		}

		if (operand instanceof Iterable || operandIsArray) {
			Iterable<?> data = (operand instanceof Iterable ?
					(Iterable<?>) operand : Arrays.asList(ObjectUtils.toObjectArray(operand)));

			List<Object> result = new ArrayList<Object>();
			int idx = 0;
			Class<?> arrayElementType = null;
			for (Object element : data) {
				try {
					state.pushActiveContextObject(new TypedValue(element));
					state.enterScope("index", idx);
					Object value = this.children[0].getValueInternal(state).getValue();
					if (value != null && operandIsArray) {
						arrayElementType = determineCommonType(arrayElementType, value.getClass());
					}
					result.add(value);
				}
				finally {
					state.exitScope();
					state.popActiveContextObject();
				}
				idx++;
			}

			if (operandIsArray) {
				if (arrayElementType == null) {
					arrayElementType = Object.class;
				}
				Object resultArray = Array.newInstance(arrayElementType, result.size());
				System.arraycopy(result.toArray(), 0, resultArray, 0, result.size());
				return new ValueRef.TypedValueHolderValueRef(new TypedValue(resultArray),this);
			}

			return new ValueRef.TypedValueHolderValueRef(new TypedValue(result),this);
		}

		if (operand == null) {
			if (this.nullSafe) {
				return ValueRef.NullValueRef.INSTANCE;
			}
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE, "null");
		}

		throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE,
				operand.getClass().getName());
	}

	@Override
	public String toStringAST() {
		return "![" + getChild(0).toStringAST() + "]";
	}

	private Class<?> determineCommonType(Class<?> oldType, Class<?> newType) {
		if (oldType == null) {
			return newType;
		}
		if (oldType.isAssignableFrom(newType)) {
			return oldType;
		}
		Class<?> nextType = newType;
		while (nextType != Object.class) {
			if (nextType.isAssignableFrom(oldType)) {
				return nextType;
			}
			nextType = nextType.getSuperclass();
		}
		for (Class<?> nextInterface : ClassUtils.getAllInterfacesForClassAsSet(newType)) {
			if (nextInterface.isAssignableFrom(oldType)) {
				return nextInterface;
			}
		}
		return Object.class;
	}

}
