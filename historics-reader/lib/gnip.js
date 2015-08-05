var http = require('http'),
	https = require('https'),
	logger = require('log4js').getLogger('historics-gnip-api');

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

	if (this._use_ssl) {
		var req = https.request(options, function (response) {
			var str = ''
			response.on('data', function (chunk) {
				str += chunk;
			});

			response.on('end', function () {
				next(err, JSON.parse(str))
			});

			response.on('error', function(err) {
				logger.error('Error encountered during response parsing of GNIP API status request for job ' + job_id + ': ' + err)
				next(err)
			})
		});

		req.on('error', function(err) {
			logger.error('Error encountered during GNIP API status request for job ' + job_id + ': ' + err)
			next(err)
		});

		req.end();
	} else {
		var req = http.request(options, function (response) {
			var str = ''
			response.on('data', function (chunk) {
				str += chunk;
			});

			response.on('end', function () {
				next(err, JSON.parse(str))
			});

			response.on('error', function(err) {
				logger.error('Error encountered during response parsing of GNIP API status request for job ' + job_id + ': ' + err)
				next(err)
			})
		});

		req.on('error', function(err) {
			logger.error('Error encountered during GNIP API status request for job ' + job_id + ': ' + err)
			next(err)
		});

		req.end();
	}
}

Gnip.prototype.getJobResults = function(job_id, next) {
	var options = {
		host: this._domain,
		path: '/accounts/' + this._account_name + '/publishers/twitter/historical/track/jobs/' + job_id + '/results.json',
		port: this._port
		//headers: {'custom': 'Custom Header Demo works'}
	}

	if (this._use_ssl) {
		var req = https.request(options, function (response) {
			var str = ''
			response.on('data', function (chunk) {
				str += chunk;
			});

			response.on('end', function () {
				next(err, JSON.parse(str))
			});

			response.on('error', function(err) {
				logger.error('Error encountered during response parsing of GNIP API results for job' + job_id + ': ' + err)
				next(err)
			})
		});

		req.on('error', function(err) {
			logger.error('Error encountered during GNIP API results for job ' + job_id + ': ' + err)
			next(err)
		});

		req.end();
	} else {
		var req = http.request(options, function (response) {
			var str = ''
			response.on('data', function (chunk) {
				str += chunk;
			});

			response.on('end', function () {
				next(err, JSON.parse(str))
			});

			response.on('error', function(err) {
				logger.error('Error encountered during response parsing of GNIP API results for job ' + job_id + ': ' + err)
				next(err)
			})
		});

		req.on('error', function(err) {
			logger.error('Error encountered during GNIP API results for job ' + job_id + ': ' + err)
			next(err)
		});

		req.end();
	}
}
