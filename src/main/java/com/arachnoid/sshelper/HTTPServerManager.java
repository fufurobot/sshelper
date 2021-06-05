/***************************************************************************
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

import android.content.ClipData;
import android.content.res.AssetManager;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.zip.GZIPInputStream;

final class HTTPServerManager {
	SSHelperApplication app;
	private int oldPort = -1;
	private boolean running = false;
	private Thread thread = null;
	private String[] pageSections;
	private byte[] favicon;
	private String[] modeNames = { "LogServer", "ClipboardServer" };
	private int mode = -1;
	private int port = -1;

	ServerSocket ss = null;

	// modes: 0 = log display, 1 = clipboard
	public HTTPServerManager(SSHelperApplication a, int mode) {
		app = a;
		String page = "";
		this.mode = mode;
		if (mode == SSHelperApplication.HTTP_SERVER_LOGMODE) {
			page = app.readTextFile(app.varDir + "/log_server_page.html");
		} else {
			page = app.readTextFile(app.varDir + "/clipboard_server_page.html");
		}
		page = page.replaceAll("#DEVNAME#", app.deviceName);
		pageSections = page.split("####");
		pageSections[0] += "\n";
		AssetManager asm = app.getAssets();
		favicon = readResource(asm, "resources/favicon.ico");
	}

	private byte[] readResource(AssetManager asm, String path) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		try {
			InputStream ir = asm.open(path);
			if (path.matches(".*_gz$")) {
				ir = new GZIPInputStream(ir);
			}
			int segSize = 32768;
			int len;
			byte[] buffer = new byte[segSize];
			while ((len = ir.read(buffer, 0, segSize)) != -1) {
				bb.write(buffer, 0, len);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bb.toByteArray();
	}

	protected void stopServer() {
		running = false;

		if (ss != null) {
			try {
				ss.close();
				if (thread != null) {
					thread.interrupt();
					Thread.sleep(100);
					thread = null;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				// Log.e("ERROR stopserver", "" + app.stackTraceAsString(e));
			}
		}

	}

	protected void startServer(int port) {
		if (running) {
			stopServer();
		}
		this.port = port;
		if ((mode == SSHelperApplication.HTTP_SERVER_LOGMODE && app.systemData.config.enableLogServer)
				|| (mode == SSHelperApplication.HTTP_SERVER_CLIPMODE && app.systemData.config.enableClipboardServer)) {
			start();
		}
	}

	private void byteWrite(ByteArrayOutputStream bb, String s) {
		byteWrite(bb, s.getBytes());
	}

	private void byteWrite(ByteArrayOutputStream bb, byte[] buf) {
		bb.write(buf, 0, buf.length);
	}

	private void start() {
		if (!running) {
			try {
				ss = new ServerSocket();
			} catch (IOException e) {
				e.printStackTrace();
				// Log.e("ERROR0", "" + app.stackTraceAsString(e));
			}
			running = true;
			//oldPort = port;
			thread = new Thread() {
				public void run() {
					while (running) {
						try {
							app.logString(modeNames[mode]
									+ ": restarting server on port: " + port);
							ss.setReuseAddress(true);
							InetSocketAddress isa = new InetSocketAddress(port);
							while (!ss.isBound()) {
								try {
									ss.bind(isa);
								} catch (Exception e) {
									e.printStackTrace();
								}
								Thread.sleep(100);
							}
							String httpHeader = "HTTP/1.0 200 OK\r\nContent-type: %s\r\n\r\n";
							while (ss.isBound() && running) {
								Socket cs = ss.accept();
								BufferedReader is = new BufferedReader(
										new InputStreamReader(
												cs.getInputStream()));
								BufferedOutputStream os = new BufferedOutputStream(
										cs.getOutputStream());
								String line;
								boolean iconRequest = false;
								boolean post = false;
								int content_length = 0;
								while (running
										&& (line = is.readLine()) != null
										&& line.length() > 0) {
									// Log.e("READLINE","content: " + line);
									if (line.matches("(?i).*favicon.*")) {
										iconRequest = true;

									} else if (line.matches("POST.*")) {
										post = true;
									}
									if (post
											&& line.matches("(?i)Content-length.*")) {
										String s = line.replaceFirst(
												".*?(\\d+).*", "$1");
										content_length = Integer.parseInt(s);
									}
								}
								ByteArrayOutputStream bb = new ByteArrayOutputStream();
								byteWrite(bb, String.format(httpHeader,
										(iconRequest) ? "image/x-icon"
												: "text/html"));
								if (iconRequest) {
									byteWrite(bb, favicon);
								} else {
									if (post && content_length > 0) {
										// Log.e("POST receiver","content length: "
										// + content_length);
										char[] buf = new char[content_length];
										is.read(buf, 0, content_length);
										// Log.e("POST receiver","phase 2 ");
										String content = URLDecoder
												.decode(new String(buf));
										// Log.e("POST receiver","phase 3 ");
										content = content.replaceFirst("clip=",
												"");
										if (app.clipboard != null) {
											app.clipboard.setText(content);
										}
									}
									byteWrite(bb, pageSections[0]);
									if (mode == SSHelperApplication.HTTP_SERVER_LOGMODE) {
										app.updateLog(false);
										synchronized (app.htmlLog) {
											byteWrite(bb,
													app.htmlLog.toString());
										}

									} else {
										if (app.clipboard != null) {
											ClipData clip = app.clipboard
													.getPrimaryClip();
											if (clip != null) {
												String sclip = clip
														.getItemAt(0).getText()
														.toString();
												sclip = sclip.replaceAll("<",
														"&lt;");
												sclip = sclip.replaceAll(">",
														"&gt;");
												byteWrite(bb, sclip);
											}
										}
									}
									byteWrite(bb, pageSections[1]);
								}
								if (bb.size() > 0) {
									os.write(bb.toByteArray());
								}
								os.flush();
								os.close();
								is.close();
								cs.close();
							}
						} catch (Exception e) {
							// Log.e("ERROR1", "" + app.stackTraceAsString(e));
							running = false;
						}
					}
					running = false;
					if (ss != null) {
						try {
							ss.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							// Log.e("ERROR2", "" + app.stackTraceAsString(e));
						}
					}
				}
			};
			thread.start();
		}
	}
}
