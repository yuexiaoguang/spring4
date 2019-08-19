package org.springframework.web.servlet.support;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.FlashMap;
import org.springframework.web.util.WebUtils;

/**
 * 在HTTP会话中存储和检索{@link FlashMap}实例.
 */
public class SessionFlashMapManager extends AbstractFlashMapManager {

	private static final String FLASH_MAPS_SESSION_ATTRIBUTE = SessionFlashMapManager.class.getName() + ".FLASH_MAPS";


	/**
	 * 从HTTP会话中检索已保存的FlashMap实例.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return (session != null ? (List<FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE) : null);
	}

	/**
	 * 将给定的FlashMap实例保存在HTTP会话中.
	 */
	@Override
	protected void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request, HttpServletResponse response) {
		WebUtils.setSessionAttribute(request, FLASH_MAPS_SESSION_ATTRIBUTE, (!flashMaps.isEmpty() ? flashMaps : null));
	}

	/**
	 * 公开最佳可用会话互斥锁.
	 */
	@Override
	protected Object getFlashMapsMutex(HttpServletRequest request) {
		return WebUtils.getSessionMutex(request.getSession());
	}
}
