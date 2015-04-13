package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.EntityAIShipAttackOnCollide;
import com.lulan.shincolle.ai.EntityAIShipCarrierAttack;
import com.lulan.shincolle.ai.EntityAIShipFlee;
import com.lulan.shincolle.ai.EntityAIShipFloating;
import com.lulan.shincolle.ai.EntityAIShipFollowOwner;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.ai.EntityAIShipWatchClosest;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class EntityDestroyerShimakazeBoss extends BasicEntityShipHostile implements IBossDisplayData {

	public int numRensouhou;
	
	public EntityDestroyerShimakazeBoss(World world) {
		super(world);
		this.setSize(1.4F, 6F);
		this.setCustomNameTag(StatCollector.translateToLocal("entity.shincolle.EntityDestroyerShimakazeBoss.name"));
        
        //basic attr
        this.atk = (float) ConfigHandler.scaleBossSMKZ[ID.ATK];
        this.atkSpeed = (float) ConfigHandler.scaleBossSMKZ[ID.SPD];
        this.atkRange = (float) ConfigHandler.scaleBossSMKZ[ID.HIT];
        this.defValue = (float) ConfigHandler.scaleBossSMKZ[ID.DEF];
        this.movSpeed = (float) ConfigHandler.scaleBossSMKZ[ID.MOV];
        this.numRensouhou = 10;

        //AI flag
        this.StartEmotion = 0;
        this.StartEmotion2 = 0;
        this.headTilt = false;
        
        //misc
        this.dropItem = new ItemStack(ModItems.ShipSpawnEgg, 1, ID.S_DestroyerShimakaze+2);
 
	    //�]�w���ݩ�
	    getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(ConfigHandler.scaleBossSMKZ[ID.HP]);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(this.movSpeed);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(atkRange + 32); //������ؼ�, ���|���d��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1D);
		if(this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
				
		//�]�wAI
		this.setAIList();
		this.setAITargetList();
	}
	
	@Override
	protected boolean canDespawn() {
        return this.ticksExisted > 6000;
    }
	
	@Override
	public float getEyeHeight() {
		return this.height * 0.5F;
	}
	
	//setup AI
	@Override
	protected void setAIList() {
		super.setAIList();

		//use range attack
		this.tasks.addTask(1, new EntityAIShipRangeAttack(this));			   //0011
		this.tasks.addTask(2, new EntityAIMoveTowardsTarget(this, 1D, 40F));   //0001
		
		//idle AI
		//moving
		this.tasks.addTask(21, new EntityAIOpenDoor(this, true));			   //0000
		this.tasks.addTask(22, new EntityAIShipFloating(this));				   //0101
		this.tasks.addTask(23, new EntityAIShipWatchClosest(this, EntityPlayer.class, 6F, 0.1F)); //0010
		this.tasks.addTask(24, new EntityAIWander(this, 0.8D));				   //0001
		this.tasks.addTask(25, new EntityAILookIdle(this));					   //0011

	}
	
	//num rensouhou++
	@Override
  	public void onLivingUpdate() {
  		super.onLivingUpdate();
          
  		if(!worldObj.isRemote) {
  			//add aura to master every N ticks
  			if(this.ticksExisted % 100 == 0) {
  				//add num of rensouhou
  				if(this.numRensouhou < 10) numRensouhou++;
  			}
  		}    
  	}
	
	@Override
	protected boolean interact(EntityPlayer player) {
		//use kaitai hammer to kill boss (creative mode only)
		if(!this.worldObj.isRemote && player.capabilities.isCreativeMode) {
			if(player.inventory.getCurrentItem() != null && 
			   player.inventory.getCurrentItem().getItem() == ModItems.KaitaiHammer) {
				this.setDead();
			}
		}
		
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

        //�o�g�̷����S�� (�۳�s�˯����ϥίS��, ���O�n�o�e�ʥ]�ӳ]�wattackTime)
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
  		
		//spawn airplane
        if(target instanceof EntityLivingBase) {
        	EntityRensouhouBoss rensoho = new EntityRensouhouBoss(this.worldObj, this, (EntityLivingBase)target);
            this.worldObj.spawnEntityInWorld(rensoho);
            return true;
        }

        return false;
	}
  	
  	//���s�˻į����p
  	@Override
  	public boolean attackEntityWithHeavyAmmo(Entity target) {	
		//get attack value
		float atkHeavy = this.atk * 0.3F;
		
		//set knockback value (testing)
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
        float missChance = 0.25F;	//max miss chance = 30%
       
        if(this.rand.nextFloat() < missChance) {
        	tarX = tarX - 3F + this.rand.nextFloat() * 6F;
        	tarY = tarY + this.rand.nextFloat() * 3F;
        	tarZ = tarZ - 3F + this.rand.nextFloat() * 6F;
        	//spawn miss particle
        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
        }
        
        //�o�g�̷����S�� (���ϥίS��, ���O�n�o�e�ʥ]�ӳ]�wattackTime)
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);

        //spawn missile
        EntityAbyssMissile missile1 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX, tarY+target.height*0.2F, tarZ, launchPos, atkHeavy, kbValue, isDirect);
        EntityAbyssMissile missile2 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX+3F, tarY+target.height*0.2F, tarZ+3F, launchPos, atkHeavy, kbValue, isDirect);
        EntityAbyssMissile missile3 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX+3F, tarY+target.height*0.2F, tarZ-3F, launchPos, atkHeavy, kbValue, isDirect);
        EntityAbyssMissile missile4 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX-3F, tarY+target.height*0.2F, tarZ+3F, launchPos, atkHeavy, kbValue, isDirect);
        EntityAbyssMissile missile5 = new EntityAbyssMissile(this.worldObj, this, 
        		tarX-3F, tarY+target.height*0.2F, tarZ-3F, launchPos, atkHeavy, kbValue, isDirect);
        
        this.worldObj.spawnEntityInWorld(missile1);
        this.worldObj.spawnEntityInWorld(missile2);
        this.worldObj.spawnEntityInWorld(missile3);
        this.worldObj.spawnEntityInWorld(missile4);
        this.worldObj.spawnEntityInWorld(missile5);
        
        return true;
	}

}
