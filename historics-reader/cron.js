#!/usr/bin/env node
var logger = require('log4js').getLogger('historics-reader'),
    utils = require('./lib/utils.js'),
    config = require('./config.js'),
    Jobs = require('./lib/jobs.js'),
    kafka = require('kafka-node'),
    fs = require('fs'),
    https = require('https'),
    url = require('url');

var jobs = new Jobs(config)
var currTimestampMs = utils.getCurrentTimestampMs()
var producer = kafka.Producer,
    client = new kafka.Client(config.kafka.socket, config.kafka.clientId),
    producer = new Producer(client);

producer.on('ready',function(){
    logger.info("kafka producer is connected");
    updateJobs()
});


producer.on('error',function(err){
    log.error("Error initialising Kafka producer: " + err)
});

function updateJobs() {

};

function processJobs() {

};

function lockJob(jobId) {

};

function fetchJobResults(jobId) {

};

function updateJobResults(jobId, data) {

};

function processFiles() {

};

function downloadFile(urlStr) {
    urlObj = url.parse(urlStr)
    var options = {
        hostname  : urlObj.hostname,
        port      : urlObj.port,
        path      : urlObj.path,
        method    : 'GET'
    };

    var pathSplit = urlObj.path.split('/')
    var fileName = pathSplit[pathSplit.length-1]
    var file = fs.createWriteStream(fileName);

    var req = https.get(options, function(res) {
        res.on('data', function(d) {
            file.write(d);
        });
        res.on('close', function() {
            processFile(fileName)
        });
    });
    req.end();

    req.on('error', function(e) {
        logger.error(e);
    });
};

function processFile(path) {

};

function validateJSON(line) {
    try {
        jsonStr = JSON.parse(line);
        if (!('info' in jsonStr)) {
            sendToKafka(line)
        }
    } catch (e) {
        logger.info("Invalid JSON parsed from downloaded file.")
    }
};

function sendToKafka(json) {

};

function deleteFile(path) {
    fs.unlink(path, function(err) {
        if (err) logger.error('Could not delete temporary job file ' + path)
        else logger.info('Successfully deleted job file: ' + path);
    })
};

function updateStats(stats) {

};

function completeJob(jobId) {

};
)
process.on('uncaughtException', function (err) {
    logger.error('Uncaught exception! ' + err)
});
