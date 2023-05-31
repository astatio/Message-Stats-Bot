import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.jdabuilder.createJDA
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.TimeFormat
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RealmGuild : RealmObject {
    @PrimaryKey
    var id: Long? = null
}

class BotSettings : RealmObject {
    var botChannel: Long? = null
    var realmGuild: RealmGuild? = null
}

class ChannelStats : RealmObject {
    @PrimaryKey
    var id: Long? = null
    var pastDayTotal: Long = 0
    var messages: Long = 0
    var realmGuild: RealmGuild? = null
}

val realm = Realm.open(
    RealmConfiguration.Builder(
        schema = setOf(RealmGuild::class, ChannelStats::class, BotSettings::class),
    ).name("channelstats.realm").build()
)


val logger = LoggerFactory.getLogger(main()::class.java)

//The purpose of this bot is to track the number of messages sent per text channel on a guild per day
//Every midnight the stats are prepared and sent to the bot channel
fun main() {
    val token = System.getenv("BOT_TOKEN") ?: throw IllegalArgumentException("No token found")

    val jda = createJDA(
        token,
        intents = listOf(
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MESSAGES
        )
    ) {
        disableCache(
            CacheFlag.EMOJI,
            CacheFlag.STICKER,
            CacheFlag.ONLINE_STATUS,
            CacheFlag.SCHEDULED_EVENTS,
            CacheFlag.ACTIVITY,
            CacheFlag.CLIENT_STATUS,
            CacheFlag.MEMBER_OVERRIDES,
            CacheFlag.VOICE_STATE
        )
        setActivity(Activity.listening("to your messages"))

    }.awaitReady()
    startListeners(jda)
    updateCommands(jda)


    val scheduledExecutorService = Executors.newScheduledThreadPool(1)
    val scope = getDefaultScope()
    val runTime = LocalTime.of(0, 0) // 00:00 - for testing purposed this can be adjusted to a few minutes ahead of the current time
    // Calculate the initial delay by getting the difference between now and the next run time
    var initialDelay = LocalTime.now().until(runTime, ChronoUnit.MINUTES)
    // To avoid it sending immediately on startup, add 24 hours to it
    if (initialDelay < 0) {
        initialDelay += TimeUnit.DAYS.toMinutes(1)
    }
    // Calculate the period between consecutive executions (24 hours in this case)
    val period = TimeUnit.DAYS.toMinutes(1)
    scheduledExecutorService.scheduleAtFixedRate({
        scope.launch {
            // the bot will get the stats from the current day, and send them to the channel set by the user
            // the bot will copy the stats to the pastDayTotal, and reset the messages to 0
            // the bot will check if there's any invalid channel, and delete it from the database
            val map = mutableMapOf<Long, MutableMap<GuildChannel, Long>>()
            try {
                realm.writeBlocking {
                    val query = this.query<ChannelStats>().find()
                    query.forEach {
                        // if the channel is invalid, delete it
                        val channel = jda.getGuildById(it.realmGuild!!.id!!)?.getTextChannelById(it.id!!)
                        if (channel == null) {
                            delete(it)
                            return@forEach
                        }
                        it.apply {
                            pastDayTotal = messages
                            messages = 0
                        }.also { stats ->
                            copyToRealm(stats, UpdatePolicy.ALL)
                        }
                        // if the channel is valid, add it to the map
                        map.getOrPut(it.realmGuild!!.id!!) { mutableMapOf() }[channel] = it.pastDayTotal
                    }
                }

                // check bot settings, and send the stats to the appropriate channel
                realm.query<BotSettings>().find().forEach {
                    val channel = jda.getGuildById(it.realmGuild!!.id!!)?.getTextChannelById(it.botChannel!!)
                    channel?.sendMessageEmbeds(
                        customEmbed(
                            "Here are the stats for ${TimeFormat.DATE_LONG.format(ZonedDateTime.now().minusMinutes(2))}",
                            map[it.realmGuild!!.id!!]!!
                        )
                    )?.queue()
                }
            } catch (e: Exception) {
                logger.error("An exception occurred while trying to send the stats", e)
            }
        }
    }, initialDelay, period, TimeUnit.MINUTES)
}

suspend fun onMessageReceived(event: MessageReceivedEvent) {
    if (!event.isFromGuild) return
    if (event.author.isBot) return
    // if a channel for announcements hasn't been set, return. No need to track messages if there's no channel to send the stats to.
    // start counting the number of messages sent per channel
    // if the channel is not in the database, add it
    // if the channel is in the database, increment the number of messages
    realm.query<BotSettings>("realmGuild.id == $0", event.guild.idLong).first().find() ?: return
    realm.write {
        val query =
            this.query<ChannelStats>("realmGuild.id == $0 AND id == $1", event.guild.idLong, event.channel.idLong)
                .first().find() ?: ChannelStats().create(this, event.guild.idLong, event.channel.idLong)
        query.apply {
            messages++
        }.also {
            copyToRealm(it, UpdatePolicy.ALL)
        }
    }
}


inline fun <reified T : RealmObject> T.create(
    transaction: MutableRealm,
    guildId: Long,
    channelId: Long?
): T {
    val clazz = this::class.java
    var newInstance = clazz.getDeclaredConstructor().newInstance()

    when (newInstance) {
        is BotSettings -> newInstance.apply {
            realmGuild = RealmGuild().apply { id = guildId }
        }

        is ChannelStats -> newInstance.apply {
            id = channelId
            realmGuild = RealmGuild().apply { id = guildId }
        }
    }
    newInstance = transaction.copyToRealm(newInstance, UpdatePolicy.ALL)
    return newInstance as T
}