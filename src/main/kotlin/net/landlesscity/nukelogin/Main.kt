// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import main.sqlite.SQLiteDatabase
import net.landlesscity.nukelogin.commands.Login
import net.landlesscity.nukelogin.commands.Migrate
import net.landlesscity.nukelogin.commands.Register
import net.landlesscity.nukelogin.listeners.PlayerListener
import org.bukkit.Server
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class Main : JavaPlugin() {
    override fun onEnable() {
        Companion.cwd = dataFolder.toPath()
        Companion.server = server
        if (!Files.exists(cwd)) {
            Files.createDirectories(cwd)
        }

        val databasePath = dataFolder.resolve("nukelogin_sqlite3.db")
        logger.info("Loading database ${databasePath}...")
        val driver = JdbcSqliteDriver(
            "jdbc:sqlite:file:${databasePath}",
            Properties().apply {
                setProperty("encoding", "\"UTF-8\"")
                setProperty("journal_mode", "WAL")
                setProperty("foreign_keys", "true")
                setProperty("trusted_schema", "false")
            },
            SQLiteDatabase.Schema,
        )
        Companion.database = SQLiteDatabase(driver)

        logger.info("Enabling event listeners and commands...")
        this.server.pluginManager.registerEvents(PlayerListener, this)
        this.getCommand("register")!!.apply {
            this.executor = Register
            this.tabCompleter = Register
        }
        this.getCommand("login")!!.apply {
            this.executor = Login
            this.tabCompleter = Login
        }
        this.getCommand("migrate")!!.apply {
            this.executor = Migrate
            this.tabCompleter = Migrate
        }
    }

    internal companion object {
        internal lateinit var cwd: Path
        internal lateinit var server: Server
        internal lateinit var database: SQLiteDatabase
    }
}
