package org.springframework.http.client.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingAsyncClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;

/**
 * The HTTP accessor that extends the base {@link AsyncHttpAccessor} with
 * request intercepting functionality.
 */
public abstract class InterceptingAsyncHttpAccessor extends AsyncHttpAccessor {

    private List<AsyncClientHttpRequestInterceptor> interceptors =
            new ArrayList<AsyncClientHttpRequestInterceptor>();


    /**
     * Set the request interceptors that this accessor should use.
     * @param interceptors the list of interceptors
     */
    public void setInterceptors(List<AsyncClientHttpRequestInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    /**
     * Return the request interceptor that this accessor uses.
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
