module.exports = {
	sqlite_filename: './db.sqlite',
	http_port: 8888,
	gnip: {
		use_ssl: true,
		domain: 'historical.gnip.com',
		port: 443,
		account_name: 'datasift'
	},
	zookeeper: {
		socket: 'localhost:2181',
		clientId: 'historics-kafka-client'
	}
}
