package com.lulan.shincolle.entity;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class ExtendPlayerProps implements IExtendedEntityProperties {

	public static final String PLAYER_EXTPROP_NAME = "TeitokuExtProps";
	private EntityPlayer player;
	private World world;
	private boolean hasRing;
	private boolean isRingActive;
	private boolean isRingFlying;
	/** 0:haste 1:speed 2:jump 3:damage*/
	private int[] ringEffect;
	private int marriageNum;
	private int bossCooldown;			//spawn boss cooldown
	private BasicEntityShip[] teamList;	//�W��6�����, ���u���ݭn�s, �]��entity id�C���n�Jor���A�����}���|��
	private boolean[] selectState;		//ship selected, for command control target
	private int saveId;					//���ܥثe����s��ĴX��, value = 0~5
	
	    
	@Override
	public void init(Entity entity, World world) {
		this.world = world;
		this.player = (EntityPlayer) entity;
		this.hasRing = false;
		this.isRingActive = false;
		this.isRingFlying = false;
		this.ringEffect = new int[] {0, 0, 0, 0};
		this.marriageNum = 0;
		this.bossCooldown = ConfigHandler.bossCooldown;
		this.teamList = new BasicEntityShip[6];
		this.selectState = new boolean[] {false, false, false, false, false, false};
		this.saveId = 0;
		
	}
	
	@Override
	public void saveNBTData(NBTTagCompound nbt) {
		NBTTagCompound nbtExt = new NBTTagCompound();
		
		nbtExt.setBoolean("hasRing", hasRing);
		nbtExt.setBoolean("RingOn", isRingActive);
		nbtExt.setBoolean("RingFly", isRingFlying);
		nbtExt.setIntArray("RingEffect", ringEffect);
		nbtExt.setInteger("MarriageNum", marriageNum);
		nbtExt.setInteger("BossCD", bossCooldown);
		
		nbt.setTag(PLAYER_EXTPROP_NAME, nbtExt);
	}

	@Override
	public void loadNBTData(NBTTagCompound nbt) {
		NBTTagCompound nbtExt = (NBTTagCompound) nbt.getTag(PLAYER_EXTPROP_NAME);
		
		hasRing = nbtExt.getBoolean("hasRing");
		isRingActive = nbtExt.getBoolean("RingOn");
		isRingFlying = nbtExt.getBoolean("RingFly");
		ringEffect = nbtExt.getIntArray("RingEffect");
		marriageNum = nbtExt.getInteger("MarriageNum");
		bossCooldown = nbtExt.getInteger("BossCD");
	}
	
	//getter
	public boolean isRingActive() {
		return isRingActive;
	}
	public int isRingActiveI() {
		return isRingActive ? 1 : 0;
	}
	public boolean isRingFlying() {
		return isRingFlying;
	}
	public boolean hasRing() {
		return hasRing;
	}
	public int getRingEffect(int id) {
		return ringEffect[id];
	}
	public int getMarriageNum() {
		return marriageNum;
	}
	public int getDigSpeedBoost() {
		return isRingActive ? marriageNum : 0;
	}
	public int getBossCooldown() {
		return this.bossCooldown;
	}
	
	public BasicEntityShip getTeamList(int id) {
		if(id > 5) id = 0;
		return teamList[id];
	}
	
	//meta��pointer��item damage
	public BasicEntityShip[] getTeamListWithSelectState(int meta) {	//get selected ship
		BasicEntityShip[] ships = new BasicEntityShip[6];
		
		switch(meta) {
		default:	//single mode
			//return�Ĥ@�ӧ�쪺�w��ܪ�ship
			for(int i = 0; i < 6; i++) {
				if(this.getTeamSelected(i)) {
					ships[0] = this.teamList[i];
					return ships;
				}
			}
			break;
		case 1:		//group mode
			//return�Ҧ��w��ܪ�ship
			int j = 0;
			for(int i = 0; i < 6; i++) {
				if(this.getTeamSelected(i)) {
					ships[j] = this.teamList[i];
					j++;
				}
			}
			break;
		case 2:		//formation mode
			//return���team
			return this.teamList;
		}
		
		return ships;
	}
	
	public boolean getTeamSelected(int id) {	//get selected state
		if(id > 5) id = 0;
		return selectState[id];
	}
	
	//setter
	public void setRingActive(boolean par1) {
		isRingActive = par1;
	}
	public void setRingActiveI(int par1) {
		if(par1 == 0) {
			isRingActive = false;
		}
		else {
			isRingActive = true;
		}
	}
	public void setRingFlying(boolean par1) {
		isRingFlying = par1;
	}
	public void setHasRing(boolean par1) {
		hasRing = par1;
	}
	public void setRingEffect(int id, int par1) {
		ringEffect[id] = par1;
	}
	public void setMarriageNum(int par1) {
		marriageNum = par1;
	}
	public void setBossCooldown(int par1) {
		this.bossCooldown = par1;
	}
	public void setTeamSelected(int id, boolean par1) {
		if(id > 5) id = 0;
		selectState[id] = par1;
	}
	
	/**�Nship�[�J����W��
	 * �Yship����null, ��ܭn�[�J�W�� -> ��ثe�Dnull�����O�_�Pid -> �Pid���remove��entity
	 *                                                  -> ���Pid��ܥi�s�Wentity
	 * �Y��null, ��ܲM�Ÿ�slot
	 * �Y��client��, ��ܥ�sync packet������, �h������ �ǤJ�ȳ]�w
	 */
	public void setTeamList(int id, BasicEntityShip entity, boolean isClient) {
		boolean canAdd = false;
		
		//client ����sync packets
		if(isClient) {
			if(id > 5) id = 0;
			this.teamList[id] = entity;
			return;
		}
		else {
			//entity����null�~�����s
			if(entity != null) {
				//debug: show team
				for(int k = 0; k < 6; k++) {
					LogHelper.info("DEBUG : show team list (before add) "+this.saveId+" "+k+" "+this.teamList[k]);
				}
				
				//�Yentity�D�ۤv����, �h����add team
				if(player != null && !EntityHelper.checkSameOwner(player, entity)) {
					return;
				}
				
				//�䦳�L����ship, �����ܫh�M����ship, id�����slot
				int inTeam = this.checkInTeamList(entity.getEntityId());
				if(inTeam >= 0) {
					this.teamList[inTeam] = null;
					this.saveId = inTeam;
					this.setTeamSelected(inTeam, false);
					return;
				}
				
				//�Y�L����entity, �h�Dnull�Ŧ�s, id���ܬ��U�@��slot
				for(int i = 0; i < 6; i++) {
					if(this.teamList[i] == null) {
						this.setTeamSelected(i, false);
						teamList[i] = entity;
						saveId = i + 1;
						if(saveId > 5) saveId = 0;
						return;
					}
				}
				
				//���S�Ŧ�, �h�Did������m�s
				this.setTeamSelected(this.saveId, false);
				this.teamList[this.saveId] = entity;
				//id++, �B�b0~5�����ܰ�
				saveId++;
				if(saveId > 5) saveId = 0;
				return;
			}
			else {
				if(id > 5) id = 0;
				this.setTeamSelected(id, false);
				this.teamList[id] = null;
				return;
			}
		}//end server side
	}
	
	public int checkInTeamList(int eid) {
		for(int i = 0; i < 6; i++) {
			if(this.teamList[i] != null) {
				if(teamList[i].getEntityId() == eid) {
					return i;
				}
			}
		}
		
		return -1;
	}
	
	public void clearTeamSelected() {			//clear a slot
		selectState[0] = false;
		selectState[1] = false;
		selectState[2] = false;
		selectState[3] = false;
		selectState[4] = false;
		selectState[5] = false;
	}
	
	public void clearShipTeam(int id) {		//clear a slot
		this.teamList[id] = null;
	}
	
	public void clearShipTeamAll() {		//clear all slot
		for(int i = 0; i < 6; i++) {
			this.teamList[i] = null;
		}
	}


}
