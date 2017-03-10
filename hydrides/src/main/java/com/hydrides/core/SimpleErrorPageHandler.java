package com.hydrides.core;

import java.util.Set;

import io.undertow.io.Sender;
import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class SimpleErrorPageHandler implements HttpHandler {
	private final HttpHandler next;

	public SimpleErrorPageHandler(final HttpHandler next) {
		this.next = next;
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		exchange.addDefaultResponseListener(new DefaultResponseListener() {
			
			
			@Override
			public boolean handleDefaultResponse(final HttpServerExchange exchange) {
				if (!exchange.isResponseChannelAvailable()) {
					return false;
				}
				
				if (exchange.getResponseCode() == 500) {
					final String errorPage = "<html><head><title>Error</title></head><body>Internal Error</body></html>";
					exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "" + errorPage.length());
					exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
					Sender sender = exchange.getResponseSender();
					sender.send(errorPage);
					return true;
				}
				return false;
			}
		});
		next.handleRequest(exchange);
	}
}
