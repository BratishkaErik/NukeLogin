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
        Status.WAITING_REGISTRATION ->
            player.sendMessage("You need to register first! Register using /register <password>!")

        Status.WAITING_LOG_IN -> {
            val samePassword: Boolean = run {
                val plainText = args[0].toByteArray()
                val hash: ByteArray = sql.getPlayerPassword(uuid = uuid.toString()).executeAsOne()
                return@run Password.check(plainText, hash).withArgon2()
            }
            when (samePassword) {
                true -> {
                    player.sendMessage("Successfully logged in!")
                    playersQueue[uuid] = Status.AUTHENTIFICATED
                }

                else -> {
                    player.sendMessage("Incorrect password! Try again!")
                }
            }
        }

        Status.AUTHENTIFICATED -> player.sendMessage("You already registered and authentificated!")
    }
    return true
}

private val tab: List<String>? = null // Nothing to complete here

internal object Login : PlayerCommand {
    override fun run(
        player: Player, args: Array<out String>
    ): Boolean = exec(player, args)

    override fun complete(
        sender: CommandSender, args: Array<out String>
    ): List<String>? = tab
}
