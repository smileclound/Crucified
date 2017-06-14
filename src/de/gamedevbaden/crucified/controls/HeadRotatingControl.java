package de.gamedevbaden.crucified.controls;

import com.jme3.animation.Bone;
import com.jme3.animation.SkeletonControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * A control which sets the head bone rotation according to the set view direction.
 * <p>
 * Created by Domenic on 09.06.2017.
 */
public class HeadRotatingControl extends AbstractControl {

    private Vector3f compVector = new Vector3f();
    private Vector3f oldViewDir = new Vector3f();
    private Vector3f viewDirection = new Vector3f(Vector3f.UNIT_X);

    private Quaternion initHeadRotation = new Quaternion();
    private Quaternion headRotation = new Quaternion();
    private Quaternion finalHeadRotation = new Quaternion();

    private float[] initAngles = new float[3];
    private float[] headAngles = new float[3];

    private Bone headBone;

    public HeadRotatingControl() {
    }

    /**
     * Applies the specified viewDirection as new facing direction for the head
     *
     * @param viewDirection the direction the head shall face
     */
    public void setViewDirection(Vector3f viewDirection) {
        this.viewDirection.set(viewDirection).normalizeLocal();
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial != null) {
            // setup
            SkeletonControl skeletonControl = spatial.getControl(SkeletonControl.class);
            this.headBone = skeletonControl.getSkeleton().getBone("Neck1");
            this.headBone.setUserControl(true);
            this.headBone.getLocalRotation().toAngles(initAngles);
            this.headRotation.set(headBone.getLocalRotation());
            this.initHeadRotation.set(headBone.getLocalRotation());
        } else {
            // cleanup
            if (headBone != null) {
                headBone.setUserControl(false);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (headBone != null) {
            headBone.setUserControl(enabled);
        }
    }

    @Override
    protected void controlUpdate(float tpf) {

        // if true --> viewDirection has been updated, so we need to set a new rotation for the head
        if (!oldViewDir.equals(viewDirection)) {

            finalHeadRotation.lookAt(viewDirection, Vector3f.UNIT_Y);

            finalHeadRotation.toAngles(headAngles);
            headAngles[1] = 0; // we don't want to have any y-rotation
            headAngles[2] = 0; // wo don't want to have any z-rotation
            finalHeadRotation.fromAngles(headAngles);

            // we update our oldViewDirection vector to avoid that all this
            // is computed more than necessary
            oldViewDir.set(viewDirection);
        }

        // interpolate to new head rotation
        headRotation.slerp(finalHeadRotation, tpf / 0.05f);

        // before we set the new user transform we need to normalize the head rotation
        // otherwise the head will "jump" away from his body when the
        // player movements are too fast
        headRotation.normalizeLocal();

        // finally apply new user transforms
        headBone.setUserTransforms(Vector3f.ZERO, headRotation, Vector3f.UNIT_XYZ);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}