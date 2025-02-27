// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin.commands

import com.password4j.Password
import net.landlesscity.nukelogin.Main.Companion.database
import net.landlesscity.nukelogin.Status
import net.landlesscity.nukelogin.playersQueue
import net.landlesscity.nukelogin.todoUuid
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

private val sql = database.playerQueries

private fun exec(
    player: Player,
    args: Array<out String>,
): Boolean {
    if (args.size != 1) {
        return false // Print usage
    }

    val uuid = player.todoUuid()
    when (playersQueue.getValue(uuid)) {
        Status.WAITING_REGISTRATION -> {
            val hash = run {
                val plainText = args[0].toByteArray()
                val hashBuilder = Password.hash(plainText)
                @Suppress("MagicNumber")
                hashBuilder.addRandomSalt(24)
                return@run hashBuilder.withArgon2().resultAsBytes
            }

            sql.registerPlayer(
                uuid = uuid.toString(),
                name = player.name,
                password = hash,
            )

            player.sendMessage("Successfully registered!")
            playersQueue[uuid] = Status.AUTHENTIFICATED
        }

        Status.WAITING_LOG_IN -> player.sendMessage("You already registered! Authentificate using /login <password>!")
        Status.AUTHENTIFICATED -> player.sendMessage("You already registered and authentificated!")
    }
    return true
}

private val tab: List<String>? = null // Nothing to complete here

internal object Register : PlayerCommand {
    override fun run(
        player: Player, args: Array<out String>
    ): Boolean = exec(player, args)

    override fun complete(
        sender: CommandSender, args: Array<out String>
    ): List<String>? = tab
}
