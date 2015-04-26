package com.lulan.shincolle.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipMount;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
/**SHIP FOLLOW OWNER AI
 * �Z���W�Lmax dist��Ĳ�o����, ���쨫�imin dist�Z���ɰ���
 * �Z���W�LTP_DIST�|����teleport��owner����
 */
public class EntityAIShipFollowOwner extends EntityAIBase {

    private BasicEntityShip host;
    private EntityLivingBase owner;
    World TheWorld;
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

    
    public EntityAIShipFollowOwner(BasicEntityShip entity) {
        this.host = entity;
        this.TheWorld = entity.worldObj;
        this.PetPathfinder = entity.getNavigator();
        this.distSq = 1D;
        this.distSqrt = 1D;
        this.setMutexBits(7);  
    }
    
    //��owner�B�ؼжW�Lmax dist��Ĳ�oAI, Ĳ�o�ᦹ��k���A����, �אּ�������cont exec
    public boolean shouldExecute() {
    	if(!host.isSitting() && !host.isRiding() && !host.getLeashed() && !host.getStateFlag(ID.F.NoFuel)) {
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
            	this.distX = this.owner.posX - this.host.posX;
        		this.distY = this.owner.posY - this.host.posY;
        		this.distZ = this.owner.posZ - this.host.posZ;
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
    	//�p�⪽�u�Z��
    	this.distX = this.owner.posX - this.host.posX;
		this.distY = this.owner.posY - this.host.posY;
		this.distZ = this.owner.posZ - this.host.posZ;
    	this.distSq = this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ;
    	
    	//�Z���W�L�ǰe�Z��
    	if(this.distSq > this.TP_DIST) {
    		return true;
    	}

    	if(this.distSq > this.minDistSq && !host.isSitting() && !host.isRiding()) {
    		if(this.host.getShipDepth() > 0D) {	//�Ω�G�餤����, �G�餤��path�g�`false
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
//    	LogHelper.info("DEBUG : exec follow owner");
    	this.findCooldown--;
    	this.motY = 0D;
    	
    	//�]�w�Y����V
        this.host.getLookHelper().setLookPositionWithEntity(this.owner, 10.0F, (float)this.host.getVerticalFaceSpeed());

        if(!this.host.isSitting() && !this.host.getLeashed()) {
        	//�Z���W�L�ǰe�Z��, �����ǰe��ؼФW
        	if(this.distSq > this.TP_DIST) {
        		this.host.posX = this.owner.posX;
        		this.host.posY = this.owner.posY + 1D;
        		this.host.posZ = this.owner.posZ;
        		this.host.setPosition(this.host.posX, this.host.posY, this.host.posZ);
        	}
        	
    		//�b�G�餤, �Ī��u����
        	if(this.host.getShipDepth() > 0D) {
        		//�B�~�[�Wy�b�t��, getPathToXYZ��Ů��G�����L��, �]��y�b�t�׭n�t�~�[
        		if(this.distY > 1.5D && this.host.getShipDepth() > 1.5D) {  //�קK�����u��
        			this.motY = 0.2F;
        		}
        		else if(this.distY < -1D) {
        			this.motY = -0.2F;
        		}
        		else {
	        		this.motY = 0F;
	        	}
        		
        		//�Y���u�i��, �h�������u����
        		if(this.host.getEntitySenses().canSee(this.owner)) {
        			double speed = this.host.getStateFinal(ID.MOV);
        			this.distSqrt = MathHelper.sqrt_double(this.distSq);
        			this.motX = (this.distX / this.distSqrt) * speed * 1D;
        			this.motZ = (this.distZ / this.distSqrt) * speed * 1D;
//        			LogHelper.info("DEBUG  : follow owner: "+motX+" "+motZ);
        			this.host.motionY = this.motY;
        			this.host.motionX = this.motX;
        			this.host.motionZ = this.motZ;
//        			this.host.getMoveHelper().setMoveTo(this.host.posX+this.motX, this.host.posY+this.motY, this.host.posZ+this.motZ, 1D);
        			
        			//���騤�׳]�w
        			float[] degree = EntityHelper.getLookDegree(motX, motY, motZ);
        			this.host.rotationYaw = degree[0];
        			this.host.rotationPitch = degree[1];
        			
        			//�Y��������F��, �h���ո���
	        		if(this.host.isCollidedHorizontally) {
	        			this.host.motionY += 0.2D;
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
            			LogHelper.info("DEBUG : follow AI: entity dimension "+host.dimension+" "+owner.dimension);
            			if(this.host.dimension == this.owner.dimension) {
            				this.host.setLocationAndAngles(this.owner.posX, this.owner.posY + 0.5D, this.owner.posZ, this.host.rotationYaw, this.host.rotationPitch);
                        	this.PetPathfinder.clearPathEntity();
                            return;
            			}
                    }
                }//end !try move to owner
            }//end path find cooldown
        }
    }
	
	
}