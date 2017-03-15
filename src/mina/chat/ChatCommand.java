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

/**
 * Encapsulates a chat command. Use {@link #valueOf(String)} to create an
 * instance given a command string.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ChatCommand {
    public static final int LOGIN = 0;

    public static final int QUIT = 1;

    public static final int BROADCAST = 2;
    
    public static final int JOIN = 3;
    
    public static final int CREATE = 4;

    public static final int LEAVE = 5;

    public static final int DESTROY = 6;

    public static final int USERKICK = 7;

    public static final int PAUSE = 8;
    
    private final int num;

    private ChatCommand(int num) {
        this.num = num;
    }
    public int toInt() {
        return num;
    }

    public static ChatCommand valueOf(String s) {
        s = s.toUpperCase();
        if ("LOGIN".equals(s)) {
            return new ChatCommand(LOGIN);
        }
        if ("QUIT".equals(s)) {
            return new ChatCommand(QUIT);
        }
        if ("BROADCAST".equals(s)) {
            return new ChatCommand(BROADCAST);
        }
        if ("JOIN".equals(s)) {
            return new ChatCommand(JOIN);
        }
        if ("CREATE".equals(s)) {
            return new ChatCommand(CREATE);
        }
        if ("LEAVE".equals(s)) {
            return new ChatCommand(LEAVE);
        }
        if ("DESTROY".equals(s)){
        	return new ChatCommand(DESTROY);
        }
        if ("USERKICK".equals(s)){
        	return new ChatCommand(USERKICK);
        }
        if ("PAUSE".equals(s)){
        	return new ChatCommand(PAUSE);
        }
        throw new IllegalArgumentException("Unrecognized command: " + s);
    }
}
