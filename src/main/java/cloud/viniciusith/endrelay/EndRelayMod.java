package cloud.viniciusith.endrelay;

import cloud.viniciusith.endrelay.block.EndRelayBlock;
import cloud.viniciusith.endrelay.block.entity.EndRelayBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.logging.Logger;


public class EndRelayMod implements ModInitializer {
    public static final String MOD_ID = "endrelay";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    public static final EndRelayBlock END_RELAY_BLOCK = new EndRelayBlock(EndRelayBlock.getBlockProperties());
    public static final BlockEntityType<EndRelayBlockEntity> END_RELAY_BLOCK_ENTITY =
            EndRelayBlockEntity.getBlockEntityType();
    public static final BlockItem ENDER_RELAY_ITEM = new BlockItem(END_RELAY_BLOCK, new Item.Settings());

    @Override
    public void onInitialize() {
        Identifier endRelayId = new Identifier(MOD_ID, "end_relay");

        Registry.register(Registries.BLOCK, endRelayId, END_RELAY_BLOCK);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, endRelayId, END_RELAY_BLOCK_ENTITY);
        Registry.register(Registries.ITEM, endRelayId, ENDER_RELAY_ITEM);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(content -> content.addAfter(Items.RESPAWN_ANCHOR, ENDER_RELAY_ITEM));
    }
}
