package com.agrievolution.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Shared declarations, bound to the native registry by the loader at startup. */
public final class RegistrationSet<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final Map<ResourceLocation, Consumer<Binder<T>>> declarations = new LinkedHashMap<>();
    private final Map<ResourceLocation, ResourceLocation> aliases = new LinkedHashMap<>();
    private boolean bound;

    public RegistrationSet(ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
    }

    public <V extends T> Supplier<V> register(String name, Supplier<V> factory) {
        var id = ModInfo.id(name);
        if (bound) throw new IllegalStateException("Registry declarations already bound: " + registryKey.location());
        var entry = new Entry<V>(id);
        if (declarations.putIfAbsent(id, binder -> entry.value = binder.register(id, factory)) != null) {
            throw new IllegalArgumentException("Duplicate registration: " + id);
        }
        return entry;
    }

    public <V extends T> Supplier<V> registerItem(String name, Function<Item.Properties, V> factory,
                                                  Item.Properties properties) {
        return register(name, () -> factory.apply(properties));
    }

    public void addAlias(ResourceLocation oldId, ResourceLocation newId) {
        if (bound) throw new IllegalStateException("Registry declarations already bound: " + registryKey.location());
        aliases.put(oldId, newId);
    }

    public ResourceKey<? extends Registry<T>> registryKey() {
        return registryKey;
    }

    public Map<ResourceLocation, ResourceLocation> aliases() {
        return Map.copyOf(aliases);
    }

    public void bind(Binder<T> binder) {
        if (bound) throw new IllegalStateException("Registry already bound: " + registryKey.location());
        declarations.values().forEach(declare -> declare.accept(binder));
        declarations.clear();
        bound = true;
    }

    public interface Binder<T> {
        <V extends T> Supplier<V> register(ResourceLocation id, Supplier<V> factory);
    }

    private static final class Entry<T> implements Supplier<T> {
        private final ResourceLocation id;
        private Supplier<T> value;

        Entry(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public T get() {
            if (value == null) throw new IllegalStateException("Registry entry is not bound: " + id);
            return value.get();
        }
    }
}
