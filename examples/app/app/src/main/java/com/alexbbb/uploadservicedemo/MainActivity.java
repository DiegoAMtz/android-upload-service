package com.alexbbb.uploadservicedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alexbbb.uploadservice.BinaryUploadRequest;
import com.alexbbb.uploadservice.MultipartUploadRequest;
import com.alexbbb.uploadservice.UploadNotificationConfig;
import com.alexbbb.uploadservice.UploadService;
import com.alexbbb.uploadservice.UploadServiceBroadcastReceiver;
import com.alexbbb.uploadservice.demo.BuildConfig;
import com.alexbbb.uploadservice.demo.R;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Activity that demonstrates how to use Android Upload Service.
 *
 * @author Alex Gotev
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "UploadServiceDemo";
    private static final String USER_AGENT = "UploadServiceDemo/" + BuildConfig.VERSION_NAME;
    private static final int FILE_CODE = 1;

    @Bind(R.id.uploadProgress) ProgressBar progressBar;
    @Bind(R.id.multipartUploadButton) Button multipartUploadButton;
    @Bind(R.id.binaryUploadButton) Button binaryUploadButton;
    @Bind(R.id.cancelUploadButton) Button cancelUploadButton;
    @Bind(R.id.serverURL) EditText serverUrl;
    @Bind(R.id.fileToUpload) EditText fileToUpload;
    @Bind(R.id.parameterName) EditText parameterName;
    @Bind(R.id.displayNotification) CheckBox displayNotification;

    private final UploadServiceBroadcastReceiver uploadReceiver =
            new UploadServiceBroadcastReceiver() {

        @Override
        public void onProgress(String uploadId, int progress) {
            progressBar.setProgress(progress);

            Log.i(TAG, "The progress of the upload with ID " + uploadId + " is: " + progress);
        }

        @Override
        public void onError(String uploadId, Exception exception) {
            progressBar.setProgress(0);

            Log.e(TAG, "Error in upload with ID: " + uploadId + ". "
                        + exception.getLocalizedMessage(), exception);
        }

        @Override
        public void onCompleted(String uploadId, int serverResponseCode, String serverResponseMessage) {
            progressBar.setProgress(0);

            Log.i(TAG, "Upload with ID " + uploadId + " is completed: " + serverResponseCode + ", "
                       + serverResponseMessage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        progressBar.setMax(100);
        progressBar.setProgress(0);

        // Uncomment this line to enable self-signed SSL certificates in HTTPS connections
        // WARNING: Do not use in production environment. Recommended for development only
        // AllCertificatesAndHostsTruster.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uploadReceiver.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uploadReceiver.unregister(this);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private UploadNotificationConfig getNotificationConfig() {
        if (!displayNotification.isChecked()) return null;

        return new UploadNotificationConfig()
            .setIcon(R.drawable.ic_upload)
            .setTitle(getString(R.string.app_name))
            .setInProgressMessage(getString(R.string.uploading))
            .setCompletedMessage(getString(R.string.upload_success))
            .setErrorMessage(getString(R.string.upload_error))
            .setAutoClearOnSuccess(false)
            .setClickIntent(new Intent(this, MainActivity.class))
            .setClearOnAction(true)
            .setRingToneEnabled(true);
    }

    @OnClick(R.id.multipartUploadButton)
    void onMultipartUploadClick() {
        final String serverUrlString = serverUrl.getText().toString();
        final String fileToUploadPath = fileToUpload.getText().toString();
        final String paramNameString = parameterName.getText().toString();
        final String uploadID = UUID.randomUUID().toString();

        try {
            new MultipartUploadRequest(this, uploadID, serverUrlString)
                .addFileToUpload(fileToUploadPath, paramNameString)
                .setNotificationConfig(getNotificationConfig())
                .setCustomUserAgent(USER_AGENT)
                .setMaxRetries(2)
                .startUpload();

        // these are the different exceptions that may be thrown
        } catch (FileNotFoundException exc) {
            showToast(exc.getMessage());
        } catch (IllegalArgumentException exc) {
            showToast("Missing some arguments. " + exc.getMessage());
        } catch (MalformedURLException exc) {
            showToast(exc.getMessage());
        }
    }

    @OnClick(R.id.binaryUploadButton)
    void onUploadBinaryClick() {
        final String serverUrlString = serverUrl.getText().toString();
        final String fileToUploadPath = fileToUpload.getText().toString();
        final String uploadID = UUID.randomUUID().toString();

        try {
            new BinaryUploadRequest(this, uploadID, serverUrlString)
                .addHeader("file-name", new File(fileToUploadPath).getName())
                .setFileToUpload(fileToUploadPath)
                .setNotificationConfig(getNotificationConfig())
                .setCustomUserAgent(USER_AGENT)
                .setMaxRetries(2)
                .startUpload();

        // these are the different exceptions that may be thrown
        } catch (FileNotFoundException exc) {
            showToast(exc.getMessage());
        } catch (IllegalArgumentException exc) {
            showToast("Missing some arguments. " + exc.getMessage());
        } catch (MalformedURLException exc) {
            showToast(exc.getMessage());
        }
    }

    @OnClick(R.id.cancelUploadButton)
    void onCancelUploadButtonClick() {
        UploadService.stopCurrentUpload();
    }

    @OnClick(R.id.pickFile)
    void onPickFileClick() {
        // Starts NoNonsense-FilePicker (https://github.com/spacecowboy/NoNonsense-FilePicker)
        Intent intent = new Intent(this, FilePickerActivity.class);

        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        intent.putExtra(FilePickerActivity.EXTRA_START_PATH,
                        Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(intent, FILE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            String absolutePath = new File(data.getData().getPath()).getAbsolutePath();
            fileToUpload.setText(absolutePath);
        }
    }
}