package com.github.Gamecube762.BankNote;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gamecube762 on 11/10/2014.
 */
public class Main extends JavaPlugin implements Listener{

    public static Economy econ;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Plugin \"Vault\" was not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy()) {
            getLogger().severe("No Economy plugin found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Needs to be a Player!");
            return true;
        }
        if (args.length == 0) return false;

        Double a, b;

        try {a = Double.parseDouble(args[0]);}
        catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Not a Number!");
            return false;
        }

        ItemStack bankNote = createNote((Player)sender, a);

        b = econ.getBalance((Player)sender);
        if ((b - a) < 0.0) {
            sender.sendMessage(ChatColor.RED + "You don't have enough money!");
            return true;
        }

        EconomyResponse eR = econ.withdrawPlayer((Player)sender, a);

        if (eR.transactionSuccess()) {
            sender.sendMessage(ChatColor.RED + "-" + a);
            sender.sendMessage(ChatColor.GREEN + "A BankNote has been added to your Inventory!");
            ((Player)sender).getInventory().addItem(bankNote);
        } else {
            sender.sendMessage(ChatColor.RED + "An error as occurred! Your account was not withdrawn and you did not receive a BankNote!");
            sender.sendMessage(ChatColor.RED + "Error: " + eR.errorMessage);
        }

        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)) return;
        if (e.getItem() == null) return;
        if (!isValidBankNote(e.getItem())) return;

        Double a = Double.parseDouble(e.getItem().getItemMeta().getLore().get(0).substring(11));
        String signer = e.getItem().getItemMeta().getLore().get(1).substring(12);

        EconomyResponse eR = econ.depositPlayer(e.getPlayer(), a);

        if (eR.transactionSuccess()) {
            e.getPlayer().sendMessage(ChatColor.GREEN + "+" + a);
            e.getPlayer().sendMessage(ChatColor.GREEN + "BankNote from " + signer + " was removed!");

            if (e.getItem().getAmount() <= 1)
                e.getPlayer().getInventory().remove(e.getItem());
            else
                e.getPlayer().getInventory().getItemInHand().setAmount(e.getItem().getAmount() -1);
        } else {
            e.getPlayer().sendMessage(ChatColor.RED + "An error as occurred! No money was deposited and the BankNote remains!");
            e.getPlayer().sendMessage(ChatColor.RED + "Error: " + eR.errorMessage);
        }

    }

    public static boolean isValidBankNote(ItemStack i) {
        if (i.getType() != Material.PAPER) return false;
        ItemMeta iM = i.getItemMeta();

        return iM.getDisplayName().equals(ChatColor.YELLOW + "" + ChatColor.BOLD + "Bank Note" + ChatColor.GRAY + " (Right Click)") &&
               iM.getLore().get(0).startsWith(ChatColor.YELLOW + "Value: " + ChatColor.WHITE) &&
               iM.getLore().get(1).startsWith(ChatColor.YELLOW + "Signer: " + ChatColor.GRAY)
        ;
    }

    public static ItemStack createNote(Player player, Double amount) {
        ItemStack i = new ItemStack(Material.PAPER);
        i.setAmount(1);

        ItemMeta iM = i.getItemMeta();
        iM.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Bank Note" + ChatColor.GRAY + " (Right Click)");

        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.YELLOW + "Value: " + ChatColor.WHITE + amount);
        lore.add(ChatColor.YELLOW + "Signer: " + ChatColor.GRAY + player.getName());

        iM.setLore(lore);
        i.setItemMeta(iM);

        return i;
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        econ = rsp.getProvider();
        return econ != null;
    }

}