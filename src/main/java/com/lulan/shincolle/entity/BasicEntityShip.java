package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
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
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;

/**SHIP DATA <br>
 * Explanation in crafting/ShipCalc.class
 */
public abstract class BasicEntityShip extends EntityTameable implements IShipCannonAttack, IShipGuardian, IShipFloating {

	protected ExtendShipProps ExtProps;			//entity�B�~NBT����
	protected ShipPathNavigate shipNavigator;	//���Ų��ʥ�navigator
	protected ShipMoveHelper shipMoveHelper;
	protected Entity guardedEntity;				//guarding target
	protected Entity atkTarget;					//attack target
	protected Entity rvgTarget;					//revenge target
	protected int revengeTime;					//revenge target time
	
	//for AI calc
	protected double ShipDepth;			//���`, �Ω�������קP�w
	protected double ShipPrevX;			//ship posX 5 sec ago
	protected double ShipPrevY;			//ship posY 5 sec ago
	protected double ShipPrevZ;			//ship posZ 5 sec ago
	/**equip states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateEquip;
	/**final states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateFinal;
	/**minor states: 0:ShipLevel 1:Kills 2:ExpCurrent 3:ExpNext 4:NumAmmoLight 
	 * 5:NumAmmoHeavy 6:NumGrudge 7:NumAirLight 8:NumAirHeavy 9:immunity time 
	 * 10:followMin 11:followMax 12:FleeHP 13:TargetAIType 14:guardX 15:guardY 16:guardZ 17:guardDim
	 * 18:guardID 19:shipType 20:shipClass 21:playerUID 22:shipUID 23:playerEID 24:guardType 
	 * 25:damageType*/
	protected int[] StateMinor;
	/**equip effect: 0:critical 1:doubleHit 2:tripleHit 3:baseMiss 4:atk_AntiAir 5:atk_AntiSS*/
	protected float[] EffectEquip;
	/**EntityState: 0:State 1:Emotion 2:Emotion2 3:HP State 4:State2 5:AttackPhase*/
	protected byte[] StateEmotion;
	/**EntityFlag: 0:canFloatUp 1:isMarried 2:noFuel 3:canMelee 4:canAmmoLight 5:canAmmoHeavy 
	 * 6:canAirLight 7:canAirHeavy 8:headTilt(client only) 9:canRingEffect 10:canDrop 11:canFollow
	 * 12:onSightChase 13:AtkType_Light 14:AtkType_Heavy 15:AtkType_AirLight 16:AtkType_AirHeavy 
	 * 17:HaveRingEffect 18:PVPFirst 19:AntiAir 20:AntiSS 21:PassiveAI */
	protected boolean[] StateFlag;
	/**BonusPoint: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT */
	protected byte[] BonusPoint;
	/**TypeModify: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT */
	protected float[] TypeModify;
	/**ModelPos: posX, posY, posZ, scale (in ship inventory) */
	protected float[] ModelPos;
	
	//for model render
	protected int StartEmotion;			//��1�}�l�ɶ�
	protected int StartEmotion2;		//��2�}�l�ɶ�
	protected float[] rotateAngle;		//�ҫ����ਤ��, �Ω������~render
	
	//for GUI display, no use
	protected String ownerName;
	
	//for initial
	private boolean initAI;
	private boolean isUpdated;	//updated ship id/owner id tag
	private int updateTime = 16;		//update check interval
	
	
	public BasicEntityShip(World world) {
		super(world);
		ignoreFrustumCheck = true;	//�Y�Ϥ��b���u���@��render
		//init value
		isImmuneToFire = true;	//set ship immune to lava
		StateEquip = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
		StateFinal = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
		StateMinor = new int[] {1, 0, 0, 40, 0,
				                0, 0, 0, 0, 0,
				                3, 12, 35, 1, -1,
				                -1, -1, 0, -1, 0,
				                0, -1, -1, -1, 0,
				                0
				                };
		EffectEquip = new float[] {0F, 0F, 0F, 0F, 0F, 0F};
		StateEmotion = new byte[] {0, 0, 0, 0, 0, 0};
		StateFlag = new boolean[] {false, false, true, false, true,
				                   true, true, true, false, true,
								   true, false, true, true, true,
								   true, true, false, true, false,
								   false, false
								};
		BonusPoint = new byte[] {0, 0, 0, 0, 0, 0};
		TypeModify = new float[] {1F, 1F, 1F, 1F, 1F, 1F};
		ModelPos = new float[] {0F, 0F, 0F, 50F};
		
		//for AI
		ShipDepth = 0D;			//water block above ship (within ship position)
		ShipPrevX = posX;		//ship position 5 sec ago
		ShipPrevY = posY;
		ShipPrevZ = posZ;
		ownerName = "";
		stepHeight = 1F;
		shipNavigator = new ShipPathNavigate(this, worldObj);
		shipMoveHelper = new ShipMoveHelper(this);
		
		//for render
		StartEmotion = -1;		//emotion start time
		StartEmotion2 = -1;		//head tile cooldown
		rotateAngle = new float[] {0F, 0F, 0F};		//model rotate angle (right hand)
		
		//for init
		initAI = false;
		isUpdated = false;
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
	
	//���`����
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
	
	//���˭���
    @Override
	protected String getHurtSound() {
    	
        return Reference.MOD_ID+":ship-hurt";
    }

    //���`����
    @Override
	protected String getDeathSound() {
    	return Reference.MOD_ID+":ship-death";
    }

    //���Ĥj�p
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
		//moving
		this.tasks.addTask(21, new EntityAIOpenDoor(this, true));	//0000
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
		LogHelper.info("DEBUG : ship target AI list: "+this.targetTasks.taskEntries.size());
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
	
	/** 0=small 1=large */
	abstract public int getKaitaiType();	//0 = small, 1 = large
	
	@Override
	public int getLevel() {
		return StateMinor[ID.M.ShipLevel];
	}
	
	public byte getShipType() {
		return (byte)getStateMinor(ID.M.ShipType);
	}
	
	//ShipID = ship CLASS ID
	public byte getShipClass() {
		return (byte)getStateMinor(ID.M.ShipClass);
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
	public int getStartEmotion() {
		return StartEmotion;
	}
	
	@Override
	public int getStartEmotion2() {
		return StartEmotion2;
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
		return StateMinor[ID.M.NumAmmoLight] > 0;
	}
	
	@Override
	public boolean hasAmmoHeavy() {
		return StateMinor[ID.M.NumAmmoHeavy] > 0;
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
	@Override
	public int getStateMinor(int id) {
		return StateMinor[id];
	}
	@Override
	public float getEffectEquip(int id) {
		return EffectEquip[id];
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
	
	/**calc equip and all attrs
	 * step:
	 * 1. reset all attrs to 0
	 * 2. calc 6 equip slots
	 * 3. calc special attrs (if @Override calcShipAttributes())
	 * 4. calc HP,DEF...etc
	 * 5. send sync packet
	 */
	public void calcEquipAndUpdateState() {
		ItemStack itemstack = null;
		float[] equipStat = {0F,0F,0F,0F,0F,0F,0F,0F,0F,0F};
		
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
		
		//calc equip slots
		for(int i=0; i<ContainerShipInventory.SLOTS_SHIPINV; i++) {
			equipStat = EquipCalc.getEquipStat(this, this.ExtProps.slots[i]);
			
			if(equipStat != null) {
//				LogHelper.info("DEBUG : equip stat "+equipStat[0]+" "+equipStat[1]+" "+equipStat[2]+" "+equipStat[3]+" "+equipStat[4]+" "+equipStat[5]+" "+equipStat[6]+" "+equipStat[7]+" "+equipStat[8]);
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
			}
		}
		//update value
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
		
		StateFinal[ID.ATK] = (atk + StateEquip[ID.ATK]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_H] = (atk * 3F + StateEquip[ID.ATK_H]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_AL] = (atk + StateEquip[ID.ATK_AL]) * (float)ConfigHandler.scaleShip[ID.ATK];
		StateFinal[ID.ATK_AH] = (atk * 3F + StateEquip[ID.ATK_AH]) * (float)ConfigHandler.scaleShip[ID.ATK];
		
		//KB Resistance
		float resisKB = ((StateMinor[ID.M.ShipLevel])/10F) * 0.05F;

		//max cap
		for(int i = 0; i < ConfigHandler.limitShip.length; i++) {
			if(ConfigHandler.limitShip[i] >= 0D && StateFinal[i] > ConfigHandler.limitShip[i]) {
				StateFinal[i] = (float) ConfigHandler.limitShip[i];
			}
		}
		
		if(ConfigHandler.limitShip[ID.ATK] >= 0D && StateFinal[ID.ATK_H] > ConfigHandler.limitShip[ID.ATK]) {
			StateFinal[ID.ATK_H] = (float) ConfigHandler.limitShip[ID.ATK];
		}
		if(ConfigHandler.limitShip[ID.ATK] >= 0D && StateFinal[ID.ATK_AL] > ConfigHandler.limitShip[ID.ATK]) {
			StateFinal[ID.ATK_AL] = (float) ConfigHandler.limitShip[ID.ATK];
		}
		if(ConfigHandler.limitShip[ID.ATK] >= 0D && StateFinal[ID.ATK_AH] > ConfigHandler.limitShip[ID.ATK]) {
			StateFinal[ID.ATK_AH] = (float) ConfigHandler.limitShip[ID.ATK];
		}

		//min cap
		if(StateFinal[ID.HP] < 1F) {
			StateFinal[ID.HP] = 1F;
		}
		if(StateFinal[ID.HIT] < 1F) {
			StateFinal[ID.HIT] = 1F;
		}
		if(StateFinal[ID.SPD] < 0F) {
			StateFinal[ID.SPD] = 0F;
		}
		if(StateFinal[ID.MOV] < 0F) {
			StateFinal[ID.MOV] = 0F;
		}
		
		//set attribute by final value
		/**
		 * DO NOT SET ATTACK DAMAGE to non-EntityMob!!!!!
		 */
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(StateFinal[ID.HP]);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(StateFinal[ID.MOV]);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(StateFinal[ID.HIT]+16); //������ؼ�, ���|���d��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(resisKB);
//		this.jumpMovementFactor = (1F + StateFinal[ID.MOV]) * 0.05F;
		
		//for server side
		if(!worldObj.isRemote) {
			sendSyncPacket();		//sync nbt data
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
				StateMinor[ID.M.ExpNext] = (StateMinor[ID.M.ShipLevel] + 1) * ConfigHandler.expMod + ConfigHandler.expMod;
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
	
	//ship attribute setter, sync packet in method: calcShipAttributes 
	public void setStateFinal(int state, float par1) {
		StateFinal[state] = par1;
	}
	
	@Override
	public void setStateMinor(int state, int par1) {
		StateMinor[state] = par1;
	}
	
	//called when GUI update
	public void setEffectEquip(int state, float par1) {
		EffectEquip[state] = par1;
	}
	
	public void setBonusPoint(int state, byte par1) {
		BonusPoint[state] = par1;
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
		
		//�Y�ק�melee flag, �hreload AI
		if(!this.worldObj.isRemote) { 
			if(id == ID.F.UseMelee) {
				clearAITasks();
	    		setAIList();
	    		
	    		//�]�wmount��AI
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
	public void setStartEmotion(int par1) {
		StartEmotion = par1;
	}
	
	@Override
	public void setStartEmotion2(int par1) {
		StartEmotion2 = par1;
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
	
	/** send sync packet: sync all data */
	public void sendSyncPacket() {
		this.sendSyncPacket(0, false);
	}
	
	/**  sync data for GUI display */
	public void sendGUISyncPacket() {
		if(!this.worldObj.isRemote) {
			if(this.getPlayerUID() > 0) {
				EntityPlayerMP player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID(), this.worldObj);
				//owner�b����~�ݭnsync
				if(player != null && this.getDistanceToEntity(player) < 32F) {
					CommonProxy.channelG.sendTo(new S2CGUIPackets(this), player);
				}
			}
		}
	}
	
	/** sync data for emotion display */
	public void sendEmotionSyncPacket() {
		this.sendSyncPacket(1, true);
	}
	
	/** send sync packet:
	 *  type: 0: all  1: emotion  2: flag  3: minor
	 *  send all: send packet to all player
	 */
	public void sendSyncPacket(int type, boolean sendAll) {
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
				
				//owner�b����~�ݭnsync
				if(player != null && this.getDistanceToEntity(player) < 65F) {
					CommonProxy.channelE.sendTo(new S2CEntitySync(this, type), player);
				}
				
				//send ship state for display
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
	            //�Y��name tag�����W�L, �h�N�W�r�K��entity�W
				if(itemstack.hasDisplayName()) {
					this.setNameTag(itemstack.getDisplayName());
					return false;
	            } 
			}
			
			//use repair bucket
			if(itemstack.getItem() == ModItems.BucketRepair) {
				//hp����max hp�ɥi�H�ϥ�bucket
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
					//�гy�Ҧ������Ӫ��~
	                if(!player.capabilities.isCreativeMode) {  //damage +1 in non-creative mode
	 	                itemstack.setItemDamage(itemstack.getItemDamage() + 1);
	                    
	                    //set item amount
	                    ItemStack item0, item1, item2, item3;
	                                
	                    //large ship -> more materials
	                    if(this.getKaitaiType() == 1) {
	                    	if(ConfigHandler.easyMode) {	//easy mode
	                    		item0 = new ItemStack(ModBlocks.BlockGrudge, 1);
	                        	item1 = new ItemStack(ModBlocks.BlockAbyssium, 1);
	                        	item2 = new ItemStack(ModItems.Ammo, 1, 1);
	                        	item3 = new ItemStack(ModBlocks.BlockPolymetal, 1);
	                    	}
	                    	else {						
	                    		item0 = new ItemStack(ModBlocks.BlockGrudge, 8 + rand.nextInt(3));
	                        	item1 = new ItemStack(ModBlocks.BlockAbyssium, 8 + rand.nextInt(3));
	                        	item2 = new ItemStack(ModItems.Ammo, 8 + rand.nextInt(3), 1);
	                        	item3 = new ItemStack(ModBlocks.BlockPolymetal, 8 + rand.nextInt(3));
	                    	}
	                        
	                    }
	                    //small ship
	                    else {
	                        item0 = new ItemStack(ModItems.Grudge, 10 + rand.nextInt(8));
	                    	item1 = new ItemStack(ModItems.AbyssMetal, 10 + rand.nextInt(8), 0);
	                    	item2 = new ItemStack(ModItems.Ammo, 10 + rand.nextInt(8), 0);
	                    	item3 = new ItemStack(ModItems.AbyssMetal, 10 + rand.nextInt(8), 1);
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
	            					//�]�w�n�H���Q�X��range
	            					float f = rand.nextFloat() * 0.8F + 0.1F;
	            					float f1 = rand.nextFloat() * 0.8F + 0.1F;
	            					float f2 = rand.nextFloat() * 0.8F + 0.1F;

	            					while(invitem.stackSize > 0) {
	            						int j = rand.nextInt(21) + 10;
	            						//�p�G���~�W�L�@���H���ƶq, �|����h�|�Q�X
	            						if(j > invitem.stackSize) {  
	            							j = invitem.stackSize;
	            						}

	            						invitem.stackSize -= j;
	            						//�Nitem����entity, �ͦ���@�ɤW
	            						EntityItem item = new EntityItem(this.worldObj, this.posX+f, this.posY+f1, this.posZ+f2, new ItemStack(invitem.getItem(), j, invitem.getItemDamage()));
	            						//�p�G��NBT tag, �]�n�ƻs�쪫�~�W
	            						if(invitem.hasTagCompound()) {
	            							item.getEntityItem().setTagCompound((NBTTagCompound)invitem.getTagCompound().copy());
	            						}
	            						
	            						worldObj.spawnEntityInWorld(item);	//�ͦ�item entity
	            					}
	            				}
	            			}
	                    }//end drop inventory item
	                    
	                    playSound(Reference.MOD_ID+":ship-kaitai", ConfigHandler.shipVolume, 1.0F);
	                    playSound(Reference.MOD_ID+":ship-death", ConfigHandler.shipVolume, 1.0F);
	                }
	                
	                //���~�Χ��ɭn�]�w��null�M�Ÿ�slot
	                if(itemstack.getItemDamage() >= itemstack.getMaxDamage()) {  //���~�@�[�ץΧ��ɭn�]�w��null�M�Ÿ�slot
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
    	        
                if(itemstack.stackSize <= 0) {  //���~�Χ��ɭn�]�w��null�M�Ÿ�slot
                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
                }
                
                return true;
			}//end wedding ring
			
			//use modernization kit
			if(itemstack.getItem() == ModItems.ModernKit) {
				if(addRandomBonusPoint()) {	//add 1 random bonus
					if(!player.capabilities.isCreativeMode) {
	                    --itemstack.stackSize;
	                    
	                    if(itemstack.stackSize <= 0) {  //���~�Χ��ɭn�]�w��null�M�Ÿ�slot
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
								this.sendSyncPacket(3, true);
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
		
		//�p�G�w�g�Q�i�j, �A�I�@�U�i�H�Ѱ��i�j
		if(this.getLeashed() && this.getLeashedToEntity() == player) {
            this.clearLeashed(true, !player.capabilities.isCreativeMode);
            return true;
        }
	
		//shift+right click�ɥ��}GUI
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

	/**�קﲾ�ʤ�k, �Ϩ�water��lava�����ʮɹ��Oflying entity
     * Moves the entity based on the specified heading.  Args: strafe, forward
     */
	@Override
    public void moveEntityWithHeading(float movX, float movZ) {
        double d0;

        if(EntityHelper.checkEntityIsInLiquid(this)) { //�P�w���G�餤��, ���|�۰ʤU�I
            d0 = this.posY;
            this.moveFlying(movX, movZ, this.getStateFinal(ID.MOV)*0.4F); //�������t�׭p��(�t�}���ĪG)
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            //�������O
            this.motionX *= 0.8D;
            this.motionY *= 0.8D;
            this.motionZ *= 0.8D;
            //��������F��|�W��
            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6D - this.posY + d0, this.motionZ)) {
                this.motionY = 0.3D;
            }
        }
        else {									//��L���ʪ��A
            float f2 = 0.91F;
            
            if(this.onGround) {					//�b�a������
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            float f3 = 0.16277136F / (f2 * f2 * f2);
            float f4;
            
            if(this.onGround) {
                f4 = this.getAIMoveSpeed() * f3;
            }
            else {								//���D��
                f4 = this.jumpMovementFactor;
            }
            this.moveFlying(movX, movZ, f4);
            f2 = 0.91F;
            
            if(this.onGround) {
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            if(this.isOnLadder()) {				//���ӱ褤
                float f5 = 0.15F;
                //����ӱ�ɪ���V���ʳt��
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
                //����ӱ誺���U�t��
                if (this.motionY < -0.15D) {
                    this.motionY = -0.15D;
                }

                boolean flag = this.isSneaking();
                //�Y�O���ӱ�ɬ�sneaking, �h���|���U(�d�b�ӱ�W)
                if(flag && this.motionY < 0D) {
                    this.motionY = 0D;
                }
            }

            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            //���ӱ����, �h�|���W��
            if(this.isCollidedHorizontally && this.isOnLadder()) {
                this.motionY = 0.4D;
            }
            //�۵M����
            if(this.worldObj.isRemote && (!this.worldObj.blockExists((int)this.posX, 0, (int)this.posZ) || !this.worldObj.getChunkFromBlockCoords((int)this.posX, (int)this.posZ).isChunkLoaded)) {
                if (this.posY > 0.0D) {
                    this.motionY = -0.1D;	//�Ů𤤪�gravity��0.1D
                }
                else {
                    this.motionY = 0.0D;
                }
            }
            else {
                this.motionY -= 0.08D;
            }
            //�Ů𤤪��T��V���O
            this.motionY *= 0.98D;			
            this.motionX *= f2;
            this.motionZ *= f2;
//            LogHelper.info("DEBUG : f2 "+f2+" ");
        }
        //�p��|���\�ʭ�
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
	 *  �b����onUpdate��onLivingUpdate�Ϥ�server��client update
	 *  for shincolle:
	 *  onUpdate = client update only
	 *  onLivingUpdate = server update only
	 */
	@Override
	public void onUpdate() {
		super.onUpdate();
		EntityHelper.checkDepth(this);	//both side
		
		//client side
		if(this.worldObj.isRemote) {
			//�����ʮ�, ���ͤ���S��
			if(this.getShipDepth() > 0D) {
				//(�`�N��entity�]���]���D���t��s, client�ݤ��|��smotionX���ƭ�, �ݦۦ�p��)
				double motX = this.posX - this.prevPosX;
				double motZ = this.posZ - this.prevPosZ;
				double parH = this.posY - (int)this.posY;
				
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
					
					//display pointer target effect, �u��owner�~�|�������ship�P�B��EID, �DownerŪ���쪺EID <= 0
					EntityPlayer player = null;
					if(this.getStateMinor(ID.M.PlayerEID) > 0) {
						player = EntityHelper.getEntityPlayerByID(getStateMinor(ID.M.PlayerEID), 0, true);
					
						if(player != null) {
							ItemStack item = player.inventory.getCurrentItem();
							
							if(ConfigHandler.alwaysShowTeam || (item != null && item.getItem() instanceof PointerItem)) {
								//�аO�bentity�W
								if(this.getGuardedEntity() != null) {
									ParticleHelper.spawnAttackParticleAtEntity(this.getGuardedEntity(), 0.3D, 6D, 0D, (byte)2);
								}
								//�аO�bblock�W
								else if(this.getGuardedPos(1) >= 0) {
									ParticleHelper.spawnAttackParticleAt(this.getGuardedPos(0)+0.5D, this.getGuardedPos(1), this.getGuardedPos(2)+0.5D, 0.3D, 6D, 0D, (byte)25);
								}
							}
						}
						else {
							this.setStateMinor(ID.M.PlayerEID, -1);
						}
					}
					
				}//end show pointer target effect
			}//end every 32 ticks
		}//end client side
	}

	/** update living entity 
	 *  �b����onUpdate��onLivingUpdate�Ϥ�server��client update
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
        	EntityHelper.updateTarget(this);
//        	LogHelper.info("DEBUG: target:   "+this.getClass().getSimpleName()+" "+this.getEntityTarget()+"      "+this.getEntityRevengeTarget()+"      "+this.getAttackTarget());
        	
        	//update/init id
        	this.updateShipID();
        	
        	//decr immune time
        	if(this.StateMinor[ID.M.ImmuneTime] > 0) {
        		this.StateMinor[ID.M.ImmuneTime]--;
        	}
        	
        	//check every 8 ticks
        	if(ticksExisted % 8 == 0) {
        		//reset AI and sync once
        		if(!this.initAI && ticksExisted > 10) {
        			this.setStateFlag(ID.F.CanDrop, true);
            		clearAITasks();
            		clearAITargetTasks();	//reset AI for get owner after loading NBT data
            		setAIList();
            		setAITargetList();
            		decrGrudgeNum(0);		//check grudge
            		sendSyncPacket();		//sync packet to client
            		
            		this.initAI = true;
        		}
        		
        		//use bucket automatically
        		if((getMaxHealth() - getHealth()) > (getMaxHealth() * 0.1F + 5F)) {
	                if(decrSupplies(7)) {
	                	if(this instanceof BasicEntityShipSmall) {
		                	this.heal(this.getMaxHealth() * 0.1F + 5F);	//1 bucket = 10% hp for small ship
		                }
		                else {
		                	this.heal(this.getMaxHealth() * 0.05F + 10F);	//1 bucket = 5% hp for large ship
		                }
	                }
	            }
//        		LogHelper.info("DEBUG : check spawn: "+this.worldObj.getChunkProvider().getPossibleCreatures(EnumCreatureType.waterCreature, (int)posX, (int)posY, (int)posZ));
        	}//end every 8 ticks
        	
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
//        		LogHelper.info("DEBUG: target:   "+this.getClass().getSimpleName()+" "+this.getEntityTarget()+"      "+this.getEntityRevengeTarget()+"      "+this.getAttackTarget());
//            	LogHelper.info("DEBUG : ship update: "+ServerProxy.getTeamData(900).getTeamBannedList());
//            	LogHelper.info("DEBUG : ship update: eid: "+ServerProxy.getNextShipID()+" "+ServerProxy.getNextPlayerID()+" "+ConfigHandler.nextPlayerID+" "+ConfigHandler.nextShipID);
//        		if(this.worldObj.provider.dimensionId == 0) {	//main world
//        			LogHelper.info("DEBUG : ship pos dim "+ClientProxy.getClientWorld().provider.dimensionId+" "+this.dimension+" "+this.posX+" "+this.posY+" "+this.posZ);
//        		}
//        		else {	//other world
//        			LogHelper.info("DEBUG : ship pos dim "+ClientProxy.getClientWorld().provider.dimensionId+" "+this.dimension+" "+this.posX+" "+this.posY+" "+this.posZ);
//        		}
        		
        	}//end every 16 ticks
        	
        	//check every 64 ticks
        	if(ticksExisted % 64 == 0) {
        		if(this.getPlayerUID() > 0) {
        			//get owner
        			EntityPlayer player = EntityHelper.getEntityPlayerByUID(this.getPlayerUID(), this.worldObj);
        					
        			//owner exists (online and same world)
        			if(player != null) {
    					//update owner entity id (could be changed when owner change dimension or dead)
            			this.setStateMinor(ID.M.PlayerEID, player.getEntityId());
            			//sync guard
            			this.sendSyncPacket(3, false);
            		}
        		}
	        }//end every 64 ticks
        	
        	//check every 128 ticks
        	if(ticksExisted % 128 == 0) {
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
//        				LogHelper.info("DEBUG : set emotion HUNGRY");
	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.HUNGRY, false);
	    			}
        		}
        		else {
        			if(hpState < 0.5F) {
    	    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.T_T) {
//    	    				LogHelper.info("DEBUG : set emotion T_T");
    	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.T_T, false);
    	    			}			
    	    		}
        			else {
        				if(this.isSitting() && this.getRNG().nextInt(2) == 0) {	//50% for bored
        	    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.BORED) {
//        	    				LogHelper.info("DEBUG : set emotion BORED");
        	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.BORED, false);
        	    			}
        	    		}
        	    		else {	//back to normal face
        	    			if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.NORMAL) {
//        	    				LogHelper.info("DEBUG : set emotion NORMAL");
        	    				this.setStateEmotion(ID.S.Emotion, ID.Emotion.NORMAL, false);
        	    			}
        	    		}
        			}
        		}
        		
        		//sync emotion
        		this.sendEmotionSyncPacket();

        		//set air value
        		if(this.getAir() < 300) {
                	setAir(300);
                }
        		
        		//get ammo if no ammo
        		if(!this.hasAmmoLight()) { this.decrAmmoNum(2); }
        		if(!this.hasAmmoHeavy()) { this.decrAmmoNum(3); }
        		
        		//calc move distance and eat grudge (check position 5 sec ago)
        		double distX = posX - ShipPrevX;
        		double distY = posY - ShipPrevY;
        		double distZ = posZ - ShipPrevZ;
        		ShipPrevX = posX;
        		ShipPrevY = posY;
        		ShipPrevZ = posZ;
	        	int distSqrt = (int) MathHelper.sqrt_double(distX*distX + distY*distY + distZ*distZ);
	        	decrGrudgeNum(distSqrt+5);	//eat grudge or change movement speed
        		
	        	//summon mounts
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
        	}//end every 128 ticks
        	
        	//auto recovery every 512 ticks
        	if(this.ticksExisted % 512 == 0) {
        		if(this.getHealth() < this.getMaxHealth()) {
        			this.setHealth(this.getHealth() + this.getMaxHealth() * 0.0128F);
        		}
        	}//end every 512 ticks
        	
        	//play timekeeping sound
        	if(ConfigHandler.timeKeeping) {
        		long time = this.worldObj.provider.getWorldTime();
            	int checkTime = (int)(time % 1000L);
            	if(checkTime == 0) {
            		playTimeSound((int)(time / 1000L) % 24);
            	}
        	}//end timekeeping
        	
        }//end if(server side)
        //client side
//        else {
//        	LogHelper.info("DEBUG : emotion "+this.getStateEmotion(ID.S.Emotion)+" "+getStartEmotion());
//        	//set client side owner
//        	if(this.ticksExisted % 20 == 0) {
//        		LogHelper.info("DEBUG : entity sync 3: get owner id "+this.getStateMinor(ID.M.OwnerID));
//        	}
//        }
        
//        //both side
//        if(this.ridingEntity instanceof BasicEntityMount) {
//    		((BasicEntityMount)this.ridingEntity).prevRotationYaw = this.prevRotationYaw;
//    		((BasicEntityMount)this.ridingEntity).rotationYaw = this.rotationYaw;
//    		((BasicEntityMount)this.ridingEntity).renderYawOffset = this.renderYawOffset;
//		}
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
		//set knockback value (testing)
		float kbValue = 0.15F;
		
		//experience++
		addShipExp(1);
		
		//grudge--
		decrGrudgeNum(1);
				
	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);

	    //play entity attack sound
        if(this.getRNG().nextInt(10) > 6) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
	    
	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue, 
	                   0.1D, MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue);
	            motionX *= 0.6D;
	            motionZ *= 0.6D;
	        }

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
		float atk = StateFinal[ID.ATK];
		
		//set knockback value (testing)
		float kbValue = 0.05F;
        
        //experience++
  		addShipExp(2);
  		
  		//grudge--
  		decrGrudgeNum(1);
        
  		//calc equip special dmg: AA, ASM
  		atk = CalcHelper.calcDamageByEquipEffect(this, target, atk, 0);
  		
        //light ammo -1
        if(!decrAmmoNum(0)) {		//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
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
        
        //�o�g�̷����S��
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
      		
	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);

	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue, 
	                   0.1D, MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue);
	            motionX *= 0.6D;
	            motionZ *= 0.6D;
	        }
	        
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
		
		//���u�O�_�ĥΪ��g
		boolean isDirect = false;
		//�p��ؼжZ��
		float tarX = (float)target.posX;	//for miss chance calc
		float tarY = (float)target.posY;
		float tarZ = (float)target.posZ;
		float distX = tarX - (float)this.posX;
		float distY = tarY - (float)this.posY;
		float distZ = tarZ - (float)this.posZ;
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        float launchPos = (float)posY + height * 0.7F;
        
        //�W�L�@�w�Z��/���� , �h�ĥΩߪ��u,  �b�����ɵo�g���׸��C
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
		decrGrudgeNum(1);
	
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //heavy ammo -1
        if(!decrAmmoNum(1)) {	//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
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
	
	//be attacked method, �]�A��Lentity����, anvil����, arrow����, fall damage���ϥΦ���k 
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
		//set hurt face
    	if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.O_O) {
    		this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
    	}
    	
    	//�Y�����謰owner, �h�����^�Ƕˮ`, ���pdef��friendly fire
		if(attacker.getEntity() instanceof EntityPlayer &&
		   EntityHelper.checkSameOwner(attacker.getEntity(), this)) {
			this.setSitting(false);
			return super.attackEntityFrom(attacker, atk);
		}
        
        //�Y����@�ɥ~, �h�ǰe�^y=4
        if(attacker.getDamageType().equals("outOfWorld")) {
        	//�������U�ʧ@
			this.setSitting(false);
			this.mountEntity(null);
        	this.setPositionAndUpdate(this.posX, 4D, this.posZ);
        	this.motionX = 0D;
        	this.motionY = 0D;
        	this.motionZ = 0D;
        	return false;
        }
        
        //�L�Ī�entity�ˮ`�L��
		if(this.isEntityInvulnerable()) {
            return false;
        }
		else if(attacker.getEntity() != null) {	//����null�~��ˮ`, �i�K�̬r/����/�������ˮ`
			Entity entity = attacker.getEntity();
			
			//���|��ۤv�y���ˮ`, �i�K�̬r/����/�������ˮ` (�����ۤv��ۤv�y���ˮ`)
			if(entity.equals(this)) {
				//�������U�ʧ@
				this.setSitting(false);
				return false;
			}
			
			//�Y�����謰player, �h�ˬdfriendly fire
			if(entity instanceof EntityPlayer) {
				//�Y�T��friendlyFire, �h���y���ˮ`
				if(!ConfigHandler.friendlyFire) {
					return false;
				}
			}
			
			//�i��def�p��
			float reduceAtk = atk * (1F - (StateFinal[ID.DEF] - rand.nextInt(20) + 10F) / 100F);    
			
			//ship vs ship, config�ˮ`�վ�
			if(entity instanceof BasicEntityShip || entity instanceof BasicEntityAirplane || 
			   entity instanceof EntityRensouhou || entity instanceof BasicEntityMount) {
				reduceAtk = reduceAtk * (float)ConfigHandler.dmgSvS * 0.01F;
			}
			
			//ship vs ship, damage type�ˮ`�վ�
			if(entity instanceof IShipAttackBase) {
				//get attack time for damage modifier setting (day, night or ...etc)
				int modSet = this.worldObj.provider.isDaytime() ? 0 : 1;
				reduceAtk = CalcHelper.calcDamageByType(reduceAtk, ((IShipAttackBase) entity).getDamageType(), this.getDamageType(), modSet);
			}
			
			//min damage�]��1
	        if(reduceAtk < 1) reduceAtk = 1;

			//�������U�ʧ@
			this.setSitting(false);
			
			//�]�mrevenge target
			this.setEntityRevengeTarget(entity);
			this.setEntityRevengeTime();
//			LogHelper.info("DEBUG : set revenge target: "+entity+"  host: "+this);
			
			//�Y�ˮ`�O�i��P��, �h�M�䪫�~�����Lrepair goddess�Ө�����������
			if(reduceAtk >= (this.getHealth() - 1F)) {
				if(this.decrSupplies(8)) {
					this.setHealth(this.getMaxHealth());
					this.StateMinor[ID.M.ImmuneTime] = 60;
					return false;
				}
			}
   
            //�����class���Q�����P�w, �]�A���mlove�ɶ�, �p����r�ܩ�, �p���K�z/�����ˮ`, 
            //hurtResistantTime(0.5sec�L�Įɶ�)�p��, 
            return super.attackEntityFrom(attacker, reduceAtk);
        }
		
		return false;
    }
	
	//decrese ammo number with type, or find ammo item from inventory
	protected boolean decrAmmoNum(int type) {
		switch(type) {
		case 0:  //use 1 light ammo
			if(hasAmmoLight()) { 
				--StateMinor[ID.M.NumAmmoLight];
				return true;
			}
			else {
				if(decrSupplies(0)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumAmmoLight] += 299;
					}
					else {
						StateMinor[ID.M.NumAmmoLight] += 29;
					}
					return true;
				}
				else if(decrSupplies(2)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumAmmoLight] += 2699;
					}
					else {
						StateMinor[ID.M.NumAmmoLight] += 269;
					}
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 1:  //use 1 heavy ammo
			if(hasAmmoHeavy()) { 
				--StateMinor[ID.M.NumAmmoHeavy];
				return true;
			}
			else {
				if(decrSupplies(1)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumAmmoHeavy] += 149;
					}
					else {
						StateMinor[ID.M.NumAmmoHeavy] += 14;
					}
					return true;
				}
				else if(decrSupplies(3)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumAmmoHeavy] += 1349;
					}
					else {
						StateMinor[ID.M.NumAmmoHeavy] += 134;
					}
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 2:	//no ammo light, use item
			if(decrSupplies(0)) {  //find ammo item
				if(ConfigHandler.easyMode) {
					StateMinor[ID.M.NumAmmoLight] += 300;
				}
				else {
					StateMinor[ID.M.NumAmmoLight] += 30;
				}
				return true;
			}
			else if(decrSupplies(2)) {  //find ammo item
				if(ConfigHandler.easyMode) {
					StateMinor[ID.M.NumAmmoLight] += 2700;
				}
				else {
					StateMinor[ID.M.NumAmmoLight] += 270;
				}
				return true;
			}
			else {				   //no ammo
				return false;
			}
		case 3:	//no ammo heavy, use item
			if(decrSupplies(1)) {  //find ammo item
				if(ConfigHandler.easyMode) {
					StateMinor[ID.M.NumAmmoHeavy] += 150;
				}
				else {
					StateMinor[ID.M.NumAmmoHeavy] += 15;
				}
				return true;
			}
			else if(decrSupplies(3)) {  //find ammo item
				if(ConfigHandler.easyMode) {
					StateMinor[ID.M.NumAmmoHeavy] += 1350;
				}
				else {
					StateMinor[ID.M.NumAmmoHeavy] += 135;
				}
				return true;
			}
			else {				   //no ammo
				return false;
			}
		case 4:  //use 4 light ammo
			if(StateMinor[ID.M.NumAmmoLight] > 3) {
				StateMinor[ID.M.NumAmmoLight] -= 4;
				return true;
			}
			else {
				if(decrSupplies(0)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumAmmoLight] += 296;
					}
					else {
						StateMinor[ID.M.NumAmmoLight] += 26;
					}
					return true;
				}
				else if(decrSupplies(2)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumAmmoLight] += 2696;
					}
					else {
						StateMinor[ID.M.NumAmmoLight] += 266;
					}
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 5:  //use 2 heavy ammo
			if(StateMinor[ID.M.NumAmmoHeavy] > 1) { 
				StateMinor[ID.M.NumAmmoHeavy] -= 2;
				return true;
			}
			else {
				if(decrSupplies(1)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumAmmoHeavy] += 148;
					}
					else {
						StateMinor[ID.M.NumAmmoHeavy] += 13;
					}
					return true;
				}
				else if(decrSupplies(3)) {  //find ammo item
					if(ConfigHandler.easyMode) {
						StateMinor[ID.M.NumAmmoHeavy] += 1348;
					}
					else {
						StateMinor[ID.M.NumAmmoHeavy] += 133;
					}
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		}
		
		return false;	//unknow attack type
	}
	
	//eat grudge and change movement speed
	protected void decrGrudgeNum(int par1) {
//		LogHelper.info("DEBUG : check grudge num");
		
		if(par1 > 215) {	//max cost = 215 (calc from speed 1 moving 5 sec)
			par1 = 215;
		}
		
		if(StateMinor[ID.M.NumGrudge] >= par1) { //has enough fuel
			StateMinor[ID.M.NumGrudge] -= par1;
		}
		else {
			if(decrSupplies(4)) {		//find grudge
				if(ConfigHandler.easyMode) {
					StateMinor[ID.M.NumGrudge] += 3600;
				}
				else {
					StateMinor[ID.M.NumGrudge] += 1200;
				}
				StateMinor[ID.M.NumGrudge] -= par1;
			}
			else if(decrSupplies(5)) {	//find grudge block
				if(ConfigHandler.easyMode) {
					StateMinor[ID.M.NumGrudge] += 32400;
				}
				else {
					StateMinor[ID.M.NumGrudge] += 10800;
				}
				StateMinor[ID.M.NumGrudge] -= par1;
			}
//�קK�Y���t���x�s��T�����, �]�����Φ�����@��grudge�ɥR�D��
//			else if(decrSupplies(6)) {	//find grudge heavy block
//				NumGrudge += 97200;
//				NumGrudge -= (int)par1;
//			}
		}
		
		if(StateMinor[ID.M.NumGrudge] <= 0) {
			setStateFlag(ID.F.NoFuel, true);
		}
		else {
			setStateFlag(ID.F.NoFuel, false);
		}
		
		//no fuel, clear AI
		if(getStateFlag(ID.F.NoFuel)) {
			//�쥻��AI, �h�M����
			if(this.targetTasks.taskEntries.size() > 0) {
				LogHelper.info("DEBUG : No fuel, clear AI "+this);
				clearAITasks();
				clearAITargetTasks();
				sendSyncPacket();
				
				//�]�wmount��AI
				if(this.ridingEntity instanceof BasicEntityMount) {
					((BasicEntityMount) this.ridingEntity).clearAITasks();
				}
			}	
		}
		//has fuel, set AI
		else {
			if(this.targetTasks.taskEntries.size() < 2) {
				LogHelper.info("DEBUG : Get fuel, set AI "+this);
				clearAITasks();
				clearAITargetTasks();
				setAIList();
				setAITargetList();
				sendSyncPacket();
				
				//�]�wmount��AI
				if(this.ridingEntity instanceof BasicEntityMount) {
					((BasicEntityMount) this.ridingEntity).clearAITasks();
					((BasicEntityMount) this.ridingEntity).setAIList();
				}
			}
		}
	}
	
	//decrese ammo/grudge/repair item, return true or false(not enough item)
	protected boolean decrSupplies(int type) {
		boolean isEnoughItem = true;
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
		else {	//not enough item
			getItem.stackSize = 0;
			isEnoughItem = false;
		}
				
		if(getItem.stackSize == 0) {
			getItem = null;
		}
		
		//save back itemstack
		//no need to sync because no GUI opened
		this.ExtProps.slots[i] = getItem;	
		
		return isEnoughItem;	
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
				this.sendSyncPacket();
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

	
}
