package com.dieold.game.world;

/**
 * Socket connection classification enforcing architectural grammar:
 * - CORRIDOR: Standard open passage between combat/exploration halls.
 * - VAULT_DOOR: Heavy reinforced blast gate leading to Gatekeeper / Apex bosses.
 * - AIRLOCK: Sealed transitional decompression chamber between sectors.
 */
public enum SocketType {
    CORRIDOR,
    VAULT_DOOR,
    AIRLOCK
}
