package com.couchbase.example;

import java.util.List;

import org.joda.time.DateTime;

import rx.Observable;

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.Stale;
import com.couchbase.client.java.view.ViewQuery;


public class EventCountConsistencyChecker {

	public static void main(String[] args) {
		CouchbaseCluster cluster = CouchbaseCluster.create("localhost");
		AsyncBucket bucket = cluster.openBucket("event-logs").async();
		
		/* 
		 * View definition
function (doc, meta) {
  if(doc.eventid && doc.createdate)
  	// +32400000 to adjust timezone.
  	// http://www.epochconverter.com/epoch/timezones.php?epoch=1435590504
    emit([doc.eventid].concat(dateToArray(doc.createdate + 32400000)), null);
}
		 */
		
		int year = 2015;
		int month = 6;
		int dayOfMonth = 30;

		// from 2015/06/30 10:00 to 14:00
		int fromH = 0;
		int toH = 23;
		
		List<Object> docs = Observable.just("login", "logout", "click-a", "click-b").flatMap(eventId -> {
			ViewQuery query = ViewQuery.from("counts", "by_event_type")
			.groupLevel(5)
			.startKey(JsonArray.from(eventId, year, month, dayOfMonth, fromH))
			.endKey(JsonArray.from(eventId, year, month, dayOfMonth, toH + 1))
			.stale(Stale.FALSE);
			return bucket.query(query)
					.flatMap(AsyncViewResult::rows);
			
		}).flatMap(vr -> {
			JsonArray key = JsonArray.fromJson(vr.key().toString());
			DateTime d = new DateTime(key.getInt(1), key.getInt(2), key.getInt(3), key.getInt(4), 0, 0);
			String docId = key.get(0) + "::" + d.toString("yyyyMMddHH") + "::counter";
			return bucket.get(docId, JsonLongDocument.class).flatMap(counterDoc -> {
				if(!Long.valueOf(vr.value().toString()).equals(counterDoc.content())){
					return Observable.just(new InconsistentCounterPair(counterDoc, vr));
				}
				return Observable.empty();
			});
		}).toList().toBlocking().single();

		for (Object doc : docs) {
			System.out.println(doc);
		}
		
		cluster.disconnect();
	}

}
