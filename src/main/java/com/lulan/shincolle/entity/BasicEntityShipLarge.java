package com.lulan.shincolle.entity;

import com.lulan.shincolle.client.particle.EntityFXMiss;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

abstract public class BasicEntityShipLarge extends BasicEntityShip {

	protected int numAircraftLight;		//# of airplane at same time
	protected int numAircraftHeavy;
	protected int maxAircraftLight;		//max airplane at same time
	protected int maxAircraftHeavy;
	protected int delayAircraft = 0;		//airplane recover delay
	protected double launchHeight;		//airplane launch height

	
	public BasicEntityShipLarge(World world) {
		super(world);
	}
	
	//getter
	public int getNumAircraftLight() {
		return this.numAircraftLight;
	}
	public int getNumAircraftHeavy() {
		return this.numAircraftHeavy;
	}
	
	//setter
	public void setNumAircraftLight(int par1) {
		if(this.worldObj.isRemote) {	//client�ݨS��max�ȥi�H�P�w, �]�������]�w�Y�i
			numAircraftLight = par1;
		}
		else {
			numAircraftLight = par1;
			if(numAircraftLight > maxAircraftLight) numAircraftLight = maxAircraftLight;
			if(numAircraftLight < 0) numAircraftLight = 0;
		}
	}
	public void setNumAircraftHeavy(int par1) {
		if(this.worldObj.isRemote) {	//client�ݨS��max�ȥi�H�P�w, �]�������]�w�Y�i
			numAircraftHeavy = par1;
		}
		else {
			numAircraftHeavy = par1;
			if(numAircraftHeavy > maxAircraftHeavy) numAircraftHeavy = maxAircraftHeavy;
			if(numAircraftHeavy < 0) numAircraftHeavy = 0;
		}
	}
	
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		
		//server side
		if(!this.worldObj.isRemote) {
			//�C�@�q�ɶ��^�_�@��ĥ����
			delayAircraft--;
			if(this.delayAircraft <= 0) {
				delayAircraft = (int)(120F / (this.getFinalState(AttrID.SPD)));
				this.setNumAircraftLight(this.getNumAircraftLight()+1);
				this.setNumAircraftHeavy(this.getNumAircraftHeavy()+1);
			}
//			LogHelper.info("DEBUG : air num "+getNumAircraftLight()+" "+getNumAircraftHeavy());
		}
	}
	
	//�W�[ĥ�����ƶq�p��
	@Override
	public void calcShipAttributes(byte id) {
		super.calcShipAttributes(id);
		
		this.maxAircraftLight = 4 + (int)(this.ShipLevel/5);
		this.maxAircraftHeavy = 2 + (int)(this.ShipLevel/10);
	}
	
	//range attack method, cost light ammo, attack delay = 20 / attack speed, damage = 100% atk 
	public boolean attackEntityWithAircraft(Entity target) {
//		LogHelper.info("DEBUG : launch LIGHT aircraft"+target);
		//clear target every attack
		this.setAttackTarget(null);
		
		//num aircraft--, number check in carrier AI
		this.setNumAircraftLight(this.getNumAircraftLight()-1);
		
		//play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-aircraft", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        
        //experience++
  		addShipExp(8);
  		
  		//grudge--
  		decrGrudgeNum(2);
        
        //light ammo -1
        if(!decrAmmoNum(4)) {		//not enough ammo
        	return false;
        }
        
        //spawn airplane
        if(target instanceof EntityLivingBase) {
        	EntityAirplane plane = new EntityAirplane(this.worldObj, this, (EntityLivingBase)target, this.posY+launchHeight);
            this.worldObj.spawnEntityInWorld(plane);
            return true;
        }
        return false;
	}

	//range attack method, cost heavy ammo, attack delay = 100 / attack speed, damage = 500% atk
	public boolean attackEntityWithHeavyAircraft(Entity target) {
//		LogHelper.info("DEBUG : launch HEAVY aircraft"+target);
		//clear target every attack
		this.setAttackTarget(null);
		
		//num aircraft--, number check in carrier AI
		this.setNumAircraftHeavy(this.getNumAircraftHeavy()-1);
		
		//play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-aircraft", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        
        //experience++
  		addShipExp(32);
  		
  		//grudge--
  		decrGrudgeNum(3);
        
        //light ammo -1
        if(!decrAmmoNum(5)) {		//not enough ammo
        	return false;
        }
        
        //spawn airplane
        if(target instanceof EntityLivingBase) {
        	EntityAirplaneTakoyaki plane = new EntityAirplaneTakoyaki(this.worldObj, this, (EntityLivingBase)target, this.posY+this.launchHeight);
            this.worldObj.spawnEntityInWorld(plane);
            return true;
        }
        return false;
	}
	
}
