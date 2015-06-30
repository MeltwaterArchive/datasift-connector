var http = require('http')
    , port = process.argv[2] || 5002
    , level = process.argv[3] || 'ERROR'
    , mode = process.argv[4] || 'BULK';

var server = http.createServer(function(req, res) {
  if(mode === 'STREAM') {
    console.log('Connection received from %s', req.connection.remoteAddress);
  }

  req.on('data', function(chunk) {
    if(level === 'DEBUG') {
      process.stdout.write(chunk.toString());
    } else if (level === 'INFO' && mode === 'STREAM') {
      process.stdout.write('.');
    }
  });

  req.on('end', function() {
    process.stdout.write('.');
    res.writeHead(200, "OK", {'Content-Type': 'text/html'});
    res.end();
  });

});

console.log('Listening on port %d', port);

server.listen(port);
