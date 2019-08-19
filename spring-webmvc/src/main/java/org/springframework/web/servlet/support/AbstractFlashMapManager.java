package org.springframework.web.servlet.support;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link FlashMapManager}实现的基类.
 */
public abstract class AbstractFlashMapManager implements FlashMapManager {

	private static final Object DEFAULT_FLASH_MAPS_MUTEX = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	private int flashMapTimeout = 180;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 设置保存{@link FlashMap} (请求完成后)的过期时间, 以秒为单位.
	 * <p>默认为 180 秒.
	 */
	public void setFlashMapTimeout(int flashMapTimeout) {
		this.flashMapTimeout = flashMapTimeout;
	}

	/**
	 * 返回FlashMap过期前的秒数.
	 */
	public int getFlashMapTimeout() {
		return this.flashMapTimeout;
	}

	/**
	 * 设置UrlPathHelper以将FlashMap实例与请求进行匹配.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回要使用的UrlPathHelper实现.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	@Override
	public final FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response) {
		List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
		if (CollectionUtils.isEmpty(allFlashMaps)) {
			return null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved FlashMap(s): " + allFlashMaps);
		}
		List<FlashMap> mapsToRemove = getExpiredFlashMaps(allFlashMaps);
		FlashMap match = getMatchingFlashMap(allFlashMaps, request);
		if (match != null) {
			mapsToRemove.add(match);
		}

		if (!mapsToRemove.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Removing FlashMap(s): " + mapsToRemove);
			}
			Object mutex = getFlashMapsMutex(request);
			if (mutex != null) {
				synchronized (mutex) {
					allFlashMaps = retrieveFlashMaps(request);
					if (allFlashMaps != null) {
						allFlashMaps.removeAll(mapsToRemove);
						updateFlashMaps(allFlashMaps, request, response);
					}
				}
			}
			else {
				allFlashMaps.removeAll(mapsToRemove);
				updateFlashMaps(allFlashMaps, request, response);
			}
		}

		return match;
	}

	/**
	 * 返回给定列表中包含的过期FlashMap实例列表.
	 */
	private List<FlashMap> getExpiredFlashMaps(List<FlashMap> allMaps) {
		List<FlashMap> result = new LinkedList<FlashMap>();
		for (FlashMap map : allMaps) {
			if (map.isExpired()) {
				result.add(map);
			}
		}
		return result;
	}

	/**
	 * 返回给定列表中包含的与请求匹配的FlashMap.
	 * 
	 * @return 匹配的FlashMap 或{@code null}
	 */
	private FlashMap getMatchingFlashMap(List<FlashMap> allMaps, HttpServletRequest request) {
		List<FlashMap> result = new LinkedList<FlashMap>();
		for (FlashMap flashMap : allMaps) {
			if (isFlashMapForRequest(flashMap, request)) {
				result.add(flashMap);
			}
		}
		if (!result.isEmpty()) {
			Collections.sort(result);
			if (logger.isDebugEnabled()) {
				logger.debug("Found matching FlashMap(s): " + result);
			}
			return result.get(0);
		}
		return null;
	}

	/**
	 * 给定的FlashMap是否与当前请求匹配.
	 * 使用FlashMap中保存的预期请求路径和查询参数.
	 */
	protected boolean isFlashMapForRequest(FlashMap flashMap, HttpServletRequest request) {
		String expectedPath = flashMap.getTargetRequestPath();
		if (expectedPath != null) {
			String requestUri = getUrlPathHelper().getOriginatingRequestUri(request);
			if (!requestUri.equals(expectedPath) && !requestUri.equals(expectedPath + "/")) {
				return false;
			}
		}
		MultiValueMap<String, String> actualParams = getOriginatingRequestParams(request);
		MultiValueMap<String, String> expectedParams = flashMap.getTargetRequestParams();
		for (String expectedName : expectedParams.keySet()) {
			List<String> actualValues = actualParams.get(expectedName);
			if (actualValues == null) {
				return false;
			}
			for (String expectedValue : expectedParams.get(expectedName)) {
				if (!actualValues.contains(expectedValue)) {
					return false;
				}
			}
		}
		return true;
	}

	private MultiValueMap<String, String> getOriginatingRequestParams(HttpServletRequest request) {
		String query = getUrlPathHelper().getOriginatingQueryString(request);
		return ServletUriComponentsBuilder.fromPath("/").query(query).build().getQueryParams();
	}

	@Override
	public final void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response) {
		if (CollectionUtils.isEmpty(flashMap)) {
			return;
		}

		String path = decodeAndNormalizePath(flashMap.getTargetRequestPath(), request);
		flashMap.setTargetRequestPath(path);

		if (logger.isDebugEnabled()) {
			logger.debug("Saving FlashMap=" + flashMap);
		}
		flashMap.startExpirationPeriod(getFlashMapTimeout());

		Object mutex = getFlashMapsMutex(request);
		if (mutex != null) {
			synchronized (mutex) {
				List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
				allFlashMaps = (allFlashMaps != null ? allFlashMaps : new CopyOnWriteArrayList<FlashMap>());
				allFlashMaps.add(flashMap);
				updateFlashMaps(allFlashMaps, request, response);
			}
		}
		else {
			List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
			allFlashMaps = (allFlashMaps != null ? allFlashMaps : new LinkedList<FlashMap>());
			allFlashMaps.add(flashMap);
			updateFlashMaps(allFlashMaps, request, response);
		}
	}

	private String decodeAndNormalizePath(String path, HttpServletRequest request) {
		if (path != null) {
			path = getUrlPathHelper().decodeRequestString(request, path);
			if (path.charAt(0) != '/') {
				String requestUri = getUrlPathHelper().getRequestUri(request);
				path = requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + path;
				path = StringUtils.cleanPath(path);
			}
		}
		return path;
	}

	/**
	 * 从底层存储中检索已保存的FlashMap实例.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return FlashMap实例的列表, 或{@code null}
	 */
	protected abstract List<FlashMap> retrieveFlashMaps(HttpServletRequest request);

	/**
	 * 更新底层存储中的FlashMap实例.
	 * 
	 * @param flashMaps 要保存的FlashMap实例的(可能为空)列表
	 * @param request 当前的请求
	 * @param response 当前的响应
	 */
	protected abstract void updateFlashMaps(
			List<FlashMap> flashMaps, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 获取由{@link #retrieveFlashMaps}和{@link #updateFlashMaps}处理的用于修改FlashMap列表的互斥锁.
	 * <p>默认实现返回共享静态互斥锁.
	 * 鼓励子类返回更具体的互斥锁, 或{@code null}以指示不需要同步.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 要使用的互斥锁 (可能是{@code null})
	 */
	protected Object getFlashMapsMutex(HttpServletRequest request) {
		return DEFAULT_FLASH_MAPS_MUTEX;
	}
}
