package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.EntityAIShipAttackOnCollide;
import com.lulan.shincolle.ai.EntityAIShipFlee;
import com.lulan.shincolle.ai.EntityAIShipFloating;
import com.lulan.shincolle.ai.EntityAIShipFollowOwner;
import com.lulan.shincolle.ai.EntityAIShipGuarding;
import com.lulan.shincolle.ai.EntityAIShipRangeTarget;
import com.lulan.shincolle.ai.EntityAIShipRevengeTarget;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.ai.EntityAIShipWander;
import com.lulan.shincolle.ai.EntityAIShipWatchClosest;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.client.gui.inventory.ContainerShipInventory;
import com.lulan.shincolle.crafting.EquipCalc;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.OwnerPaper;
import com.lulan.shincolle.item.PointerItem;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.FormationHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TargetHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;

/**SHIP DATA <br>
 * Explanation in crafting/ShipCalc.class
 */
public abstract class BasicEntityShip extends EntityTameable implements IShipCannonAttack, IShipGuardian, IShipFloating {

	protected ExtendShipProps ExtProps;			//entity額外NBT紀錄
	protected ShipPathNavigate shipNavigator;	//水空移動用navigator
	protected ShipMoveHelper shipMoveHelper;
	protected Entity guardedEntity;				//guarding target
	protected Entity atkTarget;					//attack target
	protected Entity rvgTarget;					//revenge target
	protected int revengeTime;					//revenge target time
	
	//for AI calc
	protected double ShipDepth;			//水深, 用於水中高度判定
	protected double ShipPrevX;			//ship posX 5 sec ago
	protected double ShipPrevY;			//ship posY 5 sec ago
	protected double ShipPrevZ;			//ship posZ 5 sec ago
	/** equip states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateEquip;
	/** final states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateFinal;
	/** final states before buffs: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateFinalBU;
	/** minor states: 0:ShipLevel 1:Kills 2:ExpCurrent 3:ExpNext 4:NumAmmoLight 
	 *  5:NumAmmoHeavy 6:NumGrudge 7:NumAirLight 8:NumAirHeavy 9:immunity time 
	 *  10:followMin 11:followMax 12:FleeHP 13:TargetAIType 14:guardX 15:guardY 16:guardZ 17:guardDim
	 *  18:guardID 19:shipType 20:shipClass 21:playerUID 22:shipUID 23:playerEID 24:guardType 
	 *  25:damageType 26:formationType 27:formationPos 28:grudgeConsumption 29:ammoConsumption*/
	protected int[] StateMinor;
	/** equip effect: 0:critical 1:doubleHit 2:tripleHit 3:baseMiss 4:atk_AntiAir 5:atk_AntiSS 6:dodge*/
	protected float[] EffectEquip;
	/** equip effect: 0:critical 1:doubleHit 2:tripleHit 3:baseMiss 4:atk_AntiAir 5:atk_AntiSS 6:dodge*/
	protected float[] EffectEquipBU;
	/** formation effect: 0:ATK_L 1:ATK_H 2:ATK_AL 3:ATK_AH 4:DEF 5:MOV 6:MISS 7:DODGE 8:CRI
	 *                    9:DHIT 10:THIT 11:AA 12:ASM */
	protected float[] EffectFormation;
	/** formation fixed effect: 0:MOV */
	protected float[] EffectFormationFixed;
	/** EntityState: 0:State 1:Emotion 2:Emotion2 3:HP State 4:State2 5:AttackPhase*/
	protected byte[] StateEmotion;
	/** EntityFlag: 0:canFloatUp 1:isMarried 2:noFuel 3:canMelee 4:canAmmoLight 5:canAmmoHeavy 
	 *  6:canAirLight 7:canAirHeavy 8:headTilt(client only) 9:canRingEffect 10:canDrop 11:canFollow
	 *  12:onSightChase 13:AtkType_Light 14:AtkType_Heavy 15:AtkType_AirLight 16:AtkType_AirHeavy 
	 *  17:HaveRingEffect 18:PVPFirst 19:AntiAir 20:AntiSS 21:PassiveAI 22:TimeKeeper */
	protected boolean[] StateFlag;
	/** BonusPoint: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT */
	protected byte[] BonusPoint;
	/** TypeModify: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT */
	protected float[] TypeModify;
	/** ModelPos: posX, posY, posZ, scale (in ship inventory) */
	protected float[] ModelPos;
	/** Update Flag: 0:FormationBuff */
	protected boolean[] UpdateFlag;
	
	//for model render
	/** EmotionTicks: 0:FaceTick 1:HeadTiltTick 2:AttackEmoCount */
	protected int[] EmotionTicks;		//表情開始時間 or 表情計時時間
	protected float[] rotateAngle;		//模型旋轉角度, 用於手持物品render
	protected int StartSoundHurt;		//hurt sound ticks
	
	//for GUI display, no use
	protected String ownerName;
	
	//for initial
	private boolean initAI, initWaitAI;	//init flag
	private boolean isUpdated;			//updated ship id/owner id tag
	private int updateTime = 16;		//update check interval
	
	
	public BasicEntityShip(World world) {
		super(world);
		this.ignoreFrustumCheck = true;  //即使不在視線內一樣render
		this.maxHurtResistantTime = 2;   //受傷無敵時間降為2 ticks
		this.StartSoundHurt = 0;
		
		//init value
		this.isImmuneToFire = true;	//set ship immune to lava
		this.StateEquip = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
		this.StateFinal = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
		this.StateFinalBU = this.StateFinal.clone();
		this.StateMinor = new int[] {1,  0,  0,  40, 0,
				                     0,  0,  0,  0,  0,
				                     3,  12, 35, 1,  -1,
				                     -1, -1, 0,  -1, 0,
				                     0,  -1, -1, -1, 0,
				                     0,  0,  0,  0,  0
				                    };
		this.EffectEquip = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F};
		this.EffectEquipBU = this.EffectEquip.clone();
		this.EffectFormation = new float[] {0F, 0F, 0F, 0F, 0F,
									        0F, 0F, 0F, 0F, 0F,
									        0F, 0F, 0F};
		this.EffectFormationFixed = new float[] {0F};
		this.StateEmotion = new byte[] {0, 0, 0, 0, 0, 0};
		this.StateFlag = new boolean[] {false, false, true, false, true,
				                        true, true, true, false, true,
								        true, false, true, true, true,
								        true, true, false, true, false,
								        false, false, true
								       };
		this.UpdateFlag = new boolean[] {false};
		this.BonusPoint = new byte[] {0, 0, 0, 0, 0, 0};
		this.TypeModify = new float[] {1F, 1F, 1F, 1F, 1F, 1F};
		this.ModelPos = new float[] {0F, 0F, 0F, 50F};
		
		//for AI
		this.ShipDepth = 0D;			//water block above ship (within ship position)
		this.ShipPrevX = posX;			//ship position 5 sec ago
		this.ShipPrevY = posY;
		this.ShipPrevZ = posZ;
		this.ownerName = "";
		this.stepHeight = 1F;
		this.shipNavigator = new ShipPathNavigate(this, worldObj);
		this.shipMoveHelper = new ShipMoveHelper(this, 25F);
		
		//for render
		this.EmotionTicks = new int[] {0,  0,  0};
		this.rotateAngle = new float[] {0F, 0F, 0F};		//model rotate angle (right hand)
		
		//for init
		this.initAI = false;			//normal init
		this.initWaitAI = false;		//slow init after player entity loaded
		this.isUpdated = false;
	}
	
	@Override
	public boolean isAIEnabled() {
		return true;
	}
	
	@Override
	public boolean isEntityInvulnerable() {
        return StateMinor[ID.M.ImmuneTime] > 0;
    }
	
	@Override
	public boolean isBurning() {	//display fire effect
		return this.getStateEmotion(ID.S.HPState) == ID.HPState.HEAVY;
	}
	
	@Override
	public float getEyeHeight() {
		return this.height * 1F;
	}
	
	@Override
	public boolean hasCustomNameTag() {
		if(ConfigHandler.showTag) {
			return this.dataWatcher.getWatchableObjectString(10).length() > 0;
		}
        return false;
    }
	
	//平常音效
	@Override
	protected String getLivingSound() {
		if(this.getStateFlag(ID.F.IsMarried)) {
			if(rand.nextInt(5) == 0) {
				return Reference.MOD_ID+":ship-love";
			}
			else {
				return Reference.MOD_ID+":ship-say";
			}
		}
		else {
			return Reference.MOD_ID+":ship-say";
		}
    }
	
	//受傷音效
    @Override
	protected String getHurtSound() {
    	if(this.StartSoundHurt <= 0) {
    		this.StartSoundHurt = 20 + this.getRNG().nextInt(40);
    		return Reference.MOD_ID+":ship-hurt";
    	}
    	return null;
    }

    //死亡音效
    @Override
	protected String getDeathSound() {
    	return Reference.MOD_ID+":ship-death";
    }

    //音效大小
    @Override
	protected float getSoundVolume() {
        return ConfigHandler.shipVolume;
    }
    
    //get model rotate angle, par1 = 0:X, 1:Y, 2:Z
    @Override
    public float getModelRotate(int par1) {
    	switch(par1) {
    	default:
    		return this.rotateAngle[0];
    	case 1:
    		return this.rotateAngle[1];
    	case 2:
    		return this.rotateAngle[2];
    	}
    }
    
    //set model rotate angle, par1 = 0:X, 1:Y, 2:Z
    @Override
	public void setModelRotate(int par1, float par2) {
		switch(par1) {
    	default:
    		rotateAngle[0] = par2;
    	case 1:
    		rotateAngle[1] = par2;
    	case 2:
    		rotateAngle[2] = par2;
    	}
	}

	@Override
	public EntityAgeable createChild(EntityAgeable p_90011_1_) {
		return null;
	}
	
	//setup AI
	protected void setAIList() {
		this.getNavigator().setEnterDoors(true);
		this.getNavigator().setAvoidsWater(false);
		this.getNavigator().setCanSwim(true);
		
		//high priority
		this.tasks.addTask(1, new EntityAIShipSit(this));	   		//0101
		this.tasks.addTask(2, new EntityAIShipFlee(this));			//0111
		this.tasks.addTask(3, new EntityAIShipGuarding(this));		//0111
		this.tasks.addTask(4, new EntityAIShipFollowOwner(this));	//0111
		
		//use melee attack
		if(this.getStateFlag(ID.F.UseMelee)) {
			this.tasks.addTask(15, new EntityAIShipAttackOnCollide(this, 1D, true));//0011
			this.tasks.addTask(16, new EntityAIMoveTowardsTarget(this, 1D, 48F));	//0001
		}
		
		//idle AI
//		this.tasks.addTask(21, new EntityAIOpenDoor(this, true));	//0000 //TODO ship door AI
		this.tasks.addTask(23, new EntityAIShipFloating(this));		//0101
		this.tasks.addTask(24, new EntityAIShipWatchClosest(this, EntityPlayer.class, 6F, 0.08F)); //0010
		this.tasks.addTask(25, new EntityAIShipWander(this, 0.8D));	//0001
		this.tasks.addTask(25, new EntityAILookIdle(this));			//0011
	}
	
	//setup target AI
	public void setAITargetList() {
		//passive target AI
		if(this.getStateFlag(ID.F.PassiveAI)) {
			this.targetTasks.addTask(1, new EntityAIShipRevengeTarget(this));
		}
		//active target AI
		else {
			this.targetTasks.addTask(1, new EntityAIShipRevengeTarget(this));
			this.targetTasks.addTask(5, new EntityAIShipRangeTarget(this, Entity.class));
		}
	}

	//clear AI
	protected void clearAITasks() {
		tasks.taskEntries.clear();
	}
	
	//clear target AI
	protected void clearAITargetTasks() {
		this.setAttackTarget(null);
		this.setEntityTarget(null);
		targetTasks.taskEntries.clear();
	}
	
	//getter
	public ExtendShipProps getExtProps() {
		return ExtProps;
	}
	
	@Override
	public ShipPathNavigate getShipNavigate() {
		return shipNavigator;
	}
	
	@Override
	public ShipMoveHelper getShipMoveHelper() {
		return shipMoveHelper;
	}
	
	abstract public int getEquipType();
	
	/** 0:small, 1:large, 2:mob small, 3:mob large*/
	abstract public int getKaitaiType();
	
	@Override
	public int getLevel() {
		return StateMinor[ID.M.ShipLevel];
	}
	
	public byte getShipType() {
		return (byte)getStateMinor(ID.M.ShipType);
	}
	
	//ShipID = ship CLASS ID
	public short getShipClass() {
		return (short) getStateMinor(ID.M.ShipClass);
	}
	
	//ShipUID = ship UNIQUE ID
	public int getShipUID() {
		return getStateMinor(ID.M.ShipUID);
	}
	
	//PlayerUID = player UNIQUE ID (not UUID)
	@Override
	public int getPlayerUID() {
		return getStateMinor(ID.M.PlayerUID);
	}
	
	@Override
	public int getAmmoLight() {
		return this.StateMinor[ID.M.NumAmmoLight];
	}

	@Override
	public int getAmmoHeavy() {
		return this.StateMinor[ID.M.NumAmmoHeavy];
	}
	
	@Override
	public int getFaceTick() {
		return this.EmotionTicks[0];
	}
	
	@Override
	public int getHeadTiltTick() {
		return this.EmotionTicks[1];
	}
	
	@Override
	public int getAttackAniTick() {
		return this.EmotionTicks[2];
	}
	
	@Override
	public int getTickExisted() {
		return this.ticksExisted;
	}
	
	@Override
	public int getAttackTime() {
		return this.attackTime;
	}
	
	@Override
	public boolean getIsRiding() {
		return this.isRiding();
	}
	
	@Override
	public boolean getIsLeashed() {
		return this.getLeashed();
	}

	@Override
	public boolean getIsSprinting() {
		return this.isSprinting();
	}

	@Override
	public boolean getIsSitting() {
		return this.isSitting();
	}

	@Override
	public boolean getIsSneaking() {
		return this.isSneaking();
	}
	
	@Override
	public Entity getEntityTarget() {
		return this.atkTarget;
	}
	
	@Override
	public Entity getEntityRevengeTarget() {
		return this.rvgTarget;
	}

	@Override
	public int getEntityRevengeTime() {
		return this.revengeTime;
	}
	
	@Override
	public float getAttackDamage() {	//NO USE for ship entity
		return 0;
	}
	
	@Override
	public float getAttackSpeed() {
		return this.StateFinal[ID.SPD];
	}
	
	@Override
	public float getAttackRange() {
		return this.StateFinal[ID.HIT];
	}
	
	@Override
	public boolean getAttackType(int par1) {
		return this.getStateFlag(par1);
	}
	
	@Override
	public float getDefValue() {
		return this.StateFinal[ID.DEF];
	}
	
	@Override
	public float getMoveSpeed() {
		return this.StateFinal[ID.MOV];
	}
	
	@Override
	public boolean hasAmmoLight() {
		return StateMinor[ID.M.NumAmmoLight] >= StateMinor[ID.M.AmmoCon];
	}
	
	@Override
	public boolean hasAmmoHeavy() {
		return StateMinor[ID.M.NumAmmoHeavy] >= StateMinor[ID.M.AmmoCon];
	}

	@Override
	public boolean useAmmoLight() {
		return StateFlag[ID.F.UseAmmoLight];
	}

	@Override
	public boolean useAmmoHeavy() {
		return StateFlag[ID.F.UseAmmoHeavy];
	}
	
	@Override
	public double getShipDepth() {
		return ShipDepth;
	}
	
	@Override
	public boolean getStateFlag(int flag) {	//get flag (boolean)
		return StateFlag[flag];		
	}
	
	public byte getStateFlagI(int flag) {	//get flag (byte)
		if(StateFlag[flag]) {
			return 1;
		}
		else {
			return 0;
		}
	}
	
	public float getStateEquip(int id) {
		return StateEquip[id];
	}
	
	public float getStateFinal(int id) {
		return StateFinal[id];
	}
	
	public float getStateFinalBU(int id) {
		return StateFinalBU[id];
	}
	
	@Override
	public int getStateMinor(int id) {
		return StateMinor[id];
	}
	
	@Override
	public float getEffectEquip(int id) {
		return EffectEquip[id];
	}
	
	public float getEffectEquipBU(int id) {
		return EffectEquipBU[id];
	}
	
	public boolean getUpdateFlag(int id) {
		return UpdateFlag[id];
	}
	
	public float getEffectFormation(int id) {
		return EffectFormation[id];
	}
	
	public float getEffectFormationFixed(int id) {
		return EffectFormationFixed[id];
	}
	
	@Override
	public byte getStateEmotion(int id) {
		return StateEmotion[id];
	}
	
	public byte getBonusPoint(int id) {
		return BonusPoint[id];
	}
	
	public float getTypeModify(int id) {
		return TypeModify[id];
	}
	
	/** get model pos in GUI */
	public float[] getModelPos() {
		return ModelPos;
	}
	
	/** grudge consumption when IDLE */
	public int getGrudgeConsumption() {
		return getStateMinor(ID.M.GrudgeCon);
	}
	
	public int getAmmoConsumption() {
		return getStateMinor(ID.M.AmmoCon);
	}
	
	/**calc equip, buff, debuff and all attrs
	 * 
	 * step:
	 * 1. reset all attrs to 0
	 * 2. calc 6 equip slots
	 * 3. calc special attrs (if @Override calcShipAttributes())
	 * 4. calc HP,DEF...etc
	 * 5. backup unbuff attrs
	 * 6. calc buffs
	 * 7. send sync packet
	 * 
	 */
	public void calcEquipAndUpdateState() {
		ItemStack itemstack = null;
		float[] equipStat = null;
		
		//init value
		StateEquip[ID.HP] = 0F;
		StateEquip[ID.DEF] = 0F;
		StateEquip[ID.SPD] = 0F;
		StateEquip[ID.MOV] = 0F;
		StateEquip[ID.HIT] = 0F;
		StateEquip[ID.ATK] = 0F;
		StateEquip[ID.ATK_H] = 0F;
		StateEquip[ID.ATK_AL] = 0F;
		StateEquip[ID.ATK_AH] = 0F;
		
		EffectEquip[ID.EF_CRI] = 0F;
		EffectEquip[ID.EF_DHIT] = 0F;
		EffectEquip[ID.EF_THIT] = 0F;
		EffectEquip[ID.EF_MISS] = 0F;
		EffectEquip[ID.EF_AA] = 0F;
		EffectEquip[ID.EF_ASM] = 0F;
		EffectEquip[ID.EF_DODGE] = 0F;
		
		//calc equip attrs
		for(int i=0; i<ContainerShipInventory.SLOTS_SHIPINV; i++) {
			equipStat = EquipCalc.getEquipStat(this, this.ExtProps.slots[i]);
			
			if(equipStat != null) {
				StateEquip[ID.HP] += equipStat[ID.E.HP];
				StateEquip[ID.DEF] += equipStat[ID.E.DEF];
				StateEquip[ID.SPD] += equipStat[ID.E.SPD];
				StateEquip[ID.MOV] += equipStat[ID.E.MOV];
				StateEquip[ID.HIT] += equipStat[ID.E.HIT];
				StateEquip[ID.ATK] += equipStat[ID.E.ATK_L];
				StateEquip[ID.ATK_H] += equipStat[ID.E.ATK_H];
				StateEquip[ID.ATK_AL] += equipStat[ID.E.ATK_AL];
				StateEquip[ID.ATK_AH] += equipStat[ID.E.ATK_AH];
				
				EffectEquip[ID.EF_CRI] += equipStat[ID.E.CRI];
				EffectEquip[ID.EF_DHIT] += equipStat[ID.E.DHIT];
				EffectEquip[ID.EF_THIT] += equipStat[ID.E.THIT];
				EffectEquip[ID.EF_MISS] += equipStat[ID.E.MISS];
				EffectEquip[ID.EF_AA] += equipStat[ID.E.AA];
				EffectEquip[ID.EF_ASM] += equipStat[ID.E.ASM];
				EffectEquip[ID.EF_DODGE] += equipStat[ID.E.DODGE];
			}
		}
		
		//calc attrs
		calcShipAttributes();
	}
	
	/** calc ship attrs */
	public void calcShipAttributes() {
		//get attrs value
		float[] getStat = Values.ShipAttrMap.get(this.getShipClass());

		//HP = (base + equip + (point + 1) * level * typeModify) * config scale
		StateFinal[ID.HP] = (getStat[ID.ShipAttr.BaseHP] + StateEquip[ID.HP] + (float)(BonusPoint[ID.HP]+1F) * (float)StateMinor[ID.M.ShipLevel] * TypeModify[ID.HP]) * (float)ConfigHandler.scaleShip[ID.HP]; 
		//DEF = base + ((point + 1) * level / 3 * 0.4 + equip) * typeModify
		StateFinal[ID.DEF] = (getStat[ID.ShipAttr.BaseDEF] + StateEquip[ID.DEF] + ((float)(BonusPoint[ID.DEF]+1F) * ((float)StateMinor[ID.M.ShipLevel])/3F) * 0.4F * TypeModify[ID.DEF]) * (float)ConfigHandler.scaleShip[ID.DEF];
		//SPD = base + ((point + 1) * level / 10 * 0.02 + equip) * typeModify
		StateFinal[ID.SPD] = (getStat[ID.ShipAttr.BaseSPD] + StateEquip[ID.SPD] + ((float)(BonusPoint[ID.SPD]+1F) * ((float)StateMinor[ID.M.ShipLevel])/10F) * 0.04F * TypeModify[ID.SPD]) * (float)ConfigHandler.scaleShip[ID.SPD];
		//MOV = base + ((point + 1) * level / 10 * 0.01 + equip) * typeModify
		StateFinal[ID.MOV] = (getStat[ID.ShipAttr.BaseMOV] + StateEquip[ID.MOV] + ((float)(BonusPoint[ID.MOV]+1F) * ((float)StateMinor[ID.M.ShipLevel])/10F) * 0.02F * TypeModify[ID.MOV]) * (float)ConfigHandler.scaleShip[ID.MOV];
		//HIT = base + ((point + 1) * level / 10 * 0.3 + equip) * typeModify
		StateFinal[ID.HIT] = (getStat[ID.ShipAttr.BaseHIT] + StateEquip[ID.HIT] + ((float)(BonusPoint[ID.HIT]+1F) * ((float)StateMinor[ID.M.ShipLevel])/10F) * 0.2F * TypeModify[ID.HIT]) * (float)ConfigHandler.scaleShip[ID.HIT];
		//ATK = (base + equip + ((point + 1) * level / 3) * 0.5 * typeModify) * config scale
		float atk = getStat[ID.ShipAttr.BaseATK] + ((float)(BonusPoint[ID.ATK]+1F) * ((float)StateMinor[ID.M.ShipLevel])/3F) * 0.4F * TypeModify[ID.ATK];
		
		//add equip
		StateFinal[ID.ATK] = (atk + StateEquip[ID.ATK]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_H] = (atk * 3F + StateEquip[ID.ATK_H]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_AL] = (atk + StateEquip[ID.ATK_AL]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_AH] = (atk * 3F + StateEquip[ID.ATK_AH]) * (float)ConfigHandler.scaleShip[ID.ATK];
		
		//backup value before buffs, for GUI display
		this.StateFinalBU = this.StateFinal.clone();
		this.EffectEquipBU = this.EffectEquip.clone();
		
		//update formation buff
		updateFormationBuffs();
		
		//apply formation buff
		if(this.getStateMinor(ID.M.FormatType) > 0) {
			//mul buff
			StateFinal[ID.ATK] = (EffectFormation[ID.Formation.ATK_L] * 0.01F + 1F) * StateFinal[ID.ATK];
			StateFinal[ID.ATK_H] = (EffectFormation[ID.Formation.ATK_H] * 0.01F + 1F) * StateFinal[ID.ATK_H];
			StateFinal[ID.ATK_AL] = (EffectFormation[ID.Formation.ATK_AL] * 0.01F + 1F) * StateFinal[ID.ATK_AL];
			StateFinal[ID.ATK_AH] = (EffectFormation[ID.Formation.ATK_AH] * 0.01F + 1F) * StateFinal[ID.ATK_AH];
			StateFinal[ID.DEF] = (EffectFormation[ID.Formation.DEF] * 0.01F + 1F) * StateFinal[ID.DEF];
			
			EffectEquip[ID.EF_CRI] = (EffectFormation[ID.Formation.CRI] * 0.01F + 1F) * EffectEquip[ID.EF_CRI];
			EffectEquip[ID.EF_DHIT] = (EffectFormation[ID.Formation.DHIT] * 0.01F + 1F) * EffectEquip[ID.EF_DHIT];
			EffectEquip[ID.EF_THIT] = (EffectFormation[ID.Formation.THIT] * 0.01F + 1F) * EffectEquip[ID.EF_THIT];
			EffectEquip[ID.EF_MISS] = (EffectFormation[ID.Formation.MISS] * 0.01F + 1F) * EffectEquip[ID.EF_MISS];
			EffectEquip[ID.EF_AA] = (EffectFormation[ID.Formation.AA] * 0.01F + 1F) * EffectEquip[ID.EF_AA];
			EffectEquip[ID.EF_ASM] = (EffectFormation[ID.Formation.ASM] * 0.01F + 1F) * EffectEquip[ID.EF_ASM];
			EffectEquip[ID.EF_DODGE] = (EffectFormation[ID.Formation.DODGE] * 0.01F + 1F) * EffectEquip[ID.EF_DODGE];
			
			//fixed buff
			StateFinal[ID.MOV] = getEffectFormationFixed(ID.FormationFixed.MOV);
		}
		
		//KB Resistance
		float resisKB = ((StateMinor[ID.M.ShipLevel])/10F) * 0.05F;

		//check limit
		this.StateFinal = this.checkStateFinalLimit(this.StateFinal);
		this.StateFinalBU = this.checkStateFinalLimit(this.StateFinalBU);
		this.EffectEquip = this.checkEffectEquipLimit(EffectEquip);
		this.EffectEquipBU = this.checkEffectEquipLimit(EffectEquipBU);
		
		//set attribute by final value
		/**
		 * DO NOT SET ATTACK DAMAGE to non-EntityMob!!!!!
		 */
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(StateFinal[ID.HP]);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(StateFinal[ID.MOV]);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(StateFinal[ID.HIT]+32); //此為找目標, 路徑的範圍
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(resisKB);
//		this.jumpMovementFactor = (1F + StateFinal[ID.MOV]) * 0.05F;
		
		//for server side, sync data to client
		if(!worldObj.isRemote) {
			sendSyncPacketAllValue();
			sendSyncPacketUnbuffValue();
		}
	}
	
	//set next exp value (for client load nbt data, gui display)
	public void setExpNext() {
		StateMinor[ID.M.ExpNext] = StateMinor[ID.M.ShipLevel] * ConfigHandler.expMod + ConfigHandler.expMod;
	}
		
	//called when entity exp++
	public void addShipExp(int exp) {
		int CapLevel = getStateFlag(ID.F.IsMarried) ? 150 : 100;
		
		if(StateMinor[ID.M.ShipLevel] != CapLevel && StateMinor[ID.M.ShipLevel] < 150) {	//level is not cap level
			StateMinor[ID.M.ExpCurrent] += exp;
			if(StateMinor[ID.M.ExpCurrent] >= StateMinor[ID.M.ExpNext]) {
				//level up sound
				this.worldObj.playSoundAtEntity(this, "random.levelup", 0.75F, 1.0F);
				StateMinor[ID.M.ExpCurrent] -= StateMinor[ID.M.ExpNext];	//level up
				StateMinor[ID.M.ExpNext] = (StateMinor[ID.M.ShipLevel] + 2) * ConfigHandler.expMod;
				setShipLevel(++StateMinor[ID.M.ShipLevel], true);
			}
		}	
	}
	
	@Override
	public void setShipDepth(double par1) {
		this.ShipDepth = par1;
	}
	
	//called when entity level up
	public void setShipLevel(int par1, boolean update) {
		//set level
		if(par1 < 151) {
			StateMinor[ID.M.ShipLevel] = par1;
		}
		//update attributes
		if(update) {
			LogHelper.info("DEBUG : set ship level with update");
			calcEquipAndUpdateState();
			this.setHealth(this.getMaxHealth());
		}
	}
	
	//prevent player use name tag
	@Override
	public void setCustomNameTag(String str) {}
	
	//custom name tag method
	public void setNameTag(String str) {
        this.dataWatcher.updateObject(10, str);
    }
	
	//called when a mob die near the entity (used in event handler)
	public void addKills() {
		StateMinor[ID.M.Kills]++;
	}
	
	@Override
	public void setAmmoLight(int num) {
		this.StateMinor[ID.M.NumAmmoLight] = num;
	}
	
	@Override
	public void setAmmoHeavy(int num) {
		this.StateMinor[ID.M.NumAmmoHeavy] = num;
	}
	
	public void setStateFinal(int id, float par1) {
		StateFinal[id] = par1;
	}
	
	public void setStateFinalBU(int id, float par1) {
		StateFinalBU[id] = par1;
	}
	
	@Override
	public void setStateMinor(int id, int par1) {
		StateMinor[id] = par1;
	}
	
	public void setUpdateFlag(int id, boolean par1) {
		UpdateFlag[id] = par1;
	}
	
	public void setEffectEquip(int id, float par1) {
		EffectEquip[id] = par1;
	}
	
	public void setEffectEquipBU(int id, float par1) {
		EffectEquipBU[id] = par1;
	}
	
	public void setEffectFormation(int id, float par1) {
		EffectFormation[id] = par1;
	}
	
	public void setEffectFormation(float[] par1) {
		EffectFormation = par1;
	}
	
	public void setEffectFormationFixed(int id, float par1) {
		EffectFormationFixed[id] = par1;
	}
	
	public void setBonusPoint(int id, byte par1) {
		BonusPoint[id] = par1;
	}
	
	@Override
	public void setEntitySit() {
		this.setSitting(!this.isSitting());
        this.isJumping = false;
        this.getShipNavigate().clearPathEntity();
        this.setPathToEntity(null);
        this.setTarget(null);
        this.setAttackTarget(null);
        this.setEntityTarget(null);
        
        if(this.ridingEntity instanceof BasicEntityMount) {
        	((BasicEntityMount) this.ridingEntity).getShipNavigate().clearPathEntity();
        	((BasicEntityMount) this.ridingEntity).getNavigator().clearPathEntity();
        	((BasicEntityMount) this.ridingEntity).setEntityTarget(null);
        }
	}
	
	//called when load nbt data or GUI click
	@Override
	public void setStateFlag(int id, boolean par1) {
		this.StateFlag[id] = par1;
		
		//若修改melee flag, 則reload AI
		if(!this.worldObj.isRemote) { 
			if(id == ID.F.UseMelee) {
				clearAITasks();
	    		setAIList();
	    		
	    		//設定mount的AI
				if(this.ridingEntity instanceof BasicEntityMount) {
					((BasicEntityMount) this.ridingEntity).clearAITasks();
					((BasicEntityMount) this.ridingEntity).setAIList();
				}
			}
			else if(id == ID.F.PassiveAI) {
				clearAITargetTasks();
				setAITargetList();
			}
		}
	}
	
	//called when load nbt data or GUI click
	public void setEntityFlagI(int id, int par1) {
		if(par1 > 0) {
			setStateFlag(id, true);
		}
		else {
			setStateFlag(id, false);
		}
	}
	
	//called when entity spawn, set the type modify
	public void initTypeModify() {
		//get attrs value
		float[] getStat = Values.ShipAttrMap.get(this.getShipClass());
				
		TypeModify[ID.HP] = getStat[ID.ShipAttr.ModHP];
		TypeModify[ID.ATK] = getStat[ID.ShipAttr.ModATK];
		TypeModify[ID.DEF] = getStat[ID.ShipAttr.ModDEF];
		TypeModify[ID.SPD] = getStat[ID.ShipAttr.ModSPD];
		TypeModify[ID.MOV] = getStat[ID.ShipAttr.ModMOV];
		TypeModify[ID.HIT] = getStat[ID.ShipAttr.ModHIT];
	}

	@Override
	public void setStateEmotion(int id, int value, boolean sync) {
		StateEmotion[id] = (byte)value;
		
		if(sync) {
			this.sendEmotionSyncPacket();
		}
	}
	
	//emotion start time (CLIENT ONLY), called from model class
	@Override
	public void setFaceTick(int par1) {
		this.EmotionTicks[0] = par1;
	}
	
	@Override
	public void setHeadTiltTick(int par1) {
		this.EmotionTicks[1] = par1;
	}
	
	@Override
	public void setAttackAniTick(int par1) {
		this.EmotionTicks[2] = par1;
	}
	
	public void setShipUID(int par1) {
		this.setStateMinor(ID.M.ShipUID, par1);
	}
	
	@Override
	public void setPlayerUID(int par1) {
		this.setStateMinor(ID.M.PlayerUID, par1);
	}
	

  	@Override
	public void setEntityTarget(Entity target) {
		this.atkTarget = target;
	}
  	
  	@Override
	public void setEntityRevengeTarget(Entity target) {
		this.rvgTarget = target;
	}
  	
  	@Override
	public void setEntityRevengeTime() {
		this.revengeTime = this.ticksExisted;
	}
  	
  	public void setGrudgeConsumption(int par1) {
  		if(par1 > 120) par1 = 120;
  		this.setStateMinor(ID.M.GrudgeCon, par1);
  	}
  	
  	public void setAmmoConsumption(int par1) {
  		if(par1 > 45) par1 = 45;
  		this.setStateMinor(ID.M.AmmoCon, par1);
  	}
	
	/** send sync packet: sync all data */
	public void sendSyncPacketAllValue() {
		this.sendSyncPacket(0, false, false);
	}
	
	/** send sync packet: sync unbuff data */
	public void sendSyncPacketUnbuffValue() {
		this.sendSyncPacket(S2CEntitySync.PID.SyncShip_Unbuff, false, false);
	}
	
	/** send sync packet: sync formation data */
	public void sendSyncPacketFormationValue() {
		this.sendSyncPacket(S2CEntitySync.PID.SyncShip_Formation, false, false);
	}
	
	/**  sync data for GUI display */
	public void sendGUISyncPacket() {
		if(!this.worldObj.isRemote) {
			if(this.getPlayerUID() > 0) {
				EntityPlayerMP player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID(), this.worldObj);
				//owner在附近才需要sync
				if(player != null && this.getDistanceToEntity(player) < 32F) {
					CommonProxy.channelG.sendTo(new S2CGUIPackets(this), player);
				}
			}
		}
	}
	
	/** sync data for emotion display */
	public void sendEmotionSyncPacket() {
		this.sendSyncPacket(1, true, true);
	}
	
	/** send sync packet:
	 *  type: 0: all  1: emotion  2: flag  3: minor
	 *  send all: send packet to all around player
	 *  sync emo: sync emotion to all around player
	 */
	public void sendSyncPacket(int type, boolean sendAll, boolean syncEmo) {
		if(!worldObj.isRemote) {
			//send to all player
			if(sendAll) {
				TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
				CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, type), point);
			}
			else {
				EntityPlayerMP player = null;
				//for owner, send all data
				if(this.getPlayerUID() > 0) {
					player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID(), this.worldObj);
				}
				
				//owner在附近才需要sync
				if(player != null && this.getDistanceToEntity(player) <= 64F) {
					CommonProxy.channelE.sendTo(new S2CEntitySync(this, type), player);
				}
			}
			
			if(syncEmo) {
				//send ship state for client display
				TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
				CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 1), point);
			}
		}
	}
	
	//right click on ship
	@Override
	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use item
		if(itemstack != null) {
			//use name tag, owner only
			if(itemstack.getItem() == Items.name_tag && EntityHelper.checkSameOwner(player, this)) {
	            //若該name tag有取名過, 則將名字貼到entity上
				if(itemstack.hasDisplayName()) {
					this.setNameTag(itemstack.getDisplayName());
					return false;
	            } 
			}
			
			//use repair bucket
			if(itemstack.getItem() == ModItems.BucketRepair) {
				//hp不到max hp時可以使用bucket
				if(this.getHealth() < this.getMaxHealth()) {
					if(!player.capabilities.isCreativeMode) {  //item-1 in non-creative mode
						--itemstack.stackSize;
	                }
	
	                if(this instanceof BasicEntityShipSmall) {
	                	this.heal(this.getMaxHealth() * 0.1F + 5F);	//1 bucket = 10% hp for small ship
	                }
	                else {
	                	this.heal(this.getMaxHealth() * 0.05F + 10F);	//1 bucket = 5% hp for large ship
	                }
	                
	                //airplane++
	                if(this instanceof BasicEntityShipCV)  {
	                	BasicEntityShipCV ship = (BasicEntityShipCV) this;
	                	ship.setNumAircraftLight(ship.getNumAircraftLight() + 1);
	                	ship.setNumAircraftHeavy(ship.getNumAircraftHeavy() + 1);
	                }
	                
	                if (itemstack.stackSize <= 0) {  
	                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
	                }
	                
	                return true;
	            }			
			}
			
			//use kaitai hammer, OWNER and OP only
			if(itemstack.getItem() == ModItems.KaitaiHammer && player.isSneaking() &&
			   (EntityHelper.checkSameOwner(this, player) || EntityHelper.checkOP(player)) ) {
				
				//client
				if(worldObj.isRemote) {
					return true;
				}
				//server
				else {
					//創造模式不消耗物品
	                if(!player.capabilities.isCreativeMode) {  //damage +1 in non-creative mode
	 	                itemstack.setItemDamage(itemstack.getItemDamage() + 1);
	                    
	                    //set item amount
	                    ItemStack item0 = null;
	                    ItemStack item1 = null;
	                    ItemStack item2 = null;
	                    ItemStack item3 = null;
	                                
	                    //calc materials amount
	                    switch(this.getKaitaiType()) {
	                    case 1:	  //large ship -> more materials
	                    	if(ConfigHandler.easyMode) {	//easy mode
	                    		item0 = new ItemStack(ModBlocks.BlockGrudge, 1);
	                        	item1 = new ItemStack(ModBlocks.BlockAbyssium, 1);
	                        	item2 = new ItemStack(ModItems.Ammo, 1, 1);
	                        	item3 = new ItemStack(ModBlocks.BlockPolymetal, 1);
	                    	}
	                    	else {						
	                    		item0 = new ItemStack(ModBlocks.BlockGrudge, 10 + rand.nextInt(3));
	                        	item1 = new ItemStack(ModBlocks.BlockAbyssium, 10 + rand.nextInt(3));
	                        	item2 = new ItemStack(ModItems.Ammo, 10 + rand.nextInt(3), 1);
	                        	item3 = new ItemStack(ModBlocks.BlockPolymetal, 10 + rand.nextInt(3));
	                    	}
	                    	break;
	                    case 2:	  //mob small ship
	                    	item0 = new ItemStack(ModItems.Grudge, ConfigHandler.kaitaiAmountSmall + rand.nextInt((int)(ConfigHandler.kaitaiAmountSmall * 0.25F) + 1));
	                    	item1 = new ItemStack(ModItems.AbyssMetal, ConfigHandler.kaitaiAmountSmall + rand.nextInt((int)(ConfigHandler.kaitaiAmountSmall * 0.25F) + 1), 0);
	                    	item2 = new ItemStack(ModItems.Ammo, ConfigHandler.kaitaiAmountSmall + rand.nextInt((int)(ConfigHandler.kaitaiAmountSmall * 0.25F) + 1), 0);
	                    	item3 = new ItemStack(ModItems.AbyssMetal, ConfigHandler.kaitaiAmountSmall + rand.nextInt((int)(ConfigHandler.kaitaiAmountSmall * 0.25F) + 1), 1);
	                    	break;
	                    case 3:	  //mob large ship
	                    	item0 = new ItemStack(ModBlocks.BlockGrudge, ConfigHandler.kaitaiAmountLarge + rand.nextInt((int)(ConfigHandler.kaitaiAmountLarge * 0.25F) + 1));
                        	item1 = new ItemStack(ModBlocks.BlockAbyssium, ConfigHandler.kaitaiAmountLarge + rand.nextInt((int)(ConfigHandler.kaitaiAmountLarge * 0.25F) + 1));
                        	item2 = new ItemStack(ModItems.Ammo, ConfigHandler.kaitaiAmountLarge + rand.nextInt((int)(ConfigHandler.kaitaiAmountLarge * 0.25F) + 1), 1);
                        	item3 = new ItemStack(ModBlocks.BlockPolymetal, ConfigHandler.kaitaiAmountLarge + rand.nextInt((int)(ConfigHandler.kaitaiAmountLarge * 0.25F) + 1));
	                    	break;
                    	default:  //small ship
                    		item0 = new ItemStack(ModItems.Grudge, 12 + rand.nextInt(8));
	                    	item1 = new ItemStack(ModItems.AbyssMetal, 12 + rand.nextInt(8), 0);
	                    	item2 = new ItemStack(ModItems.Ammo, 12 + rand.nextInt(8), 0);
	                    	item3 = new ItemStack(ModItems.AbyssMetal, 12 + rand.nextInt(8), 1);
                    		break;
	                    }
	                    
	                    EntityItem entityItem0 = new EntityItem(worldObj, posX+0.5D, posY+0.8D, posZ+0.5D, item0);
	                    EntityItem entityItem1 = new EntityItem(worldObj, posX+0.5D, posY+0.8D, posZ-0.5D, item1);
	                    EntityItem entityItem2 = new EntityItem(worldObj, posX-0.5D, posY+0.8D, posZ+0.5D, item2);
	                    EntityItem entityItem3 = new EntityItem(worldObj, posX-0.5D, posY+0.8D, posZ-0.5D, item3);

	                    worldObj.spawnEntityInWorld(entityItem0);
	                    worldObj.spawnEntityInWorld(entityItem1);
	                    worldObj.spawnEntityInWorld(entityItem2);
	                    worldObj.spawnEntityInWorld(entityItem3);
	                    
	                    //drop inventory item
	                    if(ExtProps != null) {
	                    	for(int i = 0; i < ExtProps.slots.length; i++) {
	            				ItemStack invitem = ExtProps.slots[i];

	            				if(invitem != null) {
	            					//設定要隨機噴出的range
	            					float f = rand.nextFloat() * 0.8F + 0.1F;
	            					float f1 = rand.nextFloat() * 0.8F + 0.1F;
	            					float f2 = rand.nextFloat() * 0.8F + 0.1F;

	            					while(invitem.stackSize > 0) {
	            						int j = rand.nextInt(21) + 10;
	            						//如果物品超過一個隨機數量, 會分更多疊噴出
	            						if(j > invitem.stackSize) {  
	            							j = invitem.stackSize;
	            						}

	            						invitem.stackSize -= j;
	            						//將item做成entity, 生成到世界上
	            						EntityItem item = new EntityItem(this.worldObj, this.posX+f, this.posY+f1, this.posZ+f2, new ItemStack(invitem.getItem(), j, invitem.getItemDamage()));
	            						//如果有NBT tag, 也要複製到物品上
	            						if(invitem.hasTagCompound()) {
	            							item.getEntityItem().setTagCompound((NBTTagCompound)invitem.getTagCompound().copy());
	            						}
	            						
	            						worldObj.spawnEntityInWorld(item);	//生成item entity
	            					}
	            				}
	            			}
	                    }//end drop inventory item
	                    
	                    playSound(Reference.MOD_ID+":ship-kaitai", ConfigHandler.shipVolume, 1.0F);
	                    playSound(Reference.MOD_ID+":ship-death", ConfigHandler.shipVolume, 1.0F);
	                }
	                
	                //物品用完時要設定為null清空該slot
	                if(itemstack.getItemDamage() >= itemstack.getMaxDamage()) {  //物品耐久度用完時要設定為null清空該slot
	                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
	                }
	                 
	                this.setDead();
	                
	                return true;
				}//end server side
			}//end kaitai hammer
			
			//use marriage ring
			if(itemstack.getItem() == ModItems.MarriageRing && !this.getStateFlag(ID.F.IsMarried) && 
			   player.isSneaking() && EntityHelper.checkSameOwner(this, player)) {
				//stack-1 in non-creative mode
				if(!player.capabilities.isCreativeMode) {
                    --itemstack.stackSize;
                }

				//set marriage flag
                this.setStateFlag(ID.F.IsMarried, true);
                
                //player marriage num +1
    			ExtendPlayerProps extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);		
    			if(extProps != null) {
    				extProps.setMarriageNum(extProps.getMarriageNum() + 1);
    			}
                
                //play hearts effect
                TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
    			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 3, false), point);
                
    			//play marriage sound
    	        this.playSound(Reference.MOD_ID+":ship-love", ConfigHandler.shipVolume, 1.0F);
    	        
    	        //add 3 random bonus point
    	        for(int i = 0; i < 3; ++i) {
    	        	addRandomBonusPoint();
    	        }
    	        
    	        this.calcEquipAndUpdateState();
    	        
                if(itemstack.stackSize <= 0) {  //物品用完時要設定為null清空該slot
                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
                }
                
                return true;
			}//end wedding ring
			
			//use modernization kit
			if(itemstack.getItem() == ModItems.ModernKit) {
				if(addRandomBonusPoint()) {	//add 1 random bonus
					if(!player.capabilities.isCreativeMode) {
	                    --itemstack.stackSize;
	                    
	                    if(itemstack.stackSize <= 0) {  //物品用完時要設定為null清空該slot
	                    	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
	                    }
	                }
					
					//play marriage sound
	    	        this.playSound(Reference.MOD_ID+":ship-love", ConfigHandler.shipVolume, 1.0F);
					
					return true;
				}	
			}//end modern kit
			
			//use modernization kit, owner only
			if(itemstack.getItem() == ModItems.OwnerPaper && EntityHelper.checkSameOwner(this, player)) {
				NBTTagCompound nbt = itemstack.getTagCompound();
				boolean changeOwner = false;
				
				/**change owner method:
				 * 1. check owner paper has 2 signs
				 * 2. check owner is A or B
				 * 3. get player entity
				 * 4. change ship's player UID
				 * 5. change ship's owner UUID
				 */
				if(nbt != null) {
					int ida = nbt.getInteger(OwnerPaper.SignIDA);
					int idb = nbt.getInteger(OwnerPaper.SignIDB);
					int idtarget = -1;	//target player uid
					
					//1. check 2 signs
					if(ida > 0 && idb > 0) {
						//2. check owner is A or B
						if(ida == this.getPlayerUID()) {	//A is owner
							idtarget = idb;
						}
						else {	//B is owner
							idtarget = ida;
						}
						
						//3. check player online
						EntityPlayer target = EntityHelper.getEntityPlayerByUID(idtarget, this.worldObj);
						
						if(target != null) {
							ExtendPlayerProps extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
							
							if(extProps != null) {
								//4. change ship's player UID
								this.setPlayerUID(idtarget);
								
								//5. change ship's owner UUID
								this.func_152115_b(target.getUniqueID().toString());
								
								LogHelper.info("DEBUG : change owner: from: pid "+this.getPlayerUID()+" uuid "+this.getOwner().getUniqueID());
								LogHelper.info("DEBUG : change owner: to: pid "+idtarget+" uuid "+target.getUniqueID().toString());
								changeOwner = true;
								
								//send sync packet
								this.sendSyncPacket(3, true, false);
							}//end player has data
						}//end target != null
					}//end item has 2 signs
				}//end item has nbt
				
				if(changeOwner) {
					//play marriage sound
	    	        this.playSound(Reference.MOD_ID+":ship-death", ConfigHandler.shipVolume, 1.0F);
	    	        
					player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
					return true;
				}
			}//end owner paper
			
			//use lead
			if(itemstack.getItem() == Items.lead && this.allowLeashing() && EntityHelper.checkSameOwner(this, player)) {
				this.getShipNavigate().clearPathEntity();
				this.setLeashedToEntity(player, true);
				return true;
	        }//end lead
			
		}//end item != null
		
		//如果已經被綑綁, 再點一下可以解除綑綁
		if(this.getLeashed() && this.getLeashedToEntity() == player) {
            this.clearLeashed(true, !player.capabilities.isCreativeMode);
            return true;
        }
	
		//shift+right click時打開GUI
		if(player.isSneaking() && EntityHelper.checkSameOwner(this, player)) {
			FMLNetworkHandler.openGui(player, ShinColle.instance, ID.G.SHIPINVENTORY, this.worldObj, this.getEntityId(), 0, 0);
    		return true;
		}
		
		//click ship without shift = sit
		if(!this.worldObj.isRemote && !player.isSneaking() && EntityHelper.checkSameOwner(this, player)) {			
			//current item = pointer
			if(itemstack != null && itemstack.getItem() == ModItems.PointerItem) {
				//call sitting method by PointerItem class, not here
			}
			else {
				this.setEntitySit();
			}
			
            return true;
        }

		return false;
	}
	
	//add random bonus point, NO SYNC, server only!
	private boolean addRandomBonusPoint() {
		int bonusChoose = rand.nextInt(6);
		
		//bonus point +1 if bonus point < 3
		if(BonusPoint[bonusChoose] < 3) {
			BonusPoint[bonusChoose] = (byte) (BonusPoint[bonusChoose] + 1);
			return true;
		}
		else {	//select other bonus point
			for(int i = 0; i < BonusPoint.length; ++i) {
				if(BonusPoint[i] < 3) {
					BonusPoint[i] = (byte) (BonusPoint[i] + 1);
					return true;
				}
			}
		}
		
		return false;
	}

	/**修改移動方法, 使其water跟lava中移動時像是flying entity
     * Moves the entity based on the specified heading.  Args: strafe, forward
     */
	@Override
    public void moveEntityWithHeading(float movX, float movZ) {
        double d0;

        if(EntityHelper.checkEntityIsInLiquid(this)) { //判定為液體中時, 不會自動下沉
            d0 = this.posY;
            this.moveFlying(movX, movZ, this.getStateFinal(ID.MOV)*0.4F); //水中的速度計算(含漂移效果)
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            //水中阻力
            this.motionX *= 0.8D;
            this.motionY *= 0.8D;
            this.motionZ *= 0.8D;
            //水中撞到東西會上升
            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6D - this.posY + d0, this.motionZ)) {
                this.motionY = 0.3D;
            }
        }
        else {									//其他移動狀態
            float f2 = 0.91F;
            
            if(this.onGround) {					//在地面移動
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            float f3 = 0.16277136F / (f2 * f2 * f2);
            float f4;
            
            if(this.onGround) {
                f4 = this.getAIMoveSpeed() * f3;
            }
            else {								//跳躍中
                f4 = this.jumpMovementFactor;
            }
            this.moveFlying(movX, movZ, f4);
            f2 = 0.91F;
            
            if(this.onGround) {
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            if(this.isOnLadder()) {				//爬樓梯中
                float f5 = 0.15F;
                //限制爬樓梯時的橫向移動速度
                if(this.motionX < (-f5)) {
                    this.motionX = (-f5);
                }
                if(this.motionX > f5) {
                    this.motionX = f5;
                }
                if(this.motionZ < (-f5)) {
                    this.motionZ = (-f5);
                }
                if(this.motionZ > f5) {
                    this.motionZ = f5;
                }

                this.fallDistance = 0.0F;
                //限制爬樓梯的落下速度
                if (this.motionY < -0.15D) {
                    this.motionY = -0.15D;
                }

                boolean flag = this.isSneaking();
                //若是爬樓梯時為sneaking, 則不會落下(卡在樓梯上)
                if(flag && this.motionY < 0D) {
                    this.motionY = 0D;
                }
            }

            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            //往樓梯推擠, 則會往上爬
            if(this.isCollidedHorizontally && this.isOnLadder()) {
                this.motionY = 0.4D;
            }
            //自然掉落
            if(this.worldObj.isRemote && (!this.worldObj.blockExists((int)this.posX, 0, (int)this.posZ) || !this.worldObj.getChunkFromBlockCoords((int)this.posX, (int)this.posZ).isChunkLoaded)) {
                if (this.posY > 0.0D) {
                    this.motionY = -0.1D;	//空氣中的gravity為0.1D
                }
                else {
                    this.motionY = 0.0D;
                }
            }
            else {
                this.motionY -= 0.08D;
            }
            //空氣中的三方向阻力
            this.motionY *= 0.98D;			
            this.motionX *= f2;
            this.motionZ *= f2;
//            LogHelper.info("DEBUG : f2 "+f2+" ");
        }
        //計算四肢擺動值
        this.prevLimbSwingAmount = this.limbSwingAmount;
        d0 = this.posX - this.prevPosX;
        double d1 = this.posZ - this.prevPosZ;
        float f6 = MathHelper.sqrt_double(d0 * d0 + d1 * d1) * 4.0F;

        if (f6 > 1.0F)
        {
            f6 = 1.0F;
        }

        this.limbSwingAmount += (f6 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }
	
	//update ship move helper
	@Override
	protected void updateAITasks() {
		super.updateAITasks();
		
        EntityHelper.updateShipNavigator(this);
    }
	
	/** update entity 
	 *  在此用onUpdate跟onLivingUpdate區分server跟client update
	 *  for shincolle:
	 *  onUpdate = client update only
	 *  onLivingUpdate = server update only
	 */
	@Override
	public void onUpdate() {
		super.onUpdate();
		EntityHelper.checkDepth(this);	//both side
		
		//both side
		if(this.StartSoundHurt > 0) this.StartSoundHurt--;
		
		//client side
		if(this.worldObj.isRemote) {
			//有移動時, 產生水花特效
			if(this.getShipDepth() > 0D) {
				//(注意此entity因為設為非高速更新, client端不會更新motionX等數值, 需自行計算)
				double motX = this.posX - this.prevPosX;
				double motZ = this.posZ - this.prevPosZ;
				double parH = this.posY - (int)this.posY;
				double limit = 0.25D;
				
				if(motX > limit) motX = limit;
				else if(motX < -limit) motX = -limit;
				
				if(motZ > limit) motZ = limit;
				else if(motZ < -limit) motZ = -limit;
				
				
				if(motX != 0 || motZ != 0) {
					ParticleHelper.spawnAttackParticleAt(this.posX + motX*1.5D, this.posY + 0.4D, this.posZ + motZ*1.5D, 
							-motX*0.5D, 0D, -motZ*0.5D, (byte)15);
				}
			}
			
			if(this.ticksExisted % 16 == 0) {
				//generate HP state effect, use parm:lookX to send width size
				switch(getStateEmotion(ID.S.HPState)) {
				case ID.HPState.MINOR:
					ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
							this.width, 0.05D, 0D, (byte)4);
					break;
				case ID.HPState.MODERATE:
					ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
							this.width, 0.05D, 0D, (byte)5);
					break;
				case ID.HPState.HEAVY:
					ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
							this.width, 0.05D, 0D, (byte)7);
					break;
				default:
					break;
				}
			}//end every 16 ticks
			
			if(this.ticksExisted % 32 == 0) {
				//show guard position
				if(!this.getStateFlag(ID.F.CanFollow)) {
					//set guard entity
					if(this.getStateMinor(ID.M.GuardID) > 0) {
						Entity getEnt = EntityHelper.getEntityByID(this.getStateMinor(ID.M.GuardID), 0, true);
						this.setGuardedEntity(getEnt);
					}
					else {
						//reset guard entity
						this.setGuardedEntity(null);
					}
				}//end show pointer target effect
				
				//display circle particle, 只有owner才會接收到該ship同步的EID, 非owner讀取到的EID <= 0
				//get owner entity
				EntityPlayer player = null;
				if(this.getStateMinor(ID.M.PlayerEID) > 0) {
					player = EntityHelper.getEntityPlayerByID(getStateMinor(ID.M.PlayerEID), 0, true);
				}
				
				//show circle particle on ship and guard target
				if(player != null && player.dimension == this.getGuardedPos(3)) {
					ItemStack item = player.inventory.getCurrentItem();
					
					if(ConfigHandler.alwaysShowTeamParticle || (item != null && item.getItem() instanceof PointerItem)) {
						//show friendly particle
						ParticleHelper.spawnAttackParticleAtEntity(this, 0.3D, 7D, 0D, (byte)2);
						
						//show guard particle
						//標記在entity上
						if(this.getGuardedEntity() != null) {
							ParticleHelper.spawnAttackParticleAtEntity(this.getGuardedEntity(), 0.3D, 6D, 0D, (byte)2);
							ParticleHelper.spawnAttackParticleAtEntity(this, this.getGuardedEntity(), 0D, 0D, 0D, (byte)3, false);
						}
						//標記在block上
						else if(this.getGuardedPos(1) >= 0) {
							ParticleHelper.spawnAttackParticleAt(this.getGuardedPos(0)+0.5D, this.getGuardedPos(1), this.getGuardedPos(2)+0.5D, 0.3D, 6D, 0D, (byte)25);
							ParticleHelper.spawnAttackParticleAtEntity(this, this.getGuardedPos(0)+0.5D, this.getGuardedPos(1)+0.2D, this.getGuardedPos(2)+0.5D, (byte)8);
						}
					}
				}
				else {
					this.setStateMinor(ID.M.PlayerEID, -1);
				}
				
				//TODO DEBUG TEST
//				ParticleHelper.spawnAttackParticleAtEntity(this, 1D, 1D, 0D, (byte)4);
			}//end every 32 ticks
		}//end client side
	}

	/** update living entity 
	 *  在此用onUpdate跟onLivingUpdate區分server跟client update
	 *  onUpdate = client update only
	 *  onLivingUpdate = server update only
	 */
	@Override
	public void onLivingUpdate() {
        super.onLivingUpdate();
        
        //debug
//      	LogHelper.info("DEBUG : ship onUpdate: flag: side: "+this.worldObj.isRemote+" "+this.StateMinor[ID.M.GuardType]);
      	
        //server side check
        if((!worldObj.isRemote)) {
        	//update target
        	TargetHelper.updateTarget(this);
        	
        	//update/init id
        	this.updateShipID();
        	
        	//decr immune time
        	if(this.StateMinor[ID.M.ImmuneTime] > 0) {
        		this.StateMinor[ID.M.ImmuneTime]--;
        	}
        	
        	//check every 8 ticks
        	if(ticksExisted % 8 == 0) {
        		//update formation buff (fast update)
        		if(this.getUpdateFlag(ID.FU.FormationBuff)) calcEquipAndUpdateState();
        		
        		//reset AI and sync once
        		if(!this.initAI && ticksExisted > 10) {
        			setStateFlag(ID.F.CanDrop, true);
            		clearAITasks();
            		clearAITargetTasks();	//reset AI for get owner after loading NBT data
            		setAIList();
            		setAITargetList();
            		decrGrudgeNum(0);		//check grudge
            		sendSyncPacketAllValue();		//sync packet to client
            		
            		this.initAI = true;
        		}
//        		LogHelper.info("DEBUG : check spawn: "+this.worldObj.getChunkProvider().getPossibleCreatures(EnumCreatureType.waterCreature, (int)posX, (int)posY, (int)posZ));
        	
        		//check every 16 ticks
            	if(ticksExisted % 16 == 0) {
            		//cancel mounts
            		if(this.canSummonMounts()) {
            			if(getStateEmotion(ID.S.State) < ID.State.EQUIP00) {
          	  	  			//cancel riding
          	  	  			if(this.isRiding() && this.ridingEntity instanceof BasicEntityMount) {
          	  	  				BasicEntityMount mount = (BasicEntityMount) this.ridingEntity;
          	  	  				
          	  	  				if(mount.seat2 != null ) {
          	  	  					mount.seat2.setRiderNull();	
          	  	  				}
          	  	  				
          	  	  				mount.setRiderNull();
          	  	  				this.ridingEntity = null;
          	  	  			}
          	  	  		}
            		}
            		
                	/** debug info */
//            		LogHelper.info("DEBUG: target:   "+this.getClass().getSimpleName()+" "+this.getEntityTarget()+"      "+this.getEntityRevengeTarget()+"      "+this.getAttackTarget());
//                	LogHelper.info("DEBUG : ship update: "+ServerProxy.getTeamData(900).getTeamBannedList());
//                	LogHelper.info("DEBUG : ship update: eid: "+ServerProxy.getNextShipID()+" "+ServerProxy.getNextPlayerID()+" "+ConfigHandler.nextPlayerID+" "+ConfigHandler.nextShipID);
//            		if(this.worldObj.provider.dimensionId == 0) {	//main world
//            			LogHelper.info("DEBUG : ship pos dim "+ClientProxy.getClientWorld().provider.dimensionId+" "+this.dimension+" "+this.posX+" "+this.posY+" "+this.posZ);
//            		}
//            		else {	//other world
//            			LogHelper.info("DEBUG : ship pos dim "+ClientProxy.getClientWorld().provider.dimensionId+" "+this.dimension+" "+this.posX+" "+this.posY+" "+this.posZ);
//            		}
            		
            		//check every 32 ticks
                	if(this.ticksExisted % 32 == 0) {
                		//use bucket automatically
                		if((getMaxHealth() - getHealth()) > (getMaxHealth() * 0.1F + 5F)) {
        	                if(decrSupplies(7)) {
        		                this.heal(this.getMaxHealth() * 0.08F + 15F);	//1 bucket = 5% hp for large ship
        	                
        		                //airplane++
        		                if(this instanceof BasicEntityShipCV)  {
        		                	BasicEntityShipCV ship = (BasicEntityShipCV) this;
        		                	ship.setNumAircraftLight(ship.getNumAircraftLight() + 1);
        		                	ship.setNumAircraftHeavy(ship.getNumAircraftHeavy() + 1);
        		                }
        	                }
        	            }
               		
                		//check every 128 ticks
                    	if(ticksExisted % 128 == 0) {
                    		//delayed init, waiting for player entity loaded
                    		if(!this.initWaitAI && ticksExisted >= 128) {
                    			setUpdateFlag(ID.FU.FormationBuff, true);  //set update formation buff
                    			this.initWaitAI = true;
                    		}
                    		
                    		//check owner online
                    		if(this.getPlayerUID() > 0) {
                    			//get owner
                    			EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID(), this.worldObj);

                    			//owner exists (online and same world)
                    			if(player != null) {
                					//update owner entity id (could be changed when owner change dimension or dead)
                        			this.setStateMinor(ID.M.PlayerEID, player.getEntityId());
                        			//sync guard
                        			this.sendSyncPacket(3, false, false);
                        		}
                    		}
                    		
                    		//update hp state
                    		updateEmotionState();
                    		
                    		//update mount
                    		updateMountSummon();
                    		
                    		//update consume item
                    		updateConsumeItem();
                    		
                    		//check every 256 ticks
                        	if(this.ticksExisted % 256 == 0) {
                        		//update buff (slow update)
                        		calcEquipAndUpdateState();
                        		
                        		//HP auto regen
                        		if(this.getHealth() < this.getMaxHealth()) {
                        			this.setHealth(this.getHealth() + this.getMaxHealth() * 0.015625F + 1);
                        		}
                        	}//end every 256 ticks
                    	}//end every 128 ticks
                	}//end every 32 ticks
            	}//end every 16 ticks
        	}//end every 8 ticks
        	
        	//play timekeeping sound
        	if(ConfigHandler.timeKeeping && this.getStateFlag(ID.F.TimeKeeper)) {
        		long time = this.worldObj.provider.getWorldTime();
            	int checkTime = (int)(time % 1000L);
            	
            	if(checkTime == 0) {
            		playTimeSound((int)(time / 1000L) % 24);
            	}
        	}//end timekeeping
        	
        }//end if(server side)
    }

	//timekeeping method
	protected void playTimeSound(int i) {
		//play entity attack sound
        this.playSound(Reference.MOD_ID+":ship-time"+i, ConfigHandler.timeKeepingVolume, 1.0F);
	}
	
	//melee attack method, no ammo cost, no attack speed, damage = 12.5% atk
	@Override
	public boolean attackEntityAsMob(Entity target) {
		//get attack value
		float atk = StateFinal[ID.ATK] * 0.125F;
		
		//experience++
		addShipExp(1);
		
		//grudge--
		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.LAtk]);
				
	    //將atk跟attacker傳給目標的attackEntityFrom方法, 在目標class中計算傷害
	    //並且回傳是否成功傷害到目標
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);

	    //play entity attack sound
        if(this.getRNG().nextInt(10) > 6) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
	    
	    //if attack success
	    if(isTargetHurt) {
	        //send packet to client for display partical effect   
	        if (!worldObj.isRemote) {
	        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
	    		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 1, false), point);
			}
	    }

	    return isTargetHurt;
	}
	
	//range attack method, cost light ammo, attack delay = 20 / attack speed, damage = 100% atk 
	@Override
	public boolean attackEntityWithAmmo(Entity target) {	
		//get attack value
		float atk = CalcHelper.calcDamageBySpecialEffect(this, target, StateFinal[ID.ATK], 0);
        
        //experience++
  		addShipExp(2);
  		
  		//grudge--
  		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.LAtk]);
  		
        //light ammo -1
        if(!decrAmmoNum(0, this.getAmmoConsumption())) {		//not enough ammo
        	return false;
        }
        
        //calc dist to target
        float distX = (float) (target.posX - this.posX);
        float distY = (float) (target.posY - this.posY);
        float distZ = (float) (target.posZ - this.posZ);
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        distX = distX / distSqrt;
        distY = distY / distSqrt;
        distZ = distZ / distSqrt;
        
        //play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-firesmall", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.rand.nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //發射者煙霧特效
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 6, this.posX, this.posY, this.posZ, distX, distY, distZ, true), point);

        //calc miss chance, if not miss, calc cri/multi hit
        float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.M.ShipLevel];
        missChance -= EffectEquip[ID.EF_MISS];		//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance
        
        //calc miss -> crit -> double -> tripple
  		if(rand.nextFloat() < missChance) {
          	atk = 0F;	//still attack, but no damage
          	//spawn miss particle
      		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
  		}
  		else {
  			//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
  			//calc critical
          	if(rand.nextFloat() < this.getEffectEquip(ID.EF_CRI)) {
          		atk *= 1.5F;
          		//spawn critical particle
          		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 11, false), point);
          	}
          	else {
          		//calc double hit
              	if(rand.nextFloat() < this.getEffectEquip(ID.EF_DHIT)) {
              		atk *= 2F;
              		//spawn double hit particle
              		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 12, false), point);
              	}
              	else {
              		//calc double hit
                  	if(rand.nextFloat() < this.getEffectEquip(ID.EF_THIT)) {
                  		atk *= 3F;
                  		//spawn triple hit particle
                  		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 13, false), point);
                  	}
              	}
          	}
  		}
  		
  		//calc damage to player
  		if(target instanceof EntityPlayer) {
  			atk *= 0.25F;
    			
  			//check friendly fire
      		if(!ConfigHandler.friendlyFire) {
      			atk = 0F;
      		}
      		else if(atk > 59F) {
      			atk = 59F;	//same with TNT
      		}
  		}
      		
	    //將atk跟attacker傳給目標的attackEntityFrom方法, 在目標class中計算傷害
	    //並且回傳是否成功傷害到目標
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);

	    //if attack success
	    if(isTargetHurt) {
        	//display hit particle on target
	        TargetPoint point1 = new TargetPoint(this.dimension, target.posX, target.posY, target.posZ, 64D);
			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point1);
        }

	    return isTargetHurt;
	}

	//range attack method, cost heavy ammo, attack delay = 100 / attack speed, damage = 500% atk
	@Override
	public boolean attackEntityWithHeavyAmmo(Entity target) {	
		//get attack value
		float atk = StateFinal[ID.ATK_H];
		float kbValue = 0.15F;
		
		//飛彈是否採用直射
		boolean isDirect = false;
		//計算目標距離
		float tarX = (float)target.posX;	//for miss chance calc
		float tarY = (float)target.posY;
		float tarZ = (float)target.posZ;
		float distX = tarX - (float)this.posX;
		float distY = tarY - (float)this.posY;
		float distZ = tarZ - (float)this.posZ;
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        float launchPos = (float)posY + height * 0.7F;
        
        //超過一定距離/水中 , 則採用拋物線,  在水中時發射高度較低
        if((distX*distX+distY*distY+distZ*distZ) < 36F) {
        	isDirect = true;
        }
        if(getShipDepth() > 0D) {
        	isDirect = true;
        	launchPos = (float)posY;
        }
		
		//experience++
		addShipExp(16);
		
		//grudge--
		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.HAtk]);
	
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //heavy ammo--
        if(!decrAmmoNum(1, this.getAmmoConsumption())) {
        	return false;
        }
        
        //calc miss chance, miss: add random offset(0~6) to missile target 
        float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.M.ShipLevel];
        missChance -= EffectEquip[ID.EF_MISS];	//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance = 30%
       
        if(this.rand.nextFloat() < missChance) {
        	tarX = tarX - 5F + this.rand.nextFloat() * 10F;
        	tarY = tarY + this.rand.nextFloat() * 5F;
        	tarZ = tarZ - 5F + this.rand.nextFloat() * 10F;
        	//spawn miss particle
        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
        }
        
        //spawn missile
        EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this, 
        		tarX, tarY+target.height*0.2F, tarZ, launchPos, atk, kbValue, isDirect, -1F);
        this.worldObj.spawnEntityInWorld(missile);
        
        return true;
	}
	
	//be attacked method, 包括其他entity攻擊, anvil攻擊, arrow攻擊, fall damage都使用此方法 
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
		//set hurt face
    	if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.O_O) {
    		this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
    	}

    	//若攻擊方為owner, 則直接回傳傷害, 不計def跟friendly fire
		if(attacker.getEntity() instanceof EntityPlayer &&
		   EntityHelper.checkSameOwner(attacker.getEntity(), this)) {
			this.setSitting(false);
			return super.attackEntityFrom(attacker, atk);
		}
        
        //若掉到世界外, 則傳送回y=4
        if(attacker.getDamageType().equals("outOfWorld")) {
        	//取消坐下動作
			this.setSitting(false);
			this.mountEntity(null);
        	this.setPositionAndUpdate(this.posX, 4D, this.posZ);
        	this.motionX = 0D;
        	this.motionY = 0D;
        	this.motionZ = 0D;
        	return false;
        }
        
        //無敵的entity傷害無效
		if(this.isEntityInvulnerable()) {
            return false;
        }
		else if(attacker.getEntity() != null) {	//不為null才算傷害, 可免疫毒/掉落/窒息等傷害
			Entity entity = attacker.getEntity();
			
			//不會對自己造成傷害, 可免疫毒/掉落/窒息等傷害 (此為自己對自己造成傷害)
			if(entity.equals(this)) {
				//取消坐下動作
				this.setSitting(false);
				return false;
			}
			
			//若攻擊方為player, 則檢查friendly fire
			if(entity instanceof EntityPlayer) {
				//若禁止friendlyFire, 則不造成傷害
				if(!ConfigHandler.friendlyFire) {
					return false;
				}
			}
			
			//進行dodge計算
			float dist = (float) this.getDistanceSqToEntity(entity);
			if(EntityHelper.canDodge(this, dist)) {
				return false;
			}
			
			//進行def計算
			float reduceAtk = atk * (1F - (StateFinal[ID.DEF] - rand.nextInt(20) + 10F) / 100F);    
			
			//ship vs ship, config傷害調整
			if(entity instanceof BasicEntityShip || entity instanceof BasicEntityAirplane || 
			   entity instanceof EntityRensouhou || entity instanceof BasicEntityMount) {
				reduceAtk = reduceAtk * (float)ConfigHandler.dmgSvS * 0.01F;
			}
			
			//ship vs ship, damage type傷害調整
			if(entity instanceof IShipAttackBase) {
				//get attack time for damage modifier setting (day, night or ...etc)
				int modSet = this.worldObj.provider.isDaytime() ? 0 : 1;
				reduceAtk = CalcHelper.calcDamageByType(reduceAtk, ((IShipAttackBase) entity).getDamageType(), this.getDamageType(), modSet);
			}
			
			//min damage設為1
	        if(reduceAtk < 1) reduceAtk = 1;

			//取消坐下動作
			this.setSitting(false);
			
			//設置revenge target
			this.setEntityRevengeTarget(entity);
			this.setEntityRevengeTime();
//			LogHelper.info("DEBUG : set revenge target: "+entity+"  host: "+this);
			
			//若傷害力可能致死, 則尋找物品中有無repair goddess來取消掉此攻擊
			if(reduceAtk >= (this.getHealth() - 1F)) {
				if(this.decrSupplies(8)) {
					this.setHealth(this.getMaxHealth());
					this.StateMinor[ID.M.ImmuneTime] = 60;
					
					//TODO add repair goddess particle
					
					return false;
				}
			}
   
            //執行父class的被攻擊判定, 包括重置love時間, 計算火毒抗性, 計算鐵砧/掉落傷害, 
            //hurtResistantTime(0.5sec無敵時間)計算, 
            return super.attackEntityFrom(attacker, reduceAtk);
        }
		
		return false;
    }
	
	/** decr ammo, type: 0:light, 1:heavy */
	protected boolean decrAmmoNum(int type, int amount) {
		int ammoType = ID.M.NumAmmoLight;
		boolean useItem = !hasAmmoLight();
		
		switch(type) {
		case 1:   //use heavy ammo
			ammoType = ID.M.NumAmmoHeavy;
			useItem = !hasAmmoHeavy();
			break;
		}
		
		//check ammo first time
		if(StateMinor[ammoType] <= amount || useItem) {
			int addAmmo = 0;
			
			//use light ammo item
			if(ammoType == ID.M.NumAmmoLight) {
				if(decrSupplies(0)) {  //use ammo item
					addAmmo = 30;
				}
				else if(decrSupplies(2)) {  //use ammo container item
					addAmmo = 270;
				}
			}
			//use heavy ammo item
			else {
				if(decrSupplies(1)) {  //use ammo item
					addAmmo = 15;
				}
				else if(decrSupplies(3)) {  //use ammo container item
					addAmmo = 135;
				}
			}
			
			//check easy mode
			if(ConfigHandler.easyMode) {
				addAmmo *= 10;
			}
			
			StateMinor[ammoType] += addAmmo;
		}
		
		//check ammo second time
		if(StateMinor[ammoType] < amount) {
			return false;
		}
		else {
			StateMinor[ammoType] -= amount;
			return true;
		}
	}
	
	//eat grudge and change movement speed
	protected void decrGrudgeNum(int par1) {
		//check max cost < 120
		if(par1 > 215) {
			par1 = 215;
		}
		
		//check fuel flag
		if(!getStateFlag(ID.F.NoFuel)) {  //has fuel
			StateMinor[ID.M.NumGrudge] -= par1;
		}
		
		//eat one grudge if fuel <= 0
		if(StateMinor[ID.M.NumGrudge] <= 0) {
			//try to find grudge
			if(decrSupplies(4)) {		//find grudge
				if(ConfigHandler.easyMode) {
					StateMinor[ID.M.NumGrudge] += 3000;
				}
				else {
					StateMinor[ID.M.NumGrudge] += 300;
				}
			}
			else {
				if(decrSupplies(5)) {	//find grudge block
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumGrudge] += 27000;
					}
					else {
						StateMinor[ID.M.NumGrudge] += 2700;
					}
				}
			}
			//避免吃掉含有儲存資訊的方塊, 因此停用heavy grudge block作為補充道具
		}
		
		//check fuel again and set fuel flag
		if(StateMinor[ID.M.NumGrudge] <= 0) {
			setStateFlag(ID.F.NoFuel, true);
		}
		else {
			setStateFlag(ID.F.NoFuel, false);
		}
		
		//check fuel flag and set AI
		if(getStateFlag(ID.F.NoFuel)) { //no fuel, clear AI
			//原本有AI, 則清除之
			if(this.targetTasks.taskEntries.size() > 0) {
//				LogHelper.info("DEBUG : No fuel, clear AI "+this);
				clearAITasks();
				clearAITargetTasks();
				sendSyncPacketAllValue();
				
				//設定mount的AI
				if(this.ridingEntity instanceof BasicEntityMount) {
					((BasicEntityMount) this.ridingEntity).clearAITasks();
				}
			}	
		}
		else { //has fuel, set AI
			if(this.targetTasks.taskEntries.size() < 1) {
//				LogHelper.info("DEBUG : Get fuel, set AI "+this);
				clearAITasks();
				clearAITargetTasks();
				setAIList();
				setAITargetList();
				sendSyncPacketAllValue();
				
				//設定mount的AI
				if(this.ridingEntity instanceof BasicEntityMount) {
					((BasicEntityMount) this.ridingEntity).clearAITasks();
					((BasicEntityMount) this.ridingEntity).setAIList();
				}
			}
		}
	}
	
	//decrese ammo/grudge/repair item, return true or false(not enough item)
	protected boolean decrSupplies(int type) {
		int itemNum = 1;
		ItemStack itemType = null;
		
		//find ammo
		switch(type) {
		case 0:	//use 1 light ammo
			itemType = new ItemStack(ModItems.Ammo,1,0);
			break;
		case 1: //use 1 heavy ammo
			itemType = new ItemStack(ModItems.Ammo,1,2);
			break;
		case 2:	//use 1 light ammo container
			itemType = new ItemStack(ModItems.Ammo,1,1);
			break;
		case 3: //use 1 heavy ammo container
			itemType = new ItemStack(ModItems.Ammo,1,3);
			break;
		case 4: //use 1 grudge
			itemType = new ItemStack(ModItems.Grudge,1);
			break;
		case 5: //use 1 grudge block
			itemType = new ItemStack(ModBlocks.BlockGrudge,1);
			break;
		case 6: //use 1 grudge block
			itemType = new ItemStack(ModBlocks.BlockGrudgeHeavy,1);
			break;
		case 7:	//use 1 repair bucket
			itemType = new ItemStack(ModItems.BucketRepair,1);
			break;
		case 8:	//use 1 repair goddess
			itemType = new ItemStack(ModItems.RepairGoddess,1);
			break;	
		}
		
		//search item in ship inventory
		int i = findItemInSlot(itemType);
		if(i == -1) {		//item not found
			return false;
		}
		
		//decr item stacksize
		ItemStack getItem = this.ExtProps.slots[i];

		if(getItem.stackSize >= itemNum) {
			getItem.stackSize -= itemNum;
		}
		else {	//not enough item, return false
			return false;
		}
				
		if(getItem.stackSize == 0) {
			getItem = null;
		}
		
		//save back itemstack
		//no need to sync because no GUI opened
		this.ExtProps.slots[i] = getItem;	
		
		return true;	
	}

	//find item in ship inventory
	protected int findItemInSlot(ItemStack parItem) {
		ItemStack slotitem = null;

		//search ship inventory (except equip slots)
		for(int i = ContainerShipInventory.SLOTS_SHIPINV; i < ContainerShipInventory.SLOTS_PLAYERINV; i++) {
			slotitem = this.ExtProps.slots[i];
			if(slotitem != null && 
			    slotitem.getItem().equals(parItem.getItem()) && 
			    slotitem.getItemDamage() == parItem.getItemDamage()) {
				return i;	//found item
			}		
		}	
		
		return -1;	//item not found
	}
	
	@Override
	public boolean canFly() {
		return false;
	}
	
	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}
	
	//true if use mounts
	public boolean canSummonMounts() {
		return false;
	}
	
	public BasicEntityMount summonMountEntity() {
		return null;
	}
	
	@Override
	public Entity getGuardedEntity() {
		return this.guardedEntity;
	}

	@Override
	public void setGuardedEntity(Entity entity) {
		if(entity != null && entity.isEntityAlive()) {
			this.guardedEntity = entity;
			this.setStateMinor(ID.M.GuardID, entity.getEntityId());
		}
		else {
			this.guardedEntity = null;
			this.setStateMinor(ID.M.GuardID, -1);
		}
	}

	@Override
	public int getGuardedPos(int vec) {
		switch(vec) {
		case 0:
			return this.getStateMinor(ID.M.GuardX);
		case 1:
			return this.getStateMinor(ID.M.GuardY);
		case 2:
			return this.getStateMinor(ID.M.GuardZ);
		case 3:
			return this.getStateMinor(ID.M.GuardDim);
		default:
			return this.getStateMinor(ID.M.GuardType);
		}
	}

	@Override
	public void setGuardedPos(int x, int y, int z, int dim, int type) {
		this.setStateMinor(ID.M.GuardX, x);
		this.setStateMinor(ID.M.GuardY, y);
		this.setStateMinor(ID.M.GuardZ, z);
		this.setStateMinor(ID.M.GuardDim, dim);
		this.setStateMinor(ID.M.GuardType, type);
	}
	
	@Override
	public double getMountedYOffset() {
		return this.height;
	}
	
	@Override
	public Entity getHostEntity() {
		if(this.getPlayerUID() > 0) {
			return EntityHelper.getEntityPlayerByUID(this.getPlayerUID(), this.worldObj);
		}
		else {
			return this.getOwner();
		}
	}
	
	@Override
	public int getDamageType() {
		return this.getStateMinor(ID.M.DamageType);
	}
	
//	//set slot 6 as held item 
//	@Override
//	public ItemStack getHeldItem() {
//		if(ExtProps != null && ExtProps.slots != null) {
//			return ExtProps.slots[6];
//		}
//		
//		return super.getHeldItem();
//	}
	
	//update ship id
	protected void updateShipID() {
		//register or update ship id and owner id
		if(!this.isUpdated && ticksExisted % updateTime == 0) {
			LogHelper.info("DEBUG : update ship: initial SID, PID  cd: "+updateTime);
			ServerProxy.updateShipID(this);		//update ship uid
			
			if(this.getPlayerUID() <= 0) {
				ServerProxy.updateShipOwnerID(this);//update owner uid
			}
			
			//update success
			if(getPlayerUID() > 0 && getShipUID() > 0 && 
			   ServerProxy.getShipWorldData(getShipUID()) != null &&
			   ServerProxy.getShipWorldData(getShipUID())[0] > 0) {
				this.sendSyncPacketAllValue();
				this.isUpdated = true;
			}
			
			//prolong update time
			if(updateTime >= 4096) {
				updateTime = 4096;
			}
			else {
				updateTime *= 2;
			}
		}//end update id
	}
	
	//不跟aircraft, mount, rider碰撞
  	protected void collideWithEntity(Entity target) {
  		if(target instanceof BasicEntityAirplane ||
  		   target.equals(this.riddenByEntity) ||
  		   target.equals(this.ridingEntity)) {
  			return;
  		}
  		target.applyEntityCollision(this);
    }
  	
  	//check state final limit
  	protected float[] checkStateFinalLimit(float[] par1) {
  		//max cap
  		for(int i = 0; i < 6; i++) {
  			if(ConfigHandler.limitShipBasic[i] >= 0D && par1[i] > ConfigHandler.limitShipBasic[i]) {
  				par1[i] = (float) ConfigHandler.limitShipBasic[i];
  			}
  		}
		
		if(ConfigHandler.limitShipBasic[ID.ATK] >= 0D && par1[ID.ATK_H] > ConfigHandler.limitShipBasic[ID.ATK]) {
			par1[ID.ATK_H] = (float) ConfigHandler.limitShipBasic[ID.ATK];
		}
		if(ConfigHandler.limitShipBasic[ID.ATK] >= 0D && par1[ID.ATK_AL] > ConfigHandler.limitShipBasic[ID.ATK]) {
			par1[ID.ATK_AL] = (float) ConfigHandler.limitShipBasic[ID.ATK];
		}
		if(ConfigHandler.limitShipBasic[ID.ATK] >= 0D && par1[ID.ATK_AH] > ConfigHandler.limitShipBasic[ID.ATK]) {
			par1[ID.ATK_AH] = (float) ConfigHandler.limitShipBasic[ID.ATK];
		}

		//min cap
		if(par1[ID.HP] < 1F) {
			par1[ID.HP] = 1F;
		}
		if(par1[ID.HIT] < 1F) {
			par1[ID.HIT] = 1F;
		}
		if(par1[ID.SPD] < 0.1F) {
			par1[ID.SPD] = 0.1F;
		}
		if(par1[ID.MOV] < 0F) {
			par1[ID.MOV] = 0F;
		}
		
		return par1;
  	}
  	
	//check equip effect limit
  	protected float[] checkEffectEquipLimit(float[] par1) {
  		//max cap
  		for(int i = 0; i < 7; i++) {
  			if(i != ID.EF_AA && i != ID.EF_ASM && i != ID.EF_DODGE) {
  				if(ConfigHandler.limitShipEffect[i] >= 0D && par1[i] > ConfigHandler.limitShipEffect[i] * 0.01F) {
  	  				par1[i] = (float) ConfigHandler.limitShipEffect[i] * 0.01F;
  	  			}
  			}
  			else {
  				if(ConfigHandler.limitShipEffect[i] >= 0D && par1[i] > ConfigHandler.limitShipEffect[i]) {
  	  				par1[i] = (float) ConfigHandler.limitShipEffect[i];
  	  			}
  			}
  		}
		
		return par1;
  	}

  	//update hp state
  	protected void updateEmotionState() {
  		float hpState = this.getHealth() / this.getMaxHealth();
		
		//check hp state
		if(hpState > 0.75F) {		//normal
			this.setStateEmotion(ID.S.HPState, ID.HPState.NORMAL, false);
		}
		else if(hpState > 0.5F){	//minor damage
			this.setStateEmotion(ID.S.HPState, ID.HPState.MINOR, false);
		}
		else if(hpState > 0.25F){	//moderate damage
			this.setStateEmotion(ID.S.HPState, ID.HPState.MODERATE, false);   			
		}
		else {						//heavy damage
			this.setStateEmotion(ID.S.HPState, ID.HPState.HEAVY, false);
		}
		
		//roll emtion: hungry > T_T > bored > O_O
		if(getStateFlag(ID.F.NoFuel)) {
			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.HUNGRY) {
				this.setStateEmotion(ID.S.Emotion, ID.Emotion.HUNGRY, false);
			}
		}
		else {
			if(hpState < 0.5F) {
    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.T_T) {
    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.T_T, false);
    			}			
    		}
			else {
				if(this.isSitting() && this.getRNG().nextInt(2) == 0) {	//50% for bored
	    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.BORED) {
//	    				LogHelper.info("DEBUG : set emotion BORED");
	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.BORED, false);
	    			}
	    		}
	    		else {	//back to normal face
	    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.NORMAL) {
//	    				LogHelper.info("DEBUG : set emotion NORMAL");
	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.NORMAL, false);
	    			}
	    		}
			}
		}
		
		//sync emotion
		this.sendEmotionSyncPacket();
  	}
  	
	//update hp state
  	protected void updateMountSummon() {
    	if(this.canSummonMounts()) {
    		//summon mount if emotion state >= equip00
  	  		if(getStateEmotion(ID.S.State) >= ID.State.EQUIP00) {
  	  			if(!this.isRiding()) {
  	  				//summon mount entity
  	  	  			BasicEntityMount mount = this.summonMountEntity();
  	  	  			this.worldObj.spawnEntityInWorld(mount);
  	  	  			
  	  	  			//clear rider
  	  	  			if(this.riddenByEntity != null) {
  	  	  				this.riddenByEntity.mountEntity(null);
  	  	  			}
  	  	  			
  	  	  			//set riding entity
	  	  			this.mountEntity(mount);
	  	  			
	  	  			//sync rider
	  	  			mount.sendSyncPacket();
  	  			}
  	  		}
    	}
  	}
  	
  	//update consume item
  	protected void updateConsumeItem() {
  		//set air value
		if(this.getAir() < 300) {
        	setAir(300);
        }
		
		//get ammo if no ammo
		if(!this.hasAmmoLight()) { this.decrAmmoNum(0, 0); }
		if(!this.hasAmmoHeavy()) { this.decrAmmoNum(1, 0); }
		
		//calc move distance
		double distX = posX - ShipPrevX;
		double distY = posY - ShipPrevY;
		double distZ = posZ - ShipPrevZ;
		ShipPrevX = posX;
		ShipPrevY = posY;
		ShipPrevZ = posZ;
		
		//calc total consumption
    	int valueConsume = (int) MathHelper.sqrt_double(distX*distX + distY*distY + distZ*distZ);
    	//moving grudge cost per block
    	valueConsume *= ConfigHandler.consumeGrudgeAction[ID.ShipConsume.Move];
    	//max moving cost = 200
    	if(valueConsume > 200) valueConsume = 200;
    	//add base grudge cost
    	valueConsume += this.getGrudgeConsumption();
    	
    	//eat grudge
    	decrGrudgeNum(valueConsume);
  	}
  	
  	//update formation buffs, SERVER SIDE ONLY
  	protected void updateFormationBuffs() {
  		//check update flag
  		if(!worldObj.isRemote) {
  			ExtendPlayerProps props = EntityHelper.getExtendPlayerProps(this.getPlayerUID());
  			
  			if(props != null) {
  				//check ship is in formation team
  				int[] teamslot = props.checkIsInFormationTeam(getShipUID());
  				
  				//if in team and can apply buff, set formation data
  				if(teamslot[0] >= 0 && teamslot[1] >= 0 && props.getFormatID(teamslot[0]) > 0) {
  					//set formation type and pos
  					setStateMinor(ID.M.FormatType, props.getFormatID(teamslot[0]));
  					
  					//if diamond formation with 5 ships
  					if(this.getStateMinor(ID.M.FormatType) == 3 &&
  					   props.getNumberOfShip(teamslot[0]) == 5) {
  						//diamond with 5 ship: formation position != team position
  						int slotID = props.getFormationPos(teamslot[0], getShipUID());
  						
  						if(slotID >= 0) {
  							setStateMinor(ID.M.FormatPos, slotID);
  						}
  					}
  					//other formation
  					else {
  						setStateMinor(ID.M.FormatPos, teamslot[1]);
  					}
  					
  					//set buff value
  					setEffectFormation(FormationHelper.getFormationBuffValue(getStateMinor(ID.M.FormatType), getStateMinor(ID.M.FormatPos)));
  					setEffectFormationFixed(ID.FormationFixed.MOV, FormationHelper.getFormationMOV(props, teamslot[0]));
  				}
  				//not in team, clear buff
  				else {
  				//set formation type and pos
  					setStateMinor(ID.M.FormatType, 0);
  					setStateMinor(ID.M.FormatPos, -1);
  					
  					//reset buff value
  					setEffectFormation(Values.zeros13);
  					setEffectFormationFixed(ID.FormationFixed.MOV, 0F);
  				}
  			}

  			//set done flag
  			this.setUpdateFlag(ID.FU.FormationBuff, false);
  		}
  	}
  	
//  	/** setDead() must to check entity health <= 0
//  	 *  FIX: probably fix ship dupe bug that killing ship when server lag
//  	 */
//  	@Override
//  	public void setDead() {
//  		if(!this.worldObj.isRemote && this.getHealth() > 0) {
//  	        return;
//  	    }
//  	    super.setDead();
//    }
  	
  	/** change ship outfit by right click cake on ship */
  	abstract public void setShipOutfit(boolean isSneaking); 
  	
  	
}
