package org.xillium.core;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
//import org.json.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.context.support.GenericApplicationContext;
//import org.springframework.transaction.TransactionDefinition;
//import org.springframework.transaction.TransactionStatus;
//import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.context.*;
import org.springframework.web.context.support.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.xillium.core.conf.*;
import org.xillium.core.intrinsic.*;


/**
 * Platform Service Dispatcher.
 *
 * This servlet dispatches inbound HTTP calls to registered services based on request URI. A valid request URI is in the form of
 * <pre>
 *      /[context]/[module]/[service]?[params]=...
 * </pre>
 * When a request URI matches the above pattern, this servlet looks up a Service instance registered under the name 'module/service'.
 * <p/>
 * The fabric of operation, administration, and maintenance (foam)
 * <ul>
 *	<li><code>/[context]/x!ll!um/[service]</code><p/>
 *		<ul>
 *			<li>service_list</li>
 *			<li>service_desc - parameter description</li>
 *		</ul>
 *	</li>
 * </ul>
 */
public class HttpServiceDispatcher extends HttpServlet {
    private static final String MODULE_NAME = "Xillium-Module-Name";
    private static final String REQUEST_VOCABULARY = "request-vocabulary.xml";
    private static final String SERVICE_CONFIG = "service-configuration.xml";
    private static final String STORAGE_CONFIG = "storage-configuration.xml";
    private static final Pattern URI_REGEX = Pattern.compile("/[^/?]+/([^/?]+/[^/?]+)"); // '/context/module/service'
    private static final File TEMPORARY = null;
    private static final Logger _logger = Logger.getLogger(HttpServiceDispatcher.class.getName());

    private final Map<String, Service> _services = new HashMap<String, Service>();
    private final Map<String, ParametricStatement> _storages = new HashMap<String, ParametricStatement>();
    private final org.xillium.data.validation.Dictionary _dict = new org.xillium.data.validation.Dictionary();

    // Wired in spring application context
    //private WebApplicationContext _wac;
    //private DataSourceTransactionManager _txm;
    private ExecutionEnvironment _env;

    public HttpServiceDispatcher() {
        _logger.log(Level.INFO, "START HTTP service dispatcher " + getClass().getName());
    }

    /**
     * Initializes persistence store.
     */
    public void init() throws ServletException {
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        //_txm = (DataSourceTransactionManager)wac.getBean("transactionManager");
        _env = new ExecutionEnvironment(_dict, (DataSource)wac.getBean("dataSource"), _storages);
        _dict.addTypeSet(org.xillium.data.validation.StandardDataTypes.class);
        scanServiceModules(wac);
    }

    /**
     * Dispatcher entry point
     */
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Service service;
        String id;

        _logger.log(Level.INFO, "Request URI = " + req.getRequestURI());
        Matcher m = URI_REGEX.matcher(req.getRequestURI());
        if (m.matches()) {
            id = m.group(1);
            _logger.log(Level.INFO, "Request service id = " + id);

            service = (Service)_services.get(id);
            if (service == null) {
                _logger.log(Level.WARNING, "Request not recognized");
                res.sendError(404);
                return;
            }
/*
            Map<String, String[]> pmap = req.getParameterMap();
            String[] desc;
            if (pmap.size() == 1 && (desc = pmap.get("desc")) != null && desc.length == 1 && desc[0].length() == 0) {
                res.setHeader("ContentType", "application/json");
                res.getWriter().append(_descriptions.get(id)).flush();
                return;
            }
*/
        } else {
            _logger.log(Level.WARNING, "Request not recognized");
            res.sendError(404);
            return;
        }
        _logger.log(Level.INFO, "SERVICE class = " + service.getClass().getName());

        List<File> upload = new ArrayList<File>();
        DataBinder binder = new DataBinder();

        try {
            if (ServletFileUpload.isMultipartContent(req)) {
                try {
                    FileItemIterator it = new ServletFileUpload().getItemIterator(req);
                    while (it.hasNext()) {
                        FileItemStream item = it.next();
                        String name = item.getFieldName();
                        InputStream stream = item.openStream();
                        if (item.isFormField()) {
                            binder.put(name, Streams.asString(stream));
                        } else {
                            // File field with file name in item.getName()
                            String original = item.getName();
                            int dot = original.lastIndexOf('.');
                            // store the file in a temporary place
                            File file = File.createTempFile("xillium", dot > 0 ? original.substring(dot) : null, TEMPORARY);
                            OutputStream out = new FileOutputStream(file);
                            byte[] buffer = new byte[1024*1024];
                            int length;
                            while ((length = stream.read(buffer)) >= 0) out.write(buffer, 0, length);
                            out.close();
                            binder.put(name, original);
                            binder.put(name + ":path", file.getAbsolutePath());
                            upload.add(file);
                        }
                    }
                } catch (FileUploadException x) {
                    throw new RuntimeException("Failed to parse multipart content", x);
                }
            } else {
                java.util.Enumeration<String> en = req.getParameterNames();
                while (en.hasMoreElements()) {
                    String name = en.nextElement();
                    String[] values = req.getParameterValues(name);
                    if (values.length == 1) {
                        binder.put(name, values[0]);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < values.length; ++i) {
                            sb.append('{').append(values[i]).append('}');
                        }
                        binder.put(name, sb.toString());
                    }
                }
            }

            // TODO: pre-service filter

/*
            if (service instanceof Service.NonTransactional) {
                binder = service.run(binder, _env);
            } else {
                DefaultTransactionDefinition def = new DefaultTransactionDefinition();
                def.setName('[' + id + ":TRANSACTION]");
                def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                def.setReadOnly(service instanceof Service.ReadOnly);
                TransactionStatus status = _txm.getTransaction(def);

                try {
                    binder = service.run(binder, _env);
                } catch (ServiceException x) {
                    _logger.log(Level.WARNING, "rollback");
                    _txm.rollback(status);
                    throw x;
                } catch (RuntimeException x) {
                    _logger.log(Level.WARNING, "rollback");
                    _txm.rollback(status);
                    throw x;
                } catch (Throwable t) {
                    _logger.log(Level.INFO, "commit upon unknown throwable");
                    _txm.commit(status);
                    throw t;
                }
                _logger.log(Level.INFO, "commit");
                _txm.commit(status);
            }
*/
            binder = service.run(binder, _env);

            // TODO: post-service filter

        } catch (Throwable x) {
            _logger.log(Level.WARNING, "Exception caught in dispatcher", x);
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            x.printStackTrace(pw);
//            pw.flush();
//            binder.put("_exception", sw.getBuffer().toString());
            String message = x.getMessage();
            if (message == null || message.length() == 0) {
            	message = x.getClass().getName();
            }
            binder.put("_exception", message);
        } finally {
            res.setHeader("ContentType", "application/json");
            try {
                JSONBuilder jb = new JSONBuilder(binder.estimateMaximumBytes()).append('{');
                //JSONObject result = new JSONObject();

                jb.quote("attibutes").append(":{");
                Iterator<String> it = binder.keySet().iterator();
                for (int i = 0; it.hasNext(); ++i) {
                    String key = it.next();
                    String val = binder.get(key);
                    if (val.startsWith("json:")) {
                        //result.put(key, new JSONObject(val.substring(5)));
                        jb.quote(key).append(':').append(val.substring(5));
                    } else {
                        //result.put(key, binder.get(key));
                        jb.serialize(key, val);
                    }
                    jb.append(',');
                }
                jb.replaceLast('}').append(',');

                jb.quote("tables").append(":{");
                Set<String> rsets = binder.getResultSetNames();
                //JSONObject resultsets = new JSONObject();
                it = rsets.iterator();
                while (it.hasNext()) {
                    CachedResultSet rset = binder.getResultSet(it.next());
                    jb.serialize(rset);
/*
                    JSONObject resultset = new JSONObject();
                    resultset.put("columns", new JSONArray(rset.columns));

                    JSONArray rows = new JSONArray();
                    for (Iterator<Object[]> i = rset.rows.iterator(); i.hasNext(); rows.put(new JSONArray(i.next())));
                    resultset.put("rows", rows);

                    resultsets.put(rset.name, resultset);
*/
                    jb.append(',');
                }
                //result.put("ResultSets", resultsets);
                jb.replaceLast('}');

                jb.append('}');

                //res.getWriter().append(result.toString()).flush();
                res.getWriter().append(jb.toString()).flush();
            } finally {
                for (File tmp: upload) {
                    try { tmp.delete(); } catch (Exception x) {}
                }
            }
        }
    }

    private void scanServiceModules(WebApplicationContext wac) throws ServletException {
        ServletContext context = getServletContext();

        // if intrinsic services are wanted
        Map<String, String> descriptions = new HashMap<String, String>();

        try {
            BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory(ValidationConfiguration.class, _dict);
            XMLBeanAssembler assembler = new XMLBeanAssembler(factory);
            Set<String> jars = context.getResourcePaths("/WEB-INF/lib/");
            _logger.log(Level.INFO, "There are " + jars.size() + " resource paths");
            for (String jar : jars) {
                try {
                    JarInputStream jis = new JarInputStream(context.getResourceAsStream(jar));
                    try {
                        String name = jis.getManifest().getMainAttributes().getValue(MODULE_NAME);
                        if (name != null) { // Xillium Module
                            factory.setBurnedIn(StorageConfiguration.class, _storages, name);
                            JarEntry entry;
                            while ((entry = jis.getNextJarEntry()) != null) {
                                if (SERVICE_CONFIG.equals(entry.getName())) {
                                    _logger.log(Level.INFO, "Services:" + jar + ":" + entry.getName());
                                    loadServiceModule(wac, name, getJarEntryAsStream(jis), descriptions);
                                } else if (STORAGE_CONFIG.equals(entry.getName())) {
                                    _logger.log(Level.INFO, "Storages:" + jar + ":" + entry.getName());
                                    assembler.build(getJarEntryAsStream(jis));
                                } else if (REQUEST_VOCABULARY.equals(entry.getName())) {
                                    _logger.log(Level.INFO, "RequestVocabulary:" + jar + ":" + entry.getName());
                                    assembler.build(getJarEntryAsStream(jis));
                                }
                            }
                        }
                    } finally {
                        jis.close();
                    }
                } catch (IOException x) {
                    // ignore this jar
                    _logger.log(Level.WARNING, "Error during jar inspection, ignored", x);
                }
            }
        } catch (Exception x) {
            throw new ServletException("Failed to construct an XMLBeanAssembler", x);
        }

        _services.put("x!ll!um/desc", new DescService(descriptions));
        _services.put("x!ll!um/list", new ListService(_services));
    }

    private GenericApplicationContext loadServiceModule(WebApplicationContext wac, String name, InputStream stream, Map<String, String> desc) {
        GenericApplicationContext gac = new GenericApplicationContext(wac);
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(gac);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        reader.loadBeanDefinitions(new InputStreamResource(stream));
        gac.refresh();

        for (String id: gac.getBeanNamesForType(Service.class)) {
            String fullname = name + '/' + id;

            BeanDefinition def = gac.getBeanDefinition(id);
            try {
                Class<?> request = Class.forName(def.getBeanClassName()+"$Request");
                if (DataObject.class.isAssignableFrom(request)) {
                    _logger.log(Level.INFO, "Service '" + fullname + "' request description captured");
                    desc.put(fullname, "json:" + DataObject.Util.describe((Class<? extends DataObject>)request));
                } else {
                    _logger.log(Level.WARNING, "Service '" + fullname + "' defines a Request type that is not a DataObject");
                    desc.put(fullname, "json:{}");
                }
            } catch (ClassNotFoundException x) {
                _logger.log(Level.WARNING, "Service '" + fullname + "' does not expose its request structure");
                desc.put(fullname, "json:{}");
            }

            _logger.log(Level.INFO, "Service '" + fullname + "' class=" + gac.getBean(id).getClass().getName());
            _services.put(fullname, (Service)gac.getBean(id));
        }

        return gac;
    }

    private static InputStream getJarEntryAsStream(JarInputStream jis) throws IOException {
        int length;
        byte[] buffer = new byte[1024*1024];
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        while ((length = jis.read(buffer, 0, buffer.length)) > -1) bas.write(buffer, 0, length);
        return new ByteArrayInputStream(bas.toByteArray());
    }
}
