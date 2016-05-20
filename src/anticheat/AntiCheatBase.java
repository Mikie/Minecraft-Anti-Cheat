package anticheat;

import anticheat.detections.*;
import anticheat.detections.combat.*;
import anticheat.detections.combat.killaura.AttackThroughWalls;
import anticheat.detections.combat.killaura.InventoryKillAuraDetection;
import anticheat.detections.combat.killaura.KillAura;
import anticheat.detections.movement.BlocksPerSecondLogger;
import anticheat.detections.movement.ConstantMovement;
import anticheat.detections.movement.FlyFix;
import anticheat.detections.movement.WaterWalkDetection;
import anticheat.events.PlayerBanEvent;
import anticheat.events.PlayerLeaveEvent;
import anticheat.util.*;
import anticheat.util.Timer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.*;

public class AntiCheatBase implements Listener, PluginMessageListener {
    private static boolean enabled = true;
    private List<String> banned = new ArrayList<String>();
    private String name = null;
    private int maxPing = 135;

    public AntiCheatBase() {
        new BlocksPerSecondLogger();
        new InvisibleFireGlitchFix();
        new FastBowFix();
        new AutoCritFix();
        new AttackThroughWalls();
        new AttackDistanceLogger();
        //new SpeedFix();
        new FlyFix();
        new InventoryKillAuraDetection();
        new SpamBotFix();
        new WaterWalkDetection();
        new AutoClicker();
        new ClickPatternDetector();
        new KillAura();
        new AutoArmorFix();
        new AutoEatFix();
        //new AutoSprintFix(); // TODO: Fix
        new AutoStealFix();
        new FastEatFix();
        new ConstantMovement();
        EventUtil.register(this);
        registerPluginChannels(AntiCheat.getInstance());
    }

    public AntiCheatBase(String name) {
        this.name = name;
    }

    public static boolean isEnabled() {
        return enabled && Timer.getTicksPerSecond() >= 18.75 && Timer.getMemory() <= 85;
    }

    public static void setEnabled(boolean enabled) {
        AntiCheatBase.enabled = enabled;
    }

    protected int getMaxPing() {
        return maxPing;
    }

    public boolean notIgnored(Player player) {
        int ping = Timer.getPing(player);
        return ping > 0 && ping <= maxPing && player.getGameMode() == GameMode.SURVIVAL && Timer.getTicksPerSecond() >= 19;
    }

    public void ban(Player player) {
        ban(player, false);
    }

    public void ban(Player player, boolean queue) {
        if (notIgnored(player) && !banned.contains(player.getName())) {
            banned.add(player.getName());
            if (queue) {
                final UUID uuid = player.getUniqueId();
                new AsyncDelayedTask(new Runnable() {
                    @Override
                    public void run() {
                        DB.NETWORK_ANTI_CHEAT_BAN_QUEUE.insert("'" + uuid.toString() + ", '" + name + "''");
                    }
                });
            } else {
                Bukkit.getPluginManager().callEvent(new PlayerBanEvent(player.getUniqueId(), player.getName(), name, queue));
                new AsyncDelayedTask(new Runnable() {
                    @Override
                    public void run() {
                        if (DB.NETWORK_ANTI_CHEAT_DATA.isKeySet("cheat", name)) {
                            int amount = DB.NETWORK_ANTI_CHEAT_DATA.getInt("cheat", name, "bans") + 1;
                            DB.NETWORK_ANTI_CHEAT_DATA.updateInt("bans", amount, "cheat", name);
                        } else {
                            DB.NETWORK_ANTI_CHEAT_DATA.insert("'" + name + "', '1'");
                        }
                    }
                });
            }
        }
    }

    public void logAnticheatMessage(String msg) {
        Bukkit.getConsoleSender().sendMessage(msg);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOp() || online.hasPermission("outsidetheblock.anticheat.alerts")) {
                MessageUtil.messagePrefix(online, MessageUtil.MessageType.BAD, msg);
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerLeaveEvent event) {
        banned.remove(event.getPlayer().getName());
    }

    public void registerPluginChannels(AntiCheat instance) {
        Bukkit.getMessenger().registerIncomingPluginChannel(instance, "MC|Brand", this);
        Bukkit.getMessenger().registerIncomingPluginChannel(instance, "PERMISSIONSREPL", this);
        Bukkit.getMessenger().registerIncomingPluginChannel(instance, "BSprint", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(instance, "BSM");
        Bukkit.getMessenger().registerIncomingPluginChannel(instance, "WDL|INIT", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(instance, "WDL|CONTROL");
        Bukkit.getMessenger().registerIncomingPluginChannel(instance, "LABYMOD", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(instance, "LABYMOD");
        Bukkit.getMessenger().registerIncomingPluginChannel(instance, "5zig_Set", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(instance, "5zig_Set");
        Bukkit.getMessenger().registerIncomingPluginChannel(instance, "DIPermissions", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(instance, "DIPermissions");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        switch (channel) {
            case "BSprint":
                if (message.length > 0 && message[0] == 5) {
                    logAnticheatMessage("&e" + player.getName() + " &clogged in using &eBetter Sprint &cmod!");
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeByte(1);
                        player.sendPluginMessage(AntiCheat.getInstance(), "BSM", baos.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "LABYMOD":
                try {
                    ByteArrayInputStream is = new ByteArrayInputStream(message);
                    DataInputStream dis = new DataInputStream(is);
                    int length = dis.readByte();
                    byte[] chars = new byte[length];
                    for (int i = 0; i < chars.length; i++) {
                        chars[i] = dis.readByte();
                    }
                    String labyModString = new String(chars);
                    logAnticheatMessage("&e" + player.getName() + " &clogged in using &e" + labyModString + " &cmod!");

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    Map<String, Boolean> disableSettings = new HashMap<>();
                    disableSettings.put("food", false);
                    disableSettings.put("nick", false);
                    disableSettings.put("blockbuild", false);
                    oos.writeObject(disableSettings);
                    player.sendPluginMessage(AntiCheat.getInstance(), "LABYMOD", baos.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "WDL|INIT":
                try {
                    ByteArrayInputStream is = new ByteArrayInputStream(message);
                    DataInputStream dis = new DataInputStream(is);
                    int length = dis.readByte();
                    byte[] chars = new byte[length];
                    for (int i = 0; i < chars.length; i++) {
                        chars[i] = dis.readByte();
                    }
                    String version = new String(chars);
                    logAnticheatMessage("&e" + player.getName() + " &clogged in using &eWorld Downloader " + version + " &cmod!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "WDL|CONTROL":
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeInt(1);
                    dos.writeBoolean(false);
                    player.sendPluginMessage(AntiCheat.getInstance(), "WDL|CONTROL", baos.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "5zig_Set":
                if (message.length > 0 && message[0] >= 2) {
                    logAnticheatMessage("&e" + player.getName() + " &clogged in using &e5zig &cmod!");
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeByte(0x08 | 0x10);
                        player.sendPluginMessage(AntiCheat.getInstance(), "5zig_Set", baos.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "DIPermissions":
                if (message.length > 0) {
                    logAnticheatMessage("&e" + player.getName() + " &clogged in using &eDamage Indicators &cmod!");
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeByte(0x1);
                        player.sendPluginMessage(AntiCheat.getInstance(), "DIPermissions", baos.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
