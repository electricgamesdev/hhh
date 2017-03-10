package com.hydrides.core;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentManager;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "container")
public class Container {

	@XmlAttribute
	private String title;

	@XmlAttribute
	private String type;
	@XmlAttribute(name = "path")
	private String path = null;

	@XmlAttribute(name = "as")
	private String as = null;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setAs(String as) {
		this.as = as;
	}

	public String getAs() {
		return as;
	}

	@XmlElement(name = "component")
	private List<Component> component = null;

	public List<Component> getComponent() {
		return component;
	}

	public void setComponent(List<Component> component) {
		this.component = component;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
