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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final public class SSHelperApplication extends Application {
    // change this by way of the manifest file
    String PROGRAM_VERSION;
    boolean DEBUG = false;
    // FIXME used only during debugging !!!
    boolean forceReinstall = true;
    boolean restartingServers = true;
    // String SSH_USERNAME = "sshelper";
    SSHelperActivity activity = null;
    String[] processStdOut = null;
    String[] processStdErr = null;
    protected Locale locale;
    Configuration systemConfig;
    String debugLogPath = null;
    private static SSHelperApplication singleton;
    protected SSHServerManager sshServerManager = null;
    protected HTTPServerManager logServerManager = null;
    protected HTTPServerManager clipServerManager = null;
    final static int HTTP_SERVER_LOGMODE = 0;
    final static int HTTP_SERVER_CLIPMODE = 1;
    Dialog termConfigDialog = null;
    Dialog logConfigDialog = null;
    int statusLEDIndex = -1;
    int networkLEDIndex = -1;
    int directoryPerm = 7;
    //String logContent = "";
    SerializedData systemData;
    String serialObjectPath = "SSHelper.obj";
    ShellTerminal terminal = null;
    //Button homeButton = null;
    // MyEditText terminalTextView = null;
    MyLogView logView;
    ScrollView configView;
    Process sshdProcess = null;
    String appBase = null;
    File appBaseFile = null;
    File binBaseFile = null;
    String binDir = null;
    String binDirName = "bin";
    String libDir = null;
    String libDirName = "lib";
    String libexecDir = null;
    String libexecDirName = "libexec";
    String shellExecPath = null;
    String sshdServerName = "sshd";
    String homeDirName = "home";
    String sshKeyDirName = ".ssh";
    String sshPidFileName = null;
    String homeDir = null;
    String tmpDir = null;
    String devDir = null;
    String varDir = null;
    //String varLogDir = null;
    String sshdConfigName = null;
    String sshKeyDir = null;
    String sshKeyFile = null;
    String sshEtcDir;
    String sshLastLog = null;
    String sshdConfigFile = null;
    String logFile = null;
    String envConfigFile = null;
    SSHServer runSSH = null;
    Intent ssHelperServiceIntent = null;
    SSHelperService helperService = null;
    ConnectivityManager connectivityManager = null;
    StringBuilder logBuffer = null;
    int oldLogLength = -1;
    boolean installed = false;
    //boolean speechReady = false;
    //Intent speechIntent = null;
    ZeroConf zeroConf = null;
    Runtime execRun = null;
    boolean stopServers = false;
    ProgressDialog progress = null;
    boolean needProgress = false;
    Spinner logSpinner = null;
    Hashtable<String, Integer> logCatHash = null;
    Hashtable<String, Integer> logSSHHash = null;
    int maxLogSize = 65536;
    int maxDispLogSize = 8192;
    Process logCatProcess = null;
    SpannableStringBuilder dispLog;
    StringBuilder htmlLog;
    //Thread httpServerThread = null;
    Pattern sshdPat;
    Pattern patSSH;
    Pattern patLogCat;
    TreeMap<String, String> sourceEnv = null;
    String appPath;
    String userName = "";
    String boardName = "";
    String libraryPath = "";
    String terminfo = "";

    String deviceName = "";
    ClipboardManager clipboard = null;
    String currentIPAddress = "";
    Handler heartbeatHandler = null;
    int heartbeatDelay = 4000;
    Runnable heartbeat = null;
    boolean arch64 = false;

    public SSHelperApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        execRun = Runtime.getRuntime();
        try {
            appBase = getFilesDir().getParentFile().toString();
            // determine platform type
            String arch = System.getProperty("os.arch");
            arch64 = arch.contains("64");
            // build LD_LIBRARY_PATH value based on platform
            String initial = "/system/" +((arch64)?"lib64":"lib");
            // try to future-proof this action
            if(new File(initial).exists()) {
                initial += ":";
            }
            else {
                initial = "";
            }
            // this value gets assigned to LD_LIBRARY_PATH
            // in several places where it's vital to functioning
            libraryPath = initial + appBase + "/lib";
            terminfo = libraryPath + "/terminfo";
            locale = getResources().getConfiguration().locale;
            PROGRAM_VERSION = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            systemData = new SerializedData();
            sourceEnv = new TreeMap<>(System.getenv());
            systemConfig = getResources().getConfiguration();
            logBuffer = new StringBuilder();
            dispLog = new SpannableStringBuilder();
            htmlLog = new StringBuilder();
            connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            deviceName = Build.MODEL;
            // use user-defined name if available
            BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
            if (myDevice != null) {
                String dn = myDevice.getName();
                if (dn != null) {
                    deviceName = dn;
                }
            }
            // underscore spaces in device names
            deviceName = deviceName.replaceAll(" ", "_");
            // if (!largeScreen()) {
            // serialData.config.allowVoiceMessages = false;
            // }
            deSerialize();
            String[] keys = new String[]{"V", "D", "I", "W", "E", "F"};
            // colors for black background
            int[] values = new int[]{0xffffff, 0x80c0ff, 0x80ff80, 0xffff80, 0xff8080, 0xff80ff};
            // colors for white background
            //int[] values = new int[]{0x000000, 0x0040a0, 0x00a000, 0xa0a000, 0xa00000, 0xa000a0};
            logCatHash = new Hashtable<>();
            for (int i = 0; i < keys.length; i++) {
                logCatHash.put(keys[i], values[i]);
            }
            String[] keys2 = new String[]{"1", "2", "3"};
            // colors for black background
            int[] values2 = new int[]{0x80c0ff, 0x80ff80, 0xff8080};
            // colors for white background
            //int[] values2 = new int[]{0x0040a0, 0x00a000, 0xa00000};
            logSSHHash = new Hashtable<>();
            for (int i = 0; i < keys2.length; i++) {
                logSSHHash.put(keys2[i], values2[i]);
                logSSHHash.put(keys2[i], values2[i]);
            }
            homeDir = buildPath(appBase, homeDirName);
            tmpDir = buildPath(appBase, "tmp");
            varDir = buildPath(appBase, "var");
            devDir = buildPath(appBase, "dev");
            binDir = buildPath(appBase, binDirName);
            libDir = buildPath(appBase, libDirName);
            libexecDir = buildPath(appBase, libexecDirName);
            appPath = String.format("%s:%s", binDir, sourceEnv.get("PATH"));
            //shellExecPath = "/system/bin/sh";
            shellExecPath = buildPath(binDir, "bash");
            sshKeyDir = buildPath(homeDir, sshKeyDirName);
            sshKeyFile = buildPath(sshKeyDir, "id_rsa");
            envConfigFile = buildPath(sshKeyDir, "environment");
            sshEtcDir = buildPath(appBase, "etc");
            sshLastLog = buildPath(sshEtcDir, "last.log");
            debugLogPath = buildPath(sshEtcDir, "debugLog.txt");
            sshPidFileName = buildPath(sshEtcDir, "sshd.pid");
            sshdConfigName = "sshd_config";
            sshdConfigFile = buildPath(sshEtcDir, sshdConfigName);
            logFile = buildPath(appBase, "sshelper_debug_log.txt");
            // regex patterns used by the log display routines
            sshdPat = Pattern.compile(".* sshd:.*");
            patSSH = Pattern.compile(".*sshd: debug([0-9]): .*");
            patLogCat = Pattern.compile(".* ([A-Z])/.*");
            boardName = Build.BOARD;
            // now get the system user id for this app
            boolean res = execCom(binDir + "/id");
            if (res && processStdOut != null && processStdOut.length > 0) {
                String id = processStdOut[0];
                userName = id.replaceFirst("(?i).*?\\((.*?)\\).*", "$1");
            }
            installProcess();
            this.heartbeatHandler = new Handler();
            heartbeat = new Runnable() {
                public void run() {
                    //Log.e("App timer 1", "activity: " + activity);
                    if (installed && !stopServers && activity == null) {
                        testNetworkState();
                        //Log.e("App timer 2", "activity: " + activity);
                    }
                    heartbeatHandler.postDelayed(this, heartbeatDelay);
                }
            };
            heartbeatHandler.postDelayed(heartbeat, heartbeatDelay);
        } catch (Exception e) {
            //Log.e("create error","message: " + e);
            logError(e);
            e.printStackTrace();
        }
        installed = true;
    }

    protected String buildPath(String parent, String child) {
        return String.format("%s/%s", parent, child);
    }

    private void createDir(String path, int perms) {
        //debugLog("createDir entry:", path);
        File f = new File(path);
        if (!f.exists()) {
            boolean result = f.mkdirs();
            //debugLog("createDir mkdir", "path: [" + path + "], result: " + result);
            boolean ownerOnly = (perms & 8) == 0;
            f.setExecutable((perms & 1) != 0, ownerOnly);
            f.setWritable((perms & 2) != 0, ownerOnly);
            f.setReadable((perms & 4) != 0, ownerOnly);
        }
    }

    // the primary, threaded installation process
    private void installProcess() {
        Thread it = new Thread() {
            @Override
            public void run() {
                File reinstall = new File(homeDir + "/REINSTALL");
                // test whether to reinstall:
                // * if it's forced for debugging
                // * if present program version differs
                // from the installed version
                // * if there's no /bin directory
                // if there's no sshd server binary
                if (DEBUG || forceReinstall || !systemData.config.oldProgramVersion.equals(PROGRAM_VERSION)
                        || !new File(binDir + "/sshd").exists() || reinstall.exists()) {
                    //debugLog("installProcess:", "Install/reinstall is triggered.");
                    eraseAppData(appBase);
                    createDir(appBase, directoryPerm);
                    appBaseFile = new File(appBase);
                    binBaseFile = new File(binDir);
                    createDir(varDir, directoryPerm);
                    createDir(devDir, directoryPerm);
                    createDir(homeDir, directoryPerm);
                    createDir(tmpDir, directoryPerm);
                    createDir(binDir, directoryPerm);
                    createDir(libexecDir, directoryPerm);
                    createDir(sshKeyDir, directoryPerm);
                    createDir(sshEtcDir, directoryPerm);
                    installResources();
                    initBusybox();
                    makeSystemSymlinks();
                    buildEnvironmentFile();
                    makeBanners();
                    execCom(binDir + "/touch " + sshLastLog);
                    execCom(binDir + "/chmod 644 " + sshLastLog);
                    execCom(binDir + "/chmod 755 " + varDir);
                    execCom(binDir + "/chmod 640 " + sshdConfigFile);
                    execCom(binDir + "/chmod 700 " + homeDir);
                    execCom(binDir + "/chmod 700 " + sshKeyDir);
                    execCom(binDir + "/chmod 777 " + tmpDir);
                    if (DEBUG) {
                        execCom("chmod 777 " + appBase);
                        execCom("chmod 777 " + homeDir);
                        execCom("chmod 777 " + binDir);
                        execCom("chmod 777 " + sshEtcDir);
                    }
                    reinstall.delete();
                }
                postInstall();
            }
        };
        it.start();
    }

    private void eraseAppData(String base) {
        //debugLog("eraseAppData", "starting point: " + base);
        deleteFilesDirecs(new File(base));
    }

    // don't erase subdirectory "home"
    // (this preserves generated keys)
    // don't delete SSHelper.obj

    private void deleteFilesDirecs(File file) {
        try {
            if (file != null) {
                if (file.isDirectory()) {
                    String[] list = file.list();
                    if (list != null) {
                        for (String child : list) {
                            File target = new File(file,child);
                            String test = target.getCanonicalPath();
                            // avoid catastrophe
                            if (!test.contains("sshelper/home") &&
                                    !test.contains(serialObjectPath)) {
                                deleteFilesDirecs(target);
                            }
                            else {
                                //debugLog("deleteFilesDirecs", "spared: " + file.getCanonicalPath());
                            }
                        }
                    }
                }
                if (!file.toString().contains(serialObjectPath)) {
                    boolean result = file.delete();
                    //debugLog("deleteFilesDirecs", "deleted: " + file.getCanonicalPath() + ":" + result);
                }
            }
        } catch (Exception e) {
            debugLog("deleteFilesDirecs", "error: " + e);
        }
    }

    private void installResources() {
        // platform-specific installs
        String installPath = "resources/" + ((arch64) ? "aarch64" : "arm");
        installResources(installPath, appBase);
        // all-platform installs
        installResources("resources/all", appBase);
    }

    private void installResources(String spath, String dpath) {
        try {
            //debugLog("installResources", "enter:   [" + spath + "][" + dpath + "]");
            String[] list = getAssets().list(spath);
            if (list.length > 0) {
                for (String item : list) {
                    String sspath = spath + "/" + item;
                    String ddpath = dpath + "/" + item;
                    //String debugss = spath + "->" + dpath;
                    if (getAssets().list(sspath).length > 0) {
                        //debugLog("installx", "dir:" + debugss);
                        createDir(ddpath, directoryPerm);
                        installResources(sspath, ddpath);
                    } else {
                        if(!item.equals("placeholder")) {
                            //debugLog("installResources", "install: [" + sspath + "][" + ddpath + "][" + item + "]");
                            installResource(sspath, ddpath, item);
                        }

                    }
                }
            }
        } catch (Exception e) {
            debugLog("installResources error:", "" + e);
        }
    }

    private void installResource(String ipath, String dpath, String item) {
        try {
            //String dfull = appBase + "/" + path;
            //createDir(dfull, directoryPerm);
            //String pfull = dfull + "/" + item;

            if (item.matches("profile|login")) {
                item = "." + item;
                dpath = homeDir + "/" + item;
            }
            //debugLog("InstallResource: ", ipath + "->" + dpath);

            File out = new File(dpath);
            moveResource(ipath, out);
            out.setReadable(true, false);
            //if (dir.equals("bin") || dir.equals("usr") || dir.equals("libexec")) {
            out.setExecutable(true, false);
            //}

        } catch (Exception e) {
            //debugLogError(e);
            debugLog("installResource error:", "" + e);
        }
    }

    protected void moveResource(String ipath, File out) {
        try {
            //out.createNewFile();
            //debugLog("moveResource:", "from: " + ipath + ", to:" + out);
            OutputStream os = new FileOutputStream(out);
            InputStream ir = getAssets().open(ipath);
            int segSize = 32768;
            int len;
            byte[] buffer = new byte[segSize];
            while ((len = ir.read(buffer, 0, segSize)) != -1) {
                os.write(buffer, 0, len);
            }
            os.close();
        } catch (Exception e) {
            debugLog("moveResource error:", "" + e);
            e.printStackTrace();
        }
    }

    protected boolean execCom(String com) {
        //Process p;
        String[] coms = com.split(" +");
        return execCom(coms);
    }

    protected boolean execCom(final String... com) {
        boolean result = false;
        try {
            //if (systemData.config.logLevel > 0) {
            String disp = "";
            for (String s : com) {
                if (disp.length() > 0) {
                    disp += ",";
                }
                disp += "\"" + s + "\"";
            }
            //debugLog("execCom2", "executing: " + disp);
            //}
            //Process p = null;

            ProcessBuilder pb = new ProcessBuilder(com);
            //List<String> test = termPb.command();
            //for(String s : test) {
            //debugLog("execCom2 breakdown", "item: [" + s + "]");
            //}
            pb.redirectErrorStream(true);
            //termPb.directory(appBaseFile);
            Map<String, String> env = pb.environment();
            env.put("LD_LIBRARY_PATH", libraryPath);
            env.put("TERMINFO", terminfo);
            Process p = pb.start();
            result = monitorProcess(p);
            if (p != null) {
                p.destroy();
            }
        } catch (Exception e) {
            debugLog("execCom2", "ERROR:" + e);
            e.printStackTrace();
        }
        return result;
    }


    private void postInstall() {

        // must repeat this for non-install runs
        //Log.e("POST","post-install");
        appBaseFile = new File(appBase);
        generateNewKeysIfNeeded(false);
        //Log.e("POST","post-key-install");
        killPriorSSHServersClients();
        //Log.e("POST","post-kill-servers");
        setInitialUserOptions();
        sshServerManager = new SSHServerManager(this);
        logServerManager = new HTTPServerManager(this, HTTP_SERVER_LOGMODE);
        clipServerManager = new HTTPServerManager(this, HTTP_SERVER_CLIPMODE);
        startService(false);
        zeroConf = new ZeroConf(this);
        zeroConf.registerServices(systemData.config.enableZeroconf);
        // now update the old program version
        systemData.config.oldProgramVersion = PROGRAM_VERSION;
        // last act before exiting this thread
        installed = true;
    }

    protected boolean generateNewKeysIfNeeded(boolean eraseOld) {
        boolean result = false;
        try {
            // The all-important key generation process
            String[] types = new String[]{"rsa", "ecdsa", "ed25519"};
            for (String type : types) {
                if (eraseOld) {
                    execCom("rm -f %s/%s %s/%s.pub", sshKeyDir, type, sshKeyDir, type);
                }
                // don't try to generate new keys if they already exist

                if (!(new File(String.format("%s/id_%s", sshKeyDir, type)).exists())) {
                    String scom = String.format(binDir + "/ssh-keygen -t %s -f %s/id_%s -Y -q", type, sshKeyDir, type);
                    String[] comarray = {binDir + "/ssh-keygen", "-q", "-t", type, "-f", sshKeyDir + "/id_" + type, "-N", ""};
                    //Log.e("KEYGEN", "Key-gen: " + scom);
                    result = execCom(comarray);
                    if (result) {
                        execCom(String.format(binDir + "/chmod 600 %s/id_%s", sshKeyDir, type));
                        execCom(String.format(binDir + "/chmod 644 %s/id_%s.pub", sshKeyDir, type));
                    }
                } else {
                    //debugLog("generateNewKey", String.format("Check: %s key is present.", type));
                }
            }
        } catch (Exception e) {
            //debugLog("generateNewKey", "error: " + e);
            e.printStackTrace();
        }
        return result;
    }

    protected void buildEnvironmentFile() {
        // set a default time zone that users may change
        int offset = new GregorianCalendar().getTimeZone().getOffset(System.currentTimeMillis());
        int tz = offset / (1000 * 3600);
        StringBuilder sb = new StringBuilder();
        sourceEnv.put("SSHELPER", appBase);
        sourceEnv.put("LD_LIBRARY_PATH", libraryPath);
        sourceEnv.put("TERMINFO", terminfo);
        sourceEnv.put("TZ", String.format(locale, "GMT%d", -tz));
        sourceEnv.put("PATH", appPath);
        sourceEnv.put("ENV", homeDir + "/.profile");
        sourceEnv.put("USER", userName);
        sourceEnv.put("BOARD", boardName);
        String osdata = System.getProperty("os.version");
        sourceEnv.put("OSDATA", deviceName + ":" + osdata);

        for (String key : sourceEnv.keySet()) {
            String arg = String.format("%s=%s\n", key, sourceEnv.get(key));
            sb.append(arg);
        }
        writeTextFile(envConfigFile, sb.toString());
        //debugLog("Environment:", sb.toString());
    }

    protected void makeBanners() {
        String out = String.format("SSHelper Version %s Copyright 2018, P. Lutus\n", PROGRAM_VERSION);
        writeTextFile(sshEtcDir + "/banner2.txt", out);
        out += "Default password is \"admin\" (recommend: change it)\n";
        writeTextFile(sshEtcDir + "/banner1.txt", out);
    }

    protected void startService(boolean force) {
        if (!stopServers && ssHelperServiceIntent == null && (force || this.systemData.config.runAtStart)) {
            //debugLog("Monitor", "SSHelperApplication:startService call");
            // new Oreo + code
            ssHelperServiceIntent = new Intent(this, SSHelperService.class);
            SSHelperService.enqueueWork(this, ssHelperServiceIntent);
            // was done this way in in pre-Oreo devices
            // ssHelperServiceIntent = new Intent(this, SSHelperService.class);
            // startService(ssHelperServiceIntent);
        }
    }

    protected void stopService() {
        try {
            //debugLog("Monitor", "SSHelperApplication:stopService call: " + helperService);
            if (helperService != null) {
                helperService.stopNotifier();
                helperService = null;
            }
            if (ssHelperServiceIntent != null) {
                stopService(ssHelperServiceIntent);
                ssHelperServiceIntent = null;
            }
        } catch (Exception e) {
            //debugLog("stopService", "Error: " + e);
        }
    }


    protected boolean monitorProcess(Process p) {
        boolean result = false;
        ArrayList<String> listStdOut = new ArrayList<>();
        ArrayList<String> listStdErr = new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;

            boolean active = true;
            while (active) {
                active = false;
                if ((line = in.readLine()) != null) {
                    //debugLog("execCom2:", "stdout result: " + line);
                    listStdOut.add(line);
                    active = true;
                }
                if ((line = err.readLine()) != null) {
                    //debugLog("execCom2:", "stderr result: " + line);
                    listStdErr.add(line);
                    active = true;
                }
            }
            p.waitFor();
            in.close();
            err.close();
            //if (p != null) {
            result = p.exitValue() == 0;
            p.destroy();
            //}
        } catch (Exception e) {
            logError(e);
            //debugLog("execCom2 error: ", e.toString());
        }
        // save the stream results from the transaction
        processStdOut = listStdOut.toArray(new String[]{});
        processStdErr = listStdErr.toArray(new String[]{});
        //debugLog("execCom2:", "exit value: " + (p.exitValue() == 0));
        return result;
    }

    protected boolean serverRunning() {
        return sshdProcess != null;
    }

    protected void restartServers(boolean force) {
        if (!stopServers) {
            //debugLog("Monitor", "Restarting servers. force=" + force);
            restartingServers = true;
            if (sshServerManager != null) {
                if (force) {
                    killPriorSSHServersClients();
                    runSSHServer(true);
                } else {
                    sshServerManager.restartIfNeeded();
                }
            }
            if (logServerManager != null) {
                logServerManager.startServer(systemData.config.log_server_port);
            }
            if (clipServerManager != null) {
                clipServerManager.startServer(systemData.config.clipboard_server_port);
            }
            if (zeroConf != null) {
                zeroConf.registerServices(true);
            }
            logCatReader();
        }
    }

    protected void stopServers() {
        //debugLog("Monitor", "Stopping servers.");
        try {
            //if (sshServerManager != null) {
            //    sshServerManager.stopSSHServer();
            //}
            if (logServerManager != null) {
                logServerManager.stopServer();
            }
            if (clipServerManager != null) {
                clipServerManager.stopServer();
            }
            if (zeroConf != null) {
                zeroConf.registerServices(false);
            }
        } catch (Exception e) {
            //debugLog("stupServers:", "Error: " + e);
        }
    }

    // this is the best way to clean up past server and client instances

    protected void killPriorSSHServersClients() {
        //debugLog("app:", "killpriorserversclients");
        if (new File(binDir, "pidof").exists()) {
            boolean success = false;
            for (int tries = 0; tries < 4 && !success; tries++) {
                success = true;
                boolean result = true;
                processStdOut = new String[]{"x"};
                while (result && processStdOut.length > 0) {
                    result = execCom(binDir + "/pidof", sshdServerName, "ssh");
                    if (result && processStdOut.length > 0) {
                        for (String s : processStdOut) {
                            for (String ss : s.split("\\s+")) {
                                try {
                                    //debugLog("killPriorSSHServersClients:", "stopping process ID " + ss);
                                    if (!execCom(binDir + "/kill", ss)) {
                                        success = false;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void initBusybox() {
        try {
            String com = String.format("%s/busybox --install -s %s", binDir, binDir);
            execCom(com);
        } catch (Exception e) {
            //debugLog("initBusybox","error:" + e);
            logError(e);
        }
    }

    private void makeSystemSymlinks() {
        File link = new File(homeDir, "SDCard");
        processSymlink(new File(Environment.getExternalStorageDirectory().getPath()), link, true);

        link = new File(appBase + "/files/usr/bin");
        processSymlink(new File(binDir), link, true);

        link = new File(appBase + "/files/usr/etc/ssh");
        processSymlink(new File(binDir + "/ssh"), link, true);

        link = new File(appBase + "/files/usr/lib");
        processSymlink(new File(libDir), link, true);

        link = new File(appBase + "/files/home");
        processSymlink(new File(appBase +"/home"), link, true);
    }

    private boolean processSymlink(File src, File link, boolean create) {
        boolean success = false;
        try {
            if (create) {
                if (!link.exists()) {
                    String com = "ln -s " + src + " " + link;

                    Process p = execRun.exec(com);
                    p.waitFor();
                    success = (p.exitValue() == 0);
                    p.destroy();
                    //debugLog("processSymlink", String.format("%s -> %s : %s", src, link, (success ? "ok" : "fail")));

                }
            } else {
                if (link.exists()) {
                    Process p = execRun.exec("rm " + link);
                    p.waitFor();
                    success = (p.exitValue() == 0);
                    p.destroy();
                }
            }
        } catch (Exception e) {
            debugLog("processSymlink", "error:" + e);
            logError(e);
        }
        return success;
    }

    private void setInitialUserOptions() {
        // enableNetworkIfOption();
    }

    protected void enableNetworkIfOption() {
        // if (serialData.config.enableNetworkWhenRun) {
        // controlNetworkState(true);
        // }
    }

    NetworkInfo getCurrentNetworkInfo() {
        if (connectivityManager == null) {
            return null;
        } else {
            return connectivityManager.getActiveNetworkInfo();
        }
    }

    protected boolean getNetworkEnabled() {
        NetworkInfo ni = getCurrentNetworkInfo();
        return ni != null && ni.isConnected();
        //if (ni == null) {
        //    return false;
        //} else {
        //    return ni.isConnected();
        //}
    }

    protected String getCurrentIPAddress() {
        return getIPAddress(!systemData.config.showIPV6Addresses);

    }

    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = sAddr.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port
                                // suffix
                                return delim < 0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    protected void testNetworkState() {
        String ip = getCurrentIPAddress();
        if (!ip.equals(currentIPAddress)) {
            if (helperService != null) {
                helperService.updateNotification();
            }
        }
        if (zeroConf != null && systemData.config.enableZeroconf && (!ip.equals(zeroConf.currentIP))) {
            zeroConf.registerServices(true);
        }
        currentIPAddress = ip;
    }

    protected void controlServer(boolean start) {
        //debugLog("app:controlserver", "start: " + start);
        if (start) {
            runSSHServer(false);
        } else {
            //logString("stopping Server", true);
            //if (sshServerManager != null) {
            //sshServerManager.stopSSHServer();
            //}
        }
        statusLEDIndex = -1;
    }

    protected void deSerialize() {
        FileInputStream fis;
        ObjectInputStream in;
        try {
            fis = openFileInput(serialObjectPath);
            if (fis != null && fis.available() > 0) {
                in = new ObjectInputStream(fis);
                systemData = (SerializedData) in.readObject();
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void runSSHServer(boolean restart) {
        //sshServerManager.stopSSHServer();
        sshServerManager.startSSHServer(restart);
    }

    protected void makeToast(String msg) {
        Context context = getApplicationContext();
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    protected void serialize() {
        // setBootFlag();
        FileOutputStream fos;
        ObjectOutputStream out;
        try {
            fos = openFileOutput(serialObjectPath, Context.MODE_PRIVATE);
            out = new ObjectOutputStream(fos);
            out.writeObject(systemData);
            out.close();
        } catch (Exception e) {
            // logError(e);
        }
    }

    protected int setLogColor(String s, Pattern sshdPat, Pattern patSSH, Pattern patLogCat) {
        int color = 0xffffff;
        String key;
        Matcher m = sshdPat.matcher(s);
        if (m.find()) {
            color = 0xa0a040;
        }
        m = patSSH.matcher(s);
        if (m.find()) {
            key = m.group(1);
            if (key != null && logSSHHash.containsKey(key)) {
                color = logSSHHash.get(key);
            }
        }
        m = patLogCat.matcher(s);
        if (m.find()) {
            key = m.group(1);
            if (key != null && logCatHash.containsKey(key)) {
                color = logCatHash.get(key);
            }
        }
        return color;
    }

    protected void colorLog(final String src, final int len) {
        Thread th = new Thread() {
            public void run() {
                int remainingLength = len;
                // dispssb feeds to Android text display
                SpannableStringBuilder dispssb = new SpannableStringBuilder();
                // htmlSSB provides the HTML content for the Web server
                // and the clipboard copy routine
                StringBuilder htmlsb = new StringBuilder();
                htmlsb.append("<pre>");
                String lines[] = src.split("\\n");
                int color;
                int dispsp = 0;
                int sl;
                try {
                    for (String s : lines) {
                        color = setLogColor(s, sshdPat, patSSH, patLogCat);
                        htmlsb.append(String.format("<span style=\"color:#%06x\">", color)).append(s).append("</span>")
                                .append("\n");
                        sl = s.length() + 1;
                        // the internal display size must be much
                        // smaller than the HTML document size
                        // to avoid slowdowns
                        if (remainingLength < maxDispLogSize) {
                            int lcolor = 0xff000000 + color;
                            dispssb.append(s).append("\n");
                            dispssb.setSpan(new ForegroundColorSpan(lcolor), dispsp, dispsp + sl,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            dispsp += sl;
                        }
                        remainingLength -= sl;
                    }
                    htmlsb.append("</pre>");
                    synchronized (dispLog) {
                        dispLog = dispssb;
                    }
                    synchronized (htmlLog) {
                        htmlLog = htmlsb;
                    }
                    // update the display with new content
                    if (activity != null) {
                        Thread updateDisp = new Thread() {
                            public void run() {
                                synchronized (dispLog) {
                                    logView.setText(dispLog);
                                }
                            }
                        };
                        activity.runOnUiThread(updateDisp);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();

    }

    protected void updateLog(final boolean erase) {
        try {
            if (erase) {
                logBuffer = new StringBuilder();
                if (logView != null) {
                    logView.setText("");
                }
                synchronized (dispLog) {
                    dispLog = new SpannableStringBuilder();
                }
                synchronized (htmlLog) {
                    htmlLog = new StringBuilder();
                }
            }
            if (logBuffer != null) {
                while (logBuffer.length() > maxLogSize) {
                    logBuffer.delete(0, maxLogSize / 4);
                }
                int len = logBuffer.length();
                if (len > 0 && len != oldLogLength) {
                    colorLog(logBuffer.toString(), len);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    protected void copyLog(boolean htmlMode) {
        String data;
        if (htmlMode) {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><body style=\"background:#000000\">");
            synchronized (htmlLog) {
                sb.append(htmlLog);
            }
            sb.append("</body></html>");
            data = sb.toString();
        } else {
            data = logBuffer.toString();
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Activity Log", data);
        clipboard.setPrimaryClip(clip);
        makeToast(String.format("Copied %d characters to clipboard.", data.length()));
    }

    protected void eraseLog() {
        updateLog(true);
    }

    protected void logCatReader() {
        if (logCatProcess != null) {
            try {
                // execRun.exec("killall logcat");
                logCatProcess.destroy();
                logCatProcess = null;
                Thread.sleep(100);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (systemData.config.logLevel >= 5 && logCatProcess == null) {
            Thread th = new Thread() {
                public void run() {
                    synchronized (logBuffer) {
                        BufferedReader bis = null;
                        try {
                            String[] filters = new String[]{"F", "E", "W", "I", "D", "V"};
                            String filt = filters[systemData.config.logLevel - 5];
                            String[] coms = new String[]{"/system/bin/logcat", "-v", "time",
                                    String.format("*:%s", filt)};
                            // logString2("*** logcatReader: "
                            // + serialData.config.logLevel + "," + coms[1],
                            // true);
                            ProcessBuilder pb = new ProcessBuilder(coms);
                            pb.directory(new File(homeDir));
                            pb.redirectErrorStream(true);
                            logCatProcess = pb.start();
                            InputStream is = logCatProcess.getInputStream();
                            if (is != null) {
                                bis = new BufferedReader(new InputStreamReader(is));
                                String line;
                                while ((line = bis.readLine()) != null) {
                                    logString2(line, false);
                                }
                                bis.close();
                                is.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (bis != null) {
                            try {
                                bis.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (logCatProcess != null) {
                            logCatProcess.destroy();
                            logCatProcess = null;
                        }
                    }
                }
            };
            th.start();
        }
    }

    public void debugLog(String a, String b) {
        //Log.e("SSHelper Debuglog: ", a + " : " + b);
        // only show debug log messages at log levels > 0
        if (systemData != null && systemData.config != null && systemData.config.logLevel > 0) {
            logString(a + ":" + b);
        }
    }

    protected void logString(String s) {
        if (systemData != null && systemData.config != null && systemData.config.logLevel < 5) {
            logString2(s, true);
        }
    }

    protected void logString2(String s, boolean date) {
        synchronized (logBuffer) {
            if (s != null) {
                s = strip(s);
                if (s.length() > 0) {
                    if (logBuffer != null) {
                        if (date) {
                            s = dateTag() + " " + s;
                        }
                        logBuffer.append(s + "\n");
                    }
                    // now that I'm displaying logcat output
                    // this can cause a fatal feedback loop
                    if (DEBUG && systemData.config.logLevel < 5) {
                        appendTextFile(debugLogPath, s + "\n");
                        Log.w("SSHelperApp:logString:", s);
                    }
                }
            }
        }
    }

    protected void debugLogError(Exception e) {
        String s = stringError(e);
        //debugLog("Java error:", s);
    }

    protected void logError(Exception e) {
        String s = stringError(e);
        logString(s);
    }

    public String dateTag() {
        Date now = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return df.format(now);
    }

    public void appendTextFile(String path, String data) {
        try {
            File f = new File(path);
            f.setReadable(true, false);
            FileWriter fw = new FileWriter(f, true);
            fw.write(data);
            fw.close();
        } catch (Exception e) {
            // logError(e);
            e.printStackTrace();
        }
    }

    protected String readTextFile(String path) {
        String result = "";
        try {
            result = new Scanner(new File(path)).useDelimiter("xxxxxxxxx").next();
        } catch (Exception e) {
            logError(e);
            e.printStackTrace();
        }
        return result;
    }

    protected void writeTextFile(String path, String content) {
        try {
            FileWriter fw = new FileWriter(path);
            fw.write(content);
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected int safeIntConverter(String s) {
        int v = 0;
        try {
            v = Integer.parseInt(s);
        } catch (Exception e) {
        }
        return v;
    }

    public String stringError(Exception e) {
        if (e != null) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 1) {
                return "Error:" + stackTrace[1] + ":" + e.toString();
            }
        }
        return "Error: Cannot recover error.";
    }

    public String strip(String s) {
        if (s != null) {
            s = s.replaceFirst("^\\s*(.*?)\\s*$", "$1");
        }
        return s;
    }

    protected <T> void addToList(ArrayList<T> list, T... args) {
        for (T arg : args) {
            list.add(arg);
        }
    }

    public void beep() {
        ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
        tg.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE);
    }

    public void showDebugMessage(SSHelperActivity ssHelperActivity, String msg) {
        new Builder(ssHelperActivity).setTitle("Debug Message").setMessage(msg).setPositiveButton("OK", null).show();
    }

    protected void messageDialog(final SSHelperActivity activity, final String title, final String message) {
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog ad = new AlertDialog.Builder(activity).create();
                    ad.setTitle(title);
                    ad.setIcon(R.mipmap.ic_dialog_launcher_foreground);
                    ad.setMessage(message);
                    ad.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    ad.show();
                }
            });
        }
    }

    public void actionDialog(Context c, String title, String message, final FunctionInterface f) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.mipmap.ic_dialog_launcher_foreground)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                f.yes_function();
                            }
                        })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        f.no_function();
                    }
                })
                .show();
    }

    //String stackTraceAsString(Exception e) {
    //    StringWriter sw = new StringWriter();
    //    PrintWriter pw = new PrintWriter(sw);
    //    e.printStackTrace(pw);
    //    return (sw.toString());
    //}

}
