package com.lulan.shincolle.entity.other;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
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
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Beam entity
 *  fly to target, create beam particle between host and target
 *  damage everything on the way
 *  setDead after X ticks
 */
public class EntityProjectileBeam extends Entity implements IShipOwner, IShipAttributes{

	//host data
	private IShipAttackBase host;	//main host type
    private Entity host2;			//second host type: entity living
    private int playerUID;			//owner UID, for owner check
    
    //beam data
	private int type, lifeLength;
	private float atk, kbValue;
	private float acc, accX, accY, accZ;
	

	public EntityProjectileBeam(World world) {
		super(world);
		this.ignoreFrustumCheck = true;  //always render
		this.noClip = true;				 //can't block
		this.stepHeight = 0F;
		this.setSize(1F, 1F);
	}
	
	public EntityProjectileBeam(World world, IShipAttackBase host, int type, float ax, float ay, float az, float atk, float kb) {
		super(world);
		this.ignoreFrustumCheck = true;  //always render
		this.noClip = true;				 //can't block
		this.stepHeight = 0F;
		this.setSize(1F, 1F);
		
		//host
		this.host = host;
		this.host2 = (Entity) host;
		this.playerUID = host.getPlayerUID();
		
		//type
		this.type = type;
		
		switch(type) {
		case 1:   //boss beam
			this.setPosition(host2.posX + ax, host2.posY + host2.height * 0.5D, host2.posZ + az);
			this.lifeLength = 31;
			this.acc = 4F;
			break;
		default:  //normal beam
			this.setPosition(host2.posX + ax, host2.posY + host2.height * 0.5D, host2.posZ + az);
			this.lifeLength = 31;
			this.acc = 4F;
			break;
		}
		
		//beam data
		this.accX = ax * acc;
		this.accY = ay * acc;
		this.accZ = az * acc;
		this.atk = atk;
		this.kbValue = kb;
		
	}

	@Override
	public float getEffectEquip(int id) {
		if(host != null) return host.getEffectEquip(id);
		return 0;
	}

	@Override
	public int getPlayerUID() {
		return this.playerUID;
	}

	@Override
	public void setPlayerUID(int uid) {}

	@Override
	public Entity getHostEntity() {
		return host2;
	}

	@Override
	protected void entityInit() {}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		nbt.setTag("speed", this.newDoubleNBTList(new double[] {this.accX, this.accY, this.accZ}));  
    	nbt.setFloat("atk", this.atk);
    	nbt.setFloat("kb", this.kbValue);
    	nbt.setInteger("type", this.type);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		if(nbt.hasKey("speed", 9)) {  //9��tag list
            NBTTagList nbttaglist = nbt.getTagList("speed", 6);  //6��tag double
            this.accX = (float) nbttaglist.func_150309_d(0);	//����get double
            this.accY = (float) nbttaglist.func_150309_d(1);
            this.accZ = (float) nbttaglist.func_150309_d(2);
        }
        else {
        	this.accX = 0F;
            this.accY = 0F;
            this.accZ = 0F;
        }
        
        this.atk = nbt.getFloat("atk");
        this.kbValue = nbt.getFloat("kb");
        this.type = nbt.getInteger("type");
	}
	
	@Override
	public boolean isEntityInvulnerable() {
        return true;
    }
	
	@Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0F;
    }
    
    public void setProjectileType(int par1) {
    	this.type = par1;
    }

    @Override
	public void onUpdate() {
    	/**************** BOTH SIDE ******************/
    	//set speed
    	this.motionX = this.accX;
    	this.motionY = this.accY;
    	this.motionZ = this.accZ;
    	
    	//set position
    	this.setPosition(this.posX, this.posY, this.posZ);
		this.posX += this.motionX;
		this.posY += this.motionY;
        this.posZ += this.motionZ;
        super.onUpdate();
        
        /*************** SERVER SIDE *****************/
        if(!this.worldObj.isRemote) {
        	//check life
        	if(this.ticksExisted > this.lifeLength || this.host == null) {
        		this.setDead();
        		return;
        	}
        	
        	//sync beam type at start
    		if(this.ticksExisted == 1) {
    			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
    			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, this.type, S2CEntitySync.PID.SyncProjectile), point);
    		}
        	
    		//�P�wbounding box���O�_���i�HĲ�o�z����entity
            Entity hitEntity = null;
            List hitList = null;
            hitList = this.worldObj.getEntitiesWithinAABB(Entity.class, this.boundingBox.expand(1.5D, 1.5D, 1.5D));
            
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
            	this.onImpact();
            }
        }
        /*************** CLIENT SIDE *****************/
        else {
    		//spawn beam head particle
        	ParticleHelper.spawnAttackParticleAtEntity(this, 0D, 32 - this.ticksExisted, 0D, (byte)4);
        }
    }
    
    @Override
	public boolean attackEntityFrom(DamageSource attacker, float atk) {
    	return false;
    }
    
	//�����P�w�ɩI�s����k
    protected void onImpact() {
    	//play sound
    	playSound(Reference.MOD_ID+":ship-explode", ConfigHandler.fireVolume * 1.5F, 0.7F / (this.rand.nextFloat() * 0.4F + 0.8F));
    	
    	//server side
    	if(!this.worldObj.isRemote) {
    		float beamAtk = atk;

            //�p��d���z���ˮ`: �P�wbounding box���O�_���i�H�Y�ˮ`��entity
            Entity hitEntity = null;
            AxisAlignedBB impactBox;
            
            if(this.type == 1) {  //boss beam
            	impactBox = this.boundingBox.expand(3D, 3D, 3D);
            }
            else {
            	impactBox = this.boundingBox.expand(1.5D, 1.5D, 1.5D);
            }
            
            List hitList = this.worldObj.getEntitiesWithinAABB(Entity.class, impactBox);
            TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
            
            //��list���Ҧ��i����entity���X�ˮ`�P�w
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) {
                	beamAtk = this.atk;
                	hitEntity = (Entity)hitList.get(i);
                	
                	//check target attackable
              		if(EntityHelper.checkAttackable(hitEntity)) {
              			//calc equip special dmg: AA, ASM
                    	beamAtk = CalcHelper.calcDamageByEquipEffect(this, hitEntity, beamAtk, 1);
                    	
                    	//�ؼФ���O�ۤv or �D�H, �B�i�H�Q�I��
                    	if(hitEntity.canBeCollidedWith() && isNotHost(hitEntity)) {
                    		//�Yowner�ۦP, �h�ˮ`�]��0 (���O�̵MĲ�o�����S��)
                    		if(EntityHelper.checkSameOwner(host2, hitEntity)) {
                    			beamAtk = 0F;
                        	}
                    		else {
                    			//calc critical
                        		if(this.host != null && (this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_CRI))) {
                        			beamAtk *= 3F;
                            		//spawn critical particle
                                	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host2, 11, false), point);
                            	}
                        		
                        		//�Y�����쪱�a, �̤j�ˮ`�T�w��TNT�ˮ` (non-owner)
                            	if(hitEntity instanceof EntityPlayer) {
                            		beamAtk *= 0.25F;
                            		
                            		if(beamAtk > 59F) {
                            			beamAtk = 59F;	//same with TNT
                            		}
                            	}
                            	
                            	//check friendly fire
                        		if(!EntityHelper.doFriendlyFire(this.host, hitEntity)) {
                        			beamAtk = 0F;
                        		}
                    		}
                    		
                    		//if attack success
                    	    if(hitEntity.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, host2).setExplosion(), beamAtk)) {
                    	    	//calc kb effect
                    	        if(this.kbValue > 0) {
                    	        	hitEntity.addVelocity(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue, 
                    	                   0.1D, MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue);
                    	            motionX *= 0.6D;
                    	            motionZ *= 0.6D;
                    	        }
                    	        
                    	        //send packet to client for display partical effect
                                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(hitEntity, 9, false), point);
                    	    }
                    	}//end can be collided with
              		}//end is attackable
                }//end hit target list for loop
            }//end hit target list != null
    	}//end if server side
    }
    
	//check entity is not host or launcher
    private boolean isNotHost(Entity entity) {
    	//not self
    	if(entity.equals(this)) return false;
    	//not launcher
		if(host2 != null && host2.getEntityId() == entity.getEntityId()) {
			return false;
		}

		return true;
	}

    
}
