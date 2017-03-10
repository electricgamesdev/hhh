package com.hydrides.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData.Record;
import org.w3c.dom.html.HTMLInputElement;

import com.hydrides.core.Constants;
import com.hydrides.core.Processor;

public class HTMLProcessor extends Processor {

	private File file = null;
	private Constants.RECORD type = null;
	private List<File> scriptList = null;

	

	public HTMLProcessor(File file, Constants.RECORD type) {
		this.file = file;
		setType(type);
		this.scriptList = new ArrayList<>();
	}

	@Override
	public boolean isRunning(Object data) {
		if (data instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest) data;
			if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
				// This is ajax request
				// so conatiner already running;
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	public Object process(List<Record> record) {

		try {
			// return new FileInputStream(file);
			URL url = file.toURI().toURL();
			// URLConnection yc = url.openConnection();

			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			// conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			if (record != null && record.size() > 0) {
				Record rec = record.get(0);
				if (rec != null) {
					String charset = "UTF-8";
					String s = "";
					int i = 1;
					int size = rec.getSchema().getFields().size();

					for (Field f : rec.getSchema().getFields()) {
						s = s + f.name() + "=" + URLEncoder.encode(rec.get(f.name()).toString(), charset);
						if (i < size)
							s += "&";
						i++;
					}
					if (getCallBackPath() != null) {
						if (!s.isEmpty())
							s += "&";
						s = "callback=" + getCallBackPath();
					}

					System.out.println(file + "...file...." + s);
					// conn.setFixedLengthStreamingMode(s.getBytes().length);
				}
			}

			List<InputStream> list = new ArrayList<>();
			InputStream input = conn.getInputStream();
			list.add(input);
			for (File file : this.scriptList) {
				list.add(new FileInputStream(file));
			}
			return new SequenceInputStream(Collections.enumeration(list));
		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}

	public void addScript(File f) {
		this.scriptList.add(f);

	}

}
