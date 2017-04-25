package de.gamedevbaden.crucified.appstates;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import de.gamedevbaden.crucified.enums.Models;
import de.gamedevbaden.crucified.es.components.*;
import de.gamedevbaden.crucified.es.utils.EntityFactory;
import de.gamedevbaden.crucified.es.utils.physics.BoxCollisionShape;
import de.gamedevbaden.crucified.es.utils.physics.CustomMeshCollisionShape;
import de.gamedevbaden.crucified.physics.PhysicConstants;

/**
 * Created by Domenic on 11.04.2017.
 */
public class GameState extends AbstractAppState {

    EntityId player;
    AppStateManager stateManager;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.stateManager = stateManager;

        EntityData entityData = stateManager.getState(EntityDataState.class).getEntityData();

        // create scene
        EntityId scene = entityData.createEntity();
        entityData.setComponents(scene,
                new Model(Models.TestScene.name()),
                new PhysicsRigidBody(0, true, new CustomMeshCollisionShape(Models.TestScene.name())),
                new DynamicTransform(new Vector3f(), new Quaternion().fromAngles(0, 45 * FastMath.DEG_TO_RAD,0), new Vector3f(1,1,1)));

        // create one player
        player = entityData.createEntity();
        entityData.setComponents(player,
                new Model(Models.Player.name()),
                new PhysicsCharacterControl(PhysicConstants.HUMAN_RADIUS, PhysicConstants.HUMAN_HEIGHT, PhysicConstants.HUMAN_WEIGHT),
                new DynamicTransform(new Vector3f(), new Quaternion(), new Vector3f(1,1,1)));
         //       new PhysicsCharacterViewDirection(Vector3f.UNIT_X));

        // create a physic box
        EntityId box = entityData.createEntity();
        entityData.setComponents(box,
                new Model(Models.TestBox.name()),
                new PhysicsRigidBody(10, false, new BoxCollisionShape(1,1,1)), new DynamicTransform(new Vector3f(2, 3, 0), new Quaternion(), new Vector3f(.5f, .5f, .5f)));

        app.getInputManager().addMapping("SPACE", new KeyTrigger(KeyInput.KEY_SPACE));
        app.getInputManager().addListener((ActionListener) (name, isPressed, tpf) -> {
            if (isPressed) return;

        }, "SPACE");
    }

    @Override
    public void update(float tpf) {
    //    stateManager.getState(EntityDataState.class).getEntityData().setComponents(player, new PhysicsCharacterViewDirection(stateManager.getState(CameraAppState.class).getCamera().getDirection()));
    }
}