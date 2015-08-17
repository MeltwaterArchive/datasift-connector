# Internal Historics API

The HTTP API runs on port 8888 by default, and is implemented in NodeJS.
The server.js application runs under Supervisor once provisioned. It's Configuration file is located at `/usr/lib/datasift/historics-api/config.js`.

## GET /api/v1/historics?page=1&per_page=20&status=accepted

This endpoint will return a list of jobs with the requested status. If the status is not provided it will return all jobs.
The status param refers to the status as reported by the Gnip API at the last point the historics-reader processed the job ID.

Example response:
```
[
  {
    "id": "1234567890",
    "added_at": 139899242,
    "completed_at": 0,
    "status": "accepted",
    "urls": [],
    "suspect_minutes_url": {},
    "stats": {
      "items": 0,
      "percent_complete": 0,
      "min_timestamp": 0,
      "max_timestamp": 0,
      "error_count": 0
    }
  },
  {
    "id": "1234567890",
    "added_at": 139899242,
    "status": "processing",
    "urls": [
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?...",
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?...",
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?...",
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?...",
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?..."
    ],
    "suspect_minutes_url": "https://archive.replay.historical.review.s3.amazonaws.com/customers/gnip-biz/20120101-20120101_5zzax1awxq/suspectMinutes.json?...",
    "stats": {
      "items": 465,
      "percent_complete": 20,
      "min_timestamp": 1399899242,
      "max_timestamp": 1405124592,
      "error_count": 0
    }
  },
  {
    "id": "1092837645",
    "added_at": 139899242,
    "status": "done",
    "urls": [
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?...",
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?...",
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?...",
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?...",
      "https://archive.replay.historical.review.s3.amazonaws.com/historical/twitter/track/original/gnip-customer/2012/08/29/20120701-20120701_jbmfgctzw5/2012/07/01/00/10_activities.json.gz?..."
    ],
    "suspect_minutes_url": "https://archive.replay.historical.review.s3.amazonaws.com/customers/gnip-biz/20120101-20120101_5zzax1awxq/suspectMinutes.json?...",
    "stats": {
      "items": 2345,
      "percent_complete": 100,
      "min_timestamp": 1399899242,
      "max_timestamp": 1431435226,
      "error_count": 42
    }
  }
]
```
### Responses

400 Bad Request

Sent if the request parameters are invalid (i.e. non-integer or < 0 page, per_page values requested).
```
{"error": "This should explain what was wrong with the request"}
```

## GET /api/v1/historics/<job_id>

Returns the data for a single job.

Example response:
```
{
  "id": "1234567890",
  "added_at": 139899242,
  "completed_at": 0,
  "status": "accepted",
  "urls": [],
  "suspect_minutes": {},
  "stats": {
    "items": 0,
    "percent_complete": 0,
    "min_timestamp": 0,
    "max_timestamp": 0,
    "error_count": 0
  }
}
```
### Responses

404 Not found
```
{"error": "Job not found"}
```
500 Server error
```
{"error": "This should explain what went wrong"}
```

## GET /api/v1/historics/<job_id>/log?level=INFO

Returns the log for the specified job as a text file (Content-Type: text/plain), including only those entries where the level is at or higher than the requested level.
Log line format is: `<timestamp> [<level>] <content>\n`

## POST /api/v1/historics

This endpoint accepts new GNIP Job IDs for monitoring and processing.

### Request Body
`{"job_id": "1234567890"}`

### Responses
200 OK
Success response
```
{
    "id": "1234567890",
    "added_at": 139899242,
    "completed_at": 0,
    "status": "accepted",
    "urls": [],
    "suspect_minutes_url": {},
    "stats": {
      "items": 0,
      "percent_complete": 0,
      "min_timestamp": 0,
      "max_timestamp": 0,
      "error_count": 0
    }
  }
```

409 Conflict
Returned if the job ID already exists in the Historics data store.
```
{"error": "Job already exists"}
```
500 Server error
```
{"error": "This should explain what went wrong"}
```
