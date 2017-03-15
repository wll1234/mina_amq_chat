/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package mina.chat;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.logging.MdcInjectionFilter;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mina.chat.filter.WebSocketCodecPacket;

/**
 * {@link IoHandler} implementation of a simple chat server protocol.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ChatProtocolHandler extends IoHandlerAdapter {
	private final static Logger LOGGER = LoggerFactory.getLogger(ChatProtocolHandler.class);
	private static final CharsetEncoder ENCODER = Charset.forName("UTF-8").newEncoder();
	private int roomIdSeq = 0;
	
	private final Set<IoSession> sessions = Collections.synchronizedSet(new HashSet<IoSession>());
	private final Set<String> users = Collections.synchronizedSet(new HashSet<String>());
	private final Set<Room> rooms = Collections.synchronizedSet(new HashSet<Room>());
	
	static String user = "admin";
	static String password = "password";
	static String host = "localhost";
	static int port = 61613;

	public ChatProtocolHandler() {
		rooms.add(new Room());
	}
	

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		LOGGER.warn("Unexpected exception.", cause);
		session.closeNow();
	}
	@Override
	public void messageReceived(IoSession session, Object message) {
		Logger log = LoggerFactory.getLogger(ChatProtocolHandler.class);

		IoBuffer buffer = (IoBuffer) message;		// message -> buffer
		byte[] bt = buffer.array();					// buffer -> byte array
		String theMessage = new String(bt);			// byte array -> string
		String result[] = theMessage.split(" ", 2);	// string -> command + message
		String theCommand = result[0];				//             [0]       [1]
		
		log.info("result: " + theMessage);

		try {
			ChatCommand command = ChatCommand.valueOf(theCommand);
			String user = (String) session.getAttribute("user");

			switch (command.toInt()) {

			case ChatCommand.QUIT:
				refreshUsers("0");
				session.closeNow();
				break;
			case ChatCommand.LOGIN:
				if (user != null) {
					session.write(str2Packet("LOGIN FAIL user " + user + " already logged in."));
					return;
				}
				if (result.length == 2) {
					user = result[1];
				} else {
					session.write(str2Packet("LOGIN FAIL invalid login command."));
					return;
				}
				if (users.contains(user)) {
					session.write(str2Packet("LOGIN FAIL the name " + user + " is already used."));
					return;
				}

				sessions.add(session);

				session.setAttribute("user", user);
				MdcInjectionFilter.setProperty(session, "user", user);
				users.add(user);
				
				session.setAttribute("roomId", "0");
				MdcInjectionFilter.setProperty(session, "roomId", "0");
				
				getRoomByRoomId("0").addAttend(session);
				
				session.write(str2Packet("LOGIN OK " + printRooms()));
				session.write(str2Packet("SETTITLE OK LOBBY"));
				refreshUsers("0");
				break;
			case ChatCommand.BROADCAST:
				if (result.length == 2) {
					String roomId = session.getAttribute("roomId").toString();
					roomBroadcast(user + ": " + result[1], roomId);
				}
				break;
			case ChatCommand.JOIN: // Written By HJ & ESP
				Room targetRoom = getRoomByRoomId(result[1]);
				int roomSize = targetRoom.getSize();
				String crTitle = targetRoom.getTitle();
				
				if (result.length == 2 && getRoomSessionCnt(targetRoom) < roomSize) {
					getRoomByRoomId("0").removeAttend(session);
					targetRoom.addAttend(session);
					setRoomid(session, targetRoom.getRoomID()+"");
					session.write(str2Packet("JOIN OK " + targetRoom.getRoomID() + " " + session.getAttribute("user").toString()));
					if(targetRoom.getAdminUser().equals(session)){
						session.write(str2Packet("CREATE OK "+targetRoom.getRoomID()));
					}
					roomBroadcast("The user " + user + " has joined this Room.", result[1]);
					refreshUsers(result[1]);
					refreshUsers("0");
					refreshRoom();
				} else {
					session.write(str2Packet("JOIN FAIL " + printRooms()));
					crTitle = "LOBBY";
				}
				session.write(str2Packet("SETTITLE OK " + crTitle));
				break;
			case ChatCommand.CREATE:
				if (result.length == 2) {
					String[] roomArg = result[1].split(" ", 2);
					int rootSize = Integer.parseInt(roomArg[1]);
					String roomTitle = roomArg[0];
					
					Room room = new Room(session, rootSize, roomTitle, ++roomIdSeq);
					rooms.add(room);
					
					getRoomByRoomId("0").removeAttend(session);
					room.addAttend(session);
					setRoomid(session, room.getRoomID()+"");
					
					session.write(str2Packet("JOIN OK"));
					session.write(str2Packet("CREATE OK "+roomIdSeq));
					session.write(str2Packet("SETTITLE OK " + roomTitle));
					roomBroadcast("The user " + user + " has joined this Room.", String.valueOf(roomIdSeq));
					refreshUsers(String.valueOf(roomIdSeq));
					refreshUsers("0");
					refreshRoom();
				}
				break;
			case ChatCommand.LEAVE:
				String roomId = session.getAttribute("roomId").toString();
				getRoomByRoomId("0").addAttend(session);
				getRoomByRoomId(roomId).removeAttend(session);
				setRoomid(session, "0");
				roomBroadcast("The user " + user + " has left this Room.", roomId);
				refreshUsers(roomId);
				refreshUsers("0");
				refreshRoom();
				session.write(str2Packet("SETTITLE OK LOBBY"));
				break;
			case ChatCommand.DESTROY: // Written By ESP
				String id = session.getAttribute("roomId").toString();
				Room room = getRoomByRoomId(id);
				Set<IoSession> attends = room.getAttends();
				Set<IoSession> pauses = room.getPauses();
				Room roomZero = getRoomByRoomId("0");
				synchronized (attends) {
					for (IoSession sessionTmp : attends) {
						if (sessionTmp.isConnected()) {
							roomZero.addAttend(sessionTmp);
							setRoomid(sessionTmp, "0");
							sessionTmp.write(str2Packet("DESTROY OK"));
							sessionTmp.write(str2Packet("SETTITLE OK LOBBY"));
						}		
					}
				}
				attends.clear();
				pauses.clear();
				rooms.remove(room);
				refreshRoom();
				refreshUsers("0");
				break;
			case ChatCommand.USERKICK: // Written By Ten
				String userk = result[1];
				
				String roomk= session.getAttribute("roomId").toString();
				IoSession roomAdminSession = getRoomByRoomId(roomk).getAdminUser();
				if(session.equals(roomAdminSession) && !session.equals(getSessionByUserName(userk))){
					IoSession kickUser = getSessionByUserName(userk);
					setRoomid(kickUser, "0");
					getRoomByRoomId(roomk).getAttends().remove(kickUser);
					getRoomByRoomId("0").getAttends().add(kickUser);
					roomBroadcast("The user " + userk + " Kicked.", roomk);
					refreshUsers(roomk);
					refreshUsers("0");
					refreshRoom();
					kickUser.write(str2Packet("SETTITLE OK LOBBY"));
				}else{
					session.write(str2Packet("USERKICK FAIL"));
				}
				break;
			case ChatCommand.PAUSE:
				String pauseRoomId = session.getAttribute("roomId").toString();
				getRoomByRoomId("0").addAttend(session);
				getRoomByRoomId(pauseRoomId).addPauses(session);
				setRoomid(session, "0");
				roomBroadcast("The user " + user + " has paused this Room.", pauseRoomId);
				refreshUsers(pauseRoomId);
				refreshUsers("0");
				refreshRoom();
				session.write(str2Packet("SETTITLE OK LOBBY"));
				session.write(str2Packet("PAUSE OK"));
				break;
			default:
				LOGGER.info("Unhandled command: " + command);
				break;
			}

		} catch (IllegalArgumentException e) {
			LOGGER.debug("Illegal argument", e);
		}
	}
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		String user = (String) session.getAttribute("user");
		String roomId = session.getAttribute("roomId").toString();
		users.remove(user);
		sessions.remove(session);
		if(!roomId.equals("0")){
			roomBroadcast("The user " + user + " has left this Room.", roomId);
			refreshUsers(roomId);
		}
		refreshUsers("0");
		refreshRoom();
	}
	public void setRoomid(IoSession session, String roomId) {
		session.setAttribute("roomId", roomId);
		MdcInjectionFilter.setProperty(session, "roomId", roomId);
	}
	public void refreshRoom(){
		Set<IoSession> attends = getRoomByRoomId("0").getAttends();
		synchronized (attends) {
			String roomTable = printRooms();
			for (IoSession sessionTmp : attends) {
				if (sessionTmp.isConnected()) {
					sessionTmp.write(str2Packet("LEAVE OK " + roomTable));
				}
			}
		}
	}
	public WebSocketCodecPacket str2Packet(String str) {
		IoBuffer buf = IoBuffer.allocate(str.length()).setAutoExpand(true);

		try {
			buf.putString(str, ENCODER);
		} catch (CharacterCodingException e) {
			e.printStackTrace();
		}
		buf.flip();
		return WebSocketCodecPacket.buildPacket(buf);
	}
	public void roomBroadcast(String message, String roomId) {
		Set<IoSession> attends = getRoomByRoomId(roomId).getAttends();
		Set<IoSession> pauses = getRoomByRoomId(roomId).getPauses();
		synchronized (attends) {
			for (IoSession sessionTmp : attends) {
				if (sessionTmp.isConnected()) {
					sessionTmp.write(str2Packet("BROADCAST OK " + message));
				}
			}
		}
		synchronized (pauses) {
			for (IoSession sessionTmp : pauses) {
				if (sessionTmp.isConnected()) {
					enQueue(roomId, (String)sessionTmp.getAttribute("user"), message);
				}
			}
		}
	}
	public int getRoomSessionCnt(Room room) {
		return room.getAttends().size();
	}
	public String printRooms() {
		String roomTable = "<table id='roomList'>";
		synchronized (rooms) {
			for (Room roomTmp : rooms) {
				if (roomTmp.getRoomID() != 0) {
					roomTable += "<tr id='room" + roomTmp.getRoomID() + "' onclick='roomJoin(" + roomTmp.getRoomID()
							+ ")'>" + "<td class='roomTitle'>" + roomTmp.getTitle() + "</td>" + "<td class='roomSize'>"
							+ getRoomSessionCnt(roomTmp) + "/" + roomTmp.getSize() + "</td>" + "</tr>";
				}
			}
		}
		roomTable += "</table>";
		return roomTable;
	}
	public void refreshUsers(String roomId){
		String usrTable = "<table id='users'>";
		String usrTableAdmin = "<table id='users'>";
		Room targetRoom = getRoomByRoomId(roomId);
		IoSession adminSession = targetRoom.getAdminUser();
		Set<IoSession> attends = targetRoom.getAttends();
		synchronized (attends) {
			for (IoSession sessionTmp : attends) {
				usrTableAdmin += "<tr class='user' onclick=\"userKick(\'" + sessionTmp.getAttribute("user")
						+ "\')\"><td>" + sessionTmp.getAttribute("user") + "</td></tr>";
				usrTable += "<tr class='user'><td>" + sessionTmp.getAttribute("user") + "</td></tr>";
			}
		}
		usrTable += "</table>";
		usrTableAdmin += "</table>";
		synchronized (attends) {
			for (IoSession sessionTmp : attends) {
				if (sessionTmp.getAttribute("roomId").equals(roomId)) {
					if(sessionTmp.equals(adminSession))
						sessionTmp.write(str2Packet("GETUSER OK " + usrTableAdmin));
					else
						sessionTmp.write(str2Packet("GETUSER OK " + usrTable));
				}
			}
		}
	}
	public Room getRoomByRoomId(String roomId){
		synchronized (rooms) {
			for(Room roomTmp : rooms){
				if(roomId.equals(roomTmp.getRoomID()+"")){
					return roomTmp;
				}
			}
		}
		return null;
	}
	public IoSession getSessionByUserName(String userName){
		synchronized (sessions) {
			for (IoSession sessionTmp : sessions) {
				if ( sessionTmp.isConnected() &&
						(userName.equals(sessionTmp.getAttribute("user"))) ){
					return sessionTmp;
				}
			}
		}
		return null;
	}
	private void enQueue(String roomId, String user, String message){
		try {
			String destination = "/" + roomId + "/" + user;
			StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
			factory.setBrokerURI("tcp://" + host + ":" + port);
			Connection connection = factory.createConnection(user, password);
			connection.start();
			Session sessionAmq = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination dest = new StompJmsDestination(destination);
			MessageProducer producer = sessionAmq.createProducer(dest);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			TextMessage msg = sessionAmq.createTextMessage(message);
			producer.send(msg);
			connection.close();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
