import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.commands.slash
import dev.minn.jda.ktx.interactions.commands.updateCommands
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

fun updateCommands(jda: JDA) {
    jda.updateCommands {
        slash("yesterday","Shows the message stats for yesterday (bot host time)") {
            restrict(guild = true, Permission.MANAGE_SERVER)
        }
        slash("today","Shows the ongoing message stats for today (bot host time)") {
            restrict(guild = true, Permission.MANAGE_SERVER)
        }
        slash("setchannel","Sets the channel where the bot will send its stats daily") {
            option<TextChannel>("channel", "The channel to send the stats to", required = true)
            restrict(guild = true, Permission.MANAGE_SERVER)
        }
        slash("info","Shows info about the bot")
    }.queue()
}