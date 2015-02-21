package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.EntityAIShipAircraftAttack;
import com.lulan.shincolle.client.particle.EntityFXSpray;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

public class EntityAirplaneTakoyaki extends BasicEntityAirplane {
	
	public EntityAirplaneTakoyaki(World world) {
		super(world);
		this.setSize(0.6F, 0.6F);
	}
	
	public EntityAirplaneTakoyaki(World world, BasicEntityShip host, EntityLivingBase target, double launchPos) {
		super(world);
		this.world = world;
        this.hostEntity = host;
        this.targetEntity = target;
        
        //basic attr
        this.atk = host.getFinalState(AttrID.ATK);
        this.atkSpeed = host.getFinalState(AttrID.SPD);
        //AI flag
        this.numAmmoLight = 0;
        this.numAmmoHeavy = 3;
        this.useAmmoLight = false;
        this.useAmmoHeavy = true;
        //�]�w�o�g��m
        this.posX = host.posX;
        this.posY = launchPos;
        this.posZ = host.posZ;
        this.setPosition(this.posX, this.posY, this.posZ);
 
	    //�]�w���ݩ�
	    getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(host.getFinalState(AttrID.HP)*0.15D);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(host.getFinalState(AttrID.MOV)*0.1D + 0.3D);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(16); //������ؼ�, ���|���d��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1D);
		if(this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
				
		//�]�wAI
		this.setAIList();
	}
	
	//setup AI
	protected void setAIList() {
		this.clearAITasks();
		this.clearAITargetTasks();

		this.getNavigator().setEnterDoors(true);
		this.getNavigator().setAvoidsWater(false);
		this.getNavigator().setCanSwim(true);
		
		this.tasks.addTask(1, new EntityAIShipAircraftAttack(this));
		this.setAttackTarget(targetEntity);
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if(this.worldObj.isRemote) {
			if(this.ticksExisted % 2 == 0) {
				EntityFX particleSpray = new EntityFXSpray(worldObj, 
			      		this.posX, this.posY+0.1D, this.posZ, 
			      		-this.motionX*0.5D, 0.07D, -this.motionZ*0.5D,
			      		1.0F, 0.0F, 0.0F, 0.8F);
				Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray);
			}
		}
	}

}
