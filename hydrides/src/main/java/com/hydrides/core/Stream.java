package com.hydrides.core;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "stream")
public class Stream {

	@XmlTransient
	private String wave = null;
	
	@XmlTransient
	private Component component = null;
	
	
	@XmlAttribute
	private String on = null;

	@XmlElement(name = "input")
	private Input input = null;

	@XmlElement(name = "output")
	private Output output = null;

	public Component getComponent() {
		return component;
	}
	
	public void setComponent(Component component) {
		this.component = component;
	}
	
	public String getWave() {
		return wave;
	}
	
	public void setWave(String wave) {
		this.wave = wave;
	}
	
	public String getOn() {
		return on;
	}

	public void setOn(String on) {
		this.on = on;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	public Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}

	@Override
	public String toString() {
		return "Wave [on=" + on + ", input=" + input + ", output=" + output + "]";
	}

}
