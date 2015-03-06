package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.EntityAIShipAircraftAttack;
import com.lulan.shincolle.client.particle.EntityFXSpray;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

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
        this.atk = host.getStateFinal(ID.ATK_AH);
        this.atkSpeed = host.getStateFinal(ID.SPD);
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
	    getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(host.getStateFinal(ID.HP)*0.15D);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(host.getStateFinal(ID.MOV)*0.1D + 0.4D);
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
				ParticleHelper.spawnAttackParticleAt(this.posX, this.posY+0.1D, this.posZ, 
			      		-this.motionX*0.5D, 0.07D, -this.motionZ*0.5D, (byte)18);
			}
		}
	}

}
