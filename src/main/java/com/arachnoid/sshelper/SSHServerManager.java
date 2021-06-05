/*
 ***************************************************************************
 *   Copyright (C) 2013 by Paul Lutus                                      *
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

//import java.security.MessageDigest;

import android.util.Log;

import java.util.ArrayList;

final public class SSHServerManager {
    SSHelperApplication app;

    public SSHServerManager(SSHelperApplication app) {
        this.app = app;
    }

    //protected void stopSSHServer() {
        // this was a bad idea
        // app.killPriorSSHServersClients();
    //}

    protected void restartIfNeeded() {
        app.debugLog("Monitor", "restartIfNeeded: " + app.runSSH + "," + app.serverRunning());
        if (app.runSSH == null || !app.serverRunning()) {
            startSSHServer(false);
        }
    }

    // stop prior instances and generate keys
    protected void startSSHServer(final boolean restart) {
        app.debugLog("Monitor", "startSSHServer: " + restart + "," + app.serverRunning());
        if (restart) {
            //stopSSHServer();
        }
        if (app.activity != null) {
            app.activity.buildWaitDialog("SSHelper Processing",
                    "Generating SSH keys, please wait ...");
        }
        Thread kt = new Thread() {
            @Override
            public void run() {
                app.generateNewKeysIfNeeded(false);
                startSSHServer2(restart);
            }
        };
        kt.start();
    }

    // post key generation actions
    private void startSSHServer2(boolean restart) {
        app.debugLog("startSSHServer2", "Restart: " + restart + ", app.serverRunning: " + app.serverRunning());
        try {
            if (app.activity != null) {
                app.activity.killDialog();
            }
            if (!app.serverRunning() || restart) {
                app.logString("Starting/restarting SSH server on port "
                        + app.systemData.config.ssh_server_port);

                ConfigValues prefix = app.systemData.config;
                ArrayList<String> coms = new ArrayList<>();
                app.addToList(coms, app.binDir + "/" + app.sshdServerName);
                // app.addToList(coms,"../utilities/readcom.sh");
                // don't become a daemon
                app.addToList(coms, "-D");
                // confirmed for sshd
                app.addToList(coms, "-p", String.format(app.locale, "%d",
                        app.systemData.config.ssh_server_port));
                // confirmed for sshd
                app.addToList(coms, "-h", app.sshKeyFile);
                app.addToList(coms, "-o PidFile " + app.sshPidFileName);
                app.addToList(coms, "-f", app.sshdConfigFile);

                // debug flag, max 3
                int ll = app.systemData.config.logLevel;
                if (ll > 1 && ll < 5) {
                    String slvl = "";
                    for (int i = 1; i < ll; i++) {
                        slvl += 'd';
                    }
                    // set debug mode
                    app.addToList(coms, "-" + slvl);
                    app.debugLog("sshd:", "*** Warning: This is a special debug mode set by \"select data");
                    app.debugLog("sshd:", "***          logging mode\", only one login allowed before restart.");
                    app.debugLog("sshd:", "***          To end this special mode, make a logging selection");
                    app.debugLog("sshd:", "***          outside the \"SSH Server Debug 1-3\" region.");
                }
                // send messages to stdout instead of system log
                app.addToList(coms, "-e");
                if (prefix.disablePasswords) {
                    app.addToList(coms, "-o PasswordAuthentication no");
                }
                if (prefix.allowForwarding) {
                    app.addToList(coms, "-o PermitTunnel yes");
                }
                app.addToList(coms, String.format("-o StrictModes %s",
                        (app.systemData.config.enableStrict ? "yes" : "no")));
                int n = (app.systemData.config.serverPassword.equals("admin")) ? 1
                        : 2;
                app.addToList(
                        coms,
                        "-o Banner " + app.sshEtcDir
                                + String.format(app.locale, "/banner%d.txt", n));
                // app.addToList(coms, "-o PermitEmptyPasswords yes");

                //boolean test = false;
                //if (test) {
                //	app.addToList(coms, "-t");
                //}

                String[] scoms = coms.toArray(new String[]{});

                // create the environment for the subprocess

                if (app.runSSH != null) {
                    app.runSSH.stopCurrentProcess();
                    // app.runSSH = null;
                }
                // FIXME: temporarily disabled
                app.runSSH = new SSHServer(app, scoms, app.appBase);

            } else {
                app.debugLog("SSHServermanager", "Server check ok");
            }
        } catch (Exception e) {
            app.debugLog("SSHServermanager", "error: " + e);
            app.logError(e);
            app.debugLog("Error", "startSSHServer2: " + e);
        }
    }
}
