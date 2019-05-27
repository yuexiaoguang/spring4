package org.springframework.beans.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.util.Assert;

/**
 * PagedListHolder是一个简单的状态持有者, 用于处理对象列表, 将它们分成页.
 * 页码编号从0开始.
 *
 * <p>这主要针对Web UI中的使用.
 * 通常, 实例将使用bean列表进行实例化, 放入会话, 并作为模型导出.
 * 可以通过编程方式设置/获取属性, 但最常见的方式是数据绑定, i.e. 从请求参数填充bean.
 * getter将主要由视图使用.
 *
 * <p>支持通过{@link SortDefinition}实现对基础列表进行排序, 可用作属性 "sort".
 * 默认情况下, 将使用{@link MutableSortDefinition}实例, 在再次设置相同属性时切换升序值.
 *
 * <p>数据绑定名称必须是 "pageSize"和"sort.ascending", 正如BeanWrapper所期望的那样.
 * 请注意, 名称和嵌套语法与相应的JSTL EL表达式匹配, 例如"myModelAttr.pageSize"和"myModelAttr.sort.ascending".
 */
@SuppressWarnings("serial")
public class PagedListHolder<E> implements Serializable {

	/**
	 * 默认页面大小.
	 */
	public static final int DEFAULT_PAGE_SIZE = 10;

	/**
	 * 默认的最大页面链接数.
	 */
	public static final int DEFAULT_MAX_LINKED_PAGES = 10;


	private List<E> source;

	private Date refreshDate;

	private SortDefinition sort;

	private SortDefinition sortUsed;

	private int pageSize = DEFAULT_PAGE_SIZE;

	private int page = 0;

	private boolean newPageSet;

	private int maxLinkedPages = DEFAULT_MAX_LINKED_PAGES;


	/**
	 * 需要设置源列表才能使用.
	 */
	public PagedListHolder() {
		this(new ArrayList<E>(0));
	}

	/**
	 * 使用给定的源列表创建一个新的holder实例, 从默认的排序定义开始("toggleAscendingOnProperty"为true).
	 * 
	 * @param source 源列表
	 */
	public PagedListHolder(List<E> source) {
		this(source, new MutableSortDefinition(true));
	}

	/**
	 * @param source 源列表
	 * @param sort
	 */
	public PagedListHolder(List<E> source, SortDefinition sort) {
		setSource(source);
		setSort(sort);
	}


	/**
	 * 设置此holder的源列表.
	 */
	public void setSource(List<E> source) {
		Assert.notNull(source, "Source List must not be null");
		this.source = source;
		this.refreshDate = new Date();
		this.sortUsed = null;
	}

	/**
	 * 返回此holder的源列表.
	 */
	public List<E> getSource() {
		return this.source;
	}

	/**
	 * 返回上次从源提供者获取列表的时间.
	 */
	public Date getRefreshDate() {
		return this.refreshDate;
	}

	/**
	 * 设置此holder的排序定义.
	 * 通常是MutableSortDefinition实例.
	 */
	public void setSort(SortDefinition sort) {
		this.sort = sort;
	}

	/**
	 * 返回此holder的排序定义.
	 */
	public SortDefinition getSort() {
		return this.sort;
	}

	/**
	 * 设置当前页面大小.
	 * 如果更改, 则重置当前页码.
	 * <p>默认是 10.
	 */
	public void setPageSize(int pageSize) {
		if (pageSize != this.pageSize) {
			this.pageSize = pageSize;
			if (!this.newPageSet) {
				this.page = 0;
			}
		}
	}

	/**
	 * 返回当前页面大小.
	 */
	public int getPageSize() {
		return this.pageSize;
	}

	/**
	 * 设置当前页码.
	 * 页码从 0 开始.
	 */
	public void setPage(int page) {
		this.page = page;
		this.newPageSet = true;
	}

	/**
	 * 返回当前页码.
	 * 页码从 0 开始.
	 */
	public int getPage() {
		this.newPageSet = false;
		if (this.page >= getPageCount()) {
			this.page = getPageCount() - 1;
		}
		return this.page;
	}

	/**
	 * 设置当前页面的周围几个页面的最大页面链接数.
	 */
	public void setMaxLinkedPages(int maxLinkedPages) {
		this.maxLinkedPages = maxLinkedPages;
	}

	/**
	 * 返回当前页面的周围几个页面的最大页面链接数.
	 */
	public int getMaxLinkedPages() {
		return this.maxLinkedPages;
	}


	/**
	 * 返回当前源列表的页数.
	 */
	public int getPageCount() {
		float nrOfPages = (float) getNrOfElements() / getPageSize();
		return (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages);
	}

	/**
	 * 返回当前页面是否是第一页.
	 */
	public boolean isFirstPage() {
		return getPage() == 0;
	}

	/**
	 * 返回当前页面是否是最后一页.
	 */
	public boolean isLastPage() {
		return getPage() == getPageCount() -1;
	}

	/**
	 * 切换到上一页.
	 * 如果已经在第一页, 将留在第一页.
	 */
	public void previousPage() {
		if (!isFirstPage()) {
			this.page--;
		}
	}

	/**
	 * 切换到下一页.
	 * 如果已经在最后一页, 将留在最后一页.
	 */
	public void nextPage() {
		if (!isLastPage()) {
			this.page++;
		}
	}

	/**
	 * 返回源列表中的元素总数.
	 */
	public int getNrOfElements() {
		return getSource().size();
	}

	/**
	 * 返回当前页面上第一个元素的元素索引.
	 * 元素索引从0开始.
	 */
	public int getFirstElementOnPage() {
		return (getPageSize() * getPage());
	}

	/**
	 * 返回当前页面上最后一个元素的元素索引.
	 * 元素索引从0开始.
	 */
	public int getLastElementOnPage() {
		int endIndex = getPageSize() * (getPage() + 1);
		int size = getNrOfElements();
		return (endIndex > size ? size : endIndex) - 1;
	}

	/**
	 * 返回表示当前页面的子列表.
	 */
	public List<E> getPageList() {
		return getSource().subList(getFirstElementOnPage(), getLastElementOnPage() + 1);
	}

	/**
	 * 返回在当前页面周围创建链接的第一页.
	 */
	public int getFirstLinkedPage() {
		return Math.max(0, getPage() - (getMaxLinkedPages() / 2));
	}

	/**
	 * 返回在当前页面周围创建链接的最后一页.
	 */
	public int getLastLinkedPage() {
		return Math.min(getFirstLinkedPage() + getMaxLinkedPages() - 1, getPageCount() - 1);
	}


	/**
	 * 列出列表, i.e. 如果当前的{@code sort}实例不等于备份的{@code sortUsed}实例.
	 * <p>调用{@code doSort}来触发实际排序.
	 */
	public void resort() {
		SortDefinition sort = getSort();
		if (sort != null && !sort.equals(this.sortUsed)) {
			this.sortUsed = copySortDefinition(sort);
			doSort(getSource(), sort);
			setPage(0);
		}
	}

	/**
	 * 创建给定排序定义的深层副本, 用作状态持有者, 以比较修改后的排序定义.
	 * <p>默认实现创建一个MutableSortDefinition实例.
	 * 可以在子类中重写, 特别是在SortDefinition接口的自定义扩展的情况下.
	 * 允许返回null, 这意味着不会保留排序状态, 从而触发每个{@code resort}调用的实际排序.
	 * 
	 * @param sort 当前的SortDefinition对象
	 * 
	 * @return SortDefinition对象的深层副本
	 */
	protected SortDefinition copySortDefinition(SortDefinition sort) {
		return new MutableSortDefinition(sort);
	}

	/**
	 * 实际上根据给定的排序定义执行给定源列表的排序.
	 * <p>默认实现使用Spring的PropertyComparator.
	 * 可以在子类中重写.
	 */
	protected void doSort(List<E> source, SortDefinition sort) {
		PropertyComparator.sort(source, sort);
	}

}
