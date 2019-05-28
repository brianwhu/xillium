package org.xillium.gear.auth;

import org.xillium.data.DataBinder;
import org.xillium.core.Service;


/**
 * An Authenticator that supports page redirection.
 */
public abstract class PageAwareAuthenticator implements Authenticator {
    private String _page;

    /**
     * Configures an authentication page.
     */
    public void setAuthenticationPage(String page) {
        _page = page;
    }

    /*#
     * Redirects to the configured authentication page, passing the target service as parameter "_redirect_".
     */
    protected void redirectToAuthenticationPage(DataBinder binder) {
        if (_page != null) {
            binder.mul(Service.SERVICE_HTTP_HEADER, String.class, String.class).add("Content-Type", "text/html; charset=utf-8");
            binder.put(Service.SERVICE_PAGE_TARGET, _page + "?_redirect_=" + binder.get(Service.REQUEST_TARGET_PATH));
        }
    }
}
