@(username: String)

$(function() {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS("@routes.PrivateChatsController.chat().webSocketURL(request)")

    var restartRequestTemplate =
        '<div class="row center-text">' +
            '<div class="col-xs-12"><p>¿Quieres empezar una nueva ronda de turnos?</p></div>' +
            '<div class="col-xs-6"><button class="btn btn-primary restartButton">Si</button></div>' +
            '<div class="col-xs-6"><button class="btn btn-primary noRestartButton">No</button></div>'
        '</div>';
    var pointsTemplate =
        '<div>' +
            '<p><h3>¡Otorga puntos por la ayuda recibida!</h3></p>' +
            '<input type="number" name="quantity" id="repPointsInput" min="1" max="5">' +
            '<button class="btn btn-primary sendRepPoints">Enviar!</button>' +
        '</div>';


    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        // Handle errors
        if(data.error) {
            chatSocket.close()
            $("#onError span").text(data.error)
            $("#onError").show()
            return
        } else {
            $("#onChat").show()
        }
        var type = data.type;
        //si es un mensaje de charla
        if(type == "talk"){
            createMessage(data);
        }else if(type == "control"){
            var kind = data.kind;
            if(kind == "minute_notify"){
                $("#timerNumberId").text(data.info);
            }else if(kind == "turn_notify"){
                changeTurn(data);
                data.info = "¡Cambio de turno!";
                createMessage(data);
            }else if(kind == "round_notify"){
                createMessage(data);
                insertRestartForm();
            }else if(kind == "fin_nack"){
                data.info = "El Chat empieza de nuevo";
                createMessage(data);
            }else if(kind == "fin_ack"){
                createMessage(data);
                insertReputationForm();
            }
        }
    }

    function changeTurn(data){
        $("#userTurn").text(data.info);
    }

    function createMessage(data){
        // Create the message element
        var el = $('<div class="message"><span></span><p></p></div>')
        $("span", el).text(data.user)
        $("p", el).text(data.info)
        $(el).addClass(data.kind)
        if(data.user == '@username') $(el).addClass('me')
        $('#messages').append(el)

        // Update the members list
        /*$("#members").html('')
        $(data.members).each(function() {
            var li = document.createElement('li');
            li.textContent = this;
            $("#members").append(li);
        })*/
    }

    function insertRestartForm(){
        // Create the message element
        var el = restartRequestTemplate;
        $('#messages').append(el);
        $(".restartButton").click(function(){
            sendRestartRequest();
        });
        $(".noRestartButton").click(function(){
            sendFinRequest();
        });
    }

    function insertReputationForm(){
        var el = pointsTemplate;
        var points = 0;
        $('#messages').append(el);
        $(".sendRepPoints").click(function(){
            points = $("#repPointsInput").val();
            console.log("Puntos enviados: " + points);
            sendReputationPoints(points);
        });
    }

    var sendMessage = function() {
        chatSocket.send(JSON.stringify(
            {type:"talk", kind: "text", info: $("#talk").val()}
        ))
        $("#talk").val('')
    }

    var sendRestartRequest = function() {
        chatSocket.send(JSON.stringify(
            {type:"control", kind: "restart_request"}
        ))
    }

    var sendFinRequest = function() {
        chatSocket.send(JSON.stringify(
            {type:"control", kind: "fin_request"}
        ))
    }

    var sendReputationPoints = function(num){
        chatSocket.send(JSON.stringify(
            {type:"control", kind: "reputation_points", info: "" + num}//da problemas por ser un numero??
        ))
    }

    var handleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            e.preventDefault()
            sendMessage()
        }
    }

    $("#talk").keypress(handleReturnKey)

    chatSocket.onmessage = receiveEvent
})
