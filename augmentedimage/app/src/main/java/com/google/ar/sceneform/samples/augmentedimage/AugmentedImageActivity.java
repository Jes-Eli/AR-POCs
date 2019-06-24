package com.google.ar.sceneform.samples.augmentedimage;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.support.v7.app.AlertDialog;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
public class AugmentedImageActivity extends AppCompatActivity {

  private ArFragment arFragment;
  private ImageView fitToScanView;
  //private CanvasNode canvasNode = new CanvasNode(this);
  private GestureDetector gestureDetector;

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

    // Set up the tap listener
    gestureDetector = new GestureDetector(this,
            new GestureDetector.SimpleOnGestureListener(){
              @Override
              public boolean onDoubleTap(MotionEvent e){
                Log.d("onDoubleTap", "TAP TAP!!!!");
                return true;
              }
              @Override
              public boolean onDown(MotionEvent e){
                return true;
              }
            });
  }

  //@TODO I added this
  private void tapUpdateImage(MotionEvent motionEvent) {
    //Frame frame = mFragment.getArSceneView().getArFrame();
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage("HELLO THERE WORLD!")
            .setTitle("TAP TAP TAP!");
    /*
    //make sure we are tracking before we process data.
    if (selectedId == -1 || motionEvent == null || frame == null ||
            frame.getCamera().getTrackingState() != TrackingState.TRACKING)
      return;

    canvas2View = ViewRenderable.builder().setView(context, R.layout.canvas2)
            .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
            .setHorizontalAlignment(ViewRenderable.HorizontalAlignment.CENTER)
            .build();*/
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
            AugmentedImageNode node = new AugmentedImageNode(this);
            node.setImage(augmentedImage);
            // Set up the tap listener for node
            node.setOnTouchListener(
                    (HitTestResult r, MotionEvent event) -> gestureDetector.onTouchEvent(event));
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