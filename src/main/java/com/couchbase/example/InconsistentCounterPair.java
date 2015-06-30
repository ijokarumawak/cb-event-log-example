package com.couchbase.example;

import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.view.AsyncViewRow;

public class InconsistentCounterPair {

	private JsonLongDocument counterDoc;
	private AsyncViewRow viewResult;
	
	public InconsistentCounterPair(JsonLongDocument counterDoc,
			AsyncViewRow viewResult) {
		super();
		this.counterDoc = counterDoc;
		this.viewResult = viewResult;
	}

	public JsonLongDocument getCounterDoc() {
		return counterDoc;
	}

	public void setCounterDoc(JsonLongDocument counterDoc) {
		this.counterDoc = counterDoc;
	}

	public AsyncViewRow getViewResult() {
		return viewResult;
	}

	public void setViewResult(AsyncViewRow viewResult) {
		this.viewResult = viewResult;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("counterDoc=").append(counterDoc)
				.append(",viewResult=").append(viewResult).toString();
	}

	
	
	
}
