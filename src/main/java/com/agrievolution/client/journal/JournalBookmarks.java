package com.agrievolution.client.journal;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;

/** Personal client bookmarks, separate from server discoveries and item data. */
final class JournalBookmarks {
    private final Set<String> keys=new HashSet<>();
    private final Path file;
    private String error="";
    JournalBookmarks(boolean building){this(building?"machines":"crops");}
    JournalBookmarks(String section){
        var mc=Minecraft.getInstance();String user=mc.player==null?"preview":mc.player.getUUID().toString();
        file=mc.gameDirectory.toPath().resolve("config/agri-evolution/bookmarks-"+user+"-"+section+".txt");
        try{if(Files.exists(file))keys.addAll(Files.readAllLines(file));}catch(java.io.IOException ex){error="Bookmarks could not be read";}
    }
    boolean contains(String key){return keys.contains(key);}
    String error(){return error;}
    String first(){return keys.stream().findFirst().orElse("");}
    void toggle(String key){
        var next=new HashSet<>(keys);
        if(!next.remove(key))next.add(key);
        save(next);
    }
    void select(String key){save(keys.contains(key)?Set.of():Set.of(key));}
    private void save(Set<String> next){
        try{
            Files.createDirectories(file.getParent());var temp=file.resolveSibling(file.getFileName()+".tmp");
            Files.write(temp,new TreeSet<>(next));
            Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);
            keys.clear();
            keys.addAll(next);
            error="";
        }catch(java.io.IOException ex){error="Could not save journal selection.";}
    }
}
