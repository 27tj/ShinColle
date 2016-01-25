package com.lulan.shincolle.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.FormationHelper;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
/**SHIP FOLLOW OWNER AI
 * �Z���W�Lmax dist��Ĳ�o����, ���쨫�imin dist�Z���ɰ���
 * �Z���W�LTP_DIST�|����teleport��owner����
 */
public class EntityAIShipFollowOwner extends EntityAIBase {

    private IShipAttackBase host;
    private EntityLiving host2;
    private EntityLivingBase owner;
    private EntityPlayer player;
    private static final double TP_DIST = 2048D;	//teleport condition ~ 45 blocks
    private ShipPathNavigate ShipNavigator;
    private int findCooldown;
    private int checkTeleport, checkTeleport2;		//use teleport time
    private double maxDistSq;
    private double minDistSq;
    private double distSq;
    private double distSqrt;
    private double distX, distY, distZ;	//��ؼЪ����u�Z��(������)
    private double[] pos, ownerPosOld;  //moving target, owner prev position

    
    public EntityAIShipFollowOwner(IShipAttackBase entity) {
        this.host = entity;
        this.host2 = (EntityLiving) entity;
        this.ShipNavigator = entity.getShipNavigate();
        this.distSq = 1D;
        this.distSqrt = 1D;
        this.setMutexBits(7);
        
        //init vars
        this.pos = new double[] {host2.posX, host2.posY, host2.posZ};
        this.ownerPosOld = new double[] {host2.posX, host2.posY, host2.posZ};
        
        if(entity instanceof BasicEntityShip || entity instanceof BasicEntityMount) {
        	player = EntityHelper.getEntityPlayerByUID(entity.getPlayerUID());
        }
    }
    
    //��owner�B�ؼжW�Lmax dist��Ĳ�oAI, Ĳ�o�ᦹ��k���A����, �אּ�������cont exec
    @Override
	public boolean shouldExecute() {
    	if(!host.getIsSitting() && !host.getIsRiding() && !host.getIsLeashed() && 
    	   !host.getStateFlag(ID.F.NoFuel) && host.getStateFlag(ID.F.CanFollow)) {
    		EntityLivingBase OwnerEntity = EntityHelper.getEntityPlayerByUID(this.host.getPlayerUID(), this.host2.worldObj);

    		//get owner distance
            if(OwnerEntity != null) {
            	this.owner = OwnerEntity;
                
            	if(this.owner.dimension != this.host2.dimension) {
            		return false;
            	}
            	
            	updateDistance();

            	if(distSq > this.maxDistSq) {
            		return true;
            	}
            }
    	}
        return false;
    }

    //�ؼ��٨S����min dist�Ϊ̶Z���W�LTP_DIST���~��AI
    @Override
	public boolean continueExecuting() {
    	//�D���U, �M��, �Q�j��ɥi�H�~�����AI
    	if(host != null) { 
    		if(!host.getIsSitting() && !host.getIsRiding() && !host.getIsLeashed() && 
    		   !host.getStateFlag(ID.F.NoFuel) && host.getStateFlag(ID.F.CanFollow)) {

	        	//�Z���W�L�ǰe�Z��, �h�B�z�ǰe����
	        	if(this.distSq > EntityAIShipFollowOwner.TP_DIST) {
	        		return true;
	        	}

	        	//�٨S���imin follow range, �~��
	        	if(this.distSq > this.minDistSq ) {		
	        		return true;
	        	}
	        	
	        	//��L���p
	        	return !ShipNavigator.noPath() ||shouldExecute();
    		}
    		else {	//�Y�����U, �M��, �Q�j, �h���mAI
    			this.resetTask();
    			return false;
    		}
    	}
    	
    	return false;
    }

    @Override
	public void startExecuting() {
        this.findCooldown = 20;
        this.checkTeleport = 0;
        this.checkTeleport2 = 0;
    }

    @Override
	public void resetTask() {
        this.owner = null;
        this.ShipNavigator.clearPathEntity();
    }

    @Override
	public void updateTask() {
    	if(host != null) {
//    		LogHelper.info("DEBUG : exec follow owner");
        	this.findCooldown--;
        	
        	//update follow range every 32 ticks
        	if(host2.ticksExisted % 32 == 0){
        		//update owner distance
            	EntityLivingBase OwnerEntity = EntityHelper.getEntityPlayerByUID(this.host.getPlayerUID(), this.host2.worldObj);

                if(OwnerEntity != null) {
                	this.owner = OwnerEntity;
                    
                	if(this.owner.dimension != this.host2.dimension) {
                		this.resetTask();
                		return;
                	}
                	
                	updateDistance();
                }
        	}//end update
        	
        	//end move
        	if(this.distSq <= this.minDistSq) {
        		this.ShipNavigator.clearPathEntity();
        	}
        	
        	//�]�w�Y����V
            this.host2.getLookHelper().setLookPositionWithEntity(this.owner, 20F, 40F);

        	//�Z���W�L�ǰe�Z��, �����ǰe��ؼФW
        	if(this.distSq > EntityAIShipFollowOwner.TP_DIST) {
        		this.checkTeleport++;
        		
        		if(this.checkTeleport > 256) {
        			this.checkTeleport = 0;
        			//�ۦPdim�~�ǰe
        			LogHelper.info("DEBUG : follow AI: distSQ > "+EntityAIShipFollowOwner.TP_DIST+" , teleport to target. dim: "+host2.dimension+" "+owner.dimension);
        			if(this.host2.dimension == this.owner.dimension) {
        				
        				//teleport
            			if(this.distSq > 1024) {	//32 blocks away, drop seat2
            				this.clearMountSeat2();
            			}
            			
        				this.host2.setLocationAndAngles(this.owner.posX, this.owner.posY + 1D, this.owner.posZ, this.host2.rotationYaw, this.host2.rotationPitch);
        				this.ShipNavigator.clearPathEntity();
        				this.sendSyncPacket();
                        return;
        			}
        		}
        	}
        	
        	//�Ccd���@�����|
        	if(this.findCooldown <= 0) {
    			this.findCooldown = 30;
    			
    			//check path result
            	if(this.host2.dimension == this.owner.dimension && !this.ShipNavigator.tryMoveToXYZ(pos[0], pos[1], pos[2], 1D)) {
            		//�Y�W�Lmax dist����240ticks, �hteleport
            		if(this.distSq > this.maxDistSq) {
            			this.checkTeleport2++;
                		
                		if(this.checkTeleport2 > 12) {
                			this.checkTeleport2 = 0;
                			//�ۦPdim�~�ǰe
                			LogHelper.info("DEBUG : follow AI: teleport entity: dimension check: "+host2.dimension+" "+owner.dimension);
                			if(this.host2.dimension == this.owner.dimension) {
                				//teleport
                    			if(this.distSq > 1024) {	//32 blocks away, drop seat2
                    				this.clearMountSeat2();
                    			}
                    			
                				this.host2.setLocationAndAngles(this.owner.posX, this.owner.posY + 1D, this.owner.posZ, this.host2.rotationYaw, this.host2.rotationPitch);
                				this.ShipNavigator.clearPathEntity();
                				this.sendSyncPacket();
                                return;
                			}
                		}	
                    }
                }//end !try move to owner
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
  		CommonProxy.channelE.sendToAllAround(new S2CEntitySync(host2, 0, S2CEntitySync.PID.SyncEntity_PosRot), point);
  	}
  	
  	//update distance
  	private void updateDistance() {
  		//if ship with formation
		if(host.getStateMinor(ID.M.FormatType) > 0) {
			this.minDistSq = 4;
            this.maxDistSq = 7;
			
            //if owner moving distSQ > 7, get new position
			double dx = ownerPosOld[0] - owner.posX;
			double dy = ownerPosOld[1] - owner.posY;
			double dz = ownerPosOld[2] - owner.posZ;
			double dsq = dx * dx + dy * dy + dz * dz;
			
			if(dsq > 7) {
				//get new position
				pos = FormationHelper.getFormationGuardingPos(host, owner, ownerPosOld[0], ownerPosOld[2]);
				
				//backup old position
				ownerPosOld[0] = owner.posX;
				ownerPosOld[1] = owner.posY;
				ownerPosOld[2] = owner.posZ;
					
				//draw moving particle
				if(ConfigHandler.alwaysShowTeamParticle || EntityHelper.checkInUsePointer(player)) {
					CommonProxy.channelP.sendTo(new S2CSpawnParticle(25, 0, pos[0], pos[1], pos[2], 0.3, 4, 0), (EntityPlayerMP) player);
				}
			}
			
			if(this.host2.ticksExisted % 16 == 0) {
				//draw moving particle
				if(ConfigHandler.alwaysShowTeamParticle || EntityHelper.checkInUsePointer(player)) {
					CommonProxy.channelP.sendTo(new S2CSpawnParticle(25, 0, pos[0], pos[1], pos[2], 0.3, 6, 0), (EntityPlayerMP) player);
				}
			}
		}
		//no formation
		else {
			float fMin = host.getStateMinor(ID.M.FollowMin) + host2.width * 0.75F;
	    	float fMax = host.getStateMinor(ID.M.FollowMax) + host2.width * 0.75F;
	    	this.minDistSq = fMin * fMin;
	        this.maxDistSq = fMax * fMax;
	        
	        pos[0] = owner.posX;
			pos[1] = owner.posY;
			pos[2] = owner.posZ;
		}

    	//�p�⪽�u�Z��
    	this.distX = pos[0] - this.host2.posX;
		this.distY = pos[1] - this.host2.posY;
		this.distZ = pos[2] - this.host2.posZ;
    	this.distSq = this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ;
    
  	}
	
	
}