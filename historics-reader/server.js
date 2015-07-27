#!/usr/bin/env node
var restify = require('restify')
  , config = require('./config.js')
  , logger = require('log4js').getLogger('historics-reader')
  , historicsController = require('./controllers/historics.js')

var server = restify.createServer()

server.use(restify.queryParser({mapParams: false}))
server.use(restify.bodyParser())

server.pre(function(req, res, next) {
	res.setHeader('Content-Type', 'application/json')
  return next()
})

server.post('/api/v1/historics',         historicsController.addJob)
server.get ('/api/v1/historics',         historicsController.getJobs)
server.get ('/api/v1/historics/:id',     historicsController.getJob)
server.get ('/api/v1/historics/:id/log', historicsController.getJobLog)

if (process.env.env != 'dev') {
	process.on('uncaughtException', function (err) {
		logger.error(JSON.parse(JSON.stringify(err, ['stack', 'message', 'inner'], 2)))
	})
}

server.listen(config.http_port || 8888, function() {
	logger.info('Server listening at %s', server.url)
})
