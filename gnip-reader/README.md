# gnip-reader

Connects to data stream and outputs items to Kafka queue


## Metrics

### Reader

- sent : the meter for measuring sends to the onward queue.
- send-error : the meter for measuring errors sending to the onward queue.
- read : the meter for measuring reads of Twitter messages from the buffer.
- read-error : the meter for measuring errors on reading Twitter messages from the buffer.
- shutdown : the meter for measuring the calls to the shutdown hook.
- disconnected : the meter for measuring whether the Gnip client has exited.

### Gnip client

- client.200s : Number of 200 http response codes in the lifetime of the client.
- client.400s : Number of 4xx http response codes in the lifetime of the client.
- client.500s : Number of 5xx http response codes in the lifetime of the client.
- client.messages : Number of messages the client has processed.
- client.disconnects :  Number of disconnects.
- client.connections :  Number of connections/reconnections.
- client.connection-failures : Number of connection failures. This includes 4xxs, 5xxs, bad host names, etc.
- client.client-events-dropped : Number of events dropped from the event queue.
- client.messages-dropped :  Number of messages dropped from the message queue.  Occurs when messages in the message queue aren't dequeued fast enough.
