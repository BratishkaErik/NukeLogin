// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin.listeners

import net.landlesscity.nukelogin.Main.Companion.database
import net.landlesscity.nukelogin.Status
import net.landlesscity.nukelogin.playersQueue
import net.landlesscity.nukelogin.todoUuid
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

internal object PlayerListener : Listener {
    private val sql = database.playerQueries

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player

        val uuid = event.player.todoUuid()
        player.sendMessage("Hello, ${player.name}! Your UUID is ${uuid}")

        when (playersQueue[uuid]) {
            Status.WAITING_REGISTRATION ->
                player.sendMessage("You have not finished registration yet! Register with /register <password>!")

            Status.WAITING_LOG_IN -> player.sendMessage("Authentificate with /login <password>!")
            Status.AUTHENTICATED -> player.sendMessage("You joined!")
            null -> {
                val playerExist = sql.checkPlayer(uuid.toString()).executeAsOne()
                when (playerExist) {
                    true -> {
                        player.sendMessage("Authentificate with /login <password>!")
                        playersQueue.put(uuid, Status.WAITING_LOG_IN)
                    }

                    else -> {
                        player.sendMessage("You are not registered yet! Register with /register <password>!")
                        playersQueue.put(uuid, Status.WAITING_REGISTRATION)
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val old = event.from
        val new = event.to
        if (old.x == new.x && old.y == new.y && old.z == new.z) {
            return // Do nothing, player just rotated.
        }

        val uuid = event.player.todoUuid()

        when (playersQueue[uuid]!!) {
            Status.WAITING_REGISTRATION,
            Status.WAITING_LOG_IN,
                -> event.isCancelled = true

            Status.AUTHENTICATED -> {}
        }
    }

    // TODO maybe too harsh or slow? Need to be more granular?
    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        val uuid = event.player.todoUuid()

        when (playersQueue[uuid]!!) {
            Status.WAITING_REGISTRATION, Status.WAITING_LOG_IN -> event.isCancelled = true
            Status.AUTHENTICATED -> {}
        }
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        val uuid = event.player.todoUuid()

        require(playersQueue.containsKey(uuid))
        playersQueue.remove(uuid)
    }
}
