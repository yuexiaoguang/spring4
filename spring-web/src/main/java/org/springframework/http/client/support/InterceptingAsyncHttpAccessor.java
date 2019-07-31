package org.springframework.http.client.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingAsyncClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;

/**
 * 使用请求拦截功能扩展基础{@link AsyncHttpAccessor}的HTTP访问器.
 */
public abstract class InterceptingAsyncHttpAccessor extends AsyncHttpAccessor {

    private List<AsyncClientHttpRequestInterceptor> interceptors =
            new ArrayList<AsyncClientHttpRequestInterceptor>();


    /**
     * 设置此访问者应使用的请求拦截器.
     * 
     * @param interceptors 拦截器列表
     */
    public void setInterceptors(List<AsyncClientHttpRequestInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    /**
     * 返回此访问者使用的请求拦截器.
     */
    public List<AsyncClientHttpRequestInterceptor> getInterceptors() {
        return this.interceptors;
    }


    @Override
    public AsyncClientHttpRequestFactory getAsyncRequestFactory() {
        AsyncClientHttpRequestFactory delegate = super.getAsyncRequestFactory();
        if (!CollectionUtils.isEmpty(getInterceptors())) {
            return new InterceptingAsyncClientHttpRequestFactory(delegate, getInterceptors());
        }
        else {
            return delegate;
        }
    }

}
