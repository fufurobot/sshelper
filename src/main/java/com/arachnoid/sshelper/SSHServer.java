/*
 ***************************************************************************
 *   Copyright (C) 2012 by Paul Lutus                                      *
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Map;

final public class SSHServer extends Thread {
	SSHelperApplication app;
	boolean starting = true;
	boolean addressInUse = false;
	private boolean noHostKeys = false;
	private boolean running = false;
	boolean dialogShown = false;
	boolean networkDialogShown = false;
	// int exitCode = 0;
	private String[] coms;
	private String homePath;

	// int currentPid = -1;

	public SSHServer(SSHelperApplication p, String[] coms, String homePath) {
		app = p;
		this.coms = coms;
		this.homePath = homePath;
		start();
	}

	protected void stopCurrentProcess() {
        app.debugLog("SSHServer:stopCurrentProcess","app.sshdProcess: " + app.sshdProcess);
        running = false;
		if (app.sshdProcess != null) {
			app.sshdProcess.destroy();
			app.sshdProcess = null;
		}
	}

	@Override
	public void run() {
		app.debugLog("SSHServer:run","app: " + app);
		try {
			if (app != null && !running && app.sshdProcess == null) {
				starting = true;
				// restart if no activity in control
				while ((starting || app.activity == null) && !addressInUse
						&& !noHostKeys) {
					running = true;
					ProcessBuilder pb = new ProcessBuilder(coms);
					pb.directory(new File(homePath));
					Map<String, String> env = pb.environment();
					env.put("PS1", "[\\u\\@\\h \\w ]\\$ ");
					env.put("SSH_SERVER_PW",
							app.systemData.config.serverPassword);
					env.put("USER", app.userName);
					env.put("LD_LIBRARY_PATH",app.libraryPath);
					env.put("TERMINFO", app.terminfo);
					env.put("SHELLEXEC",app.shellExecPath);
					env.put("SSH_USERNAME",app.userName);
					env.put("SSH_USERPATH",app.homeDir);
					pb.redirectErrorStream(true);
					app.sshdProcess = pb.start();
					app.debugLog("SSHServer:starting","sshdprocess=create: " + app.sshdProcess);
					// app.logString("--- Server started.", true);
					BufferedReader bis;
					FileWriter fw = null;
					bis = new BufferedReader(new InputStreamReader(
							app.sshdProcess.getInputStream()));
					if (app.DEBUG) {
						File logf = new File(app.logFile);
						if (!logf.exists()) {
							logf.createNewFile();
						}
						// can't set readable on a nonexistent file
						logf.setReadable(true, false);
						fw = new FileWriter(logf, true);
					}
					String line = "";
					while (running && !addressInUse && !noHostKeys
							&& (line = bis.readLine()) != null) {
						if (line.matches("(?i).*address already in use.*")) {
							addressInUse = true;
						}
						if (line.matches("(?i).*no hostkeys available.*")) {
							noHostKeys = true;
						}
						if (line.matches("(?i).*terminating.*")) {
							running = false;
						}
						if (fw != null) {
							fw.write("sshd output: " + line + "\n");
							fw.flush();
						}
						app.logString("sshd: " + line);
						app.debugLog("SSHServer from sshd: ",line);
					}
					bis.close();
                    app.debugLog("SSHServer:closing","aiu:" + addressInUse + ", nhk:" + noHostKeys + ", running:" + running + " line: " + line);
					if (app.DEBUG && fw != null) {
						fw.close();
					}
					if (addressInUse || noHostKeys) {
						String msg = (addressInUse) ? String
								.format(app.locale,"* another helperService is preventing use of port %d.",
										app.systemData.config.ssh_server_port)
								: "* There are no SSH server hostkeys available.";
						app.logString(msg);
						app.logString(
								"* to solve this problem, click the \"Restart\" menu item.");
					}
					stopCurrentProcess();
					//if (app.sshdProcess != null) {
					//	Log.e("Monitor","sshdprocess=destroy: " + app.sshdProcess);
					//	app.sshdProcess.destroy();
					//	app.sshdProcess = null;
					//}
					starting = false;
					running = false;
				}
			}
		} catch (Exception e) {
			app.logError(e);
			e.printStackTrace();
			app.debugLog("SSHServer:error",""+ e);
		}
	}
}
