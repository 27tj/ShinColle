package com.lulan.shincolle.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.MathHelper;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipFloating;
import com.lulan.shincolle.entity.IShipMount;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

/**MOUNT FOLLOW OWNER AI
 */
public class EntityAIMountFollowOwner extends EntityAIBase {

	private IShipMount mountM;
	private EntityLiving mount;
    private BasicEntityShip host;
    private EntityLivingBase owner;
    private static final double TP_DIST = 2048D;	//teleport condition ~ 45 blocks
//    private PathNavigate PetPathfinder;
    private ShipPathNavigate ShipPathfinder;
    private int findCooldown;
    private double maxDistSq;
    private double minDistSq;
    private double distSq;
    private double distSqrt;
    
    //���u�e�i���\��
    private double distX, distY, distZ, motX, motY, motZ;	//��ؼЪ����u�Z��(������)
    private float rotYaw;

    
    public EntityAIMountFollowOwner(IShipMount entity) {
    	this.mountM = entity;
    	this.mount = (EntityLiving) entity;
        this.host = (BasicEntityShip) entity.getRiddenByEntity();
//        this.PetPathfinder = mount.getNavigator();
        this.ShipPathfinder = mountM.getShipNavigate();
        this.distSq = 1D;
        this.distSqrt = 1D;
        this.setMutexBits(7);
    }
    
    //��owner�B�ؼжW�Lmax dist��Ĳ�oAI, Ĳ�o�ᦹ��k���A����, �אּ�������cont exec
    public boolean shouldExecute() {
    	//set host, dunno why host always reset to null
    	if(mountM.getRiddenByEntity() == null) {
    		return false;
    	}
    	else {
    		host = (BasicEntityShip) mountM.getRiddenByEntity();
    	}
    	
    	if(!host.isSitting() && !host.getLeashed() && !mount.getLeashed() && !host.getStateFlag(ID.F.NoFuel)) {
    		EntityLivingBase OwnerEntity = this.host.getOwner();

            //get owner distance
            if(OwnerEntity != null) {
            	this.owner = OwnerEntity;
                
            	if(this.owner.dimension != this.host.dimension) {
            		return false;
            	}
            	
            	float fMin = host.getStateMinor(ID.N.FollowMin);
            	float fMax = host.getStateMinor(ID.N.FollowMax);
            	this.minDistSq = fMin * fMin;
                this.maxDistSq = fMax * fMax;

            	//�p�⪽�u�Z��
            	this.distX = this.owner.posX - this.mount.posX;
        		this.distY = this.owner.posY - this.mount.posY;
        		this.distZ = this.owner.posZ - this.mount.posZ;
            	this.distSq = this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ;

            	if(distSq > this.maxDistSq) {
            		return true;
            	}
            }
    	} 
        
        return false;
    }

    //�ؼ��٨S����min dist�Ϊ̶Z���W�LTP_DIST���~��AI
    public boolean continueExecuting() {
    	//set host, dunno why host always reset to null
    	if(mountM.getRiddenByEntity() == null) {
    		return false;
    	}
    	else {
    		host = (BasicEntityShip) mountM.getRiddenByEntity();
    	}
    	
    	//�p�⪽�u�Z��
    	this.distX = this.owner.posX - this.mount.posX;
		this.distY = this.owner.posY - this.mount.posY;
		this.distZ = this.owner.posZ - this.mount.posZ;
    	this.distSq = this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ;
    	
    	//�Z���W�L�ǰe�Z��
    	if(this.distSq > this.TP_DIST) {
    		return true;
    	}

    	if(this.distSq > this.minDistSq && !this.host.isSitting()) {
    		if(this.mount.isInWater()) {	//�Ω�G�餤����, �G�餤��path�g�`false
    			return true;
    		}
    		
    		if(!this.ShipPathfinder.noPath()) {		//�Ω󳰤W����, path find�i���`�B�@
    			return true;
    		}
    	}
    	return shouldExecute();
    }

    public void startExecuting() {
    	this.rotYaw = 0F;
        this.findCooldown = 20;
    }

    public void resetTask() {
        this.owner = null;
        this.ShipPathfinder.clearPathEntity();
    }

    public void updateTask() {
    	//set host, dunno why host always reset to null
    	if(mountM.getRiddenByEntity() == null) {
    		return;
    	}
    	else {
    		host = (BasicEntityShip) mountM.getRiddenByEntity();
    	}
//    	LogHelper.info("DEBUG : exec follow owner");
    	this.findCooldown--;
    	this.motY = 0D;
    	
    	//�]�w�Y����V
        this.host.getLookHelper().setLookPositionWithEntity(this.owner, 10.0F, (float)this.host.getVerticalFaceSpeed());

        if(!this.host.isSitting() && !this.host.getLeashed()) {
        	//�Z���W�L�ǰe�Z��, �����ǰe��ؼФW
        	if(this.distSq > this.TP_DIST) {
        		this.mount.posX = this.owner.posX;
        		this.mount.posY = this.owner.posY + 1D;
        		this.mount.posZ = this.owner.posZ;
        		this.mount.setPosition(this.mount.posX, this.mount.posY, this.mount.posZ);
        	}

        	//�Ccd���@�����|
        	if(this.findCooldown <= 0) {
    			this.findCooldown = 20;

            	if(!this.ShipPathfinder.tryMoveToEntityLiving(this.owner, 1D)) {
            		LogHelper.info("DEBUG : mount follow AI: mount fail to follow, teleport entity");
            		if(this.distSq > this.maxDistSq) {
            			//�ۦPdim�~�ǰe
            			LogHelper.info("DEBUG : follow AI: entity dimension "+mount.dimension+" "+owner.dimension);
            			if(this.mount.dimension == this.owner.dimension) {
            				this.mount.setLocationAndAngles(this.owner.posX, this.owner.posY + 0.5D, this.owner.posZ, this.mount.rotationYaw, this.mount.rotationPitch);
                        	this.ShipPathfinder.clearPathEntity();
                            return;
            			}
                    }
                }//end !try move to owner
            }//end path find cooldown
        }
    }
	
	
}
