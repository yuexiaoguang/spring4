package org.springframework.beans.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 操作Bean工厂的方法, 特别是在{@link ListableBeanFactory}接口上.
 *
 * <p>返回bean计数, ​​bean名称或bean实例, 同时考虑bean工厂的嵌套层次结构
 * (ListableBeanFactory接口上定义的方法不会, 与BeanFactory接口上定义的方法形成对比).
 */
public abstract class BeanFactoryUtils {

	/**
	 * 生成的bean名称的分隔符.
	 * 如果类名或父级名称不是唯一的, 将插入"#1", "#2" 等, 直到名称变得唯一.
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";


	/**
	 * 返回给定名称是否为工厂取消引用 (从工厂取消引用前缀开始).
	 * 
	 * @param name bean的名称
	 * 
	 * @return 给定名称是否为工厂取消引用
	 */
	public static boolean isFactoryDereference(String name) {
		return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
	}

	/**
	 * 返回实际的bean名称, 剥离工厂取消引用前缀 (还剥离重复的工厂前缀).
	 * 
	 * @param name bean的名称
	 * 
	 * @return 改造后的名字
	 */
	public static String transformedBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		String beanName = name;
		while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
			beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
		}
		return beanName;
	}

	/**
	 * 返回给定名称是否是由默认命名策略生成的bean名称 (包含 "#..." 部分).
	 * 
	 * @param name bean的名称
	 * 
	 * @return 给定名称是否是生成的bean名称
	 */
	public static boolean isGeneratedBeanName(String name) {
		return (name != null && name.contains(GENERATED_BEAN_NAME_SEPARATOR));
	}

	/**
	 * 从给定（可能生成的）bean名称中提取“原始”bean名称, 排除可能为唯一性添加的任何“＃...”后缀.
	 * 
	 * @param name 可能是生成的bean名称
	 * 
	 * @return 原始bean名称
	 */
	public static String originalBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		int separatorIndex = name.indexOf(GENERATED_BEAN_NAME_SEPARATOR);
		return (separatorIndex != -1 ? name.substring(0, separatorIndex) : name);
	}


	/**
	 * 计算此工厂参与的任何层次结构中的所有bean. 包括祖先bean工厂的数量.
	 * <p>被“覆盖”的bean（在具有相同名称的后代工厂中指定）仅计数一次.
	 * 
	 * @param lbf bean工厂
	 * 
	 * @return bean类数量, 包括祖先工厂中定义的bean类数量
	 */
	public static int countBeansIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesIncludingAncestors(lbf).length;
	}

	/**
	 * 返回工厂中的所有bean名称, 包括祖先工厂.
	 * 
	 * @param lbf bean工厂
	 * 
	 * @return 匹配bean名称的数组, 或空数组
	 */
	public static String[] beanNamesIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesForTypeIncludingAncestors(lbf, Object.class);
	}

	/**
	 * 获取给定类型的所有bean名称, 包括在祖先工厂中定义的那些. 在重写bean定义的情况下将返回唯一名称.
	 * <p>考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * <p>此版本的{@code beanNamesForTypeIncludingAncestors}自动包含原型和FactoryBeans.
	 * 
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型 (as a {@code ResolvableType})
	 * 
	 * @return 匹配的bean名称的数组, 或空数组
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, ResolvableType type) {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有bean名称, 包括在祖先工厂中定义的那些. 在重写bean定义的情况下将返回唯一名称.
	 * <p>考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * <p>此版本的{@code beanNamesForTypeIncludingAncestors}自动包含原型和FactoryBeans.
	 * 
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型 (as a {@code Class})
	 * 
	 * @return 匹配的bean名称的数组, 或空数组
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, Class<?> type) {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有bean名称, 包括在祖先工厂中定义的那些. 在重写bean定义的情况下将返回唯一名称.
	 * <p>如果设置了“allowEagerInit”标志, 是否考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * 如果未设置“allowEagerInit”, 只检查原始FactoryBeans (这不需要每个FactoryBean都初始化).
	 * 
	 * @param lbf bean工厂
	 * @param includeNonSingletons 是否包括原型或范围Bean或只是单例 (也适用于FactoryBeans)
	 * @param allowEagerInit 是否初始化<i>lazy-init单例</i>和<i>由FactoryBeans创建的对象</i>（或通过带有“factory-bean”引用的工厂方法）进行类型检查.
	 * 请注意, 需要实时地初始化FactoryBeans以确定其类型:
	 * 因此请注意, 为此标志传入“true”将初始化FactoryBeans和“factory-bean”引用.
	 * @param type bean必须匹配的类型
	 * 
	 * @return 匹配的bean名称的数组, 或空数组
	 */
	public static String[] beanNamesForTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 返回给定类型或子类型的所有bean, 如果当前bean工厂是HierarchicalBeanFactory, 还会拾取祖先bean工厂中定义的bean.
	 * 返回的Map将只包含此类型的bean.
	 * <p>考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * <p><b>Note: 对于同名的Bean类, “最低等级”的工厂优先,
	 * i.e. 这些Bean将从发现它们的最低等级的工厂返回, 隐藏在祖先工厂中相应的bean.</b>
	 * 此功能允许通过在子工厂中显式选择相同的bean名称来“替换”bean; 那时祖先工厂里的bean是不可见的, 甚至不用于类型查找.
	 * 
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型
	 * 
	 * @return 匹配的bean实例的Map, 或空 Map
	 * @throws BeansException 如果无法创建bean
	 */
	public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> result = new LinkedHashMap<String, T>(4);
		result.putAll(lbf.getBeansOfType(type));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				Map<String, T> parentResult = beansOfTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type);
				for (Map.Entry<String, T> entry : parentResult.entrySet()) {
					String beanName = entry.getKey();
					if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
						result.put(beanName, entry.getValue());
					}
				}
			}
		}
		return result;
	}

	/**
	 * 返回给定类型或子类型的所有bean, 如果当前bean工厂是HierarchicalBeanFactory, 还会拾取祖先bean工厂中定义的bean.
	 * 返回的Map将只包含此类型的bean.
	 * <p>如果设置了“allowEagerInit”标志, 是否考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * 如果未设置“allowEagerInit”, 只检查原始FactoryBeans (这不需要每个FactoryBean都初始化).
	 * <p><b>Note: 对于同名的Bean类, “最低等级”的工厂优先,
	 * i.e. 这些Bean将从发现它们的最低等级的工厂返回, 隐藏在祖先工厂中相应的bean.</b>
	 * 此功能允许通过在子工厂中显式选择相同的bean名称来“替换”bean; 那时祖先工厂里的bean是不可见的, 甚至不用于类型查找.
	 * 
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型
	 * @param includeNonSingletons 是否包括原型或范围Bean或只是单例 (也适用于FactoryBeans)
	 * @param allowEagerInit 是否初始化<i>lazy-init单例</i>和<i>由FactoryBeans创建的对象</i>（或通过带有“factory-bean”引用的工厂方法）进行类型检查.
	 * 请注意, 需要实时地初始化FactoryBeans以确定其类型:
	 * 因此请注意, 为此标志传入“true”将初始化FactoryBeans和“factory-bean”引用.
	 * 
	 * @return 匹配的bean实例的Map, 或空 Map
	 * @throws BeansException 如果无法创建bean
	 */
	public static <T> Map<String, T> beansOfTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> result = new LinkedHashMap<String, T>(4);
		result.putAll(lbf.getBeansOfType(type, includeNonSingletons, allowEagerInit));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				Map<String, T> parentResult = beansOfTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
				for (Map.Entry<String, T> entry : parentResult.entrySet()) {
					String beanName = entry.getKey();
					if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
						result.put(beanName, entry.getValue());
					}
				}
			}
		}
		return result;
	}


	/**
	 * 返回给定类型或子类型的所有bean, 如果当前bean工厂是HierarchicalBeanFactory, 还会拾取祖先bean工厂中定义的bean.
	 * 当期望单个bean并且不关心bean名称时, 很有用.
	 * <p>考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * <p>此版本的{@code beanOfTypeIncludingAncestors}自动包含原型和FactoryBeans.
	 * <p><b>Note: 对于同名的Bean类, “最低等级”的工厂优先,
	 * i.e. 这些Bean将从发现它们的最低等级的工厂返回, 隐藏在祖先工厂中相应的bean.</b>
	 * 此功能允许通过在子工厂中显式选择相同的bean名称来“替换”bean; 那时祖先工厂里的bean是不可见的, 甚至不用于类型查找.
	 * 
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型
	 * 
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 */
	public static <T> T beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
			throws BeansException {

		Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或子类型的所有bean, 如果当前bean工厂是HierarchicalBeanFactory, 还会拾取祖先bean工厂中定义的bean.
	 * 当期望单个bean并且不关心bean名称时, 很有用.
	 * <p>如果设置了“allowEagerInit”标志, 是否考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * 如果未设置“allowEagerInit”, 只检查原始FactoryBeans (这不需要每个FactoryBean都初始化).
	 * <p><b>Note: 对于同名的Bean类, “最低等级”的工厂优先,
	 * i.e. 这些Bean将从发现它们的最低等级的工厂返回, 隐藏在祖先工厂中相应的bean.</b>
	 * 此功能允许通过在子工厂中显式选择相同的bean名称来“替换”bean; 那时祖先工厂里的bean是不可见的, 甚至不用于类型查找.
	 * 
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型
	 * @param includeNonSingletons 是否包括原型或范围Bean或只是单例 (也适用于FactoryBeans)
	 * @param allowEagerInit 是否初始化<i>lazy-init单例</i>和<i>由FactoryBeans创建的对象</i>（或通过带有“factory-bean”引用的工厂方法）进行类型检查.
	 * 请注意, 需要实时地初始化FactoryBeans以确定其类型:
	 * 因此请注意, 为此标志传入“true”将初始化FactoryBeans和“factory-bean”引用.
	 * 
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 */
	public static <T> T beanOfTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type, includeNonSingletons, allowEagerInit);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或子类型的单个bean, 不在祖先工厂中查找.
	 * 当期望单个bean并且不关心bean名称时, 很有用.
	 * <p>考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * <p>此版本的{@code beanOfType}自动包含原型和FactoryBeans.
	 * 
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型
	 * 
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 */
	public static <T> T beanOfType(ListableBeanFactory lbf, Class<T> type) throws BeansException {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> beansOfType = lbf.getBeansOfType(type);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或子类型的单个bean, 不在祖先工厂中查找.
	 * 当期望单个bean并且不关心bean名称时, 很有用.
	 * <p>如果设置了“allowEagerInit”标志, 是否考虑FactoryBeans创建的对象, 这意味着FactoryBeans将被初始化.
	 * 如果FactoryBean创建的对象不匹配, 原始FactoryBean本身将与该类型匹配.
	 * 如果未设置“allowEagerInit”, 只检查原始FactoryBeans (这不需要每个FactoryBean都初始化).
	 * 
	 * @param lbf bean工厂
	 * @param type bean必须匹配的类型
	 * @param includeNonSingletons是否包括原型或范围Bean或只是单例 (也适用于FactoryBeans)
	 * @param allowEagerInit 是否初始化<i>lazy-init单例</i>和<i>由FactoryBeans创建的对象</i>（或通过带有“factory-bean”引用的工厂方法）进行类型检查.
	 * 请注意, 需要实时地初始化FactoryBeans以确定其类型:
	 * 因此请注意, 为此标志传入“true”将初始化FactoryBeans和“factory-bean”引用.
	 * 
	 * @return 匹配的bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 * @throws BeansException 如果无法创建bean
	 */
	public static <T> T beanOfType(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> beansOfType = lbf.getBeansOfType(type, includeNonSingletons, allowEagerInit);
		return uniqueBean(type, beansOfType);
	}


	/**
	 * 将给定的bean名称结果与给定的父级结果合并.
	 * 
	 * @param result 本地bean名称结果
	 * @param parentResult 父级bean名称结果 (possibly empty)
	 * @param hbf 本地bean工厂
	 * 
	 * @return 合并后的结果 (可能是本地的结果)
	 */
	private static String[] mergeNamesWithParent(String[] result, String[] parentResult, HierarchicalBeanFactory hbf) {
		if (parentResult.length == 0) {
			return result;
		}
		List<String> merged = new ArrayList<String>(result.length + parentResult.length);
		merged.addAll(Arrays.asList(result));
		for (String beanName : parentResult) {
			if (!merged.contains(beanName) && !hbf.containsLocalBean(beanName)) {
				merged.add(beanName);
			}
		}
		return StringUtils.toStringArray(merged);
	}

	/**
	 * 从给定的匹配bean Map中提取给定类型的唯一bean.
	 * 
	 * @param type 要匹配的bean类型
	 * @param matchingBeans 所有匹配的bean
	 * 
	 * @return 唯一的bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到给定类型的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的bean
	 */
	private static <T> T uniqueBean(Class<T> type, Map<String, T> matchingBeans) {
		int count = matchingBeans.size();
		if (count == 1) {
			return matchingBeans.values().iterator().next();
		}
		else if (count > 1) {
			throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
		}
		else {
			throw new NoSuchBeanDefinitionException(type);
		}
	}
}
