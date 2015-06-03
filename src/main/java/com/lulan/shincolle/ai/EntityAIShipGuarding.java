package com.lulan.shincolle.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.IShipGuardian;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;
/**SHIP GUARDING AI
 * CanFollow = false�ɥi�H����
 * ����T�u�Y�@�IfollowMax�椧��, �Z�����IfollowMax��H�W�N�|���զ^�h���I����a��followMin�椺
 * �Z���W�Lmax��W�L���w�ɶ���, �j��ǰe�^���I
 * �Yhost��gy<=0, �h�����������Howner, �|�]�wCanFollow = true
 */
public class EntityAIShipGuarding extends EntityAIBase {

    private IShipGuardian host;
    private EntityLiving host2;
    private Entity guarded;
    private ShipPathNavigate ShipNavigator;
    private int findCooldown;
    private int checkTeleport;	//> 200 = use teleport
    private double maxDistSq;
    private double minDistSq;
    private double distSq;
    private double distSqrt;
    private double distX, distY, distZ;	//��ؼЪ����u�Z��(������)
    private int gx, gy, gz;				//guard position (block position)

    
    public EntityAIShipGuarding(IShipGuardian entity) {
        this.host = entity;
        this.host2 = (EntityLiving) entity;
        this.ShipNavigator = entity.getShipNavigate();
        this.distSq = 1D;
        this.distSqrt = 1D;
        this.setMutexBits(7);  
    }
    
    //�P�w�O�_�}�l����AI
    public boolean shouldExecute() {
    	//�D���U, �D�M��, �D�Q�j, �D�i���H, �B��fuel�~����
    	if(host != null && !host.getIsSitting() && !host.getStateFlag(ID.F.NoFuel) && !host.getStateFlag(ID.F.CanFollow)) {
    		//check guarded entity
    		this.guarded = host.getGuarded();
    		
    		//get guard target
    		if(this.guarded != null) {
    			if(!this.guarded.isEntityAlive()) {
    				host.setGuarded(null);
    				return false;
    			}
    			else {
    				this.gx = (int) this.guarded.posX;
        			this.gy = (int) this.guarded.posY;
        			this.gz = (int) this.guarded.posZ;
    			}
    		}
    		else {
    			this.gx = host.getStateMinor(ID.N.GuardX);
        		this.gy = host.getStateMinor(ID.N.GuardY);
        		this.gz = host.getStateMinor(ID.N.GuardZ);
    		}
    		
    		//�Ygy=0, ��ܳoentity���l��, �٤������AI
    		if(gy <= 0) {
    			host.setStateFlag(ID.F.CanFollow, true);
    			return false;
    		}
    		else {
    			float fMin = host.getStateMinor(ID.N.FollowMin);
            	float fMax = host.getStateMinor(ID.N.FollowMax);
            	this.minDistSq = fMin * fMin;
                this.maxDistSq = fMax * fMax;
                
                //�p�⪽�u�Z��
            	this.distX = this.gx - this.host2.posX;
        		this.distY = this.gy - this.host2.posY;
        		this.distZ = this.gz - this.host2.posZ;
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
    	if(host != null) {
    		//�D���U, �D�M��, �D�Q�j, �D�i���H, �B��fuel�~����
    		if(!host.getIsSitting() && !host.getStateFlag(ID.F.NoFuel) && !host.getStateFlag(ID.F.CanFollow)) {
    			//�٨S���imin follow range, �~��
	        	if(this.distSq > this.minDistSq) {		
	        		return true;	//need update guard position
	        	}
	        	
	        	//��L���p
	        	return !ShipNavigator.noPath() || shouldExecute();
    		}
    		else {	//�Y�����U, �M��, �Q�j, �h���mAI
    			this.resetTask();
    			return false;
    		}
    	}
    	
    	return false;
    }

    public void startExecuting() {
        this.findCooldown = 20;
        this.checkTeleport = 0;
    }

    public void resetTask() {
    	this.guarded = null;
    	this.findCooldown = 20;
        this.checkTeleport = 0;
        this.ShipNavigator.clearPathEntity();
    }

    public void updateTask() {
    	if(host != null) {
//    		LogHelper.info("DEBUG : exec guarding");
        	this.findCooldown--;
        	
        	//update position every 30 ticks
        	if(host2.ticksExisted % 30 == 0) {
        		//check guarded entity
        		this.guarded = host.getGuarded();
        		
        		//get guard target
        		if(this.guarded != null) {
        			if(!this.guarded.isEntityAlive()) {
        				host.setGuarded(null);
        				this.resetTask();
        				return;
        			}
        			else {
        				this.gx = (int) this.guarded.posX;
            			this.gy = (int) this.guarded.posY;
            			this.gz = (int) this.guarded.posZ;
        			}
        		}
        		else {
        			this.gx = host.getStateMinor(ID.N.GuardX);
            		this.gy = host.getStateMinor(ID.N.GuardY);
            		this.gz = host.getStateMinor(ID.N.GuardZ);
        		}
        		
        		//�Ygy<=0, ��ܳoentity����w�I���u, ����Howner
        		if(gy <= 0) {
        			host.setStateFlag(ID.F.CanFollow, true);
        			this.resetTask();
        			return;
        		}
        		else {
        			float fMin = host.getStateMinor(ID.N.FollowMin);
                	float fMax = host.getStateMinor(ID.N.FollowMax);
                	this.minDistSq = fMin * fMin;
                    this.maxDistSq = fMax * fMax;
                    
                    //�p�⪽�u�Z��
                	this.distX = this.gx - this.host2.posX;
            		this.distY = this.gy - this.host2.posY;
            		this.distZ = this.gz - this.host2.posZ;
                	this.distSq = this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ;
        		}
        	}//end update
        	
        	//end move
        	if(this.distSq <= this.minDistSq) {
        		this.ShipNavigator.clearPathEntity();
        	}
        	
        	//�]�w�Y����V
            this.host2.getLookHelper().setLookPosition(gx, gy, gz, 30F, (float)this.host2.getVerticalFaceSpeed());

        	//�Z���W�L�ǰe�Z��, �����ǰe��ؼФW
        	if(this.distSq > this.maxDistSq && host2.dimension == host.getStateMinor(ID.N.GuardDim)) {
        		this.checkTeleport++;
        		
        		if(this.checkTeleport > 200) {
        			LogHelper.info("DEBUG : guarding AI: point away > 200 ticks, teleport entity");
        			this.checkTeleport = 0;
        			//teleport
    				this.host2.setLocationAndAngles(this.gx, this.gy + 0.5D, this.gz, this.host2.rotationYaw, this.host2.rotationPitch);
    				this.ShipNavigator.clearPathEntity();
                    return;
        		}
        	}
        	
        	//�Ccd���@�����|
        	if(this.findCooldown <= 0) {
    			this.findCooldown = 30;
    			
    			//check path result
            	if(!this.ShipNavigator.tryMoveToXYZ(gx, gy, gz, 1D)) {
            		LogHelper.info("DEBUG : guarding AI: fail to move, cannot reach or too far away");
            		//�Y�W�Lmax dist����240ticks, �hteleport
            		if(this.distSq > this.maxDistSq && host2.dimension == host.getStateMinor(ID.N.GuardDim)) {
            			this.checkTeleport++;	//�Y�Z���W�Lmax dist�B���ʤS����, �|��checkTP�C30 tick+1
                		
                		if(this.checkTeleport > 8) {
                			this.checkTeleport = 0;
                			//teleport
            				this.host2.setLocationAndAngles(this.gx, this.gy + 0.5D, this.gz, this.host2.rotationYaw, this.host2.rotationPitch);
            				this.ShipNavigator.clearPathEntity();
                            return;
                		}	
                    }
                }//end !try move
            }//end path find cooldown
    	}
    }
	
	
}
