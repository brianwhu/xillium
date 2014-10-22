package org.xillium.gear.auth;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.*;
import javax.xml.bind.DatatypeConverter;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.core.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.annotation.Transactional;


/**
 * An Authenticator that authenticates clients by checking their X509 certificates against claimed identities.
 */
public class X509CertificateAuthenticator extends PageAwareAuthenticator {
	private static final Logger _logger = Logger.getLogger(X509CertificateAuthenticator.class.getName());

	private final Persistence _persistence;
    private final String _identityName;
    private final String _qRolesByCredential;
    private final Map<Credential, List<Role>> _roles;

    private String _headerHoldingCert;
    private boolean _useFullSubjectName;

    /**
     * Constructs an X509CertificateAuthenticator that performs identity check on database.
     *
     * @param persist - a Persistence object
     * @param identityName - the name of the identity parameter in the data binder
     * @param qRolesByCredential - a ParametricStatement that retrieves roles by an id (the identity obtained from a request
     *        parameter in the data binder) and a password (the identity obtained from the client certificate). e.g.
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
	public X509CertificateAuthenticator(Persistence persist, String identityName, String qRolesByCredential) {
		_persistence = persist;
        _identityName = identityName;
        _qRolesByCredential = qRolesByCredential;
        _roles = null;
	}

    /**
     * Constructs an X509CertificateAuthenticator that performs identity check on roles files.
     *
     * @param identityName - the name of the identity parameter in the data binder
     * @param locations - a list of roles file locations or location patterns, as supported by
     * org.springframework.core.io.support.PathMatchingResourcePatternResolver
     */
    @SuppressWarnings("unchecked")
    public X509CertificateAuthenticator(String identityName, List<String> locations) throws Exception {
        _persistence = null;
        _identityName = identityName;
        _qRolesByCredential = null;
        _roles = new TreeMap<Credential, List<Role>>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        XMLBeanAssembler assembler = new XMLBeanAssembler(new DefaultObjectFactory());
        for (String location: locations) {
            try {
                for (Resource resource: resolver.getResources(location)) {
                    try {
                        for (Authorization authorization: (List<Authorization>)assembler.build(resource.getInputStream())) {
                            _roles.put(authorization.credential, authorization.roles);
                        }
                    } catch (Exception x) {
                        _logger.log(Level.WARNING, x.getMessage(), x);
                    }
                }
            } catch (Exception x) {
                _logger.log(Level.WARNING, x.getMessage(), x);
            }
        }
    }

    /**
     * Sets whether to check the full subject name. By default, this authenticator only checks the common name part of the subject name inside
     * the X509 certificate.
     */
    public void setUseFullSubjectName(boolean full) {
        _useFullSubjectName = full;
    }

    /**
     * Sets whether to obtain client certificates from an HTTP header. This is useful if the application server is behind an HTTPS reverse proxy.
     */
    public void setCertificateHeader(String name) {
        _headerHoldingCert = name;
    }

    @Override
    @Transactional(readOnly=true)
    public List<Role> authenticate(DataBinder binder) throws AuthorizationException {
        try {
            X509Certificate certificate = null;

            HttpServletRequest req = (HttpServletRequest)binder.getNamedObject(Service.REQUEST_SERVLET_REQ);
            X509Certificate[] certs = (X509Certificate[])req.getAttribute("javax.servlet.request.X509Certificate");
            if (certs != null && certs.length > 0) {
                certificate = certs[0];
            } else if (_headerHoldingCert != null) {
                InputStream data = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(req.getHeader(_headerHoldingCert)));
                certificate = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(data);
                data.close();
            }
            _logger.fine(certificate.toString());

            if (certificate != null) {
                Credential credential = new Credential(binder.get(_identityName), getPrincipalIdentity(certificate.getSubjectX500Principal().getName()));
                _logger.fine(org.xillium.base.beans.Beans.toString(credential));
                List<Role> roles = (_persistence != null) ? _persistence.getResults(_qRolesByCredential, credential, Role.class) : _roles.get(credential);
                if (roles != null && roles.size() > 0) {
                    return roles;
                } else {
                    throw new AuthorizationException("InvalidCertificateIdentity{" + credential.password + '}');
                }
            } else {
                _logger.info("no client certificate in request");
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
