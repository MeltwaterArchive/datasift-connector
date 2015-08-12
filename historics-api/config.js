module.exports = {
	sqlite_filename: './db.sqlite',
	http_port: 8888,
	gnip: {
		use_ssl: false,
		domain: 'localhost',
		port: 5001,
		account_name: 'accepted' // for testing with faker
	},
	zookeeper: {
		socket: 'localhost:2181',
		clientId: 'historics-kafka-client'
	}
}
