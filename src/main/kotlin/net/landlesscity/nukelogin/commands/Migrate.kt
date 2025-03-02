// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin.commands

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import main.sqlite.Passwords_current.Hash
import net.landlesscity.nukelogin.Algorithm
import net.landlesscity.nukelogin.Main
import net.landlesscity.nukelogin.Main.Companion.cwd
import net.landlesscity.nukelogin.Main.Companion.server
import org.bukkit.command.ConsoleCommandSender
import userlogin.sqlite.UserLoginSQLiteDatabase
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private val availablePlugins = listOf("userlogin-sqlite")

@Suppress("ReturnCount", "LongMethod")
private fun exec(
    console: ConsoleCommandSender,
    args: Array<out String>,
): Boolean {
    val pluginName = args.getOrElse(0) {
        return false // Print usage
    }
    when (pluginName) {
        "userlogin-sqlite" -> {}
        else -> {
            console.sendMessage(
                "Unknown plugin for migration: ${pluginName}. Available plugins: ${
                    availablePlugins.joinToString(
                        ", "
                    )
                }"
            )
            return true
        }
    }
    val databaseRelativePath = args.getOrElse(1) {
        console.sendMessage("You need to pass path to the SQLite database of UserLogin plugin as a second argument.")
        return true
    }
    val databasePath = cwd.resolve(databaseRelativePath).normalize()
        .also {
            if (!it.exists()) {
                console.sendMessage("Database not found at ${it}!")
                return true
            }
            it.toRealPath()
        }.also {
            if (it.isDirectory()) {
                console.sendMessage("Should pass file, but found directory instead!")
                return true
            }
        }
    console.sendMessage("Converting ${databasePath}")

    // No scheme here since we assume database already exist.
    val driver = JdbcSqliteDriver(
        "jdbc:sqlite:file:${databasePath}?mode=ro",
        Properties().apply {
            setProperty("encoding", "\"UTF-8\"")
        },
    )
    driver.use {
        val userloginDatabase = UserLoginSQLiteDatabase(driver)
        val allPlayers = userloginDatabase.playerQueries.getAll().executeAsList()
        Main.Companion.database.transaction {
            @Suppress("MagicNumber")
            for ((uuidString, oldHash) in allPlayers) {
                val uuid: UUID = UUID.fromString(uuidString)
                val name: String? = server.getPlayer(uuid)?.name

                // If player already exist, remove it to avoid conflicts
                Main.Companion.database.playerQueries.unregisterPlayer(
                    uuid = uuid.toString()
                )
                Main.Companion.database.playerQueries.registerPlayer(
                    uuid = uuidString,
                    name = name,
                    algorithm = Algorithm.BCRYPT_USERLOGIN_2022.name,
                    hash = Hash(oldHash.toByteArray()),
                )
            }
        }
    }
    console.sendMessage("Migration succesfully finished!")
    return true
}

private fun tab(args: Array<out String>): List<String>? {
    return when (args.size) {
        1 -> availablePlugins.filter { it.startsWith(args[0]) }
        else -> null
    }
}

internal object Migrate : ConsoleCommand {
    override fun run(console: ConsoleCommandSender, args: Array<out String>): Boolean = exec(console, args)
    override fun complete(args: Array<out String>): List<String>? = tab(args)
}
