package com.lulan.shincolle.init;

import net.minecraft.block.Block;

import com.lulan.shincolle.block.*;
import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.registry.GameRegistry;

@GameRegistry.ObjectHolder(Reference.MOD_ID)	//�n��object holder��mod������e���y�q ��L�H�i�H����Ū���Ӫ���
public class ModBlocks {

	public static final Block BlockAbyssium = new BlockAbyssium();
	public static final Block BlockDesk = new BlockDesk();
	public static final Block BlockPolymetalOre = new BlockPolymetalOre();
	public static final Block BlockSmallShipyard = new BlockSmallShipyard();
	public static final Block BlockGrudge = new BlockGrudge();
	public static final Block BlockPolymetal = new BlockPolymetal();
	
	public static void init() {
		GameRegistry.registerBlock(BlockAbyssium, "BlockAbyssium");
		GameRegistry.registerBlock(BlockDesk, "BlockDesk");
		GameRegistry.registerBlock(BlockGrudge, "BlockGrudge");
		GameRegistry.registerBlock(BlockPolymetal, "BlockPolymetal");
		GameRegistry.registerBlock(BlockPolymetalOre, "BlockPolymetalOre");
		GameRegistry.registerBlock(BlockSmallShipyard, "BlockSmallShipyard");
		
	}
}
