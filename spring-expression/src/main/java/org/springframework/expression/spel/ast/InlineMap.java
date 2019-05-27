package org.springframework.expression.spel.ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;

/**
 * 表示表达式中的Map, e.g. '{name:'foo',age:12}'
 */
public class InlineMap extends SpelNodeImpl {

	// 如果Map纯粹是文字, 则它是一个常量值, 可以计算和缓存
	private TypedValue constant = null;


	public InlineMap(int pos, SpelNodeImpl... args) {
		super(pos, args);
		checkIfConstant();
	}


	/**
	 * 如果列表的所有组件都是常量, 或者列表/映射本身包含常量, 那么可以构建常量列表来表示此节点.
	 * 这将加快以后的getValue调用, 并减少创建的垃圾量.
	 */
	private void checkIfConstant() {
		boolean isConstant = true;
		for (int c = 0, max = getChildCount(); c < max; c++) {
			SpelNode child = getChild(c);
			if (!(child instanceof Literal)) {
				if (child instanceof InlineList) {
					InlineList inlineList = (InlineList) child;
					if (!inlineList.isConstant()) {
						isConstant = false;
						break;
					}
				}
				else if (child instanceof InlineMap) {
					InlineMap inlineMap = (InlineMap) child;
					if (!inlineMap.isConstant()) {
						isConstant = false;
						break;
					}
				}
				else if (!((c%2)==0 && (child instanceof PropertyOrFieldReference))) {					
					isConstant = false;
					break;
				}
			}
		}
		if (isConstant) {
			Map<Object,Object> constantMap = new LinkedHashMap<Object,Object>();			
			int childCount = getChildCount();
			for (int c = 0; c < childCount; c++) {
				SpelNode keyChild = getChild(c++);
				SpelNode valueChild = getChild(c);
				Object key = null;
				Object value = null;
				if (keyChild instanceof Literal) {
					key = ((Literal) keyChild).getLiteralValue().getValue();
				}
				else if (keyChild instanceof PropertyOrFieldReference) {
					key = ((PropertyOrFieldReference) keyChild).getName();
				}
				else {
					return;
				}
				if (valueChild instanceof Literal) {
					value = ((Literal) valueChild).getLiteralValue().getValue();
				}
				else if (valueChild instanceof InlineList) {
					value = ((InlineList) valueChild).getConstantValue();
				}
				else if (valueChild instanceof InlineMap) {
					value = ((InlineMap) valueChild).getConstantValue();
				}
				constantMap.put(key, value);
			}
			this.constant = new TypedValue(Collections.unmodifiableMap(constantMap));
		}
	}

	@Override
	public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
		if (this.constant != null) {
			return this.constant;
		}
		else {
			Map<Object, Object> returnValue = new LinkedHashMap<Object, Object>();
			int childcount = getChildCount();
			for (int c = 0; c < childcount; c++) {
				// TODO 允许键是PropertyOrFieldReference, 如Map上的Indexer
				SpelNode keyChild = getChild(c++);
				Object key = null;
				if (keyChild instanceof PropertyOrFieldReference) {
					PropertyOrFieldReference reference = (PropertyOrFieldReference) keyChild;
					key = reference.getName();
				}
				else {
					key = keyChild.getValue(expressionState);
				}
				Object value = getChild(c).getValue(expressionState);
				returnValue.put(key,  value);
			}
			return new TypedValue(returnValue);
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("{");
		int count = getChildCount();
		for (int c = 0; c < count; c++) {
			if (c > 0) {
				sb.append(",");
			}
			sb.append(getChild(c++).toStringAST());
			sb.append(":");
			sb.append(getChild(c).toStringAST());
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * @return 此列表是否为常量值
	 */
	public boolean isConstant() {
		return this.constant != null;
	}

	@SuppressWarnings("unchecked")
	public Map<Object,Object> getConstantValue() {
		return (Map<Object,Object>) this.constant.getValue();
	}

}
