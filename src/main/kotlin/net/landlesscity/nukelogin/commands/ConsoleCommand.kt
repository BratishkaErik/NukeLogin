// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.TabExecutor

internal sealed interface ConsoleCommand : TabExecutor {
    fun run(console: ConsoleCommandSender, args: Array<out String>): Boolean
    fun complete(args: Array<out String>): List<String>?

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is ConsoleCommandSender) {
            sender.sendMessage("Only console can run this command!")
            return true
        }

        return run(sender, args)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String>? = complete(args)
}
