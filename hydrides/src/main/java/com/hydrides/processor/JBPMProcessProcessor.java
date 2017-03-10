package com.hydrides.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.generic.GenericData.Record;
import org.kie.api.runtime.process.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JBPMProcessProcessor extends AbstractJBPMProcessor {
	Logger log = LoggerFactory.getLogger(getClass());

	public JBPMProcessProcessor(String processName, String opration) {
		super(processName, opration);
	}

	@Override
	public Object process(List<Record> record) {
		init(record);
		try {

			if ("StartProcess".equalsIgnoreCase(getOpration())) {

				ProcessInstance pinst = getKsession().startProcess(getProcessName(), null);
				Map<String, Object> tdata = new HashMap<String, Object>();
				tdata.put("id", pinst.getId());
				tdata.put("status", getState(pinst.getState()));

				return tdata;

			} else if ("SignalEvent".equalsIgnoreCase(getOpration())) {

				getKsession().signalEvent(getOpration(), record);
				Map<String, Object> tdata = new HashMap<String, Object>();
				tdata.put("status", getState(3));

				return tdata;

			}

		} catch (Exception e1) {
			return null;
		}

		return null;

	}

}
