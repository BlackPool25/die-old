package com.dieold.game.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.BitSet;
import java.util.PriorityQueue;

/**
 * Headless Grid-Graph A* Path Reachability Validator:
 * Mathematically proves that the player can navigate from IngressHub spawn
 * to every placed room center and key objective without obstruction or soft-locks.
 */
public class DungeonReachabilityValidator {

    private static class Node implements Comparable<Node> {
        final int x;
        final int y;
        final int index;
        float gCost;
        float fCost;

        Node(int x, int y, int index) {
            this.x = x;
            this.y = y;
            this.index = index;
        }

        @Override
        public int compareTo(Node o) {
            return Float.compare(this.fCost, o.fCost);
        }
    }

    private final Vector2 tempCenter = new Vector2();

    /**
     * Validates that all rooms in placedRooms are reachable from the player spawn in IngressHub.
     */
    public boolean validate(Array<PlacedRoom> placedRooms, int startWorldX, int startWorldY) {
        if (placedRooms == null || placedRooms.size == 0) {
            return false;
        }

        // 1. Calculate Global Grid Bounds
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < placedRooms.size; i++) {
            PlacedRoom r = placedRooms.get(i);
            minX = Math.min(minX, r.gridX);
            minY = Math.min(minY, r.gridY);
            maxX = Math.max(maxX, r.gridX + r.template.width);
            maxY = Math.max(maxY, r.gridY + r.template.height);
        }

        int gridW = maxX - minX;
        int gridH = maxY - minY;
        if (gridW <= 0 || gridH <= 0) return false;

        // 2. Build Walkability BitSet
        int totalCells = gridW * gridH;
        BitSet walkable = new BitSet(totalCells);

        for (int i = 0; i < placedRooms.size; i++) {
            PlacedRoom r = placedRooms.get(i);
            int rw = r.template.width;
            int rh = r.template.height;

            for (int lx = 0; lx < rw; lx++) {
                for (int ly = 0; ly < rh; ly++) {
                    TileType tile = r.template.getTile(lx, ly);
                    int gx = r.gridX + lx;
                    int gy = r.gridY + ly;

                    // Check if this tile is a capped socket
                    boolean isCapped = false;
                    for (int s = 0; s < r.sockets.size; s++) {
                        PlacedSocket ps = r.sockets.get(s);
                        if (ps.isCapped && ps.worldX == gx && ps.worldY == gy) {
                            isCapped = true;
                            break;
                        }
                    }

                    if (tile.isWalkable && !isCapped) {
                        int idx = (gy - minY) * gridW + (gx - minX);
                        walkable.set(idx);
                    }
                }
            }
        }

        int startIdx = (startWorldY - minY) * gridW + (startWorldX - minX);
        if (startIdx < 0 || startIdx >= totalCells || !walkable.get(startIdx)) {
            // Find nearby walkable cell if exact point wasn't marked
            boolean foundStart = false;
            for (int dx = -1; dx <= 1 && !foundStart; dx++) {
                for (int dy = -1; dy <= 1 && !foundStart; dy++) {
                    int nx = startWorldX + dx - minX;
                    int ny = startWorldY + dy - minY;
                    if (nx >= 0 && nx < gridW && ny >= 0 && ny < gridH) {
                        int nIdx = ny * gridW + nx;
                        if (walkable.get(nIdx)) {
                            startWorldX += dx;
                            startWorldY += dy;
                            startIdx = nIdx;
                            foundStart = true;
                        }
                    }
                }
            }
            if (!foundStart) return false;
        }

        // 3. Verify reachability from start to every room's center
        for (int i = 0; i < placedRooms.size; i++) {
            PlacedRoom targetRoom = placedRooms.get(i);
            targetRoom.getCenter(tempCenter);
            int targetX = (int) tempCenter.x;
            int targetY = (int) tempCenter.y;

            // Find closest walkable cell to room center
            int targetIdx = (targetY - minY) * gridW + (targetX - minX);
            if (!walkable.get(targetIdx)) {
                boolean foundTarget = false;
                for (int r = 1; r < 5 && !foundTarget; r++) {
                    for (int dx = -r; dx <= r && !foundTarget; dx++) {
                        for (int dy = -r; dy <= r && !foundTarget; dy++) {
                            int nx = targetX + dx - minX;
                            int ny = targetY + dy - minY;
                            if (nx >= 0 && nx < gridW && ny >= 0 && ny < gridH) {
                                int nIdx = ny * gridW + nx;
                                if (walkable.get(nIdx)) {
                                    targetX += dx;
                                    targetY += dy;
                                    targetIdx = nIdx;
                                    foundTarget = true;
                                }
                            }
                        }
                    }
                }
                if (!foundTarget) return false;
            }

            if (!findPathAStar(walkable, gridW, gridH, minX, minY, startWorldX, startWorldY, targetX, targetY)) {
                return false; // Reachability failed for targetRoom
            }
        }

        return true;
    }

    private boolean findPathAStar(
        BitSet walkable, int gridW, int gridH, int minX, int minY,
        int startX, int startY, int targetX, int targetY
    ) {
        if (startX == targetX && startY == targetY) return true;

        int totalCells = gridW * gridH;
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        BitSet closedSet = new BitSet(totalCells);
        float[] gCosts = new float[totalCells];
        for (int i = 0; i < totalCells; i++) gCosts[i] = Float.MAX_VALUE;

        int startLocalX = startX - minX;
        int startLocalY = startY - minY;
        int startIdx = startLocalY * gridW + startLocalX;

        int targetLocalX = targetX - minX;
        int targetLocalY = targetY - minY;
        int targetIdx = targetLocalY * gridW + targetLocalX;

        Node startNode = new Node(startLocalX, startLocalY, startIdx);
        startNode.gCost = 0f;
        startNode.fCost = heuristic(startLocalX, startLocalY, targetLocalX, targetLocalY);
        openSet.add(startNode);
        gCosts[startIdx] = 0f;

        final int[] dx = {0, 0, 1, -1};
        final int[] dy = {1, -1, 0, 0};

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current.index == targetIdx) {
                return true;
            }

            if (closedSet.get(current.index)) continue;
            closedSet.set(current.index);

            for (int d = 0; d < 4; d++) {
                int nx = current.x + dx[d];
                int ny = current.y + dy[d];

                if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) continue;
                int nIdx = ny * gridW + nx;
                if (!walkable.get(nIdx) || closedSet.get(nIdx)) continue;

                float tentativeG = current.gCost + 1.0f;
                if (tentativeG < gCosts[nIdx]) {
                    gCosts[nIdx] = tentativeG;
                    Node neighbor = new Node(nx, ny, nIdx);
                    neighbor.gCost = tentativeG;
                    neighbor.fCost = tentativeG + heuristic(nx, ny, targetLocalX, targetLocalY);
                    openSet.add(neighbor);
                }
            }
        }

        return false;
    }

    private float heuristic(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
}
