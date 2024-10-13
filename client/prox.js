"use strict";

let net = require('net');
let fs = require('fs');
let exec = require('child_process').exec;
let execSync = require('child_process').execSync;

if (process.argv.length < 5) {
  log("usage: node prox.js <ping:listener> <ping:proxy> <forwarder:proxy> [reverse base port] [max reverse ports]");
  process.exit(1);
}

const [la, lp] = process.argv[2].split(':');
const [pa, pp] = process.argv[3].split(':');
const [ra, rp] = process.argv[4].split(':');

let log = process.env["DEBUG"] == "1" ? console.log : () => {};

const bp = process.argv.length > 5 ? parseInt(process.argv[5]) : 4450;
const mp = process.argv.length > 6 ? parseInt(process.argv[6]) : 20;

let keyqueue = [];
let splitcount = 0;
let splitqueue = [];
 
let proxy = net.createServer(function (socket) {
  log("[proxy] client connected");
  const keypkg = keyqueue.shift();
  if (keypkg === undefined) {
    try {
      socket.destroy();
    } catch (e) { }
    return;
  }
  const [key, pkg] = keypkg;

  let remoteSocket = new net.Socket();
  let any_connected = false;

  remoteSocket.on('error', function(err) {
    if (err.code == "ECONNREFUSED") {
      log("[proxy] could not connect to remote host");
    }
    try {
      socket.destroy();
    } catch (e) { }
  });

  remoteSocket.connect(parseInt(rp), ra, function () {
    let me_connected = false;

    setTimeout(() => {
      if (!me_connected) {
        log("[proxy] this connection is not connected, terminating");
        socket.destroy();
        remoteSocket.destroy();
      }
    }, 500);

    const index = keyqueue.indexOf(keypkg);
    if (index > -1) {
      keyqueue.splice(index, 1);
    }

    log("[proxy] connected to remote server");
    remoteSocket.write(key);

    socket.on('data', function (data) {
      remoteSocket.write(data);
    });

    socket.on('close', function() {
      log("[proxy] client connection closed");
      try {
        remoteSocket.destroy();
      } catch (e) { }
    });

    remoteSocket.on("data", function (data) {
      //log("[proxy] data from remote server");
      me_connected = true;
      any_connected = true;
      socket.write(data);
    });

    remoteSocket.on('close', function() {
      try {
        socket.destroy();
      } catch (e) { }
      if (!any_connected) {
        log("[proxy] remote connection closed (before data arrived for any connections), pinging self");
        setTimeout(() => {
          keyqueue.push(keypkg);

          //do a fake ping
          const buflen = 32 + 4 + pkg.length;
          let buf = Buffer.alloc(buflen);
          let off = 0;
          off += key.copy(buf); // buf.copy(key);
          off = buf.writeUInt32BE(pkg.length, off);
          off += buf.write(pkg, off);

          let remotePingSocket = new net.Socket();
          remotePingSocket.on('error', function(err) {
            if (err.code == "ECONNREFUSED") {
              log("[proxy] could not connect to local ping listener");
            }
          });

          remotePingSocket.connect(lp, la, function () {
            log("[proxy] connected to local ping listener");
            remotePingSocket.write(buf);
            remotePingSocket.destroy();
          });

          remotePingSocket.on('close', function() {
            log("[proxy] connection to local ping listener closed");
          });
        }, 1000);
      } else {
        log("[proxy] remote closed connection");
      }
    });
  });

});

proxy.on('error', function(err) {
  log(err);
});

proxy.timeout = 0; 

const pkgre = /^[a-zA-Z0-9._-]+$/;
let pinglistener = net.createServer(function (socket) {
  log("[pinglistener] connection received");
  let keypkg = Buffer.from([]);
  let pkglen = 0;
  socket.on('data', function(data) {
    keypkg = Buffer.concat([keypkg, data]);
    if (keypkg.length < (32 + 4 + pkglen)) {
      return;
    }
    pkglen = keypkg.readUInt32BE(32);
    if (pkglen == 0 || pkglen > 256) {
      log("[pinglistener] invalid pkg length: " + pkglen);
      try {
        socket.destroy();
      } catch (e) { }
      return;
    }
    if (keypkg.length < (32 + 4 + pkglen)) {
      return;
    }
    try {
      socket.destroy();
    } catch (e) { }

    let key = keypkg.slice(0,32);
    let pkg = keypkg.slice(36, 36+pkglen).toString();
    if (pkg.match(pkgre) == null) {
      log("[pinglistener] invalid pkg name: " + pkg);
      return;
    }

    keyqueue.push([key, pkg]);
    keyqueue.push([key, pkg]); //it makes another connection that also needs the proxy, otherwise it fails w/ an undefined method 'cleanup' error
    let port = 0;
    let i;
    for (i = bp; i < bp + mp; i++) {
      if (!fs.existsSync(".ps" + i)) {
        port = i;
        execSync("touch .ps" + port);
        break;
      }
    }
    if (port == 0) {
      log("[pinglistner] Port range exhausted. Not starting pray-remote.");
      return;
    }

    log("[pinglistener] starting pray-remote with port " + port);

    exec("adb -d reverse tcp:" + port + " tcp:" + port, function() {
      let cmd = "tmux new-window -t paraspectre -n '" + pkg + "' 'pray-remote -s 127.0.0.1 -p 4443 -b 127.0.0.1:" + port + " -z tcp -k " + key.toString() + "; adb -d reverse --remove tcp:" + port + "; rm .ps" + port + "'";
      //log(cmd);
      exec(cmd);
    });
  });

  socket.on('error', function(err) {
    log(err);
  });
});

pinglistener.on('error', function(err) {
  log(err);
});

proxy.listen(pp, pa, function() {
  log("[proxy] listening")
});

pinglistener.timeout = 0; 
pinglistener.listen(lp, la, function() {
  log("[ping listener] listening")
});

