package org.springframework.beans.factory.parsing;

/**
 * 表示bean定义的{@link ParseState}条目.
 */
public class BeanEntry implements ParseState.Entry {

	private String beanDefinitionName;


	/**
	 * @param beanDefinitionName 关联bean定义的名称
	 */
	public BeanEntry(String beanDefinitionName) {
		this.beanDefinitionName = beanDefinitionName;
	}


	@Override
	public String toString() {
		return "Bean '" + this.beanDefinitionName + "'";
	}

}
