package com.dieold.game.physics;

/**
 * Box2D collision categories and masks.
 */
public final class PhysicsConstants {
    public static final short CATEGORY_PLAYER     = 0x0001;
    public static final short CATEGORY_ENEMY      = 0x0002;
    public static final short CATEGORY_PROJECTILE = 0x0004;
    public static final short CATEGORY_WALL       = 0x0008;
    public static final short CATEGORY_HITBOX     = 0x0010;

    public static final short MASK_PLAYER = (short) (CATEGORY_ENEMY | CATEGORY_WALL | CATEGORY_PROJECTILE);
    public static final short MASK_ENEMY  = (short) (CATEGORY_PLAYER | CATEGORY_ENEMY | CATEGORY_WALL | CATEGORY_PROJECTILE);
    public static final short MASK_WALL   = (short) (CATEGORY_PLAYER | CATEGORY_ENEMY | CATEGORY_PROJECTILE);
    
    // Un-deflected projectile hits player or wall
    public static final short MASK_PROJECTILE_ENEMY = (short) (CATEGORY_PLAYER | CATEGORY_WALL);
    // Deflected projectile hits enemy or wall
    public static final short MASK_PROJECTILE_DEFLECTED = (short) (CATEGORY_ENEMY | CATEGORY_WALL);

    public static final float PPM = 32.0f; // Pixels per meter

    private PhysicsConstants() {}
}
