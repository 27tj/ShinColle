package com.lulan.shincolle.entity;

import java.util.List;
import java.util.UUID;

import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathEntity;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.ai.path.ShipPathPoint;
import com.lulan.shincolle.client.particle.EntityFXTexts;
import com.lulan.shincolle.client.particle.EntityFXSpray;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public abstract class BasicEntityAirplane extends EntityLiving implements IShipAttack {

	protected BasicEntityShipLarge hostEntity;  		//host target
	protected EntityLivingBase targetEntity;	//onImpact target (for entity)
	protected World world;
	protected ShipPathNavigate shipNavigator;	//���Ų��ʥ�navigator
	protected ShipMoveHelper shipMoveHelper;
	
    //attributes
    public float atk;				//damage
    public float atkSpeed;			//attack speed
    public float movSpeed;			//move speed
    public float kbValue;			//knockback value
    
    //AI flag
    public int numAmmoLight;
    public int numAmmoHeavy;
    public boolean useAmmoLight;
    public boolean useAmmoHeavy;
    public boolean backHome;		//clear target, back to carrier
    private final IEntitySelector targetSelector;
	
    public BasicEntityAirplane(World world) {
        super(world);
        this.backHome = false;
        shipNavigator = new ShipPathNavigate(this, worldObj);
		shipMoveHelper = new ShipMoveHelper(this);
		this.shipNavigator.setCanFly(true);
        
        //target selector init
        this.targetSelector = new IEntitySelector() {
            public boolean isEntityApplicable(Entity target2) {
            	if((target2 instanceof EntityMob || target2 instanceof EntitySlime ||
            	   target2 instanceof EntityBat || target2 instanceof EntityDragon ||
            	   target2 instanceof EntityFlying || target2 instanceof EntityWaterMob) &&
            	   target2.isEntityAlive()) {
            		return true;
            	}
            	return false;
            }
        };
    }
    
    @Override
	public boolean isAIEnabled() {
		return true;
	}
  	
    //clear AI
  	protected void clearAITasks() {
  	   tasks.taskEntries.clear();
  	}
  	
  	//clear target AI
  	protected void clearAITargetTasks() {
  	   targetTasks.taskEntries.clear();
  	}

    //�T����󱼸��p��
    protected void fall(float world) {}
    protected void updateFallState(double par1, boolean par2) {}
    public boolean isOnLadder() {
        return false;
    }
    
    @Override
	public float getAttackDamage() {	//not used for airplane
		return 0;
	}
    
    @Override
    public float getAttackSpeed() {
    	return this.atkSpeed;
    }
    
    @Override
	public float getAttackRange() {
    	return 6F;
    }
    
    @Override
	public float getMoveSpeed() {
		return this.movSpeed;
	}
    
    @Override
   	public int getAmmoLight() {
   		return this.numAmmoLight;
   	}

   	@Override
   	public int getAmmoHeavy() {
   		return this.numAmmoHeavy;
   	}
    
    @Override
	public boolean hasAmmoLight() {
    	return this.numAmmoLight > 0;
    }
    
    @Override
	public boolean hasAmmoHeavy() {
    	return this.numAmmoHeavy > 0;
    }

	@Override
	public void setAmmoLight(int num) {
		this.numAmmoLight = num;
	}

	@Override
	public void setAmmoHeavy(int num) {
		this.numAmmoHeavy = num;
	}
        
    @Override
    public EntityLivingBase getOwner() {
        return this.hostEntity;
    }
    
    @Override
    public EntityLivingBase getAttackTarget() {
        return super.getAttackTarget();
    }

    //���ʭp��, �h��gravity����
    public void moveEntityWithHeading(float movX, float movZ) { 	
        this.moveFlying(movX, movZ, this.movSpeed*0.4F); //�������t�׭p��(�t�}���ĪG)
        this.moveEntity(this.motionX, this.motionY, this.motionZ);

        this.motionX *= 0.91D;
        this.motionY *= 0.91D;
        this.motionZ *= 0.91D;
        //����F��|�W��
        if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6D, this.motionZ)) {
            this.motionY += 0.2D;
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d1 = this.posX - this.prevPosX;
        double d0 = this.posZ - this.prevPosZ;
        float f4 = MathHelper.sqrt_double(d1 * d1 + d0 * d0) * 4.0F;

        if(f4 > 1.0F) {
            f4 = 1.0F;
        }

        this.limbSwingAmount += (f4 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

	@Override
	public void onUpdate() {
		//server side
		if(!this.worldObj.isRemote) {
			//owner����(�q�`�Oserver restart)
			if(this.getOwner() == null) {
				this.setDead();
			}
			else {
//				if(this.getAttackTarget()!=null) {
//	    			LogHelper.info("DEBUG : airplane target "+this.getAttackTarget().isDead+" "+this.getAttackTarget());
//	    		}
				
				//�W�L60��۰ʮ���
				if(this.ticksExisted > 1200) {
					this.numAmmoLight -= 2;
					if(this.numAmmoLight < 0) this.numAmmoLight = 0;
					this.numAmmoHeavy -= 1;
					if(this.numAmmoHeavy < 0) this.numAmmoHeavy = 0;
					
					this.hostEntity.setStateMinor(ID.N.NumAmmoLight, this.hostEntity.getStateMinor(ID.N.NumAmmoLight) + this.numAmmoLight);
					this.hostEntity.setStateMinor(ID.N.NumAmmoHeavy, this.hostEntity.getStateMinor(ID.N.NumAmmoHeavy) + this.numAmmoHeavy);
				
					if(this instanceof EntityAirplane) {
						hostEntity.setNumAircraftLight(hostEntity.getNumAircraftLight()+1);
					}
					else {
						hostEntity.setNumAircraftHeavy(hostEntity.getNumAircraftHeavy()+1);
					}
					
					this.setDead();
				}
				
				//�F��30��ɱj���k�v
				if(this.ticksExisted >= 600) {
					this.backHome = true;
				}
				
				//�k�v
				if(this.backHome && !this.isDead) {
					if(this.getDistanceToEntity(this.getOwner()) > 2.7F) {
						this.getShipNavigate().tryMoveToXYZ(this.getOwner().posX, this.getOwner().posY + 2.3D, this.getOwner().posZ, 1D);
					}
					else {	//�k�ٳѾl�u�� (���Ogrudge���k��)
						this.numAmmoLight -= 2;
						if(this.numAmmoLight < 0) this.numAmmoLight = 0;
						this.numAmmoHeavy -= 1;
						if(this.numAmmoHeavy < 0) this.numAmmoHeavy = 0;
						
						this.hostEntity.setStateMinor(ID.N.NumAmmoLight, this.hostEntity.getStateMinor(ID.N.NumAmmoLight) + this.numAmmoLight);
						this.hostEntity.setStateMinor(ID.N.NumAmmoHeavy, this.hostEntity.getStateMinor(ID.N.NumAmmoHeavy) + this.numAmmoHeavy);
					
						if(this instanceof EntityAirplane) {
							hostEntity.setNumAircraftLight(hostEntity.getNumAircraftLight()+1);
						}
						else {
							hostEntity.setNumAircraftHeavy(hostEntity.getNumAircraftHeavy()+1);
						}
						
						this.setDead();
					}
				}
				
				//�e�X���u���ؼв���
				if(this.ticksExisted < 15 && this.getAttackTarget() != null) {
					double distX = this.getAttackTarget().posX - this.posX;
					double distZ = this.getAttackTarget().posZ - this.posZ;
					double distSqrt = MathHelper.sqrt_double(distX*distX + distZ*distZ);
					
					this.motionX = distX / distSqrt * 0.375D;
					this.motionZ = distZ / distSqrt * 0.375D;
					this.motionY = 0.05D;
				}
				
				//�����ؼЮ���, �����ؼ� or �]��host�ثe�ؼ�
				if(!this.backHome && (this.getAttackTarget() == null || this.getAttackTarget().isDead) && this.hostEntity != null && this.ticksExisted % 10 == 0) {	
					//entity list < range1
					EntityLivingBase newTarget;
			        List list = this.worldObj.selectEntitiesWithinAABB(EntityLivingBase.class, 
			        this.boundingBox.expand(20, 20, 20), this.targetSelector);
			        
			        //���䤣��ؼЫh��host�ؼ�, ���Ohost�ؼХ����bxyz20�椺
			        if(list.isEmpty()) {
			        	newTarget = this.hostEntity.getAttackTarget();
			        }
			        else {	//�qĥ���������X���ؼ�, �P�w�O�_�n�h����
			        	newTarget = (EntityLivingBase)list.get(0);
			        }
			        
			        if(newTarget != null && newTarget.isEntityAlive() && this.getDistanceToEntity(newTarget) < 40F && this.getEntitySenses().canSee(newTarget)) {
			        	this.setAttackTarget(newTarget);
			        }
		        	else {
		        		this.backHome = true;
		        	}
				}
				
				if(this.isInWater() && this.ticksExisted % 100 == 0) {
					this.setAir(300);
				}
			}	
		}
		
		if(this.ticksExisted % 4 == 0) {
			//���V�p�� (for both side)
			float[] degree = EntityHelper.getLookDegree(posX - prevPosX, posY - prevPosY, posZ - prevPosZ);
			this.rotationYaw = degree[0];
			this.rotationPitch = degree[1];
		}

		super.onUpdate();
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float atk) {
		//disable 
		if(source.getDamageType() == "inWall") {
			return false;
		}
		
		return super.attackEntityFrom(source, atk);
    }

	//light attack
	@Override
	public boolean attackEntityWithAmmo(Entity target) {
		float atkLight = this.atk;
		float kbValue = 0.03F;

		//play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-machinegun", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //attack particle
        TargetPoint point0 = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 8, false), point0);
		
		//calc miss chance, if not miss, calc cri/multi hit
		TargetPoint point = new TargetPoint(this.dimension, this.hostEntity.posX, this.hostEntity.posY, this.hostEntity.posZ, 64D);
		float missChance = 0.25F - 0.001F * this.hostEntity.getStateMinor(ID.N.ShipLevel);
        missChance -= this.hostEntity.getEffectEquip(ID.EF_MISS);	//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;
  		
        //calc miss chance
        if(this.rand.nextFloat() < missChance) {
        	atkLight = 0;	//still attack, but no damage
        	//spawn miss particle
        	
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.hostEntity, 10, false), point);
        }
        else {
        	//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
        	//calc critical
        	if(this.rand.nextFloat() < this.hostEntity.getEffectEquip(ID.EF_CRI)) {
        		atkLight *= 1.5F;
        		//spawn critical particle
            	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.hostEntity, 11, false), point);
        	}
        	else {
        		//calc double hit
            	if(this.rand.nextFloat() < this.hostEntity.getEffectEquip(ID.EF_DHIT)) {
            		atkLight *= 2F;
            		//spawn double hit particle
            		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.hostEntity, 12, false), point);
            	}
            	else {
            		//calc double hit
                	if(this.rand.nextFloat() < this.hostEntity.getEffectEquip(ID.EF_THIT)) {
                		atkLight *= 3F;
                		//spawn triple hit particle
                		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.hostEntity, 13, false), point);
                	}
            	}
        	}
        }
        
        //vs player = 25% dmg
  		if(target instanceof EntityPlayer) {
  			atkLight *= 0.25F;
  			
  			//check friendly fire
    		if(!ConfigHandler.friendlyFire) {
    			atkLight = 0F;
    		}
    		else if(atkLight > 59F) {
    			atkLight = 59F;	//same with TNT
    		}
  		}

	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atkLight);

	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
	        }
	        
        	//send packet to client for display partical effect  
	        TargetPoint point1 = new TargetPoint(this.dimension, target.posX, target.posY, target.posZ, 64D);
			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 0, false), point1);
        }
	    
	    //���Ӽu�ĭp��
  		if(numAmmoLight > 0) {
  			numAmmoLight--;
  		}

	    return isTargetHurt;
	}

	@Override
	public boolean attackEntityWithHeavyAmmo(Entity target) {
		//get attack value
		float atkHeavy = this.atk;
		//set knockback value (testing)
		float kbValue = 0.08F;

		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        
		//calc miss chance, if not miss, calc cri/multi hit
        float missChance = 0.25F - 0.001F * this.hostEntity.getStateMinor(ID.N.ShipLevel);
        missChance -= this.hostEntity.getEffectEquip(ID.EF_MISS);	//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;
		
        //calc miss chance
        if(this.rand.nextFloat() < missChance) {
        	atkHeavy = 0;	//still attack, but no damage
        	//spawn miss particle
        	TargetPoint point = new TargetPoint(this.dimension, this.hostEntity.posX, this.hostEntity.posY, this.hostEntity.posZ, 64D);
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.hostEntity, 10, false), point);
        }

        //spawn missile
        EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this, 
        		(float)target.posX, (float)(target.posY+target.height*0.2F), (float)target.posZ, (float)(this.posY-0.8F), atkHeavy, kbValue, true, -1F);
        this.worldObj.spawnEntityInWorld(missile);
        
        //���Ӽu�ĭp��
  		if(numAmmoHeavy > 0) {
  			numAmmoHeavy--;
  		}
  		
        return true;
	}
	
	@Override
	public EntityLivingBase getTarget() {
		return this.getAttackTarget();
	}
	
	@Override
	public ShipPathNavigate getShipNavigate() {
		return this.shipNavigator;
	}

	@Override
	public ShipMoveHelper getShipMoveHelper() {
		return this.shipMoveHelper;
	}
	
	//update ship move helper
	@Override
	protected void updateAITasks() {
		super.updateAITasks();
        
        //�Y������path, �h��sship navigator
        if(shipNavigator != null && shipMoveHelper != null && !this.getShipNavigate().noPath()) {
        	//�Y�P�ɦ��x��ai�����|, �h�M���x��ai���|
        	if(!this.getNavigator().noPath()) {
        		this.getNavigator().clearPathEntity();
        	}
			//��particle���path point
			if(this.ticksExisted % 20 == 0) {
				ShipPathEntity pathtemp = this.getShipNavigate().getPath();
				ShipPathPoint pointtemp;
				
				for(int i = 0; i < pathtemp.getCurrentPathLength(); i++) {
					pointtemp = pathtemp.getPathPointFromIndex(i);
					//�o�g�̷����S��
			        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48D);
					//���|�I�e����, �ؼ��I�e���
					if(i == pathtemp.getCurrentPathIndex()) {
						CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 16, pointtemp.xCoord, pointtemp.yCoord+0.5D, pointtemp.zCoord, 0F, 0F, 0F, false), point);
					}
					else {
						CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 18, pointtemp.xCoord, pointtemp.yCoord+0.5D, pointtemp.zCoord, 0F, 0F, 0F, false), point);
					}
				}
			}

			this.worldObj.theProfiler.startSection("ship navi");
	        this.shipNavigator.onUpdateNavigation();
	        this.worldObj.theProfiler.endSection();
	        this.worldObj.theProfiler.startSection("ship move");
	        this.shipMoveHelper.onUpdateMoveHelper();
	        this.worldObj.theProfiler.endSection();
		}
    }
	
	@Override
	public boolean canFly() {
		return true;
	}
	

}

