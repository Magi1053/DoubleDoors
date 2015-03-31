/*
 * Copyright (c) Magi1053 <http://magi1053.com>
 * Copyright (c) DoubleDoors team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.magi1053.doubledoors;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("deprecation")
final class DoubleDoorsListener implements Listener {
    private static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
    private final DoubleDoorsPlugin plugin;

    public DoubleDoorsListener(DoubleDoorsPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        HandlerList.unregisterAll(this);

        plugin.getServer().getPluginManager().registerEvents(new PlayerInteractListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BlockPhysicsListener(), plugin);
    }

    // a and b must be the lower blocks
    private boolean doorsAreConnected(Block a, Block b) {
        if (a.getType() != b.getType() || !isDoor(a.getType())) {
            return false;
        }

        // hinges must be different
        return (a.getRelative(BlockFace.UP).getData() & 0x1) != (b.getRelative(BlockFace.UP).getData() & 0x1)
                && (a.getData() & 0x3) == (b.getData() & 0x3);
    }

    private boolean isDoor(Material block) {
        return (block == Material.WOODEN_DOOR || block == Material.IRON_DOOR_BLOCK || block == Material.SPRUCE_DOOR
                || block == Material.BIRCH_DOOR || block == Material.JUNGLE_DOOR || block == Material.ACACIA_DOOR
                || block == Material.DARK_OAK_DOOR);
    }

    private void doStuff(Block block) {
        if (isDoor(block.getType())) {
            new SyncDoorsTask(block).runTaskLater(plugin, 0);
        }
    }

    private class SyncDoorsTask extends BukkitRunnable {
        Block door;

        public SyncDoorsTask(Block b) {
            door = b;
        }

        @Override
        public void run() {
            Block other = null;

            if ((door.getData() & 0x8) == 0x8) {
                door = door.getRelative(BlockFace.DOWN); // get the lower block if necessary
            }

            for (BlockFace bf : FACES) {
                Block b = door.getRelative(bf);
                if (doorsAreConnected(door, b)) {
                    other = b;
                }
            }
            if (other == null) {
                return;
            }

            if ((door.getData() != other.getData())) {
                other.setData((byte) (other.getData() ^ 0x4)); // flip the open/closed bit
                other.getState().update(true);
            }
        }
    }

    private class PlayerInteractListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerInteract(PlayerInteractEvent e) {
            if (e.isCancelled() || e.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            doStuff(e.getClickedBlock());
        }
    }

    private class BlockPhysicsListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockPhysics(BlockPhysicsEvent e) {
            if (e.isCancelled()) {
                return;
            }
            doStuff(e.getBlock());
        }
    }
}
