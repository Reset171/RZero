package ru.reset.rzero.mixin.client;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import ru.reset.rzero.access.IRZeroChat;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent implements IRZeroChat {

    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    
    @Unique private int rzero$markedAllSize = -1;
    @Unique private int rzero$markedTrimmedSize = -1;

    @Override
    public void rzero$markChat() {
        this.rzero$markedAllSize = this.allMessages.size();
        this.rzero$markedTrimmedSize = this.trimmedMessages.size();
    }

    @Override
    public void rzero$rollbackChat() {
        if (this.rzero$markedAllSize >= 0 && this.rzero$markedAllSize <= this.allMessages.size()) {
            while (this.allMessages.size() > this.rzero$markedAllSize) {
                this.allMessages.removeFirst();
            }
        }
        
        if (this.rzero$markedTrimmedSize >= 0 && this.rzero$markedTrimmedSize <= this.trimmedMessages.size()) {
            while (this.trimmedMessages.size() > this.rzero$markedTrimmedSize) {
                this.trimmedMessages.removeFirst();
            }
        }
    }
}