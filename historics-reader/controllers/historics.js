var config = require('../config.js')
  , Jobs = require('../lib/jobs.js')
  , logger = require('log4js').getLogger('historics-api-controller')

var jobs = new Jobs(config)

module.exports = {
	addJob: function(req, res, next) {
		if (!req.body || !req.body.job_id) {
			return next(new restify.BadRequest('Please specify a job ID'))
		} else {
			jobs.exists(req.body.job_id, function(err, result) {
				if (result) {
					// Job already exists.
					res.send(409, {error:'Job already exists'})
					return next()
				} else {
					jobs.createJob(req.body.job_id, function(err, job) {
						if (err) {
							res.send(500, {error: 'Failed to create job: ' + err})
							return next()
						} else {
							res.send(job)
							return next()
						}
					})
				}
			})
		}
	},

	getJob: function(req, res, next) {
		jobs.getJob(req.params.id, function(err, job) {
			if (err) {
				if (err == 'Job not found') {
					res.send(404, {error:err})
				} else {
					res.send(500, {error:'Failed to get job: ' + err})
				}
				return next()
			} else {
				res.send(200, job)
				return next()
			}
		})
	},

	getJobLog: function(req, res, next) {
		res.setHeader('Content-Type', 'text/plain')
		var level = 'info'
		if (req.query.level) {
			level = req.query.level.toLowerCase()
		}
		jobs.getJobLog(req.params.id, level, function(err, log) {
			if (err) {
				if (err == 'Job not found') {
					res.send(404, err)
				} else {
					res.send(500, 'Failed to get job log: ' + err)
				}
				return next()
			} else {
				res.send(200, log)
				return next()
			}
		})
	},

	getJobs: function(req, res, next) {
		jobs.getJobs(req.query, function(err, jobs) {
			if (err) {
				res.send(500, {error:'Failed to get jobs: ' + err})
				return next()
			} else {
				res.send(200, jobs)
				return next()
			}
		})
	}
}
