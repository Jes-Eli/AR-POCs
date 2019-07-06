/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Context;
import android.util.Log;
import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import java.util.concurrent.CompletableFuture;


/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;

  // We use completable futures here to simplify
  // the error handling and asynchronous loading.  The loading is started with the
  // first construction of an instance, and then used when the image is set.
  private CompletableFuture<ViewRenderable> canvasView;
  private boolean isCurrentViewDisplayed;
  private boolean prevSet;

  public AugmentedImageNode(Context context, Integer canvasReference) {
      isCurrentViewDisplayed = false;
      prevSet = false;
      // Upon construction, start loading the models
      Log.e("AugmentedImageNode", "Creating AugmentedImageNode " + canvasReference);
      if (canvasView == null) {
          Log.e("AugmentedImageNode", "Building ViewRenderable for Node " + canvasReference);
          canvasView = ViewRenderable.builder().setView(context, canvasReference)
                  .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                  .setHorizontalAlignment(ViewRenderable.HorizontalAlignment.CENTER)
                  .build();
          Log.e("AugmentedImageNode", "YYEEET BOII" + canvasReference);
    }
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void setImage(AugmentedImage image) {
    this.image = image;
    if (!canvasView.isDone()) {
      CompletableFuture.allOf(canvasView)
              .thenAccept((Void aVoid) -> setImage(image))
              .exceptionally(
                      throwable -> {
                        Log.e(TAG, "Exception loading", throwable);
                        return null;
                      });
    }
    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    // Make the node
    Node canvasNode = new Node();
    canvasNode.setParent(this);
    canvasNode.setWorldRotation(Quaternion.identity());
    canvasNode.setLocalScale(new Vector3(image.getExtentX(), image.getExtentZ(), image.getExtentX()));
    canvasNode.setRenderable(canvasView.getNow(null));
  }
  public AugmentedImage getImage() {
      return image;
  }
  public boolean getIsCurrentViewDisplayed(){
      return isCurrentViewDisplayed;
  }
  public void setIsCurrentViewDisplayed(boolean b){
      isCurrentViewDisplayed = b;
  }
  public void deleteCanvasView(){
      canvasView = null;
  }
  public void updatePrevSet(boolean b){prevSet=b;}
  public boolean getPrevSet(){return prevSet;}
}