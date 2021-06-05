package com.arachnoid.sshelper;

/**
 * Created by lutusp on 9/26/17.
 */

/*
Example of use:

We want to show a dialog and, on acceptance, run a passed function.

Target function: void myFunction(void)

Dialog function: actionDialog(String title,String message, FunctionPointer f)

public void resetToDefaults(View v) {
        String title = "Reset to Defaults";
        String message = "Do you really want to reset all values to their defaults?";
        FunctionPointer f = new FunctionInterface() {
            public void yes_function() {
                myFunction();
            }
        };
        actionDialog(v,title,message,f);
}

public void actionDialog(String title, String message,final FunctionInterface f) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.app_icon)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                f.function();
                            }
                        })
                .setNegativeButton(android.R.string.no, null).show();
    }

*/

public interface FunctionInterface {
    void yes_function();
    void no_function();
}
