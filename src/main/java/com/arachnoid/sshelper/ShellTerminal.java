/***************************************************************************
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

import android.content.ClipboardManager;
import android.graphics.Color;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.EditText;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

final public class ShellTerminal {

    int maxDisplayBuffer = 8192;
    final char CHAR_CTLC = 3;
    final char CHAR_BELL = 7;
    final char CHAR_LF = 10;
    final char CHAR_CR = 13;
    final char CHAR_DEL = 127;
    final char CHAR_BS = 8;
    final char CHAR_TAB = 9;
    SSHelperActivity activity;
    SSHelperApplication app;
    int tickTimeMsec = 200;
    EditText display;
    StringBuffer sessionbuf;
    StringBuffer appendbuf;
    OutputStream termInput;
    InputStream termOutput;
    protected Queue<Character> inchars;
    protected Queue<Character> outchars;
    public Process terminalProcess = null;
    String TERM = "dumb";
    String ANSI_up = "\033[A";
    String ANSI_down = "\033[B";
    String ANSI_left = "\033[D";
    String ANSI_right = "\033[C";
    String ANSI_cleol = "\033[J";
    String ASCII_ESC = "\033";
    int CHAR_ESC = 27;
    // all URLS must be IPs, no name resolution
    //String host = "127.0.0.1";
    //String port;
    int ansi_state = 0;
    int historypos = 0;
    int cursorY = -1;
    int cursorX = -1;
    boolean editMode = false;
    int editPos = 0;
    public int termTextColor = Color.WHITE;
    public int termBgColor = Color.BLACK;
    boolean terminalIsRunning = false;
    int lineCount = 0;
    int eraseCount = 0;
    char lastC = '?';
    boolean newContent = false;
    String host = "localhost";
    int port = 0;
    ProcessBuilder termPb = null;
    Thread termThread = null;

    public ShellTerminal(SSHelperActivity p, SSHelperApplication app) {
        activity = p;
        this.app = app;
        inchars = new LinkedBlockingDeque<>();
        outchars = new LinkedBlockingDeque<>();
        port = app.systemData.config.ssh_server_port;
        display = (EditText) activity.findViewById(R.id.terminal_view);
        setupColors();
        //startTerminal(true);
        termLoop();
    }

    protected void termLoop() {
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    if (!terminalIsRunning && display.hasFocus()) {
                        Runnable r = new Runnable() {
                            public void run() {
                                startTerminal(true);
                            }
                        };
                        activity.runOnUiThread(r);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
    }

    protected void setColorScheme(boolean def) {
        if (app.systemData != null && app.systemData.config != null) {
            app.systemData.config.terminalDefaultColors = def;
            setupColors();
        }
    }

    protected void setupColors() {
        if (app.systemData != null &&
                app.systemData.config != null &&
                app.systemData.config.terminalDefaultColors) {
            termTextColor = Color.WHITE;
            termBgColor = Color.BLACK;
        } else {
            termTextColor = Color.BLACK;
            termBgColor = Color.WHITE;
        }
        if (display != null) {
            display.setTextColor(termTextColor);
            display.setBackgroundColor(termBgColor);
        }
    }


    protected void pasteClipboard() {
        ClipboardManager clipboard = (ClipboardManager) app.getSystemService(app.CLIPBOARD_SERVICE);
        try {
            String data = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            termWrite(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void showDialog() {
        dismissDialog();
        app.termConfigDialog = new TerminalDialog(activity);
        app.termConfigDialog.setOwnerActivity(activity);
        app.termConfigDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        app.termConfigDialog.setContentView(R.layout.terminal_dialog);
        app.termConfigDialog.setTitle("SSHelper Terminal Options");
        app.termConfigDialog.setFeatureDrawableResource(
                Window.FEATURE_LEFT_ICON, R.mipmap.ic_launcher_foreground);
        app.termConfigDialog.show();
    }

    protected void dismissDialog() {
        try {
            if (app.termConfigDialog != null) {
                app.termConfigDialog.dismiss();
                app.termConfigDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void keyboardInput(KeyEvent event) {
        char c = (char) event.getUnicodeChar();
        int k = event.getKeyCode();
        int m = event.getMetaState();
        int ctrl = ((m & KeyEvent.META_CTRL_ON) != 0) ? 1 : 0;
        try {
            if (termInput != null) {
                switch (ctrl) {
                    case 1: // control key down
                        switch (k) {
                            case KeyEvent.KEYCODE_C:
                                //termWrite(CHAR_CTLC);
                                startTerminal(true);
                                break;
                        }
                    case 0:
                        switch (k) {
                            case KeyEvent.KEYCODE_ENTER:
                                termWrite(CHAR_LF);
                                break;
                            case KeyEvent.KEYCODE_DPAD_LEFT:
                                //Log.e("keyboard->","left arrow");
                                termWrite(ANSI_left);
                                //termWrite(CHAR_BS);
                                editMode = true;
                                break;
                            case KeyEvent.KEYCODE_DPAD_RIGHT:
                                //Log.e("keyboard->", "right arrow");
                                termWrite(ANSI_right);
                                editMode = true;
                                break;
                            case KeyEvent.KEYCODE_DPAD_UP:
                                termWrite(ANSI_up);
                                break;
                            case KeyEvent.KEYCODE_DPAD_DOWN:
                                termWrite(ANSI_down);
                                break;
                            case KeyEvent.KEYCODE_ESCAPE:
                                termWrite(ASCII_ESC);
                                break;
                            case KeyEvent.KEYCODE_DEL:
                                termWrite(CHAR_DEL);
                                break;
                            default:
                                if (c != 0) {
                                    termWrite(c);
                                }
                        }
                        break;
                }
            }
            // }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void termWrite(String s) {
        for (char c : s.toCharArray()) {
            inchars.add(c);
        }
        newContent = true;
    }

    protected void termWrite(char c) {
        inchars.add(c);
        newContent = true;
    }

    protected void ansi_process(String s) {
        //app.debugLog("terminal:ansi_process","String: [" + s + "]");
        int n = 0;
        int stylev = 0;
        // int colorv = 7;

        for (char c : s.toCharArray()) {
            //app.debugLog("terminal:ansi_process", "char: [" + c + "], " + Integer.toString(c));
            if (c == CHAR_ESC && ansi_state == 0) {
                ansi_state = 1;
                // colorv = 7;
                stylev = 0;
                n = 0;
                continue;
            }
            if (c == '[' && ansi_state == 1) {
                ansi_state = 2;
                // colorv = 0;
                stylev = 0;
                continue;
            }
            if (ansi_state == 2 || ansi_state == 3) {
                if (c >= '0' && c <= '9') {
                    n = (n * 10) + (c - '0');
                } else if (c == ';') {
                    stylev = n;
                    n = 0;
                    ansi_state = 3;
                } else if (c == 'H') {
                    // can't actually use these, but
                    // they can signal clear-screen
                    if (ansi_state == 2) {
                        cursorX = n;
                        cursorY = 0;
                    } else if (ansi_state == 3) {
                        cursorX = stylev;
                        cursorY = n;
                    }
                } else if (c == 'J') {
                    // app.logString("ansi clear detected: " + n, true);
                    // ansiClear(n);
                    ansi_state = 0;

                } else if (c == 'm') {
                    if (ansi_state == 3) {
                        // colorv = n % 10;
                    } else {
                        stylev = n;
                    }
                    // if (colorv >= 0 && colorv <= 7) {
                    // textColor = textcolors[colorv];
                    // }
                    // stylev = (stylev > 1) ? 1 : 0;
                    // textStyle = textstyles[stylev];
                    n = 0;
                    ansi_state = 0;
                } else if (c == 'C') { // cursor forward
                    cursorX += n;
                    //Log.e("Cursor Position", "" + (n));
                    n = 0;
                    ansi_state = 0;
                } else if (c == 'D') { // cursor back
                    cursorX -= n;
                    //Log.e("Cursor Position", "" + (-n));
                    n = 0;
                    ansi_state = 0;
                } else {
                    // app.logString("new ansi code detected: " + c + ","
                    // + ((int) c), true);
                }
                continue;
            }
            if (ansi_state == 0) {
                if (c == CHAR_BS) {
                    cursorX -= 1;
                    editPos -= 1;
                    dispAppend((char) 0, 0);
                } else if (c == CHAR_CR) {
                    cursorX = 0;
                    dispAppend(c, 1);
                } else if (c == CHAR_LF) {
                    // newLineFlag = true;
                    cursorY += 1;
                    dispAppend(c, 2);
                } else if (c == CHAR_BELL) {
                    app.beep();
                } else {
                    cursorX += 1;
                    dispAppend(c, 3);
                }
            }
        }
    }

    protected void dispAppend(char c, int index) {
        if (c == CHAR_TAB) {
            while (lineCount == 0 || lineCount % 8 != 0) {
                addToBuf(' ', 0);
                lineCount += 1;
            }
            return;
        }
        if (c == CHAR_CR && lastC == CHAR_CR) {
            return;
        } else {
            if (c >= ' ' || c == CHAR_LF) {
                addToBuf(c, 1);
            }
        }
        lastC = c;
        if (c == CHAR_CR) {
            eraseCount = lineCount;
            editPos = 0;
            lineCount = 0;
        } else if (c == CHAR_LF) {
            eraseCount = 0;
            lineCount = 0;
            editPos = 0;
        } else if (eraseCount != 0) {
            if (editPos < 0) {
                eraseCount++;
            }
            deleteFromBuf(eraseCount);
            eraseCount = 0;
            lineCount = 0;
        }
        if (c >= ' ') {
            lineCount += 1;
        }
        newContent = true;
    }

    protected void addToBuf(char c, int index) {
        //app.debugLog("terminal:addToBuf", "index:" + index + ", char [" + c + "] = " + Integer.toString(c));
        if (appendbuf != null) {
            if (editPos == 0) {
                appendbuf.append(c);
            } else {
                int n = appendbuf.length();
                appendbuf.insert(n + editPos, c);
                editPos += 1;
                appendbuf.delete(n + editPos, n + editPos + 1);
            }
            int n = appendbuf.length();
            if (n > maxDisplayBuffer * 2) {
                appendbuf.delete(0, n - maxDisplayBuffer);
            }
        }
    }

    protected void deleteFromBuf(int v) {
        int n = appendbuf.length();
        v = Math.min(n, v);
        appendbuf.delete(n - v, n);
    }

    protected void updateDisplay() {
        Runnable upddisp = new Runnable() {
            public void run() {
                //while (true) {
                if (terminalIsRunning && newContent) {
                    newContent = false;
                    // deal with a nasty bug that indented text
                    // after a delete action
                    String content = appendbuf.toString();
                    // replaces the last instance of \n\s* with \n
                    content = content.replaceAll("(?s)(.*)\n\\s*", "$1\n");
                    //app.debugLog("terminal:updateDisplay","content: [" + content + "]");
                    display.setText(content);
                    int n = display.length();
                    int cp = n + editPos;
                    cp = Math.max(cp, 0);
                    cp = Math.min(cp, n);
                    display.setSelection(cp, cp);
                    //appendbuf.length();
                    //Log.e("update display length ", "" + editPos);
                }
            }
        };
        activity.runOnUiThread(upddisp);
    }

    protected Thread readTermThread() {
        return new Thread() {
            public void run() {
                try {
                    while (terminalIsRunning) {
                        Thread.sleep(tickTimeMsec);
                        updateDisplay();
                    }

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                    terminalIsRunning = false;
                }
            }
        };
    }

    protected void setDisplayContent(final String s) {
        Runnable runnable = new Runnable() {

            public void run() {
                // TODO Auto-generated method stub
                display.setText(s);
                //display.requestFocus();
            }
        };
        activity.runOnUiThread(runnable);
    }

    protected Thread inputThread() {
        return new Thread() {
            public void run() {
                while (terminalIsRunning && inchars != null) {
                    while (inchars.size() > 0) {
                        try {
                            char c = inchars.remove();
                            //Log.e("ZZZ: inputThread", "" + c);
                            termInput.write(c);
                            termInput.flush();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            return;
                        }
                    }
                }
            }
        };
    }

    protected Thread outputThread() {
        return new Thread() {
            public void run() {
                while (terminalIsRunning && termOutput != null) {
                    try {
                        int len = termOutput.available();
                        if (len > 0) {
                            byte[] b = new byte[len];
                            //Log.e("ZZZ: outputThread", "" + b);
                            termOutput.read(b);
                            String s = new String(b, "UTF-8");
                            ansi_process(s);
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return;
                    }

                }
            }
        };
    }

    protected void startTerminal(boolean force) {
        if (app.terminal != null) {
            display.setTextColor(app.terminal.termTextColor);
            display.setBackgroundColor(app.terminal.termBgColor);
        }
        //Log.e("ZZZ: startTerminal", "waiting for start ..." + terminalIsRunning);
        if (!terminalIsRunning || force) {
            if (terminalProcess != null) {
                int n = 8;
                while (n-- > 0 && terminalIsRunning) {
                    try {
                        termWrite("exit\n");
                        //Log.e("ZZZ: startTerminal", "waiting for exit ..." + n);
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            stopTerminal();
            display.setText("");
            if (activity != null) {
                activity.setCurrentTab(2);
            }
            termThread = launchTermThread();
            termThread.start();
        }
    }

    protected void stopTerminal() {
        // app.debugLog("ShellTerminal","stopTerminal: " + termInput);
        //Log.e("ZZZ: stopTerminal", "exiting...");
        try {
            if (termInput != null) {
                termInput.close();
                termInput = null;
            }
            if (termOutput != null) {
                termOutput.close();
                termOutput = null;
            }
            if (terminalProcess != null) {
                terminalProcess.destroy();
            }
            //terminalIsRunning = false;
            setDisplayContent("");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected Thread launchTermThread() {
        return new Thread() {
            public void run() {
                terminalIsRunning = true;
                sessionbuf = new StringBuffer();
                appendbuf = new StringBuffer();
                //Log.e("ZZZ: launchTermThread", "Starting");
                try {
                    //String[] com = new String[]{app.binDir + "/ssh", "-q",
                    //        "-tt", "-o UserKnownHostsFile /dev/null",
                    //        "-o StrictHostKeyChecking no", host, "-p", Integer.toString(port)};
                    String[] com = {app.binDir + "/bash","--rcfile", app.homeDir + "/.profile", "-i"};
                    termPb = new ProcessBuilder(com);
                    termPb.directory(new File(app.homeDir));
                    //app.debugLog("launchTermThread home dir", app.homeDir);
                    Map<String, String> env = termPb.environment();
                    env.put("PATH", app.appPath);
                    env.put("HOME", "/data/data/com.arachnoid.sshelper/home");
                    env.put("USER", app.userName);
                    env.put("SHELLEXEC", app.binDir + "/bash");
                    //env.put("SSH_SERVER_PW",
                    //app.systemData.config.serverPassword);
                    env.put("LD_LIBRARY_PATH", app.libraryPath);
                    env.put("TERMINFO", app.terminfo);
                    env.put("TERM", TERM);
                    //env.put("SSH_USER_PW", app.systemData.config.serverPassword);
                    termPb.redirectErrorStream(true);
                    terminalProcess = termPb.start();
                    termOutput = terminalProcess.getInputStream();
                    termInput = terminalProcess.getOutputStream();
                    inputThread().start();
                    outputThread().start();
                    readTermThread().start();
                    //display.requestFocus();
                    terminalProcess.waitFor();
                    terminalIsRunning = false;
                    //Log.e("ZZZ: exiting launchTermThread", "normal exit");
                } catch (Exception e) {
                    app.logError(e);
                    app.debugLog("terminal thread", "error:" + e);
                    Log.e("ZZZ: launchTermThread", "error exit: " + e);
                    return;
                }
                stopTerminal();
            }
        };
    }
}
