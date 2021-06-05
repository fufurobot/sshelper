/*
 ***************************************************************************
 *   Copyright (C) 2018 by Paul Lutus                                      *
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

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

//import android.util.Log;

final public class SSHelperActivity extends android.support.v7.app.AppCompatActivity implements KeyEvent.Callback {

    SSHelperApplication app;
    SSHelperActivity activity;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    int tickTimeMsec = 4000;
    int[] textColors = null;
    WebView helpWebView = null;
    Handler timerHandler = null;
    Runnable delayTimer = null;
    String currentTab = "";
    Stack<ConfigValues> configUndoStack;
    boolean inForeground = false;
    int oldLogLevel = -1;
    private static final int MY_DATA_CHECK_CODE = 69;
    private static TextToSpeech tts;
    boolean canSpeak = false;
    Thread speakThread = null;
    String speakMessage;
    EditText passwordEntry;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    protected ViewPager mViewPager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (SSHelperApplication) getApplication();
        activity = this;
        app.activity = this;

        setContentView(R.layout.activity_sshelper);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        android.support.v7.app.ActionBar ab = getSupportActionBar();

        ab.setTitle("");
        ab.setCustomView(R.layout.app_tag_upper_left);
        ab.setDisplayShowCustomEnabled(true);

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        // prevent pointless loss of views
        mViewPager.setOffscreenPageLimit(3);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        app.stopServers = false;

        setupTextToSpeech();

        timerHandler = new Handler();
    }

    protected void afterResume() {
        // this assures that the layout is complete, everything created
        mViewPager.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mViewPager.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                layoutComplete();
            }
        });
    }


    protected void layoutComplete() {
        // execute WaitInstall if needed
        if (!app.installed) {
            new WaitInstall(app).execute();
        } else {
            postInstall();
        }
    }

    // wait for application to complete install

    private class WaitInstall extends AsyncTask<Void, Void, Void> {
        SSHelperApplication application;

        private WaitInstall(SSHelperApplication a) {
            application = a;
        }

        @Override
        protected void onPreExecute() {
            if (!application.installed) {
                buildWaitDialog("SSHelper Initialization",
                        "Installing application and generating SSH keys, please wait ...");
            }
        }

        @Override
        protected Void doInBackground(Void... params) {


            while (!application.installed) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    application.logError(e);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void x) {
            killDialog();
            postInstall();
        }

    }

    @SuppressWarnings("deprecation")
    private void postInstall() {
        inForeground = true;
        textColors = new int[]{0xffff0000, 0xffffff00, 0xff00ff00};
        app.logSpinner = (Spinner) findViewById(R.id.log_spinner);
        app.logView = (MyLogView) findViewById(R.id.log_view);
        app.configView = (ScrollView) findViewById(R.id.config_view);
        configUndoStack = new Stack<>();
        TextView key = (TextView) findViewById(R.id.app_id_key);
        TextView value = (TextView) findViewById(R.id.app_id_value);
        if (key != null && value != null) {
            key.setText("Application:");
            String urla = "http://arachnoid.com/android/SSHelper";
            String urlb = "http://arachnoid.com/administration";
            String vs = String.format("<a href=\"%s\">SSHelper</a> Version %s<br/>Copyright © 2018, <a href=\"%s\">P. Lutus</a>", urla, app.PROGRAM_VERSION, urlb);
            Spanned result;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(vs, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(vs);
            }
            value.setText(result);
            value.setMovementMethod(LinkMovementMethod.getInstance());
        }
        passwordEntry = (EditText) findViewById(R.id.server_password);
        if (passwordEntry != null) {
            // force a data update when the password entry is done
            passwordEntry.setOnEditorActionListener(
                    new EditText.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                    actionId == EditorInfo.IME_ACTION_DONE ||
                                    event.getAction() == KeyEvent.ACTION_DOWN &&
                                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                                if (event == null || !event.isShiftPressed()) {
                                    // the user is done typing.
                                    String oldPassword = app.systemData.config.serverPassword;
                                    if (!oldPassword.equals(v.getEditableText().toString())) {
                                        alertPasswordChange();
                                    }
                                    return false; // pass on to other listeners
                                }
                            }
                            return false; // pass on to other listeners
                        }
                    });
        }


        setupLogSpinner(app.logSpinner);
        if (app.logView != null) {
            setScrollable(app.logView);
        }
        //setupWebView();
        setCurrentTab();
        setupReentryServices(true);
        app.startService(true);
        app.restartServers(false);
        app.terminal = new ShellTerminal(this, app);
        testFileWriting();
    }

    protected void setupTextToSpeech() {
        // Initialize Text To speech
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                canSpeak = (status == TextToSpeech.SUCCESS);
                // app.logString("Entering OnInit: " + status, true);
                if (canSpeak) {
                    //Log.e("onInit", "CanSpeak");
                    tts.setLanguage(Locale.US);
                    //speak();
                }
            }
        });
    }

    public static class SpinnerActivity extends Activity implements OnItemSelectedListener {

        SSHelperActivity act = null;

        public SpinnerActivity(SSHelperActivity act) {
            this.act = act;
        }

        //@Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (pos != act.app.systemData.config.logLevel) {
                act.commitButton(view);
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Another interface callback
        }

    }

    private void setupLogSpinner(Spinner sp) {
        if (sp != null) {
            String[] items = new String[]{"SSH Server Normal", "SSH Server Debug 0", "SSH Server Debug 1",
                    "SSH Server Debug 2", "SSH Server Debug 3", "LogCat Fatal", "LogCat Error", "LogCat Warn",
                    "LogCat Info", "LogCat Debug", "LogCat Verbose"};
            List<String> list = new ArrayList<>(Arrays.asList(items));
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
            if (dataAdapter != null) {
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sp.setAdapter(dataAdapter);
                sp.setOnItemSelectedListener(new SpinnerActivity(this));
            }
        }
    }

    protected void buildWaitDialog(final String title, final String message) {
        app.needProgress = true;
        try {
            if (app.progress == null && app.activity != null) {
                Thread th = new Thread() {
                    public void run() {
                        if (app.needProgress) {
                            app.progress = new ProgressDialog(activity);
                            app.progress.setTitle(title);
                            app.progress.setMessage(message);
                            app.progress.setIcon(R.mipmap.ic_launcher_foreground);
                            app.progress.show();
                        }
                    }
                };
                runOnUiThread(th);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void killDialog() {
        app.needProgress = false;
        try {

            Thread th = new Thread() {
                public void run() {
                    if (app.progress != null) {
                        app.progress.dismiss();
                        app.progress.hide();
                        app.progress = null;
                    }
                }
            };
            runOnUiThread(th);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void setupReentryServices(boolean force) {
        if ((inForeground || force) && app.installed) {
            app.statusLEDIndex = -1;
            app.networkLEDIndex = -1;
            writeControls();

            // set up a delay repetition rate
            delayTimer = new Runnable() {
                public void run() {
                    app.updateLog(false);
                    if (app.logView != null) {
                        app.logView.update();
                    }
                    app.testNetworkState();
                    testServerStatus();
                    testNetwork();
                    setStatusLEDs();
                    updateNetworkAddress();
                    // the inForeground flag allows a graceful exit
                    if (inForeground && delayTimer == this) {
                        timerHandler.postDelayed(this, tickTimeMsec);
                    }
                }
            };
            inForeground = true;
            timerHandler.postDelayed(delayTimer, 0);
        }
    }

    protected void setScrollable(TextView tv) {
        tv.setMovementMethod(new ScrollingMovementMethod());
    }

    private int spinToInt(int id) {
        int v = 0;
        Spinner sp = (Spinner) findViewById(id);
        if (sp != null) {
            v = sp.getSelectedItemPosition();
            v = (v == Spinner.INVALID_POSITION) ? 0 : v;
            // app.logString2("spinToInt *** selection: " + v, true);
        }
        return v;
    }

    private void intToSpin(int id, int v) {
        Spinner sp = (Spinner) findViewById(id);
        if (sp != null) {
            sp.setSelection(v);
        }
    }

    private boolean cbToBool(int id) {
        CheckBox et = (CheckBox) findViewById(id);
        return et != null && et.isChecked();
        //if (et != null) {
        //    return et.isChecked();
        //} else {
        //    return false;
        //}
    }

    private void boolToCb(int id, boolean value) {
        CheckBox et = (CheckBox) findViewById(id);
        if (et != null) {
            et.setChecked(value);
        }
    }

    private String tcToString(int id) {
        EditText et = (EditText) findViewById(id);
        if (et != null) {
            return et.getEditableText().toString();
        } else {
            //Log.e("MISSING String Tag", String.format("%x", id));
            return null;
        }
    }

    private void stringToTc(int id, String data) {
        EditText et = (EditText) findViewById(id);
        if (et != null) {
            et.setText(data);
        }
    }

    private void stringToTv(int id, String data) {
        TextView et = (TextView) findViewById(id);
        if (et != null) {
            et.setText(data);
        }
    }

    private void intToTc(int id, int data) {
        EditText et = (EditText) findViewById(id);
        if (et != null) {
            et.setText(String.format(app.locale, "%d", data));
        }
    }

    private int tcToInt(int id) {
        String s = tcToString(id);
        return app.safeIntConverter(s);
    }

    private void alertPasswordChange() {
        FunctionInterface f = new FunctionInterface() {
            @Override
            public void yes_function() {
                stopServersAndQuit();
            }

            @Override
            public void no_function() {
                stringToTc(R.id.server_password, app.systemData.config.serverPassword);
                writeControls();
            }
        };
        app.actionDialog(this, "Password Has Changed",
                "To make the new password take effect on the native-language SSH server, please quit and restart SShelper." +
                        "\n\nPress OK to quit SSHelper (restart with program icon)." +
                        "\nPress Cancel to restore the prior password.", f);
    }

    private void readControls() {
        ConfigValues prefix = app.systemData.config;
        prefix.serverPassword = tcToString(R.id.server_password);
        prefix.ssh_server_port = tcToInt(R.id.ssh_port_number);
        prefix.log_server_port = tcToInt(R.id.log_port_number);
        prefix.clipboard_server_port = tcToInt(R.id.clipboard_port_number);
        prefix.runAtStart = cbToBool(R.id.run_at_start);
        // prefix.enableNetworkWhenRun = cbToBool(R.id.enable_network);
        prefix.checkNetwork = cbToBool(R.id.check_network);
        prefix.showIPV6Addresses = cbToBool(R.id.display_ipv6);
        prefix.disablePasswords = cbToBool(R.id.disable_passwords);
        prefix.allowForwarding = cbToBool(R.id.allow_forwarding);
        prefix.showPasswords = cbToBool(R.id.show_passwords);
        prefix.preventStandby = cbToBool(R.id.prevent_standby);
        // prefix.noRootLogin = cbToBool(R.id.no_root_login);
        prefix.allowVoiceMessages = cbToBool(R.id.allow_voice_messages);
        prefix.allowSoundAlerts = cbToBool(R.id.allow_sound_alerts);
        prefix.enableFileWriting = cbToBool(R.id.enable_file_writing);
        prefix.enableZeroconf = cbToBool(R.id.enable_zeroconf);
        prefix.enableLogServer = cbToBool(R.id.enable_log_server);
        prefix.enableClipboardServer = cbToBool(R.id.enable_clipboard_server);
        prefix.enableStrict = cbToBool(R.id.enable_strict);
        prefix.logLevel = spinToInt(R.id.log_spinner);
        updateConfigValues();
        serialize();
        testFileWriting();
    }

    private void writeControls() {
        ConfigValues prefix = app.systemData.config;
        stringToTv(R.id.device_name, app.deviceName);
        stringToTc(R.id.server_password, prefix.serverPassword);
        intToTc(R.id.log_port_number, prefix.log_server_port);
        intToTc(R.id.clipboard_port_number, prefix.clipboard_server_port);
        intToTc(R.id.ssh_port_number, prefix.ssh_server_port);
        updateNetworkAddress();
        // boolToCb(R.id.enable_network, prefix.enableNetworkWhenRun);
        boolToCb(R.id.check_network, prefix.checkNetwork);
        boolToCb(R.id.display_ipv6, prefix.showIPV6Addresses);
        boolToCb(R.id.run_at_start, prefix.runAtStart);
        boolToCb(R.id.disable_passwords, prefix.disablePasswords);
        boolToCb(R.id.allow_forwarding, prefix.allowForwarding);
        boolToCb(R.id.show_passwords, prefix.showPasswords);
        boolToCb(R.id.prevent_standby, prefix.preventStandby);
        // boolToCb(R.id.no_root_login, prefix.noRootLogin);
        boolToCb(R.id.allow_sound_alerts, prefix.allowSoundAlerts);
        boolToCb(R.id.enable_file_writing, prefix.enableFileWriting);
        boolToCb(R.id.allow_voice_messages, prefix.allowVoiceMessages);
        boolToCb(R.id.enable_zeroconf, prefix.enableZeroconf);
        boolToCb(R.id.enable_log_server, prefix.enableLogServer);
        boolToCb(R.id.enable_clipboard_server, prefix.enableClipboardServer);
        boolToCb(R.id.enable_strict, prefix.enableStrict);
        intToSpin(R.id.log_spinner, prefix.logLevel);
        updateConfigValues();
    }

    private void updateConfigValues() {
        if (app.logServerManager != null) {
            app.enableNetworkIfOption();
            if (app.zeroConf != null) {
                app.zeroConf.registerServices(app.systemData.config.enableZeroconf);
            }
            updateNetworkAddress();
            if (app.systemData != null && app.systemData.config != null) {
                ConfigValues prefix = app.systemData.config;
                showPasswordsControl(prefix.showPasswords);
                // app.setBootFlag();
                Window window = getWindow();
                if (window != null) {
                    if (prefix.preventStandby) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            }
            if (app.systemData != null && app.systemData.config != null) {
                app.logServerManager.startServer(app.systemData.config.log_server_port);
                app.clipServerManager.startServer(app.systemData.config.clipboard_server_port);
                if (app.systemData.config.logLevel != oldLogLevel) {
                    oldLogLevel = app.systemData.config.logLevel;
                    app.oldLogLength = -1;
                    if (app.systemData.config.logLevel >= 5) {
                        // restart reader
                        app.logCatReader();
                    } else {
                        if (app.logCatProcess != null) {
                            app.logCatProcess.destroy();
                            app.logCatProcess = null;
                        }
                    }
                }
            }
        }
    }

    private void updateNetworkAddress() {
        String ip = app.getCurrentIPAddress();
        if (!ip.equals(app.currentIPAddress)) {
            // updateNotification();
            app.testNetworkState();
            app.currentIPAddress = ip;
        }
        String sip = app.getCurrentIPAddress();
        if (sip == null || sip.length() == 0) {
            sip = "None";
        }
        stringToTv(R.id.server_address, sip);
        NetworkInfo ni = app.connectivityManager.getActiveNetworkInfo();
        String netName = "None";
        if (ni != null) {
            netName = ni.getTypeName();
        }
        stringToTv(R.id.network_type, netName);
    }

    public void commitButton(View view) {
        commitAction();
        app.controlServer(true);
    }

    private void commitAction() {
        ConfigValues v = app.systemData.config.clone();
        if (configUndoStack != null) {
            configUndoStack.push(v);
        }
        readControls();
        updateNotification();
    }

    private void updateNotification() {
        if (app.helperService != null) {
            app.helperService.updateNotification();
        }
    }

    public void restartServerButton(View view) {
        // app.logString("restart server button ", true);
        restartServer();
    }

    public void reinstallFiles(View view) {

        final FunctionInterface f = new FunctionInterface() {
            @Override
            public void yes_function() {
                try {
                    // create reinstall flag file
                    File rf = new File(app.homeDir + "/REINSTALL");
                    new FileOutputStream(rf).close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopServersAndQuit();
            }

            @Override
            public void no_function() {

            }
        };
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                app.actionDialog(activity, "Reinstall SSHelper Files", "This action reinstalls all SSHelper binaries and libraries, but won't disturb user data. After this dialog, SSHelper will quit -- please restart SSHelper to continue.", f);
            }
        });
        // app.logString("restart server button ", true);
        //restartServer();

    }

    private void restartServer() {
        final FunctionInterface f = new FunctionInterface() {
            @Override
            public void yes_function() {
                app.restartServers(true);
            }

            @Override
            public void no_function() {

            }
        };
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                app.actionDialog(activity, "Restart Servers", "This action restarts all SSHelper servers, which will interrupt any transactions in progress. Proceed?", f);
            }
        });

    }

    public void cancelButton(View v) {
        if (configUndoStack != null && configUndoStack.size() > 0) {
            app.systemData.config = configUndoStack.pop();
            writeControls();
        } else {
            alertUser("No Undo is available.", true);
        }
    }

    public void defaultsButton(View view) {
        final FunctionInterface f = new FunctionInterface() {
            @Override
            public void yes_function() {
                ConfigValues v = app.systemData.config.clone();
                configUndoStack.push(v);
                app.systemData.config = new ConfigValues();
                writeControls();
            }

            @Override
            public void no_function() {

            }
        };
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                app.actionDialog(activity, "Set Configuration Defaults", "This action resets all configuration options to their default values, including the access password. Proceed?", f);
            }
        });

    }

    public void showPasswordsControl(boolean show) {
        for (int i : new int[]{R.id.server_password}) {
            EditText et = (EditText) findViewById(i);
            if (et != null) {
                et.setInputType(InputType.TYPE_CLASS_TEXT
                        | ((show) ? InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (activity == null) {
            return false;
        }
        int id = item.getItemId();
        // app.logString(tag + " " + id,true);
        switch (id) {
            case android.R.id.home:
                // payItForward();
                break;
            case R.id.restart_server:
                restartServer();
                break;
            case R.id.show_help:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://arachnoid.com/android/SSHelper"));
                startActivity(browserIntent);
                break;
            case R.id.toggle_keyboard:
                toggleSoftKeyboard();
                break;
            case R.id.stop_and_quit:
                stopServersAndQuit();
                break;
        }
        return true;
    }

    protected void stopServersAndQuit() {
        //app.debugLog("activity:stopServersandquit","");
        readControls();
        serialize();
        app.stopServers = true;
        app.stopService();
        app.stopServers();
        app.killPriorSSHServersClients();

        finish();
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    // various methods to improve application state integrity

    @Override
    public void onResume() {
        super.onResume();
        //app.activity = this;
        afterResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        //setupReentryServices(true);
        //app.activity = this;
    }

    @Override
    public void onSaveInstanceState(Bundle data) {
        super.onSaveInstanceState(data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //app.debugLog("activity:ondestroy","");
        inForeground = false;
        // end of this view
        app.restartServers(false);
        // if (multiCastLock != null)
        // multiCastLock.release();
        exitCleanup(false);
        //app.activity = null;
        //activity = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        //app.debugLog("activity:onpause","");
        inForeground = false;
        serialize();
        //app.activity = null;
        app.restartServers(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        //app.debugLog("activity:onstop","");
        inForeground = false;
        serialize();
        //app.activity = null;
        app.restartServers(false);

    }

    private void exitCleanup(boolean stopServer) {
        //app.debugLog("activity:exitCleanup","stopserver: " + stopServer);
        // signal this view's timer to exit
        inForeground = false;
        if (stopServer) {
            if (app.runSSH != null) {
                app.killPriorSSHServersClients();
            }
        }
        // if (app.logBuffer != null) {
        // app.logContent = app.logBuffer.toString();
        // }
        // alertUser("goodbye.", true);
        ttsShutdown();
        serialize();
    }

    private void serialize() {
        //TabHost tabh = app.tabHost;
        //if (tabh != null) {
        app.systemData.currentTab = mViewPager.getCurrentItem();
        app.serialize();
        //Log.e("Monitor", "Serialized.");
    }

    private void setCurrentTab() {
        if (app.systemData != null) {
            if (app.systemData.currentTab != -1) {
                //TabHost tabh = app.tabHost;
                mViewPager.setCurrentItem(app.systemData.currentTab);
            }
        }
    }

    protected void setCurrentTab(int tab) {
        mViewPager.setCurrentItem(tab, true);
    }

    private void testNetwork() {
        if (app.runSSH != null) {
            if (!app.runSSH.networkDialogShown) {
                if (app.systemData.config.checkNetwork && !app.getNetworkEnabled()) {
                    String msg = "No network connection is available (this may be temporary).\nTo correct this, enable a network with Android settings.\nTo hide this dialog, disable configuration option \"Check Network connectivity\".";
                    app.messageDialog(activity, "No Network Connection", msg);
                    alertUser("Network connection disabled", false);
                    app.runSSH.networkDialogShown = true;
                }
            }
        }
    }

    private void testServerStatus() {
        if (app.runSSH != null) {
            if (!app.runSSH.dialogShown) {
                if (!(app.serverRunning() || app.runSSH.starting)) {
                    String msg;// = "";
                    if (app.runSSH.addressInUse) {
                        msg = "Another helperService is preventing use of port " + app.systemData.config.ssh_server_port
                                + ".\nTo try to solve this problem, click the \"Restart\" menu item.";
                        alertUser("selected port is in use.", false);
                    } else {
                        msg = "The server is not running. To fix this, click the \"Restart\" menu item.";
                        alertUser("the server is not running.", false);
                    }
                    String title = "Server Initialization Problem";
                    app.messageDialog(activity, title, msg);
                    app.runSSH.dialogShown = true;
                } else {
                    if (app.serverRunning()) {
                        if (app.restartingServers) {
                            alertUser("Server is running.", true);
                            app.restartingServers = false;
                        }
                        app.runSSH.dialogShown = true;
                    }
                }
            }
        }
    }

    private void setStatusLEDs() {
        int index;// = 0;
        if (app.serverRunning()) {
            index = 2;
        } else {
            index = 1;
        }
        // if (index != app.statusLEDIndex) {
        app.statusLEDIndex = index;
        TextView sb = (TextView) findViewById(R.id.status_text);
        if (sb != null) {
            // sb.setCompoundDrawablesWithIntrinsicBounds(ledIconColors[index],
            // 0,
            // 0, 0);
            sb.setTextColor(textColors[index]);
            sb.invalidate();
        }
        // }
        // network connectivity
        index = (app.getNetworkEnabled()) ? 2 : 0;
        // if (index != app.networkLEDIndex) {
        app.networkLEDIndex = index;
        sb = (TextView) findViewById(R.id.status_network);
        if (sb != null) {
            // sb.setCompoundDrawablesWithIntrinsicBounds(ledIconColors[index],
            // 0,
            // 0, 0);
            sb.setTextColor(textColors[index]);
            sb.invalidate();
        }
        // }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && currentTab.equals("Help")) {
            if (helpWebView.canGoBack()) {
                helpWebView.goBack();
            } else {
                // alertUser("Cannot go back.", true);
                setCurrentTab(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void toggleSoftKeyboard() {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

    }

    public void toggleSoftKeyboard(View v) {
        toggleSoftKeyboard();
        dismissTermDialog(v);
    }

    public void pasteClipboard(View v) {
        if (app.terminal != null) {
            app.terminal.pasteClipboard();
        }
    }

    public void startTerminal(View v) {
        if (app.terminal != null) {
            app.terminal.startTerminal(true);
        }
    }

    public void stopTerminal(View v) {
        if (app.terminal != null) {
            app.terminal.stopTerminal();
        }
    }

    public void setTerminalWhiteMode(View v) {
        if (app.terminal != null) {
            app.terminal.setColorScheme(false);
        }
    }

    public void setTerminalBlackMode(View v) {
        if (app.terminal != null) {
            app.terminal.setColorScheme(true);
        }
    }

    public void dismissTermDialog(View v) {
        if (app.terminal != null) {
            app.terminal.dismissDialog();
        }
    }

    public void terminalNotes(View v) {
        String message = "This is a basic terminal with few features.\n"
                + "For much better results users are encouraged to\n"
                + "use a dedicated Android terminal program\n"
                + "or a Secure Shell session on a networked system.";
        app.messageDialog(activity, "Terminal Notes", message);
    }

    public void dismissLogDialog(View v) {
        if (app.logView != null) {
            app.logView.dismissDialog();
        }
    }

    public void copyLogPlaintext(View v) {
        app.copyLog(false);
        dismissLogDialog(v);
    }

    public void copyLogHTML(View v) {
        app.copyLog(true);
        dismissLogDialog(v);
    }

    public void eraseLog(View v) {
        app.eraseLog();
        dismissLogDialog(v);
    }


    protected void alertUser(final String s, boolean toastBackup) {
        if (app.systemData.config.allowSoundAlerts) {
            try {
                if (app.systemData.config.allowVoiceMessages) {
                    speakMessage = s;
                    if (canSpeak) {
                        speak();
                    } else {
                        //Log.e("alertUser","Entering setup.");
                        Intent checkIntent = new Intent();
                        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
                    }
                } else {
                    if (toastBackup) {
                        app.beep();
                        app.makeToast(s);
                    }
                }
            } catch (Exception e) {
                //Log.e("alertUser","Error: " + e.getMessage());
                app.beep();
                app.makeToast(e.getMessage());
            }
        }
    }

    protected void speak() {
        if (canSpeak && tts != null) {
            //tts.speak(speakMessage, TextToSpeech.QUEUE_ADD, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(speakMessage, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                tts.speak(speakMessage, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }


    protected void ttsShutdown() {
        //Log.e("ttsShutdown","Entering ttsShutdown.");
        if (canSpeak && tts != null) {
            try {
                tts.stop();
                tts.shutdown();
                tts = null;
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        speakThread = null;
    }

    protected void setFileWritePermissions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void testFileWriting() {
        FunctionInterface fi = new FunctionInterface() {
            public void yes_function() {
                setFileWritePermissions();
            }

            public void no_function() {
                app.systemData.config.enableFileWriting = !app.systemData.config.enableFileWriting;
                writeControls();
            }
        };
        boolean canWrite = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!app.systemData.config.enableFileWriting && canWrite) {
            app.actionDialog(this, "Disable File Writing", "Current Android settings enable file writing for SSHelper, and disabling requires a user action. In the next screen, choose \"Permissions\" and disable the \"Storage\" option. Press OK to continue, Cancel to revert to the original state.", fi);
        } else if (app.systemData.config.enableFileWriting && !canWrite) {
            app.actionDialog(this, "Enable File Writing", "Current Android settings disable file writing for SSHelper, and enabling requires a user action. In the next screen, choose \"Permissions\" and enable the \"Storage\" option. Press OK to continue, Cancel to revert to the original state.", fi);
        }
    }


    /**
     * Returns a new instance of fragment for the given section
     * number.
     */
    public PlaceHolderFragment newFragment(int sectionNumber) {
        //Log.e("newFragment: ", "" + sectionNumber);
        PlaceHolderFragment fragment = new PlaceHolderFragment();
        Bundle args = new Bundle();
        args.putInt("index", sectionNumber);
        // always use default setArguments()
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment.
            return newFragment(position);
        }

        @Override
        public int getCount() {
            // Total pages in tab list
            return 3;
        }

        // These are the tab labels
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Log";
                case 1:
                    return "Configuration";
                case 2:
                    return "Terminal";

            }
            return null;
        }

    }


}