/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon.servlet;

import java.util.Date;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import org.apache.avalon.ConfigurationException;
import org.apache.avalon.ComponentNotAccessibleException;

import org.apache.cocoon.Cocoon;
import org.apache.cocoon.Notifier;
import org.apache.cocoon.Notification;
import org.apache.cocoon.environment.http.HttpEnvironment;

import org.apache.log.Logger;
import org.apache.log.LogKit;
import org.apache.log.Priority;
import org.apache.log.Category;
import org.apache.log.output.FileOutputLogTarget;
import org.apache.log.LogTarget;

/**
 * This is the entry point for Cocoon execution as an HTTP Servlet.
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>
 *         (Apache Software Foundation, Exoffice Technologies)
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:nicolaken@supereva.it">Nicola Ken Barozzi</a> Aisa
 * @version CVS $Revision: 1.1.4.35 $ $Date: 2000-12-06 19:19:58 $
 */

public class CocoonServlet extends HttpServlet {

    private Logger log = null;

    final long second = 1000;
    final long minute = 60 * second;
    final long hour   = 60 * minute;

    private long creationTime = 0;
    private Cocoon cocoon;
    private File configFile;
    private Exception exception;
    private ServletContext context;
    private String classpath;
    private String workDir;

    /**
     * Initialize this <code>CocoonServlet</code> instance.
     */
    public void init(ServletConfig conf) throws ServletException {

        super.init(conf);

        this.context = conf.getServletContext();

        try {
            String path = this.context.getRealPath("/") +
                          "/WEB-INF/logs/cocoon.log";

            Category cocoonCategory = LogKit.createCategory("cocoon", Priority.DEBUG);
            log = LogKit.createLogger(cocoonCategory, new LogTarget[] {
                    new FileOutputLogTarget(path),
                    new ServletLogTarget(this.context, Priority.ERROR)
                });
        } catch (Exception e) {
            LogKit.log("Could not set up Cocoon Logger, will use screen instead", e);
        }

        LogKit.setGlobalPriority(Priority.DEBUG);

        /* WARNING (SM): the lines below BREAKS the Servlet API portability of
         * web applications.
         *
         * This is a hack to go around java compiler design problems that
         * do not allow applications to force their own classloader to the
         * compiler during compilation.
         *
         * We look for a specific Tomcat attribute so we are bound to Tomcat
         * this means Cocoon won't be able to compile things if the necessary
         * classes are not already present in the *SYSTEM* classpath, any other
         * container classloading will break it on other servlet containers.
         * To fix this, Javac must be redesigned and rewritten or we have to
         * write our own compiler.
         *
         * So, for now, the cocoon.war file with included libraries can work
         * only in Tomcat or in containers that simulate this context attribute
         * (I don't know if any do) or, for other servlet containers, you have
         * to extract all the libraries and place them in the system classpath
         * or the compilation of sitemaps and XSP will fail.
         * I know this sucks, but I don't have the energy to write a java
         * compiler to fix this :(
         *
         * This solution is to allow you to specify the servlet ClassPath
         * attribute so that Cocoon can use it.  If your files are in the
         * system classpath, then we are still ok.  For these popular
         * servlet containers, we will provide you with the attribute name:
         *
         * Catalina (Tomcat 4.x) = "org.apache.catalina.jsp_classpath"
         * Tomcat (3.x)          = "org.apache.tomcat.jsp_classpath"
         * Resin                 = "caucho.class.path"
         * WebSphere (3.5 sp2)   = "com.ibm.websphere.servlet.application.classpath"
         *
         * For other servlet containers, please consult your manuals or
         * put Cocoon in the System Classpath.
         */
        String servletClassPath = conf.getInitParameter("classpath-attribute");
        if (servletClassPath != null) {
            this.classpath = (String) context.getAttribute(servletClassPath);
        }

        this.workDir = ((File) this.context.getAttribute("javax.servlet.context.tempdir")).toString();

        String forceLoading = conf.getInitParameter("force-load");
        if (forceLoading != null) {
            StringTokenizer fqcnTokenizer = new StringTokenizer(forceLoading, ",", false);

            while (fqcnTokenizer.hasMoreTokens()) {
                String fqcn = fqcnTokenizer.nextToken();

                try {
                    Class.forName(fqcn);
                } catch (Exception e) {
                    log.error("Could not force-load class: " + fqcn, e);
                    this.context.log("Could not force-load  class: " + fqcn, e);
                    throw new ServletException("Could not force-load the required class: " +
                              fqcn + "\n" + e.getMessage(), e);
                }
            }
        }

        String configFileName = conf.getInitParameter("configurations");
        if (configFileName == null) {
            throw new ServletException("Servlet initialization argument 'configurations' not specified");
        } else {
            log.info("Using configuration file: " + configFileName);
            this.context.log("Using configuration file: " + configFileName);
        }

        try {
            this.configFile = new File(this.context.getResource(configFileName).getFile());
        } catch (Exception mue) {
            this.context.log("Servlet initialization argument 'configurations' not found at " + configFileName, mue);
            log.error("Servlet initialization argument 'configurations' not found at " + configFileName, mue);
            throw new ServletException("Servlet initialization argument 'configurations' not found at " + configFileName);
        }

        this.cocoon = this.create();
    }

    /**
     * Process the specified <code>HttpServletRequest</code> producing output
     * on the specified <code>HttpServletResponse</code>.
     */
    public void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        // Reload cocoon if configuration changed or we are reloading
        boolean reloaded = false;

        synchronized (this) {
            if (this.cocoon != null) {
                if (this.cocoon.modifiedSince(this.creationTime)) {
                    log.info("Configuration changed reload attempt");
                    this.context.log("Configuration changed reload attempt");
                    this.cocoon = this.create();
                    reloaded    = true;
                } else if ((req.getPathInfo() == null) && (req.getParameter(Cocoon.RELOAD_PARAM) != null)) {
                    log.info("Forced reload attempt");
                    this.context.log("Forced reload attempt");
                    this.cocoon = this.create();
                    reloaded    = true;
                }
            } else if ((req.getPathInfo() == null) && (req.getParameter(Cocoon.RELOAD_PARAM) != null)) {
                log.info("Invalid configurations reload");
                this.context.log("Invalid configurations reload");
                this.cocoon = this.create();
                reloaded    = true;
            }
        }

        // Check if cocoon was initialized
        if (this.cocoon == null) {
            res.setStatus(res.SC_INTERNAL_SERVER_ERROR);

            Notification n = new Notification(this);
            n.setType("internal-servlet-error");
            n.setTitle("Internal servlet error");
            n.setSource("Cocoon servlet");
            n.setMessage("Internal servlet error");
            n.setDescription("Cocoon was not initialized.");
            n.addExtraDescription("request-uri", req.getRequestURI());
            Notifier.notify(n, req, res);

            return;
        }

        // We got it... Process the request
        String uri = req.getServletPath();
        if (uri == null) uri = "";
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) uri += pathInfo;

        if (uri.length() == 0) {
            /* empty relative URI
                 -> HTTP-redirect from /cocoon to /cocoon/ to avoid
                    StringIndexOutOfBoundsException when calling
                    "".charAt(0)
               else process URI normally
            */
            String prefix = req.getRequestURI();

            if (prefix == null) prefix = "";

            res.sendRedirect(prefix + "/");
            return;
        }

        try {
            if (uri.charAt(0) == '/') {
                uri = uri.substring(1);
            }

            HttpEnvironment env = new HttpEnvironment(uri, req, res, context);

            if (!this.cocoon.process(env)) {

                // FIXME (NKB) It is not true that !this.cocoon.process(env)
                // means only SC_NOT_FOUND
                res.setStatus(res.SC_NOT_FOUND);

                Notification n = new Notification(this);
                n.setType("resource-not-found");
                n.setTitle("Resource not found");
                n.setSource("Cocoon servlet");
                n.setMessage("Resource not found");
                n.setDescription("The requested URI \""
                                 + req.getRequestURI()
                                 + "\" was not found.");
                n.addExtraDescription("request-uri", req.getRequestURI());
                n.addExtraDescription("path-info", uri);
                Notifier.notify(n, req, res);
            }
        } catch (Exception e) {
            //res.setStatus(res.SC_INTERNAL_SERVER_ERROR);
            Notification n = new Notification(this, e);
            n.setType("internal-server-error");
            n.setTitle("Internal server error");
            n.setSource("Cocoon servlet");
            n.addExtraDescription("request-uri", req.getRequestURI());
            n.addExtraDescription("path-info", uri);
            Notifier.notify(n, req, res);
        }

        ServletOutputStream out = res.getOutputStream();

        long end = System.currentTimeMillis();
        String timeString = processTime(end - start);
        log.debug("'" + uri + "' " + timeString);

        String showTime = req.getParameter(Cocoon.SHOWTIME_PARAM);

        if ((showTime != null) && !showTime.equalsIgnoreCase("no")) {
            boolean hide = showTime.equalsIgnoreCase("hide");
            out.print((hide) ? "<!-- " : "<p>");

            out.print(timeString);

            out.println((hide) ? " -->" : "</p>");
        }

        out.flush();
    }

    private Cocoon create() {
        try {
            log.info("Reloading from: " + this.configFile);
            this.context.log("Reloading from: " + this.configFile);
            Cocoon c = new Cocoon(this.configFile, this.classpath, this.workDir);
            this.creationTime = System.currentTimeMillis();
            return c;
        } catch (Exception e) {
            log.error("Exception reloading", e);
            this.context.log("Exception reloading: " + e.getMessage());
            this.exception = e;
            return null;
        }
    }

    private String processTime(long time) throws IOException {

        StringBuffer out = new StringBuffer("Processed by ")
                           .append(Cocoon.COMPLETE_NAME)
                           .append(" in ");

        if (time > hour) {
            out.append((float) time / (float) hour);
            out.append(" hours.");
        } else if (time > minute) {
            out.append((float) time / (float) minute);
            out.append(" minutes.");
        } else if (time > second) {
            out.append((float) time / (float) second);
            out.append(" seconds.");
        } else {
            out.append(time);
            out.append(" milliseconds.");
        }

        return out.toString();
    }
}

