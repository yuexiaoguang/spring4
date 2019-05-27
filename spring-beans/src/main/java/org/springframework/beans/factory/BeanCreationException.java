package org.springframework.beans.factory;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.FatalBeanException;
import org.springframework.core.NestedRuntimeException;

/**
 * BeanFactory在尝试从bean定义创建bean时遇到错误时抛出异常.
 */
@SuppressWarnings("serial")
public class BeanCreationException extends FatalBeanException {

	private String beanName;

	private String resourceDescription;

	private List<Throwable> relatedCauses;


	/**
	 * @param msg the detail message
	 */
	public BeanCreationException(String msg) {
		super(msg);
	}

	/**
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * @param beanName 请求的bean的名称
	 * @param msg the detail message
	 */
	public BeanCreationException(String beanName, String msg) {
		super("Error creating bean" + (beanName != null ? " with name '" + beanName + "'" : "") + ": " + msg);
		this.beanName = beanName;
	}

	/**
	 * @param beanName 请求的bean的名称
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(String beanName, String msg, Throwable cause) {
		this(beanName, msg);
		initCause(cause);
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param msg the detail message
	 */
	public BeanCreationException(String resourceDescription, String beanName, String msg) {
		super("Error creating bean" + (beanName != null ? " with name '" + beanName + "'" : "") +
				(resourceDescription != null ? " defined in " + resourceDescription : "") + ": " + msg);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(String resourceDescription, String beanName, String msg, Throwable cause) {
		this(resourceDescription, beanName, msg);
		initCause(cause);
	}


	/**
	 * 请求的bean的名称.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * bean定义来自的资源的描述.
	 */
	public String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * 向此Bean创建异常添加相关原因, 不是导致失败的直接原因, 而是在创建相同bean实例的早期发生的.
	 * 
	 * @param ex 要添加的相关原因
	 */
	public void addRelatedCause(Throwable ex) {
		if (this.relatedCauses == null) {
			this.relatedCauses = new LinkedList<Throwable>();
		}
		this.relatedCauses.add(ex);
	}

	/**
	 * 相关原因.
	 * 
	 * @return 相关原因, 或{@code null}
	 */
	public Throwable[] getRelatedCauses() {
		if (this.relatedCauses == null) {
			return null;
		}
		return this.relatedCauses.toArray(new Throwable[this.relatedCauses.size()]);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		if (this.relatedCauses != null) {
			for (Throwable relatedCause : this.relatedCauses) {
				sb.append("\nRelated cause: ");
				sb.append(relatedCause);
			}
		}
		return sb.toString();
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		synchronized (ps) {
			super.printStackTrace(ps);
			if (this.relatedCauses != null) {
				for (Throwable relatedCause : this.relatedCauses) {
					ps.println("Related cause:");
					relatedCause.printStackTrace(ps);
				}
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		synchronized (pw) {
			super.printStackTrace(pw);
			if (this.relatedCauses != null) {
				for (Throwable relatedCause : this.relatedCauses) {
					pw.println("Related cause:");
					relatedCause.printStackTrace(pw);
				}
			}
		}
	}

	@Override
	public boolean contains(Class<?> exClass) {
		if (super.contains(exClass)) {
			return true;
		}
		if (this.relatedCauses != null) {
			for (Throwable relatedCause : this.relatedCauses) {
				if (relatedCause instanceof NestedRuntimeException &&
						((NestedRuntimeException) relatedCause).contains(exClass)) {
					return true;
				}
			}
		}
		return false;
	}
}
