package com.hydrides.core;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.DeploymentManager.State;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement
public class HydrogenServlet extends HttpServlet {

	private Logger log = LoggerFactory.getLogger(HydrogenServlet.class);
	private Map<String, Domain> servers = new HashMap<>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		StringWriter writer = new StringWriter();
		Gson gson = new GsonBuilder().create();
		JsonWriter jw = gson.newJsonWriter(writer);
		jw.beginArray();
		if (servers.size() > 0) {
			jw.beginArray();
			process(jw, servers);
			jw.endArray();
		}

		jw.endArray();

		resp.setContentType("application/json");

		resp.getWriter().println(writer.toString());

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			String path = req.getPathInfo();
			Map<String, String> map = new HashMap<String, String>();

			System.out.println("Matched, " + req.getParameterMap());

			String action = req.getParameter("action");
			String domain = req.getParameter("domain");

			log.info("POST : domain = " + domain + " action=" + action);

			if (domain != null) {
				Domain d = servers.get(domain);
				if (d != null) {
					if ("stop".equalsIgnoreCase(action)) {
						log.info("Stopping .... " + domain);
						if (!"stopped".equalsIgnoreCase(d.getStatus())) {
							servers.get(domain).getServer().stop();
							servers.get(domain).setStatus("STOPPED");
						} else {
							log.info("Already stopped : " + domain);
						}
					} else if ("start".equalsIgnoreCase(action)) {
						log.info("Starting .... " + domain);
						if (!"running".equalsIgnoreCase(d.getStatus())) {
							servers.get(domain).getServer().start();
							servers.get(domain).setStatus("RUNNING");
						} else {
							log.info("Already started : " + domain);
						}
					} else if ("restart".equalsIgnoreCase(action)) {
						log.info("Restarting .... " + domain);
						if (!"running".equalsIgnoreCase(d.getStatus())) {
							servers.get(domain).getServer().start();
							servers.get(domain).setStatus("RUNNING");
						} else {
							servers.get(domain).getServer().stop();
							servers.get(domain).setStatus("STOPPED");
							servers.get(domain).getServer().start();
							servers.get(domain).setStatus("RUNNING");
						}
					}
				}
			}

		} catch (

		Exception e) {
			e.printStackTrace();
		}
		// doGet(req, resp);

	}

	private void process(JsonWriter jw, Map<String, Domain> prototype) throws IOException {
		for (String key : prototype.keySet()) {
			Domain domain = prototype.get(key);
			jw.beginObject().name("domain").value(key);
			jw.name("title").value(domain.getTitle());
			jw.name("port").value(domain.getPort());
			jw.name("state").value(domain.getStatus());
			jw.name("size").value(domain.getContainer().size());
			jw.name("traffic").value(100000);
			jw.endObject();
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		log.info("Starting Hydrogen.... " + config.getServletContext().getContextPath());

		try {

			String bp = config.getInitParameter(Constants.RESOURCE.DOMAINS.name());
			if (bp == null) {
				log.error("Servlet Init Param 'BLUEPRINT' for HydridesServlet missing in web.xml");
				return;
			}

			String rs = config.getInitParameter(Constants.RESOURCE.CONFIG.name());
			if (rs == null) {
				log.error("Servlet Init Param 'CONFIG' for HydridesServlet missing in web.xml");
				return;
			}

			String jta = config.getInitParameter(Constants.RESOURCE.JTA.name());

			final Path rootPath = Paths.get(config.getServletContext().getRealPath("/"));
			log.info("Root Directory : " + rootPath.toFile());

			final String htmlPath = "html";

			Path bpPath = Paths.get(config.getServletContext().getRealPath(bp));
			log.info("Domain Directory : " + bpPath.toFile());

			Path rsPath = Paths.get(config.getServletContext().getRealPath(rs));
			log.info("Config Directory : " + rsPath.toFile());

			List<Path> fileNames = new ArrayList<>();
			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(bpPath)) {
				for (Path path : directoryStream) {
					if (path.toString().endsWith(".domain.xml")) {
						log.info("Adding Domain : " + path.toFile());
						fileNames.add(path);
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			for (Path bprint : fileNames) {
				String name = bprint.getName(bprint.getNameCount() - 1).toString();
				HydridesContext hydides = new HydridesContext(bprint, rootPath);
				Domain domain = hydides.getDomain();
				domain.setFile(bprint);
				servers.put(domain.getNamespace(), domain);
			}

			getServletContext().setAttribute("domains", servers);

			for (String name : servers.keySet()) {
				Domain domain = servers.get(name);
				final String contextPath = "/" + domain.getNamespace();
				log.info("--->    Domain : " + name + " Context Path : " + contextPath + " @"
						+ InetAddress.getLocalHost().getHostName() + ":" + domain.getPort());
				DeploymentInfo servletBuilder = Servlets.deployment()
						.setClassLoader(HydrogenServlet.class.getClassLoader()).setContextPath(contextPath)
						.setDeploymentName(name + ".war").addServletContextAttribute("domains", servers)
						.addServlets(Servlets.servlet(name, HydridesServlet.class).setLoadOnStartup(1)
								.addInitParam(Constants.RESOURCE.DOMAINS.name(), domain.getFile().toString())
								.addInitParam(Constants.RESOURCE.JTA.name(), jta)
								.addInitParam(Constants.RESOURCE.ROOT.name(), rootPath.toString())// .addMapping("*.html")
								.addMapping("/*"));

				DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
				State state = manager.getState();
				domain.setDeploymentManager(manager);
				manager.deploy();
				HttpHandler handler = manager.start();

				PathHandler path = Handlers.path().addPrefixPath(contextPath, handler);

				FileResourceManager resourceManager = new FileResourceManager(rootPath.toFile(), 100) {

					@Override
					
					public Resource getResource(String f) {
						System.out.println("....###....." + f);
						int i = f.lastIndexOf(htmlPath);
						String lib=null;
						if (i > 0) {
							lib = f.substring(i);
						}
						
						System.out.println("....###....." + lib);
						if(lib==null)
							return super.getResource(f);
						return super.getResource(lib);
					}
				};

				ResourceHandler rhandler = new ResourceHandler(resourceManager) {
					@Override
					public void handleRequest(HttpServerExchange exchange) throws Exception {
						super.handleRequest(exchange);
						System.out.println("........." + exchange.getRelativePath());

					}
				};
				rhandler.setDirectoryListingEnabled(true);
				PredicateHandler predicateHandler = new PredicateHandler(
						Predicates.and(Predicates.not(Predicates.suffixes(".htm", ".html", ".jsp")),
								Predicates.contains(ExchangeAttributes.relativePath(), ".")),
						rhandler, path);
				Undertow server = Undertow.builder().addHttpListener(domain.getPort(), "localhost")
						.setHandler(predicateHandler).build();

				domain.setServer(server);

				server.start();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void destroy() {

		for (String blueprint : servers.keySet()) {
			log.info("Stopping ... " + blueprint);
			Domain domain = servers.get(blueprint);
			domain.getServer().stop();
		}
	}

}
