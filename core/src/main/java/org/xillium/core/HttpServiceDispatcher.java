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
import org.xillium.base.util.Multimap;
import org.xillium.base.util.Pair;
import org.xillium.data.*;
import org.xillium.data.persistence.crud.CrudConfiguration;
import org.xillium.data.xml.*;
import org.xillium.core.management.ManagedPlatform;


/**
 * Platform Service Dispatcher.
 *
 * This servlet dispatches inbound HTTP calls to registered services based on request URI. A valid request URI is in the form of
 * <pre>
 *      /[context]/[module]/[service]?[params]=...
 * </pre>
 * When a request URI matches the above pattern, this servlet looks up a Service instance registered under the name 'module/service'.
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
    //private Persistence _persistence;

    /**
     * Initializes the servlet
     */
    public void init() throws ServletException {
        _application = ((ManagedPlatform)ServicePlatform.getService(ManagedPlatform.INSTANCE).first).getName();

/*
        ApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (wac.containsBean("persistence")) { // persistence may not be there if persistent storage is not required
            _persistence = (Persistence)wac.getBean("persistence");
        }
*/
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

        Pair<Service, Persistence> service = ServicePlatform.getService(id);
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
                String method = req.getMethod().toLowerCase();
                if (service.first instanceof DataBinder.WithDecoder && "post".equals(method)) {
                    ((DataBinder.WithDecoder)service.first).getDataBinderDecoder().decode(binder, req.getInputStream()).close();
                } else {
                String content = req.getContentType();
                if (content != null && isPostedXML(method, content.toLowerCase())) {
                    XDBCodec.decode(binder, req.getInputStream()).close();
                    binder.put(Service.SERVICE_XML_CONTENT, Service.SERVICE_XML_CONTENT);
                }
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

            // pre-service filtration

            if (service.first instanceof Service.Extended) {
                try {
                    ((Service.Extended)service.first).filtrate(binder);
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

            // locate persistence object

            // authorization

            if (service.first instanceof Service.Secured) {
                try {
                    ((Service.Secured)service.first).authorize(id, binder, service.second);
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

            // acknowledgement

            if (service.first instanceof Service.Extended) {
                try { ((Service.Extended)service.first).acknowledge(binder); } catch (Throwable t) {}
            }

            binder = service.first.run(binder, ServicePlatform.getReifier(), service.second);

            // post-service filter

            if (service.first instanceof Service.Extended) {
                try { ((Service.Extended)service.first).successful(binder); } catch (Throwable t) {}
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
        } catch (Throwable x) {
            // if a new binder can't be returned from Service.run, it can be placed in the original binder as a named object
            Object replacement = binder.getNamedObject(Service.SERVICE_DATA_BINDER);
            if (replacement != null && replacement instanceof DataBinder) {
                binder = (DataBinder)replacement;
            }

            String message = Throwables.getFirstMessage(x);
            if (x instanceof org.springframework.transaction.TransactionException) {
                Matcher matcher = SQL_CONSTRAINT.matcher(message);
                if (matcher.find()) {
                    message = CrudConfiguration.icve.get(matcher.group(1));
                    if (message == null) {
                        message = matcher.group(1);
                    }
                }
            }

            binder.put(Service.FAILURE_MESSAGE, message);

            // post-service exception handler
            if (service.first instanceof Service.Extended) {
                try { ((Service.Extended)service.first).aborted(binder, x); } catch (Throwable t) {}
            }

            boolean sst = !(binder.get(Service.SERVICE_STACK_TRACE) == null);
            boolean pst = !(System.getProperty("xillium.service.PrintStackTrace") == null || id.startsWith("x!/"));
            if (sst || pst) {
                CharArrayWriter sw = new CharArrayWriter();
                x.printStackTrace(new PrintWriter(sw));
                String stack = sw.toString();
                if (sst) binder.put(Service.FAILURE_STACK, stack);
                if (pst) _logger.warning(x.getClass().getSimpleName() + " caught in (" + id + "): " + message + '\n' + stack);
            }
        } finally {
            // post-service filter
            if (service.first instanceof Service.Extended) {
                try { ((Service.Extended)service.first).complete(binder); } catch (Throwable t) {}
            }

            res.setHeader("Access-Control-Allow-Headers", "origin,x-prototype-version,x-requested-with,accept");
            res.setHeader("Access-Control-Allow-Origin", "*");

            try {
                // HTTP headers
                @SuppressWarnings("unchecked")
                Multimap<String, String> headers = binder.getNamedObject(Service.SERVICE_HTTP_HEADER, Multimap.class);
                if (headers != null) {
                    try {
                        for (Map.Entry<String, List<String>> e: headers.entrySet()) {
                            for (String value: e.getValue()) res.setHeader(e.getKey(), value);
                        }
                    } catch (Exception x) {}
                }

                String status;

                // return status only?
                if ((status = binder.get(Service.SERVICE_DO_REDIRECT)) != null) {
                    try { res.sendRedirect(status); } catch (Exception x) { _logger.log(Level.WARNING, x.getMessage(), x); }
                } else if ((status = binder.get(Service.SERVICE_HTTP_STATUS)) != null) {
                    try { res.setStatus(Integer.parseInt(status)); } catch (Exception x) { _logger.log(Level.WARNING, x.getMessage(), x); }
                } else {
                    String page = binder.get(Service.SERVICE_PAGE_TARGET);

                    if (page == null) {
                        if (service.first instanceof DataBinder.WithEncoder) {
                            DataBinder.Encoder encoder = ((DataBinder.WithEncoder)service.first).getDataBinderEncoder();
                            binder.clearAutoValues();
                            res.setContentType(encoder.getContentType(binder));
                            try {
                                encoder.encode(res.getOutputStream(), binder).flush();
                            } catch (Exception x) {
                                // bugs in the encoder?
                                _logger.log(Level.FINE, x.getMessage(), x);
                            }
                        } else
                        if (binder.find(Service.SERVICE_XML_CONTENT) != null) {
                            binder.clearAutoValues();
                            res.setContentType("application/xml;charset=utf-8");
                            try {
                                XDBCodec.encode(res.getWriter(), binder).flush();
                            } catch (Exception x) {
                                // bugs in the codec?
                                _logger.log(Level.FINE, x.getMessage(), x);
                            }
                        } else {
                            String callback = binder.get(Service.REQUEST_JS_CALLBACK);

                            binder.clearAutoValues();
                            if (callback != null) {
                                res.setContentType("application/javascript;charset=utf-8");
                            } else if (id.endsWith(".html")) {
                                res.setContentType("text/html;charset=utf-8");
                            } else if (id.endsWith(".text")) {
                                res.setContentType("text/plain;charset=utf-8");
                            } else {
                                res.setContentType("application/json;charset=utf-8");
                            }
                            String json = binder.get(Service.SERVICE_JSON_TUNNEL);

                            if (json == null) {
                                json = binder.toJSON();
                            }

                            if (callback != null) {
                                res.getWriter().append(callback).append('(').append(json).append(");").flush();
                            } else {
                                res.getWriter().append(json).flush();
                            }
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

    private static final long serialVersionUID = 1L;
}
