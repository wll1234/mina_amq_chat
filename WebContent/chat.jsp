<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%
	String address = request.getParameter("address");
	String username = request.getParameter("username");
%>
<script type="text/javascript"src="http://code.jquery.com/jquery-2.1.3.js"></script>
<script src="js/stomp.js"></script>
<script type="text/javascript" src="js/chat.js"></script>
<link rel="stylesheet" type="text/css" href="css/main.css">
<link rel="shortcut icon" href="css/pi.ico"/>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Mina-Websocket Chat Client</title>
<script type="text/javascript">
var ws = new WebSocket("ws://<%=address%>");
ws.onopen = function() {
	ws.send("LOGIN <%=username%>");
};
window.onbeforeunload = function() {
	ws.send("QUIT");
	ws.close();
};
ws.onmessage = function(event) {
	var reader;
	if (event.data instanceof Blob) {
		reader = new FileReader();
		reader.onload = function() {
			var message = reader.result;
			Handler(message);
		};
		reader.readAsText(event.data);
	}
};
ws.onclose = function() {
	alert("Connection is closed...");
	window.location.replace('login.html');
};

</script>
</head>
<body>
	<div id="chatWrapper">
		<div id="chatArea"></div>
		<div id="buttonList">
			<!-- <button class="Buttons" >Join</button> -->
			<button id="createBtn" class="Buttons" onclick="createPop()">Create</button>
			<button id="leaveBtn" class="Buttons" style="display: none;"
				onclick="roomLeave()">Leave</button>
				<button id="pauseBtn" class="Buttons" style="display: none;"
				onclick="roomPause()">Pause</button>
			<button id="destroyBtn" class="Buttons" style="display: none;" onclick="roomDestroy()">Destroy</button>
		</div>
		<div id="userList" align="center">
			<div id="currentRoom"></div> <!-- Written By ESP -->
			:::접속자 목록:::<br>
			<div id="userList2"></div>
		</div>
		<form id="chatInput">
			<input id="chatInputBox" type="text"
				onkeypress="return chatEvent(event)" disabled></input>
		</form>
	</div>
</body>
</html>