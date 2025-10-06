package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.RemoFactions

fun coerceBoolean(plugin: RemoFactions) = { value: String? ->
    value?.toBooleanStrictOrNull()?.let(::MfFlagValueCoercionSuccess)
        ?: MfFlagValueCoercionFailure(plugin.language["FactionFlagBooleanCoercionFailed"])
}
