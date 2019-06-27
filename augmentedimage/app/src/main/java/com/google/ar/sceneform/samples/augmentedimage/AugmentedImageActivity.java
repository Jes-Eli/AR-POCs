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
  //private final ArrayList<Integer> linearLayoutOptions = new ArrayList<>(
  //        Arrays.asList(R.layout.canvas, R.layout.canvas));
    private final Integer[] linearLayoutOptions = new Integer[3];


    @Override
  protected void onCreate(Bundle savedInstanceState) {
      Log.d("onCreate", "Creating a new arFragment");
      linearLayoutOptionsIndex = 0;
      linearLayoutOptions[0] = R.layout.canvas;
      linearLayoutOptions[1] = R.layout.canvas2;
      linearLayoutOptions[2] = R.layout.canvas3;

      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
      fitToScanView = findViewById(R.id.image_view_fit_to_scan);

      arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
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
    Log.d("tapSwitchView", "DEL current node SET new node");
    Frame frame = arFragment.getArSceneView().getArFrame();
    Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case TRACKING:
          AugmentedImageNode nodeVal = augmentedImageMap.get(augmentedImage);
          if(nodeVal.getIsCurrentViewDisplayed()) {
              Log.d("tapSwitchView", "Removing Child Node " + Integer.toString(linearLayoutOptionsIndex));
              nodeVal.setIsCurrentViewDisplayed(false);
              arFragment.getArSceneView().getScene().removeChild(nodeVal);
              nodeVal.deleteCanvasView();
              linearLayoutOptionsIndex = 1;
              //AugmentedImageNode newNode = createAugmentedImageNode(augmentedImage);
              AugmentedImageNode node1 = new AugmentedImageNode(
                      this, R.layout.canvas2);
              node1.setImage(augmentedImage);
              node1.setIsCurrentViewDisplayed(true);
              augmentedImageMap.put(augmentedImage, node1);
              arFragment.getArSceneView().getScene().addChild(node1);
          }
      }
    }
  }

  private AugmentedImageNode createAugmentedImageNode(AugmentedImage augmentedImage){
      Log.d("createAugmentedImageNode", "Creating Node " + Integer.toString(linearLayoutOptionsIndex));
      AugmentedImageNode node = new AugmentedImageNode(
              this, linearLayoutOptions[linearLayoutOptionsIndex]);
      node.setImage(augmentedImage);
      node.setIsCurrentViewDisplayed(true);
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
            /*AugmentedImageNode node = new AugmentedImageNode(
                    this, linearLayoutOptions[linearLayoutOptionsIndex]);
            node.setImage(augmentedImage);
            node.setIsCurrentViewDisplayed(true);
            node.setOnTouchListener(
                    (HitTestResult r, MotionEvent event) -> gestureDetector.onTouchEvent(event));
            augmentedImageMap.put(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);*/
            AugmentedImageNode node = createAugmentedImageNode(augmentedImage);
            augmentedImageMap.put(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);
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