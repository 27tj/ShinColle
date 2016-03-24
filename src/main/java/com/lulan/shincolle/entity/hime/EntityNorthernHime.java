package com.lulan.shincolle.entity.hime;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipLarge;
import com.lulan.shincolle.entity.ExtendShipProps;
import com.lulan.shincolle.entity.other.EntityFloatingFort;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class EntityNorthernHime extends BasicEntityShipLarge {
	
	private int goRidingTicks;		//�M���ؼдM��ɶ�
	private boolean goRiding;		//�O�_�n��ؼ��M��
	private Entity goRideEntity;	//�M���ؼ�
	
	public EntityNorthernHime(World world) {
		super(world);
		this.setSize(0.6F, 1.1F);
		this.setStateMinor(ID.M.ShipType, ID.ShipType.HIME);
		this.setStateMinor(ID.M.ShipClass, ID.Ship.NorthernHime);
		this.setStateMinor(ID.M.DamageType, ID.ShipDmgType.AVIATION);
		this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[ID.ShipConsume.BBV]);
		this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[ID.ShipConsume.BBV]);
		this.ModelPos = new float[] {-6F, 8F, 0F, 50F};
		ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);	
		this.initTypeModify();
		
		goRidingTicks = 0;
		goRideEntity = null;
		goRiding = false;
		launchHeight = this.height;
		
		//set attack type
		this.StateFlag[ID.F.HaveRingEffect] = true;
	}
	
	@Override
	public float getEyeHeight() {
		return 1.0F;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType() {
		return 2;
	}
	
	@Override
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
  			//every 80 ticks
        	if(this.ticksExisted % 64 == 0) {
        		//1: �W�j�Q�ʦ^��
        		if(getStateMinor(ID.M.NumGrudge) > 0 && this.getHealth() < this.getMaxHealth()) {
        			this.setHealth(this.getHealth() + this.getMaxHealth() * 0.015625F);
        		}
        		
        		//2: ���B��, �P��Y�@�ؼЦ^��, �]�A���a, �^��ؼШ̵��Ŵ��@
				if(getStateFlag(ID.F.IsMarried) && getStateFlag(ID.F.UseRingEffect) && getStateMinor(ID.M.NumGrudge) > 0) {
					//�P�wbounding box���O�_���i�H�^�媺�ؼ�
					int healCount = this.getLevel() / 25 + 1;
		            EntityLivingBase hitEntity = null;
		            List hitList = null;
		            hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(8D, 8D, 8D));
		           
		            for(int i = 0; i < hitList.size(); i++) {
		            	//�ɦ�W�B�S�F, break
		            	if(healCount <= 0) break;
		            	
		            	hitEntity = (EntityLivingBase) hitList.get(i);
		            	
		            	//��i�H�ɦ媺�ؼ�, ���]�t�ۤv
		            	if(hitEntity != this && hitEntity.getHealth() / hitEntity.getMaxHealth() < 0.98F) {
	            			if(hitEntity instanceof EntityPlayer) {
	            				hitEntity.heal(1F + this.getLevel() * 0.02F);
		            			healCount--;
		            		}
		            		else if(hitEntity instanceof BasicEntityShip && EntityHelper.checkIsAlly(this, hitEntity)) {
		            			hitEntity.heal(1F + hitEntity.getMaxHealth() * 0.02F + this.getLevel() * 0.1F);
		            			healCount--;
			            	}
		            	}
		            }
				}//end heal ability
        	}//end 80 ticks
        	
        	//every 256 ticks
        	if(this.ticksExisted % 25 == 0) {
        		int roll = this.rand.nextInt(5);
//        		LogHelper.info("DEBUG : hoppo go riding "+roll);
        		//�C�@�q�ɶ��ˬd�O�_�n�M����Lentity
        		if(roll == 0) {
        			this.checkRiding();
        		}
        	}
        	
        	//�Y�n���M���ؼ�
        	if(this.goRiding) {
        		this.goRidingTicks++;
        		
        		//��Ӥ[, ����M���ؼ�
        		if(this.goRidingTicks > 200) {
        			this.cancelGoRiding();
        		}
        		
        		float distRiding = 0F;
        		if(goRideEntity != null) {
        			distRiding = this.getDistanceToEntity(this.goRideEntity);
        		}
        		
        		//�C32 tick��@�����|
        		if(this.ticksExisted % 32 == 0) {
        			if(distRiding > 2F) {
        				this.getShipNavigate().tryMoveToEntityLiving(this.goRideEntity, 1D);
        			}
        		}
        		
        		//�Z��2�椺�h�M���ؼ�
        		if(distRiding <= 2F) {
        			if(goRideEntity != null && !goRideEntity.isRiding() && this.riddenByEntity == null) {
        				this.mountEntity(goRideEntity);
        				this.getShipNavigate().clearPathEntity();
        				this.cancelGoRiding();
        			}
        		}
        	}
        	
        	//�M����
        	if(this.isRiding()) { 	
        		//�Y�M���ؼ�sneaking, �h�������{�ؼ�
    			if(this.ridingEntity.isSneaking()) {
        			this.mountEntity(null);
        		}
        	}
         	
  		}//end server side
  		//client side
  		else {
  			//drip water effect
  			if(this.ticksExisted % 10 == 0) {
  				if(getStateEmotion(ID.S.State2) == ID.State.EQUIP01_2) {
  					if(this.isSitting() || this.isRiding()) {
  						ParticleHelper.spawnAttackParticleAt(this.posX, this.posY+0.9D, this.posZ, 0D, 0D, 0D, (byte)28);
  					}
  					else {
  						ParticleHelper.spawnAttackParticleAt(this.posX, this.posY+1.1D, this.posZ, 0D, 0D, 0D, (byte)28);
  					}
  				}
  			}
  			
  			//�P�B�M����V
  			if(this.isRiding() && this.ridingEntity instanceof EntityLivingBase) {
  				this.renderYawOffset = ((EntityLivingBase)ridingEntity).renderYawOffset;
  				this.rotationYaw = ridingEntity.rotationYaw;
  			}
  		}
  			
  		super.onLivingUpdate();
  	}
  	
  	//cancel go riding entity
  	private void cancelGoRiding() {
  		goRidingTicks = 0;
		goRideEntity = null;
		goRiding = false;
  	}
	
  	//mount other entity
	private void checkRiding() {
		this.cancelGoRiding();
		if(this.isSitting() || this.getLeashed() || this.getStateFlag(ID.F.NoFuel)) {
			return;
		}
		
		//�w�g�b�M��, �h���v�U���M
		if(this.isRiding()) {
			this.mountEntity(null);
		}
		else {
			EntityLivingBase getEnt = null;
	        AxisAlignedBB impactBox = this.boundingBox.expand(6D, 4D, 6D); 
	        List hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, impactBox);
	        List<EntityLivingBase> canRideList = new ArrayList();
	        
	        //�j�Mlist, ��X�Ĥ@�ӥi�H�M�����ؼ�
	        if(hitList != null && !hitList.isEmpty()) {
	            for(int i = 0; i < hitList.size(); ++i) {
	            	getEnt = (EntityLivingBase)hitList.get(i);
	            	
	            	//�u�M���P�D�H����ĥ�Ϊ̥D�H
	            	if(getEnt instanceof BasicEntityShip || getEnt instanceof EntityPlayer) {
	            		if(getEnt != this && !getEnt.isRiding() && getEnt.riddenByEntity == null &&
	            		   EntityHelper.checkSameOwner(this, getEnt)) {
	            			canRideList.add(getEnt);
	            		 }
	            	}
	            }
	        }
	        
	        //�q�i�M���ؼФ��D�X�@�ӥؼ��M��
	        if(canRideList.size() > 0) {
	        	this.goRideEntity = canRideList.get(rand.nextInt(canRideList.size()));
	        	this.goRidingTicks = 0;
    			this.goRiding = true;
	        }
		}//end not riding
	}

	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {
				this.setShipOutfit(player.isSneaking());
				return true;
			}
		}
		
		return super.interact(player);
  	}
	
	@Override
	public int getKaitaiType() {
		return 1;
	}
	
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
		//�Y�M���O��ship, �h�����M��
		if(this.isRiding() && (ridingEntity instanceof BasicEntityShip || ridingEntity instanceof EntityPlayer)) {
			this.mountEntity(null);
		}
		
		return super.attackEntityFrom(attacker, atk);
	}
	
	//change particle
	@Override
	public boolean attackEntityWithAmmo(Entity target) {	
		//get attack value
		float atk = CalcHelper.calcDamageByEquipEffect(this, target, StateFinal[ID.ATK], 0);
		//set knockback value (testing)
		float kbValue = 0.05F;
        
        //experience++
  		addShipExp(2);
  		
  		//grudge--
  		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.LAtk]);
        
        //light ammo--
        if(!decrAmmoNum(0, this.getAmmoConsumption())) {
        	return false;
        }
        
        //calc dist to target
        float distX = (float) (target.posX - this.posX);
        float distY = (float) (target.posY - this.posY);
        float distZ = (float) (target.posZ - this.posZ);
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        distX = distX / distSqrt;
        distY = distY / distSqrt;
        distZ = distZ / distSqrt;
        
        //play attack sound at attacker
      	this.worldObj.playSoundAtEntity(this, "random.bow", ConfigHandler.fireVolume + 0.2F, 0.4F / (rand.nextFloat() * 0.4F + 0.8F));
      	
      	//play entity attack sound
        if(this.rand.nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //�o�g�̷����S��
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 31, this.posX, this.posY, this.posZ, distX, distY, distZ, true), point);

        //calc miss chance, if not miss, calc cri/multi hit
        float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.M.ShipLevel];
        missChance -= EffectEquip[ID.EF_MISS];		//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance
        
        //calc miss -> crit -> double -> tripple
  		if(rand.nextFloat() < missChance) {
          	atk = 0F;	//still attack, but no damage
          	//spawn miss particle
      		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
  		}
  		else {
  			//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
  			//calc critical
          	if(rand.nextFloat() < this.getEffectEquip(ID.EF_CRI)) {
          		atk *= 1.5F;
          		//spawn critical particle
          		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 11, false), point);
          	}
          	else {
          		//calc double hit
              	if(rand.nextFloat() < this.getEffectEquip(ID.EF_DHIT)) {
              		atk *= 2F;
              		//spawn double hit particle
              		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 12, false), point);
              	}
              	else {
              		//calc double hit
                  	if(rand.nextFloat() < this.getEffectEquip(ID.EF_THIT)) {
                  		atk *= 3F;
                  		//spawn triple hit particle
                  		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 13, false), point);
                  	}
              	}
          	}
  		}
  		
  		//calc damage to player
  		if(target instanceof EntityPlayer) {
  			atk *= 0.25F;
    			
  			//check friendly fire
      		if(!ConfigHandler.friendlyFire) {
      			atk = 0F;
      		}
      		else if(atk > 59F) {
      			atk = 59F;	//same with TNT
      		}
  		}
      		
	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);

	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue, 
	                   0.1D, MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue);
	            motionX *= 0.6D;
	            motionZ *= 0.6D;
	        }
	        
        	//display hit particle on target
	        TargetPoint point1 = new TargetPoint(this.dimension, target.posX, target.posY, target.posZ, 64D);
			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 30, false), point1);
        }

	    return isTargetHurt;
	}
  	
  	//�߳����N����
  	@Override
  	public boolean attackEntityWithHeavyAmmo(Entity target) {
		//set knockback value (testing)
		float kbValue = 0.15F;

		//�p��ؼжZ��
		float tarX = (float)target.posX;	//for miss chance calc
		float tarY = (float)target.posY;
		float tarZ = (float)target.posZ;
		float distX = tarX - (float)this.posX;
		float distY = tarY - (float)this.posY;
		float distZ = tarZ - (float)this.posZ;
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        float launchPos = (float)posY + height;

        if(getShipDepth() > 0D) {
        	launchPos += 0.2D;
        }
		
		//experience++
		addShipExp(16);
		
		//grudge--
		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.HAtk]);
		
		//�o�g�̷����S�� (���ϥίS��, ���O�n�o�e�ʥ]�ӳ]�wattackTime)
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
	
		//play attack sound at attacker
		this.worldObj.playSoundAtEntity(this, "random.bow", ConfigHandler.fireVolume + 0.2F, 0.4F / (rand.nextFloat() * 0.4F + 0.8F));

        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //heavy ammo--
        if(!decrAmmoNum(1, this.getAmmoConsumption())) {
        	return false;
        }
        
        //spawn missile
        EntityFloatingFort ffort = new EntityFloatingFort(this.worldObj, this, (EntityLivingBase) target, launchPos);
        this.worldObj.spawnEntityInWorld(ffort);
        
        return true;
  	}
  	
  	@Override
	public double getMountedYOffset() {
  		if(this.isSitting()) {
			if(getStateEmotion(ID.S.Emotion) == ID.Emotion.BORED) {
				return (double)this.height * 0.0F;
  			}
  			else {
  				return (double)this.height * 0.0F;
  			}
  		}
  		else {
  			return (double)this.height * 0.3F;
  		}
	}

	@Override
	public void setShipOutfit(boolean isSneaking) {
		//�����˳����
		if(isSneaking) {
			switch(getStateEmotion(ID.S.State2)) {
			case ID.State.EQUIP00_2:
				setStateEmotion(ID.S.State2, ID.State.EQUIP01_2, true);
				break;
			case ID.State.EQUIP01_2:
				setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
				break;
			default:
				setStateEmotion(ID.S.State2, ID.State.EQUIP00_2, true);
				break;
			}
		}
		else {
			switch(getStateEmotion(ID.S.State)) {
			case ID.State.EQUIP00:
				setStateEmotion(ID.S.State, ID.State.EQUIP01, true);
				break;
			case ID.State.EQUIP01:
				setStateEmotion(ID.S.State, ID.State.EQUIP02, true);
				break;
			case ID.State.EQUIP02:
				setStateEmotion(ID.S.State, ID.State.NORMAL, true);
				break;
			default:
				setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
				break;
			}
		}
	}


}




