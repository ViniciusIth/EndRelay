package cloud.viniciusith.endrelay.block;

import cloud.viniciusith.endrelay.block.entity.EndRelayBlockEntity;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.Instrument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;


public class EndRelayBlock extends Block implements BlockEntityProvider {
    public static final BooleanProperty CHARGED = BooleanProperty.of("charged");

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    public EndRelayBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(CHARGED, false));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EndRelayBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        EndRelayBlockEntity blockEntity = (EndRelayBlockEntity) world.getBlockEntity(pos);
        if (blockEntity == null) {
            return ActionResult.PASS;
        }

        ItemStack heldItem = player.getStackInHand(hand);

        if (heldItem.getItem() instanceof CompassItem) {
            if (world.isClient) {
                return ActionResult.success(true);
            }

            if (!heldItem.getOrCreateNbt().contains(CompassItem.LODESTONE_POS_KEY)) {
                player.sendMessage(Text.translatable("block.endrelay.broken_compass"), false);
                return ActionResult.FAIL;
            }

            BlockPos teleportDestinationPos = NbtHelper.toBlockPos(
                    heldItem.getOrCreateNbt().getCompound(CompassItem.LODESTONE_POS_KEY)
            );

            setTarget(blockEntity, teleportDestinationPos);

            return ActionResult.SUCCESS;
        }

        if (isChargeItem(heldItem) && canCharge(state)) {
            charge(player, world, pos, state);
            if (!player.getAbilities().creativeMode) {
                heldItem.decrement(1);
            }

            return ActionResult.success(world.isClient);
        }

        if (state.get(CHARGED) && blockEntity.hasDestination()) {
            if (!world.isClient) {
                blockEntity.teleport((ServerPlayerEntity) player);
                uncharge(world, pos, state);
            }
            return ActionResult.SUCCESS;
        }

        if (!isAllowedDim(world)) {
            if (!world.isClient) {
                this.explode(world, pos);
            }

            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }

    public static void charge(@Nullable Entity charger, World world, BlockPos pos, BlockState state) {
        BlockState blockState = state.with(CHARGED, true);
        world.setBlockState(pos, blockState, 3);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(charger, blockState));
        world.playSound(
                null,
                (double) pos.getX() + 0.5,
                (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5,
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                SoundCategory.BLOCKS,
                1.0F,
                1.0F
        );
    }

    public static void uncharge(World world, BlockPos pos, BlockState state) {
        BlockState blockState = state.with(CHARGED, false);
        world.setBlockState(pos, blockState, 3);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(null, blockState));
        world.playSound(
                null,
                (double) pos.getX() + 0.5,
                (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5,
                SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.BLOCKS,
                1.0F,
                1.0F
        );
    }

    public static void setTarget(EndRelayBlockEntity blockEntity,
            BlockPos teleportDestinationPos) {
        blockEntity.setDestination(teleportDestinationPos);
    }

    private static boolean isChargeItem(ItemStack stack) {
        return stack.isOf(Items.END_CRYSTAL);
    }

    private static boolean canCharge(BlockState state) {
        return !state.get(CHARGED);
    }

    public static boolean isAllowedDim(World world) {
        return world.getRegistryKey() == World.END;
    }

    private void explode(World world, final BlockPos explodedPos) {
        world.removeBlock(explodedPos, false);
        Objects.requireNonNull(explodedPos);
        final boolean bl2 = world.getFluidState(explodedPos.up()).isIn(FluidTags.WATER);
        ExplosionBehavior explosionBehavior = new ExplosionBehavior() {
            public Optional<Float> getBlastResistance(Explosion explosion, BlockView world, BlockPos pos,
                    BlockState blockState, FluidState fluidState) {
                return pos.equals(explodedPos) && bl2 ? Optional.of(Blocks.WATER.getBlastResistance()) :
                        super.getBlastResistance(
                                explosion,
                                world,
                                pos,
                                blockState,
                                fluidState
                        );
            }
        };
        Vec3d vec3d = explodedPos.toCenterPos();
        world.createExplosion(
                null,
                world.getDamageSources().badRespawnPoint(vec3d),
                explosionBehavior,
                vec3d,
                5.0F,
                true,
                World.ExplosionSourceType.BLOCK
        );
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(CHARGED)) {
            if (random.nextInt(100) == 0) {
                world.playSound(
                        null,
                        (double) pos.getX() + 0.5,
                        (double) pos.getY() + 0.5,
                        (double) pos.getZ() + 0.5,
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_AMBIENT,
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F
                );
            }

            double d = (double) pos.getX() + 0.5 + (0.5 - random.nextDouble());
            double e = (double) pos.getY() + 1.0;
            double f = (double) pos.getZ() + 0.5 + (0.5 - random.nextDouble());
            double g = (double) random.nextFloat() * 0.04;
            world.addParticle(ParticleTypes.REVERSE_PORTAL, d, e, f, 0.0, g, 0.0);
        }
    }

    public static int getLuminance(BlockState state) {
        return state.get(CHARGED) ? 15 : 0;
    }

    public static FabricBlockSettings getBlockProperties() {
        return FabricBlockSettings.create()
                .mapColor(MapColor.BLACK)
                .instrument(Instrument.BASEDRUM)
                .requiresTool()
                .strength(50.0F, 1200.0F)
                .luminance(EndRelayBlock::getLuminance);
    }
}