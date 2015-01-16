package com.lulan.shincolle.handler;

import com.lulan.shincolle.client.gui.ContainerSmallShipyard;
import com.lulan.shincolle.client.gui.GuiSmallShipyard;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
	
	public static final int guiIDSmallShipyard = 0;
	

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity entity = world.getTileEntity(x, y, z);
		
		if(entity != null) {	//�T�w���entity�~�}ui �H�K�Q�XNPE
			switch(ID) {		//�P�wgui����
			case guiIDSmallShipyard:	//GUI small shipyard
				if (entity instanceof TileEntitySmallShipyard) {  //server���ocontainer
					return new ContainerSmallShipyard(player.inventory, (TileEntitySmallShipyard) entity);
				}
				return null;
			}
		}
		
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity entity = world.getTileEntity(x, y, z);
		
		if(entity != null) {	//�T�w���entity�~�}ui �H�K�Q�XNPE
			switch(ID) {		//�P�wgui����
			case guiIDSmallShipyard:	//GUI small shipyard
				if (entity instanceof TileEntitySmallShipyard) {  //client���ogui
					return new GuiSmallShipyard(player.inventory, (TileEntitySmallShipyard) entity);
				}
				return null;
			}
		}
		
		return null;
	}

}
