package cloud.viniciusith.endrelay.block.entity;

import cloud.viniciusith.endrelay.EndRelayMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EndRelayBlockEntity extends BlockEntity {
    private BlockPos destination;

    public EndRelayBlockEntity(BlockPos pos, BlockState state) {
        super(EndRelayMod.END_RELAY_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        if (this.destination == null) {
            return;
        }

        nbt.put("destination", NbtHelper.fromBlockPos(this.destination));
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.destination = NbtHelper.toBlockPos(nbt.getCompound("destination"));

        super.readNbt(nbt);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public void setDestination(BlockPos destination) {
        this.destination = destination;
        markDirty();
    }

    public boolean hasDestination() {
        return this.destination != null;
    }

    public void teleport(ServerPlayerEntity player) {
        if (!destinationHasLodestone(destination, player.getWorld())) {
            player.sendMessage(Text.translatable("teleport.endrelay.no_lodestone"), false);
            return;
        }


        Optional<Vec3d> targetPos = RespawnAnchorBlock.findRespawnPosition(
                player.getType(),
                player.getWorld(),
                destination
        );

        if (targetPos.isEmpty()) {
            player.sendMessage(Text.translatable("teleport.endrelay.obstructed"), false);
            return;
        }

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

    private boolean destinationHasLodestone(BlockPos destination, World world) {
        if (destination == null) {
            return false;
        }

        return world.getBlockState(destination).getBlock() == Blocks.LODESTONE;
    }
}
