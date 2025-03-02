// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin.commands

import net.landlesscity.nukelogin.Algorithm
import net.landlesscity.nukelogin.Main.Companion.database
import net.landlesscity.nukelogin.Status
import net.landlesscity.nukelogin.playersQueue
import net.landlesscity.nukelogin.todoUuid
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
            val plainText = args[0].toByteArray()
            val currentPassword = sql.getPasswordCurrent(uuid = uuid.toString()).executeAsOne()
            val algorithm = Algorithm.decode(currentPassword.algorithm)

            // If null, algorithm is unknown and so equality
            // concept is not quite applicable here.
            val samePassword: Boolean? = algorithm?.verify(
                plainText = plainText,
                hash = currentPassword.hash,
            )
            when (samePassword) {
                true -> {
                    player.sendMessage("Successfully logged in!")
                    // Update name and optionally algorithm while we have plain-text password.
                    database.transaction {
                        if (algorithm.policy.warrantsUpdate) {
                            val hash = Algorithm.Default.hash(plainText)

                            sql.updatePasswordAlgorithm(
                                uuid = uuid.toString(),
                                algorithm = Algorithm.Default.name,
                                hash = hash,
                            )
                        }
                        sql.setName(uuid = uuid.toString(), name = player.name)
                    }
                    playersQueue[uuid] = Status.AUTHENTICATED
                }

                false -> {
                    player.sendMessage("Incorrect password! Try again!")
                }

                null -> {
                    player.sendMessage("Unknown algorithm for password...")
                }
            }
        }

        Status.AUTHENTICATED -> player.sendMessage("You already registered and authentificated!")
    }
    return true
}

private val tab: List<String>? = null // Nothing to complete here

internal object Login : PlayerCommand {
    override fun run(
        player: Player, args: Array<out String>
    ): Boolean = exec(player, args)

    override fun complete(
        args: Array<out String>
    ): List<String>? = tab
}
