// const WebSocket = require('ws');

// const wss = new WebSocket.Server({ port: 8080 });

// wss.on('connection', function connection(ws) {
//   ws.on('message', function incoming(message) {
//     console.log('received: %s', message);
//   });

//   ws.send('something');
// });

var ws = require("nodejs-websocket")

// Scream server example: "hi" -> "HI!!!"
var server = ws.createServer(function(conn) {
	console.log("New connection")
	conn.on("text", function(str) {
		console.log("Received " + str)
		conn.sendText(str.toUpperCase() + "!!!")
	})
	conn.on("close", function(code, reason) {
		console.log("Connection closed")
	})
}).listen(8001)