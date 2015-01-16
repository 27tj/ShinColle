package com.lulan.shincolle.entity;


import java.util.Random;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityDestroyerI extends EntityTameable {
	
	private Random rand = new Random();
	public int entityState = 0;

	public EntityDestroyerI(World world) {
		super(world);
		this.setSize(0.9F, 1.4F);	//�I���j�p ��ҫ��j�p�L��
		//�Ѽ�: AI�u����, AI(AI�Ѽ�)
	//	this.tasks.addTask(0, new EntityAIWander(this, 1.0D));
	//	this.tasks.addTask(1, new EntityAIWatchClosest2(this, EntityPlayer.class, 5F, 2F));
	//	this.tasks.addTask(2, new EntityAIPanic(this, 2F));
	//	this.tasks.addTask(3, new EntityAILookIdle(this));
	}
	
	public boolean isAIEnabled() {
		return true;
	}
	
	//�ͪ��ݩʳ]�w
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(4.0D);
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.4D);
		
	}
	
	//���`����
	protected String getLivingSound()
    {
        return Reference.MOD_ID+":ship-say";
    }
	
	//���˭���
    protected String getHurtSound()
    {
    	
        return Reference.MOD_ID+":ship-hurt";
    }

    //���`����
    protected String getDeathSound()
    {
    	return Reference.MOD_ID+":ship-death";
    }

    //���Ĥj�p
    protected float getSoundVolume()
    {
        return 0.4F;
    }

	@Override
	public EntityAgeable createChild(EntityAgeable var1) {
		return new EntityDestroyerI(worldObj);
	}
	

}
