package com.invasion.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Dienstklasse zum einfachen Senden von Nachrichten an Spieler im Chat.
 */
public class ChatUtils {

    // Globale Referenz auf den Server
    private static MinecraftServer SERVER;

    /**
     * Vom Mod-Hauptcode aus aufrufen, wenn der Server startet.
     */
    public static void setServer(MinecraftServer server) {
        SERVER = server;
    }

    /**
     * Vom Mod-Hauptcode aus aufrufen, wenn der Server stoppt.
     */
    public static void clearServer() {
        SERVER = null;
    }

    /**
     * Sendet eine Nachricht an alle Spieler auf dem gesamten Server (global),
     * ohne dass eine Welt oder ein Nexus benötigt wird.
     */
    public static void broadcastGlobal(String message, Formatting color) {
        if (SERVER == null) return;

        SERVER.getPlayerManager().broadcast(
                Text.literal(message).formatted(color),
                false
        );
    }

    /**
     * Überladene Variante mit Standardfarbe (weiß).
     */
    public static void broadcastGlobal(String message) {
        broadcastGlobal(message, Formatting.WHITE);
    }

    /**
     * Sendet eine Nachricht an alle Spieler in der Welt der angegebenen Welt.
     * (Nutzen, wenn du eh ein ServerWorld-Objekt hast.)
     */
    public static void broadcast(ServerWorld world, String message, Formatting color) {
        if (world == null || world.getServer() == null) return;
        world.getServer().getPlayerManager().broadcast(
                Text.literal(message).formatted(color),
                false
        );
    }

    public static void broadcast(ServerWorld world, String message) {
        broadcast(world, message, Formatting.WHITE);
    }
}
