package cloud.viniciusith.endrelay.block.entity;

import cloud.viniciusith.endrelay.EndRelayMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static cloud.viniciusith.endrelay.EndRelayMod.END_RELAY_BLOCK;

public class EndRelayBlockEntity extends BlockEntity {
    private BlockPos relayDestination;

    public EndRelayBlockEntity(BlockPos pos, BlockState state) {
        super(EndRelayMod.END_RELAY_BLOCK_ENTITY, pos, state);
    }

    // NBT Serialization
    @Override
    protected void writeNbt(NbtCompound nbt) {
        if (this.relayDestination == null) {
            return;
        }
        nbt.put("destination", NbtHelper.fromBlockPos(this.relayDestination));
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.relayDestination = NbtHelper.toBlockPos(nbt.getCompound("destination"));
        super.readNbt(nbt);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    // Set and Check Destination
    public void setRelayDestination(BlockPos destination) {
        this.relayDestination = destination;
        markDirty();
    }

    public boolean hasRelayDestination() {
        return this.relayDestination != null;
    }

    // Teleportation
    public void teleport(ServerPlayerEntity player) {
        if (!destinationHasLodestone(relayDestination, player.getWorld())) {
            player.sendMessage(Text.translatable("teleport.endrelay.no_lodestone"), false);
            return;
        }

        Optional<Vec3d> targetPos = RespawnAnchorBlock.findRespawnPosition(
                player.getType(),
                player.getWorld(),
                relayDestination
        );

        if (targetPos.isEmpty()) {
            player.sendMessage(Text.translatable("teleport.endrelay.obstructed"), false);
            return;
        }

        World world = player.getEntityWorld();
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
        );
        player.teleport(targetPos.get().getX(), targetPos.get().getY(), targetPos.get().getZ());
    }

    // Helper Method
    private boolean destinationHasLodestone(BlockPos destination, World world) {
        if (destination == null) {
            return false;
        }
        return world.getBlockState(destination).getBlock() == Blocks.LODESTONE;
    }

    // Packet
    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public static BlockEntityType<EndRelayBlockEntity> getBlockEntityType() {
        return FabricBlockEntityTypeBuilder.create(
                EndRelayBlockEntity::new,
                END_RELAY_BLOCK
        ).build();
    }
}
