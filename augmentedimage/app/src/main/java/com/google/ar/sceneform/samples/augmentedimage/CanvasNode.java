package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.TextView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

/**
 * Node that represents a planet.
 *
 * <p>The planet creates two child nodes when it is activated:
 *
 * <ul>
 *   <li>The visual of the planet, rotates along it's own axis and renders the planet.
 *   <li>An info card, renders an Android View that displays the name of the planerendt. This can be
 *       toggled on and off.
 * </ul>
 *
 * The planet is rendered by a child instead of this node so that the spinning of the planet doesn't
 * make the info card spin as well.
 */
public class CanvasNode extends Node implements Node.OnTapListener {

    private Node canvasNode;
    private final Context context;

    public CanvasNode(Context context) {
        this.context = context;
        setOnTapListener(this);
    }

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void onActivate() {

        if (getScene() == null) {
            throw new IllegalStateException("Scene is null!");
        }

        if (canvasNode == null) {
            canvasNode = new Node();
            canvasNode.setParent(this);
            canvasNode.setEnabled(false);
            canvasNode.setLocalPosition(new Vector3(0.0f, 0.55f, 0.0f));

            ViewRenderable.builder()
                    .setView(context, R.layout.canvas)
                    .build()
                    .thenAccept(
                            (renderable) -> {
                                canvasNode.setRenderable(renderable);
                                //TextView textView = (TextView) renderable.getView();
                                //textView.setText(planetName);
                            })
                    .exceptionally(
                            (throwable) -> {
                                throw new AssertionError("Could not load canvas view.", throwable);
                            });
        }
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        if (canvasNode == null) {
            return;
        }

        //canvasNode.setEnabled(!canvasNode.isEnabled());
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        if (canvasNode == null) {
            return;
        }

        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.
        if (getScene() == null) {
            return;
        }
        Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
        Vector3 cardPosition = canvasNode.getWorldPosition();
        Vector3 direction = Vector3.subtract(cameraPosition, cardPosition);
        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        canvasNode.setWorldRotation(lookRotation);
    }
}
