import dev.minn.jda.ktx.messages.Embed
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.TimeFormat
import net.dv8tion.jda.api.utils.TimeUtil
import net.dv8tion.jda.api.utils.Timestamp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.*
import java.time.format.DateTimeFormatter

suspend fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
    when (event.fullCommandName) {
        "yesterday" -> yesterdayCommand(event)
        "today" -> todayCommand(event)
        "setchannel" -> setChannelCommand(event, event.getOption("channel")!!.asChannel)
        "info" -> infoCommand(event)
    }
}

fun customEmbed(
    title: String,
    fields: Map<GuildChannel, Long>
): MessageEmbed {
    return Embed {
        this.title = title
        color = embColor
        fields.forEach {
            field {
                name = it.key.asMention
                value = dec.format(it.value)
                inline = true
            }
        }
        footer {
            name = "Current host time: ${dtFormatter.format(LocalTime.now())}"
        }
    }
}

fun yesterdayCommand(event: SlashCommandInteractionEvent) {
    val yesterdayStats = mapYesterdayStatistics(event)
    event.replyEmbeds(customEmbed("Yesterday's message stats", yesterdayStats)).queue()
}

fun todayCommand(event: SlashCommandInteractionEvent) {
    val todayStats = mapTodayStatistics(event)
    event.replyEmbeds(customEmbed("Today's ongoing message stats", todayStats)).queue()
}

fun infoCommand(event: SlashCommandInteractionEvent) {
    val tomorrow = LocalDate.now().plusDays(1)
    val nextMidnight = LocalDateTime.of(tomorrow, LocalTime.MIDNIGHT)
    event.replyEmbeds(
        Embed {
            title = "Info"
            color = embColor
            field {
                value = "This bot provides statistics about the messages sent in this server." +
                        "Each 'day' displayed here is based on the host's time zone." +
                        "The bot is currently hosted in the UTC ${OffsetDateTime.now().offset}" +
                        "The next stats announcement will be sent at ${TimeFormat.TIME_SHORT.format(nextMidnight)}" +
                        "For more information, please check the GitHub page: https://github.com/astatio/Message-Stats-Bot"
            }
            footer {
                name = "Current host time: ${dtFormatter.format(LocalTime.now())}"
            }
        }
    ).queue()
}

suspend fun setChannelCommand(event: SlashCommandInteractionEvent, chosenChannel: GuildChannelUnion) {
    runCatching {
        realm.write {
            val query = realm.query<BotSettings>("realmGuild.id == $0", event.guild!!.idLong).first().find() ?: BotSettings().create(this, event.guild!!.idLong, null)
            query.apply {
                botChannel = chosenChannel.idLong
            }.also {
                copyToRealm(it, UpdatePolicy.ALL)
            }
        }
    }.onFailure {
        it.printStackTrace()
        event.reply("❌ An error occurred while setting the channel. Please try again later.").queue()
        return
    }.onSuccess {
        val tomorrow = LocalDate.now().plusDays(1)
        val nextMidnight = LocalDateTime.of(tomorrow, LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault())
        event.reply("✅ ${chosenChannel.asMention} has been set as the stats announcement channel. To note: the next announcement will be sent on this channel at ${
            TimeFormat.TIME_SHORT.format(nextMidnight)
        }").queue()
    }
}




val dtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val symbols = DecimalFormatSymbols().apply { groupingSeparator = ' ' }
val dec = DecimalFormat("###,###", symbols)
const val embColor = 0x2b2d31

fun mapYesterdayStatistics(event: SlashCommandInteractionEvent): Map<GuildChannel, Long> {
    val query = realm.query<ChannelStats>("realmGuild.id == $0", event.guild!!.idLong).find()

    return query.mapNotNull {
        event.guild!!.getTextChannelById(it.id!!)?.let { channel -> Pair(channel, it) }
    }.associate { (channel, realmChannel) -> channel to realmChannel.pastDayTotal }

}

fun mapTodayStatistics(event: SlashCommandInteractionEvent): Map<GuildChannel, Long> {
    val query = realm.query<ChannelStats>("realmGuild.id == $0", event.guild!!.idLong).find()

    return query.mapNotNull {
        event.guild!!.getTextChannelById(it.id!!)?.let { channel -> Pair(channel, it) }
    }.associate { (channel, realmChannel) -> channel to realmChannel.messages }

}