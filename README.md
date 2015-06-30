# cb-event-log-example
An event log storing application example using Couchbase Server as a backend.

# How to setup

1. Import this project into your IDE (Eclipse or IntelliJ ... etc)
2. Edit Couchbase Server connection in the source files if necessary
3. Create a bucket named 'event-logs' in your Couchbase Server cluster
4. Create a design document named 'counts'
5. Create a view named 'by_event_type' in 'counts' design document
6. Define map function as follows:

  ```
  function (doc, meta) {
    if(doc.eventid && doc.createdate) {
      // +32400000 to adjust timezone into JST.
      // http://www.epochconverter.com/epoch/timezones.php?epoch=1435590504
      emit([doc.eventid].concat(dateToArray(doc.createdate + 32400000)), null);
    }
  }
  ```

7. Define reduce function as follows:

  ```
  _count
  ```

8. Publish 'counts' design document to production

# How to execute
1. Generate event objects by executing EventDataGenerator.java
    It logs simulated failure ingestion like below:

  ```
  ## Dump simulated failures...
  Failed to insert login::2015063000::341
  Failed to insert click-a::2015063008::318
  ```

2. You can retrieve the number of event occurrence in specific hours by executing EventDataReader.java
3. Then you can check if there is any inconsistency between counter objects and actual number of events by executing EventCountConsistencyChecker.java
    The class outputs inconsistency like below (it should match with the simulated failures) :

  ```
  counterDoc=JsonLongDocument{id='login::2015063000::counter', cas=72952735858688, expiry=0, content=375},viewResult=DefaultViewRow{id='null', key=["login",2015,6,30,0], value=374, document=null}
  counterDoc=JsonLongDocument{id='click-a::2015063008::counter', cas=72955154661376, expiry=0, content=704},viewResult=DefaultViewRow{id='null', key=["click-a",2015,6,30,8], value=703, document=null}
  ```
