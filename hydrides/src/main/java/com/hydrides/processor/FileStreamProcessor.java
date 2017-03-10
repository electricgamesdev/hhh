package com.hydrides.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.avro.generic.GenericData.Record;

import com.hydrides.core.Constants;
import com.hydrides.core.Processor;

public class FileStreamProcessor extends Processor {

	private List<File> streams = null;

	public FileStreamProcessor(Constants.RECORD type) {
		this.streams = new ArrayList<>();
		setType(type);
	}

	public void addStream(File stream) {
		this.streams.add(stream);
	}

	public Object process(List<Record> record) throws Exception {
		List<InputStream> ss = new ArrayList<>();
		for (File s : streams) {
			ss.add(new FileInputStream(s));
		}
		return new SequenceInputStream(Collections.enumeration(ss));

	}

}
