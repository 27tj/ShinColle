package com.lulan.shincolle.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.lulan.shincolle.client.gui.GuiLargeShipyard;
import com.lulan.shincolle.client.gui.GuiShipInventory;
import com.lulan.shincolle.client.gui.GuiSmallShipyard;
import com.lulan.shincolle.client.inventory.ContainerLargeShipyard;
import com.lulan.shincolle.client.inventory.ContainerShipInventory;
import com.lulan.shincolle.client.inventory.ContainerSmallShipyard;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

	@Override
	public Object getServerGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity tile;
		Entity entity;
		
		switch(guiId) {		//�P�wgui����
		case ID.G.SMALLSHIPYARD:	//GUI small shipyard
			tile = world.getTileEntity(x, y, z);  //�T�w���entity�~�}ui �H�K�Q�XNPE
			if((tile != null) && (tile instanceof TileEntitySmallShipyard)) {  //server���ocontainer
				((TileEntitySmallShipyard)tile).sendSyncPacket(); //sync once when gui opened
				return new ContainerSmallShipyard(player.inventory, (TileEntitySmallShipyard) tile);
			}
			return null;
		case ID.G.SHIPINVENTORY:	//GUI ship inventory
			entity = world.getEntityByID(x);	//entity id�s�bx�y�аѼƤW
            if((entity != null) && (entity instanceof BasicEntityShip)){
            	((BasicEntityShip)entity).sendSyncPacket(); //sync once when gui opened
				return new ContainerShipInventory(player.inventory,(BasicEntityShip)entity);
			}
			return null;
		case ID.G.LARGESHIPYARD:	//GUI large shipyard
			tile = world.getTileEntity(x, y, z);  //�T�w���entity�~�}ui �H�K�Q�XNPE
			if((tile != null && tile instanceof TileMultiGrudgeHeavy)) {  //server���ocontainer
				((TileMultiGrudgeHeavy)tile).sendSyncPacket(); //sync once when gui opened
				return new ContainerLargeShipyard(player.inventory, (TileMultiGrudgeHeavy) tile);
			}
			return null;
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity tile;
		Entity entity;
		
		switch(guiId) {		//�P�wgui����
		case ID.G.SMALLSHIPYARD:	//GUI small shipyard
			tile = world.getTileEntity(x, y, z);  //�T�w���entity�~�}ui �H�K�Q�XNPE
			if ((tile!=null) && (tile instanceof TileEntitySmallShipyard)) {  //client���ogui
				return new GuiSmallShipyard(player.inventory, (TileEntitySmallShipyard) tile);
			}
			return null;
		case ID.G.SHIPINVENTORY:	//GUI ship inventory
			entity = world.getEntityByID(x);	//entity id�s�bx�y�аѼƤW
            if((entity!=null) && (entity instanceof BasicEntityShip)){
				return new GuiShipInventory(player.inventory,(BasicEntityShip)entity);
			}
			return null;
		case ID.G.LARGESHIPYARD:	//GUI large shipyard
			tile = world.getTileEntity(x, y, z);  //�T�w���entity�~�}ui �H�K�Q�XNPE
			if((tile != null && tile instanceof TileMultiGrudgeHeavy)) {  //server���ocontainer
				return new GuiLargeShipyard(player.inventory, (TileMultiGrudgeHeavy) tile);
			}
			return null;
		}
	
		return null;
	}

}
