/** SocketDSGEventHandler.java
 *  Copyright (C) 2001 Dweebo's Stone Games (http://www.pente.org/)
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can find it online at
 *  http://www.gnu.org/copyleft/gpl.txt
 */

package org.pente.gameServer.event;

import java.net.*;
import java.io.*;

public class ClientSocketDSGEventHandler extends SocketDSGEventHandler {

	/**
	 * @param onConnectionLost told once, from a socket thread, if the connection later dies
	 *                         from an error; not when {@link #destroy()} closes it
	 * @throws IOException if the socket's streams cannot be opened; no threads are started
	 */
	public ClientSocketDSGEventHandler(Socket s, ConnectionLostListener onConnectionLost) throws IOException {
		this.socket = s;
		this.connectionLostListener = onConnectionLost;

		// out = new ObjectOutputStream(socket.getOutputStream());
		// in = new ObjectInputStream(socket.getInputStream());
		outStream = new BufferedOutputStream(socket.getOutputStream());
		inStream = new BufferedInputStream(socket.getInputStream());
		// outStream.flush();
		// outStream = new DataOutputStream(socket.getOutputStream());
		// outStream.flush();
		// inStream = new DataInputStream(socket.getInputStream());

		super.go();
	}
}