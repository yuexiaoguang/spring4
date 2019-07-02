package org.springframework.test.web;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.ModelAndView;

import static org.springframework.test.util.AssertionErrors.*;

/**
 * 一组断言, 旨在简化处理Spring Web MVC {@link org.springframework.web.servlet.ModelAndView ModelAndView}对象的测试场景.
 *
 * <p>适用于JUnit 4和TestNG. 所有{@code assert*()}方法抛出{@link AssertionError}.
 */
public abstract class ModelAndViewAssert {

	/**
	 * 根据{@code expectedType}}检查给定{@code modelName}下的模型值是否存在, 并检查其类型.
	 * 如果模型条目存在且类型匹配, 则返回模型值.
	 * 
	 * @param mav 要进行测试的ModelAndView (never {@code null})
	 * @param modelName 要添加到模型的对象的名称 (never {@code null})
	 * @param expectedType 预期的类型
	 * 
	 * @return 模型值
	 */
	@SuppressWarnings("unchecked")
	public static <T> T assertAndReturnModelAttributeOfType(ModelAndView mav, String modelName, Class<T> expectedType) {
		assertTrue("ModelAndView is null", mav != null);
		Object obj = mav.getModel().get(modelName);
		assertTrue("Model attribute with name '" + modelName + "' is null", obj != null);
		assertTrue("Model attribute is not of expected type '" + expectedType.getName() + "' but rather of type '" +
				obj.getClass().getName() + "'", expectedType.isAssignableFrom(obj.getClass()));
		return (T) obj;
	}

	/**
	 * 比较列表中的每个条目, 而不先对列表进行排序.
	 * 
	 * @param mav 要进行测试的ModelAndView (never {@code null})
	 * @param modelName 要添加到模型的对象的名称 (never {@code null})
	 * @param expectedList 预期的列表
	 */
	@SuppressWarnings("rawtypes")
	public static void assertCompareListModelAttribute(ModelAndView mav, String modelName, List expectedList) {
		assertTrue("ModelAndView is null", mav != null);
		List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);
		assertTrue("Size of model list is '" + modelList.size() + "' while size of expected list is '" +
				expectedList.size() + "'", expectedList.size() == modelList.size());
		assertTrue("List in model under name '" + modelName + "' is not equal to the expected list.",
				expectedList.equals(modelList));
	}

	/**
	 * 断言模型属性是否可用.
	 * 
	 * @param mav 要进行测试的ModelAndView (never {@code null})
	 * @param modelName 要添加到模型的对象的名称 (never {@code null})
	 */
	public static void assertModelAttributeAvailable(ModelAndView mav, String modelName) {
		assertTrue("ModelAndView is null", mav != null);
		assertTrue("Model attribute with name '" + modelName + "' is not available",
				mav.getModel().containsKey(modelName));
	}

	/**
	 * 将给定的{@code expectedValue}与给定{@code modelName}下绑定的模型中的值进行比较.
	 * 
	 * @param mav 要进行测试的ModelAndView (never {@code null})
	 * @param modelName 要添加到模型的对象的名称 (never {@code null})
	 * @param expectedValue 预期的值
	 */
	public static void assertModelAttributeValue(ModelAndView mav, String modelName, Object expectedValue) {
		assertTrue("ModelAndView is null", mav != null);
		Object modelValue = assertAndReturnModelAttributeOfType(mav, modelName, Object.class);
		assertTrue("Model value with name '" + modelName + "' is not the same as the expected value which was '" +
				expectedValue + "'", modelValue.equals(expectedValue));
	}

	/**
	 * 检查{@code expectedModel}以查看模型中的所有元素是否显示且相等.
	 * 
	 * @param mav 要进行测试的ModelAndView (never {@code null})
	 * @param expectedModel 预期的模型
	 */
	public static void assertModelAttributeValues(ModelAndView mav, Map<String, Object> expectedModel) {
		assertTrue("ModelAndView is null", mav != null);
		Map<String, Object> model = mav.getModel();

		if (!model.keySet().equals(expectedModel.keySet())) {
			StringBuilder sb = new StringBuilder("Keyset of expected model does not match.\n");
			appendNonMatchingSetsErrorMessage(expectedModel.keySet(), model.keySet(), sb);
			fail(sb.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String modelName : model.keySet()) {
			Object assertionValue = expectedModel.get(modelName);
			Object mavValue = model.get(modelName);
			if (!assertionValue.equals(mavValue)) {
				sb.append("Value under name '").append(modelName).append("' differs, should have been '").append(
					assertionValue).append("' but was '").append(mavValue).append("'\n");
			}
		}

		if (sb.length() != 0) {
			sb.insert(0, "Values of expected model do not match.\n");
			fail(sb.toString());
		}
	}

	/**
	 * 在对两个列表进行排序后(可选地使用比较器), 比较列表中的每个单独条目.
	 * 
	 * @param mav 要进行测试的ModelAndView (never {@code null})
	 * @param modelName 要添加到模型的对象的名称 (never {@code null})
	 * @param expectedList 预期的列表
	 * @param comparator 要使用的比较器 (可能是{@code null}). 如果未指定比较器, 则不使用任何比较器对两个列表进行排序.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void assertSortAndCompareListModelAttribute(
			ModelAndView mav, String modelName, List expectedList, Comparator comparator) {

		assertTrue("ModelAndView is null", mav != null);
		List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);
		assertTrue("Size of model list is '" + modelList.size() + "' while size of expected list is '" +
				expectedList.size() + "'", expectedList.size() == modelList.size());

		if (comparator != null) {
			Collections.sort(modelList, comparator);
			Collections.sort(expectedList, comparator);
		}
		else {
			Collections.sort(modelList);
			Collections.sort(expectedList);
		}

		assertTrue("List in model under name '" + modelName + "' is not equal to the expected list.",
				expectedList.equals(modelList));
	}

	/**
	 * 检查ModelAndView中的视图名称是否与给定的{@code expectedName}匹配.
	 * 
	 * @param mav 要进行测试的ModelAndView (never {@code null})
	 * @param expectedName 模型值的名称
	 */
	public static void assertViewName(ModelAndView mav, String expectedName) {
		assertTrue("ModelAndView is null", mav != null);
		assertTrue("View name is not equal to '" + expectedName + "' but was '" + mav.getViewName() + "'",
				ObjectUtils.nullSafeEquals(expectedName, mav.getViewName()));
	}


	private static void appendNonMatchingSetsErrorMessage(
			Set<String> assertionSet, Set<String> incorrectSet, StringBuilder sb) {

		Set<String> tempSet = new HashSet<String>(incorrectSet);
		tempSet.removeAll(assertionSet);

		if (!tempSet.isEmpty()) {
			sb.append("Set has too many elements:\n");
			for (Object element : tempSet) {
				sb.append('-');
				sb.append(element);
				sb.append('\n');
			}
		}

		tempSet = new HashSet<String>(assertionSet);
		tempSet.removeAll(incorrectSet);

		if (!tempSet.isEmpty()) {
			sb.append("Set is missing elements:\n");
			for (Object element : tempSet) {
				sb.append('-');
				sb.append(element);
				sb.append('\n');
			}
		}
	}
}
