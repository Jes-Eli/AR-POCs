package com.google.ar.sceneform.samples.augmentedimage;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
  private final AugmentedImageNode[] linearLayoutOptions = new AugmentedImageNode[4];

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
    if (augmentedImageMap.isEmpty()) {
      fitToScanView.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Registered with the Sceneform Scene object, this method is called at the start of each frame.
   *
   * @param frameTime - time since last frame.
   */
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
}