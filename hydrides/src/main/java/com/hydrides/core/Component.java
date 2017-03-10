package com.hydrides.core;

import java.io.File;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "component")

public class Component {

	@XmlAttribute
	private String title = null;

	@XmlAttribute
	private String on = null;

	@XmlAttribute
	private String wave = null;

	@XmlElement(name = "interface")
	private List<Interface> interfaces = null;

	private File file;
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

	public String getOn() {
		return on;
	}

	public void setOn(String on) {
		this.on = on;
	}

	public String getWave() {
		return wave;
	}

	public void setWave(String waves) {
		this.wave = waves;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public List<Interface> getInterface() {
		return interfaces;
	}

	public void setInterface(List<Interface> interfaces) {
		this.interfaces = interfaces;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return this.file;
	}

}
