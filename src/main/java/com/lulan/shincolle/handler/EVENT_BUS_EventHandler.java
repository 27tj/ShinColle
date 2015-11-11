package com.lulan.shincolle.handler;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.ExtendPlayerProps;
import com.lulan.shincolle.entity.ExtendShipProps;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.hostile.EntityRensouhouBoss;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.BasicEntityItem;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**for EVENT_BUS event only <br>
 * (not for FML event)
 */
public class EVENT_BUS_EventHandler {

	//change vanilla mob drop (add grudge), this is SERVER event
	@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=true)
	public void onDrop(LivingDropsEvent event) {
	    //mob drop grudge
		if(event.entity instanceof EntityMob || event.entity instanceof EntitySlime) {
	    	if(event.entity instanceof BasicEntityShipHostile) {
	    		BasicEntityShipHostile entity = (BasicEntityShipHostile)event.entity;
	    		
	    		if(entity.canDrop) {
	    			//set drop flag to false
	    			entity.canDrop = false;
	    			
	    			ItemStack bossEgg = ((BasicEntityShipHostile)event.entity).getDropEgg();
	    			
	    			if(bossEgg != null) {
	    				BasicEntityItem entityItem1 = new BasicEntityItem(event.entity.worldObj, event.entity.posX, event.entity.posY+0.5D, event.entity.posZ, bossEgg);
			    		LogHelper.info("DEBUG : ship mob drop "+entityItem1.posX+" "+entityItem1.posY+" "+entityItem1.posZ);
			    		event.entity.worldObj.spawnEntityInWorld(entityItem1);
	    			}
	    		}	
	    	}
	    	
	    	//drop grudge
	    	if(!(event.entity instanceof EntityRensouhouBoss)) {
	    		//if config has drop rate setting
	    		int numGrudge = (int) ConfigHandler.dropGrudge;
//	    		LogHelper.info("DEBUG : drop grudge "+numGrudge+" "+ConfigHandler.dropGrudge);
	    		//�Y�]�w�W�L1, �h�����h�� (ex: 5.5 = 5��)
	    		if(numGrudge > 0) {
	    			ItemStack drop = new ItemStack(ModItems.Grudge, numGrudge);
			        event.drops.add(new EntityItem(event.entity.worldObj, event.entity.posX, event.entity.posY, event.entity.posZ, drop));
	    		}
	    		//�Ȥ���1, ���v����1��
	    		else {
	    			if(event.entity.worldObj.rand.nextFloat() <= ConfigHandler.dropGrudge) {
	    				ItemStack drop = new ItemStack(ModItems.Grudge, 1);
				        event.drops.add(new EntityItem(event.entity.worldObj, event.entity.posX, event.entity.posY, event.entity.posZ, drop));
	    			}
	    		}
	    		
	    		//�Ѿl����1����, �אּ���v����
	    		if(event.entity.worldObj.rand.nextFloat() < (ConfigHandler.dropGrudge - numGrudge)) {
    				ItemStack drop = new ItemStack(ModItems.Grudge, 1);
			        event.drops.add(new EntityItem(event.entity.worldObj, event.entity.posX, event.entity.posY, event.entity.posZ, drop));
    			}
	    	}
	    }
	    
	    //ship drop egg, if canDrop is true, drop spawn egg and set canDrop to false
	    if(event.entity instanceof BasicEntityShip) {
	    	BasicEntityShip entity = (BasicEntityShip)event.entity;
	    	
	    	if(entity.getStateFlag(ID.F.CanDrop)) {
	    		//set flag to false to prevent multiple drop from unknown bug
	    		entity.setStateFlag(ID.F.CanDrop, false);
	    		
	    		//drop ship item
	    		ItemStack item = new ItemStack(ModItems.ShipSpawnEgg, 1, entity.getShipClass()+2);
		    	BasicEntityItem entityItem2 = new BasicEntityItem(event.entity.worldObj, event.entity.posX, event.entity.posY+0.5D, event.entity.posZ, item);
		    	NBTTagCompound nbt = new NBTTagCompound();
		    	ExtendShipProps extProps = entity.getExtProps();
		    	
		    	//get inventory data
				NBTTagList list = new NBTTagList();
				for(int i = 0; i < extProps.slots.length; i++) {
					if(extProps.slots[i] != null) {
						NBTTagCompound item2 = new NBTTagCompound();
						item2.setByte("Slot", (byte)i);
						extProps.slots[i].writeToNBT(item2);
						list.appendTag(item2);
					}
				}
				
				//get attributes data
		    	int[] attrs = new int[8];
		    	
		    	if(entity.getLevel() > 1) attrs[0] = entity.getLevel() - 1;	//decrease level 1
		    	else attrs[0] = 1;
		    	
		    	attrs[1] = entity.getBonusPoint(ID.HP);
		    	attrs[2] = entity.getBonusPoint(ID.ATK);
		    	attrs[3] = entity.getBonusPoint(ID.DEF);
		    	attrs[4] = entity.getBonusPoint(ID.SPD);
		    	attrs[5] = entity.getBonusPoint(ID.MOV);
		    	attrs[6] = entity.getBonusPoint(ID.HIT);
		    	attrs[7] = entity.getStateFlagI(ID.F.IsMarried);
		    	
		    	/** OWNER SETTING
		    	 *  1. check player UID first (after rv.22)
		    	 *  2. if (1) fail, check player UUID string (before rv.22)
		    	 */
		    	
		    	/** set owner info by player's UUID (before rv.22) */
		    	String ownerUUID = EntityHelper.getPetPlayerUUID(entity);
		    	nbt.setString("owner", ownerUUID);
		    	
		    	/** set owner info by player's UID (after rv.22) */
		    	//save nbt and spawn entity item
		    	EntityPlayer owner = EntityHelper.getEntityPlayerByUID(entity.getStateMinor(ID.M.PlayerUID), entity.worldObj);
		    	
		    	if(owner != null) {
		    		nbt.setString("ownername", owner.getDisplayName());
		    	}
		    	
		    	nbt.setTag("ShipInv", list);		//save inventory data to nbt
		    	nbt.setIntArray("Attrs", attrs);	//save attributes data to nbt
		    	nbt.setInteger("PlayerID", entity.getStateMinor(ID.M.PlayerUID));
		    	nbt.setInteger("ShipID", entity.getStateMinor(ID.M.ShipUID));
		    	nbt.setString("customname", entity.getCustomNameTag());
		    	
		    	entityItem2.getEntityItem().setTagCompound(nbt);	  //save nbt to entity item
		    	event.entity.worldObj.spawnEntityInWorld(entityItem2);	//spawn entity item
	    	}
	    }
	}
	
	//apply ring effect: boost dig speed in water
	@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=true)
	public void onGetBreakSpeed(PlayerEvent.BreakSpeed event) {
		ExtendPlayerProps extProps = (ExtendPlayerProps) event.entityPlayer.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
		
		if(extProps != null && (event.entityPlayer.isInWater() || event.entityPlayer.handleLavaMovement())) {
			int digBoost = extProps.getDigSpeedBoost() + 1;
			//boost speed
			event.newSpeed = event.originalSpeed * digBoost;
		}		
	}
	
	//apply ring effect: vision in the water
	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=true)
	public void onSetLiquidFog(EntityViewRenderEvent.FogDensity event) {
		if(event.entity.isInsideOfMaterial(Material.lava) ||
		   event.entity.isInsideOfMaterial(Material.water)){
			
			ExtendPlayerProps extProps = (ExtendPlayerProps) event.entity.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
			
			if(extProps != null && extProps.isRingActive()) {
				float fogDen = 0.1F - extProps.getMarriageNum() * 0.02F;
				
				if(fogDen < 0.01F) fogDen = 0.001F;
				
				event.setCanceled(true);	//�����쥻��fog render
	            event.density = fogDen;		//���]fog�@��
	            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
			}   
            
//			float fogStart = 0F;	//for linear fog
//			float fogEnd = 20F;		//for linear fog
//            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
//            GL11.glFogf(GL11.GL_FOG_START, fogStart);	//fog start distance, only for linear fog
//            GL11.glFogf(GL11.GL_FOG_END, fogEnd);		//fog end distance, only for linear fog
		}
	}
	
	/** entity death event */
	@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=true)
	public void onEntityDeath(LivingDeathEvent event) {
		Entity ent = event.source.getSourceOfDamage();
		
		//add kills number
	    if(ent != null) {
	    	if(ent instanceof BasicEntityShip) {	//��������
	    		((BasicEntityShip)ent).addKills();
	    	}
	    	else if(ent instanceof IShipAttackBase) {	//��L�l�ꪫ����
	    		if(((IShipAttackBase) ent).getHostEntity() != null &&
	    		   ((IShipAttackBase) ent).getHostEntity() instanceof BasicEntityShip) {
	    			((BasicEntityShip)((IShipAttackBase) ent).getHostEntity()).addKills();
	    		}
	    	}
	    }
	    
	    //save player ext data
	    if(event.entityLiving instanceof EntityPlayer) {
	    	EntityPlayer player = (EntityPlayer) event.entityLiving;
	    	ExtendPlayerProps extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
	    	
	    	LogHelper.info("DEBUG : player death: save player data: "+player.getDisplayName()+" "+player.getUniqueID());
	    	
	    	if(extProps != null) {
	    		LogHelper.info("DEBUG : player death: get player extProps");
	    		//save player nbt data
	    		NBTTagCompound nbt = new NBTTagCompound();
	    		extProps.saveNBTData(nbt);
	    		
	    		//save nbt to commonproxy variable
	    		ServerProxy.setPlayerData(player.getUniqueID().toString(), nbt);
	    	}
	    }
	}
	
	//add extend props to entity
	@SubscribeEvent
	public void onEntityConstructing(EntityConstructing event) {
	    //ship ext props
		if(event.entity instanceof BasicEntityShip && event.entity.getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME) == null) {
	    	LogHelper.info("DEBUG : entity constructing: on ship constructing "+event.entity.getEntityId());
	        event.entity.registerExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME, new ExtendShipProps());
		}

		//player ext props
		if(event.entity instanceof EntityPlayer && event.entity.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME) == null) {
			LogHelper.info("DEBUG : entity constructing: on player constructing "+event.entity.getEntityId()+" "+event.entity.getClass().getSimpleName());
			EntityPlayer player = (EntityPlayer) event.entity;
			player.registerExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME, new ExtendPlayerProps());
		}
	}
	
	/**Add ship mob spawn in squid event
	 * FIX: add spawn limit (1 spawn within 32 blocks)
	 */
	@SubscribeEvent
	public void onSquidSpawn(LivingSpawnEvent.CheckSpawn event) {
		if(event.entityLiving instanceof EntitySquid) {
			if(event.world.rand.nextInt((int)ConfigHandler.scaleMobU511[6]) == 0) {
				//check 64x64 range
				AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(event.x-32D, event.y-32D, event.z-32D, event.x+32D, event.y+32D, event.z+32D);
				List ListMob = event.world.getEntitiesWithinAABB(BasicEntityShipHostile.class, aabb);

				//list�C��1�Ӫ�ܨS������Lboss
	            if(ListMob.size() < 1) {
	            	LogHelper.info("DEBUG : spawn ship mob at "+event.x+" "+event.y+" "+event.z+" rate "+ConfigHandler.scaleMobU511[6]);
	            	EntityLiving entityToSpawn;
	            	//50%:U511 50%:Ro500
	            	if(event.world.rand.nextInt(2) == 0) {
	            		entityToSpawn = (EntityLiving) EntityList.createEntityByName("shincolle.EntitySubmU511Mob", event.world);
					}
	            	else {
	            		entityToSpawn = (EntityLiving) EntityList.createEntityByName("shincolle.EntitySubmRo500Mob", event.world);
	            	}
	            	
					entityToSpawn.posX = event.x;
					entityToSpawn.posY = event.y;
					entityToSpawn.posZ = event.z;
					entityToSpawn.setPosition(event.x, event.y, event.z);
					event.world.spawnEntityInWorld(entityToSpawn);
	            }	
			}
		}
	}
	
	/**world load event
	 * init MapStorage here
	 * �ѩ�global mapstorage���ަbworld���OŪ���P�@��handler, �ҥH���ˬdworld id, �H�K�@��world�ҥi
	 * �Y��perMapStorage, �h�O���Pworld�U���@��
	 */
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		LogHelper.info("DEBUG : on world load: "+event.world.provider.dimensionId);
		
		if(event.world != null && !ServerProxy.initServerFile) {
			ServerProxy.initServerProxy(event.world);
		}
	}
	
//	/**world unload event
//	 * save ship team list here, for SINGLEPLAYER ONLY
//	 * for multiplayer: PlayerLoggedOutEvent
//	 * 
//	 * �ѩ󤣩���], logout event�u�|�b�h�H�C���U�o�X, ����C�������ϥΦ�world unload event
//	 */
//	@SubscribeEvent
//	public void onWorldUnload(WorldEvent.Unload event) {
//		LogHelper.info("DEBUG : on world unload: "+event.world.provider.dimensionId);
//	}

	
}
