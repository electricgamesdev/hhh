package com.hydrides.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.avro.generic.GenericData.Record;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.hydrides.core.Constants.RECORD;
import com.hydrides.engine.HTMLEngine;
import com.hydrides.engine.HTTPEngine;
import com.hydrides.engine.JBPMEngine;
import com.hydrides.engine.JPAEngine;
import com.hydrides.engine.JQueryEngine;
import com.hydrides.engine.JavaEngine;
import com.hydrides.parser.Streamer;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement
public class HydridesServlet extends HttpServlet implements Hydrides {

	private static final long serialVersionUID = 1L;
	private HydridesContext context = null;
	private Domain domain = null;
	private String contextPath = null;
	private Logger log = LoggerFactory.getLogger(HydridesServlet.class);
	private Map<String, Engine> engines = new HashMap<String, Engine>();
	private String jta = "java:comp/UserTransaction";
	private Map<String, Domain> servers = null;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		contextPath = config.getServletContext().getContextPath();
		l("Initializing ... ");

		try {
			this.servers = (Map<String, Domain>) getServletContext().getAttribute("domains");
			String bapp = config.getInitParameter(Constants.RESOURCE.DOMAINS.name());
			String rootpath = config.getInitParameter(Constants.RESOURCE.ROOT.name());
			jta = config.getInitParameter(Constants.RESOURCE.JTA.name());
			if (bapp == null) {
				log.error("Servlet Init Param 'BLUEPRINT' for HydridesServlet missing in web.xml");
				return;
			}

			l("File Name : " + bapp);
			l("Root Directory : " + rootpath);

			context = new HydridesContext(Paths.get(bapp), Paths.get(rootpath));

			domain = context.getDomain();
			// context.setNamespace(contextPath + "/" + blueprint.getPath());
			context.setNamespace(contextPath);
			initContainers(domain.getContainer(), engines);

			getServletContext().setAttribute("prototype", engines);
			getServletContext().setAttribute("hydrides", this);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void initContainers(List<Container> containers, Map<String, Engine> engines) {
		if (containers != null) {
			for (Container container : containers) {
				Engine b = getEngine(container.getType());
				if (b != null) {

					b.setContainer(container);
					b.setContext(context);
					b.setHydrides(this);

					if (b.build()) {
						engines.put(container.getPath(), b);
					}
				} else {
					log.error("********* Error : container not found for : " + container.getType());
				}
			}
		}
	}

	private Engine getEngine(String type) {
		if ("jpa".equalsIgnoreCase(type)) {
			return new JPAEngine();
		} else if ("html".equalsIgnoreCase(type)) {
			return new HTMLEngine();
			// return new JQueryEngine();
		} else if ("http".equalsIgnoreCase(type)) {
			return new HTTPEngine();
		} else if ("jbpm".equalsIgnoreCase(type)) {
			return new JBPMEngine();
		} else if ("java".equalsIgnoreCase(type)) {
			return new JavaEngine();
		}
		return null;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String s = req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
		OutputStream out = resp.getOutputStream();
		log.info("Servlet : SERVER : " + s);
		log.info("Servlet : Path Info : " + req.getPathInfo() + " Request URI :" + req.getRequestURI());

		Enumeration<String> en = req.getHeaderNames();
		while (en.hasMoreElements()) {
			String n = en.nextElement();
			// log.info(n + "=" + req.getHeader(n));
		}
		// if (domain.isSecure()) {
		// req.authenticate(resp);
		// }

		String path = null;

		String referer = req.getHeader("referer");

		req.getSession().setAttribute("userid", "safik");

		if (req.getPathInfo() != null) {
			path = req.getPathInfo().substring(1);
		}

		log.info("Servlet : PATH = " + path);

		try {

			Engine b = engines.get(path);
			if (b != null) {

				InputStream result = null;
				UserTransaction userTransaction = null;
				String mtype = "application/json;charset=UTF-8";
				Context ctx = new InitialContext();
				userTransaction = (UserTransaction) ctx.lookup(jta);

				if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
					userTransaction.begin();

				}
				try {

					Object obj = b.stream(path, req);

					if (obj != null) {
						if (obj instanceof Streamer) {
							result = (InputStream) obj;
							mtype = getMimeType(((Streamer) obj).getType());
							log.info("Servlet : STREAM = Streamer : " + mtype);
						} else if (obj instanceof InputStream) {
							result = (InputStream) obj;
							mtype = getMimeType(b.getContentType(path, req));
							log.info("Servlet : STREAM = InputStream : " + mtype);
						} else {
							result = error("Non Streamable Result for PATH : " + path);
							mtype = getMimeType(RECORD.JSON);
						}
					} else {
						result = error("No Data Found for PATH : " + path);
						mtype = getMimeType(RECORD.JSON);
					}
				} catch (RedirectException e) {
					if (e.getDomain() != null) {
						Domain domain = e.getDomain();
						String url = "http://" + req.getServerName() + ":" + domain.getPort() + "/" + e.getTo();
						l("Redirecting to " + url);
						HttpSession session = req.getSession(true);
						session.setAttribute("data", e.getData());
						result = redirect(url);
					}
				} catch (Exception e) {
					if (userTransaction != null && userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION)
						userTransaction.rollback();
					result = error(e.getMessage());
					e.printStackTrace();
				}

				log("......Transmitting DATA .... " + mtype);

				resp.setContentType(mtype);
				IOUtils.copy(result, out);
				IOUtils.closeQuietly(result);
				IOUtils.closeQuietly(out);
				if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
					userTransaction.commit();

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public InputStream redirect(String url) {
		StringWriter writer = new StringWriter();
		Gson gson = new GsonBuilder().create();
		try {
			JsonWriter jw = gson.newJsonWriter(writer);
			jw.beginObject().name("redirect").value(url).endObject();
		} catch (IOException e) {

		}

		return new StringBufferInputStream(writer.toString());
	}

	public InputStream error(String msg) {
		StringWriter writer = new StringWriter();
		Gson gson = new GsonBuilder().create();
		try {
			JsonWriter jw = gson.newJsonWriter(writer);
			jw.beginObject().name("message").value(msg).endObject();
		} catch (IOException e) {

		}

		return new StringBufferInputStream(writer.toString());
	}

	private void l(String msg) {
		log.info(contextPath + " : " + msg);
	}

	private String getMimeType(RECORD type) {
		log.info("Servlet : STREAM type: " + type);
		if (RECORD.HTML.equals(type))
			return "text/html";
		else if (RECORD.JSON.equals(type))
			return "application/json;charset=UTF-8";
		return "application/json";
	}

	@Override
	public Object stream(String path, Object r) throws Exception {
		log("Searching path : " + path);
		if (this.engines.containsKey(path)) {
			log("Found path in process repository : " + path);
			return engines.get(path).stream(path, r);
		} else if (this.servers != null) {
			for (String key : servers.keySet()) {
				if (path.startsWith(key)) {
					Domain d = servers.get(key);
					throw new RedirectException(d, r, path);
				}
			}
		}

		log("Searching path : FAILED, path not found: " + path);

		throw new RedirectException(null, r, path);

	}

	public void registerPath(String path, Engine e) {
		log.debug("Registered Path : " + path);
		this.engines.put(path, e);

	}

	private void oxygen(String path, HttpServletRequest req, HttpServletResponse resp) {
		log.debug("oxygen path : " + path);
		OutputStream out = null;
		InputStream in = null;

		try {

			StringWriter writer = new StringWriter();

			JAXBContext jc = JAXBContext.newInstance(Hydrides.class);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.marshal(this, writer);
			System.out.println("Writer " + writer.toString());
			// if (in != null) {
			// out = resp.getOutputStream();
			// resp.setContentType("text/html");
			// IOUtils.copy(in, out);
			// } else {
			// log.error("Data Streamer not found ");
			//
			// }

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}

	}

}
