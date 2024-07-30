package gregtech.common.blocks;

import static gregtech.api.enums.GT_Values.W;
import static gregtech.api.enums.Mods.GregTech;

import java.util.List;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Dyes;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.ITexture;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Frame;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.render.GT_Renderer_Block;

// TODO:
// - Proper name in WAILA
// - Mining level/block breaking with wrench
// - Drop correct frame on breaking instead of generic .0.name
// - Crafting using new frames instead of old frames
// - Colen's postea thing to replace old frames with new ones
// - Save TE/cover data on world reload!

public class GT_Block_FrameBox extends BlockContainer {

    protected final String mUnlocalizedName;

    private static final String DOT_NAME = ".name";
    private static final String DOT_TOOLTIP = ".tooltip";

    public GT_Block_FrameBox() {
        super(Material.glass);
        this.mUnlocalizedName = "gt.blockframes";
        setBlockName(this.mUnlocalizedName);
        GT_LanguageManager.addStringLocalization(getUnlocalizedName() + "." + W + ".name", "Any Sub Block of this one");
        GameRegistry.registerBlock(this, GT_Item_Frames.class, getUnlocalizedName());

        for (int meta = 1; meta < GregTech_API.sGeneratedMaterials.length; meta++) {
            Materials material = GregTech_API.sGeneratedMaterials[meta];
            if (material != null && (material.mTypes & 0x02) != 0) {
                GT_LanguageManager.addStringLocalization(
                    getUnlocalizedName() + "." + meta + DOT_NAME,
                    GT_LanguageManager.i18nPlaceholder ? getLocalizedNameFormat(material)
                        : getLocalizedName(GregTech_API.sGeneratedMaterials[meta]));
                GT_LanguageManager.addStringLocalization(
                    getUnlocalizedName() + "." + meta + DOT_TOOLTIP,
                    GregTech_API.sGeneratedMaterials[meta].getToolTip());
            }
        }
    }

    public ItemStack getStackForm(int amount, int meta) {
        Item item = GameRegistry.findItem(GregTech.ID, getUnlocalizedName());
        return new ItemStack(item, amount, meta);
    }

    public String getLocalizedNameFormat(Materials aMaterial) {
        return switch (aMaterial.mName) {
            case "InfusedAir", "InfusedDull", "InfusedEarth", "InfusedEntropy", "InfusedFire", "InfusedOrder", "InfusedVis", "InfusedWater" -> "%material Infused Stone";
            case "Vermiculite", "Bentonite", "Kaolinite", "Talc", "BasalticMineralSand", "GraniticMineralSand", "GlauconiteSand", "CassiteriteSand", "GarnetSand", "QuartzSand", "Pitchblende", "FullersEarth" -> "%material";
            default -> "%material" + " Frame Box";
        };
    }

    @Override
    public String getUnlocalizedName() {
        return mUnlocalizedName;
    }

    public String getLocalizedName(Materials aMaterial) {
        return aMaterial.getDefaultLocalizedNameForItem(getLocalizedNameFormat(aMaterial));
    }

    private boolean isCover(ItemStack item) {
        return GT_Utility.isStackInList(item, GregTech_API.sCovers.keySet());
    }

    private void createFrame(World worldIn, int x, int y, int z, BaseMetaPipeEntity baseMte) {
        // Obtain metadata to grab proper material identifier
        int meta = worldIn.getBlockMetadata(x, y, z);
        Materials material = GregTech_API.sGeneratedMaterials[meta];
        GT_MetaPipeEntity_Frame frame = new GT_MetaPipeEntity_Frame("GT_Frame_" + material, material);
        baseMte.setMetaTileEntity(frame);
        frame.setBaseMetaTileEntity(baseMte);
    }

    private BaseMetaPipeEntity spawnFrameEntity(World worldIn, int x, int y, int z) {
        // Spawn a TE frame box at this location, then apply the cover
        BaseMetaPipeEntity newTileEntity = new BaseMetaPipeEntity();
        createFrame(worldIn, x, y, z, newTileEntity);
        worldIn.setTileEntity(x, y, z, newTileEntity);
        return newTileEntity;
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer player, int side, float subX,
        float subY, float subZ) {
        // Get ForgeDirection from side identifier.
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        // If this block already holds a TE, just forward the call
        TileEntity te = worldIn.getTileEntity(x, y, z);
        if (te != null) {
            BaseMetaPipeEntity baseTileEntity = (BaseMetaPipeEntity) te;
            // If this baseTileEntity has no MetaTileEntity associated with it, we need to create it
            // This happens on world load for some reason
            if (baseTileEntity.getMetaTileEntity() == null) {
                createFrame(worldIn, x, y, z, baseTileEntity);
            }
            return baseTileEntity.onRightclick(player, direction, subX, subY, subZ);
        }

        // If there was no TileEntity yet, we need to check if the player was holding a cover item and if so
        // spawn a new frame box to apply the cover to
        ItemStack item = player.getHeldItem();
        if (isCover(item)) {
            BaseMetaPipeEntity newTileEntity = spawnFrameEntity(worldIn, x, y, z);
            newTileEntity.setCoverItemAtSide(direction, item);
            return true;
        }

        return false;
    }

    @Override
    public int getRenderType() {
        if (GT_Renderer_Block.INSTANCE == null) {
            return super.getRenderType();
        }
        return GT_Renderer_Block.mRenderID;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item aItem, CreativeTabs aTab, List<ItemStack> aList) {
        for (int i = 0; i < GregTech_API.sGeneratedMaterials.length; i++) {
            Materials tMaterial = GregTech_API.sGeneratedMaterials[i];
            // If material is not null and has a frame box item associated with it
            if ((tMaterial != null) && ((tMaterial.mTypes & 0x02) != 0)) {
                aList.add(new ItemStack(aItem, 1, i));
            }
        }
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        // This doesn't apply the frame color so AE2 facades look bad, but it renders fine in inventory and in world.
        // It's probably fine, who makes facades out of frame boxes anyway.
        Materials material = GregTech_API.sGeneratedMaterials[meta];
        return material.mIconSet.mTextures[OrePrefixes.frameGt.mTextureIndex].getIcon();
    }

    public ITexture[] getTexture(int meta) {
        Materials material = GregTech_API.sGeneratedMaterials[meta];
        return new ITexture[] { TextureFactory.of(
            material.mIconSet.mTextures[OrePrefixes.frameGt.mTextureIndex],
            Dyes.getModulation(-1, material.mRGBa)) };
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return null;
    }
}
