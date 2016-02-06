package com.lulan.shincolle.entity.submarine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.CalcHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class EntitySubmU511Mob extends BasicEntityShipHostile implements IShipInvisible {

	private static float ilevel = 35F;
	
	
	public EntitySubmU511Mob(World world) {
		super(world);
		this.setSize(0.7F, 1.4F);
//		this.setCustomNameTag(StatCollector.translateToLocal("entity.shincolle.EntitySubmU511Mob.name"));
        
        //basic attr
        this.atk = (float) ConfigHandler.scaleMobSubm[ID.ATK];
        this.atkSpeed = (float) ConfigHandler.scaleMobSubm[ID.SPD];
        this.atkRange = (float) ConfigHandler.scaleMobSubm[ID.HIT];
        this.defValue = (float) ConfigHandler.scaleMobSubm[ID.DEF];
        this.movSpeed = (float) ConfigHandler.scaleMobSubm[ID.MOV];
        this.stepHeight = 1F;

        //AI flag
        this.StartEmotion = 0;
        this.StartEmotion2 = 0;
        this.headTilt = false;
        
        //misc
        this.dropItem = new ItemStack(ModItems.ShipSpawnEgg, 1, ID.Ship.SubmarineU511+2);
        
	    //�]�w���ݩ�
	    getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(ConfigHandler.scaleMobSubm[ID.HP]);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(this.movSpeed);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(atkRange + 16); //������ؼ�, ���|���d��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(0.3D);
		if(this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
				
		//�]�wAI
		this.setAIList();
		this.setAITargetList();
	}
	
	@Override
	protected boolean canDespawn() {
        return this.ticksExisted > 600;
    }
	
	@Override
	public float getEyeHeight() {
		return this.height * 1.2F;
	}
	
	//chance drop
	@Override
	public ItemStack getDropEgg() {
		return this.rand.nextInt(5) == 0 ? this.dropItem : null;
	}
	
	//setup AI
	@Override
	protected void setAIList() {
		super.setAIList();

		//use range attack
		this.tasks.addTask(1, new EntityAIShipRangeAttack(this));			   //0011
		this.tasks.addTask(2, new EntityAIMoveTowardsTarget(this, 1D, 40F));   //0001
	}
	
	//set invisible
	@Override
  	public void onLivingUpdate() {
  		super.onLivingUpdate();
          
  		if(!worldObj.isRemote) {
  			//add aura to master every N ticks
  			if(this.ticksExisted % 200 == 0) {
  				if(this.rand.nextInt(2) == 0) {
  					this.addPotionEffect(new PotionEffect(Potion.invisibility.id, 120));
  				}
  			}
  		}    
  	}
	
	//�۳�s�˯��i�����
  	@Override
  	public boolean attackEntityWithAmmo(Entity target) {
  		//get attack value
  		float atk = this.atk;
  		float kbValue = 0.05F;
  		
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
  		if(getShipDepth() > 0D) {
          	isDirect = true;
          	launchPos = (float)posY;
  		}
  	
  		//play cannon fire sound at attacker
  		this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
  		//play entity attack sound
  		if(this.getRNG().nextInt(10) > 7) {
          	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
  		}
          
  		//calc miss chance, miss: add random offset(0~6) to missile target 
  		if(this.rand.nextFloat() < 0.2F) {
  			tarX = tarX - 3F + this.rand.nextFloat() * 6F;
  			tarY = tarY + this.rand.nextFloat() * 3F;
  			tarZ = tarZ - 3F + this.rand.nextFloat() * 6F;
  			//spawn miss particle
  			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
  			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
  		}

  		//spawn missile
  		EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this, 
          		tarX, tarY+target.height*0.2F, tarZ, launchPos, atk, kbValue, isDirect, 0.08F);
  		this.worldObj.spawnEntityInWorld(missile);
          
  		return true;
	}
  	
  	//���s�˻į����p
  	@Override
  	public boolean attackEntityWithHeavyAmmo(Entity target) {	
		//get attack value
		float atkHeavy = this.atk * 3F;
		float kbValue = 0.08F;
		
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
        float launchPos = (float)posY + height * 0.5F;
        
        //�W�L�@�w�Z��/���� , �h�ĥΩߪ��u,  �b�����ɵo�g���׸��C
        if((distX*distX+distY*distY+distZ*distZ) < 36F || this.getShipDepth() > 0D) {
        	isDirect = true;
        }
	
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //calc miss chance, miss: add random offset(0~6) to missile target 
        if(this.rand.nextFloat() < 0.25F) {
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
        		tarX, tarY+target.height*0.2F, tarZ, launchPos, atkHeavy, kbValue, isDirect, -1F);    
        this.worldObj.spawnEntityInWorld(missile1);
        
        return true;
	}
  	
  	@Override
	public int getDamageType() {
		return ID.ShipDmgType.SUBMARINE;
	}
  	
  	@Override
	public float getInvisibleLevel() {
		return this.ilevel;
	}
	
	@Override
	public void setInvisibleLevel(float level) {
		this.ilevel = level;
	}
  	

}




