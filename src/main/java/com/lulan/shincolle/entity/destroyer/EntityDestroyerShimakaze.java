package com.lulan.shincolle.entity.destroyer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.ExtendShipProps;
import com.lulan.shincolle.entity.IShipSummonAttack;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.entity.other.EntityRensouhouS;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class EntityDestroyerShimakaze extends BasicEntityShipSmall implements IShipSummonAttack {

	public int numRensouhou;
	
	public EntityDestroyerShimakaze(World world) {
		super(world);
		this.setSize(0.6F, 1.8F);	//�I���j�p ��ҫ��j�p�L��
		this.setStateMinor(ID.M.ShipType, ID.ShipType.DESTROYER);
		this.setStateMinor(ID.M.ShipClass, ID.Ship.DestroyerShimakaze);
		this.setStateMinor(ID.M.DamageType, ID.ShipDmgType.DESTROYER);
		this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[ID.ShipConsume.DD]);
		this.ModelPos = new float[] {0F, 15F, 0F, 40F};
		ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);
		
		this.numRensouhou = 6;
		this.initTypeModify();
		
		//set attack type
		this.StateFlag[ID.F.HaveRingEffect] = true;
		this.StateFlag[ID.F.AtkType_AirLight] = false;
		this.StateFlag[ID.F.AtkType_AirHeavy] = false;
	}
	
	//for morph
	@Override
	public float getEyeHeight() {
		return 1.7375F;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType() {
		return 1;
	}
	
	@Override
	public void setAIList() {
		super.setAIList();
		//use range attack (light)
		this.tasks.addTask(11, new EntityAIShipRangeAttack(this));			   //0011
	}
    
    //check entity state every tick
  	@Override
  	public void onLivingUpdate() {
  		super.onLivingUpdate();
          
  		if(!worldObj.isRemote) {
  			//add aura to master every 100 ticks
  			if(this.ticksExisted % 100 == 0) {
  				//add num of rensouhou
  				if(this.numRensouhou < 6) numRensouhou++;
  				
  				//apply ring effect
  				EntityPlayerMP player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID(), this.worldObj);
  				if(getStateFlag(ID.F.IsMarried) && getStateFlag(ID.F.UseRingEffect) && getStateMinor(ID.M.NumGrudge) > 0 && player != null && getDistanceSqToEntity(player) < 256D) {
  					//potion effect: id, time, level
  	  	  			player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 300, getStateMinor(ID.M.ShipLevel) / 25 + 1));
  				}
  			}
  		}    
  	}
  	
  	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {
				if(player.isSneaking()) {
					switch(getStateEmotion(ID.S.State2)) {
					case ID.State.NORMAL_2:
						setStateEmotion(ID.S.State2, ID.State.EQUIP00_2, true);
						break;
					case ID.State.EQUIP00_2:
						setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
						break;	
					default:
						setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
						break;
					}
				}
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
		
		super.interact(player);
		return false;
  	}
  	
  	//�۳�s�˯��i�����
  	@Override
  	public boolean attackEntityWithAmmo(Entity target) {
  		//check num rensouhou
  		if(this.numRensouhou <= 0) {
  			return false;
  		}
  		else {
  			this.numRensouhou--;
  		}
  		
        //play entity attack sound
        if(this.rand.nextInt(10) > 5) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //light ammo--
        if(!decrAmmoNum(4)) {		//not enough ammo
        	return false;
        }
        
        //experience++
  		addShipExp(8);
  		
  		//grudge--
  		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.LAtk] * 4);

        //�o�g�̷����S�� (�۳�s�˯����ϥίS��, ���O�n�o�e�ʥ]�ӳ]�wattackTime)
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
  		
		//spawn airplane
        if(target instanceof EntityLivingBase) {
        	if(this.getStateEmotion(ID.S.State2) > ID.State.NORMAL_2) {
        		EntityRensouhouS rensoho2 = new EntityRensouhouS(this.worldObj, this, target);
                this.worldObj.spawnEntityInWorld(rensoho2);
        	}
        	else {
        		EntityRensouhou rensoho1 = new EntityRensouhou(this.worldObj, this, target);
                this.worldObj.spawnEntityInWorld(rensoho1);
        	}
            return true;
        }

        return false;
	}
  	
  	//���s�˻į����p
  	@Override
  	public boolean attackEntityWithHeavyAmmo(Entity target) {	
		//get attack value
		float atk = StateFinal[ID.ATK_H] * 0.3F;
		float kbValue = 0.15F;
		
		//���u�O�_�ĥΪ��g
		boolean isDirect = false;
		//�p��ؼжZ��
		float tarX = (float)target.posX;	//for miss chance calc
		float tarY = (float)target.posY;
		float tarZ = (float)target.posZ;
		float distX = tarX - (float)this.posX;
		float distY = tarY - (float)this.posY;
		float distZ = tarZ - (float)this.posZ;
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        float launchPos = (float)posY + height * 0.7F;
        
        //�W�L�@�w�Z��/���� , �h�ĥΩߪ��u,  �b�����ɵo�g���׸��C
        if((distX*distX+distY*distY+distZ*distZ) < 36F) {
        	isDirect = true;
        }
        if(this.getShipDepth() > 0D) {
        	isDirect = true;
        	launchPos = (float)posY;
        }
		
		//experience++
		addShipExp(16);
		
		//grudge--
		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.HAtk]);
	
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //heavy ammo -1
        if(!decrAmmoNum(1)) {	//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
        }
        
        //calc miss chance, miss: add random offset(0~6) to missile target 
        float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.M.ShipLevel];
        missChance -= EffectEquip[ID.EF_MISS];	//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance = 30%
       
        if(this.rand.nextFloat() < missChance) {
        	tarX = tarX - 5F + this.rand.nextFloat() * 10F;
        	tarY = tarY + this.rand.nextFloat() * 5F;
        	tarZ = tarZ - 5F + this.rand.nextFloat() * 10F;
        	//spawn miss particle
        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
        }
        
        //�o�g�̷����S�� (���ϥίS��, ���O�n�o�e�ʥ]�ӳ]�wattackTime)
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);

        //spawn missile
        EntityAbyssMissile missile1 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX, tarY+target.height*0.2F, tarZ, launchPos, atk, kbValue, isDirect, -1F);
        EntityAbyssMissile missile2 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX+3F, tarY+target.height*0.2F, tarZ+5F, launchPos, atk, kbValue, isDirect, -1F);
        EntityAbyssMissile missile3 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX+3F, tarY+target.height*0.2F, tarZ-5F, launchPos, atk, kbValue, isDirect, -1F);
        EntityAbyssMissile missile4 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX-3F, tarY+target.height*0.2F, tarZ+5F, launchPos, atk, kbValue, isDirect, -1F);
        EntityAbyssMissile missile5 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX-3F, tarY+target.height*0.2F, tarZ-5F, launchPos, atk, kbValue, isDirect, -1F);
        
        this.worldObj.spawnEntityInWorld(missile1);
        this.worldObj.spawnEntityInWorld(missile2);
        this.worldObj.spawnEntityInWorld(missile3);
        this.worldObj.spawnEntityInWorld(missile4);
        this.worldObj.spawnEntityInWorld(missile5);
        
        return true;
	}
  	
  	@Override
	public int getKaitaiType() {
		return 2;
	}
  	
  	@Override
	public int getNumServant() {
		return this.numRensouhou;
	}

	@Override
	public void setNumServant(int num) {
		this.numRensouhou = num;
	}

	@Override
	public double getMountedYOffset() {
		if(this.isSitting()) {
			if(getStateEmotion(ID.S.Emotion) == ID.Emotion.BORED) {
				return 0F;
  			}
  			else {
  				return (double)this.height * 0.1F;
  			}
  		}
  		else {
  			return (double)this.height * 0.45F;
  		}
	}


}

