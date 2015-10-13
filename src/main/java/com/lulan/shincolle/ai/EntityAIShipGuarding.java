package com.lulan.shincolle.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayerMP;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipGuardian;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
/**SHIP GUARDING AI
 * CanFollow = false�ɥi�H����
 * ����T�u�Y�@�IfollowMax�椧��, �Z�����IfollowMax��H�W�N�|���զ^�h���I����a��followMin�椺
 * �Z���W�Lmax��W�L���w�ɶ���, �j��ǰe�^���I
 * �Yhost��gy<=0, �h�����������Howner, �|�]�wCanFollow = true
 * 
 * 2015/9/30:
 * move & attack mode:
 *   1. set StateMinor[GuardType] = 1
 *   2. get target within attack range every X ticks
 *   3. attack target if cooldown = 0
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
    			this.gx = host.getStateMinor(ID.M.GuardX);
        		this.gy = host.getStateMinor(ID.M.GuardY);
        		this.gz = host.getStateMinor(ID.M.GuardZ);
    		}
    		
    		//�Ygy=0, ��ܳoentity���l��, �٤������AI
    		if(gy <= 0) {
    			host.setStateFlag(ID.F.CanFollow, true);
    			return false;
    		}
    		else {
    			float fMin = host.getStateMinor(ID.M.FollowMin);
            	float fMax = host.getStateMinor(ID.M.FollowMax);
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
        			this.gx = host.getStateMinor(ID.M.GuardX);
            		this.gy = host.getStateMinor(ID.M.GuardY);
            		this.gz = host.getStateMinor(ID.M.GuardZ);
        		}
        		
        		//�Ygy<=0, ��ܳoentity����w�I���u, ����Howner
        		if(gy <= 0) {
        			host.setStateFlag(ID.F.CanFollow, true);
        			this.resetTask();
        			return;
        		}
        		else {
        			float fMin = host.getStateMinor(ID.M.FollowMin);
                	float fMax = host.getStateMinor(ID.M.FollowMax);
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
        	if(this.distSq > this.maxDistSq && host2.dimension == host.getStateMinor(ID.M.GuardDim)) {
        		this.checkTeleport++;
        		
        		if(this.checkTeleport > 200) {
        			LogHelper.info("DEBUG : guarding AI: point away > 200 ticks, teleport entity");
        			this.checkTeleport = 0;
        			
        			//teleport
        			if(this.distSq > 1024) {	//32 blocks away, drop seat2
        				this.clearMountSeat2();
        			}
        			
    				this.host2.setLocationAndAngles(this.gx+0.5D, this.gy+0.5D, this.gz+0.5D, this.host2.rotationYaw, this.host2.rotationPitch);
    				this.ShipNavigator.clearPathEntity();
    				this.sendSyncPacket();
                    return;
        		}
        	}
        	
        	//�Ccd���@�����|
        	if(this.findCooldown <= 0) {
    			this.findCooldown = 30;
    			
    			//check path result
            	if(host2.dimension == host.getStateMinor(ID.M.GuardDim) && !this.ShipNavigator.tryMoveToXYZ(gx, gy, gz, 1D)) {
            		LogHelper.info("DEBUG : guarding AI: fail to move, cannot reach or too far away "+gx+" "+gy+" "+gz);
            		//�Y�W�Lmax dist����120ticks, �hteleport
            		if(this.distSq > this.maxDistSq && host2.dimension == host.getStateMinor(ID.M.GuardDim)) {
            			this.checkTeleport++;	//�Y�Z���W�Lmax dist�B���ʤS����, �|��checkTP�C30 tick+1
                		
                		if(this.checkTeleport > 120) {
                			this.checkTeleport = 0;
                			
                			if(this.distSq > 1024) {	//32 blocks away, drop seat2
                				this.clearMountSeat2();
                			}
                			
                			//teleport
                			this.host2.setLocationAndAngles(this.gx+0.5D, this.gy+0.5D, this.gz+0.5D, this.host2.rotationYaw, this.host2.rotationPitch);
            				this.ShipNavigator.clearPathEntity();
            				this.sendSyncPacket();
                            return;
                		}	
                    }
                }//end !try move
            }//end path find cooldown
    	}
    }

    //clear seat2
	private void clearMountSeat2() {
		//�Y�y��2���H, �n����y��2�����Ƚ�
  		if(host2.ridingEntity != null) {
  			if(host2.ridingEntity instanceof BasicEntityMount) {
	  			BasicEntityMount mount = (BasicEntityMount) host2.ridingEntity;
	  			if(mount.seat2 != null) {
	  				mount.seat2.setRiderNull();
	  			}
  			}
  			host2.mountEntity(null);
  		}
  		
  		//�M���M�����H
  		if(host2.riddenByEntity != null) {
  			host2.riddenByEntity.mountEntity(null);
  			host2.riddenByEntity = null;
  		}
	}
	
	//sync position
	private void sendSyncPacket() {
		//for other player, send ship state for display
		TargetPoint point = new TargetPoint(host2.dimension, host2.posX, host2.posY, host2.posZ, 48D);
		CommonProxy.channelE.sendToAllAround(new S2CEntitySync(host2, 0, 9), point);
	}
	
}
