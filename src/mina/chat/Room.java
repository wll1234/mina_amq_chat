package mina.chat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.core.session.IoSession;

public class Room {
	private IoSession adminUser;
	private int size;
	private String title;
	private int roomID;
	private final Set<IoSession> attends;
	private final Set<IoSession> pauses;
	
	public IoSession getAdminUser() {
		return adminUser;
	}
	public void setAdminUser(IoSession adminUser) {
		this.adminUser = adminUser;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getRoomID() {
		return roomID;
	}
	public void setRoomID(int roomID) {
		this.roomID = roomID;
	}
	public Room(IoSession session, int size, String title, int roomID){
		this.adminUser = session;
		this.size = size;
		this.title = title;
		this.roomID = roomID;
		this.attends = Collections.synchronizedSet(new HashSet<IoSession>());
		this.pauses = Collections.synchronizedSet(new HashSet<IoSession>());
	}
	public Room(){
		this.adminUser = null;
		this.size = 100;
		this.title = "";
		this.roomID = 0;
		this.attends = Collections.synchronizedSet(new HashSet<IoSession>());
		this.pauses = Collections.synchronizedSet(new HashSet<IoSession>());
	}
	public Set<IoSession> getAttends() {
		return attends;
	}
	public Set<IoSession> getPauses() {
		return pauses;
	}
	public void addAttend(IoSession session){
		if(pauses.contains(session))
			pauses.remove(session);
		attends.add(session);
	}
	public void removeAttend(IoSession session){
		attends.remove(session);
	}
	public void addPauses(IoSession session){
		if(attends.contains(session))
			attends.remove(session);
		pauses.add(session);
	}
	public void removePause(IoSession session){
		pauses.remove(session);
	}
}
