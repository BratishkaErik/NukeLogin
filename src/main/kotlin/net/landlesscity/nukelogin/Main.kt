// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import net.landlesscity.nukelogin.commands.Login
import net.landlesscity.nukelogin.commands.Register
import net.landlesscity.nukelogin.listeners.PlayerListener
import net.landlesscity.nukelogin.sql.SQLite
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Main : JavaPlugin() {
    override fun onEnable() {
        if (!dataFolder.exists()) dataFolder.mkdir()

        val databasePath = dataFolder.resolve("main.db")
        logger.info("Loading database ${databasePath}...")
        val driver = JdbcSqliteDriver(
            "jdbc:sqlite:${databasePath}",
            Properties().apply {
                "encoding" to "UTF-8"
                "journal_mode" to "WAL"
                "foreign_keys" to true
                "trusted_schema" to false
            },
            SQLite.Schema,
        )
        database = SQLite(driver)

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
    }

    internal companion object {
        internal lateinit var database: SQLite
    }
}
