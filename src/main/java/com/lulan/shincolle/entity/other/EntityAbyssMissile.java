package com.lulan.shincolle.entity.other;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipAttributes;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.CalcHelper;
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
 * speical type:
 * type 1:  high speed torpedo
 *   customAcc: 0.05~0.09
 * type 2:  railgun
 *   customAcc: >0.09
 * type 3:  cluster main
 *   customAcc: 0.02
 * type 4:  cluster sub
 *   customAcc: 0.02
 *   
 * 
 */
public class EntityAbyssMissile extends Entity implements IShipOwner, IShipAttributes {
	
    private IShipAttackBase host;	//main host type
    private EntityLiving host2;		//second host type: entity living
    private int playerUID;			//owner UID, for owner check
    
    //missile motion
    private boolean isDirect;		//false:parabola  true:direct
  
    //for parabola y position
    private float accParaY;			//�B�~y�b�[�t��
    private int midFlyTime;			//�@�b������ɶ�
   
    //for direct only
    private static final float ACC = 0.015F;
    private float acce;	//�w�]�[�t��
    private float accX;				//�T�b�[�t��
    private float accY;
    private float accZ;
    
    //missile attributes
    public int type;				//missile type
    private float atk;				//missile damage
    private float kbValue;			//knockback value
    private float missileHP;		//if hp = 0 -> onImpact
    private World world;
    
    //��constructor, size�����b���]�w
    public EntityAbyssMissile(World world) {
    	super(world);
    	this.setSize(1.0F, 1.0F);
    }
    
    /** for cluster sub missile */
    public EntityAbyssMissile(World world, IShipAttackBase host, float mX, float mY, float mZ, float pX, float pY, float pZ, float atk, float kbValue) {
        super(world);
        this.world = world;
//        LogHelper.info("DEBUG : const new missile "+pX+" "+pY+" "+pZ);
        //�]�whost��owner
        this.host = host;
        this.host2 = (EntityLiving) host;
        this.setPlayerUID(host.getPlayerUID());
        
        //set basic attributes
        this.atk = atk;
        this.kbValue  = kbValue;
        this.posX = pX;
        this.posY = pY;
        this.posZ = pZ;
        this.isDirect = false;
        this.type = 4;
        this.acce = ACC;
        this.accParaY = this.acce * 0.5F;
        
        if(mY > 0) mY = 0F;
        
        //acc and motion
        this.accX = mX * 0.1F;
	    this.accY = -this.acce;
	    this.accZ = mZ * 0.1F;
	    this.motionX = mX;
	    this.motionY = mY;
	    this.motionZ = mZ;
    }
    
    public EntityAbyssMissile(World world, IShipAttackBase host, float tarX, float tarY, float tarZ, float launchPos, float atk, float kbValue, boolean isDirect, float customAcc) {
        super(world);
        this.world = world;
//        LogHelper.info("DEBUG : const normal missile ");
        //�]�whost��owner
        this.host = host;
        this.host2 = (EntityLiving) host;
        this.setPlayerUID(host.getPlayerUID());
        
        //set basic attributes
        this.atk = atk;
        this.kbValue  = kbValue;
        this.posX = this.host2.posX;
        this.posZ = this.host2.posZ;
        this.posY = launchPos;
             
        //�p��Z��, ���o��Vvector, �åB��l�Ƴt��, �ϭ��u��V�¦V�ؼ�
        float distX = (float) (tarX - this.posX);
        float distY = (float) (tarY - this.posY);
        float distZ = (float) (tarZ - this.posZ);
        
        if(MathHelper.abs(distX) < 0.001F) distX = 0F;
        if(MathHelper.abs(distY) < 0.001F) distY = 0F;
        if(MathHelper.abs(distZ) < 0.001F) distZ = 0F;
        
        //�]�w���g�Ϊ̩ߪ��u
        this.isDirect = isDirect;
        this.type = 0;
        
        //�]�w���u�t��
        if(customAcc > 0F) {
        	this.acce = customAcc;
        	if(customAcc > 0.09F) {
        		this.type = 2;
        	}
        	else if(customAcc > 0.05F) {	//ro500, u511
        		this.type = 1;
        	}
        }
        else {
        	this.acce = ACC;
        }
        
        //check special type
        if(customAcc == -2F) {
        	this.type = 3;	//cluster main
        	LogHelper.info("DEBUG : const type 3 missile ");
        }
        
        //���g�u�D, no gravity
    	float dist = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
  	    this.accX = distX / dist * this.acce;
	    this.accY = distY / dist * this.acce;
	    this.accZ = distZ / dist * this.acce;
	    this.motionX = this.accX;
	    this.motionY = this.accY;
	    this.motionZ = this.accZ;
 
	    //�ߪ��u�y�D�p��, y�b��t�[�W (�@�b����ɶ� * �B�~y�b�[�t��)
	    if(!this.isDirect) {
	    	this.midFlyTime = (int) (0.5F * MathHelper.sqrt_float(2F * dist / this.acce));
	    	this.accParaY = this.acce;
	    	this.motionY = this.motionY + (double)this.midFlyTime * this.accParaY;
	    }
    }

    @Override
	protected void entityInit() {}

    /**
     * Checks if the entity is in range to render by using the past in distance and 
     * comparing it to its average bounding box edge length * 64 * renderDistanceWeight 
     * Args: distance
     * 
     * �ѩ�entity�i�ण��������, �G����������j�p�ӭp��Z��, ����k�w�]��256������j�p
     */
    @Override
	@SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distanceSq) {
        double d1 = this.boundingBox.getAverageEdgeLength() * 256D;
        return distanceSq < d1 * d1;
    }

    //update entity
    //�`�N: ���ʭn�bserver+client�����e���~����ܥ���, particle�h�u��bclient��
    @Override
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
    	
    	//cluster sub missile acc
    	if(this.type == 4) {
    		this.motionX *= 0.8F;
    		this.motionZ *= 0.8F;
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
        this.rotationPitch = (float)(Math.atan2(this.motionY, f1));
        this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ));    
        
        //�̷�x,z�b���t�V�ץ�����(��180)
        if(this.motionX > 0) {
        	this.rotationYaw -= Math.PI;
        }
        else {
        	this.rotationYaw += Math.PI;
        }
        
        //��s��m�����򥻸�T, �P�ɧ�sprePosXYZ
        super.onUpdate();
        
        /**********server side***********/
    	if(!this.worldObj.isRemote) {
    		//�S��host���, ���������u
    		if(this.host == null) {
    			this.setDead();	//�����ٮ�, ��Ĳ�o�z��
    			return;
    		}
    		
    		//�o�g�W�L10 sec, �]�w�����`(����), �`�Nserver restart�ᦹ�ȷ|�k�s
    		if(this.ticksExisted > 200) {
    			this.setDead();	//�����ٮ�, ��Ĳ�o�z��
    			return;
    		}
    		//sync missile type at start
    		else if(this.ticksExisted == 1) {
    			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48D);
    			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, this.type, 8), point);
    		}
    		
    		//spawn cluster sub missile
    		if(this.type == 3 && this.ticksExisted > 15) {
    			if(this.ticksExisted % 8 == 0) {
    				EntityAbyssMissile subm = new EntityAbyssMissile(this.worldObj, this.host, 
    						(float)this.motionX, (float)this.motionY, (float)this.motionZ, 
    						(float)this.posX, (float)this.posY - 1F, (float)this.posZ,
    		        		atk, kbValue);
    		        this.worldObj.spawnEntityInWorld(subm);
    			}
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
            Entity hitEntity = null;
            List hitList = null;
            hitList = this.worldObj.getEntitiesWithinAABB(Entity.class, this.boundingBox.expand(1.0D, 1.0D, 1.0D));
           
            //�j�Mlist, ��X�Ĥ@�ӥi�H�P�w���ؼ�, �Y�ǵ�onImpact
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) { 
                	hitEntity = (Entity) hitList.get(i);
                	
                	/**���|��ۤv�D�HĲ�o�z��
            		 * isEntityEqual() is NOT working
            		 * use entity id to check entity  */
                	if(hitEntity.canBeCollidedWith() && isNotHost(hitEntity) && 
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
    		case 1:
    			smokeType = 16;
    			break;
    		case 2:
    			smokeType = 27;
    			break;
    		case 3:
    		case 4:
    			smokeType = 18;
    			break;
    		default:
    			break;
    		}
    		
    		for (int j = 0; j < 3; ++j) {
            	ParticleHelper.spawnAttackParticleAt(this.posX-this.motionX*1.5D*j, this.posY+1D-this.motionY*1.5D*j, this.posZ-this.motionZ*1.5D*j, 
                		-this.motionX*0.1D, -this.motionY*0.1D, -this.motionZ*0.1D, smokeType);
    		}
    	}//end client side
    	   	
    }

    //check entity is not host or launcher
    private boolean isNotHost(Entity entity) {
		if(host2 != null) {
			//not launcher
			if(host2.getEntityId() == entity.getEntityId()) {
				return false;
			}
			//not friendly target (owner or same team)
			else if(entity instanceof IShipOwner) {
				if(((IShipOwner) entity).getPlayerUID() == this.getPlayerUID()) {
					return true;
				}
			}
		}

		return true;
	}

	//�����P�w�ɩI�s����k
    protected void onImpact(Entity target) {
    	//play sound
    	playSound(Reference.MOD_ID+":ship-explode", ConfigHandler.fireVolume * 1.5F, 0.7F / (this.rand.nextFloat() * 0.4F + 0.8F));
    	
    	//server side
    	if(!this.worldObj.isRemote) {
    		//set dead
        	this.setDead();
        	
    		float missileAtk = atk;

            //�p��d���z���ˮ`: �P�wbounding box���O�_���i�H�Y�ˮ`��entity
            Entity hitEntity = null;
            AxisAlignedBB impactBox = this.boundingBox.expand(3.5D, 3.5D, 3.5D); 
            List hitList = this.worldObj.getEntitiesWithinAABB(Entity.class, impactBox);
            
            //��list���Ҧ��i����entity���X�ˮ`�P�w
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) {
                	missileAtk = this.atk;
                	hitEntity = (Entity)hitList.get(i);
                	
                	//calc equip special dmg: AA, ASM
                	missileAtk = CalcHelper.calcDamageByEquipEffect(this, hitEntity, missileAtk, 1);
                	
                	//�ؼФ���O�ۤv or �D�H, �B�i�H�Q�I��
                	if(hitEntity.canBeCollidedWith() && isNotHost(hitEntity)) {
                		//�Yowner�ۦP, �h�ˮ`�]��0 (���O�̵MĲ�o�����S��)
                		if(EntityHelper.checkSameOwner(host2, hitEntity)) {
                    		missileAtk = 0F;
                    	}
                		else {
                			//calc critical, only for type:ship
                    		if(this.host != null && (this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_CRI))) {
                        		missileAtk *= 3F;
                        		//spawn critical particle
                        		TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
                            	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host2, 11, false), point);
                        	}
                    		
                    		//�Y�����쪱�a, �̤j�ˮ`�T�w��TNT�ˮ` (non-owner)
                        	if(hitEntity instanceof EntityPlayer) {
                        		missileAtk *= 0.25F;
                        		
                        		if(missileAtk > 59F) {
                        			missileAtk = 59F;	//same with TNT
                        		}
                        		
                        		//check friendly fire
                        		if(!EntityHelper.doFriendlyFire(this.host, (EntityPlayer) hitEntity)) {
                        			missileAtk = 0F;
                        		}
                        	}
                		}
//                		LogHelper.info("DEBUG: missile onImpact: dmg "+missileAtk+" tar "+hitEntity+" host "+this.host);
                		//if attack success
                	    if(hitEntity.attackEntityFrom(DamageSource.causeMobDamage(host2).setExplosion(), missileAtk)) {
                	    	//calc kb effect
                	        if(this.kbValue > 0) {
                	        	hitEntity.addVelocity(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue, 
                	                   0.1D, MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue);
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
        }//end if server side
    }

	//�x�sentity��nbt
    @Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
    	nbt.setTag("direction", this.newDoubleNBTList(new double[] {this.motionX, this.motionY, this.motionZ}));  
    	nbt.setFloat("atk", this.atk);
    }

    //Ū��entity��nbt
    @Override
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
    @Override
	public boolean canBeCollidedWith() {
        return true;
    }

    //���o��entity��bounding box�j�p
    @Override
	public float getCollisionBorderSize() {
        return 1.0F;
    }

    //entity�Q������ɩI�s����k
    @Override
	public boolean attackEntityFrom(DamageSource attacker, float atk) {
    	//�i��dodge�p��
		if(CalcHelper.canDodge(this, 0F)) {
			return false;
		}
    	
        if(this.isEntityInvulnerable()) {	//��L�ĥؼЦ^��false
            return false;
        }
        
        //�����쭸�u�|�ɭP�ߨ��z��
        if(this.isEntityAlive() && atk > 10F) {
        	this.setDead();
        	this.onImpact(null);
        	return true;
        }
        
        return false;
    }

    //render��, ���v�j�p
    @Override
	@SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    //�p����u��
    @Override
	public float getBrightness(float p_70013_1_) {
        return 1.0F;
    }

    //render��, �G�׭��ݩ�G����
    @Override
	@SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float p_70070_1_) {
        return 15728880;
    }
    
    public void setMissileType(int par1) {
    	this.type = par1;
    }

	@Override
	public int getPlayerUID() {
		return this.playerUID;
	}

	@Override
	public void setPlayerUID(int uid) {
		this.playerUID = uid;
	}

	@Override
	public Entity getHostEntity() {
		return this.host2;
	}

	@Override
	public float getEffectEquip(int id) {
		//dodge = 50%
		if(id == ID.EF_DODGE) return 50F;
		
		if(host != null) return host.getEffectEquip(id);
		return 0F;
	}

	
}
