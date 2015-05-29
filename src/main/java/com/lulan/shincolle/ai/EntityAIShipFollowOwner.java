package com.lulan.shincolle.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;
/**SHIP FOLLOW OWNER AI
 * �Z���W�Lmax dist��Ĳ�o����, ���쨫�imin dist�Z���ɰ���
 * �Z���W�LTP_DIST�|����teleport��owner����
 */
public class EntityAIShipFollowOwner extends EntityAIBase {

    private IShipAttackBase host;
    private EntityLiving host2;
    private EntityLivingBase owner;
    private static final double TP_DIST = 2048D;	//teleport condition ~ 45 blocks
    private ShipPathNavigate ShipNavigator;
    private int findCooldown;
    private int checkTeleport;	//> 40 = use teleport
    private double maxDistSq;
    private double minDistSq;
    private double distSq;
    private double distSqrt;
    
    //���u�e�i���\��
    private double distX, distY, distZ;	//��ؼЪ����u�Z��(������)
    private float rotYaw;

    
    public EntityAIShipFollowOwner(IShipAttackBase entity) {
        this.host = entity;
        this.host2 = (EntityLiving) entity;
        this.ShipNavigator = entity.getShipNavigate();
        this.distSq = 1D;
        this.distSqrt = 1D;
        this.setMutexBits(7);  
    }
    
    //��owner�B�ؼжW�Lmax dist��Ĳ�oAI, Ĳ�o�ᦹ��k���A����, �אּ�������cont exec
    public boolean shouldExecute() {
    	if(!host.getIsSitting() && !host.getIsRiding() && !host.getIsLeashed() && !host.getStateFlag(ID.F.NoFuel)) {
    		EntityLivingBase OwnerEntity = this.host.getPlayerOwner();

            //get owner distance
            if(OwnerEntity != null) {
            	this.owner = OwnerEntity;
                
            	if(this.owner.dimension != this.host2.dimension) {
            		return false;
            	}
            	
            	float fMin = host.getStateMinor(ID.N.FollowMin);
            	float fMax = host.getStateMinor(ID.N.FollowMax);
            	this.minDistSq = fMin * fMin;
                this.maxDistSq = fMax * fMax;

            	//�p�⪽�u�Z��
            	this.distX = this.owner.posX - this.host2.posX;
        		this.distY = this.owner.posY - this.host2.posY;
        		this.distZ = this.owner.posZ - this.host2.posZ;
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
    	//�D���U, �M��, �Q�j��ɥi�H�~�����AI
    	if(host != null) { 
    		if(!host.getIsSitting() && !host.getIsRiding() && !host.getIsLeashed()) {
	    		//�p�⪽�u�Z��
	        	this.distX = this.owner.posX - this.host2.posX;
	    		this.distY = this.owner.posY - this.host2.posY;
	    		this.distZ = this.owner.posZ - this.host2.posZ;
	        	this.distSq = this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ;
	        	
	        	//�Z���W�L�ǰe�Z��, �h�B�z�ǰe����
	        	if(this.distSq > this.TP_DIST) {
	        		return true;
	        	}
	
	        	//�٨S���imin follow range, �~��
	        	if(this.distSq > this.minDistSq ) {		
	        		return true;
	        	}
	        	
	        	//��L���p
	        	return shouldExecute();
    		}
    		else {	//�Y�����U, �M��, �Q�j, �h���mAI
    			this.resetTask();
    			return false;
    		}
    	}
    	
    	return false;
    }

    public void startExecuting() {
    	this.rotYaw = 0F;
        this.findCooldown = 20;
        this.checkTeleport = 0;
    }

    public void resetTask() {
        this.owner = null;
        this.ShipNavigator.clearPathEntity();
    }

    public void updateTask() {
    	if(host != null) {
//    		LogHelper.info("DEBUG : exec follow owner");
        	this.findCooldown--;
        	
        	//�]�w�Y����V
            this.host2.getLookHelper().setLookPositionWithEntity(this.owner, 10.0F, (float)this.host2.getVerticalFaceSpeed());

        	//�Z���W�L�ǰe�Z��, �����ǰe��ؼФW
        	if(this.distSq > this.TP_DIST) {
        		this.checkTeleport++;
        		
        		if(this.checkTeleport > 60) {
        			this.checkTeleport = 0;
        			//�ۦPdim�~�ǰe
        			LogHelper.info("DEBUG : follow AI: distSQ > "+this.TP_DIST+" , teleport entity. dim: "+host2.dimension+" "+owner.dimension);
        			if(this.host2.dimension == this.owner.dimension) {
        				this.host2.setLocationAndAngles(this.owner.posX, this.owner.posY + 1D, this.owner.posZ, this.host2.rotationYaw, this.host2.rotationPitch);
        				this.ShipNavigator.clearPathEntity();
                        return;
        			}
        		}
        	}
        	
        	//�Ccd���@�����|
        	if(this.findCooldown <= 0) {
    			this.findCooldown = 30;
    			
    			//check path result
            	if(!this.ShipNavigator.tryMoveToEntityLiving(this.owner, 1D)) {
            		LogHelper.info("DEBUG : follow AI: fail to follow, teleport entity");
            		if(this.distSq > this.TP_DIST) {
            			this.checkTeleport++;
                		
                		if(this.checkTeleport > 60) {
                			this.checkTeleport = 0;
                			//�ۦPdim�~�ǰe
                			LogHelper.info("DEBUG : follow AI: entity dimension "+host2.dimension+" "+owner.dimension);
                			if(this.host2.dimension == this.owner.dimension) {
                				this.host2.setLocationAndAngles(this.owner.posX, this.owner.posY + 1D, this.owner.posZ, this.host2.rotationYaw, this.host2.rotationPitch);
                				this.ShipNavigator.clearPathEntity();
                                return;
                			}
                		}	
                    }
                }//end !try move to owner
            }//end path find cooldown
    	}
    }
	
	
}