package com.hydrides.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hydrides.core.Constants.RESOURCE;

public class HydridesContext {

	private Logger log = LoggerFactory.getLogger(HydridesServlet.class);
	private Domain domain = null;
	private Path rootPath = null;
	private String namespace = null;
	private Map<String, Wave> waves = new HashMap<>();

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public HydridesContext(Path path, Path rootPath) {

		try {
			log.info("Loading domain : " + path);
			JAXBContext jc = JAXBContext.newInstance(Domain.class);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			domain = (Domain) unmarshaller.unmarshal(path.toFile());
			this.rootPath = rootPath;
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Domain getDomain() {
		return domain;
	}

	public Path getRootPath() {
		return rootPath;
	}

	public Component getComponent(String path) {
		try {
			Path file = Paths.get(this.rootPath.toString(), path + ".component.xml");
			log.info("Loading component : " + path);
			JAXBContext jc = JAXBContext.newInstance(Component.class);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			Component comp = (Component) unmarshaller.unmarshal(file.toFile());
			if (comp.getPath() != null) {
				comp.setFile(Paths.get(this.rootPath.toString(), comp.getPath()).toFile());
			}
			return comp;
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Wave getWaves(String path) {
		try {
			if (waves.containsKey(path)) {
				return waves.get(path);
			}
			Path file = Paths.get(this.rootPath.toString(), path + ".wave.xml");
			log.info("Loading waves : " + file);
			JAXBContext jc = JAXBContext.newInstance(Wave.class);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			Wave w = (Wave) unmarshaller.unmarshal(file.toFile());
			waves.put(path, w);
			return w;
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws MalformedURLException, URISyntaxException {
		HydridesContext context = new HydridesContext(
				Paths.get("C:/workspace/oxygenweb/WebContent/blueprints/dashboard.blueprint.xml"),
				Paths.get("C:/workspace/oxygenweb/WebContent"));
		Domain blueprint = context.getDomain();
		blueprint.getContainer().forEach(domain -> {
			Component domain1 = context.getComponent(domain.getPath());
			System.out.println(domain1);

		});

		System.out.println(blueprint);

	}

	public String getContent(String p) {
		log.info("Loading Contents : " + p);
		File file = Paths.get(this.rootPath.toString(), p).toFile();

		try {
			return IOUtils.toString(new FileInputStream(file));
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public File getFile(String p) {
		File file = Paths.get(this.rootPath.toString(), p).toFile();
		return file;
	}

	public Object getResource(RESOURCE bootstrap) {
		return resourceMap.get(bootstrap);
	}

	Map<RESOURCE, Object> resourceMap = new HashMap<>();

	public void addResource(RESOURCE config, Object v) {
		resourceMap.put(config, v);
	}

}
