<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
	<!--
		Written By Ten.
	  -->
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script src="http://code.jquery.com/jquery-2.1.3.js"></script>
<link rel="shortcut icon" href="css/pi.ico"/>
<title>Room Create</title>
<link rel="stylesheet" type="text/css" href="css/ten.css">
<script type="text/javascript">
	$(document).ready(function() {
		
	});
	
	function roomEvent(){
		var size = $('#size').val();
		var title = $('#title').val();
		if(size < 2 && title == '')
			return;
		opener.parent.createRoom(size,title);
		self.close();
	}
</script>
</head>
<body>
	<div id="roomInfo" align="center">
	<div align="center">
		<img src="css/pica.gif" width="200px"/>
	</div>
	<br>
		<table>
			<tr>
				<th>제한 인원</th>
				<td><input type="number" id="size" min="2"/></td>
			</tr>
			<tr>
				<th>채팅방 이름</th>
				<td><input type="text" id="title" /></td>
			</tr>
			<tr>
				<td colspan="2" align="center" valign="bottom"><button id="buttonComplete" onclick="roomEvent()" >확인</button> </td>
			</tr>
		</table>
	</div>
</body>
</html>