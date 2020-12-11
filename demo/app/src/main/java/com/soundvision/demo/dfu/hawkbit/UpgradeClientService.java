package com.soundvision.demo.dfu.hawkbit;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.soundvision.demo.BaseActivity;
import com.soundvision.demo.R;
import com.soundvision.demo.dfu.hawkbit.model.TargetConfig;
import com.soundvision.demo.dfu.hawkbit.model.TargetDeployment;

public class UpgradeClientService {

    private static final String TAG = UpgradeClientService.class.getSimpleName();



    private String lastVersion = "";
    private TargetDeployment.Artifact updateArtifact = null;
    private HBDownloadFile.OnFirmwareUpdateCallback callback;

    public UpgradeClientService()
    {

    }

    private String getConnectURL()
    {
        return Config.UPGRADE_SERVER_URL;
    }

    public void Connect(Context context, HBDownloadFile.OnFirmwareUpdateCallback callback)
    {
        this.callback = callback;
        new HBRequest(context, getConnectURL(), TargetConfig.class, new HBRequest.OnReceive() {
            @Override
            public void OnResult(Object response) {
                TargetConfig cfg = (TargetConfig)response;
                if (cfg != null && cfg._links.deploymentBase != null) {
                    Log.i(TAG, "Config result");
                    CheckDeployment(context, cfg._links.deploymentBase.href);
                }
            }
        });
    }

    private void CheckDeployment(Context context, String URL)
    {
        new HBRequest(context, URL, TargetDeployment.class, new HBRequest.OnReceive() {
            @Override
            public void OnResult(Object response) {
                TargetDeployment deployment = (TargetDeployment)response;
                if (deployment.deployment != null && deployment.deployment.chunks.size()>0) {
                    Log.i(TAG, "deployment record received ...");
                    CheckForUpgrade(context, deployment);
                }
            }
        });
    }

    private void CheckForUpgrade(Context context, TargetDeployment td) {
        TargetDeployment.Chunk chunk = td.deployment.chunks.get(0);
        if (chunk.version != lastVersion) {
            Log.i(TAG, "New version found.");
            updateArtifact = chunk.artifacts.get(0);
            StartDownloadUpdate(context);
        }
    }

    public boolean StartDownloadUpdate(Context context) {
        if (updateArtifact != null) {
            new HBDownloadFile().setContext(context, callback).execute(updateArtifact._links.downloadHttp.href, updateArtifact.hashes.md5, updateArtifact.filename);
        }
        return updateArtifact != null;
    }


}
