package com.lulan.shincolle.init;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.registry.GameRegistry;

@GameRegistry.ObjectHolder(Reference.MOD_ID)	//�n��object holder��mod������e���y�q ��L�H�i�H����Ū���Ӫ���
public class ModOres {

	//ore
	public static ItemStack PolymetalDust = new ItemStack(ModItems.AbyssMetal, 1, 1);
	public static ItemStack PolymetalOre = new ItemStack(ModBlocks.BlockPolymetalOre, 1, 0);


	//�n��item��C���� (�bpre init���q�n��)
	public static void oreDictRegister() {
		//polymetal = manganese ore
		OreDictionary.registerOre("dustManganese", PolymetalDust);
		OreDictionary.registerOre("oreManganese", ModBlocks.BlockPolymetalOre);
	}
}
