console.log('weeee');
var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;

var chatSocket = new WS('@routes.ChatRoomController.socketRoom().webSocketURL(request)')
console.log(chatSocket);
// esto es uncomentario