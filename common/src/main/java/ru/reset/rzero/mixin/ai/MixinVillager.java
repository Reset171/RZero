package ru.reset.rzero.mixin.ai;

import com.mojang.serialization.Dynamic;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.access.IRZeroVillagerBrainMarker;
import ru.reset.rzero.engine.BrainConstructionGate;

@Mixin(Villager.class)
public abstract class MixinVillager implements IRZeroVillagerBrainMarker {

    @Shadow
    protected abstract Brain.Provider<Villager> brainProvider();

    @Unique
    private boolean rzero$brainStripped;

    @Override
    public boolean rzero$isBrainStripped() {
        return this.rzero$brainStripped;
    }

    @Override
    public void rzero$setBrainStripped(boolean stripped) {
        this.rzero$brainStripped = stripped;
    }

    @Inject(method = "makeBrain", at = @At("HEAD"), cancellable = true)
    private void rzero$skipBrainGoalsDuringRestore(Dynamic<?> dynamic, CallbackInfoReturnable<Brain<?>> cir) {
        Villager self = (Villager) (Object) this;
        boolean clientSide = self.level().isClientSide();
        if (BrainConstructionGate.loadingEntity || clientSide) {
            if (!clientSide) {
                this.rzero$brainStripped = true;
            }
            cir.setReturnValue(this.brainProvider().makeBrain(dynamic));
        }
    }

    @Inject(method = "refreshBrain", at = @At("HEAD"), cancellable = true)
    private void rzero$skipRefreshBrainDuringRestore(net.minecraft.server.level.ServerLevel level, CallbackInfo ci) {
        if (BrainConstructionGate.loadingEntity) {
            this.rzero$brainStripped = true;
            ci.cancel();
        }
    }
}
