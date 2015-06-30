# DataSift Ingestion Faker

Fakes the Datasift Ingestion endpoint.

## Operation

Listens for POST requests on the port and optionally writes the received data to stdout.

## Usage

`node server.js [port=5002] [level=ERROR]i [mode=BULK|STREAM]`

Options for `level` include:

- `DEBUG` which writes out all the data received to stdout.
- `INFO` which writes a `.` to stdout for each chunk received.
- Anything else prints nothing to stdout.
