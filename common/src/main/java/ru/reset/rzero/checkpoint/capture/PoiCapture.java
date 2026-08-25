package ru.reset.rzero.checkpoint.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import ru.reset.rzero.RZero;
import ru.reset.rzero.mixin.ai.MixinPoiRecordAccessor;

import java.util.Comparator;
import java.util.List;

public final class PoiCapture {

    private PoiCapture() {
    }

    public static ListTag capture(ServerLevel level, ChunkPos cPos) {
        try {
            ListTag poiList = new ListTag();
            level.getPoiManager()
                    .getInChunk(p -> true, cPos, PoiManager.Occupancy.ANY)
                    .sorted(Comparator.comparingLong(r -> r.getPos().asLong()))
                    .forEach(rec -> rec.getPoiType().unwrapKey().ifPresent(typeKey -> {
                        CompoundTag p = new CompoundTag();
                        p.putLong("pos", rec.getPos().asLong());
                        p.putString("type", typeKey.location().toString());
                        p.putInt("freeTickets",
                                ((MixinPoiRecordAccessor) rec).rzero$getFreeTickets());
                        poiList.add(p);
                    }));
            return poiList.isEmpty() ? null : poiList;
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] POI capture for chunk {} failed: {}", cPos.toLong(), e.getMessage());
            return null;
        }
    }

    public static void restore(ServerLevel level, ChunkPos cPos, ListTag savedPois) {
        try {
            PoiManager pm = level.getPoiManager();
            List<BlockPos> toRemove = pm
                    .getInChunk(p -> true, cPos, PoiManager.Occupancy.ANY)
                    .map(PoiRecord::getPos)
                    .toList();
            for (BlockPos p : toRemove) {
                pm.remove(p);
            }

            Registry<PoiType> poiReg = level.registryAccess().registryOrThrow(Registries.POINT_OF_INTEREST_TYPE);
            for (int i = 0; i < savedPois.size(); i++) {
                CompoundTag t = savedPois.getCompound(i);
                BlockPos p = BlockPos.of(t.getLong("pos"));
                ResourceLocation typeId = ResourceLocation.parse(t.getString("type"));
                poiReg.getHolder(ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, typeId))
                        .ifPresent(holder -> {
                            pm.add(p, holder);
                            if (!t.contains("freeTickets")) {
                                return;
                            }
                            pm.getInChunk(pt -> true, cPos, PoiManager.Occupancy.ANY)
                                    .filter(rec -> rec.getPos().equals(p))
                                    .findFirst()
                                    .ifPresent(rec -> ((MixinPoiRecordAccessor) rec)
                                            .rzero$setFreeTickets(t.getInt("freeTickets")));
                        });
            }
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] POI restore for chunk {} failed: {}",
                    cPos.toLong(), e.getMessage());
        }
    }
}
