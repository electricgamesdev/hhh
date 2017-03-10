package com.hydrides.core;

public class RedirectException extends Exception {

	private Domain domain = null;
	private Object record = null;
	private String to = null;

	public RedirectException(Domain domain, Object record, String to) {
		this.domain = domain;
		this.record = record;
		this.to = to;
	}

	public String getTo() {
		return to;
	}

	public Domain getDomain() {
		return domain;
	}

	public Object getData() {
		return record;
	}
}
