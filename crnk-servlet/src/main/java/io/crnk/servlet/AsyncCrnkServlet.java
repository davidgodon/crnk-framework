package io.crnk.servlet;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.crnk.core.boot.CrnkBoot;
import io.crnk.core.engine.dispatcher.RequestDispatcher;
import io.crnk.core.engine.http.HttpRequestContextProvider;
import io.crnk.core.engine.internal.utils.PreconditionUtil;
import io.crnk.servlet.internal.ServletModule;
import io.crnk.servlet.internal.ServletPropertiesProvider;
import io.crnk.servlet.internal.ServletRequestContext;

/**
 * Async/reactive servlet filter class to integrate with Crnk.
 * <p>
 * <p>
 * Child class can override {@link #initCrnk(CrnkBoot)} method and make use of CrnkBookt for further customizations.
 * </p>
 */
public class AsyncCrnkServlet extends HttpServlet {

	protected CrnkBoot boot = new CrnkBoot();

	private int timeout = 30000;

	public AsyncCrnkServlet() {

	}

	@Override
	public void init() throws ServletException {
		HttpRequestContextProvider provider = boot.getModuleRegistry().getHttpRequestContextProvider();
		boot.setPropertiesProvider(new ServletPropertiesProvider(getServletConfig()));
		boot.addModule(new ServletModule(provider));
		initCrnk(boot);
		boot.boot();
	}

	public CrnkBoot getBoot() {
		return boot;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	protected void initCrnk(CrnkBoot boot) {
		// nothing to do here
	}

	@Override
	public void destroy() {
		// nothing to do
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws IOException, ServletException {
		PreconditionUtil
				.assertTrue("only http supported, ", req instanceof HttpServletRequest && res instanceof HttpServletResponse);
		final AsyncContext ctx = req.startAsync();
		ctx.setTimeout(timeout);

		ServletContext servletContext = getServletContext();
		ServletRequestContext context = new ServletRequestContext(servletContext, (HttpServletRequest) req,
				(HttpServletResponse) res, boot.getWebPathPrefix());
		RequestDispatcher requestDispatcher = boot.getRequestDispatcher();

		// FIXME		requestDispatcher.processAsync(context);

		// attach listener to respond to lifecycle events of this AsyncContext
		ctx.addListener(new AsyncListener() {
			public void onComplete(AsyncEvent event) throws IOException {
				log("onComplete called");
			}

			public void onTimeout(AsyncEvent event) throws IOException {
				log("onTimeout called");
			}

			public void onError(AsyncEvent event) throws IOException {
				log("onError called");
			}

			public void onStartAsync(AsyncEvent event) throws IOException {
				log("onStartAsync called");
			}
		});
	}

}
