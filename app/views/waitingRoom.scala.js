$('document').ready(function(){

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;

    var chatSocket = new WS('@routes.ChatRoomController.socketRoom().webSocketURL(request)');

    // Cuando la conexion este abierta, ya puede empezar a enviar y recibir
    chatSocket.onopen = function(){

         chatSocket.send(JSON.stringify({'type':'getusers'}));

         chatSocket.onmessage = function(event){

             console.log('mensaje recibido');
             console.log(event.data);
             var message = JSON.parse(event.data);

            // Elimino un usuario que se desconecta
            if(message.type === 'close'){
                console.log(message.id);
                //$('#' + message.id).off();
                $('#' + message.id).remove();
            }

            // Pongo un usuario que se ha conectado
            if(message.type === 'open'){

                // Le pongo el html
                $('#userConnectedList').append(message.html);

                // Quito los eventos que tengan las listas y se lo pongo otra vez

                eventGetInfoUser();
            }

            if(message.type === 'infoUser'){

                console.log(message);
                $('.profile').html(message.html);

            }
         }

        // funcion que ejecuta las funciones principales
        init();


    };


    function eventGetInfoUser(){

        $('#userConnectedList li').off();

        $('#userConnectedList li').on('click',function(event,selector){

            alert('funciona el evento');

            var div = $(event)[0].target;
            var id = $(div).closest('li').attr('id');

            console.log(id);

            chatSocket.send(JSON.stringify({'type':'getInfoUser','username':id}));

        });

    }

    function init(){
        // Evento al li de todos los usuarios
        eventGetInfoUser();

    }
    console.log(chatSocket);
});
