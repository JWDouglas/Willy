package me.laravieira.willy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.logging.Logger;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.VoiceChannel;
import me.laravieira.willy.bitly.Bitly;
import me.laravieira.willy.config.Config;
import me.laravieira.willy.config.MyLogger;
import me.laravieira.willy.discord.Discord;
import me.laravieira.willy.kernel.Context;
import me.laravieira.willy.noadm.NoADM;
import me.laravieira.willy.player.DiscordPlayer;
import me.laravieira.willy.watson.Watson;
import me.laravieira.willy.web.Server;
import me.laravieira.willy.youtube.Youtube;

public class Command {
	
	private static Logger console = MyLogger.getConsoleLogger();
	private static Thread line    = null;
	
	public static void onCommand(String command, String[] args) {
		Thread thread = new Thread(() -> {
			if(command.equals("stop"))           {Willy.stop();
			}else if(command.equals("status"))   {status(args);
			}else if(command.equals("talk"))     {talk(args);
			}else if(command.equals("youtube"))  {youtube(args);
			}else if(command.equals("short"))    {shortLink(args);
			}else if(command.equals("contexts")) {contexts(args);
			}else if(command.equals("player"))   {player(args);
			}else if(command.equals("noadm"))    {noadm(args);
			}else if(command.equals("help"))     {help(args);
			}else {unknow();}
		});
		thread.setDaemon(true);
		thread.start();
	}
	
	private static void status(String[] args) {
		console.info("-------------------------- Status ----------------------------");
		console.info("Discord: "+(Discord.isConnected()?"Connected":"Disconnected"));
		console.info("Watson: "+(Watson.isSessionRegistered()?"Connected":"Disconnected"));
		console.info("Contexts: "+Context.getContexts().size()+" in use");
		console.info("Web Server: "+(Server.isWorking()?"Working":"Stopped"));
		console.info("Web Connections: "+Server.howConnections()+" openned");
		console.info("Bitly Use: "+Config.getBitlyUse());
		console.info("Music Players: "+DiscordPlayer.getPlayers().size()+" loaded");
		long time  = (new Date().getTime()-Willy.getStartTime())/1000;
		console.info("UpTime: "+time/(3600*24)+"d, "+(time%(3600*24))/(3600)+"h, "+(time%(3600)/60)+"m, "+time%60+"s.");
		console.info("---------------------------------------------------------------");
	}

	private static void youtube(String[] args) {
		Youtube ytd = null;
		if(args.length == 2 && args[1].startsWith("https") && args[1].contains("youtube.com") && args[1].contains("v=")) {
			ytd = new Youtube(args[1]);
			if(ytd.getVideo()) {
				ytd.autoChooseAnyFormat(null);
				console.info(" "+ytd.getDownloadLink());
			}
		}else if(args.length == 3 && (args[1].equalsIgnoreCase("any")
				|| args[1].equalsIgnoreCase("best")
				|| args[1].equalsIgnoreCase("good")
				|| args[1].equalsIgnoreCase("medium")
				|| args[1].equalsIgnoreCase("poor"))
		&& args[2].startsWith("https") && args[2].contains("youtube.com") && args[2].contains("v=")) {
			ytd = new Youtube(args[2]);
			if(ytd.getVideo()) {
				ytd.autoChooseAnyFormat(args[1]);
				console.info(" "+ytd.getDownloadLink());
			}
		}else if(args.length < 2)
			console.info("You have to enter the youtube video link.");
		else
			console.info("This entered argument it's not a valid youtube video link.");
	}

	private static void shortLink(String[] args) {
		if(Bitly.canUse) {
			if(args.length > 1 && args[1].length() > 10
			&& args[1].startsWith("http") && args[1].contains(".") && args[1].contains("/")) {
				String shortLink = new Bitly(args[1]).getShorted();
				if(shortLink == null || shortLink == args[1] || shortLink.isEmpty())
					console.info("Can't short link, maybe it's already short enouth.");
				else console.info("Smallest link: "+shortLink);
			}else console.info("This entered argument it's not a valid link.");
		}else console.info("Bitly is not enabled, check config file.");
	}
	
	private static void contexts(String[] args) {
		long   now = new Date().getTime()+10000;
		console.info("-------------------------- Contexts ---------------------------");
		Context.getContexts().forEach((identifier, context) -> {
			console.info("Context "+identifier+" expire in "+((context.getContextExpire()-now)/1000)+"s.");
		});
		console.info("---------------------------------------------------------------");
	}
	
	private static void talk(String[] args) {
		if(args.length > 2 && args[1].equalsIgnoreCase("debug")) {
			String message = "";
			for(int i = 2; i < args.length; i++)
				message += " "+args[i];
			Context context = Context.getConsoleContext();
			context.setDebugWatsonMessage(true);
			context.getWatsonMessager().sendTextMessage(message.substring(1));
		}else if(args.length > 1) {
			String message = "";
			for(int i = 1; i < args.length; i++)
				message += " "+args[i];
			Context.getConsoleContext().getWatsonMessager().sendTextMessage(message.substring(1));
		}else {
			console.info("You need to type a message after 'talk' command.");
		}
	}
	
	private static void player(String[] args) {
		if(args.length > 1) {
			VoiceChannel channel = (VoiceChannel)Discord.getBotGateway().getChannelById(Snowflake.of(Config.getPlayerDefaultChannel())).block();
			DiscordPlayer player = DiscordPlayer.createDiscordPlayer(channel);
			if(args.length > 2 && args[1].equalsIgnoreCase("add")) {
				player.add(args[2]);
			}else if(args[1].equalsIgnoreCase("play")) {
				player.play();
			}else if(args[1].equalsIgnoreCase("resume")) {
				player.resume();
			}else if(args[1].equalsIgnoreCase("pause")) {
				player.pause();
			}else if(args[1].equalsIgnoreCase("stop")) {
				player.stop();
			}else if(args[1].equalsIgnoreCase("next")) {
				player.next();
			}else if(args[1].equalsIgnoreCase("clear")) {
				player.clear();
			}else if(args[1].equalsIgnoreCase("destroy")) {
				player.destroy();
			}else if(args[1].equalsIgnoreCase("info")) {
				console.info("-------------------- Default Player Info ----------------------");
				console.info("Player status: "+(player.getPlayer().getPlayingTrack() == null?"EMPTY":player.getPlayer().getPlayingTrack().getState().name()));
				console.info("Playing track: "+(player.getPlayer().getPlayingTrack() == null?"EMPTY":player.getPlayer().getPlayingTrack().getInfo().title));
				console.info("Next track: "+(player.getTrackScheduler().getNext() == null?"EMPTY":player.getTrackScheduler().getNext().getInfo().title));
				console.info("Queue size: "+player.getTrackScheduler().getQueue().size());
				console.info("Channel status: "+(player.getChannel().isMemberConnected(Snowflake.of(Config.getDiscordID())).block()?"CONNECTED":"DISCONNECTED"));
				console.info("Channel identifier: "+player.getChannel().getName()+" ("+player.getChannel().getId().asString()+")");
				console.info("---------------------------------------------------------------");
			}else {
				console.info("Undefined command, type 'help' to see usage.");
			}
		}else {
			console.info("This command need sub command, type 'help' to see usage.");
		}
	}
	
	private static void noadm(String[] args) {
		if(args.length > 1) {
			if(args.length > 3 && args[1].equalsIgnoreCase("ban")) {
				String reason = "";
				for(int i = 4; i < args.length; reason += args[i++]);
				NoADM.ban(args[2], args[3], reason);
			}else if(args[1].equalsIgnoreCase("unban")) {
				String reason = "";
				for(int i = 4; i < args.length; reason += args[i++]);
				NoADM.unban(args[2], args[3], reason);
			}else if(args[1].equalsIgnoreCase("op")) {
				NoADM.op(args[2], args[3]);
			}else if(args[1].equalsIgnoreCase("deop")) {
				NoADM.deop(args[2], args[3]);
			}else if(args[1].equalsIgnoreCase("mperms")) {
				NoADM.listMemberPermissions(args[2], args[3]);
			}else if(args[1].equalsIgnoreCase("rperms")) {
				NoADM.listRolePermissions(args[2], args[3]);
			}else if(args[1].equalsIgnoreCase("cmperms")) {
				NoADM.listChannelMemberPermissions(args[2], args[3], args[4]);
			}else if(args[1].equalsIgnoreCase("crperms")) {
				NoADM.listChannelRolePermissions(args[2], args[3], args[4]);
			}else {
				console.info("use: noadm [cmd] [guild_id] [member_id] reason");
			}
		}else {
			console.info("This command need sub command, type 'help' to see usage.");
		}
	}
	
	private static void help(String[] args) {
		console.info("---------------------------------------------------------------");
		console.info("---                        Help Page                        ---");
		console.info("---------------------------------------------------------------");
		console.info("status         : Show Willy general status.");
		console.info("stop           : Close all connections and stop Willy.");
		console.info("talk           : Send a message to Willy, response will be printed.");
		console.info("talk debug     : Send a message to Willy, full debug response will be printed.");
		console.info("contexts       : List all contexts existent.");
		console.info("help           : Show all know commands and their descriptions.");
		console.info("player add     : Add on queue and play music on default channel.");
		console.info("player play    : Play music on player instance.");
		console.info("player resume  : Play music on player instance.");
		console.info("player pause   : Pause music on player instance.");
		console.info("player stop    : Stop music on player instance.");
		console.info("player next    : Go to next music on queue.");
		console.info("player clear   : Clear queue of player instance.");
		console.info("player destroy : Destroy the player instance.");
		console.info("player info    : See info about the player instance.");
		console.info("noadm ban      : Ban a member.");
		console.info("noadm unban    : Unban a member.");
		console.info("noadm op       : Give OP to a member.");
		console.info("noadm deop     : Remove OP of a member.");
		console.info("noadm mperms   : Show all member permissions.");
		console.info("noadm rperms   : Show all role permissions.");
		console.info("noadm cmperms  : Show all member permissions overwrite on channel.");
		console.info("noadm crperms  : Show all role permissions overwrite on channel.");
		console.info("---------------------------------------------------------------");
	}
	
	private static void unknow() {
		console.info("Unknow command, to get help type 'help'.");
	}
	
    public static void startLineReader() {
    	if(line == null) {
	    	line = new Thread(() -> {
	    		try {
	    			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
					while(!Willy.getStop()) {
						String[] commandLine = input.readLine().split(" ");
						if(commandLine.length > 0) {
							String com = commandLine[0].toLowerCase();
							
							// Remove prefix of first command
							if(com.startsWith("/") || com.startsWith("\\") || com.startsWith("!") || com.startsWith("'")
							|| com.startsWith("?") || com.startsWith("#")  || com.startsWith("@") || com.startsWith("\""))
								com = com.substring(1);
							
							// Remove suffix of first command
							if(com.endsWith("\"") || com.endsWith("'"))
								com = com.substring(0, com.length()-1);
							
							Command.onCommand(com, commandLine);
						}
					}
				} catch (IOException e) {
					MyLogger.getLogger().warning("Command Line is not available here, sorry.");
				}
	    	});
	    	line.setDaemon(true);
	    	line.start();
    	}
    }
    
}
