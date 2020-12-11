package com.soundvision.demo.dfu;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;

import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.DistributeListener;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.UpdateAction;
import com.soundvision.demo.R;

public class AppCenterUpdateListener implements DistributeListener {

    static boolean isDownloadPosponed = false;

    @Override
    public boolean onReleaseAvailable(Activity activity, ReleaseDetails releaseDetails) {

        if (isDownloadPosponed) return true;
        //if (__GlobalApp.getActivity().getClass().equals(SplashScreen.class)) return true;

        String versionName = releaseDetails.getShortVersion();
        int versionCode = releaseDetails.getVersion();
        String releaseNotes = releaseDetails.getReleaseNotes();
        Uri releaseNotesUrl = releaseDetails.getReleaseNotesUrl();

        // Build our own dialog title and message
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(String.format("%s %s %s!", activity.getResources().getString(R.string.version), versionName, activity.getResources().getString(R.string.available)));
        dialogBuilder.setMessage(releaseNotes);

        // Mimic default SDK buttons
        dialogBuilder.setPositiveButton(com.microsoft.appcenter.distribute.R.string.appcenter_distribute_update_dialog_download, (dialog, which) -> {
            Distribute.notifyUpdateAction(UpdateAction.UPDATE);
        });

        dialogBuilder.setNegativeButton(R.string.ask_me_tomorrow, (dialog, which) -> {
            Distribute.notifyUpdateAction(UpdateAction.POSTPONE);
        });


        dialogBuilder.setCancelable(true);
        dialogBuilder.setOnCancelListener(v -> isDownloadPosponed = true);
        dialogBuilder.create().show();


        return true;
    }


}