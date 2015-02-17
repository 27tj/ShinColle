package com.lulan.shincolle.utility;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class EntityHelper {

	public EntityHelper() {}
	
	//get entity by ID
	public static Entity getEntityByID(int entityID, World world) {
		for(Object obj: world.getLoadedEntityList()) {
			if(entityID != -1 && ((Entity)obj).getEntityId() == entityID) {
				LogHelper.info("DEBUG : found entity by ID, is client? "+entityID+" "+world.isRemote);
				return ((Entity)obj);
			}
		}
		return null;
	}
	
	//get player on the server by UUID
	public static EntityPlayerMP getOnlinePlayer(UUID id) {
		//get online id list (server side only)
		List onlineList = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
		Iterator iter = onlineList.iterator();
		while(iter.hasNext()) {
			EntityPlayerMP player = (EntityPlayerMP)iter.next();
		    if(player.getUniqueID().equals(id)) {
		    	LogHelper.info("DEBUG : found player by UUID "+player.getDisplayName());
		    	return player;
		    }
		}
		return null;
	}
	
	//process GUI click
	public static void setEntityByGUI(BasicEntityShip entity, int button, int value) {
		if(entity != null) {
			switch(button) {
			case AttrID.B_ShipInv_AmmoLight:
				LogHelper.info("DEBUG : set entity value "+button+" "+value);
				entity.setEntityFlagI(AttrID.F_UseAmmoLight, value);
				break;
			case AttrID.B_ShipInv_AmmoHeavy:
				LogHelper.info("DEBUG : set entity value "+button+" "+value);
				entity.setEntityFlagI(AttrID.F_UseAmmoHeavy, value);
				break;
			}
		}
		else {
			LogHelper.info("DEBUG : set entity by GUI fail, entity null");
		}
	}
	
	//process Shipyard GUI click
	public static void setTileEntityByGUI(TileEntity tile, int button, int value, int value2) {
		if(tile != null) {
			if(tile instanceof TileEntitySmallShipyard) {
				LogHelper.info("DEBUG : set tile entity value "+button+" "+value);
				((TileEntitySmallShipyard)tile).buildType = value;
				return;
			}
			if(tile instanceof TileMultiGrudgeHeavy) {
				LogHelper.info("DEBUG : set tile entity value "+button+" "+value+" "+value2);
				
				switch(button) {
				case AttrID.B_Shipyard_Type:		//build type
					((TileMultiGrudgeHeavy)tile).setBuildType(value);
					break;
				case AttrID.B_Shipyard_InvMode:		//select inventory mode
					((TileMultiGrudgeHeavy)tile).setInvMode(value);
					break;
				case AttrID.B_Shipyard_SelectMat:	//select material
					((TileMultiGrudgeHeavy)tile).setSelectMat(value);
					break;
				case AttrID.B_Shipyard_INCDEC:			//material inc,dec
					setLargeShipyardBuildMats((TileMultiGrudgeHeavy)tile, button, value, value2);
					break;
				}	
			}			
		}
		else {
			LogHelper.info("DEBUG : set tile entity by GUI fail, entity null");
		}	
	}

	//�]�wlarge shipyard��matBuild[], �����P�wmatStock�������নmatBuild
	private static void setLargeShipyardBuildMats(TileMultiGrudgeHeavy tile, int button, int value, int value2) {
		int num = 0;
		int type = 0;
		
		//value2�ഫ���ƶq
		switch(value2) {
		case 0:
			num = 1000;
			break;
		case 1:
			num = 100;
			break;
		case 2:
			num = 10;
			break;
		case 3:
			num = 1;
			break;
		case 4:
			num = -1000;
			break;
		case 5:
			num = -100;
			break;
		case 6:
			num = -10;
			break;
		case 7:
			num = -1;
			break;		
		}
		
		//�P�w�ƶq�O�_�����ഫ
		if(num > 0) {	//matStock -> matBuild
			//stock�ƶq�n����, �Bbuild�ƶq�����b100~1000����
			if(tile.getMatStock(value) - num >= 0 &&
			   tile.getMatBuild(value) + num < 1001) {
				//�Nstock�ಾ��build
				tile.addMatStock(value, -num);
				tile.addMatBuild(value, num);
			}
		}
		else {			//matBuild -> matStock
			//build�ƶq�n������^stock, �o�䤣����stock�W�� (�i�}�U), �o��num��"NEGATIVE"
			if(tile.getMatBuild(value) + num >= 0) {
				//�Nbuild�ಾ��stock
				tile.addMatStock(value, -num);
				tile.addMatBuild(value, num);
			}
		}	
	}
	
}
