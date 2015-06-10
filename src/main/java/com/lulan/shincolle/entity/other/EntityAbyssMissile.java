package com.lulan.shincolle.entity.other;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**ENTITY ABYSS MISSILE
 * @parm world, host entity, tarX, tarY, tarZ, damage, knockback value
 * 
 * Parabola Orbit(for distance 7~65)
 * �Ϊ��]�w:
 * �b�g�L�Z�����I���e, �[�W�B�~motionY�V�W�H��accY�V�U
 * �줤�I��, Vy = 0
 * 
 */
public class EntityAbyssMissile extends Entity {
	
    private IShipAttackBase host;	//main host type
    private EntityLiving host2;		//second host type: entity living
    private EntityLivingBase owner;
    
    //missile motion
    private float distX;			//target distance
    private float distY;
    private float distZ;
    private boolean isDirect;		//false:parabola  true:direct
  
    //for parabola y position
    private float accParaY;			//�B�~y�b�[�t��
    private int midFlyTime;			//�@�b������ɶ�
   
    //for direct only
    private float ACCE = 0.02F;		//�w�]�[�t��
    private float accX;				//�T�b�[�t��
    private float accY;
    private float accZ;
    
    //missile attributes
    public int type;				//missile type
    private float atk;				//missile damage
    private float kbValue;			//knockback value
    private float missileHP;		//if hp = 0 -> onImpact
    private boolean isTargetHurt;	//knockback flag
    private World world;

    
    //��constructor, size�����b���]�w
    public EntityAbyssMissile(World world) {
    	super(world);
    	this.setSize(1.0F, 1.0F);
    }
    
    public EntityAbyssMissile(World world, IShipAttackBase host, float tarX, float tarY, float tarZ, float launchPos, float atk, float kbValue, boolean isDirect, float customAcc) {
        super(world);
        this.world = world;
        
        //�]�whost��owner
        this.host = host;
        this.host2 = (EntityLiving) host;
        this.owner = this.host.getPlayerOwner();
        
        //set basic attributes
        this.atk = atk;
        this.kbValue  = kbValue;
        this.posX = this.host2.posX;
        this.posZ = this.host2.posZ;
        this.posY = launchPos;
             
        //�p��Z��, ���o��Vvector, �åB��l�Ƴt��, �ϭ��u��V�¦V�ؼ�
        this.distX = (float) (tarX - this.posX);
        this.distY = (float) (tarY - this.posY);
        this.distZ = (float) (tarZ - this.posZ);
        //�]�w���g�Ϊ̩ߪ��u
        this.isDirect = isDirect;
        this.type = 0;
        
        //�]�w���u�t��
        if(customAcc > 0F) {
        	this.ACCE = customAcc;
        	if(customAcc > 0.09F) {
        		this.type = 2;
        	}
        	else if(customAcc > 0.05F) {
        		this.type = 1;
        	}
        }
        else {
        	this.ACCE = 0.02F;
        }
        
        //���g�u�D, no gravity
    	float dist = MathHelper.sqrt_float(this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ);
  	    this.accX = (float) (this.distX / dist * this.ACCE);
	    this.accY = (float) (this.distY / dist * this.ACCE);
	    this.accZ = (float) (this.distZ / dist * this.ACCE);
	    this.motionX = this.accX;
	    this.motionZ = this.accY;
	    this.motionY = this.accZ;
 
	    //�ߪ��u�y�D�p��, y�b��t�[�W (�@�b����ɶ� * �B�~y�b�[�t��)
	    if(!this.isDirect) {
	    	this.midFlyTime = (int) (0.5F * MathHelper.sqrt_float(2F * dist / this.ACCE));
	    	this.accParaY = this.ACCE;
	    	this.motionY = this.motionY + (double)this.midFlyTime * this.accParaY;
	    }
    }

    protected void entityInit() {}

    /**
     * Checks if the entity is in range to render by using the past in distance and 
     * comparing it to its average bounding box edge length * 64 * renderDistanceWeight 
     * Args: distance
     * 
     * �ѩ�entity�i�ण��������, �G����������j�p�ӭp��Z��, ����k�w�]��256������j�p
     */
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distanceSq) {
        double d1 = this.boundingBox.getAverageEdgeLength() * 256D;
        return distanceSq < d1 * d1;
    }

    //update entity
    //�`�N: ���ʭn�bserver+client�����e���~����ܥ���, particle�h�u��bclient��
    public void onUpdate() {
    	/**********both side***********/
    	//�N��m��s (�]�tserver, client���P�B��m, �~���bounding box�B�@���`)
        this.setPosition(this.posX, this.posY, this.posZ);

        //�p��o�g�骺����
    	if(!this.isDirect) {  //���g�y�D�p��  	
			this.motionY = this.motionY + this.accY - this.accParaY;                   
    	}
    	else {
    		this.motionY += this.accY;
    	}
    	
    	//�p��next tick���t��
        this.motionX += this.accX;
        this.motionZ += this.accZ;
        
    	//�]�w�o�g�骺�U�@�Ӧ�m
		this.posX += this.motionX;
		this.posY += this.motionY;
        this.posZ += this.motionZ;
           	
    	//�p��ҫ��n�઺���� (RAD, not DEG)
        float f1 = MathHelper.sqrt_double(this.motionX*this.motionX + this.motionZ*this.motionZ);
        this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f1));
        this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ));    
        
        //�̷�x,z�b���t�V�ץ�����(��180)
        if(this.distX > 0) {
        	this.rotationYaw -= Math.PI;
        }
        else {
        	this.rotationYaw += Math.PI;
        }
        
        //��s��m�����򥻸�T, �P�ɧ�sprePosXYZ
        super.onUpdate();
        
        /**********server side***********/
    	if(!this.worldObj.isRemote) {
    		//�o�g�W�L20 sec, �]�w�����`(����), �`�Nserver restart�ᦹ�ȷ|�k�s
    		if(this.ticksExisted > 600) {
    			this.setDead();	//�����ٮ�, ��Ĳ�o�z��
    		}
    		else if(this.ticksExisted == 1) {
    			//for other player, send ship state for display
    			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48D);
    			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, this.type, 8), point);
    		}
    		
    		//�S��host���, ���������u
    		if(this.host == null) {
    			this.setDead();	//�����ٮ�, ��Ĳ�o�z��
    			return;
    		}
    		
    		//�Ӧ�m�I����, �h�]�w�z�� (��k1: �����ήy�Ч���) ����k�ѩ��y�Ш�int, �ܦh�ɭԬݰ_�Ӧ�������O�̵M�줣����
    		if(!this.worldObj.blockExists((int)this.posX, (int)this.posY, (int)this.posZ)) {
    			this.onImpact(null);
    		}
    		
    		//�Ӧ�m�I����, �h�]�w�z�� (��k2: ��raytrace����)
    		Vec3 vec3 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            Vec3 vec31 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            MovingObjectPosition movingobjectposition = this.worldObj.rayTraceBlocks(vec3, vec31);          
            vec3 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            vec31 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

            if(movingobjectposition != null) {
                vec31 = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
                this.onImpact(null);
            }
            
            //�P�wbounding box���O�_���i�HĲ�o�z����entity
            EntityLivingBase hitEntity = null;
            List hitList = null;
            hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(1.0D, 1.0D, 1.0D));
           
            //�j�Mlist, ��X�Ĥ@�ӥi�H�P�w���ؼ�, �Y�ǵ�onImpact
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) { 
                	hitEntity = (EntityLivingBase)hitList.get(i);
                	
                	/**���|��ۤv�D�HĲ�o�z��
            		 * isEntityEqual() is NOT working
            		 * use entity id to check entity  */
                	if(hitEntity.canBeCollidedWith() && isNotHost(hitEntity.getEntityId()) && 
                	   !EntityHelper.checkSameOwner(host2, hitEntity)) {
                		break;	//get target entity
                	}
                	else {
            			hitEntity = null;
            		}
                }
            }
            
            //call onImpact
            if(hitEntity != null) {
            	this.onImpact(hitEntity);
            } 
            
    	}//end server side
    	/**********client side***********/
    	else {
    		//spawn particle by speed type
    		byte smokeType = 15;
    		
    		switch(this.type) {
    		default:
    			break;
    		case 1:
    			smokeType = 16;
    			break;
    		case 2:
    			smokeType = 27;
    			break;
    		}
    		
    		for (int j = 0; j < 3; ++j) {
            	ParticleHelper.spawnAttackParticleAt(this.posX-this.motionX*1.5D*j, this.posY+1D-this.motionY*1.5D*j, this.posZ-this.motionZ*1.5D*j, 
                		-this.motionX*0.5D, -this.motionY*0.5D, -this.motionZ*0.5D, smokeType);
    		}
    	}//end client side
    	   	
    }

    //check entity is not host itself
    private boolean isNotHost(int eid) {
		if(host2 != null && host2.getEntityId() == eid) {
			return false;
		} 	
		return true;
	}

	//�����P�w�ɩI�s����k
    protected void onImpact(EntityLivingBase target) {
    	isTargetHurt = false;
    	
    	//play sound
    	playSound(Reference.MOD_ID+":ship-explode", ConfigHandler.fireVolume * 1.5F, 0.7F / (this.rand.nextFloat() * 0.4F + 0.8F));
    	
    	//server side
    	if(!this.worldObj.isRemote) {
    		float missileAtk = atk;
    		EntityLivingBase host3 = null;
    		
    		if(host2 instanceof BasicEntityMount || host2 instanceof BasicEntityAirplane) {
        		host3 = host.getOwner();
    		}
    		
            if(target != null) {	//����entity�ް_�z��
            	//�Y������P�}��entity (ex: owner), �h�ˮ`�]��0 (���O�̵MĲ�o�����S��)
            	if(EntityHelper.checkSameOwner(host2, target)) {
            		missileAtk = 0F;
            	}
            	
            	//calc critical, only for type:ship
            	if(this.host != null && (this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_CRI))) {
            		missileAtk *= 3F;
            		//spawn critical particle
            		TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
                	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host2, 11, false), point);
            	}
            	
            	//�Y�����쪱�a, �̤j�ˮ`�T�w��TNT�ˮ` (non-owner)
            	if(target instanceof EntityPlayer) {
            		missileAtk *= 0.25F;
            		
            		if(missileAtk > 59F) {
            			missileAtk = 59F;	//same with TNT
            		}
            		
            		//check friendly fire if host is not mob
            		if(EntityHelper.checkOwnerIsPlayer(host2) && !ConfigHandler.friendlyFire) {
            			missileAtk = 0F;
            		}
            	}

            	//�]�w��entity���쪺�ˮ`
        		if(host3 != null) {
        			isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(host3).setExplosion(), missileAtk);
        		}
            	else {
            		isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(host2).setExplosion(), missileAtk);
            	}
            	
            	//if attack success
        	    if(isTargetHurt) {
        	    	//calc kb effect
        	        if(this.kbValue > 0) {
        	        	target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
        	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
        	            motionX *= 0.6D;
        	            motionZ *= 0.6D;
        	        }             	 
        	    }
            }
            
            //�p��d���z���ˮ`: �P�wbounding box���O�_���i�H�Y�ˮ`��entity
            EntityLivingBase hitEntity = null;
            AxisAlignedBB impactBox = this.boundingBox.expand(3.5D, 3.5D, 3.5D); 
            List hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, impactBox);
            
            //�j�Mlist, ��X�Ĥ@�ӥi�H�P�w���ؼ�, �Y�ǵ�onImpact
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) {
                	missileAtk = this.atk;
                	hitEntity = (EntityLivingBase)hitList.get(i);
                	
                	//�ؼФ���O�ۤv or �D�H
                	if(hitEntity.canBeCollidedWith() && 
                	   isNotHost(hitEntity.getEntityId())) {

            			//calc critical, only for type:ship
                		if(this.host != null && (this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_CRI))) {
                    		missileAtk *= 3F;
                    		//spawn critical particle
                    		TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
                        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host2, 11, false), point);
                    	}
                		
                		//�Y������P�}��entity (ex: owner), �h�ˮ`�]��0 (���O�̵MĲ�o�����S��)
                		if(EntityHelper.checkSameOwner(host2, hitEntity)) {
                    		missileAtk = 0F;
                    	}
                		
                		//�Y�����쪱�a, �̤j�ˮ`�T�w��TNT�ˮ` (non-owner)
                    	if(hitEntity instanceof EntityPlayer) {
                    		missileAtk *= 0.25F;
                    		
                    		if(missileAtk > 59F) {
                    			missileAtk = 59F;	//same with TNT
                    		}
                    		
                    		//check friendly fire
                    		if(EntityHelper.checkOwnerIsPlayer(host2) && !ConfigHandler.friendlyFire) {
                    			missileAtk = 0F;
                    		}
                    	}
                		//��entity�y���ˮ`
                    	if(host3 != null) {
                    		isTargetHurt = hitEntity.attackEntityFrom(DamageSource.causeMobDamage(host3).setExplosion(), missileAtk);
                    	}
                    	else {
                    		isTargetHurt = hitEntity.attackEntityFrom(DamageSource.causeMobDamage(host2).setExplosion(), missileAtk);
                    	}
                		//if attack success
                	    if(isTargetHurt) {
                	    	//calc kb effect
                	        if(this.kbValue > 0) {
                	        	hitEntity.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
                	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
                	            motionX *= 0.6D;
                	            motionZ *= 0.6D;
                	        }             	 
                	    }
                	}//end can be collided with
                }//end hit target list for loop
            }//end hit target list != null
  
            //send packet to client for display partical effect
            TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 2, false), point);
            
            this.setDead();
        }//end if server side
    }

	//�x�sentity��nbt
    public void writeEntityToNBT(NBTTagCompound nbt) {
    	nbt.setTag("direction", this.newDoubleNBTList(new double[] {this.motionX, this.motionY, this.motionZ}));  
    	nbt.setFloat("atk", this.atk);
    }

    //Ū��entity��nbt
    public void readEntityFromNBT(NBTTagCompound nbt) {
        if(nbt.hasKey("direction", 9)) {	//9��tag list
            NBTTagList nbttaglist = nbt.getTagList("direction", 6);	//6��tag double
            this.motionX = nbttaglist.func_150309_d(0);	//����get double
            this.motionY = nbttaglist.func_150309_d(1);
            this.motionZ = nbttaglist.func_150309_d(2);
        }
        else {
            this.setDead();
        }
        
        this.atk = nbt.getFloat("atk");
    }

    //�]�wtrue�i�Ϩ�L�ͪ��P�w�O�_�n�{�}��entity
    public boolean canBeCollidedWith() {
        return true;
    }

    //���o��entity��bounding box�j�p
    public float getCollisionBorderSize() {
        return 1.0F;
    }

    //entity�Q������ɩI�s����k
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        if(this.isEntityInvulnerable()) {	//��L�ĥؼЦ^��false
            return false;
        }
        
        //�����쭸�u�|�ɭP�ߨ��z��
        this.onImpact(null);
        return true;
    }

    //render��, ���v�j�p
    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    //�p����u��
    public float getBrightness(float p_70013_1_) {
        return 1.0F;
    }

    //render��, �G�׭��ݩ�G����
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float p_70070_1_) {
        return 15728880;
    }
    
    public void setMissileType(int par1) {
    	this.type = par1;
    }
    
}
