package org.springframework.aop.aspectj;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.StringUtils;

/**
 * {@link ParameterNameDiscoverer}实现尝试从切点表达式, 返回, 抛出子句推断出一个增强方法的参数名称.
 * 如果没有明确的解释, 它返回 {@code null}.
 *
 * <p>该类以下列方式解释参数:
 * <ol>
 * <li>如果方法的第一个参数是{@link JoinPoint}或{@link ProceedingJoinPoint}类型, 它被假定为将{@code thisJoinPoint}传递给增强,
 * 并为参数名称赋值 {@code "thisJoinPoint"}.</li>
 * 
 * <li>如果方法的第一个参数是 {@code JoinPoint.StaticPart}类型, 它被假定为将{@code "thisJoinPointStaticPart"}传递给增强,
 * 并且参数名称被假定为 {@code "thisJoinPointStaticPart"}.</li>
 * 
 * <li>如果已经设置{@link #setThrowingName(String) throwingName}, 并且没有{@code Throwable+}类型的未绑定参数,
 * 那么将引发{@link IllegalArgumentException}.
 * 如果有多个类型为{@code Throwable+}的未绑定参数, 那么将引发{@link AmbiguousBindingException}.
 * 如果只有一个类型为{@code Throwable+}的未绑定参数, 那么对应的参数名称将假定为 &lt;throwingName&gt;.</li>
 * 
 * <li>如果仍有未绑定的参数, 然后检查切点表达式. 让 {@code a} 成为基于注解的切点表达式的数量 (&#64;annotation, &#64;this, &#64;target, &#64;args,
 * &#64;within, &#64;withincode) 用于绑定形式.
 * 绑定形式的用法本身可以推导出来: 如果切点内的表达式是符合Java变量名约定的单个字符串文字，则假定它是变量名.
 * 如果 {@code a}是零, 进入下一阶段. 如果{@code a} &gt; 1, 那么将引发{@code AmbiguousBindingException}.
 * 如果{@code a} == 1, 并且没有{@code Annotation+}类型的未绑定参数, 那么将引发{@code IllegalArgumentException}.
 * 如果只有一个这样的参数, 那么对应的参数名称从切点表达式中分配值.</li>
 * 
 * <li>如果已设置returningName, 并且没有未绑定的参数, 那么将引发{@code IllegalArgumentException}.
 * 如果有多个未绑定的参数, 那么将引发{@code AmbiguousBindingException}. 如果只有一个未绑定的参数, 那么对应的参数名称从&lt;returningName&gt;中分配值.</li>
 * 
 * <li>如果仍有未绑定的参数, 那么对{@code this}, {@code target}以及绑定形式中使用的{@code args}切点表达式再次检查切点表达式
 * (如针对基于注解的切点所描述的那样推导出绑定形式). 如果原始类型仍有多个未绑定参数 (只能绑定在{@code args}), 那么将引发{@code AmbiguousBindingException}.
 * 如果只有一个基本类型的参数, 那么如果找到了一个{@code args}绑定变量, 为相应的参数名称指定变量名称.
 * 如果没有找到{@code args}绑定变量, 那么将引发{@code IllegalStateException}.
 * 如果有多个{@code args}绑定变量, 那么将引发{@code AmbiguousBindingException}.
 * 在此刻, 如果还有多个未绑定的参数, 那么将引发{@code AmbiguousBindingException}.
 * 如果没有未绑定的参数, 就完成了.
 * 如果只剩下一个未绑定的参数, 并且只有一个候选变量名称来自{@code this}, {@code target}, {@code args}, 它被指定为相应的参数名称.
 * 如果有多种可能, 将引发{@code AmbiguousBindingException}.</li>
 * </ol>
 *
 * <p>引发{@code IllegalArgumentException}或{@code AmbiguousBindingException}的行为可配置, 允许这个发现者被用作责任链的一部分.
 * 默认情况下, 条件将被记录, 并且{@code getParameterNames(..)} 方法将简单返回{@code null}.
 * 如果{@link #setRaiseExceptions(boolean) raiseExceptions}属性设置为 {@code true},
 * 条件将分别被抛出为{@code IllegalArgumentException} 和 {@code AmbiguousBindingException}.
 *
 * <p>Was that perfectly clear? ;)
 *
 * <p>精简版: 如果可以推断出明确的绑定, 那么它就是.
 * 如果不能满足增强的要求, 那么将返回{@code null}.
 * 通过设置{@link #setRaiseExceptions(boolean) raiseExceptions}属性为{@code true}, 在无法发现参数名称的情况下, 将抛出描述性异常而不是返回{@code null}.
 */
public class AspectJAdviceParameterNameDiscoverer implements ParameterNameDiscoverer {

	private static final String THIS_JOIN_POINT = "thisJoinPoint";
	private static final String THIS_JOIN_POINT_STATIC_PART = "thisJoinPointStaticPart";

	// 绑定算法中的步骤...
	private static final int STEP_JOIN_POINT_BINDING = 1;
	private static final int STEP_THROWING_BINDING = 2;
	private static final int STEP_ANNOTATION_BINDING = 3;
	private static final int STEP_RETURNING_BINDING = 4;
	private static final int STEP_PRIMITIVE_ARGS_BINDING = 5;
	private static final int STEP_THIS_TARGET_ARGS_BINDING = 6;
	private static final int STEP_REFERENCE_PCUT_BINDING = 7;
	private static final int STEP_FINISHED = 8;

	private static final Set<String> singleValuedAnnotationPcds = new HashSet<String>();
	private static final Set<String> nonReferencePointcutTokens = new HashSet<String>();


	static {
		singleValuedAnnotationPcds.add("@this");
		singleValuedAnnotationPcds.add("@target");
		singleValuedAnnotationPcds.add("@within");
		singleValuedAnnotationPcds.add("@withincode");
		singleValuedAnnotationPcds.add("@annotation");

		Set<PointcutPrimitive> pointcutPrimitives = PointcutParser.getAllSupportedPointcutPrimitives();
		for (PointcutPrimitive primitive : pointcutPrimitives) {
			nonReferencePointcutTokens.add(primitive.getName());
		}
		nonReferencePointcutTokens.add("&&");
		nonReferencePointcutTokens.add("!");
		nonReferencePointcutTokens.add("||");
		nonReferencePointcutTokens.add("and");
		nonReferencePointcutTokens.add("or");
		nonReferencePointcutTokens.add("not");
	}


	/** 与增强相关的切点表达式 */
	private String pointcutExpression;

	private boolean raiseExceptions;

	/** 如果增强是 afterReturning, 并绑定返回值, 这是使用的参数名称 */
	private String returningName;

	/** 如果增强是 afterThrowing, 并绑定抛出值, 这是使用的参数名称 */
	private String throwingName;

	private Class<?>[] argumentTypes;

	private String[] parameterNameBindings;

	private int numberOfRemainingUnboundArguments;


	/**
	 * 尝试从给定的切点表达式中发现参数名称.
	 */
	public AspectJAdviceParameterNameDiscoverer(String pointcutExpression) {
		this.pointcutExpression = pointcutExpression;
	}


	/**
	 * 是否必须抛出 {@link IllegalArgumentException} 和 {@link AmbiguousBindingException},
	 * 在未能推断出增强参数名称的情况下.
	 * 
	 * @param raiseExceptions {@code true}抛出异常
	 */
	public void setRaiseExceptions(boolean raiseExceptions) {
		this.raiseExceptions = raiseExceptions;
	}

	/**
	 * 如果{@code afterReturning}增强绑定了返回值, 必须指定返回的变量名称.
	 * 
	 * @param returningName 返回变量的名称
	 */
	public void setReturningName(String returningName) {
		this.returningName = returningName;
	}

	/**
	 * 如果{@code afterThrowing}增强绑定了抛出值, 必须指定抛出的变量名称.
	 * 
	 * @param throwingName 抛出的变量名称
	 */
	public void setThrowingName(String throwingName) {
		this.throwingName = throwingName;
	}


	/**
	 * 为增强方法推断参数名称.
	 * <p>See the {@link AspectJAdviceParameterNameDiscoverer class level javadoc}
	 * for this class for details of the algorithm used.
	 * 
	 * @param method 目标{@link Method}
	 * 
	 * @return 参数名称
	 */
	@Override
	public String[] getParameterNames(Method method) {
		this.argumentTypes = method.getParameterTypes();
		this.numberOfRemainingUnboundArguments = this.argumentTypes.length;
		this.parameterNameBindings = new String[this.numberOfRemainingUnboundArguments];

		int minimumNumberUnboundArgs = 0;
		if (this.returningName != null) {
			minimumNumberUnboundArgs++;
		}
		if (this.throwingName != null) {
			minimumNumberUnboundArgs++;
		}
		if (this.numberOfRemainingUnboundArguments < minimumNumberUnboundArgs) {
			throw new IllegalStateException(
					"Not enough arguments in method to satisfy binding of returning and throwing variables");
		}

		try {
			int algorithmicStep = STEP_JOIN_POINT_BINDING;
			while ((this.numberOfRemainingUnboundArguments > 0) && algorithmicStep < STEP_FINISHED) {
				switch (algorithmicStep++) {
					case STEP_JOIN_POINT_BINDING:
						if (!maybeBindThisJoinPoint()) {
							maybeBindThisJoinPointStaticPart();
						}
						break;
					case STEP_THROWING_BINDING:
						maybeBindThrowingVariable();
						break;
					case STEP_ANNOTATION_BINDING:
						maybeBindAnnotationsFromPointcutExpression();
						break;
					case STEP_RETURNING_BINDING:
						maybeBindReturningVariable();
						break;
					case STEP_PRIMITIVE_ARGS_BINDING:
						maybeBindPrimitiveArgsFromPointcutExpression();
						break;
					case STEP_THIS_TARGET_ARGS_BINDING:
						maybeBindThisOrTargetOrArgsFromPointcutExpression();
						break;
					case STEP_REFERENCE_PCUT_BINDING:
						maybeBindReferencePointcutParameter();
						break;
					default:
						throw new IllegalStateException("Unknown algorithmic step: " + (algorithmicStep - 1));
				}
			}
		}
		catch (AmbiguousBindingException ambigEx) {
			if (this.raiseExceptions) {
				throw ambigEx;
			}
			else {
				return null;
			}
		}
		catch (IllegalArgumentException ex) {
			if (this.raiseExceptions) {
				throw ex;
			}
			else {
				return null;
			}
		}

		if (this.numberOfRemainingUnboundArguments == 0) {
			return this.parameterNameBindings;
		}
		else {
			if (this.raiseExceptions) {
				throw new IllegalStateException("Failed to bind all argument names: " +
						this.numberOfRemainingUnboundArguments + " argument(s) could not be bound");
			}
			else {
				// 失败的约定是返回 null, 允许参与责任链
				return null;
			}
		}
	}

	/**
	 * 一个增强方法永远不能成为Spring中的构造函数.
	 * 
	 * @return {@code null}
	 * @throws UnsupportedOperationException
	 * 如果 {@link #setRaiseExceptions(boolean) raiseExceptions}已经被设置为{@code true}
	 */
	@Override
	public String[] getParameterNames(Constructor<?> ctor) {
		if (this.raiseExceptions) {
			throw new UnsupportedOperationException("An advice method can never be a constructor");
		}
		else {
			// 返回null, 而不是抛出异常, 以便在责任链中表现良好.
			return null;
		}
	}


	private void bindParameterName(int index, String name) {
		this.parameterNameBindings[index] = name;
		this.numberOfRemainingUnboundArguments--;
	}

	/**
	 * 如果第一个参数是JoinPoint或ProceedingJoinPoint类型, 绑定"thisJoinPoint"作为参数名, 并返回 true, 否则返回 false.
	 */
	private boolean maybeBindThisJoinPoint() {
		if ((this.argumentTypes[0] == JoinPoint.class) || (this.argumentTypes[0] == ProceedingJoinPoint.class)) {
			bindParameterName(0, THIS_JOIN_POINT);
			return true;
		}
		else {
			return false;
		}
	}

	private void maybeBindThisJoinPointStaticPart() {
		if (this.argumentTypes[0] == JoinPoint.StaticPart.class) {
			bindParameterName(0, THIS_JOIN_POINT_STATIC_PART);
		}
	}

	/**
	 * 如果指定了抛出名称, 并且只剩下一个选项, (参数是Throwable的子类型)那么绑定它.
	 */
	private void maybeBindThrowingVariable() {
		if (this.throwingName == null) {
			return;
		}

		// So there is binding work to do...
		int throwableIndex = -1;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(Throwable.class, i)) {
				if (throwableIndex == -1) {
					throwableIndex = i;
				}
				else {
					// 找到的第二个候选 - 模糊绑定
					throw new AmbiguousBindingException("Binding of throwing parameter '" +
							this.throwingName + "' is ambiguous: could be bound to argument " +
							throwableIndex + " or argument " + i);
				}
			}
		}

		if (throwableIndex == -1) {
			throw new IllegalStateException("Binding of throwing parameter '" + this.throwingName
					+ "' could not be completed as no available arguments are a subtype of Throwable");
		}
		else {
			bindParameterName(throwableIndex, this.throwingName);
		}
	}

	/**
	 * 如果指定了返回变量, 并且只剩下一个选项, 绑定它.
	 */
	private void maybeBindReturningVariable() {
		if (this.numberOfRemainingUnboundArguments == 0) {
			throw new IllegalStateException(
					"Algorithm assumes that there must be at least one unbound parameter on entry to this method");
		}

		if (this.returningName != null) {
			if (this.numberOfRemainingUnboundArguments > 1) {
				throw new AmbiguousBindingException("Binding of returning parameter '" + this.returningName +
						"' is ambiguous, there are " + this.numberOfRemainingUnboundArguments + " candidates.");
			}

			// 都设置了... 找到未绑定的参数, 并绑定它.
			for (int i = 0; i < this.parameterNameBindings.length; i++) {
				if (this.parameterNameBindings[i] == null) {
					bindParameterName(i, this.returningName);
					break;
				}
			}
		}
	}


	/**
	 * 解析查找的字符串切点表达式:
	 * &#64;this, &#64;target, &#64;args, &#64;within, &#64;withincode, &#64;annotation.
	 * 如果找到其中一个切点表达式, 尝试提取候选变量名称 (或变量名称, 在args的情况下).
	 * <p>来自AspectJ的更多支持会很好... :)
	 */
	private void maybeBindAnnotationsFromPointcutExpression() {
		List<String> varNames = new ArrayList<String>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			String toMatch = tokens[i];
			int firstParenIndex = toMatch.indexOf('(');
			if (firstParenIndex != -1) {
				toMatch = toMatch.substring(0, firstParenIndex);
			}
			if (singleValuedAnnotationPcds.contains(toMatch)) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
			else if (tokens[i].startsWith("@args(") || tokens[i].equals("@args")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				maybeExtractVariableNamesFromArgs(body.text, varNames);
			}
		}

		bindAnnotationsFromVarNames(varNames);
	}

	/**
	 * 将提取的变量名称的给定列表与参数槽匹配.
	 */
	private void bindAnnotationsFromVarNames(List<String> varNames) {
		if (!varNames.isEmpty()) {
			// we have work to do...
			int numAnnotationSlots = countNumberOfUnboundAnnotationArguments();
			if (numAnnotationSlots > 1) {
				throw new AmbiguousBindingException("Found " + varNames.size() +
						" potential annotation variable(s), and " +
						numAnnotationSlots + " potential argument slots");
			}
			else if (numAnnotationSlots == 1) {
				if (varNames.size() == 1) {
					// it's a match
					findAndBind(Annotation.class, varNames.get(0));
				}
				else {
					// multiple candidate vars, but only one slot
					throw new IllegalArgumentException("Found " + varNames.size() +
							" candidate annotation binding variables" +
							" but only one potential argument binding slot");
				}
			}
			else {
				// 没有插槽, 所以假设那些候选变量实际上是类型名称
			}
		}
	}

	/*
	 * 如果令牌开始符合Java标识符约定, 它在里面.
	 */
	private String maybeExtractVariableName(String candidateToken) {
		if (!StringUtils.hasLength(candidateToken)) {
			return null;
		}
		if (Character.isJavaIdentifierStart(candidateToken.charAt(0)) &&
				Character.isLowerCase(candidateToken.charAt(0))) {
			char[] tokenChars = candidateToken.toCharArray();
			for (char tokenChar : tokenChars) {
				if (!Character.isJavaIdentifierPart(tokenChar)) {
					return null;
				}
			}
			return candidateToken;
		}
		else {
			return null;
		}
	}

	/**
	 * 给定一个args 切点主体(可以是 {@code args} 或 {@code at_args}), 将任何候选变量名添加到给定列表中.
	 */
	private void maybeExtractVariableNamesFromArgs(String argsSpec, List<String> varNames) {
		if (argsSpec == null) {
			return;
		}
		String[] tokens = StringUtils.tokenizeToStringArray(argsSpec, ",");
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = StringUtils.trimWhitespace(tokens[i]);
			String varName = maybeExtractVariableName(tokens[i]);
			if (varName != null) {
				varNames.add(varName);
			}
		}
	}

	/**
	 * 解析字符串切点表达式, 查找 this(), target() 和 args() 表达式.
	 * 如果找到一个, 尝试提取候选变量名并绑定它.
	 */
	private void maybeBindThisOrTargetOrArgsFromPointcutExpression() {
		if (this.numberOfRemainingUnboundArguments > 1) {
			throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments
					+ " unbound args at this(),target(),args() binding stage, with no way to determine between them");
		}

		List<String> varNames = new ArrayList<String>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("this") ||
					tokens[i].startsWith("this(") ||
					tokens[i].equals("target") ||
					tokens[i].startsWith("target(")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
			else if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				List<String> candidateVarNames = new ArrayList<String>();
				maybeExtractVariableNamesFromArgs(body.text, candidateVarNames);
				// 可能已经找到了一些在以前的原始args绑定步骤中绑定的var名称, 过滤掉它们...
				for (String varName : candidateVarNames) {
					if (!alreadyBound(varName)) {
						varNames.add(varName);
					}
				}
			}
		}


		if (varNames.size() > 1) {
			throw new AmbiguousBindingException("Found " + varNames.size() +
					" candidate this(), target() or args() variables but only one unbound argument slot");
		}
		else if (varNames.size() == 1) {
			for (int j = 0; j < this.parameterNameBindings.length; j++) {
				if (isUnbound(j)) {
					bindParameterName(j, varNames.get(0));
					break;
				}
			}
		}
		// else varNames.size must be 0 and we have nothing to bind.
	}

	private void maybeBindReferencePointcutParameter() {
		if (this.numberOfRemainingUnboundArguments > 1) {
			throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments
					+ " unbound args at reference pointcut binding stage, with no way to determine between them");
		}

		List<String> varNames = new ArrayList<String>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			String toMatch = tokens[i];
			if (toMatch.startsWith("!")) {
				toMatch = toMatch.substring(1);
			}
			int firstParenIndex = toMatch.indexOf('(');
			if (firstParenIndex != -1) {
				toMatch = toMatch.substring(0, firstParenIndex);
			}
			else {
				if (tokens.length < i + 2) {
					// no "(" and nothing following
					continue;
				}
				else {
					String nextToken = tokens[i + 1];
					if (nextToken.charAt(0) != '(') {
						// next token is not "(" either, can't be a pc...
						continue;
					}
				}

			}

			// eat the body
			PointcutBody body = getPointcutBody(tokens, i);
			i += body.numTokensConsumed;

			if (!nonReferencePointcutTokens.contains(toMatch)) {
				// then it could be a reference pointcut
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
		}

		if (varNames.size() > 1) {
			throw new AmbiguousBindingException("Found " + varNames.size() +
					" candidate reference pointcut variables but only one unbound argument slot");
		}
		else if (varNames.size() == 1) {
			for (int j = 0; j < this.parameterNameBindings.length; j++) {
				if (isUnbound(j)) {
					bindParameterName(j, varNames.get(0));
					break;
				}
			}
		}
		// else varNames.size must be 0 and we have nothing to bind.
	}

	/*
	 * 在token数组的给定索引处找到了绑定切点的开始. 现在需要提取切点主体并将其返回.
	 */
	private PointcutBody getPointcutBody(String[] tokens, int startIndex) {
		int numTokensConsumed = 0;
		String currentToken = tokens[startIndex];
		int bodyStart = currentToken.indexOf('(');
		if (currentToken.charAt(currentToken.length() - 1) == ')') {
			// 这是一个整体... 获取第一个（和最后一个）之间的文本
			return new PointcutBody(0, currentToken.substring(bodyStart + 1, currentToken.length() - 1));
		}
		else {
			StringBuilder sb = new StringBuilder();
			if (bodyStart >= 0 && bodyStart != (currentToken.length() - 1)) {
				sb.append(currentToken.substring(bodyStart + 1));
				sb.append(" ");
			}
			numTokensConsumed++;
			int currentIndex = startIndex + numTokensConsumed;
			while (currentIndex < tokens.length) {
				if (tokens[currentIndex].equals("(")) {
					currentIndex++;
					continue;
				}

				if (tokens[currentIndex].endsWith(")")) {
					sb.append(tokens[currentIndex].substring(0, tokens[currentIndex].length() - 1));
					return new PointcutBody(numTokensConsumed, sb.toString().trim());
				}

				String toAppend = tokens[currentIndex];
				if (toAppend.startsWith("(")) {
					toAppend = toAppend.substring(1);
				}
				sb.append(toAppend);
				sb.append(" ");
				currentIndex++;
				numTokensConsumed++;
			}

		}

		// We looked and failed...
		return new PointcutBody(numTokensConsumed, null);
	}

	/**
	 * 将args与原始类型的未绑定参数进行匹配
	 */
	private void maybeBindPrimitiveArgsFromPointcutExpression() {
		int numUnboundPrimitives = countNumberOfUnboundPrimitiveArguments();
		if (numUnboundPrimitives > 1) {
			throw new AmbiguousBindingException("Found '" + numUnboundPrimitives +
					"' unbound primitive arguments with no way to distinguish between them.");
		}
		if (numUnboundPrimitives == 1) {
			// 如果找到一个变量，查找arg变量并绑定它...
			List<String> varNames = new ArrayList<String>();
			String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
					PointcutBody body = getPointcutBody(tokens, i);
					i += body.numTokensConsumed;
					maybeExtractVariableNamesFromArgs(body.text, varNames);
				}
			}
			if (varNames.size() > 1) {
				throw new AmbiguousBindingException("Found " + varNames.size() +
						" candidate variable names but only one candidate binding slot when matching primitive args");
			}
			else if (varNames.size() == 1) {
				// 1个原始arg, 和一个候选...
				for (int i = 0; i < this.argumentTypes.length; i++) {
					if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
						bindParameterName(i, varNames.get(0));
						break;
					}
				}
			}
		}
	}

	/*
	 * 如果尚未分配给定参数索引的参数名称绑定，则返回true.
	 */
	private boolean isUnbound(int i) {
		return this.parameterNameBindings[i] == null;
	}

	private boolean alreadyBound(String varName) {
		for (int i = 0; i < this.parameterNameBindings.length; i++) {
			if (!isUnbound(i) && varName.equals(this.parameterNameBindings[i])) {
				return true;
			}
		}
		return false;
	}

	/*
	 * 返回 {@code true}, 如果给定的参数类型是给定超类型的子类.
	 */
	private boolean isSubtypeOf(Class<?> supertype, int argumentNumber) {
		return supertype.isAssignableFrom(this.argumentTypes[argumentNumber]);
	}

	private int countNumberOfUnboundAnnotationArguments() {
		int count = 0;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(Annotation.class, i)) {
				count++;
			}
		}
		return count;
	}

	private int countNumberOfUnboundPrimitiveArguments() {
		int count = 0;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
				count++;
			}
		}
		return count;
	}

	/*
	 * 查找具有给定类型的参数索引, 并将给定的{@code varName}绑定在该位置.
	 */
	private void findAndBind(Class<?> argumentType, String varName) {
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(argumentType, i)) {
				bindParameterName(i, varName);
				return;
			}
		}
		throw new IllegalStateException("Expected to find an unbound argument of type '" +
				argumentType.getName() + "'");
	}


	/**
	 * 简单的结构, 用于保存切点主体中提取的文本, 以及提取它时消耗的token数量.
	 */
	private static class PointcutBody {

		private int numTokensConsumed;

		private String text;

		public PointcutBody(int tokens, String text) {
			this.numTokensConsumed = tokens;
			this.text = text;
		}
	}


	/**
	 * 抛出, 以响应在尝试解析方法的参数名称时检测到模糊绑定.
	 */
	@SuppressWarnings("serial")
	public static class AmbiguousBindingException extends RuntimeException {

		public AmbiguousBindingException(String msg) {
			super(msg);
		}
	}
}
