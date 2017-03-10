package com.hydrides.core;

import java.nio.file.Path;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentManager;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "domain")
public class Domain {

	@XmlTransient
	private String status="STARTED";
	
	@XmlAttribute
	private int port;

	@XmlAttribute
	private String title;

	@XmlAttribute
	private String namespace;
	
	@XmlAttribute
	private boolean secure=false;
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public boolean isSecure() {
		return secure;
	}
	
	public String getNamespace() {
		return namespace;
	}
	
	@XmlElement(name = "container")
	private List<Container> container = null;

	

	@XmlTransient
	private Undertow server;
	
	@XmlTransient
	private Path file;

	@XmlTransient
	private DeploymentManager manager;

	public Path getFile() {
		return file;
	}
	
	public void setFile(Path file) {
		this.file = file;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Undertow getServer() {
		return server;
	}

	public void setServer(Undertow server) {
		this.server = server;
	}

	public void setDeploymentManager(DeploymentManager manager) {
		this.manager = manager;
	}

	public DeploymentManager getDeploymentManager() {
		return manager;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int path) {
		this.port = port;
	}

	public List<Container> getContainer() {
		return container;
	}
	
	public void setContainer(List<Container> container) {
		this.container = container;
	}

	@Override
	public String toString() {
		return "Domain [port=" + port + ", title=" + title + ", container=" + container + ", server=" + server
				+ ", manager=" + manager + "]";
	}
	
	
}
