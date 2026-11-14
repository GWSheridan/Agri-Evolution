package com.agrievolution.client.journal;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;

/** Personal client bookmarks, separate from server discoveries and item data. */
final class JournalBookmarks {
    private final Set<String> keys=new HashSet<>();
    private final Path file;
    private String error="";
    JournalBookmarks(boolean building){
        var mc=Minecraft.getInstance();String user=mc.player==null?"preview":mc.player.getUUID().toString();
        file=mc.gameDirectory.toPath().resolve("config/agri-evolution/bookmarks-"+user+(building?"-machines":"-crops")+".txt");
        try{if(Files.exists(file))keys.addAll(Files.readAllLines(file));}catch(java.io.IOException ex){error="Bookmarks could not be read";}
    }
    boolean contains(String key){return keys.contains(key);}
    String error(){return error;}
    void toggle(String key){
        boolean removed=keys.remove(key);if(!removed)keys.add(key);
        try{
            Files.createDirectories(file.getParent());var temp=file.resolveSibling(file.getFileName()+".tmp");
            Files.write(temp,new TreeSet<>(keys));
            Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);error="";
        }catch(java.io.IOException ex){if(removed)keys.add(key);else keys.remove(key);error="Bookmark save failed";}
    }
}
