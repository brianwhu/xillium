package org.xillium.gear.auth;

import java.net.*;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.Cookie;
import org.xillium.base.beans.Strings;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.*;
import org.xillium.core.*;
import org.springframework.transaction.annotation.Transactional;


/**
 * An Authenticator that stores credential and session data in databases.
 */
public class DatabaseBackedAuthenticator extends PageAwareAuthenticator {
	private static final Logger _logger = Logger.getLogger(DatabaseBackedAuthenticator.class.getName());

	public static final String USERNAME = "_username_";
	public static final String PASSWORD = "_password_";
    public static final String AUTHCODE = "_authcode_";

    private static final long DEFAULT_TIMEOUT = 300000; // 5 minutes
    private final long _timeout;
	private final Persistence _persistence;
    private final String IdentityName;
    private final String RetrieveRolesByCredential;
    private final String RetrieveRolesBySession;
    private final String UpdateSecureSession;
	private final Map<String, Permission[]> _roles = new HashMap<String, Permission[]>(); // a map from role name to authorized function names
	private final SecureRandom _random = new SecureRandom();
    //private String _page;

    /**
     * Constructs a DatabaseBackedAuthenticator.
     *
     * @param persist - a Persistence object
     * @param timeout - secure session timeout in seconds
     * @param identityName - the name of the identity parameter in data binder
     * @param qRolesByCredential - a ParametricStatement that retrieves roles by given credential
     * @param qRolesBySession - a ParametricStatement that retrieves roles by secure session
     * @param uSecureSession - a ParametricStatement that updates a secure session with the latest data
     */
	public DatabaseBackedAuthenticator(Persistence persist, int timeout, String identityName,
        String qRolesByCredential,
        String qRolesBySession,
        String uSecureSession
    ) throws java.net.UnknownHostException {
		_persistence = persist;
        _timeout = timeout >= 0 ? TimeUnit.SECONDS.toMillis(timeout) : DEFAULT_TIMEOUT;
        IdentityName = identityName;
        RetrieveRolesByCredential = qRolesByCredential;
        RetrieveRolesBySession = qRolesBySession;
        UpdateSecureSession = uSecureSession;
        _random.setSeed(InetAddress.getLocalHost().getAddress());
	}

/*
    public void setAuthenticationPage(String page) {
        _page = page;
    }
*/

	protected Credential collectCredential(DataBinder parameters) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // allow clients to use either <IdentityName> or USERNAME; if both are present <IdentityName> takes precedence
_logger.log(Level.FINE, "looking for {0} in data binder", IdentityName);
        String id = null;
        if ((id = parameters.get(IdentityName)) != null && id.length() > 0) {
            parameters.put(USERNAME, id);
        } else if ((id = parameters.get(USERNAME)) != null && id.length() > 0) {
            parameters.put(IdentityName, id);
        } else {
            return null;
        }
_logger.log(Level.FINE, "got identity = {0}", id);

        String password = parameters.remove(PASSWORD);
_logger.log(Level.FINE, "got password = {0}", password);
        if (password != null && password.length() > 0) {
            return new Credential(id, Strings.hash(password));
        } else {
            return null;
        }
	}

    protected Session collectSession(DataBinder parameters) throws UnsupportedEncodingException {
        Session session = null;
/*
        String id = parameters.get(USERNAME);
        if (id == null || id.length() == 0) {
            return null;
        }
*/
        String token = parameters.get(AUTHCODE);
        if (token != null && token.length() > 0) {
_logger.log(Level.FINE, "found session in binder: " + token);
            session = new Session(token);
            parameters.put(IdentityName, session.id);
        } else {
            Cookie[] cookies = (Cookie[])parameters.getNamedObject(Service.REQUEST_HTTP_COOKIE);
            if (cookies != null) for (int i = 0; i < cookies.length; ++i) {
                if (cookies[i].getName().equals(AUTHCODE)) {
_logger.log(Level.FINE, "found session in cookie: " + cookies[i].getValue());
                    session = new Session(URLDecoder.decode(cookies[i].getValue(), "UTF-8"));
                    parameters.put(IdentityName, session.id);
                    break;
                }
            }
        }

        return session;
    }

    private String createSecureToken() throws UnsupportedEncodingException {
        byte[] bytes = new byte[20];
        _random.nextBytes(bytes);
        return Strings.toHexString(bytes).toUpperCase();
    }

/*
    private void redirectToAuthenticationPage(DataBinder binder) {
        if (_page != null) {
            binder.map(Service.SERVICE_HTTP_HEADER, String.class, String.class).put("Content-Type", "text/html; charset=utf-8");
            binder.put(Service.SERVICE_PAGE_TARGET, _page + "?_redirect_=" + binder.get(Service.REQUEST_TARGET_PATH));
_logger.log(Level.FINE, "redirecting to {0}", binder.get(Service.SERVICE_PAGE_TARGET));
        }
    }
*/

    @Transactional
    public List<Role> authenticate(DataBinder parameters) throws AuthorizationException {
        try {
            long now = System.currentTimeMillis();
            Credential credential = collectCredential(parameters);
            if (credential != null) {
_logger.log(Level.FINE, "credential: {0}", org.xillium.base.beans.Beans.toString(credential));
                List<Role> roles = _persistence.getResults(RetrieveRolesByCredential, credential);
_logger.log(Level.FINE, "# of roles under this credential: {0}", roles.size());
                if (roles.size() > 0) {
_logger.log(Level.FINE, "session timeout: {0}", _timeout);
                    if (_timeout > 0) { // is session authentication enabled?
                        Session session = new Session(credential.id, createSecureToken(), now);
_logger.log(Level.FINE, "updating session {0}", org.xillium.base.beans.Beans.toString(session));
                        _persistence.executeUpdate(UpdateSecureSession, session);
                        String ticket = URLEncoder.encode(session.id + Session.AT + session.token, "UTF-8");
                        parameters.put(AUTHCODE, ticket);
                        // place the new authcode in a session cookie for the client
_logger.log(Level.FINE, "Sending ticket in cookie: {0}", ticket);
                        Map<String, String> headers = parameters.map(Service.SERVICE_HTTP_HEADER, String.class, String.class);
                        if (parameters.get(Service.REQUEST_HTTP_SECURE) != null) {
                            headers.put("Set-Cookie", AUTHCODE + "=" + ticket + ";path=/;secure");
                        } else {
                            headers.put("Set-Cookie", AUTHCODE + "=" + ticket + ";path=/");
                        }
                    }
                    return roles;
                    //authorize(deployment, roles);
                } else {
                    throw new AuthorizationException("InvalidCredential");
                }
            } else if (_timeout > 0) {
                Session session = collectSession(parameters);
                if (session != null) {
                    session.clock = now;
                    session.maxAge = _timeout;
_logger.log(Level.FINE, "Attempt to authorized with session {0}", org.xillium.base.beans.Beans.toString(session));
                    List<Role> roles = _persistence.getResults(RetrieveRolesBySession, session);
                    if (roles.size() > 0) {
                        _persistence.executeUpdate(UpdateSecureSession, session);
                        return roles;
                        //authorize(deployment, roles);
                    } else {
                        _logger.log(Level.WARNING, "merchant:"+session.id+",token:"+session.token);
                        parameters.remove(AUTHCODE);
                        throw new AuthorizationException("InvalidSession");
                    }
                } else {
                    throw new AuthenticationRequiredException("AuthenticationRequired");
                }
            } else {
                throw new AuthenticationRequiredException("AuthenticationRequired");
            }
        } catch (AuthorizationException x) {
            redirectToAuthenticationPage(parameters);
            throw x;
        } catch (Exception x) {
            redirectToAuthenticationPage(parameters);
            throw new AuthorizationException(x.getMessage(), x);
        }
    }
}
