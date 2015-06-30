package com.couchbase.example;

import static rx.Observable.from;
import static rx.Observable.range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import rx.Observable;

import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.retry.Retry;

public class EventDataGenerator {

	public static void main(String[] args) {

		CouchbaseCluster cluster = CouchbaseCluster.create("localhost");
		AsyncBucket bucket = cluster.openBucket("event-logs").async();
		
		// EventId and its provability (n/100 times).
		// n=100 means an event occur every single seconds.
		Map<String, Integer> eventProvabilities = new HashMap<>();
		eventProvabilities.put("login", 10);
		eventProvabilities.put("logout", 5);
		eventProvabilities.put("click-a", 20);
		eventProvabilities.put("click-b", 30);
		
		// Anomaly occur n/10000 times.
		int anomaliesRate = 1;
		
		Random random = new Random();
		
		// 86400 times.
		int year = 2015;
		int month = 6;
		int dayOfMonth = 30;

		List<Object> failures = range(0, 24).flatMap(h -> {
			return range(0, 60).flatMap(m -> {
				return range(0, 60).flatMap(s -> {
					DateTime d = new DateTime(year, month, dayOfMonth, h, m, s);
					long time = d.toDate().getTime();
					return from(eventProvabilities.entrySet()).flatMap(event -> {
						
						if(random.nextInt(100) >= event.getValue()){
							// Skip based on the event provability .
							return Observable.empty();
						}

						String eventId = event.getKey();
						
						String ymdh = d.toString("yyyyMMddHH");
						Observable<String> addEvent = bucket.counter(eventId + "::" + ymdh + "::counter", 1, 1).flatMap(counterDoc -> {
							// Increment a counter.
							String docId = eventId + "::" + ymdh + "::" + counterDoc.content();
							if(random.nextInt(10000) < anomaliesRate){
								// Simulate anomalies.
								return Observable.just("Failed to insert " + docId);
							} else {
								// Insert an event object.
								JsonObject eventJson = JsonObject.create()
									.put("eventid", eventId)
									.put("createdate", time)
									.put("somekey", "somevalue");
								JsonDocument eventDoc = JsonDocument.create(docId, eventJson);
								return bucket.insert(eventDoc).flatMap(res -> {
									System.out.println(Thread.currentThread().getName() + "::" + res.id());
									return Observable.empty();
								});
							}
						});
						// Inserting lots of event object can cause BackPressureException.
						// Wrapping the observable with exponential retry.
						// http://blog.couchbase.com/javasdk-2.2-dp
						return Retry.wrapForRetry(addEvent, 5, Delay.exponential(TimeUnit.SECONDS));
					});
				});
			});
		}).toList().toBlocking().single();

		System.out.println("## Dump simulated failures...");
		for (Object failure : failures) {
			System.out.println(failure);
		}
		
		cluster.disconnect();
		
		
	}

}
