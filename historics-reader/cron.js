#!/usr/bin/env node
var logger = require('log4js').getLogger('historics-reader'),
    utils = require('./lib/utils.js'),
    config = require('./config.js'),
    Jobs = require('./lib/jobs.js'),
    Gnip = require('./lib/gnip.js'),
    kafka = require('kafka-node'),
    fs = require('fs'),
    https = require('https'),
    url = require('url'),
    zlib = require('zlib'),
    es = require('event-stream'),
    Sync = require('sync'),
    ASync = require('async'),
    os = require('os');

logger.info('Historics processing script starting at timestamp: ' + utils.getCurrentTimestampMs())

var jobs = new Jobs(config)
var producer = kafka.Producer,
    client = new kafka.Client(config.kafka.socket, config.kafka.clientId),
    producer = new Producer(client);

producer.on('ready',function(){
    logger.info("kafka producer is connected");
    jobs.open(function(err) {
        if (err) logger.error('Could not open sqlite database ' + config.sqlite_filename + '. Error: ' + err)
        else readJobs()
    });
});

producer.on('error',function(err){
    log.error("Error initialising Kafka producer: " + err)
});

function readJobs() {
    var filters = { status_exclusion: ['processing', 'done'] }
    jobs.getJobs(filters, function(err, rows) {
        if (err) logger.error('Could not retrieve status of jobs from sqlite db: ' + err)
        else updateJobs(rows)
    });
};

function updateJobs(jobRows) {
    jobRows.forEach(function (jobItem) {
        Sync(function () {
            var status = fetchJobStatus.sync(jobItem.id, null)
            if (status && (status != jobItem.status)) {
                updateJobStatus.sync(jobItem.id, status, null)
            }
        })
    });
    processJobs()
};

function fetchJobStatus(jobId, cb) {
    Gnip.getJob(jobId, function(err, jsonData) {
        if (err) logger.error('Could not retrieve status for job ' + jobId + ' from GNIP API. ' + err)
        else cb(jsonData.status)
    });
}

function updateJobStatus(jobId, status, cb) {
    Jobs.updateJob(jobId, {status: status}, function(err, row) {
        if (err) logger.error('Could not update status for job ' + jobId + ' to ' + status + '. ' + err)
        else cb()
    });
}

function processJobs() {
    Sync(function() {
        lockJob.sync(null)
    })

    ASync.whilst(function() {
        return
    })
}

function lockJob(cb) {
    Jobs.lockJob(function(err) {
        if (err) logger.error('Could not lock job ' + jobId + ' in sqlite db. ' + err)
        else {

        }
    });
};

function fetchJobResults(jobId) {

};

function updateJobResults(jobId, data) {

};

function processFiles(files) {

};

function downloadFile(urlStr) {
    var urlObj = url.parse(urlStr)
    var options = {
        hostname  : urlObj.hostname,
        port      : urlObj.port,
        path      : urlObj.path,
        method    : 'GET'
    };

    var pathSplit = urlObj.path.split('/')
    var fileName = pathSplit[pathSplit.length-1]
    var file = fs.createWriteStream('/tmp/' + fileName);
    logger.info('Downloading file ' + urlStr + ' to local file ' + fileName)

    var req = https.get(options, function(res) {
        res.on('data', function(d) {
            file.write(d);
        });
        res.on('close', function() {
            processFile(fileName)
        });
    });
    req.end();

    req.on('error', function(err) {
        logger.error('HTTP GET to download ' + urlStr + ' encountered error: ' + err);
    });
};

function processFile(path) {
    var gunzip = zlib.createGunzip();
    s = fs.createReadStream(path, {flags: 'r'})
            .pipe(gunzip)
            .pipe(es.split())
            .pipe(es.map(function(line) {
                validateJSON(line)
            }));
};

function validateJSON(line) {
    try {
        jsonStr = JSON.parse(line);
        if (!('info' in jsonStr)) {
            sendToKafka(line)
        }
    } catch (err) {
        logger.info('Invalid JSON parsed from downloaded file. Detail: ' + err)
    }
};

function sendToKafka(json) {
    var payload = [
        {
            topic: config.kafka.topic,
            messages: [json],
            partition: 0,
            attributes: 0
        }
    ]
    producer.send(payload, function(err, data) {
        if (err) logger.error('Kafka producer error sending interaction: ' + err)
        else logger.debug('Producer sent data successfully.' + data)
    });
};

function deleteFile(path) {
    fs.unlink(path, function(err) {
        if (err) logger.error('Could not delete temporary job file ' + path)
        else {
            logger.info('Successfully deleted job file: ' + path);
        }
    })
};

function updateStats(stats) {
    //
};

function completeJob(jobId) {
    jobs.updateJob(jobId, {status: 'done'}, function(err, row) {
        if (err) logger.error('Could not update job ' + jobId + ' status to done. ' + err)
        else {
            logger.info('Changed status for job ' + jobId + ' to done')
            jobs.updateJob(jobId, {lock: ''}, function (err, row) {
                if (err) logger.error('Could not unlock job ' + jobId + '. ' + err)
                else logger.info('Unlocked job ' + jobId)
            });
        }
    });
};

process.on('uncaughtException', function (err) {
    logger.error('Uncaught exception! ' + err)
});
