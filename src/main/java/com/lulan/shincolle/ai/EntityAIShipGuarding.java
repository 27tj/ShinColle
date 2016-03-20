package com.lulan.shincolle.ai;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAircraftAttack;
import com.lulan.shincolle.entity.IShipCannonAttack;
import com.lulan.shincolle.entity.IShipGuardian;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.FormationHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.TargetHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
/**SHIP GUARDING AI
 * CanFollow = false�ɥi�H����
 * ����T�u�Y�@�IfollowMax�椧��, �Z�����IfollowMax��H�W�N�|���զ^�h���I����a��followMin�椺
 * �Z���W�Lmax��W�L���w�ɶ���, �j��ǰe�^���I
 * �Yhost��gy<=0, �h�����������Howner, �|�]�wCanFollow = true
 * 
 * 2015/9/30:
 * move & attack mode:
 *   attack while moving with shorter range & longer delay
 * 
 *   1. if StateMinor[GuardType] > 0
 *   2. get target within attack range every X ticks
 *   3. attack target if delay = 0
 *   
 *   guard type:
 *   0: none
 *   1: guard a block
 *   2: guard an entity
 */
public class EntityAIShipGuarding extends EntityAIBase {

    private IShipGuardian host;
    private EntityLiving host2;
    private EntityPlayer owner;
    private Entity guarded;
    private ShipPathNavigate ShipNavigator;
    private final TargetHelper.Sorter targetSorter;
    private final TargetHelper.Selector targetSelector;
    private static final double TP_DIST = 1600D;	//40 block for tp dist
    private static final int TP_TIME = 400;			//20 sec for can't move time
    private int checkTP_T, checkTP_D;				//teleport cooldown count
    private int findCooldown;						//path navi cooldown
    private double maxDistSq, minDistSq;
    private double distSq, distX, distY, distZ;	//��ؼЪ����u�Z��
    private double[] pos;							//guard position
    private double[] guardPosOld;					//last update position
    
    //attack parms, for BasicEntityShip only
    private IShipCannonAttack ship;  	//host can use cannon
    private IShipAircraftAttack ship2;	//host can use aircraft
    private EntityLivingBase target;  	//entity of target
    private int[] delayTime;			//attack delay time: 0:light 1:heavy 2:aircraft
    private int[] maxDelayTime;	    	//attack max delay time
    private int onSightTime;			//target on sight time
    private int aimTime;				//time before fire
    private float range, rangeSq;		//attack range
    private boolean launchType;			//airplane type, true = light
    private boolean isMoving;			//is moving
    private double tarDist, tarDistSqrt, tarDistX, tarDistY, tarDistZ;	//��ؼЪ����u�Z��

    
    public EntityAIShipGuarding(IShipGuardian entity) {
        this.host = entity;
        this.host2 = (EntityLiving) entity;
        this.ShipNavigator = entity.getShipNavigate();
        this.targetSorter = new TargetHelper.Sorter(host2);
        this.targetSelector = new TargetHelper.Selector(host2);
        this.distSq = 1D;
        this.isMoving = false;
        this.setMutexBits(7);
        
        //mount��, �]�wship��host
        if(entity instanceof IShipCannonAttack) {
        	this.ship = (IShipCannonAttack) entity;
        	
        	if(entity instanceof IShipAircraftAttack) {
        		this.ship2 = (IShipAircraftAttack) entity;
        	}
        }
        
        if(entity instanceof BasicEntityShip || entity instanceof BasicEntityMount) {
        	owner = EntityHelper.getEntityPlayerByUID(entity.getPlayerUID());
        }
        
        //init value
        this.pos = new double[] {-1D, -1D, -1D};
        this.guardPosOld = new double[] {-1D, -100D, -1D};
    	this.delayTime = new int[] {20, 20, 20};
    	this.maxDelayTime = new int[] {20, 40, 40};
    	this.onSightTime = 0;
    	this.aimTime = 20;
    	this.range = 1;
    	this.rangeSq = 1;
    }
    
    //�P�w�O�_�}�l����AI
    @Override
	public boolean shouldExecute() {
    	//�D���U, �D�M��, �D�Q�j, �D�i���H, �B��fuel�~����
    	if(host != null && !host.getIsRiding() && !host.getIsSitting() && !host.getStateFlag(ID.F.NoFuel) && !host.getStateFlag(ID.F.CanFollow)) {
    		//get guard target
    		return checkGuardTarget();
    	}
        return false;
    }

    //�ؼ��٨S����min dist�Ϊ̶Z���W�LTP_DIST���~��AI
    @Override
	public boolean continueExecuting() {
    	if(host != null) {
    		//�D���U, �D�M��, �D�Q�j, �D�i���H, �B��fuel�~����
    		if(!host.getIsRiding() && !host.getIsSitting() && !host.getStateFlag(ID.F.NoFuel) && !host.getStateFlag(ID.F.CanFollow)) {
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

    @Override
	public void startExecuting() {
        this.findCooldown = 10;
        this.checkTP_T = 0;
        this.checkTP_D = 0;
    }

    @Override
	public void resetTask() {
    	this.guarded = null;
    	this.isMoving = false;
    	this.findCooldown = 10;
        this.ShipNavigator.clearPathEntity();
    }

    @Override
	public void updateTask() {
    	/**update attack while moving
    	 * active when:
    	 * 1. is ship entity
    	 * 2. target AI = active attack
    	 * 3. guard type > 0
    	 */
    	if(isMoving && ship != null && !ship.getStateFlag(ID.F.PassiveAI) && ship.getStateMinor(ID.M.GuardType) > 0) {
    		//update parms
    		if(host2.ticksExisted % 64 == 0) {
    			this.updateAttackParms();
    		}
//    		LogHelper.info("DEBUG : guarding AI: exec moving attack");
    		//delay--
    		this.delayTime[0] = this.delayTime[0] - 1;
    		this.delayTime[1] = this.delayTime[1] - 1;
    		this.delayTime[2] = this.delayTime[2] - 1;
    		
    		//find target
    		if(host2.ticksExisted % 32 == 0) {
    			this.findTarget();
    			
    			//clear target if target dead
    			if(this.target != null && !this.target.isEntityAlive()) {
    				this.target = null;
    			}
    		}
    		
    		//attack target
    		if(this.target != null && this.host2.getEntitySenses().canSee(this.target)) {
    			//onsight++
    			this.onSightTime++;
    			
    			//calc dist
    			this.tarDistX = this.target.posX - this.host2.posX;
        		this.tarDistY = this.target.posY - this.host2.posY;
        		this.tarDistZ = this.target.posZ - this.host2.posZ;
        		this.tarDistSqrt = tarDistX*tarDistX + tarDistY*tarDistY + tarDistZ*tarDistZ;

        		//attack target within range
        		if(tarDistSqrt <= this.rangeSq && this.onSightTime >= this.aimTime) {
    	        	this.attackTarget();
        		}
    		}
    		//no target or not onsight, reset
    		else {
    			this.onSightTime = 0;
    		}
    	}//end attack while moving
    	
    	//update guarding
    	if(host != null) {
//    		LogHelper.info("DEBUG : exec guarding");
        	this.findCooldown--;
        	this.checkTP_T++;
        	
        	//update position every 32 ticks
        	if(host2.ticksExisted % 32 == 0) {
        		//get guard target
        		if(!checkGuardTarget()) return;
        	}//end update
        	
        	//end move
        	if(this.distSq <= this.minDistSq) {
        		this.isMoving = false;
        		this.ShipNavigator.clearPathEntity();
        	}
        	
        	//�Ccd���@�����|
        	if(this.findCooldown <= 0) {
    			this.findCooldown = 32;
    			this.isMoving = this.ShipNavigator.tryMoveToXYZ(pos[0], pos[1], pos[2], 1D);
        	}
        	
        	//�]�w�Y����V
            this.host2.getLookHelper().setLookPosition(pos[0], pos[1], pos[2], 30F, this.host2.getVerticalFaceSpeed());

            //check teleport conditions: same DIM and (dist > TP_DIST or time > TP_TIME)
        	if(this.host2.dimension == this.host.getStateMinor(ID.M.GuardDim)) {
        		//check dist
        		if(this.distSq > TP_DIST) {
        			this.checkTP_D++;
        			
        			if(this.checkTP_D > TP_TIME) {
        				this.checkTP_D = 0;
        				
        				LogHelper.info("DEBUG : guard AI: distSQ > "+TP_DIST+" , teleport to target. dim: "+host2.dimension+" "+owner.dimension);
            			this.applyTeleport();
            			return;
        			}
        		}
        		
        		//check moving time
        		if(this.checkTP_T > TP_TIME) {
        			this.checkTP_T = 0;
        			
        			LogHelper.info("DEBUG : guard AI: teleport entity: dimension check: "+host2.dimension+" "+owner.dimension);
        			this.applyTeleport();
        			return;
        		}
        	}//end same dim
    	}//end guard entity
    }
    
    //do teleport
    private void applyTeleport() {
    	if(this.host2 instanceof BasicEntityMount) {
    		BasicEntityShip ship = (BasicEntityShip) ((BasicEntityMount) this.host2).getHostEntity();
    		
    		if(this.distSq > 1024) {
    			this.clearMountSeat2(this.host2);  //too far away, drop mount
    			this.clearMountSeat2(ship);
    		}
    		
    		this.ShipNavigator.clearPathEntity();
    		ship.setLocationAndAngles(pos[0], pos[1]+0.5D, pos[2], this.host2.rotationYaw, this.host2.rotationPitch);
    		this.sendSyncPacket(ship);
    	}
    	else {
    		if(this.distSq > 1024) {
    			this.clearMountSeat2(this.host2);  //too far away, drop mount
    		}
        	
    		this.ShipNavigator.clearPathEntity();
    		this.host2.setLocationAndAngles(pos[0], pos[1]+0.5D, pos[2], this.host2.rotationYaw, this.host2.rotationPitch);
    		this.sendSyncPacket(this.host2);
    	}
    }

    //clear seat2
	private void clearMountSeat2(EntityLiving entity) {
		//�Y�y��2���H, �n����y��2�����Ƚ�
  		if(entity.ridingEntity != null) {
  			if(entity.ridingEntity instanceof BasicEntityMount) {
	  			BasicEntityMount mount = (BasicEntityMount) entity.ridingEntity;
	  			if(mount.seat2 != null) {
	  				mount.seat2.setRiderNull();
	  			}
  			}
  			entity.mountEntity(null);
  		}
  		
  		//�M���M�����H
  		if(entity.riddenByEntity != null) {
  			entity.riddenByEntity.mountEntity(null);
  			entity.riddenByEntity = null;
  		}
	}
	
	//sync position
	private void sendSyncPacket(Entity ent) {
		//for other player, send ship state for display
		TargetPoint point = new TargetPoint(ent.dimension, ent.posX, ent.posY, ent.posZ, 64D);
		CommonProxy.channelE.sendToAllAround(new S2CEntitySync(ent, 0, S2CEntitySync.PID.SyncEntity_PosRot), point);
	}
	
	//update attack parms
	private void updateAttackParms() {
    	if(this.ship != null) {
    		//attack range = 70% normal range
    		this.range = (int)(this.ship.getAttackRange() * 0.7F);
    		
    		//�ˬd�d��, ��range2 > range1 > 1
            if(this.range < 1) {
            	this.range = 1;
            }
    		
    		this.rangeSq = this.range * this.range;

    		//attack delay = 125% normal delay
    		this.maxDelayTime[0] = (int)(100F / (this.ship.getAttackSpeed()));
    		this.maxDelayTime[1] = (int)(200F / (this.ship.getAttackSpeed()));
    		this.maxDelayTime[2] = (int)(100F / (this.ship.getAttackSpeed()));
    		
    		//aim time (no change)
    		this.aimTime = (int) (20F * (150 - this.host.getLevel()) / 150F) + 10;
    	}
	}
	
	//find target
	private void findTarget() {
		List list1 = this.host2.worldObj.selectEntitiesWithinAABB(EntityLivingBase.class, 
        		this.host2.boundingBox.expand(this.range, this.range * 0.6D, this.range), this.targetSelector);
        
        //sort target list
        Collections.sort(list1, this.targetSorter);
        
        //get nearest target
		if(list1.size() > 2) {
			this.target = (EntityLivingBase)list1.get(this.host2.worldObj.rand.nextInt(3));
    	}
		else if(!list1.isEmpty()){
			this.target = (EntityLivingBase)list1.get(0);
		}
	}
	
	//attack method
	private void attackTarget() {
		//light attack
		if(this.ship.getStateFlag(ID.F.AtkType_Light) && this.delayTime[0] <= 0 && 
		   this.ship.useAmmoLight() && this.ship.hasAmmoLight()) {
    		this.ship.attackEntityWithAmmo(this.target);
            this.delayTime[0] = this.maxDelayTime[0];
    	}
    	
    	//heavy attack
    	if(this.ship.getStateFlag(ID.F.AtkType_Heavy) && this.delayTime[1] <= 0 && 
    	   this.ship.useAmmoHeavy() && this.ship.hasAmmoHeavy()) {
    		this.ship.attackEntityWithHeavyAmmo(this.target);
            this.delayTime[1] = this.maxDelayTime[1];
    	}
    	
    	//aircraft light attack
        if(this.ship2 != null && (this.ship2.getStateFlag(ID.F.UseAirLight) || this.ship2.getStateFlag(ID.F.UseAirHeavy)) && this.delayTime[2] <= 0) {
        	//�Y�u�ϥγ�@�ؼu��, �h���Ϋ��A����, �u�o�g�P�@�ح���
            if(!this.ship2.getStateFlag(ID.F.UseAirLight)) {
            	this.launchType = false;
            }
            if(!this.ship2.getStateFlag(ID.F.UseAirHeavy)) {
            	this.launchType = true;
            }
            
        	//light
        	if(this.launchType && this.ship2.hasAmmoLight() && this.ship2.hasAirLight()) {
        		this.ship2.attackEntityWithAircraft(this.target);
        		this.delayTime[2] = this.maxDelayTime[2];
        	}
        	
        	//heavy
        	if(!this.launchType && this.ship2.hasAmmoHeavy() && this.ship2.hasAirHeavy()) {
        		this.ship2.attackEntityWithHeavyAircraft(this.target);
        		this.delayTime[2] = this.maxDelayTime[2];
        	}
        	
        	this.launchType = !this.launchType;		//change type
        }
	}
	
	private boolean checkGuardTarget() {
		//check guarded entity
		this.guarded = host.getGuardedEntity();
		
		if(this.guarded != null) {
			//target is alive and same dimension
			if(!this.guarded.isEntityAlive() || this.guarded.worldObj.provider.dimensionId != this.host2.worldObj.provider.dimensionId) {
				host.setGuardedEntity(null);
				this.resetTask();
				return false;
			}
			else {
				//if guard with formation
				if(host.getStateMinor(ID.M.FormatType) > 0) {
					//if target moving distSQ > 7, get new position
					double dx = guardPosOld[0] - guarded.posX;
					double dy = guardPosOld[1] - guarded.posY;
					double dz = guardPosOld[2] - guarded.posZ;
					double dsq = dx * dx + dy * dy + dz * dz;
					
					if(dsq > 7) {
						//get new position
						pos = FormationHelper.getFormationGuardingPos(host, guarded, guardPosOld[0], guardPosOld[2]);
					
						//backup old position
						guardPosOld[0] = guarded.posX;
						guardPosOld[1] = guarded.posY;
						guardPosOld[2] = guarded.posZ;
						
						//draw moving particle
						if(ConfigHandler.alwaysShowTeamParticle || EntityHelper.checkInUsePointer(owner)) {
							CommonProxy.channelP.sendTo(new S2CSpawnParticle(25, 0, pos[0], pos[1], pos[2], 0.3, 4, 0), (EntityPlayerMP) owner);
						}
					}
					
					//DEBUG
					if(this.host2.ticksExisted % 16 == 0) {
						//draw moving particle
						if(ConfigHandler.alwaysShowTeamParticle || EntityHelper.checkInUsePointer(owner)) {
							CommonProxy.channelP.sendTo(new S2CSpawnParticle(25, 0, pos[0], pos[1], pos[2], 0.3, 6, 0), (EntityPlayerMP) owner);
						}
					}
				}
				//no formation
				else {
					pos[0] = guarded.posX;
					pos[1] = guarded.posY;
					pos[2] = guarded.posZ;
				}
			}
		}
		else {
			pos[0] = host.getStateMinor(ID.M.GuardX) + 0.5D;  //int to double block position
			pos[1] = host.getStateMinor(ID.M.GuardY) + 0.5D;
			pos[2] = host.getStateMinor(ID.M.GuardZ) + 0.5D;
		}
		
		//�Ygy<=0, ��ܳoentity����w�I���u, ����Howner
		if(pos[1] <= 0) {
			host.setStateFlag(ID.F.CanFollow, true);
			this.resetTask();
			return false;
		}
		else {
			//host is in formation
			if(this.ship != null && this.ship.getStateMinor(ID.M.FormatType) > 0){
				//guard entity
				if(this.ship.getStateMinor(ID.M.GuardType) == 2) {
					this.minDistSq = 5;
	                this.maxDistSq = 9;
				}
				//guard block
				else {
					this.minDistSq = 4;
	                this.maxDistSq = 7;
				}
				
			}
			//not formation mode
			else {
				float fMin = host.getStateMinor(ID.M.FollowMin) + host2.width * 0.75F;
            	float fMax = host.getStateMinor(ID.M.FollowMax) + host2.width * 0.75F;
            	this.minDistSq = fMin * fMin;
                this.maxDistSq = fMax * fMax;
			}
            
            //�p�⪽�u�Z��
        	this.distX = pos[0] - this.host2.posX;
    		this.distY = pos[1] - this.host2.posY;
    		this.distZ = pos[2] - this.host2.posZ;
        	this.distSq = this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ;
        	
        	if(this.distSq > this.maxDistSq && host2.dimension == host.getStateMinor(ID.M.GuardDim)) {
        		return true;
        	}
		}
		
		return false;
	}

	
}
