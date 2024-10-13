var net = require('net');

var LOCAL_ADDR = process.argv[2];
var LOCAL_PORT = process.argv[3];
var REMOTE_ADDR = process.argv[4];
var REMOTE_PORT = process.argv[5];

if (process.argv.length < 6) {
  console.log("usage: node prox.js <local bind> <local port> <remote ip> <remote port>");
  process.exit(1);
}
 
var server = net.createServer(function (socket) {
    console.log('client connected!');
    var remoteSocket = new net.Socket();

    remoteSocket.connect(parseInt(REMOTE_PORT), REMOTE_ADDR, function () {
      console.log('connected to remote!');
    });

    socket.on('data', function (data) {
      console.log('client >> proxy >> remote');
      console.log(data);

      data = data.toString().replace("\n", "\r\n")
      remoteSocket.write(data);
    });

    socket.on('close', function() {
      try {
        remoteSocket.destroy();
      } catch (e) { }
    });

    remoteSocket.on("data", function (data) {
      console.log('client << proxy << remote');//, data.toString());
      console.log(data);
      socket.write(data.toString().replace("\r", "\r\n"));
      //socket.write('')
    });

    remoteSocket.on('close', function() {
      try {
        socket.destroy();
      } catch (e) { }
    });

    remoteSocket.on('error', function(err) {
      console.log(err);
    });

});

server.on('error', function(err) {
  console.log(err);
});

server.timeout = 0; 
server.listen(LOCAL_PORT, LOCAL_ADDR);
console.log("listening on " + LOCAL_ADDR + ":" + LOCAL_PORT);
