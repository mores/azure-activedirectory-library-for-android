package com.microsoft.aad.adal;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

public class AuthContextConfig {

    public static void setSkipBrokerAccountService(final AuthenticationContext context, boolean shouldSkip) {
        context.setSkipBrokerAccountService(shouldSkip);
    }

    public static boolean getSkipBrokerAccountService(final AuthenticationContext context) {
        return context.getSkipBrokerAccountService();
    }

    public static void verifyPermissions(final Activity activity, final int requestCode, final boolean skipRationale) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.GET_ACCOUNTS) && !skipRationale) {
                showPermissionRationaleDialog(activity, requestCode);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.GET_ACCOUNTS},
                        requestCode);
            }
        }
    }

    private static void showPermissionRationaleDialog(final Activity activity, final int requestCode) {
        AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Permission required");
        alertDialog.setMessage("To proceed with acquiring a token, the AccountManager permission must be granted.");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        verifyPermissions(activity, requestCode, true);
                    }
                });
        alertDialog.show();
    }

}
