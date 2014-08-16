$('document').ready(function(){

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;

    var chatSocket = new WS('@routes.ChatRoomController.socketRoom().webSocketURL(request)');


    // Cuando la conexion este abierta, ya puede empezar a enviar y recibir
    chatSocket.onopen = function(){

         chatSocket.send(JSON.stringify({'type':'getusers'}));

    };

    console.log(chatSocket.readyState);
})
