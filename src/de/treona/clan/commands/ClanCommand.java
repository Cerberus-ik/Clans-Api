package de.treona.clan.commands;

import de.treona.clan.Clans;
import de.treona.clan.common.Clan;
import de.treona.clan.common.Invite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ClanCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length == 0) {
            commandSender.sendMessage(Clans.PREFIX_COLOR + ChatColor.GRAY + " Created by " + ChatColor.YELLOW + "Cerberus");
            return true;
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Clans.PREFIX + " This is a player only command.");
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) {
            this.createClan(commandSender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("stats")) {
            this.printStats(commandSender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("invite")) {
            this.invitePlayer(commandSender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("delete")) {
            this.delete(commandSender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("kick")) {
            this.kickPlayer(commandSender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("join")) {
            this.acceptInvite(commandSender, args);
            return true;
        }

        return true;
    }

    private void delete(CommandSender commandSender, String[] args){
        Bukkit.getScheduler().runTaskAsynchronously(Clans.getPlugin(), () -> {
            Player player = (Player) commandSender;
            Clan clan = Clans.getClan(player);
            if(clan == null){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " You are not in a clan.");
                return;
            }
            if(!clan.getOwner().equals(player.getUniqueId())){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " Only the owner can delete a clan.");
                return;
            }
            Clans.getDatabaseManager().deleteClan(clan.getClanId());
            player.sendMessage(Clans.PREFIX_COLOR + ChatColor.RED + " You deleted the clan: " + ChatColor.DARK_PURPLE + clan.getClanName());
            clan.getMembers().forEach(uuid -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if(offlinePlayer.isOnline() && !offlinePlayer.getUniqueId().equals(clan.getOwner())){
                    offlinePlayer.getPlayer().sendMessage(Clans.PREFIX_COLOR + ChatColor.RED + " Your clan owner deleted: " + ChatColor.DARK_PURPLE + clan.getClanName());
                }
            });
        });
    }

    private void kickPlayer(CommandSender commandSender, String[] args){
        Bukkit.getScheduler().runTaskAsynchronously(Clans.getPlugin(), () -> {
            Player player = (Player) commandSender;
            if(args.length == 1){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " Specify a player to kick with: " + ChatColor.GRAY + "/clan kick <player>");
                return;
            }
            Clan clan = Clans.getClan(player);
            if(clan == null){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " You are not in a clan.");
                return;
            }
            if(!clan.getOwner().equals(player.getUniqueId())){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " Only the owner can kick players.");
                return;
            }
            OfflinePlayer targetPlayer = Arrays.stream(Bukkit.getOfflinePlayers()).filter(offlinePlayer -> offlinePlayer.getName().equals(args[1])).findFirst().orElse(null);
            if(targetPlayer == null){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " This player does not exist.");
                return;
            }
            if(targetPlayer.getUniqueId().equals(player.getUniqueId())){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " You can't kick yourself.");
                return;
            }
            if(!clan.getMembers().contains(targetPlayer.getUniqueId())){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " This player is not in your clan.");
                return;
            }
            List<UUID> members = clan.getMembers();
            members.remove(targetPlayer.getUniqueId());
            Clans.updateClanMembers(clan, members);
            player.sendMessage(Clans.PREFIX_COLOR + ChatColor.GREEN + " You kicked the player successfully.");
            if(targetPlayer.isOnline()){
                targetPlayer.getPlayer().sendMessage(Clans.PREFIX_COLOR + ChatColor.RED + " You got kicked from " + ChatColor.DARK_PURPLE + clan.getClanName());
            }
        });
    }

    private void acceptInvite(CommandSender commandSender, String[] args){
        Player player = (Player) commandSender;
        if(!Clans.getInviteManager().hasInvite(player)){
            player.sendMessage(Clans.PREFIX_COLOR + " You don't have any invites.");
            return;
        }
        if(args.length == 1){
            player.sendMessage(Clans.PREFIX_COLOR + " Please specify the clan you want to join.");
            return;
        }
        if(Clans.getInviteManager().accept(player, args[1])){
            player.sendMessage(Clans.PREFIX_COLOR + " You successfully joined: " + ChatColor.RESET + args[1]);
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 1);
        }else{
            player.sendMessage(Clans.PREFIX_COLOR + " You don't have an invite from this clan.");
        }
    }

    private void invitePlayer(CommandSender commandSender, String[] args){
        Bukkit.getScheduler().runTaskAsynchronously(Clans.getPlugin(), () -> {
            Player player = (Player) commandSender;
            Clan clan = Clans.getClan(player.getUniqueId());
            if(clan == null){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " You are not in a clan.");
                return;
            }
            if(args.length == 1){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " You have to specify the player that you want to invite. " + ChatColor.GRAY + "/clan invite <player>");
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if(targetPlayer == null){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " This player is either not online or does not exist.");
                return;
            }
            if(Clans.getClan(targetPlayer.getUniqueId()) != null){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " This player is already in a clan.");
                return;
            }
            if(!clan.getOwner().equals(player.getUniqueId())){
                player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " Only the clan owner can invite people.");
                return;
            }
            Clans.getInviteManager().sendInvite(new Invite() {
                @Override
                public Clan getClan() {
                    return clan;
                }

                @Override
                public Player getTargetPlayer() {
                    return targetPlayer;
                }
            });
            player.sendMessage(Clans.PREFIX_COLOR + " A request has been send.");
        });
    }

    private void createClan(CommandSender commandSender, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(Clans.getPlugin(), () -> {
            if (args.length < 3) {
                commandSender.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " You can create a clan with: " + ChatColor.GRAY + "/clan create <name> <tag>");
                return;
            }
            Player player = (Player) commandSender;
            String clanName = args[1];
            if (Clans.getDatabaseManager().isInAClan(player.getUniqueId())) {
                commandSender.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " You can only be in one clan at a time.");
                return;
            }
            if (Clans.getDatabaseManager().isClanNameTaken(clanName)) {
                commandSender.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " This clan name is already taken.");
                return;
            }
            Clans.getDatabaseManager().createClan(clanName, args[2], Clans.getConfigManager().getConfig().getBaseElo(), player.getUniqueId());
            player.sendMessage(Clans.PREFIX_COLOR + ChatColor.GREEN + " Your clan got created successfully.");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 1);
        });
    }

    private void printStats(CommandSender commandSender, String[] args) {
        Player player = (Player) commandSender;
        Bukkit.getScheduler().runTaskAsynchronously(Clans.getPlugin(), () -> {
            if (args.length == 1) {
                Clan clan = Clans.getClan(player.getUniqueId());
                if (clan == null) {
                    player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + "You didn't specify a clan and are not in a clan yourself. Try " + ChatColor.GRAY + "/clan stats <clanName>");
                } else {
                    this.printStats(player, clan);
                }
            } else {
                Clan clan = Clans.getClan(args[1]);
                if (clan == null) {
                    player.sendMessage(Clans.PREFIX_COLOR + ChatColor.YELLOW + " This clan does not exist.");
                } else {
                    this.printStats(player, clan);
                }
            }
        });
    }

    private void printStats(Player player, Clan clan) {
        player.sendMessage(Clans.PREFIX_COLOR + ChatColor.GOLD + " Stats for the clan: " + ChatColor.DARK_AQUA + clan.getClanName() + ChatColor.GRAY + " [" + ChatColor.DARK_AQUA + clan.getClanTag() + ChatColor.GRAY + "]");
        player.sendMessage(ChatColor.GOLD + " Owner: " + ChatColor.YELLOW + Bukkit.getOfflinePlayer(clan.getOwner()).getName());
        player.sendMessage(ChatColor.GOLD + " Members: " + ChatColor.YELLOW + clan.getMembers().size());
        player.sendMessage(ChatColor.GOLD + " Elo: " + ChatColor.YELLOW + clan.getElo());
        player.sendMessage(ChatColor.GOLD + " Wins: " + ChatColor.YELLOW + clan.getWins());
        player.sendMessage(ChatColor.GOLD + " Losses: " + ChatColor.YELLOW + clan.getLosses());
    }
}