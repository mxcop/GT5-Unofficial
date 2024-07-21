package gregtech.loaders.postload.chains;

import static com.github.bartimaeusnek.bartworks.system.material.WerkstoffLoader.FluorBuergerit;
import static gregtech.api.enums.MaterialsBotania.ManaDiamond;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalBathRecipes;
import static gregtech.api.recipe.RecipeMaps.distillationTowerRecipes;
import static gregtech.api.recipe.RecipeMaps.laserEngraverRecipes;
import static gregtech.api.recipe.RecipeMaps.multiblockChemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.purificationClarifierRecipes;
import static gregtech.api.recipe.RecipeMaps.purificationDegasifierRecipes;
import static gregtech.api.recipe.RecipeMaps.purificationFlocculationRecipes;
import static gregtech.api.recipe.RecipeMaps.purificationOzonationRecipes;
import static gregtech.api.recipe.RecipeMaps.purificationPhAdjustmentRecipes;
import static gregtech.api.recipe.RecipeMaps.purificationPlasmaHeatingRecipes;
import static gregtech.api.recipe.RecipeMaps.purificationUVTreatmentRecipes;
import static gregtech.api.util.GT_RecipeBuilder.SECONDS;

import java.util.Arrays;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import goodgenerator.items.MyMaterial;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.metadata.PurificationPlantBaseChanceKey;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.multi.purification.GT_MetaTileEntity_PurificationPlant;

public class GT_PurifiedWaterRecipes {

    static final PurificationPlantBaseChanceKey BASE_CHANCE = PurificationPlantBaseChanceKey.INSTANCE;

    public static void run() {
        final int duration = GT_MetaTileEntity_PurificationPlant.CYCLE_TIME_TICKS;

        // Grade 1 - Clarifier
        GT_Values.RA.stdBuilder()
            .itemInputs(ItemList.ActivatedCarbonFilterMesh.get(1))
            .fluidInputs(GT_ModHandler.getDistilledWater(1000L))
            .fluidOutputs(Materials.Grade1PurifiedWater.getFluid(900L))
            .itemOutputs(new ItemStack(Items.stick, 1), Materials.Stone.getDust(1), Materials.Gold.getNuggets(1))
            .outputChances(1000, 500, 100)
            .duration(duration)
            .eut(TierEU.RECIPE_LuV)
            .metadata(BASE_CHANCE, 70.0f)
            .addTo(purificationClarifierRecipes);

        // Activated Carbon Line
        GT_Values.RA.stdBuilder()
            .itemInputs(Materials.Carbon.getDust(1))
            .fluidInputs(Materials.PhosphoricAcid.getFluid(1000L))
            .itemOutputs(Materials.PreActivatedCarbon.getDust(1))
            .duration(5 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(multiblockChemicalReactorRecipes);
        GT_Values.RA.stdBuilder()
            .itemInputs(Materials.PreActivatedCarbon.getDust(1))
            .itemOutputs(Materials.DirtyActivatedCarbon.getDust(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .specialValue((int) HeatingCoilLevel.EV.getHeat())
            .addTo(blastFurnaceRecipes);
        GT_Values.RA.stdBuilder()
            .itemInputs(Materials.DirtyActivatedCarbon.getDust(1))
            .fluidInputs(Materials.Water.getFluid(1000L))
            .itemOutputs(Materials.ActivatedCarbon.getDust(1))
            .fluidOutputs(Materials.PhosphoricAcid.getFluid(1000L))
            .noOptimize()
            .duration(2 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(chemicalBathRecipes);
        GT_Values.RA.stdBuilder()
            .itemInputs(
                Materials.ActivatedCarbon.getDust(64),
                GT_OreDictUnificator.get(OrePrefixes.foil, Materials.Zinc, 16))
            .itemOutputs(ItemList.ActivatedCarbonFilterMesh.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        // Grade 2 - Ozonation
        for (ItemStack lens : GT_OreDictUnificator.getOres("craftingLensBlue")) {
            GT_Values.RA.stdBuilder()
                .itemInputs(GT_Utility.copyAmount(0, lens))
                .noOptimize()
                .fluidInputs(Materials.Air.getGas(10000L))
                .fluidOutputs(Materials.Ozone.getGas(2000L))
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_LuV)
                .addTo(laserEngraverRecipes);
        }

        // Recipes for ozonation, uses 128kL, 256kL, 512kL, 1m kL at each tier
        // 20% boost per tier
        // Gets you up to 80%, need to water boost for 100%
        for (int tier = 1; tier <= 4; ++tier) {
            GT_Values.RA.stdBuilder()
                .noOptimize()
                .fluidInputs(
                    Materials.Grade1PurifiedWater.getFluid(1000L),
                    Materials.Ozone.getGas(1000 * (long) Math.pow(2, (tier + 6))))
                .fluidOutputs(Materials.Grade2PurifiedWater.getFluid(900L))
                .itemOutputs(Materials.Manganese.getDust(1), Materials.Iron.getDust(1), Materials.Sulfur.getDust(1))
                .outputChances(500, 500, 500)
                .duration(duration)
                .ignoreCollision()
                .eut(TierEU.RECIPE_LuV)
                .metadata(BASE_CHANCE, tier * 20.0f)
                .addTo(purificationOzonationRecipes);
        }

        // Grade 3 - Flocculation.
        GT_Values.RA.stdBuilder()
            .fluidInputs(Materials.Grade2PurifiedWater.getFluid(1000L))
            .fluidOutputs(Materials.Grade3PurifiedWater.getFluid(900L))
            .ignoreCollision()
            .itemOutputs(
                new ItemStack(Items.clay_ball, 1),
                Materials.QuartzSand.getDust(1),
                Materials.PolyvinylChloride.getNuggets(1))
            .outputChances(1000, 500, 100)
            .duration(duration)
            .eut(TierEU.RECIPE_ZPM)
            .metadata(BASE_CHANCE, 1 * 10.0f)
            .addTo(purificationFlocculationRecipes);

        // 2 Al(OH)3 + 3 HCl -> Al2(OH)3 Cl3 + 3 H2O
        GT_Values.RA.stdBuilder()
            .itemInputs(Materials.Aluminiumhydroxide.getDust(8))
            .fluidInputs(Materials.HydrochloricAcid.getFluid(3000L))
            .fluidOutputs(Materials.PolyAluminiumChloride.getFluid(1000L), Materials.Water.getFluid(3000L))
            .duration(4 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(multiblockChemicalReactorRecipes);

        // Diluted is twice what chem balance would suggest, but it is 2:1 with hydrochloric acid which makes it
        // correct I believe.
        GT_Values.RA.stdBuilder()
            .fluidInputs(Materials.FlocculationWasteLiquid.getFluid(1000L))
            .itemOutputs(Materials.Aluminium.getDust(2))
            .fluidOutputs(Materials.Oxygen.getGas(3000L), Materials.DilutedHydrochloricAcid.getFluid(6000L))
            .duration(1 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(distillationTowerRecipes);

        // Grade 4 - pH adjustment
        GT_Values.RA.stdBuilder()
            .fluidInputs(Materials.Grade3PurifiedWater.getFluid(1000L))
            .fluidOutputs(Materials.Grade4PurifiedWater.getFluid(900L))
            .ignoreCollision()
            .duration(duration)
            .eut(TierEU.RECIPE_ZPM)
            .metadata(BASE_CHANCE, 0.0f)
            .addTo(purificationPhAdjustmentRecipes);

        // Grade 5 - Plasma Heating
        GT_Values.RA.stdBuilder()
            .fluidInputs(Materials.Grade4PurifiedWater.getFluid(1000L))
            .fluidOutputs(Materials.Grade5PurifiedWater.getFluid(900L))
            .ignoreCollision()
            .duration(duration)
            .eut(TierEU.RECIPE_UV)
            .metadata(BASE_CHANCE, 0.0f)
            .addTo(purificationPlasmaHeatingRecipes);

        // Grade 6 - UV treatment
        GT_Values.RA.stdBuilder()
            .fluidInputs(Materials.Grade5PurifiedWater.getFluid(1000L))
            .fluidOutputs(Materials.Grade6PurifiedWater.getFluid(900L))
            // These are not actually consumed and are purely for display purposes
            .special(
                Arrays.asList(
                    MyMaterial.orundum.get(OrePrefixes.lens, 1),
                    GT_OreDictUnificator.get(OrePrefixes.lens, Materials.Amber, 1),
                    GT_OreDictUnificator.get(OrePrefixes.lens, Materials.InfusedAir, 1),
                    GT_OreDictUnificator.get(OrePrefixes.lens, Materials.Emerald, 1),
                    GT_OreDictUnificator.get(OrePrefixes.lens, ManaDiamond, 1),
                    GT_OreDictUnificator.get(OrePrefixes.lens, Materials.BlueTopaz, 1),
                    GT_OreDictUnificator.get(OrePrefixes.lens, Materials.Amethyst, 1),
                    FluorBuergerit.get(OrePrefixes.lens, 1),
                    GT_OreDictUnificator.get(OrePrefixes.lens, Materials.Dilithium, 1)))
            .ignoreCollision()
            .duration(duration)
            .eut(TierEU.RECIPE_UV)
            .metadata(BASE_CHANCE, 0.0f)
            .addTo(purificationUVTreatmentRecipes);

        // Grade 7 - Degasification
        GT_Values.RA.stdBuilder()
            .fluidInputs(Materials.Grade6PurifiedWater.getFluid(1000L))
            .fluidOutputs(Materials.Grade7PurifiedWater.getFluid(900L))
            .ignoreCollision()
            .duration(duration)
            .eut(TierEU.RECIPE_UHV)
            .metadata(BASE_CHANCE, 0.0f)
            .addTo(purificationDegasifierRecipes);
    }
}