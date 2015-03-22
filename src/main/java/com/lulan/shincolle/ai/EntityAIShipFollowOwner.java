package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
/**SHIP FOLLOW OWNER AI
 * �|������Howner, ���|�]���䤣����|�N������H
 * �Z���W�Lmax dist��Ĳ�o����, ���쨫�imin dist�Z���ɰ���
 * �Z���W�L40��|����teleport��owner����
 * 
 * @parm entity, move speed, min dist, max dist
 */
public class EntityAIShipFollowOwner extends EntityAIBase {
    private BasicEntityShip ThePet;
    private EntityLivingBase TheOwner;
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
        this.ThePet = entity;
        this.TheWorld = entity.worldObj;
        this.PetPathfinder = entity.getNavigator();
        this.distSq = 1D;
        this.distSqrt = 1D;
        this.setMutexBits(7);  
    }

    //��owner�B�ؼжW�Lmax dist��Ĳ�oAI, Ĳ�o�ᦹ��k���A����, �אּ�������cont exec
    public boolean shouldExecute() {
    	if(!this.ThePet.isSitting() && !this.ThePet.getLeashed()) {
    		EntityLivingBase OwnerEntity = this.ThePet.getOwner();

            //get owner distance
            if(OwnerEntity != null) {
            	float fMin = ThePet.getStateMinor(ID.N.FollowMin);
            	float fMax = ThePet.getStateMinor(ID.N.FollowMax);
            	
            	this.TheOwner = OwnerEntity;
            	this.minDistSq = fMin * fMin;
                this.maxDistSq = fMax * fMax;

            	//�p�⪽�u�Z��
            	this.distX = this.TheOwner.posX - this.ThePet.posX;
        		this.distY = this.TheOwner.posY - this.ThePet.posY;
        		this.distZ = this.TheOwner.posZ - this.ThePet.posZ;
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
    	this.distX = this.TheOwner.posX - this.ThePet.posX;
		this.distY = this.TheOwner.posY - this.ThePet.posY;
		this.distZ = this.TheOwner.posZ - this.ThePet.posZ;
    	this.distSq = this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ;
    	
    	//�Z���W�L�ǰe�Z��
    	if(this.distSq > this.TP_DIST) {
    		return true;
    	}

    	if(this.distSq > this.minDistSq && !this.ThePet.isSitting()) {
    		if(this.ThePet.getShipDepth() > 0D) {	//�Ω�G�餤����, �G�餤��path�g�`false
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
        this.TheOwner = null;
        this.PetPathfinder.clearPathEntity();
    }

    public void updateTask() {
//    	LogHelper.info("DEBUG : exec follow owner");
    	this.findCooldown--;
    	this.motY = 0D;
    	
    	//�]�w�Y����V
        this.ThePet.getLookHelper().setLookPositionWithEntity(this.TheOwner, 10.0F, (float)this.ThePet.getVerticalFaceSpeed());

        if(!this.ThePet.isSitting() && !this.ThePet.getLeashed()) {
        	//�Z���W�L�ǰe�Z��, �����ǰe��ؼФW
        	if(this.distSq > this.TP_DIST) {
        		this.ThePet.posX = this.TheOwner.posX;
        		this.ThePet.posY = this.TheOwner.posY + 1D;
        		this.ThePet.posZ = this.TheOwner.posZ;
        		this.ThePet.setPosition(this.ThePet.posX, this.ThePet.posY, this.ThePet.posZ);
        	}
        	
    		//�b�G�餤, �Ī��u����
        	if(this.ThePet.getShipDepth() > 0D) {
        		//�B�~�[�Wy�b�t��, getPathToXYZ��Ů��G�����L��, �]��y�b�t�׭n�t�~�[
        		if(this.distY > 1.5D && this.ThePet.getShipDepth() > 1.5D) {  //�קK�����u��
        			this.motY = 0.2F;
        		}
        		else if(this.distY < -1D) {
        			this.motY = -0.2F;
        		}
        		else {
	        		this.motY = 0F;
	        	}
        		
        		//�Y���u�i��, �h�������u����
        		if(this.ThePet.getEntitySenses().canSee(this.TheOwner)) {
        			double speed = this.ThePet.getStateFinal(ID.MOV);
        			this.distSqrt = MathHelper.sqrt_double(this.distSq);
        			this.motX = (this.distX / this.distSqrt) * speed * 1D;
        			this.motZ = (this.distZ / this.distSqrt) * speed * 1D;
//        			LogHelper.info("DEBUG  : follow owner: "+motX+" "+motZ);
        			this.ThePet.motionY = this.motY;
        			this.ThePet.motionX = this.motX;
        			this.ThePet.motionZ = this.motZ;
//        			this.ThePet.getMoveHelper().setMoveTo(this.ThePet.posX+this.motX, this.ThePet.posY+this.motY, this.ThePet.posZ+this.motZ, 1D);
        			
        			//���騤�׳]�w
        			float[] degree = EntityHelper.getLookDegree(motX, motY, motZ);
        			this.ThePet.rotationYaw = degree[0];
        			this.ThePet.rotationPitch = degree[1];
        			
        			//�Y��������F��, �h���ո���
	        		if(this.ThePet.isCollidedHorizontally) {
	        			this.ThePet.motionY += 0.2D;
	        		}
        			return;
        		}
           	}
        	
        	//�Ccd���@�����|
        	if(this.findCooldown <= 0) {
    			this.findCooldown = 10;

            	if(!this.PetPathfinder.tryMoveToEntityLiving(this.TheOwner, 1D)) {
            		LogHelper.info("DEBUG : AI try move fail, teleport entity");
            		if(this.distSq > this.maxDistSq) {
            			//�ۦPdim�~�ǰe
            			LogHelper.info("DEBUG : Ai entity dim "+ThePet.dimension+" "+TheOwner.dimension);
            			if(this.ThePet.dimension == this.TheOwner.dimension) {
            				this.ThePet.setLocationAndAngles(this.TheOwner.posX, this.TheOwner.posY + 0.5D, this.TheOwner.posZ, this.ThePet.rotationYaw, this.ThePet.rotationPitch);
                        	this.PetPathfinder.clearPathEntity();
                            return;
            			}
                    }
                }//end !try move to owner
            }//end path find cooldown
        }
    }
	
	
}