package org.xillium.gear.auth;

import java.util.*;
import java.util.logging.*;
import java.security.cert.X509Certificate;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.*;
import org.xillium.data.*;
import org.xillium.core.*;
import org.springframework.transaction.annotation.Transactional;


/**
 * An Authenticator that authenticates clients by checking their X509 certificates against claimed identities.
 */
public class X509CertificateAuthenticator extends PageAwareAuthenticator {
	private static final Logger _logger = Logger.getLogger(X509CertificateAuthenticator.class.getName());

	private final Persistence _persistence;
    private final String _identityName;
    private final String _qRolesBySubjectName;
    private boolean _useFullSubjectName;

    /**
     * Constructs an X509CertificateAuthenticator.
     *
     * @param persist - a Persistence object
     * @param identityName - the name of the identity parameter in the data binder
     * @param qRolesBySubjectName - a ParametricStatement that retrieves roles by a credential consisting of an identity name and a password.
     *        e.g.
     *  <xmp>
     *  SELECT
     *      MARKET_ID ID, 'market' ROLE_ID, 1 PERMISSION, 1 PREREQUISITE
     *  FROM
     *      MARKETS
     *  WHERE
     *      MARKET_ID = :id:VARCHAR
     *      AND
     *      SUBJECT_NAME = :password:VARCHAR
     *  </xmp>
     */
	public X509CertificateAuthenticator(Persistence persist, String identityName, String qRolesBySubjectName) {
		_persistence = persist;
        _identityName = identityName;
        _qRolesBySubjectName = qRolesBySubjectName;
	}

    /**
     * Sets whether to check the full subject name. By default, this authenticator only checks the common name part of the subject name inside
     * the X509 certificate.
     */
    public void setUseFullSubjectName(boolean full) {
        _useFullSubjectName = full;
    }

    @Override
    @Transactional(readOnly=true)
    public List<Role> authenticate(DataBinder binder) throws AuthorizationException {
        try {
            HttpServletRequest req = (HttpServletRequest)binder.getNamedObject(Service.REQUEST_SERVLET_REQ);
            X509Certificate[] certs = (X509Certificate[])req.getAttribute("javax.servlet.request.X509Certificate");

            if (certs != null && certs.length != 0) {
                Credential credential = new Credential(binder.get(_identityName), getPrincipalIdentity(certs[0].getSubjectX500Principal().getName()));
                _logger.fine(org.xillium.base.beans.Beans.toString(credential));
                List<Role> roles = _persistence.getResults(_qRolesBySubjectName, credential, Role.class);
                for (int i = 1; i < certs.length; ++i) {
                    credential.password = getPrincipalIdentity(certs[i].getSubjectX500Principal().getName());
                    _logger.fine(org.xillium.base.beans.Beans.toString(credential));
                    roles.addAll(_persistence.getResults(_qRolesBySubjectName, credential, Role.class));
                }
                if (roles.size() > 0) {
                    return roles;
                } else {
                    throw new AuthorizationException("InvalidSubjectName");
                }
            } else {
                throw new AuthenticationRequiredException("AuthenticationRequired");
            }
        } catch (AuthorizationException x) {
            redirectToAuthenticationPage(binder);
            throw x;
        } catch (Exception x) {
            redirectToAuthenticationPage(binder);
            throw new AuthorizationException(x.getMessage(), x);
        }
    }

    private final String getPrincipalIdentity(String name) throws InvalidNameException {
        if (_useFullSubjectName) {
            return name;
        } else {
            for(Rdn rdn: new LdapName(name).getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) return rdn.getValue().toString();
            }
            throw new InvalidNameException("NoCommonNameInSubjectName");
        }
    }
}
