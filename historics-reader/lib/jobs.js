var sqlite3 = require('sqlite3').verbose()
var logger = require('log4js').getLogger('historics-job-db')
var utils = require('./utils')

var levels = {
	'debug': ["debug", "info", "warning", "error"],
	'info': ["info", "warning", "error"],
	'warning': ["warning", "error"],
	'error': ["error"],
}

function Jobs(config) {
	this._sqlite_filename = config.sqlite_filename
	this._db = false
}

Jobs.prototype.open = function(next) {
	if (!this._db) {
		logger.info("Opening database: " + this._sqlite_filename)
		var jobs_obj = this
		this._db = new sqlite3.Database(this._sqlite_filename, function(err) {
			if (err) {
				next(err)
			} else {
				jobs_obj._db.serialize()
				jobs_obj._db.run(
					"create table if not exists jobs (" +
						"id text, " +
						"added_at number, " +
						"completed_at number, " +
						"status text, " +
						"urls text, " +
						"suspect_minutes_url text, " +
						"stats text, " +
						"lock text)", function(err) {
							if (err) {
								next(err)
							} else {
								jobs_obj._db.run(
									"create table if not exists logs (" +
										"job_id text, " +
										"timestamp number, " +
										"level text, " +
										"content text)", function(err) {
											next(err, jobs_obj)
										})
							}
				})
			}
		})
	} else {
		if (next) {
			next(null, this)
		}
	}
}

Jobs.prototype.close = function(next) {
	if (this._db) {
		jobs_obj = this
		this._db.serialize(function() {
			logger.info("Closing database")
			jobs_obj._db.close(function(err) {
				if (!err) jobs_obj._db = false
				if (next) next(err)
			})
		})
	} else {
		this._db = false
		if (next) next();
	}
}

Jobs.prototype.createJob = function(job_id, next) {
	this.open(function (err, jobs_obj) {
		if (err) {
			logger.error('Jobs.createJob: [' + job_id + '] ' + err)
			if (next) next(err)
		} else {
			logger.info('Jobs.createJob: [' + job_id + '] Creating')
			jobs_obj._db.run(
				'insert into jobs (id, added_at, completed_at, status, urls, suspect_minutes_url, stats, lock) values (?, ?, 0, "unknown", "[]", "", ?, "")',
				[job_id, utils.getCurrentTimestampMs(), JSON.stringify({items: 0, percent_complete: 0, min_timestamp: 0, max_timestamp: 0, error_count: 0})],
				function() {
					if (err) {
						logger.error('Jobs.createJob: [' + job_id + '] ' + err)
						if (next) next(err)
					} else {
						logger.debug('Jobs.createJob: [' + job_id + '] Created')
						jobs_obj.log(job_id, 'info', 'Job created', function(err) {
							if (err) {
								logger.error('Jobs.createJob: [' + job_id + '] ' + err)
								if (next) next(err)
							} else {
								jobs_obj.getJob(job_id, next)
							}
						})
					}
				}
			)
		}
	})
}

Jobs.prototype.exists = function(job_id, next) {
	this.open(function (err, jobs_obj) {
		if (err) {
			logger.error('Jobs.exists: [' + job_id + '] ' + err)
			next(err)
		} else {
			jobs_obj._db.get(
				'select count(1) as num from jobs where id = ?',
				[job_id],
				function(err, row) {
					if (err) {
						logger.error('Jobs.exists: [' + job_id + '] ' + err)
					}
					next(err, row ? (row.num > 0) : false)
				}
			)
		}
	})
}

Jobs.prototype.getJobs = function(filters, next) {
	this.open(function (err, jobs_obj) {
		if (err) {
			logger.error('Jobs.getJobs: ' + err)
			next(err)
		} else {
			var params = {}
			var sql = 'select * from jobs'
			if (filters.status) {
				sql += ' where status = $status'
				params['$status'] = filters.status
			} else if (filters.exclusion_status) {
				sql += ' where status != $' + filter.exclusion_status[0]
				params['$' + filter.exclusion_status[0]] = filter.exclusion_status[0]
				if (filter.exclusion_status.length > 1) {
					var otherExclusions = filter.exclusion_status.slice(1)
					for (var key in otherExclusions) {
						sql += ' or status != $' + otherExclusions[key]
						params['$' + otherExclusions[key]] = otherExclusions[key]
					}
				}
			}
			if (!filters.page) {
				filters.page = 1
			}
			if (!filters.per_page) {
				filters.per_page = 20
			}
			sql += ' order by added_at asc'
			sql += ' limit ' + (filters.per_page * (filters.page-1)) + ',' + filters.per_page
			jobs_obj._db.all(sql, params,
				function(err, rows) {
					if (err) {
						logger.error('Jobs.getJobs: ' + err)
					} else {
						for (var key in rows) {
							if (typeof(rows[key]) != 'function') {
								rows[key].urls = JSON.parse(rows[key].urls)
								rows[key].stats = JSON.parse(rows[key].stats)
							}
						}
					}
					next(err, rows)
				}
			)
		}
	})
}

Jobs.prototype.getJob = function(job_id, next) {
	this.open(function (err, jobs_obj) {
		if (err) {
			logger.error('Jobs.getJob: [' + job_id + '] ' + err)
			next(err)
		} else {
			jobs_obj._db.get(
				'select * from jobs where id = ?',
				[job_id],
				function(err, row) {
					if (err) {
						logger.error('Jobs.getJob: [' + job_id + '] ' + err)
					} else if (!row) {
						err = 'Job not found'
						logger.error('Jobs.getJob: [' + job_id + '] ' + err)
					} else {
						row.urls = JSON.parse(row.urls)
						row.stats = JSON.parse(row.stats)
					}

					next(err, row)
				}
			)
		}
	})
}

Jobs.prototype.updateJob = function(job_id, data, filters, next) {
	this.open(function (err, jobs_obj) {
		if (err) {
			logger.error('Jobs.updateJob: [' + job_id + '] ' + err)
			next(err)
		} else {
			var sql_data = {}
			var sql = 'update jobs set '
			for (var key in data) {
				if (typeof(data[key]) == 'function') {
					continue
				}

				sql += key + ' = ' + '$' + key + ', '
				if (data[key] instanceof Object || data[key] instanceof Array) {
					sql_data['$' + key] = JSON.stringify(data[key])
				} else {
					sql_data['$' + key] = data[key]
			}
			}
			sql = sql.substring(0, sql.length - 2)
			sql += ' where id = $job_id'
			sql_data['$job_id'] = job_id

			if (filters.lock) {
				sql += ' and lock = $lock'
				sql_data['$lock'] = filters.lock
			}
			if (filters.status) {
				sql += ' and status = $status'
				sql_data['$status'] = filters.status
			}

			jobs_obj._db.run(sql, sql_data, function(err) {
				if (err) {
					next(err)
				} else {
					jobs_obj.getJob(job_id, next)
				}
			})
		}
	})
}

Jobs.prototype.lockJob = function(next) {
	this.open(function (err, jobs_obj) {
		if (err) {
			logger.error('Jobs.lockJob: ' + err)
			next(err)
		} else {
			var sql_data = {}
			var sql = 'update jobs set lock = $lock and status = $status order by added_at asc limit 1'
			sql_data['$lock'] = os.hostname + '-' + process.pid
			sql_data['$status'] = 'delivered'

			jobs_obj._db.run(sql, sql_data, function(err) {
				if (err) {
					next(err)
				} else {
					next(null)
				}
			})
		}
	})
}

Jobs.prototype.getLockedJob = function(next) {
	this.open(function (err, jobs_obj) {
		if (err) {
			logger.error('Jobs.getLockedJob: ' + err)
			next(err)
		} else {
			var sql_data = {}
			var sql = 'select * from jobs where lock = $lock and status = $status order by added_at asc limit 1'
			sql_data['$lock'] = os.hostname + '-' + process.pid
			sql_data['$status'] = 'delivered'

			jobs_obj._db.run(sql, sql_data, function(err, row) {
				if (err) {
					next(err)
				} else {
					next(err, row)
				}
			})
		}
	})
}

Jobs.prototype.log = function(job_id, level, content, next) {
	if (!levels[level]) {
		next('Unknown log level: ' + level)
	} else {
		this.open(function (err, jobs_obj) {
			if (err) {
				logger.error('Jobs.log: [' + job_id + '] ' + err)
				if (next) {
					next(err)
				}
			} else {
				jobs_obj._db.run(
					'insert into logs (job_id, timestamp, level, content) values (?, ?, ?, ?)',
					[job_id, utils.getCurrentTimestampMs(), level, content],
					function() {
						if (err) {
							logger.error('Jobs.log: [' + job_id + '] ' + err)
						}
						if (next) {
							next(err)
						}
					}
				)
			}
		})
	}
}

Jobs.prototype.getJobLog = function(job_id, level, next) {
	if (!levels[level]) {
		next('Unknown log level: ' + level)
	} else {
		this.open(function (err, jobs_obj) {
			if (err) {
				logger.error('Jobs.getJobLog: ' + err)
				next(err)
			} else {
				var sql = 'select timestamp, level, content from logs where job_id = ? and level in ("' + levels[level].join('", "') + '") order by timestamp asc'
				jobs_obj._db.all(sql, [job_id],
					function(err, rows) {
						var retval = ''
						if (err) {
							logger.error('Jobs.getJobLog: ' + err)
						} else {
							for (var key in rows) {
								if (typeof(rows[key]) != 'function') {
									var ts = new Date(rows[key].timestamp)
									var pad2 = function(v) { return (v < 10 ? '0' + v : v) }
									var pad3 = function(v) { return (v < 10 ? '00' + v : (v < 100 ? '0' + v : v)) }
									retval += ts.getFullYear() + '-' + pad2(ts.getMonth()) + '-' + pad2(ts.getDate()) + ' '
									retval += pad2(ts.getHours()) + ':' + pad2(ts.getMinutes()) + ':' + pad2(ts.getSeconds()) + '.' + pad3(ts.getMilliseconds()) + ' '
									retval += '[' + rows[key].level + '] '
									retval += rows[key].content + "\n"
								}
							}
						}
						next(err, retval)
					}
				)
			}
		})
	}
}

module.exports = Jobs
