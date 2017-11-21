package net.arnulfo.waitfilter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 * @author Franck Arnulfo
 */
public class WaitFilter implements Filter {

  // The filter configuration object we are associated with.  If
  // this value is null, this filter instance is not currently
  // configured. 
  private FilterConfig filterConfig = null;
  private WaitTime waitTime;

  public WaitFilter() {
  }

  /**
   *
   * @param request The servlet request we are processing
   * @param response The servlet response we are creating
   * @param chain The filter chain we are processing
   *
   * @exception IOException if an input/output error occurs
   * @exception ServletException if a servlet error occurs
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
    FilterChain chain)
    throws IOException, ServletException {

    log("WaitFilter:doFilter()");

    Throwable problem = null;
    try {
      chain.doFilter(request, response);
      Thread.sleep(waitTime.getWaitTime());
    } catch (IOException | InterruptedException | ServletException t) {
      // If an exception is thrown somewhere down the filter chain,
      // we still want to execute our after processing, and then
      // rethrow the problem after that.
      problem = t;
      t.printStackTrace();
    }

    // If there was a problem, we want to rethrow it if it is
    // a known type, otherwise log it.
    if (problem != null) {
      if (problem instanceof ServletException) {
        throw (ServletException) problem;
      }
      if (problem instanceof IOException) {
        throw (IOException) problem;
      }
      sendProcessingError(problem, response);
    }
  }

  /**
   * Return the filter configuration object for this filter.
   */
  public FilterConfig getFilterConfig() {
    return (this.filterConfig);
  }

  /**
   * Set the filter configuration object for this filter.
   *
   * @param filterConfig The filter configuration object
   */
  public void setFilterConfig(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
  }

  /**
   * Destroy method for this filter
   */
  @Override
  public void destroy() {
  }

  /**
   * Init method for this filter
   *
   * @param filterConfig
   */
  @Override
  public void init(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
    if (filterConfig != null) {
      log("WaitFilter:Initializing filter");
      try {
        long l = Long.parseLong(filterConfig.getInitParameter("WaitTime"));
        waitTime = new WaitTime(l);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("org.arnulfo.waitfilter:type=WaitTime");
        mbs.registerMBean(waitTime, name);
      } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException ex) {
        Logger.getLogger(WaitFilter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Return a String representation of this object.
   */
  @Override
  public String toString() {
    if (filterConfig == null) {
      return ("WaitFilter()");
    }
    StringBuilder sb = new StringBuilder("WaitFilter(");
    sb.append(filterConfig);
    sb.append(")");
    return (sb.toString());
  }

  private void sendProcessingError(Throwable t, ServletResponse response) {
    String stackTrace = getStackTrace(t);

    if (stackTrace != null && !stackTrace.equals("")) {
      try {
        response.setContentType("text/html");
        try (PrintStream ps = new PrintStream(response.getOutputStream()); PrintWriter pw = new PrintWriter(ps)) {
          pw.print("<html>\n<head>\n<title>Error</title>\n</head>\n<body>\n"); //NOI18N

          // PENDING! Localize this for next official release
          pw.print("<h1>The resource did not process correctly</h1>\n<pre>\n");
          pw.print(stackTrace);
          pw.print("</pre></body>\n</html>"); //NOI18N
        }
        response.getOutputStream().close();
      } catch (IOException ex) {
      }
    } else {
      try {
        try (PrintStream ps = new PrintStream(response.getOutputStream())) {
          t.printStackTrace(ps);
        }
        response.getOutputStream().close();
      } catch (IOException ex) {
      }
    }
  }

  public static String getStackTrace(Throwable t) {
    String stackTrace = null;
    try {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      pw.close();
      sw.close();
      stackTrace = sw.getBuffer().toString();
    } catch (IOException ex) {
    }
    return stackTrace;
  }

  public void log(String msg) {
    filterConfig.getServletContext().log(msg);
  }

}
