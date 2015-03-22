package com.lulan.shincolle.entity;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIOwnerHurtByTarget;
import net.minecraft.entity.ai.EntityAIOwnerHurtTarget;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIRestrictOpenDoor;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.EntityAIShipAttackOnCollide;
import com.lulan.shincolle.ai.EntityAIShipFlee;
import com.lulan.shincolle.ai.EntityAIShipFollowOwner;
import com.lulan.shincolle.ai.EntityAIShipInRangeTarget;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipFloating;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.ai.EntityAIShipWatchClosest;
import com.lulan.shincolle.client.inventory.ContainerShipInventory;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityHeavyCruiserRi extends BasicEntityShipSmall {
	
	public EntityHeavyCruiserRi(World world) {
		super(world);
		this.setSize(0.9F, 1.7F);	//�I���j�p ��ҫ��j�p�L��
		this.setCustomNameTag(StatCollector.translateToLocal("entity.shincolle.EntityHeavyCruiserRi.name"));
		this.ShipType = ID.ShipType.HEAVY_CRUISER;
		this.ShipID = ID.S_HeavyCruiserRI;
		ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);	
		this.initTypeModify();
		
	}
	
	//for morph
	@Override
	public float getEyeHeight() {
		return this.height * 1.06F;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType() {
		return 1;
	}

	public void setAIList() {
		super.setAIList();
		
		//high priority
		this.tasks.addTask(1, new EntityAIShipSit(this));	   				   //0101
		this.tasks.addTask(2, new EntityAIShipFlee(this));					   //0111
		this.tasks.addTask(3, new EntityAIShipFollowOwner(this));	   		   //0111
		
		//use range attack (light)
		this.tasks.addTask(11, new EntityAIShipRangeAttack(this));			   //0011
		
		//use melee attack
		if(this.getStateFlag(ID.F.UseMelee)) {
			this.tasks.addTask(12, new EntityAIShipAttackOnCollide(this, 1D, true));   //0011
			this.tasks.addTask(13, new EntityAIMoveTowardsTarget(this, 1D, 48F));  //0001
		}

		//idle AI
		//moving
		this.tasks.addTask(21, new EntityAIOpenDoor(this, true));			   //0000
		this.tasks.addTask(23, new EntityAIShipFloating(this));				   //0101
		this.tasks.addTask(24, new EntityAIShipWatchClosest(this, EntityPlayer.class, 6F, 0.1F)); //0010
		this.tasks.addTask(25, new EntityAIWander(this, 0.8D));				   //0001
//		this.tasks.addTask(25, new EntityAILookIdle(this));					   //0011
		
/* 		//switch AI method
		this.tasks.removeTask(this.aiAttackOnCollide);
        this.tasks.removeTask(this.aiArrowAttack);
        ItemStack itemstack = this.getHeldItem();

        if (itemstack != null && itemstack.getItem() == Items.bow)
        {
            this.tasks.addTask(4, this.aiArrowAttack);
        }
        else
        {
            this.tasks.addTask(4, this.aiAttackOnCollide);
        }*/
	}
	
	public void setAITargetList() {	
		this.targetTasks.addTask(1, new EntityAIOwnerHurtByTarget(this));
		this.targetTasks.addTask(2, new EntityAIOwnerHurtTarget(this));
		this.targetTasks.addTask(3, new EntityAIHurtByTarget(this, false));
		this.targetTasks.addTask(4, new EntityAIShipInRangeTarget(this, 0.4F, 1));
	}

    @Override
    public void onLivingUpdate() {
    	//check server side
    	if(!this.worldObj.isRemote) {
//    		LogHelper.info("DEBUG : depth "+this.tasks.);
    		//check every 5 sec
    		if(this.ticksExisted % 100 == 0) {
	    		//apply potion effect in the night
	        	if (!this.worldObj.isDaytime()) {	
        			this.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 300, 2));
        			this.addPotionEffect(new PotionEffect(Potion.jump.id, 300, 2));
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
				switch(getStateEmotion(ID.S.State)) {
				case ID.State.NORMAL:
					setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
					break;
				case ID.State.EQUIP00:
					setStateEmotion(ID.S.State, ID.State.EQUIP01, true);
					break;
				case ID.State.EQUIP01:
					setStateEmotion(ID.S.State, ID.State.EQUIP02, true);
					break;
				case ID.State.EQUIP02:
					setStateEmotion(ID.S.State, ID.State.NORMAL, true);
					break;
				}
				return true;
			}
		}
		
		super.interact(player);
		return false;
  	}
	

}

