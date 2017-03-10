package com.hydrides.core;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "input")
public class Input {

	@XmlAttribute
	private String stream = null;
	
	@XmlAttribute
	private String wave = null;

	@XmlAttribute
	private String type = null;

	@XmlElement(name = "field")
	private List<Field> fields = null;
	
	public String getWave() {
		return wave;
	}
	
	public void setWave(String wave) {
		this.wave = wave;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

	@Override
	public String toString() {
		return "Input [stream=" + stream + ", fields=" + fields + "]";
	}

}
