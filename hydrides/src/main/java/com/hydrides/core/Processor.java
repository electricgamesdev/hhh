package com.hydrides.core;

import java.util.List;

import org.apache.avro.generic.GenericData.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hydrides.core.Constants.RECORD;

public abstract class Processor {

	Logger log = LoggerFactory.getLogger(Processor.class);

	private Constants.RECORD type = RECORD.JSON;
	private Processor parent = null;

	public void setParent(Processor parent) {
		this.parent = parent;
	}

	public Processor getParent() {
		return parent;
	}

	protected void log(String msg) {
		log.info(this.getClass().getSimpleName() + " : " + msg);
	}

	public Constants.RECORD getType() {
		return type;
	}

	public void setType(Constants.RECORD type) {
		this.type = type;
	}

	public Object execute(List<Record> record) throws Exception {
		log(" INPUT DATA  --> " + record);
		Object output = process(record);
		if (output != null) {
			log(" OUTPUT  AVAILABLE --> " + output.getClass());
		} else {
			log(" NO OUTPUT  --> " + output);
		}
		return output;
	}

	protected void l(String msg) {
		log.info(this.getClass().getSimpleName() + ":" + msg);
	}

	public abstract Object process(List<Record> record) throws Exception;

	public boolean isRunning(Object data) {
		
		return true;
	}

	private String callBackPath = null;

	public void setCallBackPath(String callBackPath) {
		this.callBackPath = callBackPath;
	}

	public String getCallBackPath() {
		return callBackPath;
	}
}
