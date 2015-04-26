package com.lulan.shincolle.handler;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.EntityBattleshipNGTBoss;
import com.lulan.shincolle.entity.EntityDestroyerShimakazeBoss;
import com.lulan.shincolle.entity.EntityMountSeat;
import com.lulan.shincolle.entity.ExtendPlayerProps;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class FML_COMMON_EventHandler {
	
	private static GameSettings keySet;
	//keys: W S A D J
	public static int rideKeys = 0;
	public static boolean openGUI = false;
	

	//player update tick, tick TWICE every tick (preTick + postTick) and BOTH SIDE (client + server)
	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent event) {
		if(event.phase == Phase.START) {
			ExtendPlayerProps extProps = (ExtendPlayerProps) event.player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
//LogHelper.info("DEBUG : player ride "+event.player.worldObj.isRemote+" "+event.player.ridingEntity); 
			//spawn boss in ocean biome, server side
			if(extProps != null && !event.player.worldObj.isRemote) {
				int blockX = (int) event.player.posX;
				int blockZ = (int) event.player.posZ;
				int spawnX, spawnY, spawnZ = 0;
				BiomeGenBase biome = event.player.worldObj.getBiomeGenForCoords(blockX, blockZ);	
				
				//cooldown--
				if(BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.OCEAN) && extProps.hasRing()) {
					extProps.setBossCooldown(extProps.getBossCooldown() - 1);
				}
				
				//cooldown = 0, roll spawn
				if(extProps.getBossCooldown() <= 0) {
					extProps.setBossCooldown(4800);
					
					int rolli = event.player.getRNG().nextInt(4);
					LogHelper.info("DEBUG : spawn boss: roll spawn "+rolli);
					if(rolli == 0) {
						//�M��10���a�I, ���@�ӥi�ͦ��a�I�Y�ͦ�����Xloop
						for(int i = 0; i < 10; i++) {
							int offX = event.player.getRNG().nextInt(32) + 32;
							int offZ = event.player.getRNG().nextInt(32) + 32;
							
							switch(event.player.getRNG().nextInt(4)) {
							case 0:
								spawnX = blockX + offX;
								spawnZ = blockZ + offZ;
								break;
							case 1:
								spawnX = blockX - offX;
								spawnZ = blockZ - offZ;
								break;
							case 2:
								spawnX = blockX + offX;
								spawnZ = blockZ - offZ;
								break;
							case 3:
								spawnX = blockX - offX;
								spawnZ = blockZ + offZ;
								break;
							default:
								spawnX = blockX + offX;
								spawnZ = blockZ + offZ;
								break;		
							}

							spawnY = 64 - event.player.getRNG().nextInt(4);
							
							Block blockY = event.player.worldObj.getBlock(spawnX, spawnY, spawnZ);
							
							LogHelper.info("DEBUG : spawn boss: get block "+blockY.getLocalizedName()+" "+spawnX+" "+spawnY+" "+spawnZ);
							//�ͦ��b����
							if(blockY == Blocks.water) {
								//check 64x64 range
								AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(spawnX-64D, spawnY-64D, spawnZ-64D, spawnX+64D, spawnY+64D, spawnZ+64D);
								List listBoss = event.player.worldObj.getEntitiesWithinAABB(BasicEntityShipHostile.class, aabb);

								LogHelper.info("DEBUG : spawn boss: check existed boss "+listBoss.size());
								
								//list�C��1�Ӫ�ܨS������Lboss
					            if(listBoss.size() < 1) {
					            	ServerProxy.getServer().getConfigurationManager().sendChatMsg(
					            			new ChatComponentText(
					            					EnumChatFormatting.YELLOW+
					            					StatCollector.translateToLocal("chat.shincolle:bossshimakaze")+
					            					EnumChatFormatting.AQUA+
					            					" "+spawnX+" "+spawnY+" "+spawnZ));
					            	LogHelper.info("DEBUG : spawn boss: Shimakaze "+" "+spawnX+" "+spawnY+" "+spawnZ);
									EntityDestroyerShimakazeBoss boss = new EntityDestroyerShimakazeBoss(event.player.worldObj);
									boss.setPosition(spawnX, spawnY, spawnZ);
									event.player.worldObj.spawnEntityInWorld(boss);
									
									//spawn boss: Nagato (33%)
									if(event.player.getRNG().nextInt(3) == 0) {
										ServerProxy.getServer().getConfigurationManager().sendChatMsg(
						            			new ChatComponentText(
						            					EnumChatFormatting.RED+
						            					StatCollector.translateToLocal("chat.shincolle:bossnagato")+
						            					EnumChatFormatting.AQUA+
						            					" "+spawnX+" "+spawnY+" "+spawnZ));
						            	LogHelper.info("DEBUG : spawn boss: Nagato "+" "+spawnX+" "+spawnY+" "+spawnZ);
										EntityBattleshipNGTBoss boss2 = new EntityBattleshipNGTBoss(event.player.worldObj);
										boss2.setPosition(spawnX, spawnY, spawnZ);
										event.player.worldObj.spawnEntityInWorld(boss2);
									}
									
									break;
					            }	
							}
						}
					}//end roll spawn boss
				}//end boss cooldown <= 0	
			}//end server side, extProps != null
			
			//check ring item (check for first found ring only) every 20 ticks
			if(event.player.ticksExisted % 20 == 0) {
				boolean hasRing = false;
				ItemStack itemRing = null;
				
				for(int i = 0; i < 36; ++i) {
					if(event.player.inventory.getStackInSlot(i) != null && 
					   event.player.inventory.getStackInSlot(i).getItem() == ModItems.MarriageRing) {
						hasRing = true;
						itemRing = event.player.inventory.getStackInSlot(i);
						break;
					}
				}
				
				if(extProps != null) {
					//�쥻��ring, �ܦ��S��, �h�������檬�A
					if(extProps.hasRing() && !hasRing) {
						event.player.capabilities.isFlying = false;
					}
					
					//update hasRing flag
					extProps.setHasRing(hasRing);
					
					if(itemRing != null) {
						if(itemRing.hasTagCompound() && itemRing.getTagCompound().getBoolean("isActive")) {
							extProps.setRingActive(true);
						}
					}
				}
			}//end player per 20 ticks
		}//end player tick phase: START
	}//end onPlayerTick
	
	//restore player extProps data, this is SERVER side
	@SubscribeEvent
	public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		LogHelper.info("DEBUG : get player respawn event "+event.player.getDisplayName()+" "+event.player.getUniqueID());
    	
        //restore player data from commonproxy variable
        NBTTagCompound nbt = CommonProxy.getEntityData(event.player.getUniqueID().toString());
        
        if(nbt != null) {
        	LogHelper.info("DEBUG : player respawn: restore player data (FML COMMON event) "+event.player.worldObj.isRemote);
        	ExtendPlayerProps extProps = (ExtendPlayerProps) event.player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
        	
        	extProps.loadNBTData(nbt);
        	
        	//sync extProps state to client
			CommonProxy.channelG.sendTo(new S2CGUIPackets(extProps), (EntityPlayerMP) event.player);
        }
	}//end onPlayerRespawn
	
	//get key input, ���U+��}���|�o�X�@��, �B�C�ӫ�����}�o�X, CLIENT side only event
	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		//getIsKeyPressed = �ӫ���O�_����, isPressed = �o��event�O�_���ӫ���
//		LogHelper.info("DEBUG : key event "+Minecraft.getMinecraft().gameSettings.keyBindBack.getIsKeyPressed());
//		LogHelper.info("DEBUG : key event "+Minecraft.getMinecraft().thePlayer.ridingEntity);
		//if player is riding, send packet
		EntityPlayer player = ClientProxy.getClientPlayer();
		
		if(player.isRiding() && player.ridingEntity instanceof EntityMountSeat) {
			this.keySet = ClientProxy.getGameSetting();
			int newKeys = 0;
			
			//forward
			if(keySet.keyBindForward.getIsKeyPressed()) {
				LogHelper.info("DEBUG : key event: press W");
				newKeys = newKeys | 1;
			}
			
			//back
			if(keySet.keyBindBack.getIsKeyPressed()) {
				LogHelper.info("DEBUG : key event: press S");
				newKeys = newKeys | 2;
			}
			
			//left
			if(keySet.keyBindLeft.getIsKeyPressed()) {
				LogHelper.info("DEBUG : key event: press A");
				newKeys = newKeys | 4;
			}
			
			//right
			if(keySet.keyBindRight.getIsKeyPressed()) {
				LogHelper.info("DEBUG : key event: press D");
				newKeys = newKeys | 8;
			}
			
			//jump
			if(keySet.keyBindJump.isPressed()) {
				LogHelper.info("DEBUG : key event: jump");
				newKeys = newKeys | 16;
			}
			
			//server��client�P�ɳ]�w, ������ܤ~�|���Z, �u�aserver�]�w���ʷ|���s��
			BasicEntityMount mount = ((EntityMountSeat)player.ridingEntity).host;

			if(mount != null) {
				//set key for packet
				this.rideKeys = newKeys;
				
				//set client key
				((EntityMountSeat) player.ridingEntity).host.keyPressed = newKeys;

				//inventory, open ship GUI
				if(keySet.keyBindInventory.isPressed()) {
					LogHelper.info("DEBUG : key event: open ship GUI");
					this.openGUI = true;
				}
				else {	//��}����ɷ|�]�즹�ﶵ, �]�^false
					this.openGUI = false;
				}
				
				//send control packet
				CommonProxy.channelG.sendToServer(new C2SInputPackets(0));
			}
		}//end is riding
	}//end key event

	//player login, called after extProps loaded
	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		LogHelper.info("DEBUG : get player login event "+event.player.getDisplayName()+" "+event.player.getUniqueID());
		ExtendPlayerProps extProps = (ExtendPlayerProps) event.player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
		
		if(extProps != null) {
			LogHelper.info("DEBUG : player login: save player extProps in CommonProxy");
			
			//save player nbt data to common proxy
    		NBTTagCompound nbt = new NBTTagCompound();
    		extProps.saveNBTData(nbt);
    		CommonProxy.storeEntityData(event.player.getUniqueID().toString(), nbt);
    		
    		//sync extProps state to client
			CommonProxy.channelG.sendTo(new S2CGUIPackets(extProps), (EntityPlayerMP) event.player);
		}
	}
	
	//player loggout, not be called in singleplayer 
	@SubscribeEvent
	public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		ExtendPlayerProps extProps = (ExtendPlayerProps) event.player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
    	
    	LogHelper.info("DEBUG : get player logout event "+event.player.getDisplayName()+" "+event.player.getUniqueID());
    	
    	if(extProps != null) {
    		LogHelper.info("DEBUG : player logout: save player extProps in CommonProxy");
    		//save player nbt data
    		NBTTagCompound nbt = new NBTTagCompound();
    		extProps.saveNBTData(nbt);
    		CommonProxy.storeEntityData(event.player.getUniqueID().toString(), nbt);
    	}
	}

}