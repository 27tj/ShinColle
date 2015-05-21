package com.lulan.shincolle.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipAttackOnCollide;
import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipFlee;
import com.lulan.shincolle.ai.EntityAIShipFloating;
import com.lulan.shincolle.ai.EntityAIShipFollowOwner;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.ai.EntityAIShipWatchClosest;
import com.lulan.shincolle.entity.renderentity.BasicRenderEntity;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class EntityAirfieldHime extends BasicEntityShipLarge {
	
	public EntityAirfieldHime(World world) {
		super(world);
		this.setSize(0.8F, 1.6F);
		this.ShipType = ID.ShipType.HIME;
		this.ShipID = ID.S_AirfieldHime;
		this.ModelPos = new float[] {-6F, 15F, 0F, 40F};
		ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);	
		this.initTypeModify();
		
		launchHeight = this.height * 0.7F;
	}
	
	@Override
	public float getEyeHeight() {
		return this.height;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType() {
		return 2;
	}
	
	public void setAIList() {
		super.setAIList();
		
		//high priority
		this.tasks.addTask(1, new EntityAIShipSit(this));	   				   //0101
		this.tasks.addTask(2, new EntityAIShipFlee(this));					   //0111
		this.tasks.addTask(3, new EntityAIShipFollowOwner(this));	   		   //0111
		
		//use range attack
		this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));		   //0100
		this.tasks.addTask(12, new EntityAIShipRangeAttack(this));			   //0011
		
		//use melee attack
		if(this.getStateFlag(ID.F.UseMelee)) {
			this.tasks.addTask(13, new EntityAIShipAttackOnCollide(this, 1D, true));   //0011
			this.tasks.addTask(14, new EntityAIMoveTowardsTarget(this, 1D, 48F));  //0001
		}
		
		//idle AI
		//moving
		this.tasks.addTask(21, new EntityAIOpenDoor(this, true));			   //0000
		this.tasks.addTask(23, new EntityAIShipFloating(this));				   //0101
		this.tasks.addTask(24, new EntityAIShipWatchClosest(this, EntityPlayer.class, 6F, 0.05F)); //0010
		this.tasks.addTask(25, new EntityAIWander(this, 0.8D));				   //0001
		this.tasks.addTask(25, new EntityAILookIdle(this));					   //0011

	}
	
	//check entity state every tick
  	@Override
  	public void onLivingUpdate() {
  		//server side
  		if(!worldObj.isRemote) {
  			//������S���O
        	if(this.ticksExisted % 100 == 0) {
        		//1: �W�j�Q�ʦ^��
        		if(getStateMinor(ID.N.NumGrudge) > 0 && this.getHealth() < this.getMaxHealth()) {
        			this.setHealth(this.getHealth() + this.getMaxHealth() * 0.03F);
        		}
        		
        		//2: ���B��, �P��Y�@�ؼЦ^��, �]�A���a, �^��ؼШ̵��Ŵ��@
				if(getStateFlag(ID.F.IsMarried) && getStateFlag(ID.F.UseRingEffect) && getStateMinor(ID.N.NumGrudge) > 0) {
					//�P�wbounding box���O�_���i�H�^�媺�ؼ�
					int healCount = (int)(this.getShipLevel() / 50) + 1;
		            EntityLivingBase hitEntity = null;
		            List hitList = null;
		            hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(16D, 16D, 16D));
		           
		            for(int i = 0; i < hitList.size(); i++) {
		            	//�ɦ�W�B�S�F, break
		            	if(healCount <= 0) break;
		            	
		            	hitEntity = (EntityLivingBase) hitList.get(i);
		            	
		            	//��i�H�ɦ媺�ؼ�, ���]�t�ۤv
		            	if(hitEntity != this && hitEntity.getHealth() / hitEntity.getMaxHealth() < 0.96F) {
	            			if(hitEntity instanceof EntityPlayer) {
	            				hitEntity.heal(1F + this.getShipLevel() * 0.05F);
		            			healCount--;
		            		}
		            		else if(hitEntity instanceof BasicEntityShip) {
		            			hitEntity.heal(1F + hitEntity.getMaxHealth() * 0.05F + this.getShipLevel() * 0.1F);
		            			healCount--;
			            	}
		            	}
		            }
				}//end heal ability
        	}

  			//check every second
  			if(this.ticksExisted % 20 == 0) {
  				//summon mount if emotion state = equip00
  	  	  		if(getStateEmotion(ID.S.State) >= ID.State.EQUIP00) {
  	  	  			if(!this.isRiding()) {
  	  	  				//summon mount entity
  	  	  	  			EntityMountAfH mount = new EntityMountAfH(worldObj, this);
  	  	  	  			this.worldObj.spawnEntityInWorld(mount);
  	  	  	  			
  	  	  	  			//set riding entity
	  	  	  			this.mountEntity(mount);
  	  	  			}
  	  	  		}
  	  	  		else {
  	  	  			//cancel riding
  	  	  			if(this.isRiding() && this.ridingEntity instanceof EntityMountAfH) {
  	  	  				EntityMountAfH mount = (EntityMountAfH) this.ridingEntity;
  	  	  				
  	  	  				if(mount.seat2 != null ) {
  	  	  					mount.seat2.setRiderNull();	
  	  	  				}
  	  	  				
  	  	  				mount.setRiderNull();
  	  	  				this.ridingEntity = null;
  	  	  			}
  	  	  		}
  			}	
  		}	
  			
  		super.onLivingUpdate();
  	}
	
	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {
				//�����˳����
				if(player.isSneaking()) {
					switch(getStateEmotion(ID.S.State2)) {
					case ID.State.NORMAL_2:
						setStateEmotion(ID.S.State2, ID.State.EQUIP00_2, true);
						break;
					case ID.State.EQUIP00_2:
						setStateEmotion(ID.S.State2, ID.State.EQUIP01_2, true);
						break;
					case ID.State.EQUIP01_2:
						setStateEmotion(ID.S.State2, ID.State.EQUIP02_2, true);
						break;
					case ID.State.EQUIP02_2:
						setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
						break;
					default:
						setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
						break;
					}
				}
				//�����O�_�M���y�M
				else {
					switch(getStateEmotion(ID.S.State)) {
					case ID.State.NORMAL:
						setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
						break;
					case ID.State.EQUIP00:
						setStateEmotion(ID.S.State, ID.State.NORMAL, true);
						break;
					default:
						setStateEmotion(ID.S.State, ID.State.NORMAL, true);
						break;
					}
				}
				return true;
			}
		}
		
		return super.interact(player);
  	}
	
	@Override
	public int getKaitaiType() {
		return 1;
	}
	
	//�ק�����S�� & �ˬd�O�_riding
  	@Override
  	public boolean attackEntityWithAmmo(Entity target) {
  		//check riding
  		if(this.isRiding()) {
  			//stop attack if riding ship mount
  			if(this.ridingEntity instanceof IShipMount) {
  				return false;
  			}
  		}
  		
  		return super.attackEntityWithAmmo(target);
	}
  	
  	//�ˬd�O�_riding
  	@Override
  	public boolean attackEntityWithHeavyAmmo(Entity target) {
  		//check riding
  		if(this.isRiding()) {
  			//stop attack if riding ship mount
  			if(this.ridingEntity instanceof IShipMount) {
  				return false;
  			}
  		}
  		
  		return super.attackEntityWithHeavyAmmo(target);
  	}
	
  	//BUG: NOT WORKING
  	@Override
	public boolean canBePushed() {
        return this.ridingEntity == null;
    }


}



