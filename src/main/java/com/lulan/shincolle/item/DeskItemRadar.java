package com.lulan.shincolle.item;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.reference.ID;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class DeskItemRadar extends BasicItem {
	
	public DeskItemRadar() {
		super();
		this.setUnlocalizedName("DeskItemRadar");
		this.maxStackSize = 1;
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player) {
		if(player != null) {  //�}�Ҥ��GUI �Ѽ�:���a, mod instance, gui ID, world, �ۭq�Ѽ�1,2,3
			FMLNetworkHandler.openGui(player, ShinColle.instance, ID.G.ADMIRALDESK, world, 1, 0, 0);
		}
		return itemstack;
	}


}

