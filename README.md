Loyal users, SSHelper is going away — not because of issues with SShelper, but because of changes in Android from version 10 and newer. Over the next few years, in a future Android version past 10, SSHelper will likely stop functioning entirely. SSHelper version 13.2 (available on this page) will probably be the last version, simply because future Android versions will not allow it to run.

Here's a recent conversation with a user that explains the changes:

Last SSHelper Version
Paul: I just read your main page, at the bottom, where you announce the final version of SSHelper. I am so sorry to hear that. I can only imagine the frustration you must be feeling!
Yes, it's frustrating, but I understand where Google is coming from, and their priorities. Details below.
I have successfully reinstalled your version 130 and it's working again. If I may impose, perhaps one last time, on behalf of all SSHelper users (when you get a chance, obviously):

1) Can you move your "final message" to the top of the page to make it more visible?
I'm planning that. I plan to say I understand and appreciate Google's position. They need to tighten up security to stay ahead of the black-hats who see Android as a way to access the mobile resources of Android's users — personal information, bank accounts, and so forth — users who aren't necessarily technical but who have valuable resources located on their devices and who rely on the security and integrity of this mobile platform. Google also has to compete with Apple's iOS platform, which seems much more secure.

So a move away from an open environment is inevitable. I should add that Termux is basically saying the same thing I am — they see this as the end of their current operational model as well, and who are also giving up.

The linked article — and others — explain that in Android 10+, (a) executable binaries can no longer reside in a read/write directory, and (b) access to /sdcard will go away. Simply put, these changes destroy my application's ability to function, and that of Termux as well.
2) If you have any documentation about how to rebuild the package from the source tarball that you've provided (that is not already in the tarball), could you post that, too? (I've never built an apk)
My application is open-source, but it's not that simple — SSHelper isn't just an APK compilation. It's a merge of several binary (i.e. non-Java) projects, written to accommodate multiple platforms and word sizes, plus front-end Java code suitable for a mainstream Android application that calls the binaries to do the heavy lifting.
3) Do you have any suggestions of "competitors" for SSHelper which may provide some of the functionality in a different way?
I would have suggested Termux, a better and much more complete environment than SSHelper, with many more details fully fleshed out like a package manager — but all are going away now. To read more on this topic, search online for "Termux Android 10".

Again, Termux (terminal, full Linux utilities and a package manager) is going away, and AndroNix (choice of GUI desktop environments, relies on Termux), and every other app that offers a Linux-style terminal and utilities — all are going away.
4) Do you have any further advice for those of us who need to be able to upload/download files and manipulate them from the command line?
Yes, I do — remove the external storage device from your Android unit, write to it, and put it back. But even that access will eventually go away, also many Android devices don't even have an external storage device, for security and other reasons.

Also, if Google completely controls Android storage access for security reasons, then they can sell you the music you already own, which means there's more than one way to look at this change. Or Google could install a photo processing app that puts everything in the cloud while it refuses to store the data locally.
5) I've never rooted a phone (my current Pixel 3a is my second phone). Do you have any advice about that? I just can't stomach a world in which I can't use a command line to upload and download my data (photos, pdfs, mp3s, etc) from my own phone.
I don't even want to suggest how to go about rooting your device. There are many tutorials, but I predict all will be disabled one by one in coming years as security tightens.
6) What about buying future phones (i.e. Android 11+) where this functionality is no longer supported, even in a non-compliant app? Is rooting the only choice? I can't imaging Apple is much better, or even as good as this, in terms of user freedom.
No, Apple's so-called "walled garden" is designed for people for whom personal security has a higher priority than easy access and Linux-like freedoms. Google wants to catch up with Apple in this respect.

But there's more to this, it's more complicated. In the Big Picture, Google has every incentive to make these changes — they lead to more security, and they're aligned with Google's corporate goals as well.
When talking to users, Google will emphasize control over hackers.
When talking to stockholders, Google will emphasize control over users.
Thank you so much for all your service to the community!
You're welcome.

Digression — when I started programming in the mid-1970s, if I had been told about the present, I would have laughed in disbelief. In those times I was able to write a single application (Apple Writer, written in meticulously hand-coded assembly language) that ran worldwide on any number of identical computers (Apple IIs), all in exactly the same way. If a user from a small French village reported a bug, I knew every other installation would have the same bug, so a single code change would resolve the issue worldwide. It was a much simpler time.

In those days 32 kilobytes was a lot of memory, only the best Apple II machines had that much. When Tom Clancy wrote "Hunt for Red October" on Apple Writer (1984), each of his chapters had to fit into 24 kilobytes (my program used the remaining 8 kilobytes). Take a deep breath and say, "24 kilobytes."

Thanks for writing.
Introduction
The Program

SSHelper is an advanced, multi-protocol, secure server for the Android platform.
SSHelper requires Android version 5.0, API 21, or newer.
SSHelper works just fine with a normal, unrooted Android device — i.e. your device.
SSHelper works as an application and as a service. As a service it runs in the background, providing secure communications protocols without requiring user attention.
SSHelper supports interactive Secure Shell (hereafter SSH) sessions and various kinds of transfers including scp, sftp, and rsync, on all common platforms:
On Windows, SSHelper works with WinSCP, PuTTY and similar programs.
On Linux, apart from normal SSH shell session activities, SSHelper can be used directly with file browsers for seamless filesystem browsing and transfers by specifying the "sftp:" protocol.
On the Mac, SSHelper works with Cyberduck and similar programs.
SSHelper announces its services on your local network by way of Zeroconf/Bonjour, a simple and efficient way to configure, and communicate within, a network.
SSHelper hosts an activity log Web server that gives a remote view of the activity log — useful for administration and development.
SSHelper also hosts a clipboard access Web server that provides read/write access to the Android clipboard from any browser on the local network.
SSHelper is free, open-source and there are no ads. It's licensed under the GPL and the source is freely available.
The Programmer

Hello — I'm Paul Lutus, author of SSHelper. I've been writing software for about 35 years and have written some very well-known titles (Apple Writer, Arachnophilia). SSHelper is my first Android application — I wrote it simply because there were no Android programs that could give me what I needed. Existing programs were missing features and protocols, primarily support for rsync, a very efficient way to synchronize two directory trees.

I hope you like SSHelper. And stay tuned — I'll be writing more Android programs in the future.







SSHelper is Copyright 2018, P. lutus and is released under the GPL.

SSHelper uses code from the following projects:

OpenSSH — license
OpenSSL — license
BusyBox — license
Rsync — license
JmDNS — license
Here is a source archive for SSHelper, organized as an Android Studio project.

Version History (reverse chronological order)

08.26.2020 Version 13.2. Revert to Android 9 (API 28). To my shock and dismay, some changes have been made in Android 10 that will destroy the fundamental principles on which this application is built: (a) no more access to a directory of executable binary files such as one sees in Linux and other modern operating systems, and (b) no more access to the user data area /sdcard. Without this access, nothing works — the SSH server cannot function, and SSHelper users cannot access the primary data storage area as is true in all modern, civilized operating systems. Therefore, this is likely the last version of SSHelper. Users who want continued access to this application need to visit this page, the SSHelper Home Page, to pick up this version or any prior releases they may need for older Android devices. Because of this reversion to API 28, SSHelper will probably vanish from the Google Play Store within 60 days at the most. And in a future Android version past 10, even this SSHelper version will likely stop functioning.

For more detail, read the "Major Development" section at the top of this page.

08.25.2020 Version 13.1. Mandatory retargeting to Android 10 (API 29) (failed).
10.01.2019 Version 13.0. Recoded the terminal module for better behavior, but users should consider alternatives for this function, and see the SSHelper FAQ for usage specifics.
09.01.2019 Version 12.9. After field testing and user reports that show compatibility only with Android versions ≥ 7, changed minimum compatible Android version to 7. Users of earlier Android versions are encouraged to download and install from my release archive located at https://arachnoid.com/android/SSHelper .
07.24.2019 Version 12.8. Added android.permission.FOREGROUND_SERVICE, apparently required by Android versions 9+.
07.16.2019 Version 12.7. Revised SSHelper for compatibility with the most recent Android version (9).
10.29.2018 Version 12.5. Changed shutdown procedure to avoid a unfixed inconsistency in Android that would prevent a reliable shutdown.
09.08.2018 Version 12.4. Recreated all binaries and libraries for greater efficiency, fixed a few small bugs.
09.04.2018 Version 12.3. Solved a persistent application/dialog icon issue, made some layout changes to accommodate Android updates and small displays.
09.03.2018 Version 12.2. (1) Added more utility programs (htop, tracepath, tree). (2) After user correspondence and research, reluctantly changed the minimum acceptable Android version to 5.0 after realizing the new required compilation method (dynamic libraries) prevents SSHelper from working on Android versions < 5.0. This change only acknowledges reality, it doesn't exclude older devices — the new code requirements do that.
09.03.2018 Version 12.1. Corrected a path assignment critical to allowing Android-native utilities (example: df) to work within SSHelper as they should.
09.02.2018 Version 12.0. SSHelper is now a dual-platform application — it automatically installs the appropriate native-code binaries on 32 and 64-bit ARM processors. Included are more command-line utilities such as bash, nano, file and strace. Secure Shell logon sessions and the terminal utility now use the included bash shell for better performance. A new manual-reinstall command replaces all the binary executables and libraries to assure that everything is up to date and correctly configured. A number of past annoying behaviors have been analyzed and corrected.
08.30.2018 Version 11.9. Revised code to deal with installation issues, added Bash shell interpreter, updated built-in terminal code.
08.29.2018 Version 11.8. Changed installation code to fully remove prior install libraries/binaries, added "nano" command-line text editor, fixed some small bugs.
08.27.2018 Version 11.7. Fixed a small bug related to recent major changes. NOTE: For some devices a complete uninstall and reinstall is needed to restore correct operation.
08.27.2018 Version 11.5. This is a major code update to accommodate Android 9's new requirements. NOTE: For some devices a complete uninstall and reinstall is needed to restore correct operation.
08.14.2018 Version 11.4. Corrected an error in the type of the application's displayed icon.
08.12.2018 Version 11.3. Fixed a bug that sometimes prevented clean reinstalls of prior SSHelper versions.
08.11.2018 Version 11.2. Restored operation of the rsync file transfer utility. To get this change in behavior, some users may need to fully uninstall and reinstall SSHelper.
08.08.2018 Version 11.1. Fixed a problem with the run-at-boot feature on certain Android versions.
08.07.2018 Version 11.0. Created special notification handling scheme required by Android versions 8.0+.
08.07.2018 Version 10.9. Refactored the server entry/exit code for more consistency between Android versions.
08.06.2018 Version 10.8. Cleaned up some code areas that produced different behaviors on different Android versions.
08.06.2018 Version 10.7. Fixed a configuration error (no WAKE LOCK permission) that caused a failure on some older Android versions.
08.04.2018 Version 10.6. Revised startup code to allow SSHelper to autostart and run in the background on Android 8.0+ versions.
07.25.2018 Version 10.5. Changed the search order in the default system PATH variable to resolve issues in newer Android versions that have overlapping commands originally only handled by SSHelper. This change makes SSHelper work better in all Android versions, but it doesn't solve the serious rsync problems caused by Android 8.0+, for which there may be no solution (sftp and other modes still work).
10.26.2017 Version 10.3. Fixed an annoying password-change issue. In the new version, a password change immediately launches a dialog that explains what must be done, then does it for you.
10.18.2017 Version 10.2. Based on field reports, added more remedies for runtime errors in some Android devices.
10.15.2017 Version 10.1. Based on field reports, added more safeguards against out-of-bounds and error conditions.
10.13.2017 Version 10.0. Changed the OpenSSH source for executable "sshd" to address an issue with Android 8.0. Unfortunately, because I don't have an install of Android 8.0 on any of my Android devices, I can't test this change on the intended target. I must rely on my loyal users to tell me if this change has solved a reported issue.
10.10.2017 Version 9.9. Upgraded the OpenSSH configuration to default to the RSA encryption key type, stopped automatically generating the now-abandoned and insecure DSA key type during installation.
10.09.2017 Version 9.8. Changed the activity creation process to avoid interrupting ongoing transactions, added a confirmation dialog on server restarts for the same reason.
10.09.2017 Version 9.7. Added a few more error guards against null pointers.
10.09.2017 Version 9.6. Fixed some miscellaneous bugs and out-of-bounds conditions reported by users.
10.03.2017 Version 9.4. Restored the ability to run at boot, a feature temporarily lost during the many recent changes.
10.02.2017 Version 9.3. After some user requests and my own experience, decided to abandon the new light theme and return to a dark application theme — much nicer.
09.29.2017 Version 9.2. A major interface overhaul and some new features. This version uses the new "Fragments" layout and color scheme. There's a new option for the user to change SShelper's Android write permissions from within the app, as well as a configuration option to disable file writing if the user prefers. A number of code changes increase reliability during an activity's lifecycle. There remains a problem with some Android devices running Oreo (i.e. 8.0), one I won't be able to fix until one of my four Android devices gets the update.
08.05.2017 Version 9.1. Created an rsync binary based on the most recent source (i.e. rsync 3.1.2) that should work on all Android versions that SSHelper supports. This should resolve the "Kernel too old" error messages reported by some users.
07.23.2017 Version 9.0. Reluctantly gave up on the idea of creating an rsync binary that will work on all Android versions and kernels. Returned to rsync version 3.1.2, the most recent version at the time of writing. This rsync binary will not work on all Android devices, but it will work on the majority of them. This update also addresses some important security issues.
07.22.2017 Version 8.9. Reverted to previous tested rsync version (3.0.9) after receiving many user bug reports that the newest rsync version (3.1.2) fails on some older Android kernels. Some users of a prior version may need to fully uninstall and reinstall SSHelper to enable the above-described rsync binary version change.
05.05.2017 Version 8.8. Updated included rsync binary to the most recent version, to support new features.
04.19.2017 Version 8.7. Changed development environments (from Eclipse to Android Studio), cleaned up many small code issues the new environment revealed.
04.18.2017 Version 8.6. Fixed a keyboard-response problem in the virtual terminal, added the ability to copy clipboard contents to terminal.
03.08.2016 Version 8.5. Updated to the most recent OpenSSL version (1.02g). This is a precautionary security update.
03.03.2016 Version 8.4. Corrected an important oversight and added a META header to the clipboard Web page to support UTF-8 content.
01.29.2016 Version 8.3. Updated OpenSSL to version 1.02f to acquire recent security-related bug fixes.
01.19.2016 Version 8.2. Fixed a bug in the Zeroconf class that sometimes prevented retirement of an old IP address.
01.18.2016 Version 8.1. Revised the Zeroconf algorithm to make it more responsive to IP changes that result from changing networks.
01.15.2016 Version 8.0. Updated to most recent OpenSSH version (7.1p2) in response to client bug vulnerability report in versions 5.4 - 7.1.
10.30.2015 Version 7.9. Fixed a bug that caused SSHelper to unceremoniously exit when another application tried to manipulate the network connection.
09.25.2015 Version 7.8. Updated network navigation code to use whichever network type the user has selected or has available — it's no longer limited to wireless networks. Any network type recognized by Android will also be used by SSHelper.
05.07.2015 Version 7.7. Updated OpenSSL to most recent version (1.02a).
05.05.2015 Version 7.6. Fixed a cursor-tracking bug in the terminal emulator.
04.24.2015 Version 7.5. Added code to update system log if HTTP log server begins serving requests, added two precautionary null-value pointer tests.
04.24.2015 Version 7.4. Fixed a bug that updated user interface elements when they weren't visible — this change significantly decreases SSHelper's overall system load.
04.17.2015 Version 7.3. Recoded the shell launch protocol for more flexibility and uniformity between user and superuser (both now read the user-level ~/.profile), fixed a bug in the text-to-speech code.
04.13.2015 Version 7.2. Restored color-keyed directory listings in interactive shell sessions.
04.13.2015 Version 7.1. Solved a longstanding bug that prevented interactive shell sessions. A side effect of this solution is that SSHelper's terminal tab works once again. Users who want the full effect of this fix (including a new default login procedure) should uninstall and reinstall SSHelper.
03.20.2015 Version 7.0. Security update — recompiled this project using the most recent OpenSSL library version, 1.0.2a, which fixes a number of significant vulnerabilities. After updating your copy of SSHelper, to confirm which OpenSSL version you have, initiate an SSHelper shell session and type "ssh -V".
02.15.2015 Version 6.9. Solved the Android 5.0 position-independent executable problem by providing two binaries, one for Android versions ≥ 5.0, which require that executables be position-independent, and versions < 4.1, which require that executables not have this feature. The installer decides which binary to use based on the Android version number.
10.09.2014 Version 6.8. Included more proactive safeguards against crashes.
09.29.2014 Version 6.7. Recoded the service notifier to reflect SSHelper's current running state.
09.10.2014 Version 6.6. Fixed a few small bugs in the new clipboard server feature.
09.09.2014 Version 6.5. Added a clipboard server that provides read/write access to the Android clipboard from any browser on the local network.
08.15.2014 Version 6.4. Updated to the most recent OpenSSL version (1.01i) for security purposes.
04.28.2014 Version 6.3. Reworked layout for better function and appearance on small displays.
04.11.2014 Version 6.2. This version of SSHelper incorporates the fix for the heartbleed bug in OpenSSL. After installing, to verify that your SSHelper version has the fix, issue this command: "ssh (device name or IP) ssh -V". The reply should be "OpenSSH_6.6p1, OpenSSL 1.0.1g 7 Apr 2014". Note the OpenSSL version 1.01g, which has the fix.
04.05.2014 Version 6.1. Corrected a user-reported bug that prevented installation in some circumstances.
04.05.2014 Version 6.0. Disabled debug logging to a log file inadvertently left enabled in recent changes.
04.04.2014 Version 5.9. Updated to the most recent version of OpenSSH, which fixes some security issues.
04.03.2014 Version 5.8. Added some safeguards against a user-reported NPE during installation.
04.02.2014 Version 5.7. Further optimized the terminal emulator, changed the cross-compilation processor identity and recompiled all the native-code binaries to avoid a potential processor conflict.
04.01.2014 Version 5.6. Rewrote the terminal emulator for greater speed and responsiveness. The emulator still won't work on Samsung devices with Knox installed, and a desktop shell session is more effective, but the emulator is handy at times.
03.30.2014 Version 5.5. Fixed configuration issue that prevented SSH compression from working.
03.27.2014 Version 5.4. Arranged to apply the device's user-defined name for the Zeroconf network name if it's been defined.
03.27.2014 Version 5.3. Revised the Zeroconf / MDNS code block to allow local-network recognition using the (device name).local addressing convention.
03.24.2014 Version 5.2. Changed the naming convention for Zeroconf/Bonjour hostnames to conform to prevailing standards.
03.03.2014 Version 5.1. Recoded a number of controls and methods to accommodate small displays like cell phones.
02.17.2014 Version 5.0. Assigned the Zeroconf devices more distinct names (the device model name) to help in sorting out devices on busy networks.
11.17.2013 Version 4.9. Gave Busybox the ability to translate Internet names into addresses (i.e. DNS). This means network-aware Busybox applets (i.e. telnet, wget, etc.) will accept host names and "do the right thing." Improved the app's overall performance through fine tuning.
11.13.2013 Version 4.8. Fixed an error in the path assignment sequence that would have prevented a remote "scp" invocation from functioning correctly.
11.12.2013 Version 4.7. Changed the shell launch code in response to user reports, with the aim of making SSHelper function with as many Android devices and OS versions as possible.
11.12.2013 Version 4.6. Revised the user/superuser launch code in response to user reports, optimized the HTTP server to run faster so log displays don't flicker, changed the default search path to accommodate a wider array of Android devices.
11.11.2013 Version 4.5. Improved the default shell environment and how it's launched. Added more environment variables to tune shell sessions.
11.11.2013 Version 4.4. Fixed a reported logon bug, rewrote the HTTP server for more responsiveness, cleaned up other small bugs.
11.10.2013 Version 4.3. Remapped the binary directory, changed the superuser setup, decided to use the Busybox shell (color-coded directory listings etc.), arranged that the configuration file ".profile" is always read in both user and superuser modes.
11.09.2013 Version 4.2. Recoded the superuser launch routine for greater compatibility with different rooting packages.
11.09.2013 Version 4.1. Added color display to the activity log, added an option to copy the log to the system clipboard, increased the number of explanatory dialogs, recoded the superuser launch procedure.
11.08.2013 Version 4.0. Revised the logging facility to show either the SSH server events or the Android "logcat" output, both with selectable levels of detail.
11.07.2013 Version 3.9. Refactored some code to accommodate different root packages as part of the superuser feature.
11.06.2013 Version 3.8. Made small changes to accommodate superuser access on a rooted Android device. These changes have no effect on normal activities, but to apply the changes, users need to fully uninstall and reinstall SSHelper.
11.05.2013 Version 3.7. Fixed the Zeroconf registration code to avoid multiple registrations.
11.05.2013 Version 3.6. Recoded the full-shutdown option, which had stopped working during recent changes.
11.05.2013 Version 3.5. Fixed an error in ssh-keygen that would have prevented automatic key generation.
11.05.2013 Version 3.4. Cleaned up the background service code so only selected servers run at boot without an activity present, changed some default values.
11.04.2013 Version 3.3. Finally located and fixed a bug that prevented public-key (passwordless) logons for some users. To get this change, users must fully uninstall, then reinstall, SSHelper.
11.03.2013 Version 3.2. Modified installation code to address a resource overuse, changed default configuration to deal with reported problems with public-key logins.
11.03.2013 Version 3.1. Improved the log web page, cleaned up a few small bugs, added an option to enable/disable strict modes in the SSH login protocol, to allow imperfectly configured clients to use public-key (passwordless) logins.
11.01.2013 Version 3.0. Added a special debugging mode that provides extra data useful for analyzing server problems. This change has no effect on normal SSHelper operations but, when activated, is very useful in analyzing Secure Shell server/client problems.
10.31.2013 Version 2.9. Fixed an error condition that could result in an ANR (Application Not Responding), restored the status notification's ability to launch the activity.
10.30.2013 Version 2.8. Added ECDSA key generation to the setup procedure. SSHelper now creates host RSA, DSA and ECDSA keys to maximize operational flexibility.
10.27.2013 Version 2.7. Decided to generate both types of SSH keys during installation — DSA and the older RSA type as well. Some users may expect to be able to use RSA out of the box, and my not generating this key type in the new SSHelper version was an oversight.
10.27.2013 Version 2.6. Added more error traps after testing revealed some unmanaged border conditions.
10.27.2013 Version 2.5. Coded some more precautionary error traps based on reports from the rather slick automated Google bug reporting system.
10.27.2013 Version 2.4. Resolved a number of user-reported issues — dead-end back button on the Help tab, agent forwarding, support for compression and a few others.
10.27.2013 Version 2.3. Rewrote the HTTP log display service, fixed a keyboard bug in the terminal applet.
10.27.2013 Version 2.2. Cleaned up some minor server/client issues, added an HTTP server for remote log display.
10.26.2013 Version 2.1. Solved a secure-login environment path problem.
10.22.2013 Version 2.0. Major upgrade: 1. Replaced Dropbear with OpenSSH for much better reliability and consistency with accepted secure shell standards. 2. Improved the built-in terminal, which now provides history and line editing. 3. Added a Zeroconf broadcast, so SSHelper tells your local network its address and what services it has to offer.
01.08.2013 Version 1.8. On user request, added Android WRITE_MEDIA_STORAGE permission to the set of permissions, even though this doesn't seem to work on a non-rooted Android device at present, in the hope that someone in Android development will get the message and enable this permission.
04.21.2012 Version 1.7. Fixed a problem with the start-at-boot option.
02.23.2012 Version 1.6. Fixed several bugs that crept in after the change to Android ICS, all of which revolved around AsyncTask threads, which are becoming increasingly unreliable, so changed to ordinary threads in some cases.
11.16.2011 Version 1.5. Fixed a bug in the service initialization code.
11.15.2011 Version 1.4. Revised the service code again, for greater robustness.
11.15.2011 Version 1.3. Revised the background service launch method to keep Android from stopping it.
11.12.2011 Version 1.2. Rewrote code to avoid leaking the Text-To-Speech resource.
11.02.2011 Version 1.1. Released updated package after realizing I only support Android API 13 and above.
11.01.2011 Version 1.0. Initial Public Release.
