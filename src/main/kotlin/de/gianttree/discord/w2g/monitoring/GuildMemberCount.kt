package de.gianttree.discord.w2g.monitoring

import kotlinx.datetime.Instant

data class GuildMemberCount(val lastUpdate: Instant, val memberCount: Int)
