$('document').ready(function(){

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;

    //var socket = io('@routes.ChatRoomController.socketRoom().webSocketURL(request)');
    var chatSocket = new WS('@routes.ChatRoomController.socketRoom().webSocketURL(request)');

    // Cuando la conexion este abierta, ya puede empezar a enviar y recibir
    chatSocket.onopen = function(){

            //chatSocket.send(JSON.stringify({'type':'getusers'}));

            chatSocket.onmessage = function(event){

                console.log('mensaje recibido');


                var message = JSON.parse(event.data);
                console.log(message);
                // Elimino un usuario que se desconecta
                if(message.type === 'close'){

                    $('#' + message.id).off();
                    $('#' + message.id).remove();
                }

                // Pongo un usuario que se ha conectado
                if(message.type === 'open'){

                    // Le pongo el html
                    $('#userConnectedList').append(message.html);



                    // Quito los eventos que tengan las listas y se lo pongo otra vez

                    eventGetInfoUser(message.id);
                }
                // Recibe la informacion del usuario solicitado
                if(message.type === 'infoUser'){

                    $('#infouser').html(message.html);

                }
                // Recibe la peticion de iniciar una conversacion
                if(message.type === 'chatRequest'){


                    $('#modal-user-name').attr('name',message.username);
                    $('#modal-user-name').html(message.username);

                    $("#modalRequest").modal('toggle');

                }
                // Recibe el rechazo de una invitacion que se haya hecho anteriormente
                if(message.type === 'rejectinvitation'){

                    $('#requestResponse').html('Denegado').attr('class','bg-danger');

                    //$('#modalInfo').modal('toggle');

                }
                // recibe la aceptacion de la invitacion
                if(message.type === 'acceptinvitation'){

                    $('#requestResponse').html('Aceptado, redirigiendo al chat...').attr('class','bg-success');
                    Window.location.assign('@routes.ChatRoomController.showChatView()');

                }
                // Recibe el mensaje informando de que se ha cancelado la peticion
                if(message.type === 'cancelInvitation'){

                    $("#modalRequest").modal('toggle');
                    alert('han cancelado la invitacion');

                }
                // SI hay algun error, se le informa al usuario
                if(message.type === 'error'){

                    $("#modalInfo").modal('toggle');
                    alert(message.message);

                }
            }
            // Se ejecuta cuando se cierra la conexion con el websocket del servidor
            chatSocket.onclose = function(){



            }

        // funcion que ejecuta las funciones principales
        init();


    };

    // Pone el evento para obtener informacion del usuario

    function eventGetInfoUser(id){
        // Si no se le indica un id concreto, el evento se le pondra a todos los usuarios
        // de la lista. EN caso contrario, solo seria a ese usuario concreto
        if(id === 'undefined'){

            $('#userConnectedList li').on('click',function(event,selector){

                var div = $(event)[0].target;
                var id = $(div).closest('li').attr('id');

                chatSocket.send(JSON.stringify({'type':'getInfoUser','username':id}));

            });
        }
        else{

            $('#'+id).on('click',function(event,selector){

                chatSocket.send(JSON.stringify({'type':'getInfoUser','username':id}));

            });

        }

    }
    // Pone el evento para invitar auna persona con el boton invitar
    // Muestra el modal indicando la espera de la respuesta
    function invite(){

        $('#invite').on('click',function(event){
            if($('#infoUser').attr('name')){
                $('#modalInfo').modal('toggle');
                // obtengo el username del usuario al que se va a invitar y envio la invitacion
                chatSocket.send(JSON.stringify({'type':'chatRequest','username':$('#infoUser').attr('name')}));
            }

        });

    }
    // Pone los eventos a los botones del modal para aceptar, rechazar o cancelar la peticion de chat
    function buttonsModalRequest(){

        $('#accept').on('click',function(event){

            chatSocket.send(JSON.stringify({'type':'acceptInvitation'}));
            Window.location.assign("room");

        });

        $('#reject').on('click',function(event){
            chatSocket.send(JSON.stringify({'type':'rejectInvitation'}));
        });

        $('#cancel').on('click',function(event){

            chatSocket.send(JSON.stringify({'type':'cancelInvitation'}));

        });

    }
    // Se declaran las configuraciones de los eventos de los modal
    function eventsModals(){
        // Cuando se esconde el modal, cambia la clase del elemento con el id requestResponse
        // que se encarga de mostrar si la peticion a sido aceptada o rechazada
        $('#modalInfo').on('hide.bs.modal',function(event){

            $('#requestResponse').attr('class','hidden');

        });

    }
    // Ejecuta las funciones principales
    function init(){
        // Evento al li de todos los usuarios
        eventGetInfoUser('undefined');
        invite();
        buttonsModalRequest();
        eventsModals();
    }

});
