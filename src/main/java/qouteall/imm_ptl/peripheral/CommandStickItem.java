package qouteall.imm_ptl.peripheral;

import com.mojang.serialization.Lifecycle;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.peripheral.platform_specific.PeripheralModEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandStickItem extends Item {
    
    private static final ResourceKey<Registry<Data>> registryRegistryKey =
        ResourceKey.createRegistryKey(new ResourceLocation("immersive_portals:command_stick_type"));
    
    public static class Data {
        public final String command;
        public final String nameTranslationKey;
        public final List<String> descriptionTranslationKeys;
        
        public Data(
            String command, String nameTranslationKey, List<String> descriptionTranslationKeys
        ) {
            this.command = command;
            this.nameTranslationKey = nameTranslationKey;
            this.descriptionTranslationKeys = descriptionTranslationKeys;
        }
        
        public void serialize(CompoundTag tag) {
            tag.putString("command", command);
            tag.putString("nameTranslationKey", nameTranslationKey);
            ListTag listTag = new ListTag();
            for (String descriptionTK : descriptionTranslationKeys) {
                listTag.add(StringTag.valueOf(descriptionTK));
            }
            tag.put("descriptionTranslationKeys", listTag);
        }
        
        public static Data deserialize(CompoundTag tag) {
            return new Data(
                tag.getString("command"),
                tag.getString("nameTranslationKey"),
                tag.getList(
                        "descriptionTranslationKeys",
                        StringTag.valueOf("").getId()
                    )
                    .stream()
                    .map(tag1 -> ((StringTag) tag1).getAsString())
                    .collect(Collectors.toList())
            );
        }
    }
    
    public static final MappedRegistry<Data> commandStickTypeRegistry = new MappedRegistry<>(
        registryRegistryKey, Lifecycle.stable(), null
    );
    
    public static void registerType(String id, Data data) {
        commandStickTypeRegistry.register(
            ResourceKey.create(
                registryRegistryKey, new ResourceLocation(id)
            ),
            data,
            Lifecycle.stable()
        );
    }
    
    public CommandStickItem(Properties settings) {
        super(settings);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        doUse(player, player.getItemInHand(hand));
        return super.use(world, player, hand);
    }
    
    private void doUse(Player player, ItemStack stack) {
        if (player.level.isClientSide()) {
            return;
        }
        
        if (canUseCommand(player)) {
            Data data = Data.deserialize(stack.getOrCreateTag());
            
            CommandSourceStack commandSource = player.createCommandSourceStack().withPermission(2);
            
            Commands commandManager =  commandSource.getServer().getCommands();
            
            commandManager.performCommand(commandSource, data.command);
        }
        else {
            sendMessage(player, new TextComponent("No Permission"));
        }
    }
    
    private static boolean canUseCommand(Player player) {
        if (IPGlobal.easeCommandStickPermission) {
            return true;// any player regardless of gamemode can use
        }
        else {
            return player.hasPermissions(2) || player.isCreative();
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        
        Data data = Data.deserialize(stack.getOrCreateTag());
        
        tooltip.add(new TextComponent(data.command).withStyle(ChatFormatting.GOLD));
        
        for (String descriptionTranslationKey : data.descriptionTranslationKeys) {
            tooltip.add(new TranslatableComponent(descriptionTranslationKey).withStyle(ChatFormatting.AQUA));
        }
        
        tooltip.add(new TranslatableComponent("imm_ptl.command_stick").withStyle(ChatFormatting.GRAY));
    }
    
    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (allowdedIn(group)) {
            commandStickTypeRegistry.stream().forEach(data -> {
                ItemStack stack = new ItemStack(PeripheralModEntry.COMMAND_STICK_ITEM.get());
                data.serialize(stack.getOrCreateTag());
                stacks.add(stack);
            });
        }
    }
    
    @Override
    public String getDescriptionId(ItemStack stack) {
        Data data = Data.deserialize(stack.getOrCreateTag());
        return data.nameTranslationKey;
    }
    
    public static void sendMessage(Player player, Component message) {
        ((ServerPlayer) player).sendMessage(message, ChatType.GAME_INFO, Util.NIL_UUID);
    }
    
    public static void init() {
        PortalCommand.createCommandStickCommandSignal.connect((player, command) -> {
            ItemStack itemStack = new ItemStack(PeripheralModEntry.COMMAND_STICK_ITEM.get(), 1);
            Data data = new Data(
                command,
                command, new ArrayList<>()
            );
            data.serialize(itemStack.getOrCreateTag());
            
            player.getInventory().add(itemStack);
            player.inventoryMenu.broadcastChanges();
        });
    }
}
