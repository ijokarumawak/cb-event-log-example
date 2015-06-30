package com.couchbase.example;

import static rx.Observable.range;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;

import rx.Observable;

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonLongDocument;

public class EventDataReader {

	public static void main(String[] args) {
		CouchbaseCluster cluster = CouchbaseCluster.create("localhost");
		AsyncBucket bucket = cluster.openBucket("event-logs").async();
		
		int year = 2015;
		int month = 6;
		int dayOfMonth = 30;

		// from 2015/06/30 10:00 to 14:00
		List<Object> docs = range(10, 5).flatMap(h -> {
			return Observable.just("login", "logout", "click-a", "click-b").flatMap(eventId -> {
				DateTime d = new DateTime(year, month, dayOfMonth, h, 0, 0);
				String docId = eventId + "::" + d.toString("yyyyMMddHH") + "::counter";
				return bucket.get(docId, JsonLongDocument.class);
			});
		}).toList().toBlocking().single();

		Collections.sort(docs, (d1, d2) -> ((JsonLongDocument) d1).id()
				.compareTo(((JsonLongDocument) d2).id()));
		
		for (Object doc : docs) {
			System.out.println(doc);
		}
		
		cluster.disconnect();
	}

}
