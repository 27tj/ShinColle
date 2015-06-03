package com.lulan.shincolle.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.reference.ID;

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
		
		//use range attack
		this.tasks.addTask(11, new EntityAIShipCarrierAttack(this));		   //0100
		this.tasks.addTask(12, new EntityAIShipRangeAttack(this));			   //0011
	}
	
	//check entity state every tick
  	@Override
  	public void onLivingUpdate() {
  		//server side
  		if(!worldObj.isRemote) {
  			//飛行場特殊能力
        	if(this.ticksExisted % 100 == 0) {
        		//1: 增強被動回血
        		if(getStateMinor(ID.N.NumGrudge) > 0 && this.getHealth() < this.getMaxHealth()) {
        			this.setHealth(this.getHealth() + this.getMaxHealth() * 0.03F);
        		}
        		
        		//2: 結婚後, 周圍某一目標回血, 包括玩家, 回血目標依等級提昇
				if(getStateFlag(ID.F.IsMarried) && getStateFlag(ID.F.UseRingEffect) && getStateMinor(ID.N.NumGrudge) > 0) {
					//判定bounding box內是否有可以回血的目標
					int healCount = (int)(this.getLevel() / 50) + 1;
		            EntityLivingBase hitEntity = null;
		            List hitList = null;
		            hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(12D, 12D, 12D));
		           
		            for(int i = 0; i < hitList.size(); i++) {
		            	//補血名額沒了, break
		            	if(healCount <= 0) break;
		            	
		            	hitEntity = (EntityLivingBase) hitList.get(i);
		            	
		            	//抓可以補血的目標, 不包含自己
		            	if(hitEntity != this && hitEntity.getHealth() / hitEntity.getMaxHealth() < 0.96F) {
	            			if(hitEntity instanceof EntityPlayer) {
	            				hitEntity.heal(1F + this.getLevel() * 0.05F);
		            			healCount--;
		            		}
		            		else if(hitEntity instanceof BasicEntityShip) {
		            			hitEntity.heal(1F + hitEntity.getMaxHealth() * 0.05F + this.getLevel() * 0.1F);
		            			healCount--;
			            	}
		            	}
		            }
				}//end heal ability
        	}
  		}//end server side
  			
  		super.onLivingUpdate();
  	}
	
	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {
				//切換裝備顯示
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
				//切換是否騎乘座騎
				else {
					switch(getStateEmotion(ID.S.State)) {
					case ID.State.NORMAL:
						setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
						break;
					case ID.State.EQUIP00:
						setStateEmotion(ID.S.State, ID.State.NORMAL, true);
						this.setPositionAndUpdate(posX, posY + 2D, posZ);
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
	
	//修改煙霧特效 & 檢查是否riding
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
  	
  	//檢查是否riding
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
  	
  	//true if use mounts
  	@Override
  	public boolean canSummonMounts() {
  		return true;
  	}
  	
  	@Override
  	public BasicEntityMount summonMountEntity() {
		return new EntityMountAfH(worldObj, this);
	}
  	
  	@Override
  	public float[] getModelPos() {
  		if(this.isRiding()) {
  			ModelPos[1] = -25F;
  		}
  		else {
  			ModelPos[1] = 15F;
  		}
  		
		return ModelPos;
	}


}



