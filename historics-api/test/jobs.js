var fs = require("fs")
  , assert = require("assert")
  , Jobs = require("../lib/jobs")
  , utils = require('../lib/utils')
  , log4js = require('log4js')
  , logger = log4js.getLogger('test')

// Disable logging.
log4js.configure({appenders: []})

var db_filename = '/tmp/historics-reader-test-database.sqlite';
var getJobsObject = function() { return new Jobs({sqlite_filename: db_filename}) }

var deleteDatabaseFile = function(done) {
	if (fs.existsSync(db_filename)) {
		fs.unlinkSync(db_filename)
	}
	done()
}

describe('Jobs', function() {
	before(deleteDatabaseFile)
	after(deleteDatabaseFile)

	describe('Open then close the database', function() {
		it('should create the database file', function(done) {
			var jobs = getJobsObject()
			jobs.open(function(err) {
				if (err) throw err

				jobs.close(function(err) {
					if (err) throw err

					assert.equal(fs.existsSync(db_filename), true)
					done()
				})
			})
		})
	})

	describe('Create a new Job', function() {
		var jobs = getJobsObject()
		var job = null

		beforeEach(function(done) {
			jobs.createJob('job' + utils.randomInt(100,999), function(err, j) {
				if (err) throw err
				job = j
				done()
			})
		})

		it('should have an added_at time within the last 60 seconds', function(done) {
			var now = utils.getCurrentTimestampMs()
			assert.equal(job.added_at >= (now - 60) && job.added_at <= now, true)
			done()
		})

		it('should have a completed_at of 0', function(done) {
			assert.equal(job.completed_at, 0)
			done()
		})

		it('should have a status of unknown', function(done) {
			assert.equal(job.status, 'unknown')
			done()
		})

		it('should have an empty suspect_minutes_url', function(done) {
			assert.equal(job.suspect_minutes_url.length, 0)
			done()
		})

		it('should have an empty urls array', function(done) {
			assert.equal(job.urls.length, 0)
			done()
		})

		it('should have an empty stats object', function(done) {
			assert.equal(job.stats.items, 0)
			assert.equal(job.stats.percent_complete, 0)
			assert.equal(job.stats.min_timestamp, 0)
			assert.equal(job.stats.max_timestamp, 0)
			assert.equal(job.stats.error_count, 0)
			done()
		})

		it('should have an empty lock', function(done) {
			assert.equal(job.lock.length, 0)
			done()
		})
	})

	describe('Update a Job', function() {
		var job_id = 'updated-job'
		var updated_fields = {
			'status': 'updated',
			'completed_at': 987654321,
			'suspect_minutes_url': 'http://suspect.minutes.com/url',
			'urls': ['http://google.com', 'http://twitter.com', 'http://facebook.com'],
			'stats': {items: 100, percent_complete: 12, min_timestamp: 123450, max_timestamp: 123459, error_count: 42},
			'lock': 'localhost:1234'
		}
		var jobs = getJobsObject()
		var job = null

		before(function(done) {
			jobs.createJob(job_id, function(err, j) {
				if (err) throw err
				jobs.updateJob(job_id, updated_fields, function(err, j) {
					if (err) throw err
					job = j
					done()
				})
			})
		})

		it('should have an added_at time within the last 60 seconds', function(done) {
			var now = utils.getCurrentTimestampMs()
			assert.equal(job.added_at >= (now - 60) && job.added_at <= now, true)
			done()
		})

		it('should have an updated completed_at', function(done) {
			assert.equal(job.completed_at, updated_fields.completed_at)
			done()
		})

		it('should have an updated status', function(done) {
			assert.equal(job.status, 'updated')
			done()
		})

		it('should have an updated suspect_minutes_url', function(done) {
			assert.equal(job.suspect_minutes_url, updated_fields.suspect_minutes_url)
			done()
		})

		it('should have an updated urls array', function(done) {
			assert.equal(job.urls.equals(updated_fields.urls), true)
			done()
		})

		it('should have an updated stats object', function(done) {
			assert.equal(job.stats.equals(updated_fields.stats), true)
			done()
		})

		it('should have an empty lock', function(done) {
			assert.equal(job.lock, updated_fields.lock)
			done()
		})
	})})
