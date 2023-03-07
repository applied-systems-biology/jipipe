package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;
import ij3d.behaviors.BehaviorCallback;
import ij3d.behaviors.InteractiveViewPlatformTransformer;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.*;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Copy of all contents of {@link ij3d.behaviors.ViewPlatformTransformer} and {@link InteractiveViewPlatformTransformer}, so the behavior can be changed
 */
public class CustomInteractiveViewPlatformTransformer {
    protected CustomImage3DUniverse universe;
    protected ImageCanvas3D canvas;

    protected final Point3d rotCenter = new Point3d();

    private final BehaviorCallback callback;

    private final TransformGroup centerTG;
    private final TransformGroup rotationTG;
    private final TransformGroup zoomTG;
    private final TransformGroup translateTG;

    private final Transform3D centerXform = new Transform3D();
    private final Transform3D rotationXform = new Transform3D();
    private final Transform3D zoomXform = new Transform3D();
    private final Transform3D translateXform = new Transform3D();

    private final Point3d origin = new Point3d(0, 0, 0);
    private final Point3d eyePos = new Point3d();

    private final Point3d oneInX = new Point3d(1, 0, 0);
    private final Point3d oneInY = new Point3d(0, 1, 0);
    private final Point3d oneInZ = new Point3d(0, 0, 1);

    private final Vector3d zDir = new Vector3d();
    private final Vector3d xDir = new Vector3d();
    private final Vector3d yDir = new Vector3d();

    private final Vector3d centerV = new Vector3d();

    private final Transform3D ipToVWorld = new Transform3D();

    /**
     * Initialize this ViewPlatformTransformer.
     *
     * @param universe
     * @param callback
     */
    public CustomInteractiveViewPlatformTransformer(final CustomImage3DUniverse universe,
                                                    final BehaviorCallback callback) {
        this.universe = universe;
        this.canvas = (ImageCanvas3D) universe.getCanvas();
        this.callback = callback;
        this.centerTG = universe.getCenterTG();
        this.rotationTG = universe.getRotationTG();
        this.zoomTG = universe.getZoomTG();
        this.translateTG = universe.getTranslateTG();
        ((Image3DUniverse) universe).getGlobalCenterPoint(rotCenter);
    }

    /**
     * Copies the rotation center into the given Tuple3d.
     */
    public void getRotationCenter(final Tuple3d ret) {
        ret.set(rotCenter);
    }

    /**
     * Moves the view back (i.e. in the z-direction of the image plate) to the
     * specified distance.
     *
     * @param distance
     */
    public void zoomTo(final double distance) {
        zDir.set(0, 0, 1);
        zDir.scale(distance);
        zoomXform.set(zDir);
        zoomTG.setTransform(zoomXform);
        universe.getViewer().getView().setBackClipDistance(5 * distance);
        universe.getViewer().getView().setFrontClipDistance(5 * distance / 100);
        transformChanged(BehaviorCallback.TRANSLATE, zoomXform);
    }

    public void updateFrontBackClip() {
        zoomTG.getTransform(zoomXform);
        zoomXform.get(zDir);
        final double d = zDir.length();
        universe.getViewer().getView().setBackClipDistance(5 * d);
        universe.getViewer().getView().setFrontClipDistance(5 * d / 100);
    }

    private final Transform3D tmp = new Transform3D();

    /**
     * Zoom by the specified amounts of units.
     *
     * @param units
     */
    public void zoom(final double units) {
        origin.set(0, 0, 0);
        canvas.getCenterEyeInImagePlate(eyePos);
        canvas.getImagePlateToVworld(ipToVWorld);
        ipToVWorld.transform(eyePos);
        final float dD = (float) eyePos.distance(origin);

        originInCanvas(originInCanvas);
        canvas.getPixelLocationInImagePlate(originInCanvas, originOnIp);
        ipToVWorld.transform(originOnIp);
//        final float dd = (float) eyePos.distance(originOnIp);
        final float dd = 1;

        canvas.getPixelLocationInImagePlate((int) Math.round(originInCanvas.x + 5),
                (int) Math.round(originInCanvas.y), currentPtOnIp);
        ipToVWorld.transform(currentPtOnIp);
        final float dx = (float) originOnIp.distance(currentPtOnIp);

        zDir.set(0, 0, -1);
        final float factor = dx * dD / dd;
        zDir.scale(units * factor);

        zoomTG.getTransform(zoomXform);
        tmp.set(zDir);
        zoomXform.mul(tmp, zoomXform);

        zoomTG.setTransform(zoomXform);
        zoomXform.get(centerV);
        final double distance = centerV.length();
        universe.getViewer().getView().setBackClipDistance(5 * distance);
        universe.getViewer().getView().setFrontClipDistance(5 * distance / 100);
        transformChanged(BehaviorCallback.TRANSLATE, zoomXform);
    }

    /**
     * Center the view at the given point.
     *
     * @param center
     */
    public void centerAt(final Point3d center) {
        // set the center transformation to the translation given by
        // the specified point
        centerV.set(center.x, center.y, center.z);
        centerXform.set(centerV);
        centerTG.setTransform(centerXform);
        // set the global translation to identity
        centerXform.setIdentity();
        translateTG.setTransform(centerXform);
        transformChanged(BehaviorCallback.TRANSLATE, centerXform);
        // update rotation center
        rotCenter.set(center);
    }

    private final Point2d originInCanvas = new Point2d();
    private final Point3d originOnIp = new Point3d();
    private final Point3d currentPtOnIp = new Point3d();

    /**
     * Translates the view suitable to a mouse movement by dxPix and dyPix on the
     * canvas.
     *
     * @param dxPix
     * @param dyPix
     */
    public void translateXY(final int dxPix, final int dyPix) {
        origin.set(0, 0, 0);
        canvas.getCenterEyeInImagePlate(eyePos);
        canvas.getImagePlateToVworld(ipToVWorld);
        ipToVWorld.transform(eyePos);
        final float dD = (float) eyePos.distance(origin);

        originInCanvas(originInCanvas);
        canvas.getPixelLocationInImagePlate(originInCanvas, originOnIp);
        ipToVWorld.transform(originOnIp);
        final float dd = (float) eyePos.distance(originOnIp);

        canvas.getPixelLocationInImagePlate((int) Math.round(originInCanvas.x + 1),
                (int) Math.round(originInCanvas.y), currentPtOnIp);
        ipToVWorld.transform(currentPtOnIp);
        final float dx = (float) originOnIp.distance(currentPtOnIp);

        canvas.getPixelLocationInImagePlate((int) Math.round(originInCanvas.x),
                (int) Math.round(originInCanvas.y + 1), currentPtOnIp);
        ipToVWorld.transform(currentPtOnIp);
        final float dy = (float) originOnIp.distance(currentPtOnIp);

        final float dX = dx * dxPix * dD / dd;
        final float dY = dy * dyPix * dD / dd;

        translateXY(dX, dY);
    }

    /**
     * Translates the view by the specified distances along the x, y and z
     * direction (of the vworld).
     *
     * @param v The distances in x, y and z direction, given in vworld dimensions.
     */
    public void translate(final Vector3d v) {
        getTranslateTranslation(tmpV);
        tmpV.sub(v);
        translateXform.set(tmpV);
        translateTG.setTransform(translateXform);
        transformChanged(BehaviorCallback.TRANSLATE, translateXform);
    }

    public void resetView(StandardView standardView) {
        universe.resetView();

        AxisAngle4d angle4d;

        if (standardView == StandardView.Top) {
            angle4d = new AxisAngle4d(1, 0, 0, Math.PI);
        } else if (standardView == StandardView.Bottom) {
            angle4d = new AxisAngle4d(0, 0, 1, Math.PI);
        }
        else if (standardView == StandardView.North) {
            angle4d = new AxisAngle4d(0, -1, 1, Math.PI);
        } else if (standardView == StandardView.South) {
            angle4d = new AxisAngle4d(0, 1, 1, Math.PI);
        } else if (standardView == StandardView.East) {
            angle4d = new AxisAngle4d(1, 0, 1, Math.PI);
        } else if (standardView == StandardView.West) {
            angle4d = new AxisAngle4d(1, 0, -1, Math.PI);
        }
        else {
            throw new UnsupportedOperationException("Unsupported: " + standardView);
        }

        rotationXform.set(angle4d);
        rotationTG.setTransform(rotationXform);
        transformChanged(BehaviorCallback.ROTATE, rotationXform);

        universe.fixWeirdRendering();
    }

    /**
     * Translates the view by the specified distances along the x and y direction
     * (of the image plate).
     *
     * @param dx The distance in x direction, given in vworld dimensions.
     * @param dy The distance in y direction, given in vworld dimensions.
     */
    public void translateXY(final double dx, final double dy) {
        getXDir(xDir);
        getYDir(yDir);
        xDir.scale(dx);
        yDir.scale(dy);
        xDir.add(yDir);
        translate(xDir);
    }

    private final AxisAngle4d aa = new AxisAngle4d();
    private final Vector3d tmpV = new Vector3d();

    /**
     * Rotates the view around the global rotation center by the specified angle
     * around the x axis (of the image plate).
     *
     * @param angle The angle (in rad) around the x-axis
     */
    public void rotateX(final double angle) {
        xDir.set(1, 0, 0);
        rotate(xDir, angle);
    }

    /**
     * Rotates the view around the global rotation center by the specified angle
     * around the y axis (of the image plate).
     *
     * @param angle The angle (in rad) around the y-axis
     */
    public void rotateY(final double angle) {
        yDir.set(0, 1, 0);
        rotate(yDir, angle);
    }

    /**
     * Rotates the view around the global rotation center by the specified angle
     * around the z axis (of the image plate).
     *
     * @param angle The angle (in rad) around the z-axis
     */
    public void rotateZ(final double angle) {
        zDir.set(0, 0, 1);
        rotate(zDir, angle);
    }

    /**
     * Rotates the view around the center of view by the specified angle around
     * the given axis (of the image plate).
     *
     * @param axis  The axis of rotation (in image plate coordinate system)
     * @param angle The angle (in rad) around the given axis
     */
    public void rotate(final Vector3d axis, final double angle) {
        final Vector3d axisVW = new Vector3d();
        getAxisVworld(axis, axisVW);
        aa.set(axisVW, angle);
        tmp.set(aa);

        // first apply the old transform
        rotationTG.getTransform(rotationXform);
        // rotate
        rotationXform.mul(tmp, rotationXform);

        rotationTG.setTransform(rotationXform);
        transformChanged(BehaviorCallback.ROTATE, rotationXform);
    }

    private final AxisAngle4d aa2 = new AxisAngle4d();
    private final Transform3D tmp2 = new Transform3D();

    /**
     * Rotates the view around the center of view by the specified angles around
     * the x and y axis (of the image plate).
     *
     * @param angleX The angle (in rad) around the x-axis
     * @param angleY The angle (in rad) around the y-axis
     */
    public void rotateXY(final double angleX, final double angleY) {
        getXDir(xDir);
        aa.set(xDir, angleX);
        tmp.set(aa);

        getYDir(yDir);
        aa2.set(yDir, angleY);
        tmp2.set(aa2);

        // first apply the old transform
        rotationTG.getTransform(rotationXform);
        // rotate x
        rotationXform.mul(tmp, rotationXform);
        // rotate y
        rotationXform.mul(tmp2, rotationXform);

        rotationTG.setTransform(rotationXform);
        transformChanged(BehaviorCallback.ROTATE, rotationXform);
    }

    /**
     * Retrieves the manual translation vector of the view.
     *
     * @param v
     */
    public void getTranslateTranslation(final Vector3d v) {
        translateTG.getTransform(tmp);
        tmp.get(v);
    }

    /**
     * Retrieves the translation vector which is responsible for centering the
     * view.
     *
     * @param v
     */
    public void getCenterTranslation(final Vector3d v) {
        centerTG.getTransform(tmp);
        tmp.get(v);
    }

    /**
     * Retrieves the translation vector which is responsible for the current
     * zooming and stores it in the given Vector3d.
     *
     * @param v
     */
    public void getZoomTranslation(final Vector3d v) {
        zoomTG.getTransform(tmp);
        tmp.get(v);
    }

    /**
     * Stores the canvas position of the origin of the vworld in the specified
     * Point2d.
     *
     * @param out
     */
    public void originInCanvas(final Point2d out) {
        origin.set(0, 0, 0);
        pointInCanvas(origin, out);
    }

    private final Point3d tmpP = new Point3d();
    private final Transform3D ipToVWorldInverse = new Transform3D();

    /**
     * Calculates where the specified point in the vworld space is placed on the
     * canvas and stores the result in the specified Point2d.
     *
     * @param in
     * @param out
     */
    public void pointInCanvas(final Point3d in, final Point2d out) {
        tmpP.set(in);
        canvas.getImagePlateToVworld(ipToVWorld);
        ipToVWorldInverse.invert(ipToVWorld);
        ipToVWorldInverse.transform(in);
        canvas.getPixelLocationFromImagePlate(in, out);
    }

    /**
     * Calculates the distance between the viewer's eye and an arbitrary point in
     * the vworld space.
     *
     * @return
     */
    public double distanceEyeTo(final Point3d p) {
        canvas.getCenterEyeInImagePlate(eyePos);
        canvas.getImagePlateToVworld(ipToVWorld);
        ipToVWorld.transform(eyePos);
        return eyePos.distance(p);
    }

    /**
     * Calculates the distance between the viewer's eye and the origin in the
     * vworld space.
     *
     * @return
     */
    public double distanceEyeOrigin() {
        origin.set(0, 0, 0);
        return distanceEyeTo(origin);
    }

    /**
     * Calculates from the specified axis in image plate coordinate system the
     * corresponding vector in the vworld coordinate system.
     */
    public void getAxisVworld(final Vector3d axis, final Vector3d axisVW) {
        canvas.getImagePlateToVworld(ipToVWorld);
        origin.set(0, 0, 0);
        oneInX.set(axis);
        ipToVWorld.transform(oneInX);
        ipToVWorld.transform(origin);
        axisVW.sub(oneInX, origin);
        axisVW.normalize();
    }

    /**
     * Transforms the x-direction of the image plate to a normalized vector
     * representing this direction in the vworld space.
     *
     * @param v Vector3d in which the result in stored.
     */
    public void getXDir(final Vector3d v) {
        canvas.getImagePlateToVworld(ipToVWorld);
        getXDir(v, ipToVWorld);
    }

    /**
     * Transforms the x-direction of the image plate to a normalized vector
     * representing this direction in the vworld space.
     *
     * @param v          Vector3d in which the result in stored.
     * @param ipToVWorld the image plate to vworld transformation.
     */
    public void getXDir(final Vector3d v, final Transform3D ipToVWorld) {
        origin.set(0, 0, 0);
        oneInX.set(1, 0, 0);
        ipToVWorld.transform(oneInX);
        ipToVWorld.transform(origin);
        v.sub(oneInX, origin);
        v.normalize();
    }

    /**
     * Stores the y-direction in the image plate coordinate system, i.e. the
     * direction towards the user, in the given Vector3d.
     *
     * @param v Vector3d in which the result in stored.
     */
    public void getYDir(final Vector3d v) {
        canvas.getImagePlateToVworld(ipToVWorld);
        getYDir(v, ipToVWorld);
    }

    /**
     * Transforms the y-direction of the image plate to a normalized vector
     * representing this direction in the vworld space.
     *
     * @param v          Vector3d in which the result in stored.
     * @param ipToVWorld the image plate to vworld transformation.
     */
    public void getYDir(final Vector3d v, final Transform3D ipToVWorld) {
        origin.set(0, 0, 0);
        oneInY.set(0, 1, 0);
        ipToVWorld.transform(oneInY);
        ipToVWorld.transform(origin);
        v.sub(oneInY, origin);
        v.normalize();
    }

    /**
     * Transforms the z-direction of the image plate to a normalized vector
     * representing this direction in the vworld space.
     *
     * @param v Vector3d in which the result in stored.
     */
    public void getZDir(final Vector3d v) {
        canvas.getImagePlateToVworld(ipToVWorld);
        getZDir(v, ipToVWorld);
    }

    /**
     * Transforms the z-direction of the image plate to a normalized vector
     * representing this direction in the vworld space.
     *
     * @param v          Vector3d in which the result in stored.
     * @param ipToVWorld the image plate to vworld transformation.
     */
    public void getZDir(final Vector3d v, final Transform3D ipToVWorld) {
        origin.set(0, 0, 0);
        oneInZ.set(0, 0, 1);
        ipToVWorld.transform(oneInZ);
        ipToVWorld.transform(origin);
        v.sub(origin, oneInZ);
        v.normalize();
    }

    private void transformChanged(final int type, final Transform3D t) {
        if (callback != null) callback.transformChanged(type, t);
    }

    private static final double ONE_RAD = 2 * Math.PI / 360;
    private int xLast, yLast;

    /**
     * This method should be called when a new transformation is started (i.e.
     * when the mouse is pressed before dragging in order to rotate or translate).
     *
     * @param e
     */
    public void init(final MouseEvent e) {
        this.xLast = e.getX();
        this.yLast = e.getY();
    }

    /**
     * This method should be called during the mouse is dragged, if the mouse
     * event should result in a translation.
     *
     * @param e
     */
    public void translate(final MouseEvent e) {
        final int dx = xLast - e.getX();
        final int dy = yLast - e.getY();
        translateXY(-dx, dy);
        xLast = e.getX();
        yLast = e.getY();
    }

    /**
     * This method should be called during the mouse is dragged, if the mouse
     * event should result in a rotation.
     *
     * @param e
     */
    public void rotate(final MouseEvent e) {
        final int dx = xLast - e.getX();
        final int dy = yLast - e.getY();
        rotateXY(dy * ONE_RAD, dx * ONE_RAD);
        xLast = e.getX();
        yLast = e.getY();
    }

    /**
     * This method should be called, if the specified MouseEvent should affect
     * zooming based on wheel movement.
     *
     * @param e
     */
    public void wheel_zoom(final MouseEvent e) {
        final MouseWheelEvent we = (MouseWheelEvent) e;
        int units = 0;
        if (we.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) units =
                we.getUnitsToScroll();
        zoom(units);
    }

    /**
     * This method should be called, if the specified MouseEvent should affect
     * zooming based on vertical mouse dragging.
     *
     * @param e
     */
    public void zoom(final MouseEvent e) {
        final int y = e.getY();
        final int dy = y - yLast;
        zoom(dy);
        xLast = e.getX();
        yLast = y;
    }


}
