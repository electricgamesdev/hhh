package com.hydrides.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.avro.generic.GenericData.Record;

import com.hydrides.core.Constants;
import com.hydrides.core.Processor;

public class FileProcessor extends Processor {

	private File file = null;
	private Constants.RECORD type = null;

	public FileProcessor(File file, Constants.RECORD type) {
		this.file = file;
		setType(type);
	}

	public Object process(List<Record> record) {

		try {
			return new FileInputStream(file);

		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}

		return null;
	}

	@Override
	public boolean isRunning(Object data) {
		return false;
	}

}
