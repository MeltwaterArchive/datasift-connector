#!/usr/bin/env node
var fs = require('fs')
    , http = require('http')
    , port = process.argv[2] || 5001
    , data_filename = process.argv[3] || 'data/raw.json'
    , min_delay_ms = process.argv[4] || 4
    , max_delay_ms = process.argv[5] || 500

var data = fs.readFileSync(data_filename).toString().split("\n");

var sendNextLine = function(res, line_number) {
	if (!data[line_number]) {
		line_number = 0
	}
	res.write(data[line_number] + "\n")
	setTimeout(function() {
		sendNextLine(res, line_number + 1)
	}, Math.floor(Math.random() * (max_delay_ms - min_delay_ms) + min_delay_ms))
}

var server = http.createServer(function (req, res) {
	console.log('Connection received from ' + req.connection.remoteAddress + ': ' + req.url)
	res.writeHead(200, {'Content-Type': 'application/json'})
	res.on('end', function() {
		console.log('Disconnected from ' + req.connection.remoteAddress)
		res.end()
	});
	sendNextLine(res, 0)
})

console.log('Listening on port ' + port)
server.listen(Number(port))
