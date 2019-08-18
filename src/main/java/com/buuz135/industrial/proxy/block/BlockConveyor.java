/*
 * This file is part of Industrial Foregoing.
 *
 * Copyright 2019, Buuz135
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.buuz135.industrial.proxy.block;

import com.buuz135.industrial.IndustrialForegoing;
import com.buuz135.industrial.api.conveyor.ConveyorUpgrade;
import com.buuz135.industrial.module.ModuleCore;
import com.buuz135.industrial.proxy.block.tile.TileEntityConveyor;
import com.hrznstudio.titanium.api.IFactory;
import com.hrznstudio.titanium.api.raytrace.DistanceRayTraceResult;
import com.hrznstudio.titanium.block.BlockTileBase;
import com.hrznstudio.titanium.module.api.RegistryManager;
import com.hrznstudio.titanium.recipe.generator.CraftingJsonData;
import com.hrznstudio.titanium.recipe.generator.IIngredient;
import com.hrznstudio.titanium.util.RayTraceUtils;
import com.hrznstudio.titanium.util.TileUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlockConveyor extends BlockTileBase<TileEntityConveyor> implements IItemProvider {

    public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;
    public static final EnumProperty<EnumType> TYPE = EnumProperty.create("type", EnumType.class);
    public static final EnumProperty<EnumSides> SIDES = EnumProperty.create("sides", EnumSides.class);
    private static String[] dyes = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"};
    private ConveyorItem item;

    public BlockConveyor(ItemGroup group) {
        super("conveyor", Properties.create(Material.ANVIL, MaterialColor.ADOBE).doesNotBlockMovement().hardnessAndResistance(2.0f), TileEntityConveyor.class);
        this.setDefaultState(this.getDefaultState().with(FACING, Direction.NORTH).with(SIDES, EnumSides.NONE));
        this.item = new ConveyorItem(this, group);
        this.setItemGroup(group);
    }

    @Override
    public Item asItem() {
        return item;
    }

    @Override
    public IFactory<BlockItem> getItemBlockFactory() {
        return this::getItem;
    }

    @Override
    public void addAlternatives(RegistryManager registry) {
        super.addAlternatives(registry);
        IndustrialForegoing.RECIPES.addRecipe(
                CraftingJsonData.ofShaped(new ItemStack(this, 6),
                        new String[]{"ppp", "iri", "ppp"},
                        'p', IIngredient.TagIngredient.of("forge:plastic"),
                        'i', IIngredient.TagIngredient.of("forge:ingots/iron"),
                        'r', IIngredient.ItemStackIngredient.of(new ItemStack(Items.REDSTONE)))
        );
    }

    @OnlyIn(Dist.CLIENT)
    public void createRecipe() {
//        RecipeUtils.addShapedRecipe(new ItemStack(this, 4, 0), "ppp", "iri", "ppp",
//                'p', ItemRegistry.plastic,
//                'i', "ingotIron",
//                'r', Items.REDSTONE);
//        for (int i = 0; i < dyes.length; i++) {
//            RecipeUtils.addShapedRecipe(new ItemStack(this, 8, 15 - i), "_" + dyes[i].toLowerCase(), new HashMap<>(), "ccc", "cdc", "ccc",
//                    'c', new ItemStack(this, 1, OreDictionary.WILDCARD_VALUE),
//                    'd', "dye" + dyes[i]);
//        }
    }

    @Override
    public int getWeakPower(BlockState blockState, IBlockReader world, BlockPos pos, Direction side) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityConveyor) {
            return ((TileEntityConveyor) tileEntity).getPower();
        }
        return super.getWeakPower(blockState, world, pos, side);
    }

    @Override
    public int getStrongPower(BlockState blockState, IBlockReader world, BlockPos pos, Direction side) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityConveyor) {
            return side == Direction.UP ? ((TileEntityConveyor) tileEntity).getPower() : 0;
        }
        return super.getStrongPower(blockState, world, pos, side);
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityConveyor) {
            if (target instanceof DistanceRayTraceResult) {
                ConveyorUpgrade upgrade = ((TileEntityConveyor) tileEntity).getUpgradeMap().get(getFacingUpgradeHit(state, player.world, pos, player));
                if (upgrade != null) {
                    return new ItemStack(upgrade.getFactory().getUpgradeItem(), 1);
                }
            }
            return new ItemStack(this, 1);//TODO Fix types
        }
        return super.getPickBlock(state, target, world, pos, player);
    }

    @Override
    public BlockState getStateAtViewpoint(BlockState state, IBlockReader world, BlockPos pos, Vec3d viewpoint) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityConveyor) {
            state = state.with(FACING, ((TileEntityConveyor) tileEntity).getFacing()).with(TYPE, ((TileEntityConveyor) tileEntity).getConveyorType());
        }
        if (state.get(TYPE).equals(EnumType.FLAT) || state.get(TYPE).equals(EnumType.FLAT_FAST)) {
            Direction right = state.get(FACING).rotateY();
            Direction left = state.get(FACING).rotateYCCW();
            if (isConveyorAndFacing(pos.offset(right), world, left) && isConveyorAndFacing(pos.offset(left), world, right) || (isConveyorAndFacing(pos.offset(right).down(), world, left) && isConveyorAndFacing(pos.offset(left).down(), world, right))) {
                state = state.with(SIDES, EnumSides.BOTH);
            } else if (isConveyorAndFacing(pos.offset(right), world, left) || isConveyorAndFacing(pos.offset(right).down(), world, left)) {
                state = state.with(SIDES, EnumSides.RIGHT);
            } else if (isConveyorAndFacing(pos.offset(left), world, right) || isConveyorAndFacing(pos.offset(left).down(), world, right)) {
                state = state.with(SIDES, EnumSides.LEFT);
            } else {
                state = state.with(SIDES, EnumSides.NONE);
            }
        }
        return state;
    }

    private boolean isConveyorAndFacing(BlockPos pos, IBlockReader world, Direction toFace) {
        return world.getBlockState(pos).getBlock() instanceof BlockConveyor && (toFace == null || world.getBlockState(pos).get(FACING).equals(toFace));
    }

    //@Override
    //public BlockFaceShape getBlockFaceShape(IBlockReader worldIn, BlockState state, BlockPos pos, Direction face) {
    //    return state.get(TYPE).isVertical() ? BlockFaceShape.UNDEFINED : face == Direction.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    //}

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean p_220069_6_) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, p_220069_6_);
        //        if (!worldIn.isRemote && !canPlaceBlockAt(worldIn, pos)) {
//            worldIn.destroyBlock(pos, false);
//        }
    }

    @Override
    public List<VoxelShape> getBoundingBoxes(BlockState state, IBlockReader source, BlockPos pos) {
        List<VoxelShape> boxes = new ArrayList<>();
        if (state.get(TYPE).isVertical()) {
            boxes.add(VoxelShapes.create(0, 0, 0, 1, 0.40, 1));
        } else {
            boxes.add(VoxelShapes.create(0, 0, 0, 1, 1 / 16D, 1));
        }
        TileEntity entity = source.getTileEntity(pos);
        if (entity instanceof TileEntityConveyor) {
            for (ConveyorUpgrade upgrade : ((TileEntityConveyor) entity).getUpgradeMap().values())
                if (upgrade != null)
                    boxes.add(VoxelShapes.create(upgrade.getBoundingBox().getBoundingBox()));
        }
        return boxes;
    }

    @Override
    public boolean hasCustomBoxes(BlockState state, IBlockReader source, BlockPos pos) {
        return true;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(FACING, SIDES, TYPE);
    }

    @Override
    public boolean hasTileEntity() {
        return true;
    }

    @Override
    public IFactory<TileEntityConveyor> getTileEntityFactory() {
        return TileEntityConveyor::new;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (placer != null) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (tileEntity instanceof TileEntityConveyor) {
                ((TileEntityConveyor) tileEntity).setFacing(placer.getHorizontalFacing());
            }
            updateConveyorPlacing(worldIn, pos, state, true);
        }
    }

    @Override
    public void onPlayerDestroy(IWorld world, BlockPos pos, BlockState state) {
        NonNullList<ItemStack> list = NonNullList.create();
        //getDrops(state, list, world.getWorld(), pos, 0); TODO
        for (ItemStack stack : list) {
            float f = 0.7F;
            float d0 = world.getRandom().nextFloat() * f + (1.0F - f) * 0.5F;
            float d1 = world.getRandom().nextFloat() * f + (1.0F - f) * 0.5F;
            float d2 = world.getRandom().nextFloat() * f + (1.0F - f) * 0.5F;
            ItemEntity ItemEntity = new ItemEntity(world.getWorld(), pos.getX() + d0, pos.getY() + d1, pos.getZ() + d2, stack);
            world.addEntity(ItemEntity);
        }
        super.onPlayerDestroy(world, pos, state);
    }


    //@Override
    //public void getDrops(BlockState state, NonNullList<ItemStack> drops, World world, BlockPos pos, int fortune) {
    //    TileEntity entity = world.getTileEntity(pos);
    //    if (entity instanceof TileEntityConveyor) {
    //        drops.add(new ItemStack(this, 1));
    //        for (ConveyorUpgrade upgrade : ((TileEntityConveyor) entity).getUpgradeMap().values()) {
    //            drops.addAll(upgrade.getDrops());
    //        }
    //        if (((TileEntityConveyor) entity).getConveyorType().isFast()) {
    //            drops.add(new ItemStack(Items.GLOWSTONE_DUST, 1));
    //        }
    //        if (((TileEntityConveyor) entity).isSticky()) {
    //            drops.add(new ItemStack(ModuleCore.PLASTIC, 1));
    //        }
    //    }
    //}
//
    private void updateConveyorPlacing(World worldIn, BlockPos pos, BlockState state, boolean first) {
        TileEntity entity = worldIn.getTileEntity(pos);
        if (entity instanceof TileEntityConveyor) {
            Direction direction = ((TileEntityConveyor) entity).getFacing();
            Direction right = state.get(FACING).rotateY();
            Direction left = state.get(FACING).rotateYCCW();
            if (((TileEntityConveyor) entity).getUpgradeMap().isEmpty()) {
                if (isConveyorAndFacing(pos.up().offset(direction), worldIn, null)) {//SELF UP
                    ((TileEntityConveyor) entity).setType(((TileEntityConveyor) entity).getConveyorType().getVertical(Direction.UP));
                } else if (isConveyorAndFacing(pos.up().offset(direction.getOpposite()), worldIn, null)) { //SELF DOWN
                    ((TileEntityConveyor) entity).setType(((TileEntityConveyor) entity).getConveyorType().getVertical(Direction.DOWN));
                }
            }
            //UPDATE SURROUNDINGS
            if (!first) return;
            if (isConveyorAndFacing(pos.offset(direction.getOpposite()).down(), worldIn, direction)) { //BACK DOWN
                updateConveyorPlacing(worldIn, pos.offset(direction.getOpposite()).down(), state, false);
            }
            if (isConveyorAndFacing(pos.offset(left).down(), worldIn, right)) { //LEFT DOWN
                updateConveyorPlacing(worldIn, pos.offset(left).down(), state, false);
            }
            if (isConveyorAndFacing(pos.offset(right).down(), worldIn, left)) { //RIGHT DOWN
                updateConveyorPlacing(worldIn, pos.offset(right).down(), state, false);
            }
            if (isConveyorAndFacing(pos.offset(direction).down(), worldIn, direction)) { //FRONT DOWN
                updateConveyorPlacing(worldIn, pos.offset(direction).down(), state, false);
            }
            worldIn.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        ItemStack handStack = player.getHeldItem(hand);
        if (tileEntity instanceof TileEntityConveyor) {
            if (player.isSneaking()) {
                Direction facing = getFacingUpgradeHit(state, worldIn, pos, player);
                if (facing != null) {
                    ((TileEntityConveyor) tileEntity).removeUpgrade(facing, true);
                    return true;
                }
                return false;
            } else {
                Direction facing = getFacingUpgradeHit(state, worldIn, pos, player);
                if (facing == null) {
                    if (handStack.getItem().equals(Items.GLOWSTONE_DUST) && !((TileEntityConveyor) tileEntity).getConveyorType().isFast()) {
                        ((TileEntityConveyor) tileEntity).setType(((TileEntityConveyor) tileEntity).getConveyorType().getFast());
                        handStack.shrink(1);
                        return true;
                    }
                    if (handStack.getItem().equals(ModuleCore.PLASTIC) && !((TileEntityConveyor) tileEntity).isSticky()) {
                        ((TileEntityConveyor) tileEntity).setSticky(true);
                        handStack.shrink(1);
                        return true;
                    }
                    if (handStack.getItem() instanceof DyeItem) {
                        ((TileEntityConveyor) tileEntity).setColor(((DyeItem) handStack.getItem()).getDyeColor());
                        return true;
                    }
                } else {
                    if (((TileEntityConveyor) tileEntity).hasUpgrade(facing)) {
                        ConveyorUpgrade upgrade = ((TileEntityConveyor) tileEntity).getUpgradeMap().get(facing);
                        if (upgrade.onUpgradeActivated(player, hand)) {
                            return true;
                        } else if (upgrade.hasGui()) {
                            ((TileEntityConveyor) tileEntity).openGui(player, facing);
                            return true;
                        }
                    }
                }
                return false;

            }
        }
        return super.onBlockActivated(state, worldIn, pos, player, hand, ray);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext p_220053_4_) {
        if (state.get(TYPE).isVertical()) {
            return VoxelShapes.create(0, 0, 0, 1, 0.40, 1);
        } else {
            VoxelShape shape = VoxelShapes.create(0, 0, 0, 1, 1 / 16D, 1);
            TileEntity entity = world.getTileEntity(pos);
            if (entity instanceof TileEntityConveyor) {
                for (ConveyorUpgrade upgrade : ((TileEntityConveyor) entity).getUpgradeMap().values())
                    if (upgrade != null)
                        shape = VoxelShapes.or(shape, VoxelShapes.create(upgrade.getBoundingBox().getBoundingBox()));
            }
            return shape;
        }
    }

    public Direction getFacingUpgradeHit(BlockState state, World worldIn, BlockPos pos, PlayerEntity player) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        RayTraceResult result = RayTraceUtils.rayTraceSimple(worldIn, player, 32, 0);
        if (result instanceof BlockRayTraceResult) {
            VoxelShape hit = RayTraceUtils.rayTraceVoxelShape((BlockRayTraceResult) result, worldIn, player, 32, 0);
            if (hit != null && tileEntity instanceof TileEntityConveyor) {
                for (Direction Direction : ((TileEntityConveyor) tileEntity).getUpgradeMap().keySet()) {
                    if (VoxelShapes.compare(((TileEntityConveyor) tileEntity).getUpgradeMap().get(Direction).getBoundingBox(), hit, IBooleanFunction.AND)) {
                        return Direction;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean hasIndividualRenderVoxelShape() {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        TileEntityConveyor tile = new TileEntityConveyor();
        return tile;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getPlayer().getHorizontalFacing());
    }

    @Override
    public BlockRenderType getRenderType(BlockState p_149645_1_) {
        return BlockRenderType.MODEL;
    }

    @OnlyIn(Dist.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
        super.onEntityCollision(state, worldIn, pos, entityIn);
        TileEntity entity = worldIn.getTileEntity(pos);
        if (entity instanceof TileEntityConveyor) {
            ((TileEntityConveyor) entity).handleEntityMovement(entityIn);
        }
    }

    @Override
    public NonNullList<ItemStack> getDynamicDrops(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        NonNullList<ItemStack> drops = NonNullList.create();
        Optional<TileEntityConveyor> entity = TileUtil.getTileEntity(worldIn, pos, TileEntityConveyor.class);
        entity.ifPresent(tileEntityConveyor -> {
            for (Direction value : Direction.values()) {
                if (tileEntityConveyor.getUpgradeMap().containsKey(value)) {
                    ConveyorUpgrade upgrade = tileEntityConveyor.getUpgradeMap().get(value);
                    drops.addAll(upgrade.getDrops());
                }
            }
            if (tileEntityConveyor.isSticky()) drops.add(new ItemStack(ModuleCore.PLASTIC));
            if (tileEntityConveyor.getConveyorType().isFast()) drops.add(new ItemStack(Items.GLOWSTONE_DUST));
        });
        return drops;
    }

    @Override
    public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType) {
        return true;
    }

    @Override
    public boolean canEntitySpawn(BlockState state, IBlockReader worldIn, BlockPos pos, EntityType<?> type) {
        return true;
    }

    @Override
    public boolean canSpawnInBlock() {
        return true;
    }

    public ConveyorItem getItem() {
        return item;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new TileEntityConveyor();
    }

    public enum EnumType implements IStringSerializable {


        FLAT(false), UP(false), DOWN(false), FLAT_FAST(true), UP_FAST(true), DOWN_FAST(true);

        private boolean fast;

        EnumType(boolean fast) {
            this.fast = fast;
        }

        public static EnumType getFromName(String name) {
            for (EnumType type : EnumType.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return FLAT;
        }

        public boolean isFast() {
            return fast;
        }

        public EnumType getFast() {
            switch (this) {
                case FLAT:
                    return FLAT_FAST;
                case UP:
                    return UP_FAST;
                case DOWN:
                    return DOWN_FAST;
                default:
                    return this;
            }
        }

        public EnumType getVertical(Direction facing) {
            if (this.isFast()) {
                if (facing == Direction.UP) {
                    return UP_FAST;
                }
                if (facing == Direction.DOWN) {
                    return DOWN_FAST;
                }
                return FLAT_FAST;
            } else {
                if (facing == Direction.UP) {
                    return UP;
                }
                if (facing == Direction.DOWN) {
                    return DOWN;
                }
                return FLAT_FAST;
            }
        }

        public boolean isVertical() {
            return isDown() || isUp();
        }

        public boolean isUp() {
            return this.equals(UP) || this.equals(UP_FAST);
        }

        public boolean isDown() {
            return this.equals(DOWN) || this.equals(DOWN_FAST);
        }

        @Override
        public String getName() {
            return this.toString().toLowerCase();
        }

    }

    public enum EnumSides implements IStringSerializable {
        NONE, LEFT, RIGHT, BOTH;

        @Override
        public String getName() {
            return this.toString().toLowerCase();
        }
    }

    private class ConveyorItem extends BlockItem {

        public ConveyorItem(Block block, ItemGroup group) {
            super(block, new Item.Properties().group(group));
            this.setRegistryName(block.getRegistryName());
        }

    }

}
