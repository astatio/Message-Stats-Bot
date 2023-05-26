import dev.minn.jda.ktx.events.listener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

fun startListeners(jda: JDA) {
    jda.listener<MessageReceivedEvent> {
        onMessageReceived(it)
    }
    jda.listener<SlashCommandInteractionEvent> {
        onSlashCommandInteraction(it)
    }
}