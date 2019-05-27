package org.springframework.expression.spel.ast;

import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * 表示对类型的bean引用, 例如 <tt>@foo</tt> 或 <tt>@'foo.bar'</tt>.
 * 对于FactoryBean, 语法<tt>&foo</tt>可用于访问工厂本身.
 */
public class BeanReference extends SpelNodeImpl {

	private final static String FACTORY_BEAN_PREFIX = "&";
	
	private final String beanName;


	public BeanReference(int pos, String beanName) {
		super(pos);
		this.beanName = beanName;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		BeanResolver beanResolver = state.getEvaluationContext().getBeanResolver();
		if (beanResolver == null) {
			throw new SpelEvaluationException(
					getStartPosition(), SpelMessage.NO_BEAN_RESOLVER_REGISTERED, this.beanName);
		}

		try {
			return new TypedValue(beanResolver.resolve(state.getEvaluationContext(), this.beanName));
		}
		catch (AccessException ex) {
			throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_BEAN_RESOLUTION,
				this.beanName, ex.getMessage());
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		if (!this.beanName.startsWith(FACTORY_BEAN_PREFIX)) {
			sb.append("@");
		}
		if (!this.beanName.contains(".")) {
			sb.append(this.beanName);
		}
		else {
			sb.append("'").append(this.beanName).append("'");
		}
		return sb.toString();
	}

}
