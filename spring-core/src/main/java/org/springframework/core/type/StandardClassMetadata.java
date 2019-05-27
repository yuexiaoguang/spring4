package org.springframework.core.type;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 使用标准反射来内省给定{@code Class}的{@link ClassMetadata}实现.
 */
public class StandardClassMetadata implements ClassMetadata {

	private final Class<?> introspectedClass;


	/**
	 * @param introspectedClass 要内省的Class
	 */
	public StandardClassMetadata(Class<?> introspectedClass) {
		Assert.notNull(introspectedClass, "Class must not be null");
		this.introspectedClass = introspectedClass;
	}

	/**
	 * 返回底层类.
	 */
	public final Class<?> getIntrospectedClass() {
		return this.introspectedClass;
	}


	@Override
	public String getClassName() {
		return this.introspectedClass.getName();
	}

	@Override
	public boolean isInterface() {
		return this.introspectedClass.isInterface();
	}

	@Override
	public boolean isAnnotation() {
		return this.introspectedClass.isAnnotation();
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract(this.introspectedClass.getModifiers());
	}

	@Override
	public boolean isConcrete() {
		return !(isInterface() || isAbstract());
	}

	@Override
	public boolean isFinal() {
		return Modifier.isFinal(this.introspectedClass.getModifiers());
	}

	@Override
	public boolean isIndependent() {
		return (!hasEnclosingClass() ||
				(this.introspectedClass.getDeclaringClass() != null &&
						Modifier.isStatic(this.introspectedClass.getModifiers())));
	}

	@Override
	public boolean hasEnclosingClass() {
		return (this.introspectedClass.getEnclosingClass() != null);
	}

	@Override
	public String getEnclosingClassName() {
		Class<?> enclosingClass = this.introspectedClass.getEnclosingClass();
		return (enclosingClass != null ? enclosingClass.getName() : null);
	}

	@Override
	public boolean hasSuperClass() {
		return (this.introspectedClass.getSuperclass() != null);
	}

	@Override
	public String getSuperClassName() {
		Class<?> superClass = this.introspectedClass.getSuperclass();
		return (superClass != null ? superClass.getName() : null);
	}

	@Override
	public String[] getInterfaceNames() {
		Class<?>[] ifcs = this.introspectedClass.getInterfaces();
		String[] ifcNames = new String[ifcs.length];
		for (int i = 0; i < ifcs.length; i++) {
			ifcNames[i] = ifcs[i].getName();
		}
		return ifcNames;
	}

	@Override
	public String[] getMemberClassNames() {
		LinkedHashSet<String> memberClassNames = new LinkedHashSet<String>(4);
		for (Class<?> nestedClass : this.introspectedClass.getDeclaredClasses()) {
			memberClassNames.add(nestedClass.getName());
		}
		return StringUtils.toStringArray(memberClassNames);
	}

}
