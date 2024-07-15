package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.GT_HatchElement.OutputHatch;
import static gregtech.api.enums.GT_Values.AuthorEvgenWarGold;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_WATER_PUMP;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_WATER_PUMP_ACTIVE;
import static gregtech.api.util.GT_StructureUtility.buildHatchAdder;
import static gregtech.api.util.GT_StructureUtility.ofFrame;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.VoidProtectionHelper;
import gregtech.common.blocks.GT_Block_Casings9;
import gtPlusPlus.core.util.minecraft.PlayerUtils;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GregtechMeta_MultiBlockBase;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class GregtechMetaTileEntity_WaterPump extends GregtechMeta_MultiBlockBase<GregtechMetaTileEntity_WaterPump>
    implements ISurvivalConstructable {

    public GregtechMetaTileEntity_WaterPump(String aName) {
        super(aName);
    }

    public GregtechMetaTileEntity_WaterPump(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Override
    public IMetaTileEntity newMetaEntity(final IGregTechTileEntity aTileEntity) {
        return new GregtechMetaTileEntity_WaterPump(this.mName);
    }

    @Override
    public String getMachineType() {
        return "Water Pump";
    }

    private static IStructureDefinition<GregtechMetaTileEntity_WaterPump> STRUCTURE_DEFINITION = null;

    private static final int horizontalOffSet = 1;
    private static final int verticalOffSet = 2;
    private static final int depthOffSet = 0;
    private static final String tier1 = "tier1";
    private static final String tier2 = "tier2";

    private static final int COUNT_OF_WATER_WITH_HUMIDITY = 5_000;
    private static final int PROGRESSION_TIME = 20;

    private int mSetTier = 1;

    private float CURRENT_HUMIDITY;
    private boolean CAN_SEE_SKY;

    private static final Fluid water = FluidRegistry.getFluid("water");

    private FluidStack[] getWater() {
        return new FluidStack[] { new FluidStack(water, getWaterCount()) };
    }

    private int mCountCasing;

    private boolean canSeeSky() {
        return this.getBaseMetaTileEntity()
            .getWorld()
            .canBlockSeeTheSky(
                this.getBaseMetaTileEntity()
                    .getXCoord(),
                this.getBaseMetaTileEntity()
                    .getYCoord() + 2,
                this.getBaseMetaTileEntity()
                    .getZCoord());
    }

    private float getHumidity() {
        return this.getBaseMetaTileEntity()
            .getWorld()
            .getBiomeGenForCoords(getBaseMetaTileEntity().getXCoord(), getBaseMetaTileEntity().getZCoord()).rainfall;
    }

    private int getWaterCount() {
        return (int) (CURRENT_HUMIDITY * COUNT_OF_WATER_WITH_HUMIDITY * mSetTier);
    }

    @Override
    public IStructureDefinition<GregtechMetaTileEntity_WaterPump> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {

            STRUCTURE_DEFINITION = StructureDefinition.<GregtechMetaTileEntity_WaterPump>builder()

                .addShape(
                    tier1,
                    transpose(
                        new String[][] {
                            // spotless:off
                        { " A ", " A ", "AAA", " A " },
                        { " A ", "   ", "A A", " A " },
                        { "C~C", "CCC", "CCC", "CCC" } }))
                            // spotless:on
                .addShape(
                    tier2,
                    transpose(
                        new String[][] {
                            // spotless:off
                        { " D ", " D ", "DDD", " D " },
                        { " D ", "   ", "D D", " D " },
                        { "C~C", "CCC", "CCC", "CCC" } }))
                            // spotless:on
                .addElement('A', ofFrame(Materials.Bronze))
                .addElement('D', ofFrame(Materials.Steel))
                .addElement(
                    'C',
                    buildHatchAdder(GregtechMetaTileEntity_WaterPump.class).atLeast(OutputHatch)
                        .casingIndex(((GT_Block_Casings9) GregTech_API.sBlockCasings9).getTextureIndex(2))
                        .dot(1)
                        .buildAndChain(onElementPass(x -> ++x.mCountCasing, ofBlock(GregTech_API.sBlockCasings9, 2))))
                .build();

        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        if (stackSize.stackSize == 1) {
            this.buildPiece(tier1, stackSize, hintsOnly, horizontalOffSet, verticalOffSet, depthOffSet);
        } else {
            this.buildPiece(tier2, stackSize, hintsOnly, horizontalOffSet, verticalOffSet, depthOffSet);
        }
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (this.mMachine) return -1;
        int built = 0;
        if (stackSize.stackSize == 1) {
            mSetTier = 1;
            built += this.survivialBuildPiece(
                tier1,
                stackSize,
                horizontalOffSet,
                verticalOffSet,
                depthOffSet,
                elementBudget,
                env,
                false,
                true);
        } else {
            mSetTier = 2;
            built += this.survivialBuildPiece(
                tier2,
                stackSize,
                horizontalOffSet,
                verticalOffSet,
                depthOffSet,
                elementBudget,
                env,
                false,
                true);
        }
        return built;
    }

    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        if (mSetTier >= 2) {
            if (!checkPiece(tier2, horizontalOffSet, verticalOffSet, depthOffSet)) return false;
        } else if (!checkPiece(tier1, horizontalOffSet, verticalOffSet, depthOffSet)) return false;

        if (this.mOutputHatches.size() != 1) return false;

        CURRENT_HUMIDITY = getHumidity();
        CAN_SEE_SKY = canSeeSky();
        return mCountCasing >= 10;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection sideDirection,
        ForgeDirection facingDirection, int colorIndex, boolean active, boolean redstoneLevel) {
        if (sideDirection == facingDirection) {
            if (active) return new ITexture[] {
                Textures.BlockIcons
                    .getCasingTextureForId(GT_Utility.getCasingTextureIndex(GregTech_API.sBlockCasings9, 2)),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_WATER_PUMP_ACTIVE)
                    .extFacing()
                    .build() };
            return new ITexture[] {
                Textures.BlockIcons
                    .getCasingTextureForId(GT_Utility.getCasingTextureIndex(GregTech_API.sBlockCasings9, 2)),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_WATER_PUMP)
                    .extFacing()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons
            .getCasingTextureForId(GT_Utility.getCasingTextureIndex(GregTech_API.sBlockCasings9, 2)) };
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType(getMachineType())
            .addInfo("Controller Block for the Water Pump")
            .addInfo("Generates water based on biomes humidity")
            .addInfo("Has 2 tiers: Bronze and Steel")
            .addInfo("Steel tier extracts water 2x more water")
            .addInfo("Tiers can be configured with a screwdriver")
            .addInfo(
                EnumChatFormatting.AQUA + "Generation water: " + EnumChatFormatting.WHITE + "(Humidity * 50) * Tier")
            .addSeparator()
            .beginStructureBlock(3, 3, 5, false)
            .addInputHatch(EnumChatFormatting.GOLD + "1" + EnumChatFormatting.GRAY + " Any casing", 1)
            .addStructureInfo(EnumChatFormatting.BLUE + "Tier " + EnumChatFormatting.DARK_PURPLE + 1)
            .addStructureInfo(EnumChatFormatting.GOLD + "10" + EnumChatFormatting.GRAY + " Bronze Frame Box")
            .addStructureInfo(EnumChatFormatting.GOLD + "10" + EnumChatFormatting.GRAY + " Wooden Casing")
            .addStructureInfo(EnumChatFormatting.BLUE + "Tier " + EnumChatFormatting.DARK_PURPLE + 2)
            .addStructureInfo("10" + EnumChatFormatting.GRAY + " Steel Frame Box")
            .addStructureInfo(EnumChatFormatting.GOLD + "10 " + EnumChatFormatting.GRAY + " Wooden Casing")
            .toolTipFinisher(AuthorEvgenWarGold);
        return tt;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

        }.setEuModifier(0F)
            .setMaxParallelSupplier(this::getMaxParallelRecipes);
    }

    @Override
    @NotNull
    public CheckRecipeResult checkProcessing() {
        if (!CAN_SEE_SKY) {
            CAN_SEE_SKY = canSeeSky();
            return CheckRecipeResultRegistry.NO_SEE_SKY;
        }

        mMaxProgresstime = PROGRESSION_TIME;
        mOutputFluids = getWater();

        VoidProtectionHelper voidProtection = new VoidProtectionHelper().setMachine(this)
            .setFluidOutputs(mOutputFluids)
            .build();

        if (voidProtection.isFluidFull()) {
            mOutputFluids = null;
            mMaxProgresstime = 0;
            return CheckRecipeResultRegistry.FLUID_OUTPUT_FULL;
        } else {
            updateSlots();
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            if ((aTick % 1200) == 0) {
                CURRENT_HUMIDITY = getHumidity();
                CAN_SEE_SKY = canSeeSky();
            }
        }
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currenttip, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();

        currenttip.add(
            StatCollector.translateToLocal("GT5U.machines.tier") + ": "
                + EnumChatFormatting.BLUE
                + tag.getInteger("mSetTier")
                + EnumChatFormatting.RESET);
        currenttip.add(
            StatCollector.translateToLocal("GT5U.machines.water_pump") + " "
                + EnumChatFormatting.BLUE
                + tag.getFloat("humidity")
                + " %"
                + EnumChatFormatting.RESET);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setFloat("humidity", CURRENT_HUMIDITY * 100);
        tag.setInteger("mSetTier", mSetTier);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("mSetTier", mSetTier);
    }

    @Override
    public void loadNBTData(final NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        mSetTier = aNBT.getInteger("mSetTier");
    }

    @Override
    public void onModeChangeByScrewdriver(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        mSetTier++;
        if (mSetTier > 2) mSetTier = 1;
        mUpdated = true;
        PlayerUtils.messagePlayer(aPlayer, "Tier: " + mSetTier);
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public int getMaxParallelRecipes() {
        return 1;
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && !f.isVerticallyFliped();
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }
}