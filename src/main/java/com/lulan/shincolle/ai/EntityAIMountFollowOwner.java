package com.lulan.shincolle.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.MathHelper;

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
	private IShipFloating mountF;
	private EntityLiving mount;
    private BasicEntityShip host;
    private EntityLivingBase owner;
    private static final double TP_DIST = 2048D;	//teleport condition ~ 45 blocks
    private PathNavigate PetPathfinder;
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
    	this.mountF = (IShipFloating) entity;
    	this.mount = (EntityLiving) entity;
        this.host = (BasicEntityShip) entity.getRiddenByEntity();
        this.PetPathfinder = mount.getNavigator();
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
    		
    		if(!this.PetPathfinder.noPath()) {		//�Ω󳰤W����, path find�i���`�B�@
    			return true;
    		}
    	}
    	return false;
    }

    public void startExecuting() {
    	this.rotYaw = 0F;
        this.findCooldown = 0;
        this.PetPathfinder.setAvoidsWater(false);
        this.PetPathfinder.setEnterDoors(true);
        this.PetPathfinder.setCanSwim(true);
    }

    public void resetTask() {
        this.owner = null;
        this.PetPathfinder.clearPathEntity();
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
        	
    		//�b�G�餤, �Ī��u����
        	if(this.mount.isInWater()) {
        		//�B�~�[�Wy�b�t��, getPathToXYZ��Ů��G�����L��, �]��y�b�t�׭n�t�~�[
        		if(this.distY > 1.5D && this.mountF.getShipDepth() > 1.5D) {  //�קK�����u��
        			this.motY = 0.2F;
        		}
        		else if(this.distY < -1D) {
        			this.motY = -0.2F;
        		}
        		else {
	        		this.motY = 0F;
	        	}
        		
        		//�Y���u�i��, �h�������u����
        		if(this.mount.getEntitySenses().canSee(this.owner)) {
        			double speed = this.host.getStateFinal(ID.MOV);
        			this.distSqrt = MathHelper.sqrt_double(this.distSq);
        			this.motX = (this.distX / this.distSqrt) * speed * 1D;
        			this.motZ = (this.distZ / this.distSqrt) * speed * 1D;
//        			LogHelper.info("DEBUG  : follow owner: "+motX+" "+motZ);
        			this.mount.motionY = this.motY;
        			this.mount.motionX = this.motX;
        			this.mount.motionZ = this.motZ;
//        			this.host.getMoveHelper().setMoveTo(this.host.posX+this.motX, this.host.posY+this.motY, this.host.posZ+this.motZ, 1D);
        			
        			//���騤�׳]�w
        			float[] degree = EntityHelper.getLookDegree(motX, motY, motZ);
        			this.mount.rotationYaw = degree[0];
        			this.mount.rotationPitch = degree[1];
        			
        			//�Y��������F��, �h���ո���
	        		if(this.mount.isCollidedHorizontally) {
	        			this.mount.motionY += 0.6D;
	        		}
        			return;
        		}
           	}
        	
        	//�Ccd���@�����|
        	if(this.findCooldown <= 0) {
    			this.findCooldown = 20;

            	if(!this.PetPathfinder.tryMoveToEntityLiving(this.owner, 1D)) {
            		LogHelper.info("DEBUG : follow AI: fail to move, teleport entity");
            		if(this.distSq > this.maxDistSq) {
            			//�ۦPdim�~�ǰe
            			LogHelper.info("DEBUG : follow AI: entity dimension "+mount.dimension+" "+owner.dimension);
            			if(this.mount.dimension == this.owner.dimension) {
            				this.mount.setLocationAndAngles(this.owner.posX, this.owner.posY + 0.5D, this.owner.posZ, this.mount.rotationYaw, this.mount.rotationPitch);
                        	this.PetPathfinder.clearPathEntity();
                            return;
            			}
                    }
                }//end !try move to owner
            }//end path find cooldown
        }
    }
	
	
}
