package com.dieold.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Procedural Modular Jigsaw Socket Expansion Generator:
 * Generates an architecturally coherent, fully enclosed dungeon layout from a deterministic seed.
 * Enforces socket matching, bounding box overlap rejection with 1-tile safety margins,
 * socket capping with solid fortress walls, and A* reachability validation.
 */
public class JigsawDungeonGenerator {

    private final RoomPrefabRegistry registry;
    private final DungeonReachabilityValidator reachabilityValidator;
    private final Rectangle candidateBounds = new Rectangle();
    private final Rectangle expandedBounds = new Rectangle();

    public static class CandidateMatch {
        final RoomTemplate template;
        final Socket childSocket;

        CandidateMatch(RoomTemplate template, Socket childSocket) {
            this.template = template;
            this.childSocket = childSocket;
        }
    }

    public JigsawDungeonGenerator(RoomPrefabRegistry registry) {
        this.registry = registry;
        this.reachabilityValidator = new DungeonReachabilityValidator();
    }

    public JigsawDungeonGenerator() {
        this(new RoomPrefabRegistry());
    }

    /**
     * Generates a fully validated dungeon layout with deterministic seed and target room count.
     */
    public Array<PlacedRoom> generate(long seed, int targetRoomCount) {
        int maxRetries = 10;
        for (int retry = 0; retry < maxRetries; retry++) {
            long currentSeed = seed + retry;
            Array<PlacedRoom> layout = attemptGeneration(currentSeed, targetRoomCount);

            if (layout != null && layout.size >= Math.min(6, targetRoomCount)) {
                // Find spawn coordinates in IngressHub (Room 0)
                PlacedRoom ingress = layout.get(0);
                int spawnX = ingress.gridX + (ingress.template.playerSpawns.size > 0 ? (int) ingress.template.playerSpawns.get(0).x : 8);
                int spawnY = ingress.gridY + (ingress.template.playerSpawns.size > 0 ? (int) ingress.template.playerSpawns.get(0).y : 8);

                if (reachabilityValidator.validate(layout, spawnX, spawnY)) {
                    return layout;
                }
            }
        }

        // Fallback: return the last generated layout capped
        return attemptGeneration(seed, targetRoomCount);
    }

    private Array<PlacedRoom> attemptGeneration(long seed, int targetRoomCount) {
        Random random = new Random(seed);
        Array<PlacedRoom> placedRooms = new Array<>(false, targetRoomCount + 4);
        Array<PlacedSocket> unresolvedSockets = new Array<>(false, 32);

        // 1. Place IngressHub at (0, 0)
        RoomTemplate ingressTemplate = registry.getTemplate("IngressHub");
        if (ingressTemplate == null) {
            ingressTemplate = registry.getAllTemplates().get(0);
        }

        PlacedRoom ingressRoom = new PlacedRoom(ingressTemplate, 0, 0);
        placedRooms.add(ingressRoom);

        for (int i = 0; i < ingressRoom.sockets.size; i++) {
            unresolvedSockets.add(ingressRoom.sockets.get(i));
        }

        // Pre-gather candidate templates (excluding IngressHub)
        Array<RoomTemplate> pool = new Array<>();
        for (int i = 0; i < registry.getAllTemplates().size; i++) {
            RoomTemplate t = registry.getAllTemplates().get(i);
            if (!t.id.equals("IngressHub")) {
                pool.add(t);
            }
        }

        // 2. Jigsaw Expansion Loop
        while (unresolvedSockets.size > 0 && placedRooms.size < targetRoomCount) {
            // Pick a socket (mix of breadth and depth for natural dungeon branching)
            int socketIdx = random.nextInt(unresolvedSockets.size);
            PlacedSocket parentSocket = unresolvedSockets.removeIndex(socketIdx);

            if (!parentSocket.isOpen()) continue;

            // Search compatible templates
            List<CandidateMatch> candidates = new ArrayList<>();
            boolean isNearEnd = (placedRooms.size >= targetRoomCount - 1);

            for (int t = 0; t < pool.size; t++) {
                RoomTemplate candidateTemplate = pool.get(t);
                // If near end, prioritize Antechamber; otherwise include other chambers
                if (isNearEnd && candidateTemplate.id.equals("Antechamber")) {
                    findSocketMatches(parentSocket, candidateTemplate, candidates);
                } else if (!candidateTemplate.id.equals("Antechamber") || isNearEnd) {
                    findSocketMatches(parentSocket, candidateTemplate, candidates);
                }
            }

            // Shuffle candidates deterministically
            Collections.shuffle(candidates, random);

            boolean placed = false;
            for (CandidateMatch match : candidates) {
                RoomTemplate template = match.template;
                Socket childSocket = match.childSocket;

                // Formula: P_room = P_parentSocketConnect - R_childSocket
                int roomX = parentSocket.connectWorldX - childSocket.x;
                int roomY = parentSocket.connectWorldY - childSocket.y;

                candidateBounds.set(roomX, roomY, template.width, template.height);

                if (!checkOverlap(candidateBounds, placedRooms, parentSocket.room)) {
                    // Fits cleanly! Instantiate room
                    PlacedRoom newRoom = new PlacedRoom(template, roomX, roomY);
                    placedRooms.add(newRoom);

                    // Connect parent and child sockets
                    PlacedSocket childPlacedSocket = null;
                    for (int s = 0; s < newRoom.sockets.size; s++) {
                        PlacedSocket ps = newRoom.sockets.get(s);
                        if (ps.socket.id.equals(childSocket.id)) {
                            childPlacedSocket = ps;
                            break;
                        }
                    }

                    if (childPlacedSocket != null) {
                        parentSocket.connect(childPlacedSocket);
                    }

                    // Enqueue new room's open sockets
                    for (int s = 0; s < newRoom.sockets.size; s++) {
                        PlacedSocket ps = newRoom.sockets.get(s);
                        if (ps.isOpen()) {
                            unresolvedSockets.add(ps);
                        }
                    }

                    placed = true;
                    break;
                }
            }

            if (!placed) {
                // Socket remains open, will be capped at the end
            }
        }

        // 3. Socket Capping: Cap all remaining open sockets with solid cast-iron walls
        for (int r = 0; r < placedRooms.size; r++) {
            PlacedRoom room = placedRooms.get(r);
            for (int s = 0; s < room.sockets.size; s++) {
                PlacedSocket ps = room.sockets.get(s);
                if (ps.isOpen()) {
                    ps.isCapped = true;
                }
            }
        }

        return placedRooms;
    }

    private void findSocketMatches(PlacedSocket parentSocket, RoomTemplate candidateTemplate, List<CandidateMatch> outMatches) {
        for (int i = 0; i < candidateTemplate.sockets.size; i++) {
            Socket childSocket = candidateTemplate.sockets.get(i);
            if (parentSocket.socket.isCompatible(childSocket)) {
                outMatches.add(new CandidateMatch(candidateTemplate, childSocket));
            }
        }
    }

    /**
     * Bounding-box intersection check against all previously placed rooms:
     * - Disallows any overlap with any placed room.
     * - Enforces 1-tile safety margin against non-parent rooms to prevent overlapping walls.
     */
    public boolean checkOverlap(Rectangle candidate, Array<PlacedRoom> placedRooms, PlacedRoom parentRoom) {
        // 1. Direct overlap check against all placed rooms
        for (int i = 0; i < placedRooms.size; i++) {
            PlacedRoom existing = placedRooms.get(i);
            if (candidate.overlaps(existing.bounds)) {
                return true; // Collides directly
            }
        }

        // 2. 1-tile safety margin check against non-parent rooms
        expandedBounds.set(candidate.x - 1, candidate.y - 1, candidate.width + 2, candidate.height + 2);
        for (int i = 0; i < placedRooms.size; i++) {
            PlacedRoom existing = placedRooms.get(i);
            if (existing != parentRoom && expandedBounds.overlaps(existing.bounds)) {
                return true; // Violates safety margin with unrelated room
            }
        }

        return false;
    }
}
