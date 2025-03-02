// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: MPL-2.0

package net.landlesscity.nukelogin

import org.bukkit.entity.Player
import java.util.*

internal val playersQueue: LinkedHashMap<UUID, Status> = LinkedHashMap()

internal enum class Status {
    WAITING_REGISTRATION,
    WAITING_LOG_IN,
    AUTHENTICATED,
}


// TODO what about offline and online mode?
// TODO do I need to use `getOfflinePlayer` here?
// TODO or store some table name <-> UUID?
internal fun Player.todoUuid(): UUID {
    return this.uniqueId
}
