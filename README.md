# Message Stats Bot
A Discord bot that generates message statistics

## Usage
This bot does not have a public instance. If you want to use it, you will have to host it yourself.
It's assumed by default that you use IntelliJ IDEA as your IDE and know how to use it.
Please note that this bot is not intended to be used as a public bot. It is intended to be used in a few guilds at most. It does not automatically handle data left by guilds that it is no longer in, although, instructions on how to do it manually are provided.
It is also assumed that you know how to create a bot application in the Discord Developer Portal and how to invite it to your guild. Should you need any help, please check the [JDA Wiki](https://jda.wiki/using-jda/getting-started/)
1. Clone the repository
2. Open the project in IntelliJ IDEA
3. Build the project
4. On the right side of the IDE, click on the Gradle tab
5. Navigate to Tasks > shadow > shadowJar
6. Now, a jar file should have been generated in the Message-Stats-Bot > build > libs folder
7. Before running it you will need to set an environment variable called `BOT_TOKEN` with your bot's token.

### Linux

If you are using Linux, you can create a file called `run.sh` with the following contents:
```
   #!/bin/bash
   export BOT_TOKEN=your-bot-token
   java -jar /path/to/your/file.jar
```
Make sure to replace `your-bot-token` with your bot's token and `/path/to/your/file.jar` with the path to the jar file you generated earlier.
Then, you can run the bot by executing `./run.sh` in the terminal.

### Windows

If you are using Windows, you can create a file called `run.bat` with the following contents:
```
   set BOT_TOKEN=your-bot-token
   java -jar C:\path\to\your\file.jar
```
Make sure to replace `your-bot-token` with your bot's token and `C:\path\to\your\file.jar` with the path to the jar file you generated earlier.
Then, you can run the bot by executing `run.bat` in the command prompt.

### MacOS

If you are using MacOS, please follow the Linux instructions. Both MacOS and Linux use Unix-based command line interfaces, so the process is the same.

## How to delete leftover data

This bot uses Realm as its database. Realm stores its data in files with the extension `.realm` (by default it has no extension but thats a matter of configuration). The bot stores its data in a file called `channelstats.realm` in the root directory of the bot. You can open this file using [Realm Studio](https://github.com/realm/realm-studio/releases)
To delete leftover data, you will need to open the file in Realm Studio and delete the data manually. If you wish to delete data related to a certain guild, you'll first need to know it's guild id. After that, you can delete it's corresponding `RealmGuild` and all references to it will become null. Make sure to delete all null references as they're going to cause problems.
The file can be edited while the bot is running, but it is recommended to stop the bot before editing it.

## Commands

The bot only interacts through slash commands. Here is a list:
- `yesterday` - Shows the message stats for the previous day (according to the host's timezone)
- `today` - Shows the ongoing message stats for the current day (according to the host's timezone)
- `setchannel` - Sets the channel where the bot will send the message stats.
- `info` - Shows information about the bot

By default, these commands (with the exception of `info`) can only be used by users with the `Manage Server` permission.
