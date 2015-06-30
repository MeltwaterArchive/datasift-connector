# Gnip Faker

Fakes the Gnip streaming server.

## Operation

1. Reads the data file into memory (make sure it's not too big) and splits it into lines.
2. Listens for connections.
3. Sends one line at a time from the file.
4. Waits for a random delay between mindelay and maxdelay (milliseconds).
5. Repeat from 3.

## Usage

./server.js [port=5001] [datafile=data/raw.json] [mindelay=4] [maxdelay=500]
