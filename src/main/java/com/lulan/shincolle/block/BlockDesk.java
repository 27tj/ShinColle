package com.lulan.shincolle.block;

import net.minecraft.block.material.Material;

public class BlockDesk extends BasicBlock {
	public BlockDesk() {
		super(Material.wood); //�����w���A �w�]�Y��rock
		this.setBlockName("BlockDesk");
		this.setHardness(1.0F);
	    this.setHarvestLevel("axe", 2);
		
	}

}
