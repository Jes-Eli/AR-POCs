package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import android.support.v7.app.AlertDialog;
import android.widget.LinearLayout;
import com.google.ar.sceneform.rendering.ViewRenderable;
import java.util.concurrent.CompletableFuture;


import com.google.ar.sceneform.HitTestResult;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
public class AugmentedImageActivity extends AppCompatActivity {

  private ArFragment arFragment;
  private ImageView fitToScanView;
  private GestureDetector gestureDetector;
  private Integer linearLayoutOptionsIndex;
  private final AugmentedImageNode[] linearLayoutOptions = new AugmentedImageNode[4];

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
    private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
    private FloatingActionButton photoFab, emailFab, facebookFab, downloadFab, exitFab;
    private static ArSceneView view;
    private boolean isViewPaused = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
      Log.d("onCreate", "Creating a new arFragment");
      linearLayoutOptionsIndex = 0;

      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
      fitToScanView = findViewById(R.id.image_view_fit_to_scan);
      arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    
      linearLayoutOptions[0] = new AugmentedImageNode(this, R.layout.canvas);
      linearLayoutOptions[1] = new AugmentedImageNode(this, R.layout.canvas2);
      linearLayoutOptions[2] = new AugmentedImageNode(this, R.layout.canvas3);
      linearLayoutOptions[3] = new AugmentedImageNode(this, R.layout.portrait);

    photoFab = findViewById(R.id.photo_button);
        photoFab.setOnClickListener(view -> {
            try {
                takePhoto();
            }
            catch (CameraNotAvailableException e) {
            }
        });

        emailFab = findViewById(R.id.email_button);
        emailFab.hide();
        emailFab.setOnClickListener(view -> {
            try {
                sendEmail();
            }
            catch (CameraNotAvailableException e) {
            }
        });

        facebookFab = findViewById(R.id.facebook_button);
        facebookFab.hide();
        facebookFab.setOnClickListener(view -> postToFacebook());

        downloadFab = findViewById(R.id.download_button);
        downloadFab.hide();
        downloadFab.setOnClickListener(view -> {
            try {
                savePhoto();
            }
            catch (CameraNotAvailableException e) {
            }
        });

        exitFab = findViewById(R.id.exit);
        exitFab.hide();
        exitFab.setOnClickListener(view -> {
            try {
                exit();
            }
            catch (CameraNotAvailableException e) {
            }
        });
    
      gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
          @Override
          public boolean onDoubleTap(MotionEvent e) {
              Log.d("onDoubleTap", "TAP TAP!!!!");
              tapSwitchView(e);
              return true;
          }

          @Override
          public boolean onDown(MotionEvent e) {
              return true;
          }
    });
  }

  private void tapSwitchView(MotionEvent motionEvent) {
    Log.d("tapSwitchView", "REMOVE current node SET new node");
    Frame frame = arFragment.getArSceneView().getArFrame();
    Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImageKey : updatedAugmentedImages) {
      Log.d("tapSwitchView", "augmentedImageKey - " + linearLayoutOptionsIndex);
      switch (augmentedImageKey.getTrackingState()) {
        case TRACKING:
          Log.d("tapSwitchView", "TRACKING: " + linearLayoutOptionsIndex);
          AugmentedImageNode nodeVal = augmentedImageMap.get(augmentedImageKey);
          if(nodeVal.getIsCurrentViewDisplayed()) {
              Log.d("tapSwitchView", "Removing Child Node " + linearLayoutOptionsIndex);
              nodeVal.setIsCurrentViewDisplayed(false);
              arFragment.getArSceneView().getScene().removeChild(nodeVal);
              linearLayoutOptionsIndex++;
              if (linearLayoutOptionsIndex == linearLayoutOptions.length){linearLayoutOptionsIndex = 0;}
              // only set if setting for the first time
              if(linearLayoutOptions[linearLayoutOptionsIndex].getPrevSet() == false){
                  Log.d("tapSwitchView", "Node to be set for the first time: " + linearLayoutOptionsIndex);
                  linearLayoutOptions[linearLayoutOptionsIndex] = setAugmentedImageNode(
                          linearLayoutOptions[linearLayoutOptionsIndex], augmentedImageKey);
              }
              Log.d("tapSwitchView", "Updating Image Map: " + linearLayoutOptionsIndex);
              linearLayoutOptions[linearLayoutOptionsIndex].setIsCurrentViewDisplayed(true);
              augmentedImageMap.put(augmentedImageKey, linearLayoutOptions[linearLayoutOptionsIndex]);
              arFragment.getArSceneView().getScene().addChild(linearLayoutOptions[linearLayoutOptionsIndex]);
          }
      }
    }
  }

  private AugmentedImageNode setAugmentedImageNode(AugmentedImageNode node, AugmentedImage augmentedImage){
      Log.d("setAugmentedImageNode", "Setting Node Properties " + linearLayoutOptionsIndex);
      node.updatePrevSet(true);
      node.setImage(augmentedImage);
      node.setOnTouchListener(
              (HitTestResult r, MotionEvent event) -> gestureDetector.onTouchEvent(event));
      return node;
  }
  
    @Override
    protected void onResume() {
        super.onResume();
        if (isViewPaused) {
            setFabVisibilityOnExit();
            isViewPaused = false;
        }
        if (augmentedImageMap.isEmpty()) {
            fitToScanView.setVisibility(View.VISIBLE);
        }
    }
  
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();

    // If there is no frame or ARCore is not tracking yet, just return.
    if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      return;
    }

    Collection<AugmentedImage> updatedAugmentedImages =
        frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          String text = "Detected Image " + augmentedImage.getIndex();
          SnackbarHelper.getInstance().showMessage(this, text);
          break;

        case TRACKING:
          // Have to switch to UI Thread to update View.
          fitToScanView.setVisibility(View.GONE);

          // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage)) {
            linearLayoutOptions[linearLayoutOptionsIndex] = setAugmentedImageNode(
                    linearLayoutOptions[linearLayoutOptionsIndex], augmentedImage);
            linearLayoutOptions[linearLayoutOptionsIndex].setIsCurrentViewDisplayed(true);
            augmentedImageMap.put(augmentedImage, linearLayoutOptions[linearLayoutOptionsIndex]);
            arFragment.getArSceneView().getScene().addChild(linearLayoutOptions[linearLayoutOptionsIndex]);
            // Create Alert Message
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You can double tap on the image to change it.").setTitle("Hint!");
            builder.setPositiveButton("Got It!", null);
            AlertDialog dialog = builder.create();
            dialog.show();
          }
          break;

        case STOPPED:
          augmentedImageMap.remove(augmentedImage);
          break;
      }
    }
  }
  
    private String generateFilename() {
        String date = new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "ARtGallery/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {
        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    private void takePhoto() throws CameraNotAvailableException {
        view = arFragment.getArSceneView();
        view.pause();
        isViewPaused = true;
        fitToScanView.setVisibility(View.GONE);
        setFabVisibilityOnTakePhoto();
    }

    private void savePhoto() throws CameraNotAvailableException {
        final String filename = generateFilename();

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(AugmentedImageActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                Snackbar snackbar = Snackbar.make(findViewById(R.id.photo_button),
                        "Photo saved", Snackbar.LENGTH_LONG);

                snackbar.setAction("Open in Photos", v -> {
                    File photoFile = new File(filename);

                    Uri photoURI = FileProvider.getUriForFile(AugmentedImageActivity.this,
                            AugmentedImageActivity.this.getPackageName() + ".ar.codelab.name.provider",
                                    photoFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                    intent.setDataAndType(photoURI, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                });
                snackbar.show();
            } else {
                Toast toast = Toast.makeText(AugmentedImageActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    private void sendEmail() throws CameraNotAvailableException {
        final String filename = generateFilename();

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(AugmentedImageActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

                File photoFile = new File(filename);

                Uri photoURI = FileProvider.getUriForFile(AugmentedImageActivity.this,
                        AugmentedImageActivity.this.getPackageName() + ".ar.codelab.name.provider",
                        photoFile);
                Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                intent.setDataAndType(photoURI, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);

                // send email
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822");
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Your ARt Gallery photo from SIGGRAPH 2019");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Thank you for checking out the ARt Gallery at SIGGRAPH Studio! ");
                emailIntent.putExtra(Intent.EXTRA_STREAM, photoURI);

                try {
                    startActivity(Intent.createChooser(emailIntent, "Send email"));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(AugmentedImageActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }


            } else {
                Toast toast = Toast.makeText(AugmentedImageActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    private void postToFacebook() {
        final String filename = generateFilename();

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(AugmentedImageActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

                File photoFile = new File(filename);

                Uri photoURI = FileProvider.getUriForFile(AugmentedImageActivity.this,
                        AugmentedImageActivity.this.getPackageName() + ".ar.codelab.name.provider",
                        photoFile);

                Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                intent.setDataAndType(photoURI, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);

                SharePhoto photo = new SharePhoto.Builder().setBitmap(bitmap).build();
                SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo)
                        .setShareHashtag(new ShareHashtag.Builder().setHashtag("#SiggraphStudioARtGallery2019").build())
                        .build();
                ShareDialog dialog = new ShareDialog(this);

                if (dialog.canShow(SharePhotoContent.class)){
                    dialog.show(content);
                }
                else{
                    Toast.makeText(AugmentedImageActivity.this, "Cannot share photo", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast toast = Toast.makeText(AugmentedImageActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    private void exit() throws CameraNotAvailableException {
        try {
            view.resume();
            isViewPaused = false;
            if (augmentedImageMap.isEmpty()) {
                fitToScanView.setVisibility(View.VISIBLE);
            }
            setFabVisibilityOnExit();
        } catch (CameraNotAvailableException e) {
            throw e;
        }
    }

    private void setFabVisibilityOnTakePhoto() {
        photoFab.hide();
        emailFab.show();
        facebookFab.show();
        downloadFab.show();
        exitFab.show();
    }

    private void setFabVisibilityOnExit() {
        photoFab.show();
        emailFab.hide();
        facebookFab.hide();
        downloadFab.hide();
        exitFab.hide();
   }
}