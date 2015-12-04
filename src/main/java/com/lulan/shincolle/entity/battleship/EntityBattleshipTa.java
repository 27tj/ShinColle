package com.lulan.shincolle.entity.battleship;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.ExtendShipProps;
import com.lulan.shincolle.entity.IShipSummonAttack;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.entity.other.EntityRensouhouS;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class EntityBattleshipTa extends BasicEntityShip implements IShipSummonAttack {
	
	public int numRensouhou;
	
	public EntityBattleshipTa(World world) {
		super(world);
		this.setSize(0.6F, 1.8F);
		this.setStateMinor(ID.M.ShipType, ID.ShipType.BATTLESHIP);
		this.setStateMinor(ID.M.ShipClass, ID.S_BattleshipTA);
		this.setStateMinor(ID.M.DamageType, ID.ShipDmgType.BATTLESHIP);
		this.ModelPos = new float[] {-6F, 10F, 0F, 40F};
		ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);	
		this.initTypeModify();
		
		//set attack type
		this.StateFlag[ID.F.AtkType_AirLight] = false;
		this.StateFlag[ID.F.AtkType_AirHeavy] = false;
	}
	
	@Override
	public float getEyeHeight() {
		return 1.7375F;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType() {
		return 1;
	}
	
	@Override
	public void setAIList() {
		super.setAIList();

		//use range attack
		this.tasks.addTask(12, new EntityAIShipRangeAttack(this));			   //0011
	}
	
	//check entity state every tick
  	@Override
  	public void onLivingUpdate() {
  		super.onLivingUpdate();
          
  		if(!worldObj.isRemote) {
  			//add aura to master every 100 ticks
  			if(this.ticksExisted % 100 == 0) {
  				//add num of rensouhou
  				if(this.numRensouhou < 6) numRensouhou++;
  			}
  		}    
  	}

	//Ta���B�~�W�[�ݩ�
	@Override
	public void calcShipAttributes() {
		EffectEquip[ID.EF_CRI] = EffectEquip[ID.EF_CRI] + 0.1F;
		EffectEquip[ID.EF_MISS] = EffectEquip[ID.EF_MISS] + 0.1F;
		
		super.calcShipAttributes();	
	}
	
	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {
				if(player.isSneaking()) {
					switch(getStateEmotion(ID.S.State2)) {
					case ID.State.NORMAL_2:
						setStateEmotion(ID.S.State2, ID.State.EQUIP00_2, true);
						break;
					case ID.State.EQUIP00_2:
						setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
						break;	
					default:
						setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
						break;
					}
				}
				else {
					switch(getStateEmotion(ID.S.State)) {
					case ID.State.NORMAL:	//���S��
						setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
						break;
					case ID.State.EQUIP00:	//�u���ܭ�
						setStateEmotion(ID.S.State, ID.State.EQUIP01, true);
						break;
					case ID.State.EQUIP01:	//�u���@��
						setStateEmotion(ID.S.State, ID.State.EQUIP02, true);
						break;
					case ID.State.EQUIP02:	//�ܭ�+�@��
						setStateEmotion(ID.S.State, ID.State.NORMAL, true);
						break;
					default:
						setStateEmotion(ID.S.State, ID.State.NORMAL, true);
						break;
					}
				}
				return true;
			}
		}
		
		super.interact(player);
		return false;
  	}
	
	@Override
	//range attack method, cost light ammo, attack delay = 20 / attack speed, damage = 100% atk 
	public boolean attackEntityWithAmmo(Entity target) {	
		//check num rensouhou
  		if(this.numRensouhou <= 0) {
  			return false;
  		}
  		else {
  			this.numRensouhou--;
  		}
  		
        //play entity attack sound
        if(this.rand.nextInt(10) > 5) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //light ammo--
        if(!decrAmmoNum(4)) {		//not enough ammo
        	return false;
        }
        
        //experience++
  		addShipExp(8);
  		
  		//grudge--
  		decrGrudgeNum(2);

        //�o�g�̷����S�� (�۳�s�˯����ϥίS��, ���O�n�o�e�ʥ]�ӳ]�wattackTime)
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
  		
		//spawn airplane
        if(target instanceof EntityLivingBase) {
        	if(this.getStateEmotion(ID.S.State2) > ID.State.NORMAL_2) {
        		EntityRensouhou rensoho1 = new EntityRensouhou(this.worldObj, this, (EntityLivingBase)target);
                this.worldObj.spawnEntityInWorld(rensoho1);
        	}
        	else {
        		EntityRensouhouS rensoho2 = new EntityRensouhouS(this.worldObj, this, (EntityLivingBase)target);
                this.worldObj.spawnEntityInWorld(rensoho2);
        	}
            return true;
        }

        return false;
	}
	
	@Override
	public int getKaitaiType() {
		return 1;
	}

	@Override
	public int getNumServant() {
		return this.numRensouhou;
	}

	@Override
	public void setNumServant(int num) {
		this.numRensouhou = num;
	}
	
	@Override
	public double getMountedYOffset() {
		if(this.isSitting()) {
			if(getStateEmotion(ID.S.Emotion) == ID.Emotion.BORED) {
  				return 0F;
  			}
  			else {
  				return (double)this.height * 0.0F;
  			}
  		}
  		else {
  			return (double)this.height * 0.68F;
  		}
	}
	
	
}

