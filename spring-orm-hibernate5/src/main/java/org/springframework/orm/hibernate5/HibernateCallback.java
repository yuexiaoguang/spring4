package org.springframework.orm.hibernate5;

import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 * Hibernate代码的回调接口.
 * 与{@link HibernateTemplate}的执行方法一起使用, 通常作为方法实现中的匿名类.
 * 典型的实现将调用{@code Session.load/find/update}来对持久对象执行某些操作.
 */
public interface HibernateCallback<T> {

	/**
	 * 由{@code HibernateTemplate.execute}使用活动的Hibernate {@code Session}调用.
	 * 无需关心激活或关闭{@code Session}, 或处理事务.
	 * <p>允许返回在回调中创建的结果对象, 即域对象或域对象的集合.
	 * 抛出的自定义RuntimeException被视为应用程序异常:
	 * 它会传播到模板的调用者.
	 * 
	 * @param session 活动的Hibernate会话
	 * 
	 * @return 结果对象, 或{@code null}
	 * @throws HibernateException 如果被Hibernate API抛出
	 */
	T doInHibernate(Session session) throws HibernateException;

}
