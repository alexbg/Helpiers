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

                if(message.type === 'infoUser'){

                    $('#infouser').html(message.html);

                }

                if(message.type === 'chatRequest'){


                    $('#modal-user-name').attr('name',message.username);
                    $('#modal-user-name').html(message.username);

                    $("#modalRequest").modal('toggle');

                }

                if(message.type === 'rejectinvitation'){

                    $('#requestResponse').html('Denegado').attr('class','bg-danger');

                    //$('#modalInfo').modal('toggle');

                }

                if(message.type === 'acceptinvitation'){

                    $('#requestResponse').html('Aceptado, redirigiendo al chat...').attr('class','bg-success');

                }

                if(message.type === 'cancelInvitation'){

                    $("#modalRequest").modal('toggle');
                    alert('han cancelado la invitacion');

                }

                if(message.type === 'error'){

                    $("#modalInfo").modal('toggle');
                    alert(message.message);

                }
            }

            chatSocket.onclose = function(){



            }

        // funcion que ejecuta las funciones principales
        init();


    };

    // Pone el evento para obtener informacion del usuario

    function eventGetInfoUser(id){

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

    function invite(){

        //$('#invite').off();
        $('#invite').on('click',function(event){
            if($('#infoUser').attr('name') !== ''){
                $('#modalInfo').modal('toggle');
                // obtengo el username del usuario al que se va a invitar y envio la invitacion
                chatSocket.send(JSON.stringify({'type':'chatRequest','username':$('#infoUser').attr('name')}));
            }

        });

    }

    function buttonsModalRequest(){

        $('#accept').on('click',function(event){

            chatSocket.send(JSON.stringify({'type':'acceptInvitation'}));

        });

        $('#reject').on('click',function(event){
            chatSocket.send(JSON.stringify({'type':'rejectInvitation'}));
        });

        $('#cancel').on('click',function(event){

            chatSocket.send(JSON.stringify({'type':'cancelInvitation'}));

        });

    }

    function eventsModals(){

        $('#modalInfo').on('hide.bs.modal',function(event){

            $('#requestResponse').attr('class','hidden');

        });

    }

    function init(){
        // Evento al li de todos los usuarios
        eventGetInfoUser('undefined');
        invite();
        buttonsModalRequest();
        eventsModals();
    }

});
