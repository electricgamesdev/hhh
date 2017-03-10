package com.hydrides.core;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "macro")
public class Macro {

	@XmlAttribute
	private String type = null;

	@XmlAttribute
	private String on = null;

	@XmlAttribute
	private String to = null;

	@XmlAttribute(name = "path")
	private String path = null;

	@XmlAttribute(name = "as")
	private String as = null;
	
	@XmlElement(name = "interface")
	private List<Interface> interfaces = null;
	
	public List<Interface> getInterfaces() {
		return interfaces;
	}

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getOn() {
		return on;
	}

	public void setOn(String on) {
		this.on = on;
	}

	@Override
	public String toString() {
		return "Macro [type=" + type + ", on=" + on + ", to=" + to + "]";
	}

}
