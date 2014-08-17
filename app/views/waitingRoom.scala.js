$('document').ready(function(){

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;

    var chatSocket = new WS('@routes.ChatRoomController.socketRoom().webSocketURL(request)');


    // Cuando la conexion este abierta, ya puede empezar a enviar y recibir
    chatSocket.onopen = function(){

         chatSocket.send(JSON.stringify({'type':'getusers'}));

         chatSocket.onmessage = function(event){

             console.log('mensaje recibido');
             console.log(event.data);
             var list = $('#userConnectedList li');
             /*jQuery.each(JSON.parse(event.data),function(index,element){

                list.after("<Strong>ESTO ES UNA PRUEBA</strong>");

             });*/

         }

    };

    console.log(chatSocket.readyState);
})
