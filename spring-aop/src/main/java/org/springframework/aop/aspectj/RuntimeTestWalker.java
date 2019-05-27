package org.springframework.aop.aspectj;

import java.lang.reflect.Field;

import org.aspectj.weaver.ReferenceType;
import org.aspectj.weaver.ReferenceTypeDelegate;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.ast.And;
import org.aspectj.weaver.ast.Call;
import org.aspectj.weaver.ast.FieldGetCall;
import org.aspectj.weaver.ast.HasAnnotation;
import org.aspectj.weaver.ast.ITestVisitor;
import org.aspectj.weaver.ast.Instanceof;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Not;
import org.aspectj.weaver.ast.Or;
import org.aspectj.weaver.ast.Test;
import org.aspectj.weaver.internal.tools.MatchingContextBasedTest;
import org.aspectj.weaver.reflect.ReflectionBasedReferenceTypeDelegate;
import org.aspectj.weaver.reflect.ReflectionVar;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.ShadowMatch;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 该类封装了一些AspectJ内部知识, 这些知识应该在将来的版本中推回到AspectJ项目中.
 *
 * <p>它依赖于AspectJ中的实现特定知识来打破封装并执行AspectJ未设计的操作: 
 * 查询将要执行的运行时测试的类型. 
 * 这里的代码应该迁移到{@code ShadowMatch.getVariablesInvolvedInRuntimeTest()}或类似的操作.
 *
 * <p>See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=151593"/>Bug 151593</a>
 */
class RuntimeTestWalker {

	private static final Field residualTestField;

	private static final Field varTypeField;

	private static final Field myClassField;


	static {
		try {
			residualTestField = ShadowMatchImpl.class.getDeclaredField("residualTest");
			varTypeField = ReflectionVar.class.getDeclaredField("varType");
			myClassField = ReflectionBasedReferenceTypeDelegate.class.getDeclaredField("myClass");
		}
		catch (NoSuchFieldException ex) {
			throw new IllegalStateException("The version of aspectjtools.jar / aspectjweaver.jar " +
					"on the classpath is incompatible with this version of Spring: " + ex);
		}
	}


	private final Test runtimeTest;


	public RuntimeTestWalker(ShadowMatch shadowMatch) {
		try {
			ReflectionUtils.makeAccessible(residualTestField);
			this.runtimeTest = (Test) residualTestField.get(shadowMatch);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException(ex);
		}
	}


	/**
	 * 如果测试使用this, target, at_this, at_target, at_annotation 变量, 那么它测试子类型敏感的变量.
	 */
	public boolean testsSubtypeSensitiveVars() {
		return (this.runtimeTest != null &&
				new SubtypeSensitiveVarTypeTestVisitor().testsSubtypeSensitiveVars(this.runtimeTest));
	}

	public boolean testThisInstanceOfResidue(Class<?> thisClass) {
		return (this.runtimeTest != null &&
				new ThisInstanceOfResidueTestVisitor(thisClass).thisInstanceOfMatches(this.runtimeTest));
	}

	public boolean testTargetInstanceOfResidue(Class<?> targetClass) {
		return (this.runtimeTest != null &&
				new TargetInstanceOfResidueTestVisitor(targetClass).targetInstanceOfMatches(this.runtimeTest));
	}


	private static class TestVisitorAdapter implements ITestVisitor {

		protected static final int THIS_VAR = 0;
		protected static final int TARGET_VAR = 1;
		protected static final int AT_THIS_VAR = 3;
		protected static final int AT_TARGET_VAR = 4;
		protected static final int AT_ANNOTATION_VAR = 8;

		@Override
		public void visit(And e) {
			e.getLeft().accept(this);
			e.getRight().accept(this);
		}

		@Override
		public void visit(Or e) {
			e.getLeft().accept(this);
			e.getRight().accept(this);
		}

		@Override
		public void visit(Not e) {
			e.getBody().accept(this);
		}

		@Override
		public void visit(Instanceof i) {
		}

		@Override
		public void visit(Literal literal) {
		}

		@Override
		public void visit(Call call) {
		}

		@Override
		public void visit(FieldGetCall fieldGetCall) {
		}

		@Override
		public void visit(HasAnnotation hasAnnotation) {
		}

		@Override
		public void visit(MatchingContextBasedTest matchingContextTest) {
		}

		protected int getVarType(ReflectionVar v) {
			try {
				ReflectionUtils.makeAccessible(varTypeField);
				return (Integer) varTypeField.get(v);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}


	private static abstract class InstanceOfResidueTestVisitor extends TestVisitorAdapter {

		private final Class<?> matchClass;

		private boolean matches;

		private final int matchVarType;

		public InstanceOfResidueTestVisitor(Class<?> matchClass, boolean defaultMatches, int matchVarType) {
			this.matchClass = matchClass;
			this.matches = defaultMatches;
			this.matchVarType = matchVarType;
		}

		public boolean instanceOfMatches(Test test) {
			test.accept(this);
			return this.matches;
		}

		@Override
		public void visit(Instanceof i) {
			int varType = getVarType((ReflectionVar) i.getVar());
			if (varType != this.matchVarType) {
				return;
			}
			Class<?> typeClass = null;
			ResolvedType type = (ResolvedType) i.getType();
			if (type instanceof ReferenceType) {
				ReferenceTypeDelegate delegate = ((ReferenceType) type).getDelegate();
				if (delegate instanceof ReflectionBasedReferenceTypeDelegate) {
					try {
						ReflectionUtils.makeAccessible(myClassField);
						typeClass = (Class<?>) myClassField.get(delegate);
					}
					catch (IllegalAccessException ex) {
						throw new IllegalStateException(ex);
					}
				}
			}
			try {
				// Don't use ResolvedType.isAssignableFrom() as it won't be aware of (Spring) mixins
				if (typeClass == null) {
					typeClass = ClassUtils.forName(type.getName(), this.matchClass.getClassLoader());
				}
				this.matches = typeClass.isAssignableFrom(this.matchClass);
			}
			catch (ClassNotFoundException ex) {
				this.matches = false;
			}
		}
	}


	/**
	 * 检查target(TYPE)类型的剩余内容. See SPR-3783 for more details.
	 */
	private static class TargetInstanceOfResidueTestVisitor extends InstanceOfResidueTestVisitor {

		public TargetInstanceOfResidueTestVisitor(Class<?> targetClass) {
			super(targetClass, false, TARGET_VAR);
		}

		public boolean targetInstanceOfMatches(Test test) {
			return instanceOfMatches(test);
		}
	}


	/**
	 * 检查this(TYPE)类型的剩余内容. See SPR-2979 for more details.
	 */
	private static class ThisInstanceOfResidueTestVisitor extends InstanceOfResidueTestVisitor {

		public ThisInstanceOfResidueTestVisitor(Class<?> thisClass) {
			super(thisClass, true, THIS_VAR);
		}

		// TODO: 优化: 仅当this()指定一个类型而不是标识符时才处理.
		public boolean thisInstanceOfMatches(Test test) {
			return instanceOfMatches(test);
		}
	}


	private static class SubtypeSensitiveVarTypeTestVisitor extends TestVisitorAdapter {

		private final Object thisObj = new Object();

		private final Object targetObj = new Object();

		private final Object[] argsObjs = new Object[0];

		private boolean testsSubtypeSensitiveVars = false;

		public boolean testsSubtypeSensitiveVars(Test aTest) {
			aTest.accept(this);
			return this.testsSubtypeSensitiveVars;
		}

		@Override
		public void visit(Instanceof i) {
			ReflectionVar v = (ReflectionVar) i.getVar();
			Object varUnderTest = v.getBindingAtJoinPoint(this.thisObj, this.targetObj, this.argsObjs);
			if (varUnderTest == this.thisObj || varUnderTest == this.targetObj) {
				this.testsSubtypeSensitiveVars = true;
			}
		}

		@Override
		public void visit(HasAnnotation hasAnn) {
			// 如果你以前认为事情很糟糕, 现在我们陷入了恐怖的新境界...
			ReflectionVar v = (ReflectionVar) hasAnn.getVar();
			int varType = getVarType(v);
			if (varType == AT_THIS_VAR || varType == AT_TARGET_VAR || varType == AT_ANNOTATION_VAR) {
				this.testsSubtypeSensitiveVars = true;
			}
		}
	}

}
