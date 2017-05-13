var WebSocket = require('ws');
var WebSocketServer = WebSocket.Server;

var wss = new WebSocketServer({port: 3434});

wss.broadcast = function(data) {
    this.clients.forEach(function(client) {
        if(client.readyState === WebSocket.OPEN) {
            client.send(data);
        }
    });
};

wss.on('connection', function(ws) {
    console.log("NEW CLIENT");

    ws.on('message', function(message) {
        console.log('mreceived: %s', message);
        wss.broadcast(message);
    });
});

console.log("running")
