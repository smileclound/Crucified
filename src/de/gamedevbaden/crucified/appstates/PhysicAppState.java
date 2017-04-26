package de.gamedevbaden.crucified.appstates;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import de.gamedevbaden.crucified.es.components.DynamicTransform;
import de.gamedevbaden.crucified.es.components.Model;
import de.gamedevbaden.crucified.es.components.PhysicsCharacterControl;
import de.gamedevbaden.crucified.es.components.PhysicsRigidBody;
import de.gamedevbaden.crucified.es.utils.physics.CollisionShapeType;
import de.gamedevbaden.crucified.physics.CustomCharacterControl;

import java.util.HashMap;

/**
 * The <code>{@link PhysicAppState}</code> takes care of all physical entities.
 * Currently there is RigidBody and CharacterControl support (most things are handled with those two)
 * This state will set new {@link DynamicTransform} components when physics position and rotation changes.
 *
 * Created by Domenic on 13.04.2017.
 */
public class PhysicAppState extends AbstractAppState {

    private EntitySet characters;
    private EntitySet rigidBodies;

    private HashMap<EntityId, CustomCharacterControl> characterControls;
    private HashMap<EntityId, RigidBodyControl> rigidBodyControls;

    private AppStateManager stateManager;
    private BulletAppState bulletAppState;
    private ModelLoaderAppState modelLoader; // might be needed to create collision shapes out of a spatial

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.stateManager = stateManager;
        this.modelLoader = stateManager.getState(ModelLoaderAppState.class);

        this.bulletAppState = new BulletAppState();
        this.bulletAppState.setDebugEnabled(true);
        this.stateManager.attach(bulletAppState);

        this.characterControls = new HashMap<>();
        this.rigidBodyControls = new HashMap<>();

        EntityData entityData = stateManager.getState(EntityDataState.class).getEntityData();
        this.characters = entityData.getEntities(Model.class, PhysicsCharacterControl.class, DynamicTransform.class);
        this.rigidBodies = entityData.getEntities(Model.class, PhysicsRigidBody.class, DynamicTransform.class);


        super.initialize(stateManager, app);
    }

    @Override
    public void update(float tpf) {
        if (!bulletAppState.isInitialized()) return;


        // character controls
        if (characters.applyChanges()) {

            for (Entity entity : characters.getAddedEntities()) {
                addCharacterControl(entity);
            }

            for (Entity entity : characters.getRemovedEntities()) {
                removeCharacterControl(entity);
            }

        }

        // rigid body controls
        if (rigidBodies.applyChanges()) {

            for (Entity entity : rigidBodies.getAddedEntities()) {
                addRigidBodyControl(entity);
            }

            for (Entity entity : rigidBodies.getChangedEntities()) {
                if (entity.get(PhysicsRigidBody.class).isKinematic()) {
                    RigidBodyControl rigidBodyControl = getRigidBodyControl(entity.getId());
                    DynamicTransform transform = entity.get(DynamicTransform.class);
                    rigidBodyControl.setPhysicsLocation(transform.getTranslation());
                    rigidBodyControl.setPhysicsRotation(transform.getRotation());
                    //   rigidBodyControl.getCollisionShapeType().setScale(transform.getScale());
                }

            }

            for (Entity entity : rigidBodies.getRemovedEntities()) {
                removeRigidBodyControl(entity);
            }

        }


        // apply new transforms for rigid bodies
        for (Entity entity : rigidBodies) {
            com.jme3.bullet.objects.PhysicsRigidBody rigidBody = rigidBodyControls.get(entity.getId());
            if (entity.get(PhysicsRigidBody.class).isKinematic()) {

            } else {
                Vector3f location = rigidBody.getPhysicsLocation();
                Quaternion rotation = rigidBody.getPhysicsRotation();
                Vector3f scale = entity.get(DynamicTransform.class).getScale();
                applyNewChanges(entity, location, rotation, scale);
            }

        }

        // apply new transforms for characters
        for (Entity entity : characters) {
            CustomCharacterControl characterControl = characterControls.get(entity.getId());
            Vector3f location = characterControl.getPhysicsRigidBody().getPhysicsLocation();
            Quaternion rotation = characterControl.getCharacterRotation(); //ToDo: Shall that be changed? Player Rotation is just a thing of the view, so how could we implement this instantly
            Vector3f scale = entity.get(DynamicTransform.class).getScale();
            applyNewChanges(entity, location, rotation, scale);
        }

    }

    /**
     * This method sets if necessary a new transformation component for that entity
     * @param entity the entity the new transformation shall be applied to
     * @param location the latest physic location
     * @param rotation the latest physic rotation
     * @param scale the scale
     */
    private void applyNewChanges(Entity entity, Vector3f location, Quaternion rotation, Vector3f scale) {
        DynamicTransform currentTransform = entity.get(DynamicTransform.class);

        // we only will set a new FixedTransformation if the spatial has really changed its position, rotation or scale
        if (location.equals(currentTransform.getTranslation()) &&
            rotation.equals(currentTransform.getRotation()) &&
            scale.equals(currentTransform.getScale())) {
            return;
        }

        // create new FixedTransformation for entity
        entity.set(new DynamicTransform(location, rotation, scale));
    }


    /**
     * Get the {@link CustomCharacterControl} for this entity.
     * @param entityId the entity id the character control is added to
     * @return the character control or null if there is none
     */
    public CustomCharacterControl getCharacterControl(EntityId entityId) {
        return characterControls.get(entityId);
    }

    /**
     * Get the {@link RigidBodyControl} for this entity.
     * @param entityId the entity id the rigid body contol is added to
     * @return the rigid body control or null if there is none.
     */
    public RigidBodyControl getRigidBodyControl(EntityId entityId) {
        return rigidBodyControls.get(entityId);
    }

    private void addCharacterControl(Entity entity) {
        PhysicsCharacterControl pcc = entity.get(PhysicsCharacterControl.class);
        CustomCharacterControl characterControl = new CustomCharacterControl(pcc.getWidth(), pcc.getHeight(), pcc.getMass()); //Todo:
        addPhysicsControl(characterControl);
        characterControls.put(entity.getId(), characterControl);
    }

    private void removeCharacterControl(Entity entity) {
        CustomCharacterControl cc = characterControls.remove(entity.getId());
        removePhysicsControl(cc.getPhysicsRigidBody());
    }

    private void addRigidBodyControl(Entity entity) {
        PhysicsRigidBody rigidBody = entity.get(PhysicsRigidBody.class);
        DynamicTransform transform = entity.get(DynamicTransform.class);
        int shapeType = entity.get(PhysicsRigidBody.class).getCollisionShapeType();
        CollisionShape collisionShape = getCollisionShape(shapeType, entity.get(Model.class).getModelPath(), transform.getScale());
        System.out.println(collisionShape + " " + shapeType);
        RigidBodyControl rigidBodyControl = new RigidBodyControl(collisionShape, rigidBody.getMass());
        addPhysicsControl(rigidBodyControl);
        rigidBodyControl.setPhysicsLocation(transform.getTranslation());
        rigidBodyControl.setPhysicsRotation(transform.getRotation());
        rigidBodyControl.setKinematic(rigidBody.isKinematic());
        rigidBodyControls.put(entity.getId(), rigidBodyControl);
    }

    private void removeRigidBodyControl(Entity entity) {
        RigidBodyControl body = rigidBodyControls.remove(entity.getId());
        removePhysicsControl(body);
    }

    private void addPhysicsControl(PhysicsControl control) {
        bulletAppState.getPhysicsSpace().add(control);
    }

    private void removePhysicsControl(com.jme3.bullet.objects.PhysicsRigidBody control) {
        bulletAppState.getPhysicsSpace().remove(control);
    }

    private CollisionShape getCollisionShape(int type, String modelPath, Vector3f scale) {
        if (type == CollisionShapeType.BOX_COLLISION_SHAPE) {
            Spatial model = modelLoader.loadModel(modelPath);
            model.setLocalScale(scale);
            return CollisionShapeFactory.createBoxShape(model);
        } else if (type == CollisionShapeType.MESH_COLLISION_SHAPE) {
            Spatial model = modelLoader.loadModel(modelPath);
            if (model != null) {
                model.setLocalScale(scale);
                return CollisionShapeFactory.createMeshShape(model);
            }
            return null;
        } else if (type == CollisionShapeType.TERRAIN_COLLISION_SHAPE) {
            Spatial model = modelLoader.loadModel(modelPath);
            TerrainQuad terrain = null;
            if (model instanceof TerrainQuad) {
                terrain = (TerrainQuad) model;
            } else if (model instanceof Node) { // should always be this case
                Node node = (Node) model;
                if (node.getChild(0) instanceof TerrainQuad) {
                    terrain = (TerrainQuad) node.getChild(0);
                }
            }
            if (terrain != null) {
                model.setLocalScale(scale);
                // the method will recognize that this is a terrain
                return CollisionShapeFactory.createMeshShape(model);
            }
        }
        return null;

    }

    @Override
    public void cleanup() {
        for (Entity entity : characters) {
            removeCharacterControl(entity);
        }
        for (Entity entity : rigidBodies) {
            removeRigidBodyControl(entity);
        }
        this.characterControls = null;
        this.rigidBodyControls = null;

        this.characters.clear();
        this.characters = null;

        this.rigidBodies.clear();
        this.rigidBodies = null;

        this.bulletAppState = null;

        super.cleanup();
    }
}
