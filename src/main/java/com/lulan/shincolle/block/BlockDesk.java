package com.lulan.shincolle.block;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.tileentity.TileEntityDesk;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;

public class BlockDesk extends BasicBlockContainer {
	
	
	public BlockDesk() {
		super(Material.rock); //�����w���A �w�]�Y��rock
		this.setBlockName("BlockDesk");
	    this.setHarvestLevel("pickaxe", 0);
	    this.setHardness(1F);
	}

	//�D�зǤ�Τ��  �n��-1��ܥΦۤv��render
	@Override
	public int getRenderType() {
		return -1;	//-1 = non standard render
	}
	
	//�D�зǤ�Τ��  �]��false
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	//�D�зǤ�Τ��  �]��false
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityDesk();
	}
	
	/**�k���I�����ɩI�s����k
	 * �Ѽ�: world,���x,y,z,���a,���a���V,���a�I�쪺x,y,z
	 */	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {	//client�ݥu�ݭn����true
    		return true;
    	}
		else if(!player.isSneaking()) {  //server��: ����shift�����I�}���gui
			TileEntity entity = world.getTileEntity(x, y, z);
    		
    		if (entity != null) {	//�}�Ҥ��GUI �Ѽ�:���a,mod instance,gui ID,world,�y��xyz
    			FMLNetworkHandler.openGui(player, ShinColle.instance, ID.G.ADMIRALDESK, world, x, y, z);
    		}
    		return true;
    	}
    	else {
    		return false;
    	}
    }
	
	

}
