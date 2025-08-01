# Tecrido
Discord Bot - Kotlin + Kord

A Discord bot built with **Kotlin** using the **Kord** library.

---

## ✨ Features
- Get total member count of the server.
- Send messages to Discord channels.
- Fully asynchronous with Kotlin Coroutines.
- Simple and clean architecture, ready to extend.

---

## 🛠 Requirements
- **Java 17+**
- **Gradle (Kotlin DSL)**
- **Kord 0.13.0**
- **Kotlin Coroutines**
- A Discord Bot Token with enabled Intents.

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/your-bot.git
cd your-bot
```

### 2. Configure Your Bot Token
Open `src/main/kotlin/Bot.kt` and replace `"YOUR_BOT_TOKEN"` with your actual bot token:
```kotlin
val kord = Kord("YOUR_BOT_TOKEN")
```

### 3. Enable Privileged Gateway Intents
You **must enable intents** in the Discord Developer Portal, otherwise the bot will fail to connect.

Steps:
1. Go to [Discord Developer Portal](https://discord.com/developers/applications).
2. Select your bot application.
3. From the sidebar, click on **Bot**.
4. Scroll down to the section **Privileged Gateway Intents**.
5. Enable the following:
    - ✅ SERVER MEMBERS INTENT
    - ✅ MESSAGE CONTENT INTENT
    - (Optional) PRESENCE INTENT (Only if needed)
6. Click **Save Changes**.

### 4. Build & Run the Project
You can build and run the project using Gradle.

#### On Linux / macOS:
```bash
./gradlew build
./gradlew run
```

#### On Windows:
```bash
gradlew.bat build
gradlew.bat run
```

---

## 🧑‍💻 Example: Get Server Member Count
Example function to get the total number of members in a guild and send it to a channel:
```kotlin
import dev.kord.core.entity.channel.TextChannel
import kotlinx.coroutines.flow.count

suspend fun sendMemberCount(channel: TextChannel) {
    val guild = channel.getGuild()
    val memberCount = guild.members.count()
    channel.createMessage("Total members: $memberCount")
}
```

---

## 📂 Project Structure
```
your-bot/
 ├── build.gradle.kts
 ├── settings.gradle.kts
 └── src/
     └── main/
         └── kotlin/
             └── Bot.kt
```

---

## 📦 Dependencies
- [Kord](https://github.com/kordlib/kord)
- [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- [Ktor Client](https://ktor.io/)

---

## ⚠️ Important Notes
- Ensure your bot has permissions to **Read Messages**, **View Channels**, and **Read Member List** in your Discord server.
- Without enabling the required intents in Developer Portal, you will get this error:
  - `Gateway closed: 4014 Disallowed intent(s)`
- Keep your bot token **private** and **never share it publicly**.
- If you need the bot to read message content, you must enable **MESSAGE CONTENT INTENT**.

---

## 📄 License
[MIT License](https://github.com/omslx/tecrido/blob/main/LICENSE)
