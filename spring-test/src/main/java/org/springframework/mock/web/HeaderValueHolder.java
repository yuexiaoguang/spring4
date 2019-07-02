package org.springframework.mock.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 内部帮助类, 用作请求header的值保存器.
 */
class HeaderValueHolder {

	private final List<Object> values = new LinkedList<Object>();


	public void setValue(Object value) {
		this.values.clear();
		this.values.add(value);
	}

	public void addValue(Object value) {
		this.values.add(value);
	}

	public void addValues(Collection<?> values) {
		this.values.addAll(values);
	}

	public void addValueArray(Object values) {
		CollectionUtils.mergeArrayIntoCollection(values, this.values);
	}

	public List<Object> getValues() {
		return Collections.unmodifiableList(this.values);
	}

	public List<String> getStringValues() {
		List<String> stringList = new ArrayList<String>(this.values.size());
		for (Object value : this.values) {
			stringList.add(value.toString());
		}
		return Collections.unmodifiableList(stringList);
	}

	public Object getValue() {
		return (!this.values.isEmpty() ? this.values.get(0) : null);
	}

	public String getStringValue() {
		return (!this.values.isEmpty() ? String.valueOf(this.values.get(0)) : null);
	}

	@Override
	public String toString() {
		return this.values.toString();
	}


	/**
	 * 按名称查找HeaderValueHolder, 忽略大小写.
	 * 
	 * @param headers header名称到HeaderValueHolder的Map
	 * @param name 所需header的名称
	 * 
	 * @return 相应的HeaderValueHolder, 或{@code null}
	 */
	public static HeaderValueHolder getByName(Map<String, HeaderValueHolder> headers, String name) {
		Assert.notNull(name, "Header name must not be null");
		for (String headerName : headers.keySet()) {
			if (headerName.equalsIgnoreCase(name)) {
				return headers.get(headerName);
			}
		}
		return null;
	}
}
