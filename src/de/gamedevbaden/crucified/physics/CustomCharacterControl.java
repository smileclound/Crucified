package de.gamedevbaden.crucified.physics;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;

/**
 * Created by Domenic on 11.04.2017.
 */
public class CustomCharacterControl extends BetterCharacterControl {

    public CustomCharacterControl() {
    }

    public CustomCharacterControl(float radius, float height, float mass) {
        super(radius, height, mass);
    }

    public PhysicsRigidBody getPhysicsRigidBody() {
        return rigidBody;
    }

    public Quaternion getCharacterRotation() {
        return rotation.clone();
    }

}