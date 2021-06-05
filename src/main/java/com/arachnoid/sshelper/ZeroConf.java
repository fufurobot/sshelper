/***************************************************************************
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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

final public class ZeroConf {
	SSHelperApplication app;
	JmDNS jmdns = null;
	boolean launched = false;
	Thread startThread = null;
	String currentIP = "";
	ArrayList<ServiceInfo> svclist;

	public ZeroConf(SSHelperApplication app) {
		this.app = app;
		// Log.e("Zeroconf", "Constructor");
		svclist = new ArrayList<ServiceInfo>();
	}

	// this scheme is required because Android doesn't
	// allow network tasks on the UI thread

	protected void registerServices(final boolean enable) {
		// don't launch multiple threads
		// Log.e("Zeroconf", "Register services " + launched + "," + enable +
		// "," + startThread + "," + app.stopServers
		// + "," + app.serialData.config.enableZeroconf + "," + currentIP + ","
		// + app.getCurrentIPAddress());
		if (startThread == null && app.systemData.config.enableZeroconf && !app.stopServers) {
			if (launched != enable || !(app.getCurrentIPAddress().equals(currentIP))) {
				currentIP = app.getCurrentIPAddress();
				// Log.e("Zeroconf",
				// "Starting thread " + launched + "," + enable + "," +
				// startThread + "," + app.stopServers);
				Thread startThread = new Thread() {
					@Override
					public void run() {
						updateService(enable);
					}
				};
				startThread.start();
			}
		}
		launched = enable;
	}

	private void updateService(boolean enable) {
		// Log.e("Zeroconf:updateService", "enable: " + enable + ", launched: "
		// + launched);
		closeService();
		if (enable) {
			registerService();
		}
		if (startThread != null) {
			startThread.destroy();
			startThread = null;
		}
	}

	private void registerService() {
		try {
			String ip = app.getCurrentIPAddress();
			// Log.e("Zeroconf: Current IP", ip);
			jmdns = JmDNS.create(InetAddress.getByName(ip), app.deviceName);
			// Log.e("Zeroconf: Current IP from jmdns", "IP:" +
			// jmdns.getInetAddress());
			if (jmdns != null) {
				String[] services = new String[] { "_workstation._tcp.local", "_sftp-ssh._tcp.local.",
						"_ssh._tcp.local.", "_rsync._tcp.local.", "_http._tcp.local." };
				for (String service : services) {
					int port = -1;
					String text = "Service";
					if (service.matches(".*http.*")) {
						if (app.systemData.config.enableLogServer) {
							port = app.systemData.config.log_server_port;
							text = "SSH server activity log page";
						}
					} else if (service.matches(".*workstation.*")) {
						port = 9;
						text = "Workstation services";
					} else {
						port = app.systemData.config.ssh_server_port;
						text = "Secure Shell services";
					}
					if (port != -1) {
						HashMap<String, String> hm = new HashMap<String, String>();
						hm.put("Description", text);
						ServiceInfo svc = ServiceInfo.create(service, app.deviceName, port, 0, 0, hm);
						svclist.add(svc);
						jmdns.registerService(svc);
						//app.debugLog("JmDNS Zeroconf", "register helperService: " + service);
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// Log.e("Zeroconf: Error", "" + e);
			//app.debugLog("JmDNS Zeroconf", "error: " + e.getMessage());
		}
	}

	private void closeService() {
		try {
			//jmdns = JmDNS.create();
			if (jmdns != null) {
				jmdns.unregisterAllServices();
				jmdns.close();
				jmdns = null;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
};