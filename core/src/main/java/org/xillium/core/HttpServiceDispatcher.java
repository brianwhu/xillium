package org.xillium.core;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.persistence.crud.CrudConfiguration;
import org.xillium.data.xml.*;


/**
 * Platform Service Dispatcher.
 *
 * This servlet dispatches inbound HTTP calls to registered services based on request URI. A valid request URI is in the form of
 * <pre>
 *      /[context]/[module]/[service]?[params]=...
 * </pre>
 * When a request URI matches the above pattern, this servlet looks up a Service instance registered under the name 'module/service'.
 * <p/>
 */
@WebServlet(name="dispatcher", value="/x!/*", loadOnStartup=1)
public class HttpServiceDispatcher extends HttpServlet {
    private static final Pattern URI_REGEX = Pattern.compile("/[^/?]+/([^/?]+/[^/?]+)"); // '/context/module/service'
    private static final Pattern SQL_CONSTRAINT = Pattern.compile("\\([^.]+\\.([\\w-]+)\\)");
    private static final File TEMPORARY = null;
    private static final Logger _logger = Logger.getLogger(HttpServiceDispatcher.class.getName());

    // Servlet context path without the leading '/'
    private String _application;

    // Wired in spring application context
    private Persistence _persistence;

    /**
     * Initializes the servlet, loading and initializing xillium modules.
     */
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        _application = context.getContextPath();
        if (_application.charAt(0) == '/') _application = _application.substring(1);

        ApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(context);
        if (wac.containsBean("persistence")) { // persistence may not be there if persistent storage is not required
            _persistence = (Persistence)wac.getBean("persistence");
        }
    }

    /**
     * Dispatcher entry point
     */
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String id;

        if (req.getParameterValues(Service.REQUEST_TARGET_PATH) != null) {
            id = req.getParameterValues(Service.REQUEST_TARGET_PATH)[0];
        } else {
            Matcher m = URI_REGEX.matcher(req.getRequestURI());
            if (m.matches()) {
                id = m.group(1);
            } else {
                _logger.warning("Request not recognized: " + req.getRequestURI());
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        Service service = ServicePlatform.getService(id);
        if (service == null) {
            _logger.warning("Request not recognized: " + req.getRequestURI());
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

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
                String content = req.getContentType();
                if (content != null && isPostedXML(req.getMethod().toLowerCase(), content.toLowerCase())) {
                    XDBCodec.decode(binder, req.getInputStream()).close();
                    binder.put(Service.SERVICE_XML_CONTENT, Service.SERVICE_XML_CONTENT);
                }
                Enumeration<String> en = req.getParameterNames();
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

            // auto-parameters
            binder.putNamedObject(Service.REQUEST_SERVLET_REQ, req);

            binder.put(Service.REQUEST_CLIENT_ADDR, req.getRemoteAddr());
            binder.put(Service.REQUEST_CLIENT_PORT, String.valueOf(req.getRemotePort()));
            binder.put(Service.REQUEST_SERVER_PORT, String.valueOf(req.getServerPort()));
            binder.put(Service.REQUEST_HTTP_METHOD, req.getMethod());
            binder.put(Service.REQUEST_SERVER_PATH, _application);
            binder.put(Service.REQUEST_TARGET_PATH, id);
            binder.putNamedObject(Service.REQUEST_HTTP_COOKIE, req.getCookies());
            if (req.isSecure()) binder.put(Service.REQUEST_HTTP_SECURE, Service.REQUEST_HTTP_SECURE);

            if (id.startsWith("x!/")) {
                req.setAttribute("intrinsic", "intrinsic");
            } else if (id.endsWith(".html")) {
                // TODO provide a default, error reporting page template
            }

            // pre-service filter

            if (service instanceof Service.Extended) {
                try {
                    ((Service.Extended)service).filtrate(binder);
                } catch (AuthenticationRequiredException x) {
                    if (binder.get(Service.REQUEST_HTTP_STATUS) != null) {
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    } else {
                        throw x;
                    }
                } catch (AuthorizationException x) {
                    if (binder.get(Service.REQUEST_HTTP_STATUS) != null) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    } else {
                        throw x;
                    }
                }
            }

            // authorization

            if (service instanceof Service.Secured) {
                try {
                    ((Service.Secured)service).authorize(id, binder, _persistence);
                } catch (AuthenticationRequiredException x) {
                    if (binder.get(Service.REQUEST_HTTP_STATUS) != null) {
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    } else {
                        throw x;
                    }
                } catch (AuthorizationException x) {
                    if (binder.get(Service.REQUEST_HTTP_STATUS) != null) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    } else {
                        throw x;
                    }
                }
            }

            binder = service.run(binder, ServicePlatform.getDictionary(), _persistence);

            // post-service filter

            if (service instanceof Service.Extended) {
                try { ((Service.Extended)service).successful(binder); } catch (Throwable t) { _logger.log(Level.WARNING, "successful()", t); }
            }

            // post-service action (deprecated)
            try {
                Runnable task = (Runnable)binder.getNamedObject(Service.SERVICE_POST_ACTION);
                if (task != null) {
                    task.run();
                }
            } catch (Throwable t) {
                _logger.warning("In post-service processing caught " + t.getClass() + ": " + t.getMessage());
            }
        } catch (org.springframework.transaction.TransactionException x) {
            String message = Throwables.getFirstMessage(x);
            Matcher matcher = SQL_CONSTRAINT.matcher(message);
            if (matcher.find()) {
                message = CrudConfiguration.icve.get(matcher.group(1));
                if (message == null) {
                    message = matcher.group(1);
                }
            }
            serviceAborted(binder, id, service, x, message);
        } catch (Throwable x) {
            serviceAborted(binder, id, service, x, Throwables.getFirstMessage(x));
        } finally {
            // post-service filter
            if (service instanceof Service.Extended) {
                try { ((Service.Extended)service).complete(binder); } catch (Throwable t) { _logger.log(Level.WARNING, "complete()", t); }
            }

            res.setHeader("Access-Control-Allow-Headers", "origin,x-prototype-version,x-requested-with,accept");
            res.setHeader("Access-Control-Allow-Origin", "*");

            try {
                // HTTP headers
                @SuppressWarnings("unchecked")
                Map<String, String> headers = binder.getNamedObject(Service.SERVICE_HTTP_HEADER, Map.class);
                if (headers != null) {
                    try {
                        for (Map.Entry<String, String> e: headers.entrySet()) {
                            res.setHeader(e.getKey(), e.getValue());
                        }
                    } catch (Exception x) {}
                }

                // return status only?
                String status = binder.get(Service.SERVICE_HTTP_STATUS);
                if (status != null) {
                    try { res.setStatus(Integer.parseInt(status)); } catch (Exception x) {}
                } else {
                    String page = binder.get(Service.SERVICE_PAGE_TARGET);

                    if (page == null) {
                        if (binder.find(Service.SERVICE_XML_CONTENT) != null) {
                            binder.clearAutoValues();
                            res.setContentType("application/xml;charset=utf-8");
                            try {
                                XDBCodec.encode(res.getWriter(), binder).flush();
                            } catch (Exception x) {}
                        } else {
                            binder.clearAutoValues();
                            res.setContentType("application/json;charset=utf-8");
                            String json = binder.get(Service.SERVICE_JSON_TUNNEL);

                            if (json == null) {
                                json = binder.toJSON();
                            }

                            res.getWriter().append(json).flush();
                        }
                    } else {
                        _logger.fine("\t=> " + getServletContext().getResource(page));
                        req.setAttribute(Service.SERVICE_DATA_BINDER, binder);
                        getServletContext().getRequestDispatcher(page).include(req, res);
                    }
                }
            } finally {
                for (File tmp: upload) {
                    try { tmp.delete(); } catch (Exception x) {}
                }
            }
        }
    }

    private static boolean isPostedXML(String method, String content) {
        return "post".equals(method) && (content.endsWith("xml") || content.contains("xml;"));
    }

    private static void serviceAborted(DataBinder binder, String id, Service service, Throwable x, String message) {
        binder.put(Service.FAILURE_MESSAGE, message);

        // post-service exception handler
        if (service instanceof Service.Extended) {
            try { ((Service.Extended)service).aborted(binder, x); } catch (Throwable t) { _logger.log(Level.WARNING, "aborted()", t); }
        }

        String sst = binder.get(Service.SERVICE_STACK_TRACE);
        String pst = System.getProperty("xillium.service.PrintStackTrace");
        if (sst != null || pst != null) {
            CharArrayWriter sw = new CharArrayWriter();
            x.printStackTrace(new PrintWriter(sw));
            String stack = sw.toString();
            if (sst != null) binder.put(Service.FAILURE_STACK, stack);
            if (pst != null) _logger.warning(x.getClass().getSimpleName() + " caught in (" + id + "): " + message + '\n' + stack);
        }
    }

    private static final long serialVersionUID = 1L;
}
