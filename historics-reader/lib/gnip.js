var http = require('http');

function Gnip(config) {
	this._use_ssl = config.gnip.use_ssl
	this._domain = config.gnip.domain
	this._port = config.gnip.port
	this._account_name = config.gnip.account_name
}

Gnip.prototype.getJob = function(job_id, next) {
	var options = {
		host: this._domain,
		path: '/accounts/' + this._account_name + '/publishers/twitter/historical/track/jobs/' + job_id + '.json',
		port: this._port
		//headers: {'custom': 'Custom Header Demo works'}
	}

	var req = http.request(options, function(response) {
		var str = ''
		response.on('data', function (chunk) {
			str += chunk;
		});

		response.on('end', function () {
			console.log(str);
		});
	});
	req.end();
}
