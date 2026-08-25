package ru.reset.rzero.checkpoint.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpenMenuSnapshot {
    public String menuTypeId;
    public int containerId;
    public CompoundTag titleTag;
    public Long anchorBlockPos;
    public UUID anchorEntityUuid;
    public ListTag slots = new ListTag();
    public int[] dataSlots = new int[0];
    public CompoundTag carriedTag;
    public ListTag craftMatrix = new ListTag();
    public int creativeTab = -1;

    public transient ItemStack[] cachedSlotStacks;
    public transient ItemStack[] cachedCraftStacks;
    public transient ItemStack cachedCarriedStack;

    public CompoundTag toNBT(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        if (menuTypeId != null) tag.putString("type", menuTypeId);
        tag.putInt("cid", containerId);
        if (titleTag != null) tag.put("title", titleTag);
        if (anchorBlockPos != null) tag.putLong("anchorBlock", anchorBlockPos);
        if (anchorEntityUuid != null) tag.putUUID("anchorEntity", anchorEntityUuid);
        tag.put("slots", slots);
        tag.putIntArray("data", dataSlots);
        if (carriedTag != null) tag.put("carried", carriedTag);
        tag.put("craft", craftMatrix);
        tag.putInt("ctab", creativeTab);
        return tag;
    }

    public static OpenMenuSnapshot fromNBT(CompoundTag tag) {
        OpenMenuSnapshot s = new OpenMenuSnapshot();
        s.cachedSlotStacks = null;
        s.cachedCraftStacks = null;
        s.cachedCarriedStack = null;
        if (tag.contains("type")) s.menuTypeId = tag.getString("type");
        s.containerId = tag.getInt("cid");
        if (tag.contains("title", Tag.TAG_COMPOUND)) s.titleTag = tag.getCompound("title");
        if (tag.contains("anchorBlock")) s.anchorBlockPos = tag.getLong("anchorBlock");
        if (tag.hasUUID("anchorEntity")) s.anchorEntityUuid = tag.getUUID("anchorEntity");
        if (tag.contains("slots", Tag.TAG_LIST)) s.slots = tag.getList("slots", Tag.TAG_COMPOUND);
        if (tag.contains("data")) s.dataSlots = tag.getIntArray("data");
        if (tag.contains("carried", Tag.TAG_COMPOUND)) s.carriedTag = tag.getCompound("carried");
        if (tag.contains("craft", Tag.TAG_LIST)) s.craftMatrix = tag.getList("craft", Tag.TAG_COMPOUND);
        s.creativeTab = tag.contains("ctab") ? tag.getInt("ctab") : -1;
        return s;
    }

    public static OpenMenuSnapshot capture(ServerPlayer player, net.minecraft.core.HolderLookup.Provider lookup) {
        AbstractContainerMenu menu = player.containerMenu;
        boolean isInventory = (menu == player.inventoryMenu);
        ItemStack carried = menu == null ? ItemStack.EMPTY : menu.getCarried();

        OpenMenuSnapshot s = new OpenMenuSnapshot();

        net.minecraft.world.inventory.InventoryMenu invMenu = player.inventoryMenu;
        try {
            CraftingContainer cc = invMenu.getCraftSlots();
            int sz = cc.getContainerSize();
            s.cachedCraftStacks = new ItemStack[sz];
            for (int i = 0; i < sz; i++) {
                ItemStack stack = cc.getItem(i);
                CompoundTag stackTag = new CompoundTag();
                if (!stack.isEmpty()) {
                    Tag enc = stack.save(lookup);
                    if (enc instanceof CompoundTag c) stackTag = c;
                }
                s.craftMatrix.add(stackTag);
                s.cachedCraftStacks[i] = stack.copy();
            }
        } catch (Throwable ignored) {}

        if (!carried.isEmpty()) {
            Tag enc = carried.save(lookup);
            if (enc instanceof CompoundTag c) s.carriedTag = c;
            s.cachedCarriedStack = carried.copy();
        } else {
            s.cachedCarriedStack = ItemStack.EMPTY;
        }

        if (isInventory && carried.isEmpty()) {
            return s;
        }

        if (!isInventory && menu != null) {
            ResourceLocation typeKey = BuiltInRegistries.MENU.getKey(menu.getType());
            s.menuTypeId = typeKey == null ? null : typeKey.toString();
            s.containerId = menu.containerId;

            int slotCount = menu.slots.size();
            s.cachedSlotStacks = new ItemStack[slotCount];
            int idx = 0;
            for (Slot slot : menu.slots) {
                ItemStack stack = slot.getItem();
                CompoundTag slotTag = new CompoundTag();
                if (!stack.isEmpty()) {
                    Tag enc = stack.save(lookup);
                    if (enc instanceof CompoundTag c) slotTag = c;
                }
                s.slots.add(slotTag);
                s.cachedSlotStacks[idx++] = stack.copy();

                if (s.anchorBlockPos == null && s.anchorEntityUuid == null) {
                    Container container = slot.container;
                    if (container instanceof BlockEntity be) {
                        s.anchorBlockPos = be.getBlockPos().asLong();
                    }
                }
            }

            if (s.anchorBlockPos == null && s.anchorEntityUuid == null) {
                if (player.level() instanceof ServerLevel lvl) {
                    net.minecraft.world.phys.AABB box =
                            player.getBoundingBox().inflate(8.0);
                    for (AbstractVillager v : lvl.getEntitiesOfClass(AbstractVillager.class, box)) {
                        if (v.getTradingPlayer() == player) {
                            s.anchorEntityUuid = v.getUUID();
                            break;
                        }
                    }
                    if (s.anchorEntityUuid == null) {
                        for (AbstractHorse h : lvl.getEntitiesOfClass(AbstractHorse.class, box)) {
                            if (h.distanceToSqr(player) < 16.0) {
                                s.anchorEntityUuid = h.getUUID();
                                break;
                            }
                        }
                    }
                }
            }

            try {
                Component title = null;
                if (title != null) {
                    Tag t = net.minecraft.network.chat.ComponentSerialization.CODEC
                            .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, title)
                            .result().orElse(null);
                    if (t instanceof CompoundTag c) s.titleTag = c;
                }
            } catch (Throwable ignored) {}

            try {
                List<DataSlot> ds =
                        ((ru.reset.rzero.mixin.world.MixinAbstractContainerMenu)(Object) menu).rzero$getDataSlots();
                int[] arr = new int[ds.size()];
                for (int i = 0; i < ds.size(); i++) arr[i] = ds.get(i).get();
                s.dataSlots = arr;
            } catch (Throwable ignored) {}
        }

        return s;
    }

    public void restore(ServerPlayer player, ServerLevel level, net.minecraft.core.HolderLookup.Provider lookup) {
        try {
            CraftingContainer cc = player.inventoryMenu.getCraftSlots();
            int n = Math.min(cc.getContainerSize(),
                    cachedCraftStacks != null ? cachedCraftStacks.length : craftMatrix.size());
            for (int i = 0; i < n; i++) {
                ItemStack stack;
                if (cachedCraftStacks != null) {
                    ItemStack cached = cachedCraftStacks[i];
                    stack = cached == null ? ItemStack.EMPTY : cached.copy();
                } else {
                    CompoundTag t = craftMatrix.getCompound(i);
                    stack = t.isEmpty() ? ItemStack.EMPTY
                            : ItemStack.parse(lookup, t).orElse(ItemStack.EMPTY);
                }
                cc.setItem(i, stack);
            }
        } catch (Throwable ignored) {}

        ItemStack carried;
        if (cachedCarriedStack != null) {
            carried = cachedCarriedStack.isEmpty() ? ItemStack.EMPTY : cachedCarriedStack.copy();
        } else if (carriedTag != null && !carriedTag.isEmpty()) {
            carried = ItemStack.parse(lookup, carriedTag).orElse(ItemStack.EMPTY);
        } else {
            carried = ItemStack.EMPTY;
        }
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerClosePacket(player.containerMenu.containerId));
        player.containerMenu.setCarried(carried);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(-1, 0, 0, carried));

        if (menuTypeId == null) {
            return;
        }

        MenuProvider provider = null;

        if (anchorBlockPos != null) {
            BlockPos pos = BlockPos.of(anchorBlockPos);
            try {
                provider = level.getBlockState(pos).getMenuProvider(level, pos);
            } catch (Throwable ignored) {}
        }

        if (provider == null && anchorEntityUuid != null) {
            Entity ent = level.getEntity(anchorEntityUuid);
            if (ent instanceof AbstractVillager v) {
                v.setTradingPlayer(player);
                int tradeLevel = (v instanceof Villager vill)
                        ? vill.getVillagerData().getLevel()
                        : 1;
                v.openTradingScreen(player, v.getDisplayName(), tradeLevel);
                applySlotAndDataResync(player);
                return;
            } else if (ent instanceof AbstractHorse h) {
                h.openCustomInventoryScreen(player);
                applySlotAndDataResync(player);
                return;
            }
        }

        if (provider == null) {
            try {
                ResourceLocation rid = ResourceLocation.parse(menuTypeId);
                MenuType<?> type = BuiltInRegistries.MENU.get(rid);
                if (type != null) {
                    final MenuType<?> finalType = type;
                    provider = new SimpleMenuProvider(
                            (id, inv, p) -> finalType.create(id, inv),
                            Component.empty());
                }
            } catch (Throwable ignored) {}
        }

        if (provider != null) {
            player.openMenu(provider);
            applySlotAndDataResync(player);
        }
    }

    private void applySlotAndDataResync(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) return;
        net.minecraft.core.NonNullList<ItemStack> stacks =
                net.minecraft.core.NonNullList.withSize(menu.slots.size(), ItemStack.EMPTY);
        int n = Math.min(stacks.size(),
                cachedSlotStacks != null ? cachedSlotStacks.length : slots.size());
        for (int i = 0; i < n; i++) {
            ItemStack stack;
            if (cachedSlotStacks != null) {
                ItemStack cached = cachedSlotStacks[i];
                stack = cached == null ? ItemStack.EMPTY : cached.copy();
            } else {
                CompoundTag t = slots.getCompound(i);
                stack = t.isEmpty() ? ItemStack.EMPTY
                        : ItemStack.parse(player.registryAccess(), t).orElse(ItemStack.EMPTY);
            }
            stacks.set(i, stack);
        }
        ItemStack cursor = menu.getCarried();
        player.connection.send(new ClientboundContainerSetContentPacket(
                menu.containerId, menu.incrementStateId(), stacks, cursor));

        for (int i = 0; i < dataSlots.length; i++) {
            try {
                List<DataSlot> ds =
                        ((ru.reset.rzero.mixin.world.MixinAbstractContainerMenu)(Object) menu).rzero$getDataSlots();
                if (i < ds.size()) {
                    ds.get(i).set(dataSlots[i]);
                }
            } catch (Throwable ignored) {}
            player.connection.send(new ClientboundContainerSetDataPacket(
                    menu.containerId, i, dataSlots[i]));
        }
    }
}
