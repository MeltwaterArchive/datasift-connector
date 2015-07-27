#!/usr/bin/env node
var fs = require('fs')
		, http = require('http')
		, port = process.argv[2] || 5001
		, data_filename = process.argv[3] || 'data/raw.json'
		, min_delay_ms = process.argv[4] || 4
		, max_delay_ms = process.argv[5] || 500

var data = fs.readFileSync(data_filename).toString().split("\n");

var handleGetJobRequest = function(req, res) {
	console.log('Get job request received from ' + req.connection.remoteAddress + ': ' + req.url)
	// Get the account from the URL - this will tell us which canned response is wanted.
	res.writeHead(200, {'Content-Type': 'application/json'})
	var json = {error: 'Unknown response requested'}
	var url_bits = req.url.split('/')
	switch (url_bits[2]) {
		case 'accepted':
			json = {
				'title':'my_job',
				'account':'gnip-customer',
				'publisher':'twitter',
				'streamType':'track',
				'format':'activity-streams',
				'fromDate':'201201010000',
				'toDate':'201201010001',
				'requestedBy':'foo@gnip-customer.com',
				'requestedAt':'2012-06-13T22:43:27Z',
				'status': 'accepted',
				'statusMessage': 'Job running.',
				'quote': {
					'costDollars': 5000,
					'estimatedActivityCount':10000,
					'estimatedDurationHours':72,
					'estimatedFileSizeMb': 10.0,
					'expiresAt': '2012-06-16T22:43:00Z'
				},
				'percentComplete': 42
			}
			break

		case 'delivered':
			json = {
				'title':'my_job',
				'account':'gnip-customer',
				'publisher':'twitter',
				'streamType':'track',
				'format':'activity-streams',
				'fromDate':'201201010000',
				'toDate':'201201010001',
				'requestedBy':'foo@gnip-customer.com',
				'requestedAt':'2012-06-13T22:43:27Z',
				'status': 'delivered',
				'statusMessage': 'Job delivered and available for download.',
				'jobURL': 'http://' + req.headers.host + '/accounts/delivered/publishers/twitter/historical/track/jobs/' + url_bits[8],
				'quote': {
					'costDollars': 5000,
					'estimatedActivityCount':10000,
					'estimatedDurationHours':72,
					'estimatedFileSizeMb': 10.0,
					'expiresAt': '2012-06-16T22:43:00Z'
				},
				'acceptedBy': 'foo@gnip-customer.com',
				'acceptedAt': '2012-06-14T22:43:27Z',
				'percentComplete': 100,
				'results': {
					'completedAt':'2012-06 17T22:43:27Z',
					'activityCount':1200,
					'fileCount':1000,
					'fileSizeMb':10.0,
					'dataURL': 'http://' + req.headers.host + '/accounts/results/publishers/twitter/historical/track/jobs/' + url_bits[8].substring(0, url_bits[8].length - 5) + '/results.json',
					'expiresAt': '2012-06-30T22:43:00Z'
				}
			}
			break
	}
	res.write(JSON.stringify(json))
	res.end()
}

var sendNextLine = function(res, line_number) {
	if (!data[line_number]) {
		line_number = 0
	}
	res.write(data[line_number] + "\n")
	console.log(data[line_number])
	setTimeout(function() {
		sendNextLine(res, line_number + 1)
	}, Math.floor(Math.random() * (max_delay_ms - min_delay_ms) + min_delay_ms))
}

var handleStreamingConnection = function(req, res) {
	console.log('Streaming connection received from ' + req.connection.remoteAddress + ': ' + req.url)
	res.writeHead(200, {'Content-Type': 'application/json'})
	res.on('end', function() {
		console.log('Disconnected from ' + req.connection.remoteAddress)
		res.end()
	});
	sendNextLine(res, 0)
}

var server = http.createServer(function (req, res) {
	if (req.url.indexOf('/publishers/twitter/historical/track/jobs/') > -1) {
		handleGetJobRequest(req, res)
	} else if (req.url.indexOf('/streams/track/') > -1) {
		handleStreamingConnection(req, res)
	} else {
		res.writeHead(404, {'Content-Type': 'text/plain'})
		res.write('Resource not found')
		res.end()
	}
})

console.log('Listening on port ' + port)
server.listen(Number(port))
