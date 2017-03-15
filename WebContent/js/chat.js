/**
 * 
 */
var client;
function Handler(message){
	var result = message.split(" ");
	var status = result[1];
	var theCommand = result[0];
	if (status == "OK") {
		switch (theCommand) {
		case "BROADCAST":
			var message;
			message = result2String(result);
			$("#chatArea").append(message + "<br>");
			$("#chatArea").scrollTop($("#chatArea").prop("scrollHeight"));
			break;
		case "LOGIN":
			var message;
			message = result2String(result);
			$("#chatArea").append(message + "<br>");
			break;
		case "JOIN":
			$("#chatArea").empty();
			$("#chatInputBox").attr("disabled", false);
			$("#leaveBtn").show();
			$("#pauseBtn").show();
			$("#createBtn").hide();
			client = Stomp.client("ws://localhost:61614");
			client.connect("admin","password", function(frame){
				client.subscribe("/"+result[2]+"/"+result[3], function(message) {
					$("#chatArea").append(message.body + "<br>");
		        });
			});
			break;
		case "LEAVE":
			$("#chatArea").empty();
			$("#chatInputBox").attr("disabled", true);
			$("#destroyBtn").hide();
			$("#leaveBtn").hide();
			$("#pauseBtn").hide();
			$("#createBtn").show();
			var message;
			message = result2String(result);
			$("#chatArea").append(message + "<br>");
			break;
		case "CREATE":
			$("#destroyBtn").show();
			$("#destroyBtn").onclick = function(){ roomDestroy(result[2]); };
			break;
		case "GETUSER":
			$("#userList2").empty();
			var message;
			message = result2String(result);
			$('#userList2').append(message);
			break;
		case "SETTITLE": // Written By ESP
			var message;
			message = result2String(result);
			$("#currentRoom").empty();
			$("#currentRoom").append(message);
			$("#currentRoom").show();
			// End ESP
			break;
		case "DESTROY":
			alert("This room is desroyed.");
			break;
		case "PAUSE":
			client.disconnect(function() {

	        });
			break;
		}
	} else if (status == "FAIL") {
		switch (theCommand) {
		case "LOGIN":
			var message;
			message = result2String(result);
			alert(message);
			window.location.replace('login.html');
			break;
		case "JOIN":
			alert("Room Join Failed, Room does not exist or full.");
			$("#chatArea").empty();
			$("#chatInputBox").attr("disabled", true);
			$("#leaveBtn").hide();
			$("#pauseBtn").hide();
			$("#createBtn").show();
			var message;
			message = result2String(result);
			$("#chatArea").append(message + "<br>");
			break;
		case "USERKICK":
			alert("User kick failed.");
			break;
		}
	}
	return console.log(message);
}
function result2String(result){
	var i, len, message;
	len = result.length;
	msg = "";
	for (i = 2; i < len; i++) {
		msg += result[i] + " ";
	}
	return msg;
}
function roomJoin(no) {
	ws.send("JOIN " + no);
}
function roomLeave() {
	ws.send("LEAVE");
}
function roomPause() {
	ws.send("PAUSE");
}
function chatEvent(e) {
	if (e.keyCode == 13) {
		var chatVal = $("#chatInputBox").val();
		ws.send("BROADCAST " + chatVal);
		$("#chatInputBox").val("");
		return false;
	} else {
		return true;
	}
}
function createRoom(size, title) {
	ws.send("CREATE " + title + " " + size);
}

function createPop() {/* Written By Ten */
	window.open('createRoom.jsp',
				'',
				'left=200, top=200, width=500, height=320, scrollbars=n0, status=no, resizeable=no, fullscreen=no, channelmode=no');
}
function roomDestroy(no){
	ws.send("DESTROY "+no);
}

function userKick(user){
	var r = confirm("Do you want kick user [ "+user+" ]?");
	if(r == true)
		ws.send("USERKICK " + user);
}