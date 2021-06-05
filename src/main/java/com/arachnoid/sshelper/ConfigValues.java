/*
 ***************************************************************************
 *   Copyright (C) 2017 by Paul Lutus                                      *
 *   http://arachnoid.com/administration                                   *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package com.arachnoid.sshelper;
import java.io.Serializable;

final class ConfigValues implements Serializable , Cloneable {
	
	private static final long serialVersionUID = 9657867488L;
	String serverPassword = "admin";
	//String clientPassword = "admin";
	String oldProgramVersion = "";
	int ssh_server_port = 2222;
	int log_server_port = 8080;
	int clipboard_server_port = 8081;
	int logLevel = 0;
	boolean runAtStart = false;
	boolean disablePasswords = false;
	boolean allowForwarding = true;
	boolean showPasswords = false;
	boolean checkNetwork = true;
	boolean preventStandby = true;
	//boolean noRootLogin = true;
	boolean allowVoiceMessages = false;
	boolean allowSoundAlerts = true;
	boolean enableZeroconf = true;
	boolean enableLogServer = true;
	boolean enableClipboardServer = true;
	boolean enableStrict = true;
	boolean terminalDefaultColors = true;
	boolean showIPV6Addresses = false;
	boolean enableFileWriting = true;
	
	public ConfigValues clone() {
		ConfigValues v = null;
		try {
			v = (ConfigValues) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		} 
		return v;
	}
}
