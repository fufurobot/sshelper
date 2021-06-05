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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.JobIntentService;
//import android.util.Log;

final public class SSHelperService extends JobIntentService {
    SSHelperApplication app;
    private static final int HELLO_ID = 111;
    Notification notification = null;
    NotificationManager notificationManager = null;
    public static final int JOB_ID = 0x01;
    String CHANNEL_ID = "My Channel";
    Intent myIntent = null;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SSHelperService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // not used
    }

    @Override
    public IBinder onBind(Intent intent) {
        app.debugLog("Monitor", "SSHelperService:onBind intent: " + intent);
        // must copy this process' intent
        myIntent = intent;
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = (SSHelperApplication) getApplication();
        app.debugLog("Monitor", "SSHelperService:onCreate call myintent: " + myIntent);
        new WaitInstall().start();
    }

    private class WaitInstall extends Thread {

        public void run() {
            while (!app.installed) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            postInstall();
        }
    }

    private void postInstall() {
        app.helperService = this;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        app.restartServers(false);
        updateNotification();
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && myIntent != null) {
            startForegroundService(myIntent);
        }
        startForeground(HELLO_ID, notification);
    }

    private int chooseIcon() {
        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        return useWhiteIcon ? R.drawable.ic_action_name : R.mipmap.ic_launcher_foreground;
    }

    public void updateNotification() {
        Context context = app;
        String ip = app.getCurrentIPAddress();
        //String infoStr = "SSHelper is " + ((!ip.equals(""))?"available":"disabled");
        String sip = app.getCurrentIPAddress();
        String port = "" + app.systemData.config.ssh_server_port;
        CharSequence contentTitle = "SSHelper is " + ((!ip.equals("")) ? "available" : "disabled");
        CharSequence contentText = "Monitoring " + sip + ":" + port;
        Intent nIntent = new Intent(context, SSHelperActivity.class);
        nIntent.setAction(Intent.ACTION_VIEW);
        nIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, nIntent,
                0);
        if (notification == null) {
            Notification.Builder nb = new Notification.Builder(context);
            nb.setContentIntent(pIntent);
            nb.setContentText(contentText);
            nb.setContentTitle(contentTitle);
            nb.setWhen(System.currentTimeMillis());
            nb.setSmallIcon(chooseIcon());
            nb.setOngoing(true);
            nb.setAutoCancel(true);
            notification = nb.build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "My Channel", importance);
                notificationManager.createNotificationChannel(mChannel);
                nb.setChannelId(CHANNEL_ID);
            } else {
                notificationManager.notify(HELLO_ID, notification);
            }
        }

    }

    public void stopNotifier() {
        app.debugLog("Error Log...", "SSHelperService:stopNotifier call");
        notificationManager.cancel(HELLO_ID);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app.debugLog("Monitor", "SSHelperService:onDestroy call");
        app.controlServer(false);
        //app.helperService = null;

    }


}
