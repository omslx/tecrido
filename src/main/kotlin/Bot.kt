import dev.kord.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.*
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds



// --- CONFIGURATION ---
private const val BOT_TOKEN = "" // TODO: Replace with your bot's token
private const val MINECRAFT_SERVER_IP = "" // TODO: Replace with your Minecraft server IP
private val TARGET_CHANNEL_ID = Snowflake("") // TODO: Replace with your channel ID

// --- Data classes for parsing mcsrvstat.us API response ---
@Serializable
data class ServerStatus(
    val online: Boolean,
    val ip: String? = null,
    val motd: Motd? = null,
    val players: Players? = null,
    val icon: String? = null
)

@Serializable
data class Motd(
    val clean: List<String>
)

@Serializable
data class Players(
    val online: Int,
    val max: Int
)

// --- Helper for JSON parsing ---
private val json = Json { ignoreUnknownKeys = true }

// --- HTTP Client ---
private val httpClient = HttpClient(CIO)

/**
 * Fetches the status of a Minecraft server using the mcsrvstat.us API.
 * @param serverIp The IP address of the Minecraft server.
 * @return A [ServerStatus] object, or null if the request fails.
 */
private suspend fun fetchServerStatus(serverIp: String): ServerStatus? {
    return try {
        val response = httpClient.get("https://api.mcsrvstat.us/2/$serverIp")
        json.decodeFromString<ServerStatus>(response.body())
    } catch (e: Exception) {
        println("Error fetching server status: ${e.message}")
        null
    }
}

/**
 * Generates a PNG image displaying the Minecraft server status.
 * @param status The [ServerStatus] object containing the server data.
 * @param serverIp The IP address of the server, used as a fallback title.
 * @return A [ByteArray] containing the PNG data of the generated image.
 */
private fun generateStatusImage(status: ServerStatus, serverIp: String): ByteArray {
    val width = 500
    val height = 150
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2d = image.createGraphics()

    // Enable anti-aliasing' for better quality
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    if (status.online) {
        // Background
        g2d.color = Color(45, 45, 45)
        g2d.fillRect(0, 0, width, height)

        // Draw Favicon
        status.icon?.let {
            try {
                val base64Data = it.substringAfter("base64,")
                val imageBytes = Base64.getDecoder().decode(base64Data)
                val favicon = ImageIO.read(ByteArrayInputStream(imageBytes))
                g2d.drawImage(favicon, 20, 20, 64, 64, null)
            } catch (e: Exception) {
                println("Failed to decode or draw favicon: ${e.message}")
                // Draw a placeholder if favicon fails
                g2d.color = Color.DARK_GRAY
                g2d.fillRect(20, 20, 64, 64)
            }
        }

        // Draw Text Info
        g2d.color = Color.WHITE
        g2d.font = Font("SansSerif", Font.BOLD, 24)
        g2d.drawString(serverIp, 100, 45)

        g2d.font = Font("SansSerif", Font.PLAIN, 18)
        val players = status.players?.let { "${it.online} / ${it.max}" } ?: "N/A"
        g2d.drawString("Players: $players", 100, 75)

        // Draw MOTD
        g2d.font = Font("Monospaced", Font.PLAIN, 14)
        status.motd?.clean?.forEachIndexed { index, line ->
            if (index < 2) { // Limit to 2 lines of MOTD
                g2d.drawString(line, 20, 110 + (index * 18))
            }
        }
    } else {
        // Offline Status Background
        g2d.color = Color(80, 20, 20)
        g2d.fillRect(0, 0, width, height)

        // Offline Text
        g2d.color = Color.WHITE
        g2d.font = Font("SansSerif", Font.BOLD, 28)
        val text = "Server is Offline"
        val textWidth = g2d.fontMetrics.stringWidth(text)
        g2d.drawString(text, (width - textWidth) / 2, height / 2 + 10)
    }

    g2d.dispose()

    // Convert image to byte array
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, "png", outputStream)
    return outputStream.toByteArray()
}


@OptIn(PrivilegedIntent::class)
suspend fun main() = coroutineScope {
    if (BOT_TOKEN == "YOUR_BOT_TOKEN_HERE" || TARGET_CHANNEL_ID.toString() == "123456789012345678") {
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!! PLEASE CONFIGURE THE BOT TOKEN AND CHANNEL ID IN   !!!")
        println("!!! Bot.kt BEFORE RUNNING THE BOT.                     !!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        return@coroutineScope
    }

    val kord = Kord(BOT_TOKEN)

    var statusMessage: Message? = null
    var usePlayerCountStatus = true

    // A coroutine job that will run the status updates
    launch {
        // Wait until the bot is connected and ready
        kord.on<ReadyEvent> {
            println("Bot is ready! Logged in as ${kord.getSelf().username}")
            val channel = kord.getChannel(TARGET_CHANNEL_ID)
            if (channel == null || channel !is TextChannel) {
                println("Error: Target channel not found or is not a text channel. Please check TARGET_CHANNEL_ID.")
                kord.logout()
                return@on
            }

            println("Starting status update loop for channel #${channel.name}...")
            // The main loop for updating the status
            while (true) {
                val serverStatus = fetchServerStatus(MINECRAFT_SERVER_IP)

                // Update Bot Presence
                if (usePlayerCountStatus) {
                    val playersOnline = serverStatus?.players?.online ?: 0
                    kord.editPresence {
                        playing(name = "$playersOnline players online on Minecraft")
                    }
                } else {
                    val memberCount = channel.guild.members.count() ?: "N/A"
                    kord.editPresence {
                        watching(name = "$memberCount members in Discord")
                    }
                }
                usePlayerCountStatus = !usePlayerCountStatus

                // Send or Update Message
                if (serverStatus != null) {
                    val imageBytes = generateStatusImage(serverStatus, MINECRAFT_SERVER_IP)
                    val messageContent = "Status updated: ${Instant.now()}"
                    val fileName = "status.png"

                    if (statusMessage == null) {
                        // Create the initial message
                        statusMessage = channel.createMessage {
                            content = messageContent
                            addFile(fileName, ByteReadPacket(imageBytes))
                            embed(fun EmbedBuilder.() {
 image = "attachment://$fileName"
                                color = if (serverStatus.online) dev.kord.common.Color(76, 175, 80) else dev.kord.common.Color(0xF44336)
})
                        }
                    } else {
                        // Edit the existing message
                        statusMessage?.edit {
                            content = messageContent
                            // Kord's edit function replaces all attachments, so we provide a new list.
                            files.clear()

                            val provider = ChannelProvider { ByteReadChannel(imageBytes) }

                            files.add(NamedFile(fileName, provider))


                            embed {
                                this.image = "attachment://$fileName"
                                color = if (serverStatus.online) dev.kord.common.Color(76, 175, 80) else dev.kord.common.Color(0xF44336)
                            }
                        }
                    }
                    println("Status message updated in #${channel.name}.")
                } else {
                    println("Could not fetch server status. Skipping message update.")
                }
                delay(30.seconds) // Wait for 30 seconds
            }
        }
    }

    println("Logging in with required intents...")
    kord.login {
        intents += Intent.GuildMembers // Required for getMemberCount()
        intents += Intent.Guilds
    }
}

fun addFile(name: String, contentProvider: ByteReadPacket) {}
