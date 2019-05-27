package org.springframework.core.type.classreading;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.Attribute;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.core.type.ClassMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * ASM类访问器, 它只查找类名和实现的类型, 通过{@link org.springframework.core.type.ClassMetadata}接口公开它们.
 */
class ClassMetadataReadingVisitor extends ClassVisitor implements ClassMetadata {

	private String className;

	private boolean isInterface;

	private boolean isAnnotation;

	private boolean isAbstract;

	private boolean isFinal;

	private String enclosingClassName;

	private boolean independentInnerClass;

	private String superClassName;

	private String[] interfaces;

	private Set<String> memberClassNames = new LinkedHashSet<String>(4);


	public ClassMetadataReadingVisitor() {
		super(SpringAsmInfo.ASM_VERSION);
	}


	@Override
	public void visit(int version, int access, String name, String signature, String supername, String[] interfaces) {
		this.className = ClassUtils.convertResourcePathToClassName(name);
		this.isInterface = ((access & Opcodes.ACC_INTERFACE) != 0);
		this.isAnnotation = ((access & Opcodes.ACC_ANNOTATION) != 0);
		this.isAbstract = ((access & Opcodes.ACC_ABSTRACT) != 0);
		this.isFinal = ((access & Opcodes.ACC_FINAL) != 0);
		if (supername != null && !this.isInterface) {
			this.superClassName = ClassUtils.convertResourcePathToClassName(supername);
		}
		this.interfaces = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			this.interfaces[i] = ClassUtils.convertResourcePathToClassName(interfaces[i]);
		}
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		this.enclosingClassName = ClassUtils.convertResourcePathToClassName(owner);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (outerName != null) {
			String fqName = ClassUtils.convertResourcePathToClassName(name);
			String fqOuterName = ClassUtils.convertResourcePathToClassName(outerName);
			if (this.className.equals(fqName)) {
				this.enclosingClassName = fqOuterName;
				this.independentInnerClass = ((access & Opcodes.ACC_STATIC) != 0);
			}
			else if (this.className.equals(fqOuterName)) {
				this.memberClassNames.add(fqName);
			}
		}
	}

	@Override
	public void visitSource(String source, String debug) {
		// no-op
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		// no-op
		return new EmptyAnnotationVisitor();
	}

	@Override
	public void visitAttribute(Attribute attr) {
		// no-op
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		// no-op
		return new EmptyFieldVisitor();
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// no-op
		return new EmptyMethodVisitor();
	}

	@Override
	public void visitEnd() {
		// no-op
	}


	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public boolean isInterface() {
		return this.isInterface;
	}

	@Override
	public boolean isAnnotation() {
		return this.isAnnotation;
	}

	@Override
	public boolean isAbstract() {
		return this.isAbstract;
	}

	@Override
	public boolean isConcrete() {
		return !(this.isInterface || this.isAbstract);
	}

	@Override
	public boolean isFinal() {
		return this.isFinal;
	}

	@Override
	public boolean isIndependent() {
		return (this.enclosingClassName == null || this.independentInnerClass);
	}

	@Override
	public boolean hasEnclosingClass() {
		return (this.enclosingClassName != null);
	}

	@Override
	public String getEnclosingClassName() {
		return this.enclosingClassName;
	}

	@Override
	public boolean hasSuperClass() {
		return (this.superClassName != null);
	}

	@Override
	public String getSuperClassName() {
		return this.superClassName;
	}

	@Override
	public String[] getInterfaceNames() {
		return this.interfaces;
	}

	@Override
	public String[] getMemberClassNames() {
		return StringUtils.toStringArray(this.memberClassNames);
	}


	private static class EmptyAnnotationVisitor extends AnnotationVisitor {

		public EmptyAnnotationVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return this;
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return this;
		}
	}


	private static class EmptyMethodVisitor extends MethodVisitor {

		public EmptyMethodVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}
	}


	private static class EmptyFieldVisitor extends FieldVisitor {

		public EmptyFieldVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}
	}
}
